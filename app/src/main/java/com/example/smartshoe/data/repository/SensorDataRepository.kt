package com.example.smartshoe.data.repository

import android.util.Log
import com.example.smartshoe.config.AppConfig
import com.example.smartshoe.data.local.CircularBuffer
import com.example.smartshoe.data.model.SensorDataPoint
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 传感器数据仓库
 * 纯数据逻辑，只负责数据存储和访问
 *
 * 职责：
 * - 传感器数据解析和存储
 * - 历史数据管理
 * - 备份数据管理
 *
 * 注意：不包含任何业务逻辑（如压力提醒），业务逻辑应在 ViewModel 中处理
 */
@Singleton
class SensorDataRepository @Inject constructor() {

    companion object {
        private const val TAG = "SensorDataRepository"
    }

    // 实时显示数据 - 使用环形缓冲区
    private val historicalDataBuffer = CircularBuffer<SensorDataPoint>(AppConfig.Sensor.HISTORICAL_BUFFER_SIZE)

    // 备份数据 - 使用环形缓冲区
    private val backupDataBuffer = CircularBuffer<SensorDataPoint>(AppConfig.Sensor.BACKUP_BUFFER_SIZE)

    // 记录相关状态
    private var lastRecordTime = 0L
    private val recordingInterval = AppConfig.Sensor.RECORDING_INTERVAL_MS

    /**
     * 处理接收到的蓝牙数据
     * @param data 接收到的字符串数据（格式：十六进制数值，用逗号分隔）
     * @param shouldRecord 是否应该记录数据
     * @return Pair<传感器数值, 额外数值> 或 null 如果解析失败
     */
    fun processReceivedData(data: String, shouldRecord: Boolean = true): Pair<List<Int>, List<Int>>? {
        val hexValues = data.trim().split(",")
        if (hexValues.size >= 6) {
            try {
                val values = hexValues.take(3).map { it.toInt(16) }.toMutableList()
                var extras = hexValues.takeLast(3).map { it.toInt(16) }.toMutableList()

                // 传感器3替代方案：当硬件损坏时，使用传感器1和2的平均值
                if (AppConfig.Sensor.SENSOR3_USE_CALCULATED_VALUE) {
                    val calculatedSensor3 = (extras[0] + extras[1]) / 2
                    extras[2] = calculatedSensor3
                    // 同时更新 values 数组（如果蓝牙数据中的 values 也需要更新）
                    if (values.size >= 3) {
                        values[2] = calculatedSensor3
                    }
                }

                if (shouldRecord) {
                    autoRecordData(extras[0], extras[1], extras[2])
                }

                return Pair(values, extras)
            } catch (e: NumberFormatException) {
                Log.e(TAG, "Error parsing sensor data: ${e.message}", e)
            }
        }
        return null
    }

    /**
     * 检查压力是否超过阈值
     * @param extras 传感器数值列表
     * @return 压力异常的传感器索引和数值列表，如果没有异常返回空列表
     */
    fun checkPressureAlerts(extras: List<Int>): List<Pair<Int, Int>> {
        val alerts = mutableListOf<Pair<Int, Int>>()
        extras.forEachIndexed { index, value ->
            if (value > AppConfig.Sensor.PRESSURE_ALERT_THRESHOLD) {
                alerts.add(Pair(index, value))
            }
        }
        return alerts
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
    }

    /**
     * 清空所有传感器数据
     */
    fun clearSensorData() {
        historicalDataBuffer.clear()
        backupDataBuffer.clear()
        lastRecordTime = 0L
    }

    /**
     * 获取历史数据列表
     */
    fun getHistoricalData(): List<SensorDataPoint> {
        return historicalDataBuffer.toOrderedList()
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
     * 获取最新数据点
     */
    fun getLatestData(): SensorDataPoint? = historicalDataBuffer.getLatest()

    /**
     * 生成模拟传感器数据（用于调试）
     * @return 生成的数据点列表
     */
    fun generateMockData(
        count: Int = AppConfig.Sensor.DEFAULT_MOCK_DATA_COUNT,
        timeRangeMinutes: Int = AppConfig.Sensor.DEFAULT_MOCK_TIME_RANGE_MINUTES
    ): List<SensorDataPoint> {
        clearSensorData()

        val currentTime = System.currentTimeMillis()
        val timeRangeMillis = timeRangeMinutes * AppConfig.UI.MS_PER_MINUTE
        val startTime = currentTime - timeRangeMillis

        val dataPoints = mutableListOf<SensorDataPoint>()

        for (i in 0 until count) {
            val progress = i.toFloat() / count.toFloat()
            val timestamp = startTime + (progress * timeRangeMillis).toLong()

            val baseSensor1 = 500 + (Math.random() * 2500).toInt()
            val baseSensor2 = 300 + (Math.random() * 1500).toInt()
            val baseSensor3 = 200 + (Math.random() * 1000).toInt()

            val wave = kotlin.math.sin(progress * kotlin.math.PI * 4) * 500
            val sensor1 = (baseSensor1 + wave).toInt().coerceIn(0, AppConfig.Sensor.SENSOR_MAX_VALUE)
            val sensor2 = (baseSensor2 + wave * 0.7).toInt().coerceIn(0, AppConfig.Sensor.SENSOR_MAX_VALUE)
            val sensor3 = (baseSensor3 + wave * 0.5).toInt().coerceIn(0, AppConfig.Sensor.SENSOR_MAX_VALUE)

            dataPoints.add(SensorDataPoint(timestamp, sensor1, sensor2, sensor3))
        }

        dataPoints.sortedBy { it.timestamp }.forEach { point ->
            historicalDataBuffer.add(point)
            backupDataBuffer.add(point)
        }

        return dataPoints
    }
}
