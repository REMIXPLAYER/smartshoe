package com.example.smartshoe.domain.repository

import com.example.smartshoe.domain.model.SensorDataPoint

/**
 * 传感器数据仓库接口
 *
 * 职责：定义传感器数据相关的数据操作
 * 这是领域层接口，不依赖任何数据层实现细节
 */
interface SensorDataRepository {

    /**
     * 处理接收到的蓝牙数据
     * @param data 接收到的字符串数据（格式：十六进制数值，用逗号分隔）
     * @param shouldRecord 是否应该记录数据
     * @return Pair<传感器数值, 额外数值> 或 null 如果解析失败
     */
    fun processReceivedData(data: String, shouldRecord: Boolean = true): Pair<List<Int>, List<Int>>?

    /**
     * 获取当前加权平均值列表
     * @return 三个传感器的加权平均值列表
     */
    fun getWeightedAverages(): List<Float>

    /**
     * 获取当前压力状态列表
     * @return 三个传感器的压力状态列表
     */
    fun getPressureStatuses(): List<com.example.smartshoe.domain.model.PressureStatus>

    /**
     * 清空滑动窗口数据
     */
    fun clearSlidingWindow()

    /**
     * 清空所有传感器数据
     */
    fun clearSensorData()

    /**
     * 获取历史数据列表
     */
    fun getHistoricalData(): List<SensorDataPoint>

    /**
     * 获取备份数据列表（用于上传）
     */
    fun getBackupDataForUpload(): List<SensorDataPoint>

    /**
     * 获取历史数据缓冲区大小
     */
    fun getHistoricalDataSize(): Int

    /**
     * 获取备份数据缓冲区大小
     */
    fun getBackupDataSize(): Int

    /**
     * 检查备份数据是否为空
     */
    fun isBackupDataEmpty(): Boolean

    /**
     * 获取最新数据点
     */
    fun getLatestData(): SensorDataPoint?

    /**
     * 生成模拟传感器数据（用于调试）
     * @param count 数据点数量
     * @param timeRangeMinutes 时间范围（分钟）
     * @return 生成的数据点列表
     */
    fun generateMockData(
        count: Int = 100,
        timeRangeMinutes: Int = 5
    ): List<SensorDataPoint>
}
