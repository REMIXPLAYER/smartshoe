package com.example.smartshoe.data.repository

import android.util.Log
import com.example.smartshoe.config.AppConfig
import com.example.smartshoe.data.local.CircularBuffer
import com.example.smartshoe.domain.model.SensorDataPoint
import com.example.smartshoe.domain.model.PressureStatus
import com.example.smartshoe.domain.repository.SensorDataRepository
import com.example.smartshoe.util.ColorUtils
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
 * - 滑动窗口加权平均计算
 *
 * 注意：不包含任何业务逻辑（如压力提醒），业务逻辑应在 ViewModel 中处理
 */
@Singleton
class SensorDataRepositoryImpl @Inject constructor() : SensorDataRepository {

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

    // ========== 滑动窗口加权平均相关 ==========
    // 三个传感器的滑动窗口数据
    private val sensor1Window = ArrayDeque<Int>(AppConfig.Sensor.WEIGHTED_WINDOW_SIZE)
    private val sensor2Window = ArrayDeque<Int>(AppConfig.Sensor.WEIGHTED_WINDOW_SIZE)
    private val sensor3Window = ArrayDeque<Int>(AppConfig.Sensor.WEIGHTED_WINDOW_SIZE)

    // 当前加权平均值
    private var sensor1WeightedAvg = 0f
    private var sensor2WeightedAvg = 0f
    private var sensor3WeightedAvg = 0f

    // 当前压力状态
    private var sensor1Status = PressureStatus.NONE
    private var sensor2Status = PressureStatus.NONE
    private var sensor3Status = PressureStatus.NONE

    /**
     * 处理接收到的蓝牙数据
     * @param data 接收到的字符串数据（格式：十六进制数值，用逗号分隔）
     * @param shouldRecord 是否应该记录数据
     * @return Pair<传感器数值, 额外数值> 或 null 如果解析失败
     */
    override fun processReceivedData(data: String, shouldRecord: Boolean): Pair<List<Int>, List<Int>>? {
        // 清理非法字符，只保留十六进制字符和逗号
        val cleaned = data.replace(Regex("[^0-9A-Fa-f,]"), "")

        val hexValues = cleaned.split(",")
        if (hexValues.size != 6) {
            Log.w(TAG, "Invalid frame format: expected 6 values, got ${hexValues.size}, data='$data'")
            return null
        }

        val values = mutableListOf<Int>()
        for ((index, hex) in hexValues.withIndex()) {
            try {
                values.add(hex.toInt(16))
            } catch (e: NumberFormatException) {
                Log.e(TAG, "Invalid hex at index $index: '$hex' in data='$data'")
                return null
            }
        }

        // 值域校验（12位ADC，0-4095）
        if (values.any { it < 0 || it > AppConfig.Sensor.SENSOR_MAX_VALUE }) {
            Log.w(TAG, "Value out of range (0-${AppConfig.Sensor.SENSOR_MAX_VALUE}): $values")
            return null
        }

        val extras = values.toMutableList()

        // 传感器3补偿：硬件不可用时，使用传感器2计算值替代
        if (AppConfig.Sensor.SENSOR3_USE_CALCULATED_VALUE) {
            val calculatedSensor3 = (extras[1] * AppConfig.Sensor.SENSOR3_MULTIPLIER / AppConfig.Sensor.SENSOR3_DIVISOR)
                .coerceIn(0, AppConfig.Sensor.SENSOR_MAX_VALUE)
            extras[2] = calculatedSensor3
        }

        updateSlidingWindowAndCalculate(extras[0], extras[1], extras[2])

        // 记录数据到缓冲区
        if (shouldRecord) {
            autoRecordData(extras[0], extras[1], extras[2])
        }

        val displayValues = listOf(extras[0], extras[1], extras[2])
        return Pair(displayValues, displayValues)
    }

