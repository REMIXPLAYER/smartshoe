package com.example.smartshoe.data.remote

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * 请求合并器
 * 用于合并相同或相似的并发请求，减少网络请求次数
 */
class RequestBatcher private constructor() {

    // 存储正在进行的请求
    private val pendingRequests = ConcurrentHashMap<String, Deferred<Any>>()
    private val mutex = Mutex()
    private val batcherScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "RequestBatcher"

        @Volatile
        private var instance: RequestBatcher? = null

        fun getInstance(): RequestBatcher {
            return instance ?: synchronized(this) {
                instance ?: RequestBatcher().also { instance = it }
            }
        }
    }

    /**
     * 执行可合并的请求
     * 如果相同key的请求正在进行中，则等待该请求完成并返回结果
     * 否则执行新的请求
     *
     * @param key 请求的唯一标识
     * @param request 实际的请求操作
     * @return 请求结果
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun <T> execute(
        key: String,
        request: suspend () -> T
    ): T {
        // 先检查是否有正在进行的相同请求
        val existingDeferred = pendingRequests[key] as? Deferred<T>

        if (existingDeferred != null) {
            Log.d(TAG, "Reusing pending request for key: $key")
            return existingDeferred.await()
        }

        // 创建新请求
        return mutex.withLock {
            // 双重检查
            val doubleCheckDeferred = pendingRequests[key] as? Deferred<T>
            if (doubleCheckDeferred != null) {
                Log.d(TAG, "Double-check: reusing pending request for key: $key")
                return@withLock doubleCheckDeferred.await()
            }

            // 创建新的Deferred
            val newDeferred = batcherScope.async {
                try {
                    Log.d(TAG, "Executing new request for key: $key")
                    request()
                } finally {
                    // 请求完成后移除
                    pendingRequests.remove(key)
                    Log.d(TAG, "Request completed and removed for key: $key")
                }
            }

            pendingRequests[key] = newDeferred as Deferred<Any>
            (newDeferred.await() as T)
        }
    }

    /**
     * 批量执行多个请求
     * 将多个请求合并为一个批量请求
     *
     * @param keys 请求标识列表
     * @param batchRequest 批量请求操作
     * @return 批量请求结果
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun <T, R> executeBatch(
        keys: List<String>,
        batchRequest: suspend (List<String>) -> Map<String, T>
    ): Map<String, T> {
        if (keys.isEmpty()) return emptyMap()

        val batchKey = "batch_${keys.sorted().joinToString(",")}"

        return execute(batchKey) {
            val results = batchRequest(keys)
            results as R
            results
        } as Map<String, T>
    }

    /**
     * 取消指定key的请求
     */
    fun cancel(key: String) {
        val deferred = pendingRequests.remove(key)
        deferred?.cancel()
        Log.d(TAG, "Cancelled request for key: $key")
    }

    /**
     * 取消所有待处理的请求
     */
    fun cancelAll() {
        val keys = pendingRequests.keys.toList()
        keys.forEach { cancel(it) }
        Log.d(TAG, "Cancelled all ${keys.size} pending requests")
    }

    /**
     * 获取待处理请求数量
     */
    fun getPendingRequestCount(): Int = pendingRequests.size

    /**
     * 检查是否有待处理的请求
     */
    fun hasPendingRequest(key: String): Boolean = pendingRequests.containsKey(key)

    /**
     * 销毁批处理器
     */
    fun destroy() {
        cancelAll()
        batcherScope.cancel()
        Log.d(TAG, "RequestBatcher destroyed")
    }
}
