package com.example.smartshoe.data.remote

import android.util.Log
import java.util.concurrent.ConcurrentHashMap

/**
 * 请求缓存管理器
 * 用于缓存网络请求结果，减少重复请求
 */
class RequestCache private constructor() {

    private val cache = ConcurrentHashMap<String, CacheEntry>()
    private val lock = Any()

    companion object {
        private const val TAG = "RequestCache"
        private const val DEFAULT_CACHE_DURATION_MS = 5 * 60 * 1000L // 默认缓存5分钟

        @Volatile
        private var instance: RequestCache? = null

        fun getInstance(): RequestCache {
            return instance ?: synchronized(this) {
                instance ?: RequestCache().also { instance = it }
            }
        }
    }

    /**
     * 缓存条目
     */
    data class CacheEntry(
        val data: Any,
        val timestamp: Long,
        val durationMs: Long
    ) {
        fun isExpired(): Boolean {
            return System.currentTimeMillis() - timestamp > durationMs
        }
    }

    /**
     * 生成缓存键
     */
    fun generateKey(endpoint: String, params: Map<String, Any?> = emptyMap()): String {
        val sortedParams = params.toSortedMap()
        return "$endpoint?${sortedParams.entries.joinToString("&") { "${it.key}=${it.value}" }}"
    }

    /**
     * 获取缓存数据
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: String): T? {
        synchronized(lock) {
            val entry = cache[key]
            return if (entry != null && !entry.isExpired()) {
                Log.d(TAG, "Cache hit for key: $key")
                entry.data as? T
            } else {
                if (entry != null) {
                    // 缓存已过期，移除
                    cache.remove(key)
                    Log.d(TAG, "Cache expired for key: $key")
                }
                null
            }
        }
    }

    /**
     * 设置缓存数据
     */
    fun put(key: String, data: Any, durationMs: Long = DEFAULT_CACHE_DURATION_MS) {
        synchronized(lock) {
            cache[key] = CacheEntry(data, System.currentTimeMillis(), durationMs)
            Log.d(TAG, "Cached data for key: $key, duration: ${durationMs}ms")
        }
    }

    /**
     * 移除缓存
     */
    fun remove(key: String) {
        synchronized(lock) {
            cache.remove(key)
            Log.d(TAG, "Removed cache for key: $key")
        }
    }

    /**
     * 清空所有缓存
     */
    fun clear() {
        synchronized(lock) {
            cache.clear()
            Log.d(TAG, "All cache cleared")
        }
    }

    /**
     * 清理过期缓存
     */
    fun clearExpired() {
        synchronized(lock) {
            val expiredKeys = cache.filterValues { it.isExpired() }.keys
            expiredKeys.forEach { cache.remove(it) }
            if (expiredKeys.isNotEmpty()) {
                Log.d(TAG, "Cleared ${expiredKeys.size} expired cache entries")
            }
        }
    }

    /**
     * 获取缓存统计信息
     */
    fun getStats(): CacheStats {
        val totalEntries = cache.size
        val expiredEntries = cache.count { it.value.isExpired() }
        return CacheStats(
            totalEntries = totalEntries,
            expiredEntries = expiredEntries,
            validEntries = totalEntries - expiredEntries
        )
    }

    /**
     * 缓存统计信息
     */
    data class CacheStats(
        val totalEntries: Int,
        val expiredEntries: Int,
        val validEntries: Int
    )
}
