package com.example.smartshoe.domain.model

/**
 * SSE 事件密封类（领域层）
 * 
 * 职责：定义 SSE 流式传输的事件类型
 * 这是纯领域层模型，不依赖任何框架或数据层
 */
sealed class SseEvent {
    /**
     * 数据事件 - 包含 AI 生成的文本片段
     */
    data class Data(val content: String) : SseEvent()

    /**
     * 完成事件 - AI 生成完成
     */
    data class Complete(
        val model: String,
        val duration: Long
    ) : SseEvent()

    /**
     * 错误事件
     */
    data class Error(val message: String) : SseEvent()
}

/**
 * SSE 连接状态密封类（领域层）
 * 
 * 职责：定义 SSE 连接的各种状态
 * 这是纯领域层模型，不依赖任何框架或数据层
 */
sealed class SseConnectionState {
    /**
     * 空闲状态
     */
    object Idle : SseConnectionState()

    /**
     * 连接中
     */
    object Connecting : SseConnectionState()

    /**
     * 已连接
     */
    object Connected : SseConnectionState()

    /**
     * 连接错误
     */
    data class Error(val message: String) : SseConnectionState()

    /**
     * 连接已关闭
     */
    object Closed : SseConnectionState()
}
