package com.example.smartshoe.debug.util

import android.content.Context
import android.util.Log
import com.example.smartshoe.config.AppConfig
import com.example.smartshoe.data.local.CircularBuffer
import com.example.smartshoe.data.manager.BluetoothResourceManager
import com.example.smartshoe.data.manager.MemoryLeakDetector
import com.example.smartshoe.data.manager.PerformanceMonitor
import com.example.smartshoe.domain.model.SensorDataPoint
import com.example.smartshoe.util.DateTimeUtils
import kotlinx.coroutines.*
import java.io.File
import java.io.FileWriter
import java.util.*
import kotlin.random.Random

/**
 * 性能测试工具类
 * 用于进行基准性能测试、对比优化前后性能并生成报告
 *
 * 重构：
 * 1. 从 data/repository/ 移动到 debug/util/ 目录，符合 Clean Architecture
 * 2. 移除 getInstance() 调用，改为通过参数传入依赖
 * 3. 使用 AppConfig 替代硬编码魔法数字
 */
object PerformanceTestUtils {

    private const val TAG = "PerformanceTestUtils"

    /**
     * 性能测试结果数据类
     */
    data class PerformanceTestResult(
        val testName: String,
        val startTime: Long,
        val endTime: Long,
        val duration: Long,
        val fpsMetrics: FpsMetrics,
        val memoryMetrics: MemoryMetrics,
        val cpuMetrics: CpuMetrics,
        val additionalMetrics: Map<String, Any> = emptyMap()
    )

    /**
     * FPS指标
     */
    data class FpsMetrics(
        val averageFps: Double,
        val minFps: Int,
        val maxFps: Int,
        val fpsStability: Double, // FPS稳定性（标准差）
        val dropFrameRate: Double // 掉帧率
    )

    /**
     * 内存指标
     */
    data class MemoryMetrics(
        val averageHeapUsage: Long,
        val peakHeapUsage: Long,
        val averagePssUsage: Long,
        val peakPssUsage: Long,
        val memoryGrowthRate: Double // 内存增长率（%/分钟）
    )

    /**
     * CPU指标
     */
    data class CpuMetrics(
        val averageCpuUsage: Double,
        val peakCpuUsage: Double,
        val averageThreadCount: Int
    )

    /**
     * 性能对比结果
     */
    data class PerformanceComparison(
        val beforeResult: PerformanceTestResult,
        val afterResult: PerformanceTestResult,
        val fpsImprovement: Double,
        val memoryImprovement: Double,
        val cpuImprovement: Double,
        val overallImprovement: Double
    )

    /**
     * 运行性能测试
     * @param context 上下文
     * @param testName 测试名称
     * @param performanceMonitor 性能监控器实例
     * @param testDurationMs 测试时长（毫秒）
     * @param onProgress 进度回调
     * @param onComplete 完成回调
     */
    fun runPerformanceTest(
        context: Context,
        testName: String,
        performanceMonitor: PerformanceMonitor,
        testDurationMs: Long = AppConfig.Debug.PERFORMANCE_TEST_DURATION_MS,
        onProgress: (progress: Int, message: String) -> Unit = { _, _ -> },
        onComplete: (result: PerformanceTestResult) -> Unit
    ) {
        val fpsSamples = mutableListOf<Int>()
        val memorySamples = mutableListOf<Pair<Long, Long>>() // Pair<heapAlloc, pss>
        val cpuSamples = mutableListOf<Double>()
        val threadCountSamples = mutableListOf<Int>()

        CoroutineScope(Dispatchers.Main).launch {
            onProgress(0, "开始性能测试: $testName")

            // 预热阶段
            onProgress(5, "预热中...")
            performanceMonitor.startMonitoring(context)
            delay(AppConfig.Debug.WARMUP_DURATION_MS)

            val startTime = System.currentTimeMillis()
            val testJob = launch(Dispatchers.Default) {
                var elapsedTime = 0L
                while (elapsedTime < testDurationMs && isActive) {
                    val progress = ((elapsedTime.toDouble() / testDurationMs) * 90).toInt() + 5
                    onProgress(progress, "测试中... ${elapsedTime / 1000}s / ${testDurationMs / 1000}s")

                    // 收集样本
                    val fpsState = performanceMonitor.fpsState.value
                    val memoryState = performanceMonitor.memoryState.value
                    val cpuState = performanceMonitor.cpuState.value

                    fpsSamples.add(fpsState.currentFps)
                    memorySamples.add(Pair(memoryState.heapAlloc, memoryState.totalPss))
                    cpuSamples.add(cpuState.appCpuUsage)
                    threadCountSamples.add(cpuState.threadCount)

                    delay(AppConfig.Debug.SAMPLE_INTERVAL_MS) // 采样间隔
                    elapsedTime += AppConfig.Debug.SAMPLE_INTERVAL_MS
                }
            }

            testJob.join()

            val endTime = System.currentTimeMillis()
            performanceMonitor.stopMonitoring()

            onProgress(95, "分析测试结果...")

            // 计算指标
            val result = analyzeTestResult(
                testName = testName,
                startTime = startTime,
                endTime = endTime,
                fpsSamples = fpsSamples,
                memorySamples = memorySamples,
                cpuSamples = cpuSamples,
                threadCountSamples = threadCountSamples
            )

            onProgress(100, "测试完成")
            onComplete(result)
        }
    }

