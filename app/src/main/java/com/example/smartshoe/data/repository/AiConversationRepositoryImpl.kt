package com.example.smartshoe.data.repository

import com.example.smartshoe.data.local.dao.AiConversationDao
import com.example.smartshoe.data.local.dao.AiMessageDao
import com.example.smartshoe.data.local.entity.AiConversationEntity
import com.example.smartshoe.data.local.entity.AiMessageEntity
import com.example.smartshoe.domain.model.AiConversation
import com.example.smartshoe.domain.model.ChatMessage
import com.example.smartshoe.domain.repository.AiConversationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI对话仓库实现
 */
@Singleton
class AiConversationRepositoryImpl @Inject constructor(
    private val conversationDao: AiConversationDao,
    private val messageDao: AiMessageDao
) : AiConversationRepository {

    override fun getAllConversations(): Flow<List<AiConversation>> {
        return conversationDao.getAllConversations().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun searchConversations(keyword: String): Flow<List<AiConversation>> {
        return conversationDao.searchConversations(keyword).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getMessagesByConversationId(conversationId: String): Flow<List<ChatMessage>> {
        return messageDao.getMessagesByConversationId(conversationId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun createConversation(title: String): String {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val entity = AiConversationEntity(
            id = id,
            title = title,
            createdAt = now,
            updatedAt = now,
            messageCount = 0
        )
        conversationDao.insertConversation(entity)
        return id
    }

    override suspend fun saveMessage(conversationId: String, message: ChatMessage) {
        val entity = message.toEntity(conversationId)
        messageDao.insertMessage(entity)
        conversationDao.updateMessageCount(conversationId)
    }

    override suspend fun saveMessages(conversationId: String, messages: List<ChatMessage>) {
        val entities = messages.map { it.toEntity(conversationId) }
        messageDao.insertMessages(entities)
        conversationDao.updateMessageCount(conversationId)
    }

    override suspend fun updateConversationTitle(conversationId: String, title: String) {
        val conversation = conversationDao.getConversationById(conversationId)
        conversation?.let {
            conversationDao.updateConversation(
                it.copy(title = title, updatedAt = System.currentTimeMillis())
            )
        }
    }

    override suspend fun updateConversationTime(conversationId: String) {
        val conversation = conversationDao.getConversationById(conversationId)
        conversation?.let {
            conversationDao.updateConversation(
                it.copy(updatedAt = System.currentTimeMillis())
            )
        }
    }

    override suspend fun updateLastReadPosition(conversationId: String, position: Float) {
        conversationDao.updateLastReadPosition(conversationId, position)
    }

    override suspend fun deleteConversation(conversationId: String) {
        conversationDao.deleteConversationById(conversationId)
        messageDao.deleteMessagesByConversationId(conversationId)
    }

    override suspend fun deleteAllConversations() {
        messageDao.deleteAllMessages()
        conversationDao.deleteAllConversations()
    }

    // ==================== 转换方法 ====================

    private fun AiConversationEntity.toDomainModel(): AiConversation {
        return AiConversation(
            id = id,
            title = title,
            createdAt = createdAt,
            updatedAt = updatedAt,
            messageCount = messageCount,
            lastReadPosition = lastReadPosition
        )
    }

    private fun ChatMessage.toEntity(conversationId: String): AiMessageEntity {
        return when (this) {
            is ChatMessage.User -> AiMessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = conversationId,
                type = "USER",
                content = content,
                timestamp = timestamp
            )
            is ChatMessage.Ai -> AiMessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = conversationId,
                type = "AI",
                content = content,
                model = model,
                generationTimeMs = generationTimeMs,
                timestamp = timestamp
            )
            is ChatMessage.HealthAdvice -> AiMessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = conversationId,
                type = "HEALTH_ADVICE",
                content = content,
                model = model,
                generationTimeMs = generationTimeMs,
                timestamp = timestamp
            )
            is ChatMessage.StreamingAi -> AiMessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = conversationId,
                type = "STREAMING_AI",
                content = content,
                model = model,
                timestamp = timestamp
            )
        }
    }

    private fun AiMessageEntity.toDomainModel(): ChatMessage {
        return when (type) {
            "USER" -> ChatMessage.User(content = content, timestamp = timestamp)
            "AI" -> ChatMessage.Ai(
                content = content,
                model = model ?: "",
                generationTimeMs = generationTimeMs,
                timestamp = timestamp
            )
            "HEALTH_ADVICE" -> ChatMessage.HealthAdvice(
                content = content,
                summary = null,
                model = model ?: "",
                generationTimeMs = generationTimeMs,
                timestamp = timestamp
            )
            "STREAMING_AI" -> ChatMessage.StreamingAi(
                content = content,
                model = model ?: "",
                timestamp = timestamp
            )
            else -> ChatMessage.User(content = content, timestamp = timestamp)
        }
    }
}
