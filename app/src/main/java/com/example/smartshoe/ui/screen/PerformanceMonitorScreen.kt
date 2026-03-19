package com.example.smartshoe.ui.screen

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import com.example.smartshoe.util.AnimationDefaults
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartshoe.data.manager.PerformanceMonitor
import com.example.smartshoe.ui.theme.AppColors
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 性能监控面板组件
 * 在Debug模式下显示实时性能数据
 *
 * 重构：PerformanceMonitor 通过参数传入，由调用方使用 Hilt 注入
 */
object PerformanceMonitorScreen {

    /**
     * 性能监控面板主组件
     * @param context 上下文
     * @param performanceMonitor 性能监控器实例（由调用方通过 Hilt 注入）
     * @param modifier 修饰符
     * @param isExpanded 是否默认展开
     */
    @Composable
    fun PerformanceMonitorPanel(
        context: Context,
        performanceMonitor: PerformanceMonitor,
        modifier: Modifier = Modifier,
        isExpanded: Boolean = true
    ) {
        var fpsState by remember { mutableStateOf(PerformanceMonitor.FpsState()) }
        var memoryState by remember { mutableStateOf(PerformanceMonitor.MemoryState()) }
        var cpuState by remember { mutableStateOf(PerformanceMonitor.CpuState()) }
        var report by remember { mutableStateOf(PerformanceMonitor.PerformanceReport()) }
        var isPanelExpanded by remember { mutableStateOf(isExpanded) }
        var showReportDialog by remember { mutableStateOf(false) }

        // 收集性能数据
        LaunchedEffect(Unit) {
            performanceMonitor.startMonitoring(context)

            // 收集FPS状态
            launch {
                performanceMonitor.fpsState.collectLatest { state ->
                    fpsState = state
                }
            }

            // 收集内存状态
            launch {
                performanceMonitor.memoryState.collectLatest { state ->
                    memoryState = state
                }
            }

            // 收集CPU状态
            launch {
                performanceMonitor.cpuState.collectLatest { state ->
                    cpuState = state
                }
            }

            // 收集性能报告
            launch {
                performanceMonitor.performanceReport.collectLatest { r ->
                    report = r
                }
            }
        }

        // 清理
        DisposableEffect(Unit) {
            onDispose {
                performanceMonitor.stopMonitoring()
            }
        }

        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // 标题栏
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isPanelExpanded = !isPanelExpanded },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 状态指示器
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    color = if (fpsState.isRunning) Color(0xFF4CAF50) else Color(0xFFF44336),
                                    shape = RoundedCornerShape(4.dp)
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "性能监控",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Row {
                        // 生成报告按钮
                        TextButton(
                            onClick = {
                                performanceMonitor.stopMonitoring()
                                showReportDialog = true
                            },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = "报告",
                                fontSize = 12.sp,
                                color = Color(0xFF64B5F6)
                            )
                        }

                        Text(
                            text = if (isPanelExpanded) "▼" else "▶",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }

