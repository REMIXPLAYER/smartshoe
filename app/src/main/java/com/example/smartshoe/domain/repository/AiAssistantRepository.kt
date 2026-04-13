package com.example.smartshoe.domain.repository

import com.example.smartshoe.domain.model.AiStatusResult
import com.example.smartshoe.domain.model.HealthAdviceResult
import com.example.smartshoe.domain.model.SseConnectionState
import com.example.smartshoe.domain.model.SseEvent
import kotlinx.coroutines.flow.Flow

/**
 * AI 助手仓库接口
 * 
 * 职责：定义 AI 助手相关的数据操作
 * 这是领域层接口，只使用领域层类型，不依赖任何数据层实现细节
 */
interface AiAssistantRepository {

    /**
     * 检查 AI 服务状态
     * 
     * @return AI 服务状态结果
     */
    suspend fun checkAiStatus(): AiStatusResult

    /**
     * 获取健康建议
     * 
     * @param recordId 传感器记录 ID
     * @param token 用户 Token
     * @param userAge 用户年龄（可选）
     * @return 健康建议结果
     */
    suspend fun getHealthAdvice(
        recordId: String,
        token: String,
        userAge: Int? = null
    ): HealthAdviceResult

    /**
     * 发送消息并接收流式响应
     * 
     * @param message 用户消息内容
     * @param token 用户 Token
     * @param enableThinking 是否启用深度思考模式
     * @param onStateChange SSE 连接状态变化回调
     * @return 流式事件 Flow
     */
    fun sendMessageStream(
        message: String,
        token: String,
        enableThinking: Boolean = false,
        onStateChange: (SseConnectionState) -> Unit = {}
    ): Flow<SseEvent>

    /**
     * 分析传感器记录并接收流式响应
     * 
     * @param recordId 传感器记录 ID
     * @param token 用户 Token
     * @param enableThinking 是否启用深度思考模式
     * @param onStateChange SSE 连接状态变化回调
     * @return 流式事件 Flow
     */
    fun analyzeRecordStream(
        recordId: String,
        token: String,
        enableThinking: Boolean = false,
        onStateChange: (SseConnectionState) -> Unit = {}
    ): Flow<SseEvent>
}