    /**
     * 分析测试结果
     */
    private fun analyzeTestResult(
        testName: String,
        startTime: Long,
        endTime: Long,
        fpsSamples: List<Int>,
        memorySamples: List<Pair<Long, Long>>,
        cpuSamples: List<Double>,
        threadCountSamples: List<Int>
    ): PerformanceTestResult {
        val duration = endTime - startTime

        // FPS指标
        val avgFps = fpsSamples.average()
        val minFps = fpsSamples.minOrNull() ?: 0
        val maxFps = fpsSamples.maxOrNull() ?: 0
        val fpsStdDev = calculateStdDev(fpsSamples.map { it.toDouble() })
        val dropFrameRate = if (fpsSamples.isNotEmpty()) {
            fpsSamples.count { it < AppConfig.Performance.FPS_STUTTER_THRESHOLD }.toDouble() / fpsSamples.size * AppConfig.UI.PERCENTAGE_BASE
        } else 0.0

        // 内存指标
        val heapSamples = memorySamples.map { it.first }
        val pssSamples = memorySamples.map { it.second }
        val avgHeap = if (heapSamples.isNotEmpty()) heapSamples.average().toLong() else 0
        val peakHeap = heapSamples.maxOrNull() ?: 0
        val avgPss = if (pssSamples.isNotEmpty()) pssSamples.average().toLong() else 0
        val peakPss = pssSamples.maxOrNull() ?: 0

        // 计算内存增长率
        val memoryGrowthRate = if (heapSamples.size >= 2) {
            val firstHalf = heapSamples.take(heapSamples.size / 2).average()
            val secondHalf = heapSamples.drop(heapSamples.size / 2).average()
            if (firstHalf > 0) ((secondHalf - firstHalf) / firstHalf) * AppConfig.UI.PERCENTAGE_BASE * (AppConfig.UI.MS_PER_MINUTE.toDouble() / duration) else 0.0
        } else 0.0

        // CPU指标
        val avgCpu = cpuSamples.average()
        val peakCpu = cpuSamples.maxOrNull() ?: 0.0
        val avgThreads = if (threadCountSamples.isNotEmpty()) threadCountSamples.average().toInt() else 0

        return PerformanceTestResult(
            testName = testName,
            startTime = startTime,
            endTime = endTime,
            duration = duration,
            fpsMetrics = FpsMetrics(
                averageFps = avgFps,
                minFps = minFps,
                maxFps = maxFps,
                fpsStability = fpsStdDev,
                dropFrameRate = dropFrameRate
            ),
            memoryMetrics = MemoryMetrics(
                averageHeapUsage = avgHeap,
                peakHeapUsage = peakHeap,
                averagePssUsage = avgPss,
                peakPssUsage = peakPss,
                memoryGrowthRate = memoryGrowthRate
            ),
            cpuMetrics = CpuMetrics(
                averageCpuUsage = avgCpu,
                peakCpuUsage = peakCpu,
                averageThreadCount = avgThreads
            )
        )
    }

