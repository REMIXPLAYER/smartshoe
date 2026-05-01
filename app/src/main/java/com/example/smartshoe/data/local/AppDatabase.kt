package com.example.smartshoe.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.smartshoe.data.local.dao.AiConversationDao
import com.example.smartshoe.data.local.dao.AiMessageDao
import com.example.smartshoe.data.local.entity.AiConversationEntity
import com.example.smartshoe.data.local.entity.AiMessageEntity

/**
 * 应用数据库
 * 包含AI对话和消息表
 */
@Database(
    entities = [
        AiConversationEntity::class,
        AiMessageEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun aiConversationDao(): AiConversationDao
    abstract fun aiMessageDao(): AiMessageDao

    companion object {
        const val DATABASE_NAME = "smartshoe_database"
    }
}
