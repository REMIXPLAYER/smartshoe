package com.example.smartshoe.data.remote

/**
 * AI助手API服务接口
 * 提供与AI模型对话的HTTP接口
 */
interface AiAssistantApiService {

    /**
     * 发送消息给AI并获取回复（同步版本）
     * @param message 用户消息
     * @param enableThinking 是否启用深度思考模式
     * @param token 用户认证令牌
     * @return AI回复结果
     */
    suspend fun sendMessage(
        message: String,
        enableThinking: Boolean = false,
        token: String
    ): AiChatResult

    /**
     * 获取健康建议（基于历史记录）
     * @param recordId 历史记录ID
     * @param userAge 用户年龄（可选）
     * @param userWeight 用户体重（可选）
     * @param token 用户认证令牌
     * @return 健康建议结果
     */
    suspend fun getHealthAdvice(
        recordId: String,
        userAge: Int? = null,
        userWeight: Double? = null,
        token: String
    ): HealthAdviceResult

    /**
     * 分析足部压力数据
     * @param recordId 记录ID
     * @param token 用户认证令牌
     * @param enableThinking 是否启用深度思考模式
     * @return 分析结果
     */
    suspend fun analyzeFootPressure(
        recordId: String,
        token: String,
        enableThinking: Boolean = false
    ): FootPressureAnalysisResult

    /**
     * 检查AI服务状态
     * @return 服务状态结果
     */
    suspend fun checkAiStatus(): AiStatusResult
}
