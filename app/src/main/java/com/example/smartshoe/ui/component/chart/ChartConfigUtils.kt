package com.example.smartshoe.ui.component.chart

import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.smartshoe.config.AppConfig
import com.example.smartshoe.data.model.SensorDataPoint
import com.example.smartshoe.util.DateTimeUtils
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import java.util.Date

/**
 * 图表配置工具类
 * 统一图表样式配置，避免重复代码
 *
 * 重构：统一使用历史图表类型，支持实时和历史数据展示
 * 重构：使用 DateTimeUtils 替代重复的 SimpleDateFormat
 */
object ChartConfigUtils {

    /**
     * 图表类型枚举
     */
    enum class ChartType {
        REALTIME,   // 实时数据图表（最近2分钟）
        HISTORY     // 历史数据图表（完整时间范围）
    }

    /**
     * 传感器颜色配置
     */
    object SensorColors {
        val SENSOR_1 = Color(0xFFFF6B6B)  // 红色 - 脚掌前部
        val SENSOR_2 = Color(0xFF4ECDC4)  // 青色 - 脚弓部
        val SENSOR_3 = Color(0xFF45B7D1)  // 蓝色 - 脚跟部
        val DEFAULT = Color(0xFF3949AB)   // 默认颜色

        fun getColor(sensorIndex: Int): Color {
            return when (sensorIndex) {
                0 -> SENSOR_1
                1 -> SENSOR_2
                2 -> SENSOR_3
                else -> DEFAULT
            }
        }
    }

    /**
     * 统一图表样式配置
     * 支持实时和历史两种模式
     *
     * @param chart LineChart 实例
     * @param firstTimestamp 数据起始时间戳
     * @param lastTimestamp 数据结束时间戳
     * @param chartType 图表类型（实时或历史）
     */
    fun setupChartStyle(
        chart: LineChart,
        firstTimestamp: Long,
        lastTimestamp: Long,
        chartType: ChartType = ChartType.HISTORY
    ) {
        // 基本交互配置
        chart.apply {
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            isDoubleTapToZoomEnabled = true
            description.isEnabled = false
            legend.isEnabled = false
        }

        // 计算时间范围
        val totalDurationSeconds = if (lastTimestamp > firstTimestamp) {
            (lastTimestamp - firstTimestamp) / 1000f
        } else {
            0f
        }

        // X轴配置
        setupXAxis(chart.xAxis, firstTimestamp, totalDurationSeconds, chartType)

        // Y轴配置
        setupYAxis(chart.axisLeft)
        chart.axisRight.isEnabled = false
    }

