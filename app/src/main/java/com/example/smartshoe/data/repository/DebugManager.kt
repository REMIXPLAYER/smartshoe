package com.example.smartshoe.data.repository

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartshoe.UIConstants

/**
 * 调试管理器
 * 封装调试功能UI和性能测试逻辑
 */
class DebugManager(
    private val context: Context,
    private val sensorDataProcessor: SensorDataProcessor
) {

    /**
     * 生成模拟数据
     */
    fun generateMockData(onComplete: () -> Unit = {}) {
        sensorDataProcessor.generateMockData()
        onComplete()
    }

    /**
     * 运行回归测试
     */
    fun runRegressionTest(
        onProgress: (String, String) -> Unit,
        onComplete: (List<PerformanceTestUtils.RegressionTestResult>) -> Unit
    ) {
        PerformanceTestUtils.runRegressionTest(
            context = context,
            onProgress = onProgress,
            onComplete = onComplete
        )
    }

    /**
     * 运行性能测试
     */
    fun runPerformanceTest(
        onProgress: (Int, String) -> Unit,
        onComplete: (PerformanceTestUtils.PerformanceTestResult) -> Unit
    ) {
        PerformanceTestUtils.runPerformanceTest(
            context = context,
            testName = "SmartShoe性能基准测试",
            onProgress = onProgress,
            onComplete = onComplete
        )
    }

    /**
     * 生成回归测试报告
     */
    fun generateRegressionTestReport(results: List<PerformanceTestUtils.RegressionTestResult>): String {
        return PerformanceTestUtils.generateRegressionTestReport(results)
    }

    /**
     * 生成性能测试报告
     */
    fun generatePerformanceTestReport(result: PerformanceTestUtils.PerformanceTestResult): String {
        return PerformanceTestUtils.generateTestReport(result)
    }

    /**
     * 保存报告到文件
     */
    fun saveReportToFile(report: String) {
        PerformanceTestUtils.saveReportToFile(context, report)
    }
}

/**
 * 调试功能区域 Composable
 * @param onGenerateMockData 生成模拟数据回调
 * @param onRunPerformanceTest 运行性能测试回调
 * @param onRunRegressionTest 运行回归测试回调
 */
@Composable
fun DebugSection(
    onGenerateMockData: () -> Unit,
    onRunPerformanceTest: () -> Unit = {},
    onRunRegressionTest: () -> Unit = {}
) {
    var showPerformanceTestDialog by remember { mutableStateOf(false) }
    var showRegressionTestDialog by remember { mutableStateOf(false) }
    var testProgress by remember { mutableIntStateOf(0) }
    var testMessage by remember { mutableStateOf("") }
    var testReport by remember { mutableStateOf("") }

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = UIConstants.SurfaceColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "调试功能",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = UIConstants.PrimaryColor
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 生成模拟数据按钮
            Button(
                onClick = onGenerateMockData,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF9C27B0)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Build,
                    contentDescription = "生成数据",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("生成模拟传感器数据(15分钟)", fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "用于调试数据备份功能，无需连接蓝牙设备",
                fontSize = 12.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))

            Spacer(modifier = Modifier.height(12.dp))

            // 性能测试按钮
            Button(
                onClick = { showPerformanceTestDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2196F3)
                )
            ) {
                Text("运行性能基准测试", fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 回归测试按钮
            Button(
                onClick = {
                    showRegressionTestDialog = true
                    testProgress = 0
                    testMessage = "准备运行回归测试..."
                    testReport = ""
                    onRunRegressionTest()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                )
            ) {
                Text("运行回归测试", fontSize = 14.sp)
            }
        }
    }

    // 性能测试对话框
    if (showPerformanceTestDialog) {
        PerformanceTestDialogContent(
            onDismiss = { showPerformanceTestDialog = false },
            onStartTest = onRunPerformanceTest
        )
    }

    // 回归测试报告对话框
    if (showRegressionTestDialog && testReport.isNotEmpty()) {
        RegressionTestReportDialog(
            report = testReport,
            onDismiss = { showRegressionTestDialog = false }
        )
    }
}

/**
 * 性能测试对话框内容
 */
@Composable
private fun PerformanceTestDialogContent(
    onDismiss: () -> Unit,
    onStartTest: () -> Unit
) {
    var isRunning by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf("") }
    var report by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!isRunning) onDismiss() },
        title = {
            Text(
                text = "性能基准测试",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = UIConstants.PrimaryColor
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (isRunning) {
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = message, fontSize = 14.sp, color = Color.Gray)
                } else if (report.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                    ) {
                        Text(
                            text = report,
                            fontSize = 12.sp,
                            color = Color.DarkGray,
                            lineHeight = 16.sp
                        )
                    }
                } else {
                    Text(
                        text = "点击开始运行性能基准测试，测试将持续约60秒。",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }
        },
        confirmButton = {
            if (!isRunning && report.isEmpty()) {
                Button(
                    onClick = {
                        isRunning = true
                        onStartTest()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = UIConstants.PrimaryColor
                    )
                ) {
                    Text("开始测试")
                }
            } else if (report.isNotEmpty()) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = UIConstants.PrimaryColor
                    )
                ) {
                    Text("关闭")
                }
            }
        },
        dismissButton = {
            if (!isRunning) {
                Button(onClick = onDismiss) {
                    Text("取消")
                }
            }
        }
    )
}

/**
 * 回归测试报告对话框
 */
@Composable
private fun RegressionTestReportDialog(
    report: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "回归测试报告",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = UIConstants.PrimaryColor
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
                    containerColor = UIConstants.PrimaryColor
                )
            ) {
                Text("关闭")
            }
        }
    )
}
