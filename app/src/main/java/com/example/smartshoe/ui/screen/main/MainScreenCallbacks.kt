package com.example.smartshoe.ui.screen.main

import android.bluetooth.BluetoothDevice
import com.example.smartshoe.domain.model.SensorDataRecord
import com.example.smartshoe.ui.viewmodel.AiAssistantViewModel
import com.example.smartshoe.ui.viewmodel.AuthUiState
import com.example.smartshoe.ui.viewmodel.SettingViewModel
import java.util.Date

/**
 * 主屏幕回调数据类
 * 封装所有回调函数，简化参数传递
 */
data class MainScreenCallbacks(
    // 蓝牙设备
    val onScanDevices: () -> Unit = {},
    val onConnectDevice: (BluetoothDevice) -> Unit = {},
    val onDisconnectDevice: () -> Unit = {},
    // 用户资料
    val onEditWeight: (Float) -> Unit = {},
    val onEditProfile: (String, String, String, String) -> Unit = { _, _, _, _ -> }, // username, email, password, currentPassword
    // 用户认证
    val onLogin: (String, String) -> Unit = { _, _ -> },
    val onRegister: (String, String, String) -> Unit = { _, _, _ -> },
    val onLogout: () -> Unit = {},
    // 认证UI状态（用于监听登录/注册/修改资料状态变化）
    val authUiState: AuthUiState = AuthUiState.Idle,
    // 设置
    val onPressureAlertsChange: (Boolean) -> Unit = {},
    // 弹窗
    val onDismissAlert: () -> Unit = {},
    // 缓存和备份
    val onClearCache: () -> Unit = {},
    val onBackupData: (Boolean, String, (Boolean) -> Unit) -> Unit = { _, _, _ -> },
    // 历史记录
    val onQueryHistory: () -> Unit = {},
    val onRecordSelect: (SensorDataRecord?) -> Unit = {},
    val onStartDateChange: (Date?) -> Unit = {},
    val onEndDateChange: (Date?) -> Unit = {},
    val onShowDatePicker: ((Date, (Date) -> Unit) -> Unit)? = null,
    // 导航
    val onTabSelected: (Int) -> Unit = {},
    // Debug
    val onGenerateMockData: () -> Unit = {},
    // SettingViewModel
    val settingViewModel: SettingViewModel? = null,
    // AI助手
    val aiAssistantViewModel: AiAssistantViewModel? = null,
    val userToken: String = "",
    // 错误提示
    val onShowError: ((String) -> Unit)? = null,
    // AI分析 - 从历史记录页面跳转
    val onAiAnalysisClick: ((String) -> Unit)? = null,
    // Snackbar
    val onSnackbarDismiss: () -> Unit = {}
)
