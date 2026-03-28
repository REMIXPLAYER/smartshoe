package com.example.smartshoe.data.manager

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.util.Log
import android.view.Choreographer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.BufferedReader
import java.io.FileReader
import java.io.RandomAccessFile
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 性能监控器
 * 统一管理FPS、内存、CPU使用率的监控
 * 
 * 重构：使用 Hilt 注入替代单例模式
 */
@Singleton
class PerformanceMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "PerformanceMonitor"
        private const val FPS_SAMPLE_INTERVAL = 1000L // FPS采样间隔1秒
        private const val MEMORY_SAMPLE_INTERVAL = 2000L // 内存采样间隔2秒
        private const val CPU_SAMPLE_INTERVAL = 3000L // CPU采样间隔3秒
        private const val MAX_FPS_SAMPLES = 60 // 最大FPS样本数
        private const val MAX_MEMORY_SAMPLES = 30 // 最大内存样本数
        private const val MAX_CPU_SAMPLES = 20 // 最大CPU样本数

        // 是否启用详细日志（DEBUG模式下可开启）
        private const val ENABLE_VERBOSE_LOGS = false
    }

    // FPS监控
    private val _fpsState = MutableStateFlow(FpsState())
    val fpsState: StateFlow<FpsState> = _fpsState.asStateFlow()

    // 内存监控
    private val _memoryState = MutableStateFlow(MemoryState())
    val memoryState: StateFlow<MemoryState> = _memoryState.asStateFlow()

    // CPU监控
    private val _cpuState = MutableStateFlow(CpuState())
    val cpuState: StateFlow<CpuState> = _cpuState.asStateFlow()

    // 性能报告
    private val _performanceReport = MutableStateFlow(PerformanceReport())
    val performanceReport: StateFlow<PerformanceReport> = _performanceReport.asStateFlow()

    // 调度器
    private val scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(3)
    private val mainHandler = Handler(Looper.getMainLooper())

    // FPS计算相关
    private var frameCount = AtomicLong(0)
    private var lastFpsTime = System.nanoTime()
    private var isFpsMonitoring = false
    private val fpsHistory = ConcurrentLinkedQueue<Int>()

    // 内存历史
    private val memoryHistory = ConcurrentLinkedQueue<MemorySnapshot>()

    // CPU历史
    private val cpuHistory = ConcurrentLinkedQueue<CpuSnapshot>()
    private var lastCpuTime: Long = 0
    private var lastAppCpuTime: Long = 0

    // 是否正在监控
    private var isMonitoring = false

    /**
     * FPS状态数据类
     */
    data class FpsState(
        val currentFps: Int = 60,
        val averageFps: Double = 60.0,
        val minFps: Int = 60,
        val maxFps: Int = 60,
        val fpsDropCount: Int = 0, // 掉帧次数（低于55fps）
        val isRunning: Boolean = false
    )

    /**
     * 内存状态数据类
     */
    data class MemoryState(
        val heapSize: Long = 0, // 堆大小（字节）
        val heapAlloc: Long = 0, // 已分配堆内存
        val heapFree: Long = 0, // 空闲堆内存
        val totalPss: Long = 0, // 总PSS内存
        val nativeHeap: Long = 0, // Native堆内存
        val isRunning: Boolean = false
    )

    /**
     * CPU状态数据类
     */
    data class CpuState(
        val appCpuUsage: Double = 0.0, // 应用CPU使用率
        val systemCpuUsage: Double = 0.0, // 系统CPU使用率
        val threadCount: Int = 0, // 线程数
        val isRunning: Boolean = false
    )

    /**
     * 内存快照
     */
    data class MemorySnapshot(
        val timestamp: Long,
        val heapSize: Long,
        val heapAlloc: Long,
        val totalPss: Long
    )

    /**
     * CPU快照
     */
    data class CpuSnapshot(
        val timestamp: Long,
        val appCpuUsage: Double,
        val systemCpuUsage: Double
    )

    /**
     * 性能报告
     */
    data class PerformanceReport(
        val startTime: Long = 0,
        val endTime: Long = 0,
        val duration: Long = 0,
        val avgFps: Double = 0.0,
        val minFps: Int = 0,
        val maxFps: Int = 0,
        val fpsDropRate: Double = 0.0,
        val avgMemoryUsage: Long = 0,
        val peakMemoryUsage: Long = 0,
        val avgCpuUsage: Double = 0.0,
        val peakCpuUsage: Double = 0.0,
        val memoryLeakRisk: String = "Unknown",
        val overallScore: Int = 100 // 综合评分
    )

    /**
     * 开始性能监控
     */
    fun startMonitoring(context: Context) {
        if (isMonitoring) return
        isMonitoring = true

        Log.d(TAG, "Starting performance monitoring")

        // 开始FPS监控
        startFpsMonitoring()

        // 开始内存监控
        startMemoryMonitoring(context)

        // 开始CPU监控
        startCpuMonitoring()

        // 重置报告
        _performanceReport.value = PerformanceReport(startTime = System.currentTimeMillis())
    }

    /**
     * 停止性能监控
     */
    fun stopMonitoring() {
        if (!isMonitoring) return
        isMonitoring = false

        Log.d(TAG, "Stopping performance monitoring")

        // 停止FPS监控
        stopFpsMonitoring()

        // 生成最终报告
        generateFinalReport()
    }

    /**
     * 开始FPS监控
     */
    private fun startFpsMonitoring() {
        if (isFpsMonitoring) return
        isFpsMonitoring = true

        _fpsState.value = _fpsState.value.copy(isRunning = true)

        // 注册帧回调
        mainHandler.post {
            Choreographer.getInstance().postFrameCallback(object : Choreographer.FrameCallback {
                override fun doFrame(frameTimeNanos: Long) {
                    if (!isFpsMonitoring) return

                    frameCount.incrementAndGet()
                    Choreographer.getInstance().postFrameCallback(this)
                }
            })
        }

        // 定期计算FPS
        scheduler.scheduleAtFixedRate({
            calculateFps()
        }, FPS_SAMPLE_INTERVAL, FPS_SAMPLE_INTERVAL, TimeUnit.MILLISECONDS)
    }

    /**
     * 停止FPS监控
     */
    private fun stopFpsMonitoring() {
        isFpsMonitoring = false
        _fpsState.value = _fpsState.value.copy(isRunning = false)
    }

    /**
     * 计算FPS
     */
    private fun calculateFps() {
        val currentTime = System.nanoTime()
        val elapsedNanos = currentTime - lastFpsTime
        val elapsedSeconds = elapsedNanos / 1_000_000_000.0

        if (elapsedSeconds > 0) {
            val currentFrames = frameCount.getAndSet(0)
            val fps = (currentFrames / elapsedSeconds).toInt().coerceIn(0, 120)

            fpsHistory.offer(fps)
            if (fpsHistory.size > MAX_FPS_SAMPLES) {
                fpsHistory.poll()
            }

            val fpsList = fpsHistory.toList()
            val avgFps = fpsList.average()
            val minFps = fpsList.minOrNull() ?: fps
            val maxFps = fpsList.maxOrNull() ?: fps
            val dropCount = if (fps < 55) _fpsState.value.fpsDropCount + 1 else _fpsState.value.fpsDropCount

            _fpsState.value = FpsState(
                currentFps = fps,
                averageFps = avgFps,
                minFps = minFps,
                maxFps = maxFps,
                fpsDropCount = dropCount,
                isRunning = true
            )

            if (ENABLE_VERBOSE_LOGS) {
                Log.v(TAG, "FPS: $fps, Avg: ${String.format("%.1f", avgFps)}")
            }
        }

        lastFpsTime = currentTime
    }

    /**
     * 开始内存监控
     */
    private fun startMemoryMonitoring(context: Context) {
        _memoryState.value = _memoryState.value.copy(isRunning = true)

        scheduler.scheduleAtFixedRate({
            try {
                val runtime = Runtime.getRuntime()
                val heapSize = runtime.totalMemory()
                val heapFree = runtime.freeMemory()
                val heapAlloc = heapSize - heapFree

                // 获取PSS内存
                val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                val memoryInfo = ActivityManager.MemoryInfo()
                activityManager.getMemoryInfo(memoryInfo)

                val debugInfo = Debug.MemoryInfo()
                Debug.getMemoryInfo(debugInfo)
                val totalPss = debugInfo.totalPss * 1024L // 转换为字节
                val nativeHeap = Debug.getNativeHeapAllocatedSize()

                _memoryState.value = MemoryState(
                    heapSize = heapSize,
                    heapAlloc = heapAlloc,
                    heapFree = heapFree,
                    totalPss = totalPss,
                    nativeHeap = nativeHeap,
                    isRunning = true
                )

                // 记录历史
                memoryHistory.offer(MemorySnapshot(
                    timestamp = System.currentTimeMillis(),
                    heapSize = heapSize,
                    heapAlloc = heapAlloc,
                    totalPss = totalPss
                ))

                if (memoryHistory.size > MAX_MEMORY_SAMPLES) {
                    memoryHistory.poll()
                }

                if (ENABLE_VERBOSE_LOGS) {
                    Log.v(TAG, "Memory - Heap: ${formatBytes(heapAlloc)}/${formatBytes(heapSize)}, PSS: ${formatBytes(totalPss)}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error monitoring memory", e)
            }
        }, 0, MEMORY_SAMPLE_INTERVAL, TimeUnit.MILLISECONDS)
    }

    /**
     * 开始CPU监控
     */
    private fun startCpuMonitoring() {
        _cpuState.value = _cpuState.value.copy(isRunning = true)

        // 初始化CPU时间
        lastCpuTime = readSystemCpuTime()
        lastAppCpuTime = readAppCpuTime()

        scheduler.scheduleAtFixedRate({
            try {
                val currentCpuTime = readSystemCpuTime()
                val currentAppCpuTime = readAppCpuTime()

                if (lastCpuTime > 0 && lastAppCpuTime > 0) {
                    val systemCpuDiff = currentCpuTime - lastCpuTime
                    val appCpuDiff = currentAppCpuTime - lastAppCpuTime

                    val appCpuUsage = if (systemCpuDiff > 0) {
                        (appCpuDiff.toDouble() / systemCpuDiff.toDouble()) * 100.0
                    } else 0.0

                    val systemCpuUsage = readSystemCpuUsage()
                    val threadCount = Thread.activeCount()

                    _cpuState.value = CpuState(
                        appCpuUsage = appCpuUsage.coerceIn(0.0, 100.0),
                        systemCpuUsage = systemCpuUsage,
                        threadCount = threadCount,
                        isRunning = true
                    )

                    // 记录历史
                    cpuHistory.offer(CpuSnapshot(
                        timestamp = System.currentTimeMillis(),
                        appCpuUsage = appCpuUsage,
                        systemCpuUsage = systemCpuUsage
                    ))

                    if (cpuHistory.size > MAX_CPU_SAMPLES) {
                        cpuHistory.poll()
                    }

                    if (ENABLE_VERBOSE_LOGS) {
                        Log.v(TAG, "CPU - App: ${String.format("%.1f", appCpuUsage)}%, System: ${String.format("%.1f", systemCpuUsage)}%, Threads: $threadCount")
                    }
                }

                lastCpuTime = currentCpuTime
                lastAppCpuTime = currentAppCpuTime
            } catch (e: Exception) {
                Log.e(TAG, "Error monitoring CPU", e)
            }
        }, CPU_SAMPLE_INTERVAL, CPU_SAMPLE_INTERVAL, TimeUnit.MILLISECONDS)
    }

    /**
     * 读取系统CPU时间
     */
    private fun readSystemCpuTime(): Long {
        return try {
            val reader = BufferedReader(FileReader("/proc/stat"))
            val line = reader.readLine()
            reader.close()

            if (line != null && line.startsWith("cpu ")) {
                val parts = line.split(" ").filter { it.isNotEmpty() }
                if (parts.size >= 5) {
                    var totalTime = 0L
                    for (i in 1 until parts.size) {
                        totalTime += parts[i].toLongOrNull() ?: 0
                    }
                    totalTime
                } else 0
            } else 0
        } catch (e: Exception) {
            0
        }
    }

    /**
     * 读取应用CPU时间
     */
    private fun readAppCpuTime(): Long {
        return try {
            val pid = Process.myPid()
            val reader = BufferedReader(FileReader("/proc/$pid/stat"))
            val line = reader.readLine()
            reader.close()

            if (line != null) {
                val parts = line.split(" ")
                if (parts.size >= 17) {
                    // utime + stime (用户态时间 + 内核态时间)
                    (parts[13].toLongOrNull() ?: 0) + (parts[14].toLongOrNull() ?: 0)
                } else 0
            } else 0
        } catch (e: Exception) {
            0
        }
    }

    /**
     * 读取系统CPU使用率
     */
    private fun readSystemCpuUsage(): Double {
        return try {
            val reader = RandomAccessFile("/proc/stat", "r")
            val line = reader.readLine()
            reader.close()

            if (line != null && line.startsWith("cpu ")) {
                val parts = line.split(" ").filter { it.isNotEmpty() }
                if (parts.size >= 5) {
                    val user = parts[1].toLongOrNull() ?: 0
                    val nice = parts[2].toLongOrNull() ?: 0
                    val system = parts[3].toLongOrNull() ?: 0
                    val idle = parts[4].toLongOrNull() ?: 0

                    val total = user + nice + system + idle
                    val used = user + nice + system

                    if (total > 0) (used.toDouble() / total.toDouble()) * 100.0 else 0.0
                } else 0.0
            } else 0.0
        } catch (e: Exception) {
            0.0
        }
    }

    /**
     * 生成最终性能报告
     */
    private fun generateFinalReport() {
        val fpsList = fpsHistory.toList()
        val memoryList = memoryHistory.toList()
        val cpuList = cpuHistory.toList()

        val avgFps = fpsList.average()
        val minFps = fpsList.minOrNull() ?: 0
        val maxFps = fpsList.maxOrNull() ?: 0
        val fpsDropRate = if (fpsList.isNotEmpty()) {
            fpsList.count { it < 55 }.toDouble() / fpsList.size.toDouble() * 100.0
        } else 0.0

        val avgMemory = if (memoryList.isNotEmpty()) {
            memoryList.map { it.heapAlloc }.average().toLong()
        } else 0
        val peakMemory = memoryList.maxOfOrNull { it.heapAlloc } ?: 0

        val avgCpu = cpuList.map { it.appCpuUsage }.average()
        val peakCpu = cpuList.maxOfOrNull { it.appCpuUsage } ?: 0.0

        // 评估内存泄漏风险
        val memoryLeakRisk = when {
            memoryList.size >= 10 -> {
                val firstHalf = memoryList.take(memoryList.size / 2).map { it.heapAlloc }.average()
                val secondHalf = memoryList.drop(memoryList.size / 2).map { it.heapAlloc }.average()
                val growthRate = if (firstHalf > 0) (secondHalf - firstHalf) / firstHalf * 100 else 0.0

                when {
                    growthRate > 50 -> "High"
                    growthRate > 20 -> "Medium"
                    else -> "Low"
                }
            }
            else -> "Unknown"
        }

        // 计算综合评分
        val fpsScore = when {
            avgFps >= 58 -> 40
            avgFps >= 55 -> 35
            avgFps >= 50 -> 30
            avgFps >= 45 -> 20
            else -> 10
        }

        val memoryScore = when (memoryLeakRisk) {
            "Low" -> 30
            "Medium" -> 20
            "High" -> 10
            else -> 25
        }

        val cpuScore = when {
            avgCpu < 10 -> 30
            avgCpu < 20 -> 25
            avgCpu < 30 -> 20
            avgCpu < 50 -> 15
            else -> 10
        }

        val overallScore = fpsScore + memoryScore + cpuScore

        _performanceReport.value = PerformanceReport(
            startTime = _performanceReport.value.startTime,
            endTime = System.currentTimeMillis(),
            duration = System.currentTimeMillis() - _performanceReport.value.startTime,
            avgFps = avgFps,
            minFps = minFps,
            maxFps = maxFps,
            fpsDropRate = fpsDropRate,
            avgMemoryUsage = avgMemory,
            peakMemoryUsage = peakMemory,
            avgCpuUsage = avgCpu,
            peakCpuUsage = peakCpu,
            memoryLeakRisk = memoryLeakRisk,
            overallScore = overallScore
        )

        Log.d(TAG, "Performance Report Generated: Score=$overallScore, AvgFPS=${String.format("%.1f", avgFps)}, MemoryRisk=$memoryLeakRisk")
    }

    /**
     * 获取格式化的性能报告
     */
    fun getFormattedReport(): String {
        val report = _performanceReport.value
        return buildString {
            appendLine("=== 性能报告 ===")
            appendLine("测试时长: ${formatDuration(report.duration)}")
            appendLine("")
            appendLine("--- FPS性能 ---")
            appendLine("平均FPS: ${String.format("%.1f", report.avgFps)}")
            appendLine("最低FPS: ${report.minFps}")
            appendLine("最高FPS: ${report.maxFps}")
            appendLine("掉帧率: ${String.format("%.1f", report.fpsDropRate)}%")
            appendLine("")
            appendLine("--- 内存使用 ---")
            appendLine("平均内存: ${formatBytes(report.avgMemoryUsage)}")
            appendLine("峰值内存: ${formatBytes(report.peakMemoryUsage)}")
            appendLine("泄漏风险: ${report.memoryLeakRisk}")
            appendLine("")
            appendLine("--- CPU使用 ---")
            appendLine("平均CPU: ${String.format("%.1f", report.avgCpuUsage)}%")
            appendLine("峰值CPU: ${String.format("%.1f", report.peakCpuUsage)}%")
            appendLine("")
            appendLine("--- 综合评分 ---")
            appendLine("总分: ${report.overallScore}/100")
            when {
                report.overallScore >= 90 -> appendLine("评级: 优秀")
                report.overallScore >= 75 -> appendLine("评级: 良好")
                report.overallScore >= 60 -> appendLine("评级: 及格")
                else -> appendLine("评级: 需优化")
            }
        }
    }

    /**
     * 格式化字节数为可读字符串
     */
    private fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 * 1024 -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
            bytes >= 1024 * 1024 -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> String.format("%.2f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }

    /**
     * 格式化时长为可读字符串
     */
    private fun formatDuration(millis: Long): String {
        val seconds = millis / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        return when {
            hours > 0 -> String.format("%02d:%02d:%02d", hours, minutes % 60, seconds % 60)
            else -> String.format("%02d:%02d", minutes, seconds % 60)
        }
    }

    /**
     * 清除历史数据
     */
    fun clearHistory() {
        fpsHistory.clear()
        memoryHistory.clear()
        cpuHistory.clear()
        frameCount.set(0)
        lastFpsTime = System.nanoTime()
    }

    /**
     * 销毁监控器
     */
    fun destroy() {
        stopMonitoring()
        scheduler.shutdown()
        clearHistory()
        Log.d(TAG, "PerformanceMonitor destroyed")
    }
}
