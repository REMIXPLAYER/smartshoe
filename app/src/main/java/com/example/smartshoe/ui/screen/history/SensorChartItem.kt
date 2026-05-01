package com.example.smartshoe.ui.screen.history

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.smartshoe.domain.model.SensorDataPoint
import com.example.smartshoe.domain.model.SensorDataRecord
import com.example.smartshoe.ui.component.chart.ChartConfigUtils
import com.example.smartshoe.ui.component.chart.SensorChartView

/**
 * 历史记录页面的传感器图表项
 * 委托给公共的SensorChartView组件
 */
@Composable
fun SensorChartItem(
    data: List<SensorDataPoint>,
    record: SensorDataRecord,
    sensorIndex: Int,
    sensorName: String,
    color: Color
) {
    SensorChartView(
        data = data,
        sensorIndex = sensorIndex,
        sensorName = sensorName,
        color = color,
        firstTimestamp = record.startTime,
        lastTimestamp = record.endTime,
        chartType = ChartConfigUtils.ChartType.HISTORY,
        titleFontSize = 14f,
        chartHeight = 180
    )
}
