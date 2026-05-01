package com.example.smartshoe.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * AI对话实体
 * 存储对话的基本信息
 */
@Entity(tableName = "ai_conversations")
data class AiConversationEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val messageCount: Int = 0
)
