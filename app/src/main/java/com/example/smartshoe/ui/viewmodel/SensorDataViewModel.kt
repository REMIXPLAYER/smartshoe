package com.example.smartshoe.ui.viewmodel

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartshoe.config.AppConfig
import com.example.smartshoe.data.manager.BluetoothConnectionManager
import com.example.smartshoe.data.manager.UserPreferencesManager
import com.example.smartshoe.domain.model.SensorDataPoint
import com.example.smartshoe.domain.model.SensorDataRecord
import com.example.smartshoe.domain.model.PressureStatus
import com.example.smartshoe.domain.repository.SensorDataRemoteRepository
import com.example.smartshoe.domain.repository.SensorDataRepository
import com.example.smartshoe.util.ColorUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

// 导入深灰色常量，用于初始状态
private val INITIAL_SENSOR_COLOR = ColorUtils.COLOR_ZERO

/**
 * 传感器数据视图模型
 * 管理传感器数据相关的UI状态和逻辑
 *
 * 重构：
 * 1. 添加 SensorDataRemoteRepository 依赖，封装数据上传功能
 * 2. 添加 UserPreferencesManager 依赖，管理用户偏好设置
 * 3. 添加 BluetoothConnectionManager 依赖，直接收集蓝牙原始数据流
 * 4. 消除 MainActivity 直接访问 Manager 和 LocalDataSource 的问题
 * 5. 消除 MainActivity 作为蓝牙数据中转站的职责
 */
