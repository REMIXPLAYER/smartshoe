package com.example.smartshoe.data.repository

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.Color
import com.example.smartshoe.data.CircularBuffer
import com.example.smartshoe.data.SensorDataPoint

/**
 * 传感器数据处理器
 * 负责传感器数据的接收、处理、存储和颜色计算
 */
class SensorDataProcessor {

    // 传感器颜色状态列表，用于显示压力颜色（灰→蓝→橙→红渐变）
    val sensorColors = mutableStateListOf(Color.Gray, Color.Gray, Color.Gray)

    // 传感器原始数值列表，存储从蓝牙接收的原始数据
    val extraValues = mutableStateListOf(0, 0, 0)

    // 实时显示数据 - 使用环形缓冲区，最大1200个点（2分钟数据）
    val historicalData = mutableStateListOf<SensorDataPoint>()
    private val historicalDataBuffer = CircularBuffer<SensorDataPoint>(1200)

    // 备份数据 - 使用环形缓冲区，最大18000个点（30分钟数据）
    private val backupDataBuffer = CircularBuffer<SensorDataPoint>(18000)

    // 记录相关状态
    private var lastRecordTime = 0L
    private val recordingInterval = 100L // 记录间隔100毫秒

    // 压力提醒相关回调
    var onPressureAlert: ((String) -> Unit)? = null
    var pressureAlertsEnabled = true

    // 数据更新回调
    var onDataUpdated: (() -> Unit)? = null

    /**
     * 处理接收到的蓝牙数据
     * @param data 接收到的字符串数据（格式：十六进制数值，用逗号分隔）
     * @param shouldRecord 是否应该记录数据
     */
    fun processReceivedData(data: String, shouldRecord: Boolean = true) {
        val hexValues = data.trim().split(",")
        if (hexValues.size >= 6) {
            try {
                val values = hexValues.take(3).map { it.toInt(16) }
                val extras = hexValues.takeLast(3).map { it.toInt(16) }

                updateColors(values)
                updateExtraValues(extras)

                if (pressureAlertsEnabled) {
                    checkPressureAlerts(extras)
                }

                if (shouldRecord) {
                    autoRecordData(extras[0], extras[1], extras[2])
                }

                onDataUpdated?.invoke()
            } catch (e: NumberFormatException) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 更新传感器颜色
     */
    private fun updateColors(values: List<Int>) {
        values.forEachIndexed { index, value ->
            if (index < sensorColors.size) {
                sensorColors[index] = calculateColorFromPressure(value)
            }
        }
    }

    /**
     * 更新显示的传感器数值
     */
    private fun updateExtraValues(values: List<Int>) {
        values.forEachIndexed { index, value ->
            if (index < extraValues.size) {
                extraValues[index] = value
            }
        }
    }

    /**
     * 检查压力异常并触发提醒
     */
    private fun checkPressureAlerts(extras: List<Int>) {
        extras.forEachIndexed { index, value ->
            if (value > 4000) {
                val sensorNames = listOf("脚掌前部", "脚弓部", "脚跟部")
                onPressureAlert?.invoke("${sensorNames[index]} 检测到异常压力: ${value}")
            }
        }
    }

    /**
     * 自动记录传感器数据（按时间间隔记录）
     */
    private fun autoRecordData(sensor1: Int, sensor2: Int, sensor3: Int) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastRecordTime >= recordingInterval) {
            recordDataPoint(sensor1, sensor2, sensor3, currentTime)
            lastRecordTime = currentTime
        }
    }

    /**
     * 记录传感器数据点
     */
    private fun recordDataPoint(sensor1: Int, sensor2: Int, sensor3: Int, timestamp: Long) {
        val dataPoint = SensorDataPoint(timestamp, sensor1, sensor2, sensor3)
        historicalDataBuffer.add(dataPoint)
        backupDataBuffer.add(dataPoint)
        historicalDataBuffer.toSnapshotStateList(historicalData)
    }