    /**
     * 更新滑动窗口并计算加权平均值
     *
     * @param sensor1Value 传感器1数值
     * @param sensor2Value 传感器2数值
     * @param sensor3Value 传感器3数值
     */
    private fun updateSlidingWindowAndCalculate(
        sensor1Value: Int,
        sensor2Value: Int,
        sensor3Value: Int
    ) {
        // 更新传感器1窗口
        sensor1Window.addLast(sensor1Value)
        if (sensor1Window.size > AppConfig.Sensor.WEIGHTED_WINDOW_SIZE) {
            sensor1Window.removeFirst()
        }

        // 更新传感器2窗口
        sensor2Window.addLast(sensor2Value)
        if (sensor2Window.size > AppConfig.Sensor.WEIGHTED_WINDOW_SIZE) {
            sensor2Window.removeFirst()
        }

        // 更新传感器3窗口
        sensor3Window.addLast(sensor3Value)
        if (sensor3Window.size > AppConfig.Sensor.WEIGHTED_WINDOW_SIZE) {
            sensor3Window.removeFirst()
        }

        // 计算加权平均值
        sensor1WeightedAvg = ColorUtils.calculateWeightedAverage(sensor1Window.toList())
        sensor2WeightedAvg = ColorUtils.calculateWeightedAverage(sensor2Window.toList())
        sensor3WeightedAvg = ColorUtils.calculateWeightedAverage(sensor3Window.toList())

        // 更新压力状态
        sensor1Status = ColorUtils.getPressureStatusFromAverage(sensor1WeightedAvg)
        sensor2Status = ColorUtils.getPressureStatusFromAverage(sensor2WeightedAvg)
        sensor3Status = ColorUtils.getPressureStatusFromAverage(sensor3WeightedAvg)

        Log.d(TAG, "加权平均值 - S1: ${sensor1WeightedAvg.toInt()}, S2: ${sensor2WeightedAvg.toInt()}, S3: ${sensor3WeightedAvg.toInt()}")
    }

    /**
     * 获取当前加权平均值列表
     *
     * @return 三个传感器的加权平均值列表
     */
    override fun getWeightedAverages(): List<Float> {
        return listOf(sensor1WeightedAvg, sensor2WeightedAvg, sensor3WeightedAvg)
    }

    /**
     * 获取当前压力状态列表
     *
     * @return 三个传感器的压力状态列表
     */
    override fun getPressureStatuses(): List<PressureStatus> {
        return listOf(sensor1Status, sensor2Status, sensor3Status)
    }

    /**
     * 清空滑动窗口数据
     */
    override fun clearSlidingWindow() {
        sensor1Window.clear()
        sensor2Window.clear()
        sensor3Window.clear()
        sensor1WeightedAvg = 0f
        sensor2WeightedAvg = 0f
        sensor3WeightedAvg = 0f
        sensor1Status = PressureStatus.NONE
        sensor2Status = PressureStatus.NONE
        sensor3Status = PressureStatus.NONE
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
    override fun clearSensorData() {
        historicalDataBuffer.clear()
        backupDataBuffer.clear()
        lastRecordTime = 0L
        clearSlidingWindow()
    }

    /**
     * 获取历史数据列表
     */
    override fun getHistoricalData(): List<SensorDataPoint> {
        return historicalDataBuffer.toOrderedList()
    }

    /**
     * 获取备份数据列表（用于上传）
     */
    override fun getBackupDataForUpload(): List<SensorDataPoint> {
        return backupDataBuffer.toOrderedList()
    }

    /**
     * 获取历史数据缓冲区大小
     */
    override fun getHistoricalDataSize(): Int = historicalDataBuffer.size()

    /**
     * 获取备份数据缓冲区大小
     */
    override fun getBackupDataSize(): Int = backupDataBuffer.size()

    /**
     * 检查备份数据是否为空
     */
    override fun isBackupDataEmpty(): Boolean = backupDataBuffer.isEmpty()

    /**
     * 获取最新数据点
     */
    override fun getLatestData(): SensorDataPoint? = historicalDataBuffer.getLatest()

    /**
     * 生成模拟传感器数据（用于调试）
     * @return 生成的数据点列表
     */
    override fun generateMockData(
        count: Int,
        timeRangeMinutes: Int
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
            // 关键修复：同时更新滑动窗口和加权平均值
            updateSlidingWindowAndCalculate(point.sensor1, point.sensor2, point.sensor3)
        }

        return dataPoints
    }
}