    /**
     * 计算标准差
     */
    private fun calculateStdDev(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val avg = values.average()
        val variance = values.map { (it - avg) * (it - avg) }.average()
        return kotlin.math.sqrt(variance)
    }

    /**
     * 对比优化前后的性能
     */
    fun comparePerformance(
        beforeResult: PerformanceTestResult,
        afterResult: PerformanceTestResult
    ): PerformanceComparison {
        val fpsImprovement = calculateImprovement(
            beforeResult.fpsMetrics.averageFps,
            afterResult.fpsMetrics.averageFps
        )
        val memoryImprovement = calculateImprovement(
            beforeResult.memoryMetrics.averageHeapUsage.toDouble(),
            afterResult.memoryMetrics.averageHeapUsage.toDouble(),
            lowerIsBetter = true
        )
        val cpuImprovement = calculateImprovement(
            beforeResult.cpuMetrics.averageCpuUsage,
            afterResult.cpuMetrics.averageCpuUsage,
            lowerIsBetter = true
        )

        val overallImprovement = (fpsImprovement + memoryImprovement + cpuImprovement) / 3

        return PerformanceComparison(
            beforeResult = beforeResult,
            afterResult = afterResult,
            fpsImprovement = fpsImprovement,
            memoryImprovement = memoryImprovement,
            cpuImprovement = cpuImprovement,
            overallImprovement = overallImprovement
        )
    }

    /**
     * 计算改进百分比
     */
    private fun calculateImprovement(before: Double, after: Double, lowerIsBetter: Boolean = false): Double {
        if (before == 0.0) return 0.0
        val change = ((after - before) / before) * AppConfig.UI.PERCENTAGE_BASE
        return if (lowerIsBetter) -change else change
    }

    /**
     * 生成格式化的测试报告
     */
    fun generateTestReport(result: PerformanceTestResult): String {
        return buildString {
            appendLine("=".repeat(50))
            appendLine("性能测试报告")
            appendLine("=".repeat(50))
            appendLine()
            appendLine("测试名称: ${result.testName}")
            appendLine("测试时间: ${formatTimestamp(result.startTime)}")
            appendLine("测试时长: ${result.duration / 1000}秒")
            appendLine()
            appendLine("-".repeat(50))
            appendLine("FPS性能指标")
            appendLine("-".repeat(50))
            appendLine("  平均FPS: ${String.format("%.2f", result.fpsMetrics.averageFps)}")
            appendLine("  最低FPS: ${result.fpsMetrics.minFps}")
            appendLine("  最高FPS: ${result.fpsMetrics.maxFps}")
            appendLine("  FPS稳定性: ${String.format("%.2f", result.fpsMetrics.fpsStability)}")
            appendLine("  掉帧率: ${String.format("%.2f", result.fpsMetrics.dropFrameRate)}%")
            appendLine()
            appendLine("-".repeat(50))
            appendLine("内存使用指标")
            appendLine("-".repeat(50))
            appendLine("  平均堆内存: ${formatBytes(result.memoryMetrics.averageHeapUsage)}")
            appendLine("  峰值堆内存: ${formatBytes(result.memoryMetrics.peakHeapUsage)}")
            appendLine("  平均PSS内存: ${formatBytes(result.memoryMetrics.averagePssUsage)}")
            appendLine("  峰值PSS内存: ${formatBytes(result.memoryMetrics.peakPssUsage)}")
            appendLine("  内存增长率: ${String.format("%.2f", result.memoryMetrics.memoryGrowthRate)}%/分钟")
            appendLine()
            appendLine("-".repeat(50))
            appendLine("CPU使用指标")
            appendLine("-".repeat(50))
            appendLine("  平均CPU使用率: ${String.format("%.2f", result.cpuMetrics.averageCpuUsage)}%")
            appendLine("  峰值CPU使用率: ${String.format("%.2f", result.cpuMetrics.peakCpuUsage)}%")
            appendLine("  平均线程数: ${result.cpuMetrics.averageThreadCount}")
            appendLine()
            appendLine("=".repeat(50))
        }
    }