    /**
     * 根据压力值计算显示颜色 - 使用ColorHunt配色方案
     * 压力值越低越接近淡绿色(#A3D78A)，越高越接近红色(#FF5555)
     */
    private fun calculateColorFromPressure(value: Int): Color {
        val colorLow = Color(0xFFA3D78A)
        val colorMidLow = Color(0xFFC1E59F)
        val colorMidHigh = Color(0xFFFF937E)
        val colorHigh = Color(0xFFFF5555)

        val normalizedValue = value.coerceIn(0, 4095) / 4095f

        return when {
            normalizedValue < 0.33f -> {
                val t = normalizedValue / 0.33f
                lerpColor(colorLow, colorMidLow, t)
            }
            normalizedValue < 0.66f -> {
                val t = (normalizedValue - 0.33f) / 0.33f
                lerpColor(colorMidLow, colorMidHigh, t)
            }
            else -> {
                val t = (normalizedValue - 0.66f) / 0.34f
                lerpColor(colorMidHigh, colorHigh, t)
            }
        }
    }

    /**
     * 线性插值计算两个颜色之间的过渡色
     */
    private fun lerpColor(start: Color, end: Color, fraction: Float): Color {
        return Color(
            red = start.red + (end.red - start.red) * fraction,
            green = start.green + (end.green - start.green) * fraction,
            blue = start.blue + (end.blue - start.blue) * fraction,
            alpha = start.alpha + (end.alpha - start.alpha) * fraction
        )
    }

    /**
     * 重置传感器显示状态
     */
    fun resetSensorDisplayState() {
        sensorColors.forEachIndexed { index, _ ->
            sensorColors[index] = Color.Gray
        }
        extraValues.forEachIndexed { index, _ ->
            extraValues[index] = 0
        }
    }

    /**
     * 清空所有传感器数据
     */
    fun clearSensorData() {
        historicalDataBuffer.clear()
        backupDataBuffer.clear()
        historicalData.clear()
        lastRecordTime = 0L
    }

    /**
     * 获取备份数据列表（用于上传）
     */
    fun getBackupDataForUpload(): List<SensorDataPoint> {
        return backupDataBuffer.toOrderedList()
    }

    /**
     * 获取历史数据缓冲区大小
     */
    fun getHistoricalDataSize(): Int = historicalDataBuffer.size()

    /**
     * 获取备份数据缓冲区大小
     */
    fun getBackupDataSize(): Int = backupDataBuffer.size()

    /**
     * 检查备份数据是否为空
     */
    fun isBackupDataEmpty(): Boolean = backupDataBuffer.isEmpty()

    /**
     * 生成模拟传感器数据（用于调试）
     */
    fun generateMockData(count: Int = 10000, timeRangeMinutes: Int = 15) {
        clearSensorData()

        val currentTime = System.currentTimeMillis()
        val timeRangeMillis = timeRangeMinutes * 60 * 1000L
        val startTime = currentTime - timeRangeMillis

        val dataPoints = mutableListOf<SensorDataPoint>()

        for (i in 0 until count) {
            val progress = i.toFloat() / count.toFloat()
            val timestamp = startTime + (progress * timeRangeMillis).toLong()

            val baseSensor1 = 500 + (Math.random() * 2500).toInt()
            val baseSensor2 = 300 + (Math.random() * 1500).toInt()
            val baseSensor3 = 200 + (Math.random() * 1000).toInt()

            val wave = kotlin.math.sin(progress * kotlin.math.PI * 4) * 500
            val sensor1 = (baseSensor1 + wave).toInt().coerceIn(0, 4095)
            val sensor2 = (baseSensor2 + wave * 0.7).toInt().coerceIn(0, 4095)
            val sensor3 = (baseSensor3 + wave * 0.5).toInt().coerceIn(0, 4095)

            dataPoints.add(SensorDataPoint(timestamp, sensor1, sensor2, sensor3))
        }

        dataPoints.sortedBy { it.timestamp }.forEach { point ->
            historicalDataBuffer.add(point)
            backupDataBuffer.add(point)
        }

        historicalDataBuffer.toSnapshotStateList(historicalData)

        val latestData = historicalDataBuffer.getLatest()
        if (latestData != null) {
            updateColors(listOf(latestData.sensor1, latestData.sensor2, latestData.sensor3))
            updateExtraValues(listOf(latestData.sensor1, latestData.sensor2, latestData.sensor3))
        }
    }
}
