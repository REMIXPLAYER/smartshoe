package com.example.smartshoe.data.remote

import android.util.Log
import com.example.smartshoe.data.SensorDataPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream

import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.Deflater
import java.util.zip.GZIPOutputStream

/**
 * 传感器数据API服务
 * 提供上传、查询传感器数据的HTTP接口
 * 优化版本：支持请求合并、缓存和数据压缩
 */
interface SensorDataApiService {

    /**
     * 上传传感器数据
     * @param recordId 记录ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param interval 采样间隔（毫秒）
     * @param data 传感器数据 [[sensor1, sensor2, sensor3], ...]
     * @param token 用户Token
     * @param compress 是否启用压缩
     * @return 上传结果
     */
    suspend fun uploadSensorData(
        recordId: String,
        startTime: Long,
        endTime: Long,
        interval: Int,
        data: List<List<Int>>,
        token: String,
        compress: Boolean = true
    ): SensorDataUploadResult

    /**
     * 批量上传传感器数据
     * @param batches 批量数据列表
     * @param token 用户Token
     * @param onProgress 进度回调
     * @return 批量上传结果
     */
    suspend fun uploadSensorDataBatch(
        batches: List<UploadBatch>,
        token: String,
        onProgress: ((Int, Int) -> Unit)? = null
    ): BatchUploadResult

    /**
     * 获取用户数据记录列表
     * @param token 用户Token
     * @param page 页码
     * @param size 每页大小
     * @param useCache 是否使用缓存
     * @return 记录列表
     */
    suspend fun getUserRecords(
        token: String,
        page: Int = 0,
        size: Int = 20,
        useCache: Boolean = true
    ): SensorDataRecordsResult

    /**
     * 删除记录
     * @param recordId 记录ID
     * @param token 用户Token
     * @return 删除结果
     */
    suspend fun deleteRecord(recordId: String, token: String): SensorDataResult

    /**
     * 按时间范围查询历史记录
     * @param token 用户Token
     * @param startTime 开始时间戳
     * @param endTime 结束时间戳
     * @param useCache 是否使用缓存
     * @return 记录列表
     */
    suspend fun getRecordsByTimeRange(
        token: String,
        startTime: Long,
        endTime: Long,
        page: Int = 0,
        size: Int = 20,
        useCache: Boolean = true
    ): SensorDataRecordsResult

    /**
     * 获取记录详情（包含完整数据）
     * @param recordId 记录ID
     * @param token 用户Token
     * @param useCache 是否使用缓存
     * @return 记录详情和数据
     */
    suspend fun getRecordDetail(
        recordId: String,
        token: String,
        useCache: Boolean = true
    ): SensorDataDetailResult

    companion object {
        // 服务器基础URL - 生产环境服务器
        //private const val BASE_URL = "http://39.97.37.162:8080/api"

        // 测试环境地址（本地服务器）
        private const val BASE_URL = "http://10.0.2.2:8080/api"

        private const val UPLOAD_URL = "$BASE_URL/sensor/upload"
        private const val BATCH_UPLOAD_URL = "$BASE_URL/sensor/upload/batch"
        private const val RECORDS_URL = "$BASE_URL/sensor/records"
        private const val DELETE_URL = "$BASE_URL/sensor/record"
        private const val CONNECT_TIMEOUT = 30000 // 连接超时30秒
        private const val READ_TIMEOUT = 120000 // 读取超时2分钟，支持大数据量上传

        // 压缩阈值：数据大于10KB才进行压缩
        private const val COMPRESSION_THRESHOLD_BYTES = 10 * 1024

        fun create(): SensorDataApiService = SensorDataApiServiceImpl()
    }

    private class SensorDataApiServiceImpl : SensorDataApiService {

        private val requestCache = RequestCache.getInstance()
        private val requestBatcher = RequestBatcher.getInstance()

