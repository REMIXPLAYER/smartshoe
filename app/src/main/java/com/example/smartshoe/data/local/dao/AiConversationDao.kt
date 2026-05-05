package com.example.smartshoe.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.smartshoe.data.local.entity.AiConversationEntity
import kotlinx.coroutines.flow.Flow

/**
 * AI对话DAO
 */
@Dao
interface AiConversationDao {

    @Query("SELECT * FROM ai_conversations ORDER BY updatedAt DESC")
    fun getAllConversations(): Flow<List<AiConversationEntity>>

    @Query("SELECT * FROM ai_conversations WHERE title LIKE '%' || :keyword || '%' ORDER BY updatedAt DESC")
    fun searchConversations(keyword: String): Flow<List<AiConversationEntity>>

    @Query("SELECT * FROM ai_conversations WHERE id = :conversationId")
    suspend fun getConversationById(conversationId: String): AiConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: AiConversationEntity)

    @Update
    suspend fun updateConversation(conversation: AiConversationEntity)

    @Delete
    suspend fun deleteConversation(conversation: AiConversationEntity)

    @Query("DELETE FROM ai_conversations WHERE id = :conversationId")
    suspend fun deleteConversationById(conversationId: String)

    @Query("DELETE FROM ai_conversations")
    suspend fun deleteAllConversations()

    @Query("UPDATE ai_conversations SET messageCount = (SELECT COUNT(*) FROM ai_messages WHERE conversationId = :conversationId) WHERE id = :conversationId")
    suspend fun updateMessageCount(conversationId: String)

    @Query("UPDATE ai_conversations SET lastReadPosition = :position WHERE id = :conversationId")
    suspend fun updateLastReadPosition(conversationId: String, position: Float)
}