    /**
     * 配置X轴
     */
    private fun setupXAxis(
        xAxis: XAxis,
        firstTimestamp: Long,
        totalDurationSeconds: Float,
        chartType: ChartType
    ) {
        xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            setDrawGridLines(true)
            gridColor = AndroidColor.parseColor("#E0E0E0")
            setDrawAxisLine(true)
            textColor = AndroidColor.parseColor("#757575")
            textSize = 8f
            labelRotationAngle = -45f

            // 根据图表类型和时长设置标签数量
            labelCount = when (chartType) {
                ChartType.REALTIME -> 5
                ChartType.HISTORY -> when {
                    totalDurationSeconds < 60 -> 5
                    totalDurationSeconds < 600 -> 5
                    totalDurationSeconds < 3600 -> 6
                    else -> 6
                }
            }

            // 时间格式化器
            valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val actualTimestamp = firstTimestamp + (value * 1000).toLong()
                    return if (totalDurationSeconds < 3600) {
                        DateTimeUtils.formatTime(actualTimestamp)
                    } else {
                        DateTimeUtils.formatShortTime(actualTimestamp)
                    }
                }
            }
        }
    }

    /**
     * 配置Y轴
     */
    private fun setupYAxis(yAxis: YAxis) {
        yAxis.apply {
            setDrawGridLines(true)
            gridColor = AndroidColor.parseColor("#E0E0E0")
            setDrawAxisLine(true)
            textColor = AndroidColor.parseColor("#757575")
            textSize = 10f
            axisMinimum = 0f
            axisMaximum = AppConfig.Sensor.SENSOR_MAX_VALUE.toFloat()
            labelCount = 5
        }
    }

    /**
     * 创建图表数据集
     *
     * @param entries 数据点列表
     * @param label 数据集标签
     * @param lineColor 线条颜色
     * @param fillColor 填充颜色（默认为线条颜色的透明版本）
     */
    fun createLineDataSet(
        entries: List<Entry>,
        label: String,
        lineColor: Color,
        fillColor: Color = lineColor.copy(alpha = 0.2f)
    ): LineDataSet {
        return LineDataSet(entries, label).apply {
            color = lineColor.toArgb()
            lineWidth = 2f
            setDrawCircles(false)
            setDrawValues(false)
            mode = LineDataSet.Mode.LINEAR
            setDrawFilled(true)
            setFillColor(fillColor.toArgb())
            fillAlpha = 50
        }
    }

    /**
     * 转换传感器数据为图表条目
     *
     * @param data 传感器数据点列表
     * @param sensorIndex 传感器索引（0, 1, 2）
     * @param baseTimestamp 基准时间戳（用于计算相对时间）
     * @return Entry 列表
     */
    fun convertToEntries(
        data: List<SensorDataPoint>,
        sensorIndex: Int,
        baseTimestamp: Long
    ): List<Entry> {
        return data.map { dataPoint ->
            val value = dataPoint.getValue(sensorIndex).toFloat()
            val relativeTime = (dataPoint.timestamp - baseTimestamp) / 1000f
            Entry(relativeTime, value)
        }
    }

    /**
     * 更新图表数据（统一方法）
     *
     * @param chart LineChart 实例
     * @param data 传感器数据点列表
     * @param sensorIndex 传感器索引
     * @param baseTimestamp 基准时间戳（用于历史图表）
     * @param lineColor 线条颜色
     * @param chartType 图表类型
     */
    fun updateChartData(
        chart: LineChart,
        data: List<SensorDataPoint>,
        sensorIndex: Int,
        baseTimestamp: Long,
        lineColor: Color,
        chartType: ChartType = ChartType.HISTORY
    ) {
        if (data.isEmpty()) {
            chart.clear()
            chart.invalidate()
            return
        }

        // 根据图表类型过滤数据
        val displayData = when (chartType) {
            ChartType.REALTIME -> {
                // 只取最近2分钟的数据
                val lastTimestamp = data.lastOrNull()?.timestamp ?: 0L
                val twoMinutesAgo = lastTimestamp - AppConfig.UI.MS_PER_MINUTE * 2
                data.filter { it.timestamp >= twoMinutesAgo }
            }
            ChartType.HISTORY -> data
        }

        if (displayData.isEmpty()) {
            chart.clear()
            chart.invalidate()
            return
        }

        // 根据图表类型确定基准时间戳
        // REALTIME: 使用过滤后数据的第一个时间戳作为基准
        // HISTORY: 使用传入的 baseTimestamp
        val actualBaseTimestamp = when (chartType) {
            ChartType.REALTIME -> displayData.first().timestamp
            ChartType.HISTORY -> baseTimestamp
        }

        // 转换数据
        val entries = convertToEntries(displayData, sensorIndex, actualBaseTimestamp)

        // 创建数据集
        val dataSet = createLineDataSet(entries, "传感器数据", lineColor)
        val lineData = LineData(dataSet)

        chart.data = lineData

        // 根据图表类型设置X轴范围
        when (chartType) {
            ChartType.REALTIME -> {
                chart.xAxis.axisMinimum = 0f
                chart.xAxis.axisMaximum = 120f // 2分钟 = 120秒
            }
            ChartType.HISTORY -> {
                chart.fitScreen()
            }
        }

        chart.notifyDataSetChanged()
        chart.invalidate()
    }

    /**
     * 设置空图表样式
     */
    fun setupEmptyChart(chart: LineChart) {
        chart.clear()
        chart.data = null

        // 设置默认坐标轴范围
        chart.xAxis.apply {
            axisMinimum = 0f
            axisMaximum = 120f
        }

        chart.axisLeft.apply {
            axisMinimum = 0f
            axisMaximum = AppConfig.Sensor.SENSOR_MAX_VALUE.toFloat()
        }

        // 禁用默认的 "No chart data available" 提示
        chart.setNoDataText("")
        chart.setNoDataTextColor(AndroidColor.TRANSPARENT)

        chart.invalidate()
    }

    /**
     * 设置图表选中监听器
     *
     * @param chart LineChart 实例
     * @param onValueSelected 选中值回调
     * @param onNothingSelected 取消选中回调
     */
    fun setupValueSelectedListener(
        chart: LineChart,
        onValueSelected: (entry: Entry, data: List<SensorDataPoint>, baseTimestamp: Long, sensorIndex: Int) -> Unit,
        onNothingSelected: () -> Unit,
        data: List<SensorDataPoint>,
        baseTimestamp: Long,
        sensorIndex: Int
    ) {
        chart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry?, h: Highlight?) {
                e?.let { entry ->
                    onValueSelected(entry, data, baseTimestamp, sensorIndex)
                }
            }

            override fun onNothingSelected() {
                onNothingSelected()
            }
        })
    }

    /**
     * 根据Entry查找最接近的传感器数据点
     *
     * @param entry 图表条目
     * @param data 传感器数据列表
     * @param baseTimestamp 基准时间戳
     * @return 最接近的 SensorDataPoint
     */
    fun findClosestDataPoint(
        entry: Entry,
        data: List<SensorDataPoint>,
        baseTimestamp: Long
    ): SensorDataPoint? {
        val targetTime = baseTimestamp + (entry.x * 1000).toLong()
        return data.minByOrNull { kotlin.math.abs(it.timestamp - targetTime) }
    }

    /**
     * 获取传感器数值
     *
     * @param dataPoint 传感器数据点
     * @param sensorIndex 传感器索引
     * @return 数值
     */
    fun getSensorValue(dataPoint: SensorDataPoint, sensorIndex: Int): Int {
        return dataPoint.getValue(sensorIndex)
    }
}
