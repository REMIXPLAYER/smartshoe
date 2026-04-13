package com.example.smartshoe.data.repository

import com.example.smartshoe.data.remote.AiAssistantApiService
import com.example.smartshoe.data.remote.SseClient
import com.example.smartshoe.domain.model.AiStatusResult
import com.example.smartshoe.domain.model.HealthAdviceResult
import com.example.smartshoe.domain.model.SseConnectionState
import com.example.smartshoe.domain.model.SseEvent
import com.example.smartshoe.domain.repository.AiAssistantRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI 助手仓库实现
 * 
 * 职责：实现 AiAssistantRepository 接口，协调数据源
 * - 远程：AiAssistantApiService (HTTP API)
 * - SSE: SseClient (流式传输)
 * 
 * 注意：进行数据层类型到领域层类型的转换
 */
@Singleton
class AiAssistantRepositoryImpl @Inject constructor(
    private val aiAssistantApiService: AiAssistantApiService,
    private val sseClient: SseClient
) : AiAssistantRepository {

    override suspend fun checkAiStatus(): AiStatusResult {
        return when (val result = aiAssistantApiService.checkAiStatus()) {
            is com.example.smartshoe.data.remote.AiStatusResult.Success -> {
                AiStatusResult.Success(result.isAvailable, result.model)
            }
            is com.example.smartshoe.data.remote.AiStatusResult.Error -> {
                AiStatusResult.Error(result.message)
            }
        }
    }

    override suspend fun getHealthAdvice(
        recordId: String,
        token: String,
        userAge: Int?
    ): HealthAdviceResult {
        return when (val result = aiAssistantApiService.getHealthAdvice(recordId, userAge, null, token)) {
            is com.example.smartshoe.data.remote.HealthAdviceResult.Success -> {
                HealthAdviceResult.Success(
                    advice = result.advice,
                    summary = result.summary?.let {
                        com.example.smartshoe.domain.model.HealthAdviceSummary(
                            dataPoints = it.dataPoints,
                            averagePressure = it.averagePressure,
                            maxPressure = it.maxPressure,
                            minPressure = it.minPressure,
                            pressureBalanced = it.pressureBalanced,
                            anomalyCount = it.anomalyCount
                        )
                    },
                    model = result.model,
                    generationTimeMs = result.generationTimeMs
                )
            }
            is com.example.smartshoe.data.remote.HealthAdviceResult.Error -> {
                HealthAdviceResult.Error(result.message)
            }
        }
    }

    override fun sendMessageStream(
        message: String,
        token: String,
        enableThinking: Boolean,
        onStateChange: (SseConnectionState) -> Unit
    ): Flow<SseEvent> {
        return sseClient.chatStream(
            message = message,
            token = token,
            enableThinking = enableThinking,
            onStateChange = { dataState ->
                // 转换数据层状态到领域层状态
                onStateChange(convertToDomainState(dataState))
            }
        ).map { dataEvent ->
            // 转换数据层事件到领域层事件
            convertToDomainEvent(dataEvent)
        }
    }

    override fun analyzeRecordStream(
        recordId: String,
        token: String,
        enableThinking: Boolean,
        onStateChange: (SseConnectionState) -> Unit
    ): Flow<SseEvent> {
        return sseClient.analysisStream(
            recordId = recordId,
            token = token,
            enableThinking = enableThinking,
            onStateChange = { dataState ->
                // 转换数据层状态到领域层状态
                onStateChange(convertToDomainState(dataState))
            }
        ).map { dataEvent ->
            // 转换数据层事件到领域层事件
            convertToDomainEvent(dataEvent)
        }
    }

    /**
     * 转换数据层 SSE 状态到领域层状态
     */
    private fun convertToDomainState(dataState: com.example.smartshoe.data.remote.SseConnectionState): SseConnectionState {
        return when (dataState) {
            is com.example.smartshoe.data.remote.SseConnectionState.Idle -> SseConnectionState.Idle
            is com.example.smartshoe.data.remote.SseConnectionState.Connecting -> SseConnectionState.Connecting
            is com.example.smartshoe.data.remote.SseConnectionState.Connected -> SseConnectionState.Connected
            is com.example.smartshoe.data.remote.SseConnectionState.Error -> SseConnectionState.Error(dataState.message)
            is com.example.smartshoe.data.remote.SseConnectionState.Closed -> SseConnectionState.Closed
        }
    }

    /**
     * 转换数据层 SSE 事件到领域层事件
     */
    private fun convertToDomainEvent(dataEvent: com.example.smartshoe.data.remote.SseEvent): SseEvent {
        return when (dataEvent) {
            is com.example.smartshoe.data.remote.SseEvent.Data -> SseEvent.Data(dataEvent.content)
            is com.example.smartshoe.data.remote.SseEvent.Complete -> SseEvent.Complete(dataEvent.model, dataEvent.duration)
            is com.example.smartshoe.data.remote.SseEvent.Error -> SseEvent.Error(dataEvent.message)
        }
    }
}