@HiltViewModel
class SensorDataViewModel @Inject constructor(
    private val sensorDataRepository: SensorDataRepository,
    private val sensorDataRemoteRepository: SensorDataRemoteRepository,
    private val userPreferencesManager: UserPreferencesManager,
    private val bluetoothConnectionManager: BluetoothConnectionManager
) : ViewModel() {

    // 传感器颜色状态 - 基于瞬时数值渐变渲染（使用深灰色作为初始状态）
    private val _sensorColors = MutableStateFlow<List<Color>>(
        listOf(INITIAL_SENSOR_COLOR, INITIAL_SENSOR_COLOR, INITIAL_SENSOR_COLOR)
    )
    val sensorColors: StateFlow<List<Color>> = _sensorColors.asStateFlow()

    // 传感器数值（原始值）
    private val _extraValues = MutableStateFlow<List<Int>>(listOf(0, 0, 0))
    val extraValues: StateFlow<List<Int>> = _extraValues.asStateFlow()

    // 加权平均值
    private val _weightedAverages = MutableStateFlow<List<Float>>(listOf(0f, 0f, 0f))
    val weightedAverages: StateFlow<List<Float>> = _weightedAverages.asStateFlow()

    // 压力状态描述
    private val _pressureStatuses = MutableStateFlow<List<PressureStatus>>(
        listOf(PressureStatus.NONE, PressureStatus.NONE, PressureStatus.NONE)
    )
    val pressureStatuses: StateFlow<List<PressureStatus>> = _pressureStatuses.asStateFlow()

    // 历史数据
    private val _historicalData = MutableStateFlow<List<SensorDataPoint>>(emptyList())
    val historicalData: StateFlow<List<SensorDataPoint>> = _historicalData.asStateFlow()

    // 压力提醒相关
    private val _showAlertDialog = MutableStateFlow(false)
    val showAlertDialog: StateFlow<Boolean> = _showAlertDialog.asStateFlow()

    private val _alertMessage = MutableStateFlow("")
    val alertMessage: StateFlow<String> = _alertMessage.asStateFlow()

    // 压力提醒启用状态
    private val _pressureAlertsEnabled = MutableStateFlow(true)
    val pressureAlertsEnabled: StateFlow<Boolean> = _pressureAlertsEnabled.asStateFlow()

    // 蓝牙错误状态（从 BluetoothConnectionManager 转发）
    private val _bluetoothError = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val bluetoothError: SharedFlow<String> = _bluetoothError.asSharedFlow()

    // 蓝牙连接结果（从 BluetoothConnectionManager 转发）
    private val _bluetoothConnectionResult = MutableSharedFlow<Pair<Boolean, String?>>(extraBufferCapacity = 16)
    val bluetoothConnectionResult: SharedFlow<Pair<Boolean, String?>> = _bluetoothConnectionResult.asSharedFlow()

    init {
        // 收集蓝牙原始数据流
        // 注意：rawDataFlow 是 SharedFlow，即使无订阅者也会接收数据
        // 当蓝牙未连接时，数据仍会被解析但 shouldRecord=false
        viewModelScope.launch {
            bluetoothConnectionManager.rawDataFlow.collect { data ->
                val shouldRecord = bluetoothConnectionManager.connectedDevice.value != null
                processReceivedData(data, shouldRecord)
            }
        }

        // 收集蓝牙错误流并转发到 UI
        viewModelScope.launch {
            bluetoothConnectionManager.errorFlow.collect { error ->
                _bluetoothError.emit(error)
            }
        }

        // 收集蓝牙连接结果流并转发到 UI
        viewModelScope.launch {
            bluetoothConnectionManager.connectionResultFlow.collect { result ->
                _bluetoothConnectionResult.emit(result)
            }
        }
    }

    /**
     * 处理接收到的蓝牙数据
     * 包含压力提醒检查，业务逻辑在 ViewModel 中处理
     * 颜色渲染：基于瞬时数值（extras）使用渐变方式
     * 压力提醒：基于加权平均值
     *
     * 优化：使用 suspend + withContext 切换调度器，避免阻塞主线程
     * 注意：此方法应在 collect 协程中顺序调用，不创建新协程，保证数据顺序
     * 蓝牙数据频率较高（10-50Hz），计算密集型操作在 Dispatchers.Default 执行
     */
    suspend fun processReceivedData(data: String, shouldRecord: Boolean = true) {
        // 切换到 Default 调度器执行计算密集型操作
        val result = withContext(Dispatchers.Default) {
            sensorDataRepository.processReceivedData(data, shouldRecord)
        }
        result?.let { (values, extras) ->
            // withContext 会自动恢复原来的调度器（Main）
            // 更新原始数值
            updateExtraValues(extras)
            refreshHistoricalData()

            // 颜色渲染：基于瞬时数值（渐变方式）
            updateColorsFromExtras(extras)

            // 加权平均值和压力状态（用于显示和报警）
            updateWeightedAverages()
            updatePressureStatuses()

            // 检查压力提醒（基于加权平均值）
            if (_pressureAlertsEnabled.value) {
                checkAndTriggerPressureAlertsFromWeighted()
            }
        }
    }

    /**
     * 基于瞬时数值更新传感器颜色（渐变渲染）
     */
    private fun updateColorsFromExtras(extras: List<Int>) {
        val colors = extras.map { ColorUtils.calculateColorFromPressure(it) }
        _sensorColors.value = colors
    }

    /**
     * 更新加权平均值
     */
    private fun updateWeightedAverages() {
        _weightedAverages.value = sensorDataRepository.getWeightedAverages()
    }

    /**
     * 更新压力状态
     */
    private fun updatePressureStatuses() {
        _pressureStatuses.value = sensorDataRepository.getPressureStatuses()
    }

    /**
     * 基于加权平均值检查并触发压力提醒
     * 报警阈值：滑动窗口加权平均值 > 1350
     */
    private fun checkAndTriggerPressureAlertsFromWeighted() {
        val weightedAvgs = sensorDataRepository.getWeightedAverages()

        // 找到第一个超过报警阈值的传感器（加权平均值 > 1350）
        val alertIndex = weightedAvgs.indexOfFirst { it > PRESSURE_ALERT_THRESHOLD }

        if (alertIndex != -1) {
            val sensorNames = listOf("脚掌前部", "脚弓部", "脚跟部")
            _alertMessage.value = "${sensorNames[alertIndex]} 压力异常，请注意足部健康哦~"
            _showAlertDialog.value = true
        }
    }

    companion object {
        // 报警阈值：从 AppConfig 读取
        val PRESSURE_ALERT_THRESHOLD = AppConfig.Sensor.ALERT_THRESHOLD_WEIGHTED
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
        _weightedAverages.value = listOf(0f, 0f, 0f)
        _pressureStatuses.value = listOf(PressureStatus.NONE, PressureStatus.NONE, PressureStatus.NONE)
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
     * 修复：使用加权平均值更新颜色和状态
     */
    fun generateMockData(count: Int = 10000, timeRangeMinutes: Int = 15) {
        viewModelScope.launch {
            sensorDataRepository.generateMockData(count, timeRangeMinutes)
            _historicalData.value = sensorDataRepository.getHistoricalData()

            // 更新最新数据的颜色和数值
            val latestData = sensorDataRepository.getLatestData()
            if (latestData != null) {
                val extras = listOf(latestData.sensor1, latestData.sensor2, latestData.sensor3)
                updateExtraValues(extras)
                // 颜色渲染：基于瞬时数值（渐变方式）
                updateColorsFromExtras(extras)
                // 加权平均值和压力状态
                updateWeightedAverages()
                updatePressureStatuses()
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
     * 封装 SensorDataRemoteRepository 的上传功能，供 UI 层调用
     *
     * @param dataPoints 要上传的数据点列表
     * @param onResult 上传结果回调 (success, message)
     */
    fun uploadDataToServer(
        dataPoints: List<SensorDataPoint>,
        onResult: (Boolean, String) -> Unit
    ) {
        Log.d("BackupDebug", "========== SensorDataViewModel.uploadDataToServer 被调用 ==========")
        Log.d("BackupDebug", "数据点数量: ${dataPoints.size}")

        // 检查登录状态
        if (!sensorDataRemoteRepository.isLoggedIn()) {
            Log.w("BackupDebug", "未登录，无法上传")
            onResult(false, "请先登录后再上传数据")
            return
        }
        Log.d("BackupDebug", "已登录，继续上传流程")

        // 检查数据是否为空
        if (dataPoints.isEmpty()) {
            Log.w("BackupDebug", "数据为空，无法上传")
            onResult(false, "没有数据可上传")
            return
        }

        // 调用 Repository 上传数据
        Log.d("BackupDebug", "调用 sensorDataRemoteRepository.uploadSensorData()")
        sensorDataRemoteRepository.uploadSensorData(dataPoints) { success, message, info ->
            Log.d("BackupDebug", "sensorDataRemoteRepository.uploadSensorData 回调: success=$success, message=$message")
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
     * 执行数据备份（简化版）
     * 自动获取本地数据并上传，上传成功后清空本地历史数据
     * 所有业务逻辑封装在ViewModel中，UI层只需调用此方法
     *
     * @param onComplete 完成回调，返回是否成功
     */
    fun backupData(onComplete: (Boolean) -> Unit) {
        val dataPoints = getBackupDataForUpload()

        if (dataPoints.isEmpty()) {
            onComplete(false)
            return
        }

        uploadDataToServer(dataPoints) { success, _ ->
            if (success) {
                clearSensorData()
            }
            onComplete(success)
        }
    }

    /**
     * 获取用户的数据记录列表
     * 封装 SensorDataRemoteRepository 的查询功能
     *
     * @param page 页码
     * @param onResult 查询结果回调 (success, message, records)
     */
    fun getUserRecords(
        page: Int = 0,
        onResult: (Boolean, String, List<SensorDataRecord>?) -> Unit
    ) {
        // 检查登录状态
        if (!sensorDataRemoteRepository.isLoggedIn()) {
            onResult(false, "请先登录", null)
            return
        }

        // 调用 Repository 获取记录
        sensorDataRemoteRepository.getUserRecords(page = page) { success, message, records, total ->
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