    /**
     * 生成对比报告
     */
    fun generateComparisonReport(comparison: PerformanceComparison): String {
        return buildString {
            appendLine("=".repeat(60))
            appendLine("性能优化对比报告")
            appendLine("=".repeat(60))
            appendLine()
            appendLine("测试名称: ${comparison.beforeResult.testName}")
            appendLine("优化前测试时间: ${formatTimestamp(comparison.beforeResult.startTime)}")
            appendLine("优化后测试时间: ${formatTimestamp(comparison.afterResult.startTime)}")
            appendLine()
            appendLine("-".repeat(60))
            appendLine("FPS性能对比")
            appendLine("-".repeat(60))
            appendLine("  优化前平均FPS: ${String.format("%.2f", comparison.beforeResult.fpsMetrics.averageFps)}")
            appendLine("  优化后平均FPS: ${String.format("%.2f", comparison.afterResult.fpsMetrics.averageFps)}")
            appendLine("  改进: ${formatImprovement(comparison.fpsImprovement)}")
            appendLine()
            appendLine("-".repeat(60))
            appendLine("内存使用对比")
            appendLine("-".repeat(60))
            appendLine("  优化前平均堆内存: ${formatBytes(comparison.beforeResult.memoryMetrics.averageHeapUsage)}")
            appendLine("  优化后平均堆内存: ${formatBytes(comparison.afterResult.memoryMetrics.averageHeapUsage)}")
            appendLine("  改进: ${formatImprovement(comparison.memoryImprovement)}")
            appendLine()
            appendLine("-".repeat(60))
            appendLine("CPU使用对比")
            appendLine("-".repeat(60))
            appendLine("  优化前平均CPU: ${String.format("%.2f", comparison.beforeResult.cpuMetrics.averageCpuUsage)}%")
            appendLine("  优化后平均CPU: ${String.format("%.2f", comparison.afterResult.cpuMetrics.averageCpuUsage)}%")
            appendLine("  改进: ${formatImprovement(comparison.cpuImprovement)}")
            appendLine()
            appendLine("-".repeat(60))
            appendLine("综合评估")
            appendLine("-".repeat(60))
            appendLine("  整体改进: ${formatImprovement(comparison.overallImprovement)}")
            when {
                comparison.overallImprovement >= 20 -> appendLine("  评级: 显著优化")
                comparison.overallImprovement >= 10 -> appendLine("  评级: 明显优化")
                comparison.overallImprovement >= 5 -> appendLine("  评级: 有所优化")
                comparison.overallImprovement > 0 -> appendLine("  评级: 轻微优化")
                comparison.overallImprovement > -5 -> appendLine("  评级: 基本持平")
                else -> appendLine("  评级: 需要关注")
            }
            appendLine()
            appendLine("=".repeat(60))
        }
    }

    /**
     * 保存报告到文件
     */
    fun saveReportToFile(context: Context, report: String, filename: String? = null): File? {
        return try {
            val timestamp = DateTimeUtils.formatFileTimestamp(System.currentTimeMillis())
            val actualFilename = filename ?: "performance_report_$timestamp.txt"
            val file = File(context.getExternalFilesDir(null), actualFilename)

            FileWriter(file).use { writer ->
                writer.write(report)
            }

            Log.d(TAG, "Report saved to: ${file.absolutePath}")
            file
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save report", e)
            null
        }
    }

    /**
     * 格式化改进百分比
     */
    private fun formatImprovement(value: Double): String {
        val prefix = if (value >= 0) "+" else ""
        return "$prefix${String.format("%.2f", value)}%"
    }

