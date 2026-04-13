package com.example.smartshoe.data.remote

/**
 * AI助手相关的数据模型
 */

sealed class AiChatResult {
    data class Success(
        val reply: String,
        val model: String,
        val generationTimeMs: Long
    ) : AiChatResult()

    data class Error(val message: String) : AiChatResult()
}

sealed class HealthAdviceResult {
    data class Success(
        val advice: String,
        val summary: HealthAdviceSummary?,
        val model: String,
        val generationTimeMs: Long
    ) : HealthAdviceResult()

    data class Error(val message: String) : HealthAdviceResult()
}

data class HealthAdviceSummary(
    val dataPoints: Int,
    val averagePressure: Double,
    val maxPressure: Int,
    val minPressure: Int,
    val pressureBalanced: Boolean,
    val anomalyCount: Int
)

sealed class FootPressureAnalysisResult {
    data class Success(
        val advice: String,
        val model: String,
        val generationTimeMs: Long
    ) : FootPressureAnalysisResult()

    data class Error(val message: String) : FootPressureAnalysisResult()
}

sealed class AiStatusResult {
    data class Success(
        val isAvailable: Boolean,
        val model: String
    ) : AiStatusResult()

    data class Error(val message: String) : AiStatusResult()
}