        override suspend fun uploadSensorData(
            recordId: String,
            startTime: Long,
            endTime: Long,
            interval: Int,
            data: List<List<Int>>,
            token: String,
            compress: Boolean
        ): SensorDataUploadResult = withContext(Dispatchers.IO) {
            try {
                // 构建JSON请求体
                val jsonBody = JSONObject().apply {
                    put("recordId", recordId)
                    put("startTime", startTime)
                    put("endTime", endTime)
                    put("interval", interval)
                    put("data", JSONArray(data.map { JSONArray(it) }))
                }

                val jsonString = jsonBody.toString()
                Log.d("SensorDataApiService", "Request JSON (first 1000 chars): ${jsonString.take(1000)}")
                Log.d("SensorDataApiService", "Request JSON length: ${jsonString.length} bytes")
                Log.d("SensorDataApiService", "Request data points count: ${data.size}")
                Log.d("SensorDataApiService", "Request recordId: $recordId, startTime: $startTime, endTime: $endTime, interval: $interval")

                // 注意：服务器支持应用层数据压缩（存储时），但不支持HTTP传输层gzip压缩
                val requestBody = jsonString.toByteArray(Charsets.UTF_8)
                Log.d("SensorDataApiService", "Request body: ${requestBody.size} bytes (uncompressed)")

                // 执行HTTP请求（不压缩请求体）
                val result = executeUploadRequest(requestBody, null, token)

                // 上传成功后清除相关缓存
                if (result is SensorDataUploadResult.Success) {
                    invalidateRelatedCache(token)
                }

                result
            } catch (e: Exception) {
                Log.e("SensorDataApiService", "Upload error: ${e.message}", e)
                SensorDataUploadResult.Error("网络错误: ${e.message}")
            }
        }

        override suspend fun uploadSensorDataBatch(
            batches: List<UploadBatch>,
            token: String,
            onProgress: ((Int, Int) -> Unit)?
        ): BatchUploadResult = withContext(Dispatchers.IO) {
            if (batches.isEmpty()) {
                return@withContext BatchUploadResult.Success(0, 0, emptyList())
            }

            val totalBatches = batches.size
            val results = mutableListOf<SensorDataUploadResult>()
            var successCount = 0
            var failCount = 0

            batches.forEachIndexed { index, batch ->
                // 上报进度
                onProgress?.invoke(index + 1, totalBatches)

                val result = uploadSensorData(
                    recordId = batch.recordId,
                    startTime = batch.startTime,
                    endTime = batch.endTime,
                    interval = batch.interval,
                    data = batch.data,
                    token = token,
                    compress = true
                )

                results.add(result)

                when (result) {
                    is SensorDataUploadResult.Success -> successCount++
                    is SensorDataUploadResult.Error -> failCount++
                }
            }

            // 清除相关缓存
            invalidateRelatedCache(token)

            if (failCount == 0) {
                BatchUploadResult.Success(successCount, failCount, results)
            } else {
                BatchUploadResult.PartialSuccess(successCount, failCount, results)
            }
        }

        override suspend fun getUserRecords(
            token: String,
            page: Int,
            size: Int,
            useCache: Boolean
        ): SensorDataRecordsResult {
            val cacheKey = requestCache.generateKey("records", mapOf("page" to page, "size" to size))

            // 尝试从缓存获取
            if (useCache) {
                val cached = requestCache.get<SensorDataRecordsResult>(cacheKey)
                if (cached != null) {
                    return cached
                }
            }

            // 使用请求合并器执行请求
            return requestBatcher.execute(cacheKey) {
                fetchUserRecords(token, page, size)
            }.also { result ->
                // 缓存成功的结果
                if (result is SensorDataRecordsResult.Success && useCache) {
                    requestCache.put(cacheKey, result, 60 * 1000) // 缓存1分钟
                }
            }
        }

        private suspend fun fetchUserRecords(
            token: String,
            page: Int,
            size: Int
        ): SensorDataRecordsResult = withContext(Dispatchers.IO) {
            try {
                val url = URL("$RECORDS_URL?page=$page&size=$size")
                val connection = url.openConnection() as HttpURLConnection

                connection.apply {
                    requestMethod = "GET"
                    setRequestProperty("Authorization", "Bearer $token")
                    connectTimeout = CONNECT_TIMEOUT
                    readTimeout = READ_TIMEOUT
                }

                val responseCode = connection.responseCode
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()

                parseRecordsResponse(response, responseCode)
            } catch (e: Exception) {
                SensorDataRecordsResult.Error("网络错误: ${e.message}")
            }
        }

