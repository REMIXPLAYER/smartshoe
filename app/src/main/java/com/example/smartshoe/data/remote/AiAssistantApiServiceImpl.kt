package com.example.smartshoe.data.remote

import okhttp3.OkHttpClient
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI助手API服务实现
 */
@Singleton
class AiAssistantApiServiceImpl @Inject constructor(
    private val client: OkHttpClient
) : BaseApiService(), AiAssistantApiService {

    companion object {
        private const val CHAT_PATH = "/ai/chat"
        private const val HEALTH_ADVICE_PATH = "/health/advice"
        private const val ANALYSIS_PATH = "/analysis/foot-pressure"
        private const val STATUS_PATH = "/health/status"
    }

    override fun getOkHttpClient(): OkHttpClient = client

    override suspend fun sendMessage(
        message: String,
        enableThinking: Boolean,
        token: String
    ): AiChatResult {
        return try {
            val jsonBody = JSONObject().apply {
                put("message", message)
                put("enableThinking", enableThinking)
            }

            val result = executePostJson(
                path = CHAT_PATH,
                token = token,
                jsonBody = jsonBody,
                useLongTimeout = true
            )

            parseChatResult(result)
        } catch (e: Exception) {
            AiChatResult.Error("请求失败: ${e.message}")
        }
    }

    override suspend fun getHealthAdvice(
        recordId: String,
        userAge: Int?,
        userWeight: Double?,
        token: String
    ): HealthAdviceResult {
        return try {
            val jsonBody = JSONObject().apply {
                put("recordId", recordId)
                userAge?.let { put("userAge", it) }
                userWeight?.let { put("userWeight", it) }
            }

            val result = executePostJson(
                path = HEALTH_ADVICE_PATH,
                token = token,
                jsonBody = jsonBody,
                useLongTimeout = true
            )

            parseHealthAdviceResult(result)
        } catch (e: Exception) {
            HealthAdviceResult.Error("请求失败: ${e.message}")
        }
    }

    override suspend fun analyzeFootPressure(
        recordId: String,
        token: String,
        enableThinking: Boolean
    ): FootPressureAnalysisResult {
        return try {
            val jsonBody = JSONObject().apply {
                put("recordId", recordId)
                put("enableThinking", enableThinking)
            }

            val result = executePostJson(
                path = ANALYSIS_PATH,
                token = token,
                jsonBody = jsonBody,
                useLongTimeout = true
            )

            parseAnalysisResult(result)
        } catch (e: Exception) {
            FootPressureAnalysisResult.Error("请求失败: ${e.message}")
        }
    }

    override suspend fun checkAiStatus(): AiStatusResult {
        return try {
            val result = executeGet(
                path = STATUS_PATH
            )

            parseStatusResult(result)
        } catch (e: Exception) {
            AiStatusResult.Error("请求失败: ${e.message}")
        }
    }

    private fun parseChatResult(result: ApiResult<String>): AiChatResult {
        return when (result) {
            is ApiResult.Success -> parseChatResponse(result.data)
            is ApiResult.Error -> AiChatResult.Error(result.exception.message ?: "网络错误")
        }
    }

    private fun parseChatResponse(response: String): AiChatResult {
        return try {
            val json = JSONObject(response)
            val success = json.optBoolean("success", false)

            if (success) {
                // 服务器使用ApiResult包装响应，实际数据在data字段内
                val dataJson = json.optJSONObject("data") ?: JSONObject()
                AiChatResult.Success(
                    reply = dataJson.optString("reply", ""),
                    model = dataJson.optString("model", "unknown"),
                    generationTimeMs = dataJson.optLong("generationTimeMs", 0)
                )
            } else {
                AiChatResult.Error(json.optString("message", "对话失败"))
            }
        } catch (e: Exception) {
            AiChatResult.Error("解析响应失败: ${e.message}")
        }
    }

    private fun parseHealthAdviceResult(result: ApiResult<String>): HealthAdviceResult {
        return when (result) {
            is ApiResult.Success -> parseHealthAdviceResponse(result.data)
            is ApiResult.Error -> HealthAdviceResult.Error(result.exception.message ?: "网络错误")
        }
    }

    private fun parseHealthAdviceResponse(response: String): HealthAdviceResult {
        return try {
            val json = JSONObject(response)
            val success = json.optBoolean("success", false)

            if (success) {
                // 服务器使用ApiResult包装响应，实际数据在data字段内
                val dataJson = json.optJSONObject("data") ?: JSONObject()
                val summaryJson = dataJson.optJSONObject("summary")
                val summary = summaryJson?.let {
                    HealthAdviceSummary(
                        dataPoints = it.optInt("dataPoints", 0),
                        averagePressure = it.optDouble("averagePressure", 0.0),
                        maxPressure = it.optInt("maxPressure", 0),
                        minPressure = it.optInt("minPressure", 0),
                        pressureBalanced = it.optBoolean("pressureBalanced", false),
                        anomalyCount = it.optInt("anomalyCount", 0)
                    )
                }

                HealthAdviceResult.Success(
                    advice = dataJson.optString("advice", ""),
                    summary = summary,
                    model = dataJson.optString("model", "unknown"),
                    generationTimeMs = dataJson.optLong("generationTimeMs", 0)
                )
            } else {
                HealthAdviceResult.Error(json.optString("message", "获取健康建议失败"))
            }
        } catch (e: Exception) {
            HealthAdviceResult.Error("解析响应失败: ${e.message}")
        }
    }

    private fun parseAnalysisResult(result: ApiResult<String>): FootPressureAnalysisResult {
        return when (result) {
            is ApiResult.Success -> parseAnalysisResponse(result.data)
            is ApiResult.Error -> FootPressureAnalysisResult.Error(result.exception.message ?: "网络错误")
        }
    }

    private fun parseAnalysisResponse(response: String): FootPressureAnalysisResult {
        return try {
            val json = JSONObject(response)
            val success = json.optBoolean("success", false)

            if (success) {
                // 服务器使用ApiResult包装响应，实际数据在data字段内
                val dataJson = json.optJSONObject("data") ?: JSONObject()
                FootPressureAnalysisResult.Success(
                    advice = dataJson.optString("reply", ""),
                    model = dataJson.optString("model", "unknown"),
                    generationTimeMs = dataJson.optLong("generationTimeMs", 0)
                )
            } else {
                FootPressureAnalysisResult.Error(json.optString("message", "分析失败"))
            }
        } catch (e: Exception) {
            FootPressureAnalysisResult.Error("解析响应失败: ${e.message}")
        }
    }

    private fun parseStatusResult(result: ApiResult<String>): AiStatusResult {
        return when (result) {
            is ApiResult.Success -> parseStatusResponse(result.data)
            is ApiResult.Error -> AiStatusResult.Error(result.exception.message ?: "网络错误")
        }
    }

    private fun parseStatusResponse(response: String): AiStatusResult {
        return try {
            val json = JSONObject(response)
            val success = json.optBoolean("success", false)

            if (success) {
                // 服务器使用ApiResult包装响应，实际数据在data字段内
                val dataJson = json.optJSONObject("data") ?: JSONObject()
                AiStatusResult.Success(
                    isAvailable = dataJson.optBoolean("aiServiceAvailable", false),
                    model = dataJson.optString("model", "unknown")
                )
            } else {
                AiStatusResult.Error(json.optString("message", "检查状态失败"))
            }
        } catch (e: Exception) {
            AiStatusResult.Error("解析响应失败: ${e.message}")
        }
    }
}
