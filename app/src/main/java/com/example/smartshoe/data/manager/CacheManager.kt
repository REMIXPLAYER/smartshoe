package com.example.smartshoe.data.manager

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 缓存管理器
 * 统一管理应用缓存清理，包括文件缓存和内存缓存
 *
 * 职责：
 * - 清理应用文件缓存（cacheDir, externalCacheDir）
 * - 提供缓存清理状态监控
 * - 支持协程取消的异步清理
 */
@Singleton
class CacheManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferencesManager: UserPreferencesManager
) {

    companion object {
        private const val TAG = "CacheManager"
    }

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // 缓存清理状态流
    private val _clearCacheState = MutableStateFlow<CacheClearState>(CacheClearState.Idle)
    val clearCacheState: StateFlow<CacheClearState> = _clearCacheState.asStateFlow()

    private var cacheClearJob: Job? = null

    /**
     * 缓存清理状态
     */
    sealed class CacheClearState {
        object Idle : CacheClearState()
        object Clearing : CacheClearState()
        data class Success(val message: String) : CacheClearState()
        data class Error(val message: String) : CacheClearState()
    }

    /**
     * 清除应用文件缓存
     * 包括：cacheDir 和 externalCacheDir
     *
     * @param onComplete 清理完成回调（在主线程执行）
     */
    fun clearFileCache(onComplete: ((Boolean, String) -> Unit)? = null) {
        cacheClearJob?.cancel() // 取消之前的清理任务

        _clearCacheState.value = CacheClearState.Clearing

        cacheClearJob = coroutineScope.launch {
            try {
                var clearedCount = 0

                // 清理内部缓存目录
                context.cacheDir.listFiles()?.forEach { file ->
                    if (isActive) {
                        if (file.isDirectory) {
                            file.deleteRecursively()
                        } else {
                            file.delete()
                        }
                        clearedCount++
                    }
                }

                // 清理外部缓存目录
                context.externalCacheDir?.listFiles()?.forEach { file ->
                    if (isActive) {
                        if (file.isDirectory) {
                            file.deleteRecursively()
                        } else {
                            file.delete()
                        }
                        clearedCount++
                    }
                }

                val message = "已清理 $clearedCount 个缓存文件"
                _clearCacheState.value = CacheClearState.Success(message)

                withContext(Dispatchers.Main) {
                    onComplete?.invoke(true, message)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error clearing cache: ${e.message}", e)
                val errorMessage = "清理缓存失败: ${e.message}"
                _clearCacheState.value = CacheClearState.Error(errorMessage)

                withContext(Dispatchers.Main) {
                    onComplete?.invoke(false, errorMessage)
                }
            }
        }
    }

    /**
     * 清除用户偏好设置
     */
    fun clearUserPreferences() {
        userPreferencesManager.clearPreferences()
    }

    /**
     * 获取缓存大小（字节）
     */
    suspend fun getCacheSize(): Long = withContext(Dispatchers.IO) {
        var size = 0L

        context.cacheDir.listFiles()?.forEach { file ->
            size += getFileSize(file)
        }

        context.externalCacheDir?.listFiles()?.forEach { file ->
            size += getFileSize(file)
        }

        size
    }

    /**
     * 递归计算文件/目录大小
     */
    private fun getFileSize(file: java.io.File): Long {
        return if (file.isDirectory) {
            file.listFiles()?.sumOf { getFileSize(it) } ?: 0L
        } else {
            file.length()
        }
    }

    /**
     * 重置状态为 Idle
     */
    fun resetState() {
        _clearCacheState.value = CacheClearState.Idle
    }

    /**
     * 取消正在进行的清理任务
     */
    fun cancelClearing() {
        cacheClearJob?.cancel()
        _clearCacheState.value = CacheClearState.Idle
    }

    /**
     * 清理资源
     */
    fun cleanup() {
        cancelClearing()
        coroutineScope.cancel()
    }
}