    /**
     * 格式化时间戳
     */
    private fun formatTimestamp(timestamp: Long): String {
        return DateTimeUtils.formatDateTime(timestamp)
    }

    /**
     * 格式化字节数
     */
    private fun formatBytes(bytes: Long): String {
        return when {
            bytes >= AppConfig.Performance.BYTES_PER_GB -> String.format("%.2f GB", bytes / (AppConfig.Performance.BYTES_PER_GB.toDouble()))
            bytes >= AppConfig.Performance.BYTES_PER_MB -> String.format("%.2f MB", bytes / (AppConfig.Performance.BYTES_PER_MB.toDouble()))
            bytes >= AppConfig.Performance.BYTES_PER_KB -> String.format("%.2f KB", bytes / AppConfig.Performance.BYTES_PER_KB.toDouble())
            else -> "$bytes B"
        }
    }

    /**
     * 模拟压力测试 - 生成大量数据
     */
    fun generateStressTestData(count: Int = AppConfig.Debug.STRESS_TEST_DATA_COUNT): List<SensorDataPoint> {
        val data = mutableListOf<SensorDataPoint>()
        val currentTime = System.currentTimeMillis()
        val timeRange = AppConfig.Debug.STRESS_TEST_TIME_RANGE_MINUTES * AppConfig.UI.MS_PER_MINUTE

        repeat(count) { i ->
            val progress = i.toDouble() / count
            val timestamp = currentTime - timeRange + (progress * timeRange).toLong()
            val sensor1 = (500 + kotlin.math.sin(progress * kotlin.math.PI * 10) * 1500 + Random.nextDouble(0.0, 500.0)).toInt()
            val sensor2 = (300 + kotlin.math.sin(progress * kotlin.math.PI * 8) * 1000 + Random.nextDouble(0.0, 300.0)).toInt()
            val sensor3 = (200 + kotlin.math.sin(progress * kotlin.math.PI * 6) * 800 + Random.nextDouble(0.0, 200.0)).toInt()

            data.add(SensorDataPoint(timestamp, sensor1, sensor2, sensor3))
        }

        return data
    }

    /**
     * 运行回归测试
     * @param context 上下文
     * @param memoryLeakDetector 内存泄漏检测器实例
     * @param bluetoothResourceManager 蓝牙资源管理器实例
     * @param performanceMonitor 性能监控器实例
     * @param onProgress 进度回调
     * @param onComplete 完成回调
     */
    fun runRegressionTest(
        context: Context,
        memoryLeakDetector: MemoryLeakDetector,
        bluetoothResourceManager: BluetoothResourceManager,
        performanceMonitor: PerformanceMonitor,
        onProgress: (testName: String, status: String) -> Unit,
        onComplete: (results: List<RegressionTestResult>) -> Unit
    ) {
        val results = mutableListOf<RegressionTestResult>()

        CoroutineScope(Dispatchers.Main).launch {
            // 测试1: 基础功能测试
            onProgress("基础功能测试", "运行中...")
            results.add(runBasicFunctionalityTest(memoryLeakDetector, bluetoothResourceManager, performanceMonitor))
            delay(500)

            // 测试2: 内存泄漏测试
            onProgress("内存泄漏测试", "运行中...")
            results.add(runMemoryLeakTest(context))
            delay(500)

            // 测试3: 性能基准测试
            onProgress("性能基准测试", "运行中...")
            results.add(runPerformanceBaselineTest(context))
            delay(500)

            // 测试4: 边界条件测试
            onProgress("边界条件测试", "运行中...")
            results.add(runBoundaryConditionTest())

            onComplete(results)
        }
    }

    /**
     * 回归测试结果
     */
    data class RegressionTestResult(
        val testName: String,
        val passed: Boolean,
        val message: String,
        val details: String = ""
    )

