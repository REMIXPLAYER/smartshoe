package com.example.smartshoe.data.remote

import android.util.Log
import com.example.smartshoe.config.AppConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 请求管理器
 * 统一管理请求缓存和请求合并功能
 * 重构：使用 Hilt 依赖注入替代单例模式
 * 重构：使用 AppConfig 替代硬编码魔法数字
 */
@Singleton
class RequestManager @Inject constructor() {

    // 缓存相关
    private val cache = ConcurrentHashMap<String, CacheEntry>()
    private val cacheLock = Any()

    // 请求合并相关
    private val pendingRequests = ConcurrentHashMap<String, Deferred<Any>>()
    private val requestMutex = Mutex()
    private val requestScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "RequestManager"
    }

    // ==================== 缓存功能 ====================

    /**
     * 缓存条目
     */
    data class CacheEntry(
        val data: Any,
        val timestamp: Long,
        val durationMs: Long
    ) {
        fun isExpired(): Boolean = System.currentTimeMillis() - timestamp > durationMs
    }

    /**
     * 生成缓存键
     */
    fun generateCacheKey(endpoint: String, params: Map<String, Any?> = emptyMap()): String {
        val sortedParams = params.toSortedMap()
        return "$endpoint?${sortedParams.entries.joinToString("&") { "${it.key}=${it.value}" }}"
    }

    /**
     * 从缓存获取数据
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getFromCache(key: String): T? {
        synchronized(cacheLock) {
            val entry = cache[key]
            return if (entry != null && !entry.isExpired()) {
                Log.d(TAG, "Cache hit: $key")
                entry.data as? T
            } else {
                entry?.let { cache.remove(key); Log.d(TAG, "Cache expired: $key") }
                null
            }
        }
    }

    /**
     * 存入缓存
     */
    fun putToCache(key: String, data: Any, durationMs: Long = AppConfig.Cache.TIME_RANGE_CACHE_DURATION_MS) {
        synchronized(cacheLock) {
            cache[key] = CacheEntry(data, System.currentTimeMillis(), durationMs)
            Log.d(TAG, "Cached: $key, duration: ${durationMs}ms")
        }
    }

    /**
     * 清除指定缓存
     */
    fun removeFromCache(key: String) {
        synchronized(cacheLock) { cache.remove(key) }
    }

    /**
     * 清空所有缓存
     */
    fun clearCache() {
        synchronized(cacheLock) {
            cache.clear()
            Log.d(TAG, "All cache cleared")
        }
    }

    /**
     * 获取缓存统计
     */
    fun getCacheStats(): CacheStats {
        val total = cache.size
        val expired = cache.count { it.value.isExpired() }
        return CacheStats(total, expired, total - expired)
    }

    data class CacheStats(val total: Int, val expired: Int, val valid: Int)

    // ==================== 错误结果检测 ====================

    /**
     * 检查结果是否为错误类型
     * 用于决定是否缓存该结果
     * 
     * 注意：此方法通过检查类名来判断是否为错误结果，
     * 适用于项目中使用的密封类（sealed class）模式，如 SensorDataRecordsResult.Error 等
     */
    private fun isErrorResult(result: Any?): Boolean {
        if (result == null) return true

        val resultClass = result::class.java
        val className = resultClass.simpleName

        // 检查是否为密封类的 Error 子类（如 XXXResult.Error）
        return when {
            // 精确匹配 Error 类名（密封类的错误子类）
            className == "Error" -> true
            // 以 Error 结尾的类名
            className.endsWith("Error") -> true
            // Throwable 及其子类
            result is Throwable -> true
            else -> false
        }
    }

    // ==================== 请求合并功能 ====================

    /**
     * 执行可合并的请求
     * 相同key的并发请求会合并为一个请求
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun <T> executeWithDedup(
        key: String,
        request: suspend () -> T
    ): T {
        // 快速检查是否有正在进行的请求
        pendingRequests[key]?.let { existing ->
            Log.d(TAG, "Reusing pending request: $key")
            return (existing as Deferred<T>).await()
        }

        // 加锁创建新请求
        return requestMutex.withLock {
            // 双重检查
            (pendingRequests[key] as? Deferred<T>)?.await()?.let { return it }

            val newDeferred = requestScope.async {
                try {
                    Log.d(TAG, "Executing new request: $key")
                    request()
                } finally {
                    pendingRequests.remove(key)
                    Log.d(TAG, "Request completed: $key")
                }
            }

            pendingRequests[key] = newDeferred as Deferred<Any>
            newDeferred.await() as T
        }
    }

    /**
     * 带缓存的请求执行
     * 先检查缓存，再执行请求，最后缓存结果（仅成功响应会被缓存）
     */
    suspend fun <T> executeWithCache(
        cacheKey: String,
        useCache: Boolean = true,
        cacheDurationMs: Long = AppConfig.Cache.TIME_RANGE_CACHE_DURATION_MS,
        request: suspend () -> T
    ): T {
        // 检查缓存
        if (useCache) {
            getFromCache<T>(cacheKey)?.let { return it }
        }

        // 执行请求
        val result = request()

        // 仅缓存成功结果（检查结果是否为Error类型）
        if (useCache && !isErrorResult(result)) {
            @Suppress("UNCHECKED_CAST")
            putToCache(cacheKey, result as Any, cacheDurationMs)
        }

        return result
    }

    /**
     * 带缓存和请求合并的执行
     * 仅成功响应会被缓存
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun <T> executeWithCacheAndDedup(
        cacheKey: String,
        useCache: Boolean = true,
        cacheDurationMs: Long = AppConfig.Cache.TIME_RANGE_CACHE_DURATION_MS,
        request: suspend () -> T
    ): T {
        // 检查缓存
        if (useCache) {
            getFromCache<T>(cacheKey)?.let { return it }
        }

        // 执行请求（带合并）
        val result = executeWithDedup(cacheKey) { request() }

        // 仅缓存成功结果（检查结果是否为Error类型）
        if (useCache && !isErrorResult(result)) {
            putToCache(cacheKey, result as Any, cacheDurationMs)
        }

        return result
    }

    /**
     * 取消指定请求
     */
    fun cancelRequest(key: String) {
        pendingRequests.remove(key)?.cancel()
        Log.d(TAG, "Cancelled: $key")
    }

    /**
     * 取消所有待处理请求
     */
    fun cancelAllRequests() {
        val keys = pendingRequests.keys.toList()
        keys.forEach { cancelRequest(it) }
        Log.d(TAG, "Cancelled all ${keys.size} requests")
    }

    /**
     * 获取待处理请求数
     */
    fun getPendingRequestCount(): Int = pendingRequests.size

    /**
     * 销毁管理器
     */
    fun destroy() {
        cancelAllRequests()
        clearCache()
        requestScope.cancel()
        Log.d(TAG, "RequestManager destroyed")
    }
}
