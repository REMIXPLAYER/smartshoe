package com.example.smartshoe.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 日期时间工具类
 * 统一日期格式化，避免重复创建SimpleDateFormat实例
 */
object DateTimeUtils {
    
    // 复用的DateFormat实例
    private val timeFormat by lazy { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    private val dateFormat by lazy { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    private val dateTimeFormat by lazy { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }
    private val shortTimeFormat by lazy { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    
    /**
     * 格式化时间为 HH:mm:ss
     */
    fun formatTime(timestamp: Long): String {
        return timeFormat.format(Date(timestamp))
    }
    
    /**
     * 格式化为短格式 HH:mm
     */
    fun formatShortTime(timestamp: Long): String {
        return shortTimeFormat.format(Date(timestamp))
    }
    
    /**
     * 格式化日期为 yyyy-MM-dd
     */
    fun formatDate(timestamp: Long): String {
        return dateFormat.format(Date(timestamp))
    }
    
    /**
     * 格式化完整日期时间为 yyyy-MM-dd HH:mm:ss
     */
    fun formatDateTime(timestamp: Long): String {
        return dateTimeFormat.format(Date(timestamp))
    }
    
    /**
     * 格式化时长为可读字符串
     * @param durationSeconds 时长（秒）
     */
    fun formatDuration(durationSeconds: Long): String {
        return when {
            durationSeconds < 60 -> "${durationSeconds}秒"
            durationSeconds < 3600 -> "${durationSeconds / 60}分${durationSeconds % 60}秒"
            else -> "${durationSeconds / 3600}小时${(durationSeconds % 3600) / 60}分"
        }
    }
}
