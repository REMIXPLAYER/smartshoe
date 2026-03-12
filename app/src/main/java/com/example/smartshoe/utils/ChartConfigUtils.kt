package com.example.smartshoe.utils

import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 图表配置工具类
 * 统一图表样式配置，避免重复代码
 */
object ChartConfigUtils {
    
    private val timeFormat by lazy { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    
    /**
     * 配置实时图表样式（数据记录页面）
     */
    fun setupRealtimeChartStyle(chart: LineChart, firstTimestamp: Long, lastTimestamp: Long) {
        chart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(true)
            setDrawGridBackground(false)
            
            // X轴配置
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(true)
                gridColor = AndroidColor.parseColor("#E0E0E0")
                textColor = AndroidColor.parseColor("#757575")
                textSize = 10f
                
                // 时间格式化
                val duration = (lastTimestamp - firstTimestamp) / 1000f
                axisMinimum = 0f
                axisMaximum = duration.coerceAtLeast(120f) // 至少显示2分钟
                
                valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        val actualTimestamp = firstTimestamp + (value * 1000).toLong()
                        return timeFormat.format(Date(actualTimestamp))
                    }
                }
                
                setLabelCount(5, false)
            }
            
            // Y轴配置
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = AndroidColor.parseColor("#E0E0E0")
                textColor = AndroidColor.parseColor("#757575")
                textSize = 10f
                axisMinimum = 0f
                axisMaximum = 4095f
            }
            
            axisRight.isEnabled = false
        }
    }
    
    /**
     * 配置历史图表样式（历史记录页面）
     */
    fun setupHistoryChartStyle(chart: LineChart, firstTimestamp: Long, lastTimestamp: Long) {
        chart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(true)
            setDrawGridBackground(false)
            
            val duration = (lastTimestamp - firstTimestamp) / 1000f
            
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(true)
                gridColor = AndroidColor.parseColor("#E0E0E0")
                textColor = AndroidColor.parseColor("#757575")
                textSize = 9f
                
                axisMinimum = 0f
                axisMaximum = duration.coerceAtLeast(60f)
                
                // 根据时长选择时间格式
                val format = if (duration > 3600) "HH:mm" else "HH:mm:ss"
                val formatter = SimpleDateFormat(format, Locale.getDefault())
                
                valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        val actualTimestamp = firstTimestamp + (value * 1000).toLong()
                        return formatter.format(Date(actualTimestamp))
                    }
                }
                
                setLabelCount(5, false)
            }
            
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = AndroidColor.parseColor("#E0E0E0")
                textColor = AndroidColor.parseColor("#757575")
                textSize = 10f
                axisMinimum = 0f
                axisMaximum = 4095f
            }
            
            axisRight.isEnabled = false
        }
    }
    
    /**
     * 创建图表数据集
     */
    fun createLineDataSet(
        entries: List<Entry>,
        label: String,
        lineColor: Color,
        fillColor: Color = lineColor.copy(alpha = 0.1f)
    ): LineDataSet {
        return LineDataSet(entries, label).apply {
            color = lineColor.toArgb()
            lineWidth = 2f
            setDrawCircles(false)
            setDrawValues(false)
            mode = LineDataSet.Mode.LINEAR
            
            // 填充
            setDrawFilled(true)
            setFillColor(fillColor.toArgb())
            fillAlpha = 50
        }
    }
    
    /**
     * 设置空图表样式
     */
    fun setupEmptyChart(chart: LineChart) {
        chart.apply {
            clear()
            data = null
            invalidate()
            
            xAxis.apply {
                axisMinimum = 0f
                axisMaximum = 120f
                setLabelCount(5, false)
            }
            
            axisLeft.apply {
                axisMinimum = 0f
                axisMaximum = 4095f
            }
        }
    }
}
