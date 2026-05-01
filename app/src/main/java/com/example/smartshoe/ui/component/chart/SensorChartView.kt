package com.example.smartshoe.ui.component.chart

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.smartshoe.domain.model.SensorDataPoint
import com.example.smartshoe.ui.theme.AppColors
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.listener.OnChartValueSelectedListener

/**
 * 通用传感器图表组件
 * 支持历史记录和实时数据两种模式
 *
 * 行为差异：
 * - HISTORY模式：启用峰值采样优化（大数据集降采样），支持点击交互
 * - REALTIME模式：禁用峰值采样（保留所有数据点），禁用点击交互
 */
@Composable
fun SensorChartView(
    data: List<SensorDataPoint>,
    sensorIndex: Int,
    sensorName: String,
    color: Color,
    firstTimestamp: Long,
    lastTimestamp: Long,
    chartType: ChartConfigUtils.ChartType,
    modifier: Modifier = Modifier,
    titleFontSize: Float = 14f,
    chartHeight: Int = 180
) {
    // 实时模式禁用峰值采样，历史模式启用
    val enableSampling = chartType == ChartConfigUtils.ChartType.HISTORY

    var selectedHighlight by remember(data) { mutableStateOf<Int?>(null) }
    val selectedValue = selectedHighlight ?: (data.lastOrNull()?.getValue(sensorIndex) ?: 0)

    // 峰值采样：仅历史模式启用，大数据集时只保留峰值、谷值和间隔点
    val sampledData by remember(data, enableSampling) {
        derivedStateOf {
            when {
                data.isEmpty() -> emptyList()
                !enableSampling -> data  // 实时模式：保留所有数据
                data.size <= 200 -> data  // 历史模式但数据量小：保留所有数据
                else -> {
                    // 历史模式大数据集：峰值采样
                    val sampleInterval = data.size / 150
                    val sampled = mutableListOf<SensorDataPoint>()
                    sampled.add(data.first())
                    var i = 1
                    while (i < data.size - 1) {
                        val current = data[i]
                        val prev = data[i - 1]
                        val next = data[i + 1]
                        val currentValue = current.getValue(sensorIndex)
                        val prevValue = prev.getValue(sensorIndex)
                        val nextValue = next.getValue(sensorIndex)
                        val isPeak = currentValue > prevValue && currentValue > nextValue
                        val isValley = currentValue < prevValue && currentValue < nextValue
                        val isIntervalPoint = i % sampleInterval == 0
                        if (isPeak || isValley || isIntervalPoint) {
                            sampled.add(current)
                        }
                        i++
                    }
                    if (data.last() != sampled.lastOrNull()) {
                        sampled.add(data.last())
                    }
                    sampled
                }
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = sensorName,
                fontSize = titleFontSize.sp,
                fontWeight = FontWeight.Medium,
                color = AppColors.Primary
            )

            Text(
                text = "数值: $selectedValue",
                fontSize = 12.sp,
                color = if (data.isEmpty()) AppColors.DisabledText else AppColors.PlaceholderText,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight.dp),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { context ->
                    LineChart(context).apply {
                        ChartConfigUtils.setupChartStyle(this, firstTimestamp, lastTimestamp, chartType)

                        // 仅历史模式启用点击交互
                        if (enableSampling) {
                            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                                override fun onValueSelected(e: com.github.mikephil.charting.data.Entry?, h: com.github.mikephil.charting.highlight.Highlight?) {
                                e?.let { entry ->
                                    val closestPoint = ChartConfigUtils.findClosestDataPoint(entry, sampledData, firstTimestamp)
                                    closestPoint?.let { point ->
                                        selectedHighlight = ChartConfigUtils.getSensorValue(point, sensorIndex)
                                    }
                                }
                            }
                            override fun onNothingSelected() {
                                selectedHighlight = null
                            }
                            })
                        }
                    }
                },
                update = { chart ->
                    if (sampledData.isNotEmpty()) {
                        ChartConfigUtils.updateChartData(chart, sampledData, sensorIndex, firstTimestamp, color, chartType)
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