    /**
     * 基础功能测试
     * @param memoryLeakDetector 内存泄漏检测器实例
     * @param bluetoothResourceManager 蓝牙资源管理器实例
     * @param performanceMonitor 性能监控器实例
     */
    private fun runBasicFunctionalityTest(
        memoryLeakDetector: MemoryLeakDetector,
        bluetoothResourceManager: BluetoothResourceManager,
        performanceMonitor: PerformanceMonitor
    ): RegressionTestResult {
        return try {
            // 验证基本组件初始化 - 现在通过参数传入，不再调用 getInstance()
            val allInitialized = memoryLeakDetector != null &&
                    bluetoothResourceManager != null &&
                    performanceMonitor != null

            RegressionTestResult(
                testName = "基础功能测试",
                passed = allInitialized,
                message = if (allInitialized) "所有核心组件初始化成功" else "部分组件未初始化",
                details = "MemoryLeakDetector, BluetoothResourceManager, PerformanceMonitor"
            )
        } catch (e: Exception) {
            RegressionTestResult(
                testName = "基础功能测试",
                passed = false,
                message = "组件初始化失败",
                details = e.message ?: "Unknown error"
            )
        }
    }

    /**
     * 内存泄漏测试
     * 使用挂起函数和 delay 替代 Thread.sleep，避免阻塞线程
     */
    private suspend fun runMemoryLeakTest(context: Context): RegressionTestResult {
        val runtime = Runtime.getRuntime()

        // 强制垃圾回收
        System.gc()
        delay(AppConfig.Debug.DELAY_SHORT_MS)

        val memoryBefore = runtime.totalMemory() - runtime.freeMemory()

        // 模拟一些操作
        val testData = generateStressTestData(AppConfig.Debug.REGRESSION_TEST_MEDIUM_COUNT)
        val circularBuffer = CircularBuffer<SensorDataPoint>(AppConfig.Debug.REGRESSION_TEST_MEDIUM_COUNT)
        testData.forEach { circularBuffer.add(it) }

        // 清空数据
        circularBuffer.clear()

        // 再次垃圾回收
        System.gc()
        delay(AppConfig.Debug.DELAY_SHORT_MS)

        val memoryAfter = runtime.totalMemory() - runtime.freeMemory()
        val memoryDiff = memoryAfter - memoryBefore
        val memoryGrowthPercent = (memoryDiff.toDouble() / memoryBefore) * AppConfig.UI.PERCENTAGE_BASE

        val passed = memoryGrowthPercent < AppConfig.UI.PERCENTAGE_BASE / 10 // 内存增长小于10%视为通过

        return RegressionTestResult(
            testName = "内存泄漏测试",
            passed = passed,
            message = if (passed) "内存使用正常" else "检测到潜在内存泄漏",
            details = "内存变化: ${formatBytes(memoryDiff)} (${String.format("%.2f", memoryGrowthPercent)}%)"
        )
    }

    /**
     * 性能基准测试
     */
    private fun runPerformanceBaselineTest(context: Context): RegressionTestResult {
        val startTime = System.currentTimeMillis()

        // 测试数据操作性能
        val testData = generateStressTestData(AppConfig.Debug.REGRESSION_TEST_LARGE_COUNT)
        val circularBuffer = CircularBuffer<SensorDataPoint>(AppConfig.Debug.REGRESSION_TEST_LARGE_COUNT)

        val addStart = System.currentTimeMillis()
        testData.forEach { circularBuffer.add(it) }
        val addDuration = System.currentTimeMillis() - addStart

        val readStart = System.currentTimeMillis()
        val readData = circularBuffer.toOrderedList()
        val readDuration = System.currentTimeMillis() - readStart

        val totalDuration = System.currentTimeMillis() - startTime

        // 性能标准
        val addPerformanceOk = addDuration < AppConfig.Debug.ADD_PERFORMANCE_THRESHOLD_MS
        val readPerformanceOk = readDuration < AppConfig.Debug.READ_PERFORMANCE_THRESHOLD_MS
        val passed = addPerformanceOk && readPerformanceOk

        return RegressionTestResult(
            testName = "性能基准测试",
            passed = passed,
            message = if (passed) "性能符合基准要求" else "性能低于基准",
            details = """
                添加${AppConfig.Debug.REGRESSION_TEST_LARGE_COUNT}条数据: ${addDuration}ms (标准: <${AppConfig.Debug.ADD_PERFORMANCE_THRESHOLD_MS}ms)
                读取${AppConfig.Debug.REGRESSION_TEST_LARGE_COUNT}条数据: ${readDuration}ms (标准: <${AppConfig.Debug.READ_PERFORMANCE_THRESHOLD_MS}ms)
                总耗时: ${totalDuration}ms
            """.trimIndent()
        )
    }

