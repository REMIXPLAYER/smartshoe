package com.example.smartshoe.ui.screen.datarecord

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.smartshoe.domain.model.SensorDataPoint
import com.example.smartshoe.ui.component.chart.ChartConfigUtils
import com.example.smartshoe.ui.theme.AppColors
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.listener.OnChartValueSelectedListener

/**
 * 实时图表显示的最大数据点数量
 * 限制数据点数量可减少MPAndroidChart的渲染开销
 * 500点 ≈ 50秒数据（按100ms间隔计算），足够实时观察
 */
private const val MAX_REALTIME_CHART_POINTS = 500

/**
 * 使用MPAndroidChart的图表组件 - 支持拖动查看和点击显示选中值
 * 用户点击/拖动时显示选中值，点击其他地方或2秒后自动归位到最新值
 *
 * 性能优化：
 * 1. 使用derivedStateOf缓存图表数据，避免每次重组重新计算
 * 2. 使用remember缓存entries和lineData，减少对象创建
 * 3. 优化AndroidView update调用频率，仅在数据变化时更新
 * 4. 限制实时模式数据点数量，减少图表渲染开销
 */
@Composable
fun MPLineChart(
    data: List<SensorDataPoint>,
    sensorIndex: Int,
    sensorName: String,
    modifier: Modifier = Modifier
) {
    var selectedValue by remember { mutableStateOf<Int?>(null) }
    var isUserInteracting by remember { mutableStateOf(false) }
    var lastInteractionTime by remember { mutableStateOf(0L) }

    // 优化：限制实时图表数据点数量，减少渲染开销
    val displayData by remember(data) {
        derivedStateOf {
            if (data.size > MAX_REALTIME_CHART_POINTS) {
                data.takeLast(MAX_REALTIME_CHART_POINTS)
            } else {
                data
            }
        }
    }

    val sensorValueExtractor = remember(sensorIndex) {
        { point: SensorDataPoint ->
            point.getValue(sensorIndex)
        }
    }

    val latestValue by remember(displayData) {
        derivedStateOf {
            displayData.lastOrNull()?.let(sensorValueExtractor) ?: 0
        }
    }

    val displayValue by remember(displayData, selectedValue) {
        derivedStateOf {
            selectedValue ?: latestValue
        }
    }

    val firstTimestamp by remember(displayData) {
        derivedStateOf { displayData.firstOrNull()?.timestamp ?: 0L }
    }
    val lastTimestamp by remember(displayData) {
        derivedStateOf { displayData.lastOrNull()?.timestamp ?: firstTimestamp }
    }

    val realtimeBaseTimestamp by remember(displayData) {
        derivedStateOf {
            val last = displayData.lastOrNull()?.timestamp ?: 0L
            val twoMinutesAgo = last - 120000L
            displayData.firstOrNull { it.timestamp >= twoMinutesAgo }?.timestamp ?: firstTimestamp
        }
    }

    LaunchedEffect(displayData) {
        if (!isUserInteracting && displayData.isNotEmpty()) {
            selectedValue = sensorValueExtractor(displayData.last())
        }
    }

    LaunchedEffect(isUserInteracting, lastInteractionTime) {
        if (isUserInteracting) {
            kotlinx.coroutines.delay(2000)
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastInteractionTime >= 2000) {
                isUserInteracting = false
                if (displayData.isNotEmpty()) {
                    selectedValue = sensorValueExtractor(displayData.last())
                }
            }
        }
    }

    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = sensorName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Primary
                )

                Text(
                    text = if (isUserInteracting && selectedValue != null) "选中: $displayValue" else "当前: $displayValue",
                    fontSize = 12.sp,
                    color = if (isUserInteracting && selectedValue != null) AppColors.Primary else AppColors.PlaceholderText,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { context ->
                        LineChart(context).apply {
                            ChartConfigUtils.setupChartStyle(
                                this,
                                firstTimestamp,
                                lastTimestamp,
                                ChartConfigUtils.ChartType.REALTIME
                            )
                            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                                override fun onValueSelected(e: com.github.mikephil.charting.data.Entry?, h: com.github.mikephil.charting.highlight.Highlight?) {
                                    e?.let { entry ->
                                        isUserInteracting = true
                                        lastInteractionTime = System.currentTimeMillis()
                                        val closestPoint = ChartConfigUtils.findClosestDataPoint(entry, displayData, realtimeBaseTimestamp)
                                        selectedValue = closestPoint?.let { sensorValueExtractor(it) }
                                    }
                                }
                                override fun onNothingSelected() {
                                    lastInteractionTime = System.currentTimeMillis()
                                }
                            })
                            if (displayData.isEmpty()) {
                                ChartConfigUtils.setupEmptyChart(this)
                            }
                        }
                    },
                    update = { chart ->
                        if (displayData.isNotEmpty()) {
                            ChartConfigUtils.updateChartData(
                                chart,
                                displayData,
                                sensorIndex,
                                firstTimestamp,
                                ChartConfigUtils.SensorColors.getColor(sensorIndex),
                                ChartConfigUtils.ChartType.REALTIME
                            )
                            if (!isUserInteracting) {
                                chart.moveViewToX(chart.data.getDataSetByIndex(0).xMax)
                            }
                        } else {
                            ChartConfigUtils.setupEmptyChart(chart)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                if (displayData.isEmpty()) {
                    Text(
                        text = "暂无数据",
                        fontSize = 14.sp,
                        color = AppColors.DisabledText,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