        override suspend fun deleteRecord(recordId: String, token: String): SensorDataResult = withContext(Dispatchers.IO) {
            try {
                val url = URL("$DELETE_URL/$recordId")
                val connection = url.openConnection() as HttpURLConnection

                connection.apply {
                    requestMethod = "DELETE"
                    setRequestProperty("Authorization", "Bearer $token")
                    connectTimeout = CONNECT_TIMEOUT
                    readTimeout = READ_TIMEOUT
                }

                val responseCode = connection.responseCode
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()

                // 删除成功后清除缓存
                invalidateRelatedCache(token)

                parseResultResponse(response, responseCode)
            } catch (e: Exception) {
                SensorDataResult.Error("网络错误: ${e.message}")
            }
        }

        override suspend fun getRecordsByTimeRange(
            token: String,
            startTime: Long,
            endTime: Long,
            page: Int,
            size: Int,
            useCache: Boolean
        ): SensorDataRecordsResult {
            val cacheKey = requestCache.generateKey(
                "records_range",
                mapOf("start" to startTime, "end" to endTime, "page" to page, "size" to size)
            )

            // 尝试从缓存获取
            if (useCache) {
                val cached = requestCache.get<SensorDataRecordsResult>(cacheKey)
                if (cached != null) {
                    return cached
                }
            }

            // 使用请求合并器执行请求
            return requestBatcher.execute(cacheKey) {
                fetchRecordsByTimeRange(token, startTime, endTime, page, size)
            }.also { result ->
                // 缓存成功的结果
                if (result is SensorDataRecordsResult.Success && useCache) {
                    requestCache.put(cacheKey, result, 2 * 60 * 1000) // 缓存2分钟
                }
            }
        }

        private suspend fun fetchRecordsByTimeRange(
            token: String,
            startTime: Long,
            endTime: Long,
            page: Int,
            size: Int
        ): SensorDataRecordsResult = withContext(Dispatchers.IO) {
            try {
                val url = URL("$RECORDS_URL/range?startTime=$startTime&endTime=$endTime&page=$page&size=$size")
                val connection = url.openConnection() as HttpURLConnection

                connection.apply {
                    requestMethod = "GET"
                    setRequestProperty("Authorization", "Bearer $token")
                    connectTimeout = CONNECT_TIMEOUT
                    readTimeout = READ_TIMEOUT
                }

                val responseCode = connection.responseCode
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()

                parseRecordsResponse(response, responseCode)
            } catch (e: Exception) {
                SensorDataRecordsResult.Error("网络错误: ${e.message}")
            }
        }

        override suspend fun getRecordDetail(
            recordId: String,
            token: String,
            useCache: Boolean
        ): SensorDataDetailResult {
            val cacheKey = requestCache.generateKey("record_detail", mapOf("id" to recordId))

            // 尝试从缓存获取
            if (useCache) {
                val cached = requestCache.get<SensorDataDetailResult>(cacheKey)
                if (cached != null) {
                    return cached
                }
            }

            // 使用请求合并器执行请求
            return requestBatcher.execute(cacheKey) {
                fetchRecordDetail(recordId, token)
            }.also { result ->
                // 缓存成功的结果
                if (result is SensorDataDetailResult.Success && useCache) {
                    requestCache.put(cacheKey, result, 5 * 60 * 1000) // 缓存5分钟
                }
            }
        }

        private suspend fun fetchRecordDetail(
            recordId: String,
            token: String
        ): SensorDataDetailResult = withContext(Dispatchers.IO) {
            try {
                val url = URL("$RECORDS_URL/$recordId/detail")
                val connection = url.openConnection() as HttpURLConnection

                connection.apply {
                    requestMethod = "GET"
                    setRequestProperty("Authorization", "Bearer $token")
                    connectTimeout = CONNECT_TIMEOUT
                    readTimeout = READ_TIMEOUT
                }

                val responseCode = connection.responseCode
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()

                parseDetailResponse(response, responseCode)
            } catch (e: Exception) {
                SensorDataDetailResult.Error("网络错误: ${e.message}")
            }
        }

