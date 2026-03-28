package com.example.smartshoe.ui.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartshoe.data.manager.SensorDataManager
import com.example.smartshoe.data.manager.UserPreferencesManager
import com.example.smartshoe.data.model.SensorDataPoint
import com.example.smartshoe.data.repository.SensorDataRepository
import com.example.smartshoe.util.ColorUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// 导入深灰色常量，用于初始状态
private val INITIAL_SENSOR_COLOR = ColorUtils.COLOR_ZERO

/**
 * 传感器数据视图模型
 * 管理传感器数据相关的UI状态和逻辑
 *
 * 重构：
 * 1. 添加 SensorDataManager 依赖，封装数据上传功能
 * 2. 添加 UserPreferencesManager 依赖，管理用户偏好设置
 * 消除 MainActivity 直接访问 Manager 和 LocalDataSource 的问题
 */
@HiltViewModel
class SensorDataViewModel @Inject constructor(
    private val sensorDataRepository: SensorDataRepository,
    private val sensorDataManager: SensorDataManager,
    private val userPreferencesManager: UserPreferencesManager
) : ViewModel() {

    // 传感器颜色状态 - 使用深灰色作为初始状态
    private val _sensorColors = MutableStateFlow<List<Color>>(
        listOf(INITIAL_SENSOR_COLOR, INITIAL_SENSOR_COLOR, INITIAL_SENSOR_COLOR)
    )
    val sensorColors: StateFlow<List<Color>> = _sensorColors.asStateFlow()

    // 传感器数值
    private val _extraValues = MutableStateFlow<List<Int>>(listOf(0, 0, 0))
    val extraValues: StateFlow<List<Int>> = _extraValues.asStateFlow()

    // 历史数据
    private val _historicalData = MutableStateFlow<List<SensorDataPoint>>(emptyList())
    val historicalData: StateFlow<List<SensorDataPoint>> = _historicalData.asStateFlow()

    // 压力提醒相关
    private val _showAlertDialog = MutableStateFlow(false)
    val showAlertDialog: StateFlow<Boolean> = _showAlertDialog.asStateFlow()

    // 数据接收回调（供Activity设置）
    var onDataReceived: ((String) -> Unit)? = null

    private val _alertMessage = MutableStateFlow("")
    val alertMessage: StateFlow<String> = _alertMessage.asStateFlow()

    // 压力提醒启用状态
    private val _pressureAlertsEnabled = MutableStateFlow(true)
    val pressureAlertsEnabled: StateFlow<Boolean> = _pressureAlertsEnabled.asStateFlow()

    /**
     * 处理接收到的蓝牙数据
     * 包含压力提醒检查，业务逻辑在 ViewModel 中处理
     */
    fun processReceivedData(data: String, shouldRecord: Boolean = true) {
        val result = sensorDataRepository.processReceivedData(data, shouldRecord)
        result?.let { (values, extras) ->
            // 修复：颜色应该基于 extras（传感器数值）计算，而不是 values
            updateColors(extras)
            updateExtraValues(extras)
            refreshHistoricalData()

            // 检查压力提醒（业务逻辑在 ViewModel 中处理）
            if (_pressureAlertsEnabled.value) {
                checkAndTriggerPressureAlerts(extras)
            }
        }
    }

    /**
     * 检查并触发压力提醒
     * 业务逻辑：将压力数值转换为用户友好的提醒消息
     */
    private fun checkAndTriggerPressureAlerts(extras: List<Int>) {
        val alerts = sensorDataRepository.checkPressureAlerts(extras)
        if (alerts.isNotEmpty()) {
            val sensorNames = listOf("脚掌前部", "脚弓部", "脚跟部")
            // 取第一个异常压力作为提醒消息
            val (index, value) = alerts.first()
            _alertMessage.value = "${sensorNames[index]} 检测到异常压力: ${value}"
            _showAlertDialog.value = true
        }
    }

    /**
     * 更新传感器颜色
     */
    private fun updateColors(values: List<Int>) {
        val colors = values.map { ColorUtils.calculateColorFromPressure(it) }
        _sensorColors.value = colors
    }

    /**
     * 更新显示的传感器数值
     */
    private fun updateExtraValues(values: List<Int>) {
        _extraValues.value = values
    }

    /**
     * 刷新历史数据
     */
    private fun refreshHistoricalData() {
        _historicalData.value = sensorDataRepository.getHistoricalData()
    }

    /**
     * 重置传感器显示状态
     */
    fun resetSensorDisplayState() {
        _sensorColors.value = listOf(INITIAL_SENSOR_COLOR, INITIAL_SENSOR_COLOR, INITIAL_SENSOR_COLOR)
        _extraValues.value = listOf(0, 0, 0)
    }

    /**
     * 清空所有传感器数据
     */
    fun clearSensorData() {
        sensorDataRepository.clearSensorData()
        _historicalData.value = emptyList()
        resetSensorDisplayState()
    }

    /**
     * 获取备份数据（用于上传）
     */
    fun getBackupDataForUpload(): List<SensorDataPoint> {
        return sensorDataRepository.getBackupDataForUpload()
    }

    /**
     * 获取历史数据大小
     */
    fun getHistoricalDataSize(): Int = sensorDataRepository.getHistoricalDataSize()

    /**
     * 获取备份数据大小
     */
    fun getBackupDataSize(): Int = sensorDataRepository.getBackupDataSize()

    /**
     * 检查备份数据是否为空
     */
    fun isBackupDataEmpty(): Boolean = sensorDataRepository.isBackupDataEmpty()

    /**
     * 生成模拟数据
     */
    fun generateMockData(count: Int = 10000, timeRangeMinutes: Int = 15) {
        viewModelScope.launch {
            sensorDataRepository.generateMockData(count, timeRangeMinutes)
            _historicalData.value = sensorDataRepository.getHistoricalData()

            // 更新最新数据的颜色和数值
            val latestData = sensorDataRepository.getLatestData()
            if (latestData != null) {
                updateColors(listOf(latestData.sensor1, latestData.sensor2, latestData.sensor3))
                updateExtraValues(listOf(latestData.sensor1, latestData.sensor2, latestData.sensor3))
            }
        }
    }

    /**
     * 设置压力提醒启用状态
     * 重构：通过 UserPreferencesManager 管理，不再直接访问 LocalDataSource
     * 状态保存在 ViewModel 中，不直接操作 Repository
     */
    fun setPressureAlertsEnabled(enabled: Boolean) {
        _pressureAlertsEnabled.value = enabled
        userPreferencesManager.setPressureAlertsEnabled(enabled)
    }

    /**
     * 初始化压力提醒设置
     * 从 UserPreferencesManager 读取设置
     */
    fun initPressureAlertsSetting() {
        val enabled = userPreferencesManager.getPressureAlertsEnabled()
        _pressureAlertsEnabled.value = enabled
    }

    /**
     * 关闭提醒弹窗
     */
    fun dismissAlertDialog() {
        _showAlertDialog.value = false
    }

    /**
     * 上传传感器数据到服务器
     * 封装 SensorDataManager 的上传功能，供 UI 层调用
     *
     * @param dataPoints 要上传的数据点列表
     * @param onResult 上传结果回调 (success, message)
     */
    fun uploadDataToServer(
        dataPoints: List<SensorDataPoint>,
        onResult: (Boolean, String) -> Unit
    ) {
        // 检查登录状态
        if (!sensorDataManager.isLoggedIn()) {
            onResult(false, "请先登录后再上传数据")
            return
        }

        // 检查数据是否为空
        if (dataPoints.isEmpty()) {
            onResult(false, "没有数据可上传")
            return
        }

        // 调用 Manager 上传数据
        sensorDataManager.uploadSensorData(dataPoints) { success, message, info ->
            val resultMessage = if (success && info != null) {
                val compressionInfo = String.format(
                    "压缩率: %.1f%% (${info.originalSize}B → ${info.compressedSize}B)",
                    (1 - info.compressionRatio) * 100
                )
                "上传成功! $compressionInfo"
            } else {
                message
            }
            onResult(success, resultMessage)
        }
    }

    /**
     * 获取用户的数据记录列表
     * 封装 SensorDataManager 的查询功能
     *
     * @param page 页码
     * @param onResult 查询结果回调 (success, message, records)
     */
    fun getUserRecords(
        page: Int = 0,
        onResult: (Boolean, String, List<com.example.smartshoe.data.model.SensorDataRecord>?) -> Unit
    ) {
        // 检查登录状态
        if (!sensorDataManager.isLoggedIn()) {
            onResult(false, "请先登录", null)
            return
        }

        // 调用 Manager 获取记录
        sensorDataManager.getUserRecords(page = page) { success, message, records, total ->
            val fullMessage = "$message (共${total}条)"
            onResult(success, fullMessage, records)
        }
    }

    /**
     * 清除应用缓存
     * 包括传感器数据、本地存储等
     */
    fun clearCache() {
        viewModelScope.launch {
            // 清除传感器数据
            sensorDataRepository.clearSensorData()
            _historicalData.value = emptyList()
            resetSensorDisplayState()
        }
    }
}
