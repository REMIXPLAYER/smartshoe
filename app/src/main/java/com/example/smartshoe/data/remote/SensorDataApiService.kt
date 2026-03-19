package com.example.smartshoe.data.remote

import android.util.Log
import com.example.smartshoe.data.model.SensorDataPoint
import com.example.smartshoe.data.model.SensorDataRecord
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 传感器数据API服务
 * 提供上传、查询传感器数据的HTTP接口
 * 优化版本：支持请求合并、缓存和数据压缩
 *
 * 重构：使用 Hilt 依赖注入 RequestManager
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
}

/**
 * 传感器数据API服务实现
 * 使用 Hilt 注入 RequestManager
 */
@Singleton
class SensorDataApiServiceImpl @Inject constructor(
    private val requestManager: RequestManager
) : SensorDataApiService, BaseApiService() {

    companion object {
        private const val UPLOAD_PATH = "/sensor/upload"
        private const val RECORDS_PATH = "/sensor/records"
        private const val DELETE_PATH = "/sensor/record"
        private const val TAG = "SensorDataApiService"
    }

    override suspend fun uploadSensorData(
        recordId: String,
        startTime: Long,
        endTime: Long,
        interval: Int,
        data: List<List<Int>>,
        token: String,
        compress: Boolean
    ): SensorDataUploadResult {
        return try {
            // 构建JSON请求体
            val jsonBody = JSONObject().apply {
                put("recordId", recordId)
                put("startTime", startTime)
                put("endTime", endTime)
                put("interval", interval)
                put("data", JSONArray(data.map { JSONArray(it) }))
            }

            Log.d(TAG, "Uploading ${data.size} data points")

            val result = executePostJson(
                path = UPLOAD_PATH,
                token = token,
                jsonBody = jsonBody,
                useLongTimeout = true
            )

            // 上传成功后清除相关缓存
            if (result is ApiResult.Success) {
                requestManager.clearCache()
            }

            parseUploadResult(result)
        } catch (e: Exception) {
            Log.e(TAG, "Upload error: ${e.message}", e)
            SensorDataUploadResult.Error("网络错误: ${e.message}")
        }
    }

    override suspend fun uploadSensorDataBatch(
        batches: List<UploadBatch>,
        token: String,
        onProgress: ((Int, Int) -> Unit)?
    ): BatchUploadResult {
        if (batches.isEmpty()) {
            return BatchUploadResult.Success(0, 0, emptyList())
        }

        val totalBatches = batches.size
        val results = mutableListOf<SensorDataUploadResult>()
        var successCount = 0
        var failCount = 0

        batches.forEachIndexed { index, batch ->
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

        requestManager.clearCache()

        return if (failCount == 0) {
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
        val cacheKey = requestManager.generateCacheKey("records", mapOf("page" to page, "size" to size))
        return requestManager.executeWithCacheAndDedup(
            cacheKey = cacheKey,
            useCache = useCache,
            cacheDurationMs = 60 * 1000
        ) {
            fetchUserRecords(token, page, size)
        }
    }

    private suspend fun fetchUserRecords(
        token: String,
        page: Int,
        size: Int
    ): SensorDataRecordsResult {
        val result = executeGet(
            path = RECORDS_PATH,
            token = token,
            params = mapOf("page" to page, "size" to size)
        )
        return parseRecordsResult(result)
    }

    override suspend fun deleteRecord(recordId: String, token: String): SensorDataResult {
        val result = executeDelete(
            path = "$DELETE_PATH/$recordId",
            token = token
        )

        requestManager.clearCache()
        return parseResult(result)
    }

    override suspend fun getRecordsByTimeRange(
        token: String,
        startTime: Long,
        endTime: Long,
        page: Int,
        size: Int,
        useCache: Boolean
    ): SensorDataRecordsResult {
        val cacheKey = requestManager.generateCacheKey(
            "records_range",
            mapOf("start" to startTime, "end" to endTime, "page" to page, "size" to size)
        )
        return requestManager.executeWithCacheAndDedup(
            cacheKey = cacheKey,
            useCache = useCache,
            cacheDurationMs = 2 * 60 * 1000
        ) {
            fetchRecordsByTimeRange(token, startTime, endTime, page, size)
        }
    }

    private suspend fun fetchRecordsByTimeRange(
        token: String,
        startTime: Long,
        endTime: Long,
        page: Int,
        size: Int
    ): SensorDataRecordsResult {
        val result = executeGet(
            path = "$RECORDS_PATH/range",
            token = token,
            params = mapOf(
                "startTime" to startTime,
                "endTime" to endTime,
                "page" to page,
                "size" to size
            )
        )
        return parseRecordsResult(result)
    }

    override suspend fun getRecordDetail(
        recordId: String,
        token: String,
        useCache: Boolean
    ): SensorDataDetailResult {
        val cacheKey = requestManager.generateCacheKey("record_detail", mapOf("id" to recordId))
        return requestManager.executeWithCacheAndDedup(
            cacheKey = cacheKey,
            useCache = useCache,
            cacheDurationMs = 5 * 60 * 1000
        ) {
            fetchRecordDetail(recordId, token)
        }
    }

    private suspend fun fetchRecordDetail(
        recordId: String,
        token: String
    ): SensorDataDetailResult {
        val result = executeGet(
            path = "$RECORDS_PATH/$recordId/detail",
            token = token
        )
        return parseDetailResult(result)
    }

    // ==================== 解析方法 ====================

    private fun parseUploadResult(result: ApiResult<String>): SensorDataUploadResult {
        return when (result) {
            is ApiResult.Success -> parseUploadResponse(result.data)
            is ApiResult.Error -> {
                // 检查是否是401错误，如果是则抛出UnauthorizedException
                if (result.exception is NetworkException.UnauthorizedException) {
                    throw result.exception
                }
                SensorDataUploadResult.Error(
                    result.exception.message ?: "网络错误"
                )
            }
        }
    }

    private fun parseUploadResponse(response: String): SensorDataUploadResult {
        return try {
            val json = JSONObject(response)
            val success = json.optBoolean("success", false)

            if (success) {
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

    private fun parseRecordsResult(result: ApiResult<String>): SensorDataRecordsResult {
        return when (result) {
            is ApiResult.Success -> parseRecordsResponse(result.data)
            is ApiResult.Error -> {
                // 如果是401错误，清除缓存
                if (result.exception is NetworkException.UnauthorizedException) {
                    Log.w(TAG, "Token过期(401)，清除API缓存")
                    requestManager.clearCache()
                }
                SensorDataRecordsResult.Error(
                    result.exception.message ?: "网络错误"
                )
            }
        }
    }

    private fun parseRecordsResponse(response: String): SensorDataRecordsResult {
        return try {
            val json = JSONObject(response)
            val success = json.optBoolean("success", false)

            if (success) {
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
                            compressionRatio = parseCompressionRatio(
                                item?.optString("compressionRatio", "0%")
                            )
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

    private fun parseResult(result: ApiResult<String>): SensorDataResult {
        return when (result) {
            is ApiResult.Success -> parseResultResponse(result.data)
            is ApiResult.Error -> {
                // 如果是401错误，清除缓存
                if (result.exception is NetworkException.UnauthorizedException) {
                    Log.w(TAG, "Token过期(401)，清除API缓存")
                    requestManager.clearCache()
                }
                SensorDataResult.Error(
                    result.exception.message ?: "网络错误"
                )
            }
        }
    }

    private fun parseResultResponse(response: String): SensorDataResult {
        return try {
            val json = JSONObject(response)
            val success = json.optBoolean("success", false)

            if (success) {
                SensorDataResult.Success(json.optString("message", "操作成功"))
            } else {
                SensorDataResult.Error(json.optString("message", "操作失败"))
            }
        } catch (e: Exception) {
            SensorDataResult.Error("解析响应失败: ${e.message}")
        }
    }

    private fun parseDetailResult(result: ApiResult<String>): SensorDataDetailResult {
        return when (result) {
            is ApiResult.Success -> parseDetailResponse(result.data)
            is ApiResult.Error -> {
                // 如果是401错误，清除缓存
                if (result.exception is NetworkException.UnauthorizedException) {
                    Log.w(TAG, "Token过期(401)，清除API缓存")
                    requestManager.clearCache()
                }
                SensorDataDetailResult.Error(
                    result.exception.message ?: "网络错误"
                )
            }
        }
    }

    private fun parseDetailResponse(response: String): SensorDataDetailResult {
        return try {
            val json = JSONObject(response)
            val success = json.optBoolean("success", false)

            if (success) {
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

// ==================== 数据类定义 ====================

data class UploadBatch(
    val recordId: String,
    val startTime: Long,
    val endTime: Long,
    val interval: Int,
    val data: List<List<Int>>
)

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

sealed class SensorDataRecordsResult {
    data class Success(
        val records: List<SensorDataRecord>,
        val total: Long,
        val page: Int,
        val size: Int
    ) : SensorDataRecordsResult()

    data class Error(val message: String) : SensorDataRecordsResult()
}

sealed class SensorDataResult {
    data class Success(val message: String) : SensorDataResult()
    data class Error(val message: String) : SensorDataResult()
}

sealed class SensorDataDetailResult {
    data class Success(
        val record: SensorDataRecord,
        val dataPoints: List<SensorDataPoint>
    ) : SensorDataDetailResult()

    data class Error(val message: String) : SensorDataDetailResult()
}
