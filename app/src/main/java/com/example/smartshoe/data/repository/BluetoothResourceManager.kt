package com.example.smartshoe.data.repository

import android.bluetooth.BluetoothSocket
import android.util.Log
import kotlinx.coroutines.*
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 蓝牙资源管理器
 * 负责蓝牙资源的分配、释放和监控
 * 防止蓝牙连接相关的内存泄漏
 */
class BluetoothResourceManager private constructor() {

    private val activeSockets = ConcurrentHashMap<String, BluetoothSocket>()
    private val activeStreams = ConcurrentHashMap<String, InputStream>()
    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val isReleased = AtomicBoolean(false)
    private val resourceLock = Any()

    // 协程作用域用于资源清理
    private val cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "BluetoothResourceMgr"

        @Volatile
        private var instance: BluetoothResourceManager? = null

        fun getInstance(): BluetoothResourceManager {
            return instance ?: synchronized(this) {
                instance ?: BluetoothResourceManager().also { instance = it }
            }
        }
    }

    /**
     * 注册蓝牙Socket
     */
    fun registerSocket(deviceAddress: String, socket: BluetoothSocket) {
        synchronized(resourceLock) {
            if (isReleased.get()) {
                Log.w(TAG, "Cannot register socket, manager is released")
                closeSocketSafely(socket)
                return
            }

            // 关闭同一设备的旧连接
            activeSockets[deviceAddress]?.let { oldSocket ->
                Log.d(TAG, "Closing old socket for device: $deviceAddress")
                closeSocketSafely(oldSocket)
            }

            activeSockets[deviceAddress] = socket
            Log.d(TAG, "Socket registered for device: $deviceAddress")
        }
    }

    /**
     * 注册输入流
     */
    fun registerInputStream(deviceAddress: String, inputStream: InputStream) {
        synchronized(resourceLock) {
            if (isReleased.get()) {
                Log.w(TAG, "Cannot register stream, manager is released")
                closeStreamSafely(inputStream)
                return
            }

            activeStreams[deviceAddress] = inputStream
            Log.d(TAG, "InputStream registered for device: $deviceAddress")
        }
    }

    /**
     * 注册协程Job
     */
    fun registerJob(deviceAddress: String, job: Job) {
        synchronized(resourceLock) {
            if (isReleased.get()) {
                Log.w(TAG, "Cannot register job, manager is released")
                job.cancel()
                return
            }

            // 取消同一设备的旧任务
            activeJobs[deviceAddress]?.let { oldJob ->
                if (oldJob.isActive) {
                    Log.d(TAG, "Cancelling old job for device: $deviceAddress")
                    oldJob.cancel()
                }
            }

            activeJobs[deviceAddress] = job
            Log.d(TAG, "Job registered for device: $deviceAddress")
        }
    }

    /**
     * 释放指定设备的所有资源
     */
    fun releaseDeviceResources(deviceAddress: String) {
        synchronized(resourceLock) {
            Log.d(TAG, "Releasing resources for device: $deviceAddress")

            // 取消协程任务
            activeJobs.remove(deviceAddress)?.let { job ->
                if (job.isActive) {
                    job.cancel()
                    Log.d(TAG, "Cancelled job for device: $deviceAddress")
                }
            }

            // 关闭输入流
            activeStreams.remove(deviceAddress)?.let { stream ->
                closeStreamSafely(stream)
                Log.d(TAG, "Closed input stream for device: $deviceAddress")
            }

            // 关闭Socket
            activeSockets.remove(deviceAddress)?.let { socket ->
                closeSocketSafely(socket)
                Log.d(TAG, "Closed socket for device: $deviceAddress")
            }
        }
    }

    /**
     * 安全关闭Socket
     */
    private fun closeSocketSafely(socket: BluetoothSocket) {
        try {
            if (socket.isConnected) {
                socket.close()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error closing socket: ${e.message}")
        }
    }

    /**
     * 安全关闭输入流
     */
    private fun closeStreamSafely(stream: InputStream) {
        try {
            stream.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing stream: ${e.message}")
        }
    }

    /**
     * 释放所有资源
     */
    fun releaseAllResources() {
        synchronized(resourceLock) {
            if (isReleased.getAndSet(true)) {
                return
            }

            Log.d(TAG, "Releasing all bluetooth resources")

            // 取消所有协程任务
            activeJobs.values.forEach { job ->
                if (job.isActive) {
                    job.cancel()
                }
            }
            activeJobs.clear()

            // 关闭所有输入流
            activeStreams.values.forEach { stream ->
                closeStreamSafely(stream)
            }
            activeStreams.clear()

            // 关闭所有Socket
            activeSockets.values.forEach { socket ->
                closeSocketSafely(socket)
            }
            activeSockets.clear()

            // 取消清理协程作用域
            cleanupScope.cancel()

            Log.d(TAG, "All bluetooth resources released")
        }
    }

    /**
     * 获取活动连接数量
     */
    fun getActiveConnectionCount(): Int = activeSockets.size

    /**
     * 检查设备是否有活动连接
     */
    fun hasActiveConnection(deviceAddress: String): Boolean {
        return activeSockets.containsKey(deviceAddress)
    }

    /**
     * 获取指定设备的Socket
     */
    fun getSocket(deviceAddress: String): BluetoothSocket? {
        return activeSockets[deviceAddress]
    }

    /**
     * 获取资源使用统计
     */
    fun getResourceStats(): ResourceStats {
        return ResourceStats(
            activeSockets = activeSockets.size,
            activeStreams = activeStreams.size,
            activeJobs = activeJobs.size,
            isReleased = isReleased.get()
        )
    }

    /**
     * 异步清理过期资源
     */
    fun cleanupStaleResources(maxIdleTimeMs: Long = 30000) {
        cleanupScope.launch {
            val currentTime = System.currentTimeMillis()
            // 这里可以添加基于时间的资源清理逻辑
            // 例如：关闭长时间空闲的连接
        }
    }

    /**
     * 资源统计信息
     */
    data class ResourceStats(
        val activeSockets: Int,
        val activeStreams: Int,
        val activeJobs: Int,
        val isReleased: Boolean
    ) {
        fun getTotalResources(): Int = activeSockets + activeStreams + activeJobs
    }
}