        /**
         * 执行上传请求
         */
        private fun executeUploadRequest(
            body: ByteArray,
            contentEncoding: String?,
            token: String
        ): SensorDataUploadResult {
            val url = URL(UPLOAD_URL)
            val connection = url.openConnection() as HttpURLConnection

            // 记录请求开始时间
            val requestStartTime = System.currentTimeMillis()

            connection.apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Accept", "application/json")
                contentEncoding?.let { setRequestProperty("Content-Encoding", it) }
                // 设置请求体长度，避免分块传输可能导致的问题
                setFixedLengthStreamingMode(body.size)
                connectTimeout = CONNECT_TIMEOUT
                readTimeout = READ_TIMEOUT
            }

            Log.d("SensorDataApiService", "[网络诊断] 请求URL: $UPLOAD_URL")
            Log.d("SensorDataApiService", "[网络诊断] 请求大小: ${body.size} bytes (${body.size / 1024} KB)")
            Log.d("SensorDataApiService", "[网络诊断] 连接超时: ${CONNECT_TIMEOUT}ms, 读取超时: ${READ_TIMEOUT}ms")

            return try {
                // 记录连接建立时间
                val connectStartTime = System.currentTimeMillis()
                connection.connect()
                val connectTime = System.currentTimeMillis() - connectStartTime
                Log.d("SensorDataApiService", "[网络诊断] 连接建立耗时: ${connectTime}ms")

                // 发送请求体
                val writeStartTime = System.currentTimeMillis()
                connection.outputStream.use { outputStream ->
                    outputStream.write(body)
                    outputStream.flush()
                }
                val writeTime = System.currentTimeMillis() - writeStartTime
                Log.d("SensorDataApiService", "[网络诊断] 请求体发送耗时: ${writeTime}ms")

                // 获取响应
                val responseStartTime = System.currentTimeMillis()
                val responseCode = connection.responseCode
                val responseTime = System.currentTimeMillis() - responseStartTime
                val totalTime = System.currentTimeMillis() - requestStartTime

                Log.d("SensorDataApiService", "[网络诊断] 响应码: $responseCode, 获取响应耗时: ${responseTime}ms, 总耗时: ${totalTime}ms")

                val response = if (responseCode == HttpURLConnection.HTTP_OK) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                }

                Log.d("SensorDataApiService", "Upload response: code=$responseCode, body=$response")

