package com.example.smartshoe.data.manager

import android.util.Log
import com.example.smartshoe.config.AppConfig
import com.example.smartshoe.data.local.LocalDataSource
import com.example.smartshoe.data.model.SensorDataPoint
import com.example.smartshoe.data.model.SensorDataRecord
import com.example.smartshoe.data.remote.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 传感器数据管理器
 * 封装传感器数据的上传、查询、管理功能
 * 优化版本：支持批量上传、重试机制和进度监控
 *
 * 重构：
 * 1. 使用 Hilt 注入替代单例模式
 * 2. 使用 AuthManager 获取 Token，避免重复存储
 * 3. 使用 LocalDataSource 统一管理本地存储
 */
@Singleton
class SensorDataManager @Inject constructor(
    private val localDataSource: LocalDataSource,
    private val apiService: SensorDataApiService,
    private val authManager: AuthManager
) {

    // 统一的协程作用域，使用 SupervisorJob 确保子协程错误不会影响其他协程
    private val sensorDataScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // 上传队列（用于批量上传）
    private val uploadQueue = ConcurrentLinkedQueue<UploadTask>()
    private val isUploading = AtomicBoolean(false)

    // 上传进度状态
    private val _uploadProgress = MutableStateFlow<UploadProgress?>(null)
    val uploadProgress: StateFlow<UploadProgress?> = _uploadProgress.asStateFlow()

    // 上传状态
    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState: StateFlow<UploadState> = _uploadState.asStateFlow()

    companion object {
        private const val TAG = "SensorDataManager"
    }

    /**
     * 上传进度信息
     */
    data class UploadProgress(
        val currentBatch: Int,
        val totalBatches: Int,
        val currentBatchProgress: Float, // 0.0 - 1.0
        val totalProgress: Float, // 0.0 - 1.0
        val uploadedCount: Int,
        val totalCount: Int,
        val speedBytesPerSecond: Long = 0,
        val estimatedTimeRemainingMs: Long = 0
    )

    /**
     * 上传状态
     */
    sealed class UploadState {
        object Idle : UploadState()
        object Preparing : UploadState()
        data class Uploading(val progress: UploadProgress) : UploadState()
        data class Success(val uploadedCount: Int) : UploadState()
        data class Error(val message: String, val failedCount: Int) : UploadState()
        data class PartialSuccess(val successCount: Int, val failCount: Int) : UploadState()
    }

    /**
     * 上传任务
     */
    private data class UploadTask(
        val dataPoints: List<SensorDataPoint>,
        val retryCount: Int = 0,
        val onResult: ((Boolean, String, UploadResultInfo?) -> Unit)? = null
    )

    /**
     * 获取Token
     * 从 AuthManager 获取，避免重复存储
     */
    private fun getToken(): String? {
        return authManager.getToken()
    }

    /**
     * 检查是否已登录
     */
    fun isLoggedIn(): Boolean {
        return authManager.isLoggedIn()
    }

    /**
     * 上传传感器数据（带重试机制）
     * @param dataPoints 传感器数据点列表
     * @param onResult 上传结果回调
     */
    fun uploadSensorData(
        dataPoints: List<SensorDataPoint>,
        onResult: (Boolean, String, UploadResultInfo?) -> Unit
    ) {
        val token = getToken()
        if (token.isNullOrEmpty()) {
            onResult(false, "请先登录", null)
            return
        }

        if (dataPoints.isEmpty()) {
            onResult(false, "没有数据可上传", null)
            return
        }

        sensorDataScope.launch {
            val result = uploadWithRetry(dataPoints, token, 0)
            withContext(Dispatchers.Main) {
                when (result) {
                    is UploadResult.Success -> {
                        onResult(true, result.message, result.info)
                    }
                    is UploadResult.Error -> {
                        onResult(false, result.message, null)
                    }
                }
            }
        }
    }

    /**
     * 批量上传传感器数据（带进度监控）
     * @param dataPoints 传感器数据点列表
     * @param batchSize 每批大小
     * @param onProgress 进度回调
     * @param onResult 结果回调
     */
    fun uploadSensorDataBatch(
        dataPoints: List<SensorDataPoint>,
        batchSize: Int = AppConfig.Upload.BATCH_SIZE,
        onProgress: ((UploadProgress) -> Unit)? = null,
        onResult: (Boolean, String, BatchUploadResultInfo?) -> Unit
    ) {
        val token = getToken()
        if (token.isNullOrEmpty()) {
            onResult(false, "请先登录", null)
            return
        }

        if (dataPoints.isEmpty()) {
            onResult(false, "没有数据可上传", null)
            return
        }

        sensorDataScope.launch {
            _uploadState.value = UploadState.Preparing

            val startTime = System.currentTimeMillis()
            val batches = createBatches(dataPoints, batchSize)
            val totalBatches = batches.size
            val successCount = AtomicInteger(0)
            val failCount = AtomicInteger(0)

            _uploadState.value = UploadState.Uploading(
                UploadProgress(0, totalBatches, 0f, 0f, 0, dataPoints.size)
            )

            batches.forEachIndexed { index, batch ->
                val batchStartTime = System.currentTimeMillis()

                // 更新进度
                val progress = UploadProgress(
                    currentBatch = index + 1,
                    totalBatches = totalBatches,
                    currentBatchProgress = 0f,
                    totalProgress = index.toFloat() / totalBatches,
                    uploadedCount = successCount.get() * batchSize,
                    totalCount = dataPoints.size,
                    speedBytesPerSecond = calculateSpeed(startTime, index + 1, batchSize),
                    estimatedTimeRemainingMs = calculateRemainingTime(
                        startTime,
                        index + 1,
                        totalBatches
                    )
                )

                _uploadProgress.value = progress
                _uploadState.value = UploadState.Uploading(progress)
                onProgress?.invoke(progress)

                // 上传批次
                val result = uploadWithRetry(batch, token, 0)

                when (result) {
                    is UploadResult.Success -> {
                        successCount.incrementAndGet()
                    }
                    is UploadResult.Error -> {
                        failCount.incrementAndGet()
                        Log.e(TAG, "Batch ${index + 1} upload failed: ${result.message}")
                    }
                }

                // 更新批次进度为100%
                val updatedProgress = progress.copy(
                    currentBatchProgress = 1f,
                    totalProgress = (index + 1).toFloat() / totalBatches,
                    uploadedCount = (successCount.get() * batchSize).coerceAtMost(dataPoints.size)
                )
                _uploadProgress.value = updatedProgress
                onProgress?.invoke(updatedProgress)

                // 添加小延迟避免服务器过载
                if (index < batches.size - 1) {
                    delay(AppConfig.Upload.BATCH_DELAY_MS)
                }
            }

            val totalTime = System.currentTimeMillis() - startTime
            val finalSuccessCount = successCount.get()
            val finalFailCount = failCount.get()

            val batchInfo = BatchUploadResultInfo(
                totalBatches = totalBatches,
                successBatches = finalSuccessCount,
                failBatches = finalFailCount,
                totalDataPoints = dataPoints.size,
                totalTimeMs = totalTime,
                averageSpeed = if (totalTime > 0) dataPoints.size * 1000 / totalTime else 0
            )

            withContext(Dispatchers.Main) {
                when {
                    finalFailCount == 0 -> {
                        _uploadState.value = UploadState.Success(finalSuccessCount)
                        onResult(true, "批量上传成功，共上传 $finalSuccessCount 批数据", batchInfo)
                    }
                    finalSuccessCount == 0 -> {
                        _uploadState.value = UploadState.Error("所有批次上传失败", finalFailCount)
                        onResult(false, "批量上传失败，$finalFailCount 批数据上传失败", batchInfo)
                    }
                    else -> {
                        _uploadState.value = UploadState.PartialSuccess(finalSuccessCount, finalFailCount)
                        onResult(
                            true,
                            "批量上传部分成功：$finalSuccessCount 批成功，$finalFailCount 批失败",
                            batchInfo
                        )
                    }
                }
            }

            // 重置状态
            delay(AppConfig.Upload.STATUS_RESET_DELAY_MS)
            _uploadState.value = UploadState.Idle
            _uploadProgress.value = null
        }
    }

    /**
     * 带重试的上传（只在数据备份时降采样到最高1000点，不使用压缩）
     */
    private suspend fun uploadWithRetry(
        dataPoints: List<SensorDataPoint>,
        token: String,
        retryCount: Int
    ): UploadResult {
        return try {
            // 只在数据备份时降采样到最高1000点
            val downsampledData = downsampleDataForBackup(dataPoints)
            val originalSize = dataPoints.size
            val downsampledSize = downsampledData.size
            val reductionRatio = if (originalSize > 0) {
                (1 - downsampledSize.toFloat() / originalSize) * 100
            } else 0f

            // 转换数据格式
            val startTime = downsampledData.first().timestamp
            val endTime = downsampledData.last().timestamp
            val interval = calculateInterval(downsampledData)
            val data = downsampledData.map { listOf(it.sensor1, it.sensor2, it.sensor3) }
            val recordId = generateRecordId()

            // 记录上传开始信息
            val estimatedJsonSize = downsampledSize * 20 // 粗略估计每个数据点约20字节
            Log.d(TAG, "开始上传: recordId=$recordId, 原始数据点数=$originalSize, 降采样后=$downsampledSize, 减少=${reductionRatio.toInt()}%, 预估JSON大小=${estimatedJsonSize}B")

            // 上传数据（不压缩）
            val uploadStartTime = System.currentTimeMillis()
            when (val result = apiService.uploadSensorData(
                recordId = recordId,
                startTime = startTime,
                endTime = endTime,
                interval = interval,
                data = data,
                token = token,
                compress = false // 禁用压缩，使用降采样
            )) {
                is SensorDataUploadResult.Success -> {
                    val uploadTime = System.currentTimeMillis() - uploadStartTime
                    Log.d(TAG, "上传成功: 耗时=${uploadTime}ms, 数据点数=${result.dataCount}")
                    UploadResult.Success(
                        message = result.message,
                        info = UploadResultInfo(
                            recordId = result.recordId,
                            dataCount = result.dataCount,
                            originalSize = originalSize,
                            compressedSize = downsampledSize,
                            compressionRatio = reductionRatio
                        )
                    )
                }
                is SensorDataUploadResult.Error -> {
                    throw Exception(result.message)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Upload attempt ${retryCount + 1} failed: ${e.message}", e)

            // 检查是否是401错误（Token过期）
            val isUnauthorized = e is NetworkException.UnauthorizedException ||
                e.cause is NetworkException.UnauthorizedException
            if (isUnauthorized) {
                Log.w(TAG, "Token已过期，自动登出用户")
                authManager.handleTokenExpired()
                return UploadResult.Error("登录已过期，请重新登录")
            }

            if (retryCount < AppConfig.Upload.MAX_RETRY_COUNT) {
                val delayTime = AppConfig.Upload.RETRY_DELAY_MS * (retryCount + 1)
                Log.d(TAG, "Retrying upload after ${delayTime}ms... (attempt ${retryCount + 2}/${AppConfig.Upload.MAX_RETRY_COUNT + 1})")
                delay(delayTime)
                uploadWithRetry(dataPoints, token, retryCount + 1)
            } else {
                Log.e(TAG, "Upload failed after ${AppConfig.Upload.MAX_RETRY_COUNT} retries")
                UploadResult.Error("上传失败，已重试 ${AppConfig.Upload.MAX_RETRY_COUNT} 次: ${e.message}")
            }
        }
    }

    /**
     * 创建数据批次
     */
    private fun createBatches(
        dataPoints: List<SensorDataPoint>,
        batchSize: Int
    ): List<List<SensorDataPoint>> {
        return dataPoints.chunked(batchSize)
    }

    /**
     * 数据备份时的降采样 - 最高保留1000点
     * 使用改进的LTTB算法，保留数据变化的关键点
     */
    private fun downsampleDataForBackup(dataPoints: List<SensorDataPoint>): List<SensorDataPoint> {
        // 如果数据点不超过阈值，直接返回原数据
        if (dataPoints.size <= AppConfig.Upload.MAX_BACKUP_POINTS) {
            return dataPoints
        }

        Log.d(TAG, "备份降采样: 原始数据点数=${dataPoints.size}, 目标点数=${AppConfig.Upload.MAX_BACKUP_POINTS}")

        val result = mutableListOf<SensorDataPoint>()
        val targetCount = AppConfig.Upload.MAX_BACKUP_POINTS
        val bucketSize = (dataPoints.size - 2).toDouble() / (targetCount - 2)

        // 保留第一个点
        result.add(dataPoints.first())

        // 使用改进的LTTB算法
        for (i in 0 until targetCount - 2) {
            val bucketStart = (1 + i * bucketSize).toInt()
            val bucketEnd = (1 + (i + 1) * bucketSize).toInt()
                .coerceAtMost(dataPoints.size - 1)

            if (bucketStart >= bucketEnd) continue

            // 计算当前桶的平均点
            var avgSensor1 = 0
            var avgSensor2 = 0
            var avgSensor3 = 0
            for (j in bucketStart until bucketEnd) {
                avgSensor1 += dataPoints[j].sensor1
                avgSensor2 += dataPoints[j].sensor2
                avgSensor3 += dataPoints[j].sensor3
            }
            val count = bucketEnd - bucketStart
            avgSensor1 /= count
            avgSensor2 /= count
            avgSensor3 /= count

            // 找到桶中最重要的点（与平均点差异最大）
            var maxImportance = -1.0
            var selectedPoint = dataPoints[bucketStart]

            for (j in bucketStart until bucketEnd) {
                val point = dataPoints[j]
                // 计算与平均点的差异（三个传感器的综合差异）
                val importance = kotlin.math.abs(point.sensor1 - avgSensor1) +
                        kotlin.math.abs(point.sensor2 - avgSensor2) +
                        kotlin.math.abs(point.sensor3 - avgSensor3)

                if (importance > maxImportance) {
                    maxImportance = importance.toDouble()
                    selectedPoint = point
                }
            }

            result.add(selectedPoint)
        }

        // 保留最后一个点
        result.add(dataPoints.last())

        Log.d(TAG, "备份降采样完成: 结果数据点数=${result.size}")
        return result
    }

    /**
     * 计算上传速度
     */
    private fun calculateSpeed(startTime: Long, completedBatches: Int, batchSize: Int): Long {
        val elapsedTime = System.currentTimeMillis() - startTime
        return if (elapsedTime > 0) {
            (completedBatches * batchSize * 1000 / elapsedTime).toLong()
        } else 0
    }

    /**
     * 计算剩余时间
     */
    private fun calculateRemainingTime(
        startTime: Long,
        completedBatches: Int,
        totalBatches: Int
    ): Long {
        if (completedBatches == 0) return 0
        val elapsedTime = System.currentTimeMillis() - startTime
        val avgTimePerBatch = elapsedTime / completedBatches
        val remainingBatches = totalBatches - completedBatches
        return avgTimePerBatch * remainingBatches
    }

    /**
     * 获取用户的数据记录列表
     */
    fun getUserRecords(
        page: Int = 0,
        size: Int = AppConfig.Cache.HISTORY_PAGE_SIZE,
        useCache: Boolean = true,
        onResult: (Boolean, String, List<SensorDataRecord>?, Long) -> Unit
    ) {
        val token = getToken()
        if (token.isNullOrEmpty()) {
            onResult(false, "请先登录", null, 0)
            return
        }

        sensorDataScope.launch {
            when (val result = apiService.getUserRecords(token, page, size, useCache)) {
                is SensorDataRecordsResult.Success -> {
                    withContext(Dispatchers.Main) {
                        onResult(true, "获取成功", result.records, result.total)
                    }
                }
                is SensorDataRecordsResult.Error -> {
                    withContext(Dispatchers.Main) {
                        onResult(false, result.message, null, 0)
                    }
                }
            }
        }
    }

    /**
     * 删除记录
     */
    fun deleteRecord(recordId: String, onResult: (Boolean, String) -> Unit) {
        val token = getToken()
        if (token.isNullOrEmpty()) {
            onResult(false, "请先登录")
            return
        }

        sensorDataScope.launch {
            when (val result = apiService.deleteRecord(recordId, token)) {
                is SensorDataResult.Success -> {
                    withContext(Dispatchers.Main) {
                        onResult(true, result.message)
                    }
                }
                is SensorDataResult.Error -> {
                    withContext(Dispatchers.Main) {
                        onResult(false, result.message)
                    }
                }
            }
        }
    }

    /**
     * 按时间范围查询历史记录（支持分页）
     */
    fun getRecordsByTimeRange(
        startTime: Long,
        endTime: Long,
        page: Int = 0,
        size: Int = AppConfig.Cache.HISTORY_PAGE_SIZE,
        useCache: Boolean = true,
        onResult: (Boolean, String, List<SensorDataRecord>?, Long) -> Unit
    ) {
        val token = getToken()
        if (token.isNullOrEmpty()) {
            onResult(false, "请先登录", null, 0)
            return
        }

        sensorDataScope.launch {
            when (val result = apiService.getRecordsByTimeRange(token, startTime, endTime, page, size, useCache)) {
                is SensorDataRecordsResult.Success -> {
                    withContext(Dispatchers.Main) {
                        onResult(true, "获取成功", result.records, result.total)
                    }
                }
                is SensorDataRecordsResult.Error -> {
                    withContext(Dispatchers.Main) {
                        onResult(false, result.message, null, 0)
                    }
                }
            }
        }
    }

    /**
     * 获取记录详情
     */
    fun getRecordDetail(
        recordId: String,
        useCache: Boolean = true,
        onResult: (Boolean, String, SensorDataRecord?, List<SensorDataPoint>?) -> Unit
    ) {
        val token = getToken()
        if (token.isNullOrEmpty()) {
            onResult(false, "请先登录", null, null)
            return
        }

        sensorDataScope.launch {
            when (val result = apiService.getRecordDetail(recordId, token, useCache)) {
                is SensorDataDetailResult.Success -> {
                    withContext(Dispatchers.Main) {
                        onResult(true, "获取成功", result.record, result.dataPoints)
                    }
                }
                is SensorDataDetailResult.Error -> {
                    withContext(Dispatchers.Main) {
                        onResult(false, result.message, null, null)
                    }
                }
            }
        }
    }

    /**
     * 计算采样间隔
     */
    private fun calculateInterval(dataPoints: List<SensorDataPoint>): Int {
        if (dataPoints.size < 2) return AppConfig.Upload.DEFAULT_INTERVAL_MS

        val intervals = mutableListOf<Long>()
        for (i in 1 until dataPoints.size) {
            intervals.add(dataPoints[i].timestamp - dataPoints[i - 1].timestamp)
        }

        return intervals.average().toInt().coerceIn(50, 1000)
    }

    /**
     * 生成记录ID
     */
    private fun generateRecordId(): String {
        return "record_${System.currentTimeMillis()}_${UUID.randomUUID().toString().substring(0, 8)}"
    }

    /**
     * 取消所有上传任务
     */
    fun cancelAllUploads() {
        uploadQueue.clear()
        isUploading.set(false)
        sensorDataScope.coroutineContext.cancelChildren()
        _uploadState.value = UploadState.Idle
        _uploadProgress.value = null
        Log.d(TAG, "All uploads cancelled")
    }

    /**
     * 获取当前上传状态
     */
    fun getCurrentUploadState(): UploadState = _uploadState.value

    /**
     * 销毁管理器
     */
    fun destroy() {
        cancelAllUploads()
        sensorDataScope.cancel()
        Log.d(TAG, "SensorDataManager destroyed")
    }

    /**
     * 上传结果
     */
    private sealed class UploadResult {
        data class Success(val message: String, val info: UploadResultInfo) : UploadResult()
        data class Error(val message: String) : UploadResult()
    }

    /**
     * 上传结果信息
     */
    data class UploadResultInfo(
        val recordId: String,
        val dataCount: Int,
        val originalSize: Int,
        val compressedSize: Int,
        val compressionRatio: Float
    )

    /**
     * 批量上传结果信息
     */
    data class BatchUploadResultInfo(
        val totalBatches: Int,
        val successBatches: Int,
        val failBatches: Int,
        val totalDataPoints: Int,
        val totalTimeMs: Long,
        val averageSpeed: Long
    ) {
        fun getSuccessRate(): Float {
            return if (totalBatches > 0) successBatches.toFloat() / totalBatches else 0f
        }
    }
}
