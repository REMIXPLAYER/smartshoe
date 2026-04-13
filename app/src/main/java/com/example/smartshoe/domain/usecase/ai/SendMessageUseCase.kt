package com.example.smartshoe.domain.usecase.ai

import com.example.smartshoe.domain.model.SseConnectionState
import com.example.smartshoe.domain.model.SseEvent
import com.example.smartshoe.domain.repository.AiAssistantRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 发送消息给 AI（流式版本）
 * 
 * 职责：封装发送消息的业务逻辑，供 ViewModel 调用
 * 
 * 注意：使用领域层类型，符合 Clean Architecture
 */
@Singleton
class SendMessageUseCase @Inject constructor(
    private val aiAssistantRepository: AiAssistantRepository
) {

    /**
     * 发送消息并接收流式响应
     * 
     * @param message 用户消息内容
     * @param token 用户 Token
     * @param enableThinking 是否启用深度思考模式
     * @param onStateChange SSE 连接状态变化回调
     * @return 流式事件 Flow
     */
    operator fun invoke(
        message: String,
        token: String,
        enableThinking: Boolean = false,
        onStateChange: (SseConnectionState) -> Unit = {}
    ): Flow<SseEvent> {
        return aiAssistantRepository.sendMessageStream(
            message = message,
            token = token,
            enableThinking = enableThinking,
            onStateChange = onStateChange
        )
    }
}
