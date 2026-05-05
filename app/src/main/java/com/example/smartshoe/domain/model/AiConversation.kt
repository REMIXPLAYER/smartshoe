package com.example.smartshoe.domain.model

/**
 * AI对话领域模型
 */
data class AiConversation(
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val messageCount: Int = 0,
    val lastReadPosition: Float = -1f
)