                // 展开内容
                AnimatedVisibility(
                    visible = isPanelExpanded,
                    enter = expandVertically(animationSpec = AnimationDefaults.expandTween),
                    exit = shrinkVertically(animationSpec = AnimationDefaults.shrinkTween)
                ) {
                    Column(
                        modifier = Modifier.padding(top = 12.dp)
                    ) {
                        // FPS指标
                        FpsIndicator(fpsState)

                        Spacer(modifier = Modifier.height(8.dp))

                        // 内存指标
                        MemoryIndicator(memoryState)

                        Spacer(modifier = Modifier.height(8.dp))

                        // CPU指标
                        CpuIndicator(cpuState)

                        Spacer(modifier = Modifier.height(8.dp))

                        // 综合评分
                        OverallScoreIndicator(report.overallScore)
                    }
                }
            }
        }

        // 性能报告对话框
        if (showReportDialog) {
            PerformanceReportDialog(
                report = performanceMonitor.getFormattedReport(),
                onDismiss = {
                    showReportDialog = false
                    performanceMonitor.startMonitoring(context)
                }
            )
        }
    }

    /**
     * FPS指示器
     */
    @Composable
    private fun FpsIndicator(fpsState: PerformanceMonitor.FpsState) {
        val fpsColor = when {
            fpsState.currentFps >= 55 -> Color(0xFF4CAF50)
            fpsState.currentFps >= 45 -> Color(0xFFFFC107)
            else -> Color(0xFFF44336)
        }

        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "FPS",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = "${fpsState.currentFps}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = fpsColor
                )
            }

            // FPS进度条
            LinearProgressIndicator(
                progress = { fpsState.currentFps / 60f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .padding(top = 4.dp),
                color = fpsColor,
                trackColor = Color(0xFF333333)
            )

            // FPS统计
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Avg: ${String.format("%.1f", fpsState.averageFps)}",
                    fontSize = 10.sp,
                    color = Color(0xFFAAAAAA)
                )
                Text(
                    text = "Min: ${fpsState.minFps}",
                    fontSize = 10.sp,
                    color = Color(0xFFAAAAAA)
                )
                Text(
                    text = "Max: ${fpsState.maxFps}",
                    fontSize = 10.sp,
                    color = Color(0xFFAAAAAA)
                )
                Text(
                    text = "Drops: ${fpsState.fpsDropCount}",
                    fontSize = 10.sp,
                    color = if (fpsState.fpsDropCount > 0) Color(0xFFF44336) else Color(0xFFAAAAAA)
                )
            }
        }
    }

    /**
     * 内存指示器
     */
    @Composable
    private fun MemoryIndicator(memoryState: PerformanceMonitor.MemoryState) {
        val memoryColor = when {
            memoryState.heapAlloc < 50 * 1024 * 1024 -> Color(0xFF4CAF50) // < 50MB
            memoryState.heapAlloc < 100 * 1024 * 1024 -> Color(0xFFFFC107) // < 100MB
            else -> Color(0xFFF44336)
        }

        val heapSizeMB = memoryState.heapSize / (1024.0 * 1024.0)
        val heapAllocMB = memoryState.heapAlloc / (1024.0 * 1024.0)
        val pssMB = memoryState.totalPss / (1024.0 * 1024.0)

        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "内存",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = String.format("%.1f MB", heapAllocMB),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = memoryColor
                )
            }

            // 内存进度条
            val progress = if (memoryState.heapSize > 0) {
                (memoryState.heapAlloc.toFloat() / memoryState.heapSize.toFloat()).coerceIn(0f, 1f)
            } else 0f

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .padding(top = 4.dp),
                color = memoryColor,
                trackColor = Color(0xFF333333)
            )

            // 内存统计
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = String.format("Heap: %.1f/%.1f MB", heapAllocMB, heapSizeMB),
                    fontSize = 10.sp,
                    color = Color(0xFFAAAAAA)
                )
                Text(
                    text = String.format("PSS: %.1f MB", pssMB),
                    fontSize = 10.sp,
                    color = Color(0xFFAAAAAA)
                )
            }
        }
    }

    /**
     * CPU指示器
     */
    @Composable
    private fun CpuIndicator(cpuState: PerformanceMonitor.CpuState) {
        val cpuColor = when {
            cpuState.appCpuUsage < 10 -> Color(0xFF4CAF50)
            cpuState.appCpuUsage < 30 -> Color(0xFFFFC107)
            else -> Color(0xFFF44336)
        }

        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CPU",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = String.format("%.1f%%", cpuState.appCpuUsage),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = cpuColor
                )
            }

            // CPU进度条
            LinearProgressIndicator(
                progress = { (cpuState.appCpuUsage / 100f).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .padding(top = 4.dp),
                color = cpuColor,
                trackColor = Color(0xFF333333)
            )

            // CPU统计
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = String.format("System: %.1f%%", cpuState.systemCpuUsage),
                    fontSize = 10.sp,
                    color = Color(0xFFAAAAAA)
                )
                Text(
                    text = "Threads: ${cpuState.threadCount}",
                    fontSize = 10.sp,
                    color = Color(0xFFAAAAAA)
                )
            }
        }
    }

    /**
     * 综合评分指示器
     */
    @Composable
    private fun OverallScoreIndicator(score: Int) {
        val scoreColor = when {
            score >= 90 -> Color(0xFF4CAF50)
            score >= 75 -> Color(0xFF8BC34A)
            score >= 60 -> Color(0xFFFFC107)
            score >= 40 -> Color(0xFFFF9800)
            else -> Color(0xFFF44336)
        }

        val scoreText = when {
            score >= 90 -> "优秀"
            score >= 75 -> "良好"
            score >= 60 -> "及格"
            else -> "需优化"
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "综合评分",
                fontSize = 12.sp,
                color = Color.Gray
            )
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$score",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = scoreColor
                )
                Text(
                    text = "/100 ($scoreText)",
                    fontSize = 12.sp,
                    color = Color(0xFFAAAAAA),
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }

    /**
     * 性能报告对话框
     */
    @Composable
    private fun PerformanceReportDialog(
        report: String,
        onDismiss: () -> Unit
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = "性能测试报告",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Primary
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                ) {
                    Text(
                        text = report,
                        fontSize = 13.sp,
                        color = Color.DarkGray,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.Primary
                    )
                ) {
                    Text("关闭")
                }
            }
        )
    }
}