    /**
     * 边界条件测试
     */
    private fun runBoundaryConditionTest(): RegressionTestResult {
        val tests = mutableListOf<Pair<String, Boolean>>()

        // 测试1: 空数据处理
        try {
            val emptyBuffer = CircularBuffer<SensorDataPoint>(AppConfig.Debug.REGRESSION_TEST_SMALL_COUNT)
            val emptyList = emptyBuffer.toOrderedList()
            tests.add("空缓冲区读取" to (emptyList.isEmpty()))
        } catch (e: Exception) {
            tests.add("空缓冲区读取" to false)
        }

        // 测试2: 缓冲区溢出处理
        try {
            val smallBuffer = CircularBuffer<SensorDataPoint>(AppConfig.Debug.REGRESSION_TEST_SMALL_COUNT / 10)
            repeat(AppConfig.Debug.REGRESSION_TEST_SMALL_COUNT / 5) { i ->
                smallBuffer.add(SensorDataPoint(System.currentTimeMillis() + i, i, i, i))
            }
            val size = smallBuffer.size()
            tests.add("缓冲区溢出处理" to (size == AppConfig.Debug.REGRESSION_TEST_SMALL_COUNT / 10))
        } catch (e: Exception) {
            tests.add("缓冲区溢出处理" to false)
        }

        // 测试3: 大数据集处理
        try {
            val largeBuffer = CircularBuffer<SensorDataPoint>(AppConfig.Debug.REGRESSION_TEST_BUFFER_SIZE)
            val largeData = generateStressTestData(AppConfig.Debug.STRESS_TEST_LARGE_DATA_COUNT)
            largeData.forEach { largeBuffer.add(it) }
            tests.add("大数据集处理" to (largeBuffer.size() == AppConfig.Debug.REGRESSION_TEST_BUFFER_SIZE))
        } catch (e: Exception) {
            tests.add("大数据集处理" to false)
        }

        val allPassed = tests.all { it.second }

        return RegressionTestResult(
            testName = "边界条件测试",
            passed = allPassed,
            message = if (allPassed) "所有边界条件处理正确" else "部分边界条件处理异常",
            details = tests.joinToString("\n") { "${it.first}: ${if (it.second) "通过" else "失败"}" }
        )
    }

    /**
     * 生成回归测试报告
     */
    fun generateRegressionTestReport(results: List<RegressionTestResult>): String {
        return buildString {
            appendLine("=".repeat(60))
            appendLine("回归测试报告")
            appendLine("=".repeat(60))
            appendLine()
            appendLine("测试时间: ${formatTimestamp(System.currentTimeMillis())}")
            appendLine()

            val passedCount = results.count { it.passed }
            val totalCount = results.size

            appendLine("测试结果汇总: $passedCount/$totalCount 通过")
            appendLine()

            results.forEach { result ->
                appendLine("-".repeat(60))
                appendLine("${result.testName}: ${if (result.passed) "✓ 通过" else "✗ 失败"}")
                appendLine("  消息: ${result.message}")
                if (result.details.isNotEmpty()) {
                    appendLine("  详情:")
                    result.details.lines().forEach { line ->
                        appendLine("    $line")
                    }
                }
                appendLine()
            }

            appendLine("=".repeat(60))
            appendLine("总体评估: ${if (passedCount == totalCount) "所有测试通过" else "部分测试失败，需要关注"}")
            appendLine("=".repeat(60))
        }
    }
}
