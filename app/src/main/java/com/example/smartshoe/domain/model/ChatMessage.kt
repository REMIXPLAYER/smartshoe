package com.example.smartshoe.domain.model

/**
 * 对话消息
 * 领域层模型，表示AI对话中的消息类型
 */
sealed class ChatMessage {
    abstract val content: String
    abstract val timestamp: Long

    data class User(
        override val content: String,
        override val timestamp: Long = System.currentTimeMillis()
    ) : ChatMessage()

    data class Ai(
        override val content: String,
        val model: String,
        val generationTimeMs: Long = 0,
        override val timestamp: Long = System.currentTimeMillis()
    ) : ChatMessage()

    data class HealthAdvice(
        override val content: String,
        val summary: HealthAdviceSummary?,
        val model: String,
        val generationTimeMs: Long = 0,
        override val timestamp: Long = System.currentTimeMillis()
    ) : ChatMessage()

    data class StreamingAi(
        override val content: String,
        val model: String,
        override val timestamp: Long = System.currentTimeMillis()
    ) : ChatMessage()
}
