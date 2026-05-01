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
 * 使用MPAndroidChart的图表组件 - 支持拖动查看和点击显示选中值
 * 用户点击/拖动时显示选中值，点击其他地方或2秒后自动归位到最新值
 *
 * 性能优化：
 * 1. 使用derivedStateOf缓存图表数据，避免每次重组重新计算
 * 2. 使用remember缓存entries和lineData，减少对象创建
 * 3. 优化AndroidView update调用频率，仅在数据变化时更新
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

    val sensorValueExtractor = remember(sensorIndex) {
        { point: SensorDataPoint ->
            point.getValue(sensorIndex)
        }
    }

    val latestValue by remember(data) {
        derivedStateOf {
            data.lastOrNull()?.let(sensorValueExtractor) ?: 0
        }
    }

    val displayValue by remember(data, selectedValue) {
        derivedStateOf {
            selectedValue ?: latestValue
        }
    }

    val firstTimestamp by remember(data) {
        derivedStateOf { data.firstOrNull()?.timestamp ?: 0L }
    }
    val lastTimestamp by remember(data) {
        derivedStateOf { data.lastOrNull()?.timestamp ?: firstTimestamp }
    }

    val realtimeBaseTimestamp by remember(data) {
        derivedStateOf {
            val last = data.lastOrNull()?.timestamp ?: 0L
            val twoMinutesAgo = last - 120000L
            data.firstOrNull { it.timestamp >= twoMinutesAgo }?.timestamp ?: firstTimestamp
        }
    }

    LaunchedEffect(data) {
        if (!isUserInteracting && data.isNotEmpty()) {
            selectedValue = sensorValueExtractor(data.last())
        }
    }

    LaunchedEffect(isUserInteracting, lastInteractionTime) {
        if (isUserInteracting) {
            kotlinx.coroutines.delay(2000)
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastInteractionTime >= 2000) {
                isUserInteracting = false
                if (data.isNotEmpty()) {
                    selectedValue = sensorValueExtractor(data.last())
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
                                        val closestPoint = ChartConfigUtils.findClosestDataPoint(entry, data, realtimeBaseTimestamp)
                                        selectedValue = closestPoint?.let { sensorValueExtractor(it) }
                                    }
                                }
                                override fun onNothingSelected() {
                                    lastInteractionTime = System.currentTimeMillis()
                                }
                            })
                            if (data.isEmpty()) {
                                ChartConfigUtils.setupEmptyChart(this)
                            }
                        }
                    },
                    update = { chart ->
                        if (data.isNotEmpty()) {
                            ChartConfigUtils.updateChartData(
                                chart,
                                data,
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

                if (data.isEmpty()) {
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
