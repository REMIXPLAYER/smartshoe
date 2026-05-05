package com.example.smartshoe.domain.repository

import com.example.smartshoe.domain.model.AiConversation
import com.example.smartshoe.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow

/**
 * AI对话仓库接口
 * 符合Clean Architecture，定义领域层需要的操作
 */
interface AiConversationRepository {

    /**
     * 获取所有对话，按更新时间倒序
     */
    fun getAllConversations(): Flow<List<AiConversation>>

    /**
     * 根据关键词搜索对话
     */
    fun searchConversations(keyword: String): Flow<List<AiConversation>>

    /**
     * 获取指定对话的消息列表
     */
    fun getMessagesByConversationId(conversationId: String): Flow<List<ChatMessage>>

    /**
     * 创建新对话
     */
    suspend fun createConversation(title: String): String

    /**
     * 保存消息到指定对话
     */
    suspend fun saveMessage(conversationId: String, message: ChatMessage)

    /**
     * 保存多条消息
     */
    suspend fun saveMessages(conversationId: String, messages: List<ChatMessage>)

    /**
     * 更新对话标题
     */
    suspend fun updateConversationTitle(conversationId: String, title: String)

    /**
     * 更新对话时间
     */
    suspend fun updateConversationTime(conversationId: String)

    /**
     * 删除对话
     */
    suspend fun deleteConversation(conversationId: String)

    /**
     * 更新最后阅读位置
     */
    suspend fun updateLastReadPosition(conversationId: String, position: Float)

    /**
     * 删除所有对话
     */
    suspend fun deleteAllConversations()
}