                parseUploadResponse(response, responseCode)
            } catch (e: java.net.SocketTimeoutException) {
                val timeoutType = if (System.currentTimeMillis() - requestStartTime < CONNECT_TIMEOUT) {
                    "读取超时"
                } else {
                    "连接超时"
                }
                Log.e("SensorDataApiService", "[网络诊断] $timeoutType: ${e.message}")
                SensorDataUploadResult.Error("$timeoutType，请检查网络连接或稍后重试")
            } catch (e: java.net.ConnectException) {
                Log.e("SensorDataApiService", "[网络诊断] 连接失败: ${e.message}")
                SensorDataUploadResult.Error("无法连接到服务器，请检查网络")
            } catch (e: java.net.SocketException) {
                Log.e("SensorDataApiService", "[网络诊断] Socket错误: ${e.message}")
                SensorDataUploadResult.Error("网络连接中断，可能是网络不稳定或服务器重置连接")
            } catch (e: Exception) {
                Log.e("SensorDataApiService", "[网络诊断] 上传错误: ${e.message}", e)
                SensorDataUploadResult.Error("网络错误: ${e.message}")
            } finally {
                connection.disconnect()
            }
        }

        /**
         * 使用GZIP压缩数据
         */
        private fun compressData(data: ByteArray): ByteArray {
            val outputStream = ByteArrayOutputStream()
            GZIPOutputStream(outputStream).use { gzipStream ->
                gzipStream.write(data)
            }
            return outputStream.toByteArray()
        }

        /**
         * 使用Deflater压缩数据（备用方案）
         */
        private fun compressDataDeflater(data: ByteArray): ByteArray {
            val deflater = Deflater(Deflater.BEST_COMPRESSION)
            deflater.setInput(data)
            deflater.finish()

            val outputStream = ByteArrayOutputStream(data.size)
            val buffer = ByteArray(1024)

            while (!deflater.finished()) {
                val count = deflater.deflate(buffer)
                outputStream.write(buffer, 0, count)
            }

            deflater.end()
            return outputStream.toByteArray()
        }

        /**
         * 清除相关缓存
         */
        private fun invalidateRelatedCache(token: String) {
            // 清除记录列表缓存
            requestCache.clear()
        }

        private fun parseUploadResponse(response: String, code: Int): SensorDataUploadResult {
            return try {
                val json = JSONObject(response)

                // 处理Spring Boot默认错误格式
                if (code != HttpURLConnection.HTTP_OK) {
                    val errorMessage = json.optString("error", "")
                    val status = json.optInt("status", 0)
                    val path = json.optString("path", "")

                    if (errorMessage.isNotEmpty()) {
                        return SensorDataUploadResult.Error("服务器错误 [$status]: $errorMessage (路径: $path)")
                    }
                }

                // 处理自定义API格式
                val success = json.optBoolean("success", false)

                if (success && code == HttpURLConnection.HTTP_OK) {
                    val data = json.optJSONObject("data")
                    SensorDataUploadResult.Success(
                        recordId = data?.optString("recordId", "") ?: "",
                        dataCount = data?.optInt("dataCount", 0) ?: 0,
                        originalSize = data?.optInt("originalSize", 0) ?: 0,
                        compressedSize = data?.optInt("compressedSize", 0) ?: 0,
                        compressionRatio = data?.optDouble("compressionRatio", 0.0)?.toFloat() ?: 0f,
                        message = json.optString("message", "上传成功")
                    )
                } else {
                    SensorDataUploadResult.Error(json.optString("message", "上传失败"))
                }
            } catch (e: Exception) {
                SensorDataUploadResult.Error("解析响应失败: ${e.message}")
            }
        }

        private fun parseRecordsResponse(response: String, code: Int): SensorDataRecordsResult {
            return try {
                val json = JSONObject(response)
                val success = json.optBoolean("success", false)

                if (success && code == HttpURLConnection.HTTP_OK) {
                    val dataArray = json.optJSONArray("data") ?: JSONArray()
                    val records = mutableListOf<SensorDataRecord>()

                    for (i in 0 until dataArray.length()) {
                        val item = dataArray.optJSONObject(i)
                        records.add(
                            SensorDataRecord(
                                recordId = item?.optString("recordId", "") ?: "",
                                startTime = item?.optLong("startTime", 0) ?: 0,
                                endTime = item?.optLong("endTime", 0) ?: 0,
                                dataCount = item?.optInt("dataCount", 0) ?: 0,
                                interval = item?.optInt("interval", 0) ?: 0,
                                createdAt = item?.optString("createdAt", "") ?: "",
                                originalSize = item?.optInt("originalSize", 0) ?: 0,
                                compressedSize = item?.optInt("compressedSize", 0) ?: 0,
                                compressionRatio = parseCompressionRatio(item?.optString("compressionRatio", "0%"))
                            )
                        )
                    }

                    SensorDataRecordsResult.Success(
                        records = records,
                        total = json.optLong("total", 0),
                        page = json.optInt("page", 0),
                        size = json.optInt("size", 0)
                    )
                } else {
                    SensorDataRecordsResult.Error(json.optString("message", "获取失败"))
                }
            } catch (e: Exception) {
                SensorDataRecordsResult.Error("解析响应失败: ${e.message}")
            }
        }

        private fun parseResultResponse(response: String, code: Int): SensorDataResult {
            return try {
                val json = JSONObject(response)
                val success = json.optBoolean("success", false)

                if (success && code == HttpURLConnection.HTTP_OK) {
                    SensorDataResult.Success(json.optString("message", "操作成功"))
                } else {
                    SensorDataResult.Error(json.optString("message", "操作失败"))
                }
            } catch (e: Exception) {
                SensorDataResult.Error("解析响应失败: ${e.message}")
            }
        }

        private fun parseDetailResponse(response: String, code: Int): SensorDataDetailResult {
            return try {
                val json = JSONObject(response)
                val success = json.optBoolean("success", false)

                if (success && code == HttpURLConnection.HTTP_OK) {
                    val recordJson = json.optJSONObject("record") ?: JSONObject()
                    val dataArray = json.optJSONArray("data") ?: JSONArray()

                    val record = SensorDataRecord(
                        recordId = recordJson.optString("recordId", ""),
                        startTime = recordJson.optLong("startTime", 0),
                        endTime = recordJson.optLong("endTime", 0),
                        dataCount = recordJson.optInt("dataCount", 0),
                        interval = recordJson.optInt("interval", 0),
                        createdAt = recordJson.optString("createdAt", "")
                    )

                    val dataPoints = mutableListOf<SensorDataPoint>()
                    for (i in 0 until dataArray.length()) {
                        val pointArray = dataArray.optJSONArray(i)
                        if (pointArray != null && pointArray.length() >= 3) {
                            dataPoints.add(
                                SensorDataPoint(
                                    timestamp = record.startTime + (i * record.interval),
                                    sensor1 = pointArray.optInt(0, 0),
                                    sensor2 = pointArray.optInt(1, 0),
                                    sensor3 = pointArray.optInt(2, 0)
                                )
                            )
                        }
                    }

                    SensorDataDetailResult.Success(record, dataPoints)
                } else {
                    SensorDataDetailResult.Error(json.optString("message", "获取详情失败"))
                }
            } catch (e: Exception) {
                SensorDataDetailResult.Error("解析响应失败: ${e.message}")
            }
        }

        /**
         * 解析服务器返回的压缩率字符串
         */
        private fun parseCompressionRatio(ratioStr: String?): Float {
            if (ratioStr.isNullOrEmpty() || ratioStr == "N/A") return 0f
            return try {
                val numberStr = ratioStr.replace("%", "").trim()
                val percentage = numberStr.toFloatOrNull() ?: 0f
                percentage / 100f
            } catch (e: Exception) {
                0f
            }
        }
    }
}

