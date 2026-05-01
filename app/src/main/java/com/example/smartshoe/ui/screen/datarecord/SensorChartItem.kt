package com.example.smartshoe.ui.screen.datarecord

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.smartshoe.domain.model.SensorDataPoint
import com.example.smartshoe.ui.component.chart.ChartConfigUtils
import com.example.smartshoe.ui.component.chart.SensorChartView

/**
 * 数据记录页面的传感器图表项
 * 委托给公共的SensorChartView组件
 */
@Composable
fun SensorChartItem(
    data: List<SensorDataPoint>,
    sensorIndex: Int,
    sensorName: String
) {
    val firstTimestamp = data.firstOrNull()?.timestamp ?: 0L
    val lastTimestamp = data.lastOrNull()?.timestamp ?: firstTimestamp

    SensorChartView(
        data = data,
        sensorIndex = sensorIndex,
        sensorName = sensorName,
        color = ChartConfigUtils.SensorColors.getColor(sensorIndex),
        firstTimestamp = firstTimestamp,
        lastTimestamp = lastTimestamp,
        chartType = ChartConfigUtils.ChartType.REALTIME,
        titleFontSize = 16f,
        chartHeight = 200
    )
}
