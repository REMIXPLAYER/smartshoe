package com.example.smartshoe.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * AI消息实体
 * 存储对话中的每条消息
 */
@Entity(
    tableName = "ai_messages",
    foreignKeys = [
        ForeignKey(
            entity = AiConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("conversationId")]
)
data class AiMessageEntity(
    @PrimaryKey
    val id: String,
    val conversationId: String,
    val type: String, // USER, AI, HEALTH_ADVICE, STREAMING_AI
    val content: String,
    val model: String? = null,
    val generationTimeMs: Long = 0,
    val timestamp: Long
)