/**
 * 上传批次数据
 */
data class UploadBatch(
    val recordId: String,
    val startTime: Long,
    val endTime: Long,
    val interval: Int,
    val data: List<List<Int>>
)

/**
 * 批量上传结果
 */
sealed class BatchUploadResult {
    data class Success(
        val successCount: Int,
        val failCount: Int,
        val results: List<SensorDataUploadResult>
    ) : BatchUploadResult()

    data class PartialSuccess(
        val successCount: Int,
        val failCount: Int,
        val results: List<SensorDataUploadResult>
    ) : BatchUploadResult()
}

/**
 * 传感器数据上传结果
 */
sealed class SensorDataUploadResult {
    data class Success(
        val recordId: String,
        val dataCount: Int,
        val originalSize: Int,
        val compressedSize: Int,
        val compressionRatio: Float,
        val message: String
    ) : SensorDataUploadResult()

    data class Error(val message: String) : SensorDataUploadResult()
}

/**
 * 传感器数据记录列表结果
 */
sealed class SensorDataRecordsResult {
    data class Success(
        val records: List<SensorDataRecord>,
        val total: Long,
        val page: Int,
        val size: Int
    ) : SensorDataRecordsResult()

    data class Error(val message: String) : SensorDataRecordsResult()
}

/**
 * 通用操作结果
 */
sealed class SensorDataResult {
    data class Success(val message: String) : SensorDataResult()
    data class Error(val message: String) : SensorDataResult()
}

/**
 * 传感器数据记录
 */
data class SensorDataRecord(
    val recordId: String,
    val startTime: Long,
    val endTime: Long,
    val dataCount: Int,
    val interval: Int,
    val createdAt: String,
    val originalSize: Int = 0,
    val compressedSize: Int = 0,
    val compressionRatio: Float = 0f
)

/**
 * 传感器数据详情结果
 */
sealed class SensorDataDetailResult {
    data class Success(
        val record: SensorDataRecord,
        val dataPoints: List<SensorDataPoint>
    ) : SensorDataDetailResult()

    data class Error(val message: String) : SensorDataDetailResult()
}
