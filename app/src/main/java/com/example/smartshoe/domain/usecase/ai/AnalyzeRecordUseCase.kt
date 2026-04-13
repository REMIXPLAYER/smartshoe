package com.example.smartshoe.domain.usecase.ai

import com.example.smartshoe.domain.model.SseConnectionState
import com.example.smartshoe.domain.model.SseEvent
import com.example.smartshoe.domain.repository.AiAssistantRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 分析传感器记录并获取 AI 建议（流式版本）
 * 
 * 职责：封装分析记录的业务逻辑
 * 
 * 注意：使用领域层类型，符合 Clean Architecture
 */
@Singleton
class AnalyzeRecordUseCase @Inject constructor(
    private val aiAssistantRepository: AiAssistantRepository
) {

    /**
     * 分析传感器记录并接收流式响应
     * 
     * @param recordId 传感器记录 ID
     * @param token 用户 Token
     * @param enableThinking 是否启用深度思考模式
     * @param onStateChange SSE 连接状态变化回调
     * @return 流式事件 Flow
     */
    operator fun invoke(
        recordId: String,
        token: String,
        enableThinking: Boolean = false,
        onStateChange: (SseConnectionState) -> Unit = {}
    ): Flow<SseEvent> {
        return aiAssistantRepository.analyzeRecordStream(
            recordId = recordId,
            token = token,
            enableThinking = enableThinking,
            onStateChange = onStateChange
        )
    }
}
