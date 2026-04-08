package com.example.smartshoe.llm

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.StatFs
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File
import java.io.RandomAccessFile
import java.net.SocketTimeoutException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
            as ConnectivityManager

    private val isCancelled = AtomicBoolean(false)

    data class ModelInfo(
        val name: String,
        val url: String,
        val fileName: String,
        val expectedSize: Long,
        val expectedHash: String,
        val description: String
    )

    companion object {
        // 阿里云 OSS 配置 - 请替换为实际的 OSS 地址
        val DEFAULT_MODEL = ModelInfo(
            name = "Qwen-0.5B-Instruct",
            url = "https://your-bucket.oss-cn-beijing.aliyuncs.com/models/qwen-0.5b-instruct-q4_k_m.gguf",
            fileName = "qwen-0.5b-instruct-q4_k_m.gguf",
            expectedSize = 352_321_632,
            expectedHash = "a1b2c3d4e5f6789",
            description = "通义千问 0.5B 量化模型"
        )

        const val BUFFER_SIZE = 8192
        const val MIN_STORAGE_MB = 600
        const val MAX_RETRY_COUNT = 3
    }

    enum class NetworkType { WIFI, MOBILE, NONE }

    sealed class DownloadState {
        object Idle : DownloadState()
        data class Checking(val message: String) : DownloadState()
        data class Downloading(
            val progress: Float,
            val downloadedBytes: Long,
            val totalBytes: Long,
            val speedKbps: Double
        ) : DownloadState()
        object Verifying : DownloadState()
        data class Success(val file: File) : DownloadState()
        data class Error(val message: String, val retryable: Boolean = true) : DownloadState()
        object Cancelled : DownloadState()
    }

    /**
     * 检查网络状态
     */
    fun getNetworkType(): NetworkType {
        val network = connectivityManager.activeNetwork ?: return NetworkType.NONE
        val capabilities = connectivityManager.getNetworkCapabilities(network)
            ?: return NetworkType.NONE

        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.MOBILE
            else -> NetworkType.NONE
        }
    }

    /**
     * 检查模型是否就绪
     */
    suspend fun isModelReady(model: ModelInfo = DEFAULT_MODEL): Boolean =
        withContext(Dispatchers.IO) {
            val modelFile = getModelFile(model.fileName)
            if (!modelFile.exists()) return@withContext false
            if (modelFile.length() != model.expectedSize) return@withContext false
            true
        }

    fun getModelFile(fileName: String): File {
        val modelsDir = File(context.filesDir, "models").apply {
            if (!exists()) mkdirs()
        }
        return File(modelsDir, fileName)
    }

    /**
     * 取消下载
     */
    fun cancelDownload() {
        isCancelled.set(true)
    }

    /**
     * 下载模型（支持断点续传和取消）
     */
    fun downloadModel(
        model: ModelInfo = DEFAULT_MODEL,
        requireWifi: Boolean = true
    ): Flow<DownloadState> = flow {
        isCancelled.set(false)

        // 检查网络
        val networkType = getNetworkType()
        if (networkType == NetworkType.NONE) {
            emit(DownloadState.Error("无网络连接", retryable = true))
            return@flow
        }
        if (requireWifi && networkType != NetworkType.WIFI) {
            emit(DownloadState.Error("请在 WiFi 环境下下载模型（约336MB）", retryable = true))
            return@flow
        }

        emit(DownloadState.Checking("检查存储空间..."))

        if (!hasEnoughStorage(model.expectedSize)) {
            emit(
                DownloadState.Error(
                    "存储空间不足，需要 ${formatBytes(model.expectedSize)}",
                    retryable = false
                )
            )
            return@flow
        }

        val modelFile = getModelFile(model.fileName)
        val tempFile = File(modelFile.parent, "${model.fileName}.tmp")

        if (isModelReady(model)) {
            emit(DownloadState.Success(modelFile))
            return@flow
        }

        // 重试机制
        var retryCount = 0
        var lastError: Exception? = null

        while (retryCount < MAX_RETRY_COUNT) {
            if (isCancelled.get()) {
                emit(DownloadState.Cancelled)
                return@flow
            }

            try {
                performDownloadWithProgress(model, tempFile, modelFile).collect { state ->
                    emit(state)
                }

                // 检查是否成功
                if (isModelReady(model)) {
                    return@flow
                }

                retryCount++
                if (retryCount < MAX_RETRY_COUNT) {
                    delay((1000 * retryCount).toLong())
                }
            } catch (e: Exception) {
                lastError = e
                retryCount++
                if (retryCount >= MAX_RETRY_COUNT) break
                delay((1000 * retryCount).toLong())
            }
        }

        emit(
            DownloadState.Error(
                lastError?.message ?: "下载失败，请重试",
                retryable = true
            )
        )
    }.flowOn(Dispatchers.IO)

    private fun performDownloadWithProgress(
        model: ModelInfo,
        tempFile: File,
        modelFile: File
    ): Flow<DownloadState> = flow {
        val downloadedBytes = if (tempFile.exists()) tempFile.length() else 0L

        val request = Request.Builder()
            .url(model.url)
            .apply {
                if (downloadedBytes > 0) {
                    header("Range", "bytes=$downloadedBytes-")
                }
            }
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful && response.code != 206) {
                emit(DownloadState.Error("下载失败: HTTP ${response.code}"))
                return@flow
            }

            val body = response.body ?: run {
                emit(DownloadState.Error("响应体为空"))
                return@flow
            }

            val totalBytes = body.contentLength() + downloadedBytes

            RandomAccessFile(tempFile, "rw").use { raf ->
                if (downloadedBytes > 0) raf.seek(downloadedBytes)

                val source = body.source()
                val buffer = ByteArray(BUFFER_SIZE)
                var totalRead = downloadedBytes
                var lastEmitTime = System.currentTimeMillis()
                var lastReadBytes = downloadedBytes

                while (!isCancelled.get()) {
                    val read = try {
                        source.read(buffer)
                    } catch (e: SocketTimeoutException) {
                        emit(DownloadState.Error("网络超时，请重试", retryable = true))
                        return@flow
                    }

                    if (read == -1) break

                    raf.write(buffer, 0, read)
                    totalRead += read

                    val now = System.currentTimeMillis()
                    if (now - lastEmitTime > 500) {
                        val duration = (now - lastEmitTime) / 1000.0
                        val speedKbps = ((totalRead - lastReadBytes) / 1024.0) / duration

                        emit(
                            DownloadState.Downloading(
                                progress = totalRead.toFloat() / totalBytes,
                                downloadedBytes = totalRead,
                                totalBytes = totalBytes,
                                speedKbps = speedKbps
                            )
                        )

                        lastEmitTime = now
                        lastReadBytes = totalRead
                    }
                }

                if (isCancelled.get()) {
                    emit(DownloadState.Cancelled)
                    return@flow
                }
            }

            // 验证文件
            emit(DownloadState.Verifying)

            if (tempFile.length() != model.expectedSize) {
                tempFile.delete()
                emit(DownloadState.Error("文件大小不匹配"))
                return@flow
            }

            tempFile.renameTo(modelFile)
            emit(DownloadState.Success(modelFile))
        }
    }

    private fun hasEnoughStorage(requiredBytes: Long): Boolean {
        val stat = StatFs(context.filesDir.path)
        val availableBytes = stat.availableBytes
        return availableBytes > requiredBytes + (MIN_STORAGE_MB * 1024 * 1024)
    }

    private fun formatBytes(bytes: Long): String {
        val gb = bytes / (1024.0 * 1024 * 1024)
        return if (gb >= 1) String.format("%.2f GB", gb) else "${bytes / (1024 * 1024)} MB"
    }

    suspend fun deleteModel(model: ModelInfo = DEFAULT_MODEL): Boolean =
        withContext(Dispatchers.IO) {
            getModelFile(model.fileName).delete()
        }

    suspend fun getModelSize(model: ModelInfo = DEFAULT_MODEL): Long =
        withContext(Dispatchers.IO) {
            val file = getModelFile(model.fileName)
            if (file.exists()) file.length() else 0L
        }
}
