package com.example.smartshoe.ui.screen.main

import android.bluetooth.BluetoothDevice
import androidx.compose.ui.graphics.Color
import com.example.smartshoe.domain.model.SensorDataPoint
import com.example.smartshoe.domain.model.SensorDataRecord
import com.example.smartshoe.domain.model.UserState
import java.util.Date

/**
 * 主屏幕状态数据类
 * 封装所有UI状态，简化参数传递
 */
data class MainScreenState(
    // 传感器数据
    val scannedDevices: List<BluetoothDevice> = emptyList(),
    val sensorColors: List<Color> = emptyList(),
    val extraValues: List<Int> = emptyList(),
    val pressureStatuses: List<com.example.smartshoe.domain.model.PressureStatus> = emptyList(),
    val historicalData: List<SensorDataPoint> = emptyList(),
    val connectedDevice: BluetoothDevice? = null,
    val userWeight: Float = 0f,
    // 蓝牙扫描状态
    val isScanning: Boolean = false,
    // 蓝牙连接状态
    val isConnecting: Boolean = false,
    val connectingDeviceAddress: String? = null,
    // 用户认证
    val userState: UserState = UserState(),
    val isLoggedIn: Boolean = false,
    // 设置
    val pressureAlertsEnabled: Boolean = true,
    val hasData: Boolean = false,
    // 弹窗
    val showAlertDialog: Boolean = false,
    val alertMessage: String = "",
    // 历史记录
    val historyRecords: List<SensorDataRecord> = emptyList(),
    val selectedHistoryRecord: SensorDataRecord? = null,
    val recordData: List<SensorDataPoint> = emptyList(),
    val isHistoryLoading: Boolean = false,
    val isRecordDetailLoading: Boolean = false,
    val historyStartDate: Date? = null,
    val historyEndDate: Date? = null,
    val queryExecuted: Boolean = false,
    // 导航
    val selectedTab: Int = 0,
    // Snackbar 消息
    val snackbarMessage: String? = null,
    val snackbarType: SnackbarType = SnackbarType.Info
)

/**
 * Snackbar 类型
 */
enum class SnackbarType {
    Success,
    Error,
    Info,
    Warning
}
