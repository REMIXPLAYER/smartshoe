package com.example.smartshoe

import android.Manifest
import android.app.DatePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import com.example.smartshoe.data.manager.BluetoothConnectionManager
import com.example.smartshoe.data.manager.CacheManager
import com.example.smartshoe.data.manager.PerformanceMonitor
import com.example.smartshoe.ui.viewmodel.AiAssistantViewModel
import com.example.smartshoe.ui.viewmodel.AuthViewModel
import com.example.smartshoe.ui.viewmodel.BluetoothViewModel
import com.example.smartshoe.ui.viewmodel.HistoryRecordViewModel
import com.example.smartshoe.ui.viewmodel.MainViewModel
import com.example.smartshoe.ui.viewmodel.SensorDataViewModel
import com.example.smartshoe.ui.viewmodel.SettingViewModel
import com.example.smartshoe.ui.viewmodel.UserProfileViewModel
import com.example.smartshoe.ui.screen.MainScreen
import com.example.smartshoe.ui.screen.MainScreenState
import com.example.smartshoe.ui.screen.MainScreenCallbacks
import com.example.smartshoe.ui.screen.SnackbarType


/**
 * 主活动类，负责应用的生命周期管理和主要功能实现
 * 包含蓝牙连接、数据接收和UI显示等功能
 * 
 * 使用 Hilt 进行依赖注入，ViewModel 管理状态
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // 使用 Hilt 注入 ViewModel
    private val authViewModel: AuthViewModel by viewModels()
    private val bluetoothViewModel: BluetoothViewModel by viewModels()
    private val sensorDataViewModel: SensorDataViewModel by viewModels()
    private val historyRecordViewModel: HistoryRecordViewModel by viewModels()
    private val userProfileViewModel: UserProfileViewModel by viewModels()
    private val mainViewModel: MainViewModel by viewModels()
    private val settingViewModel: SettingViewModel by viewModels()
    private val aiAssistantViewModel: AiAssistantViewModel by viewModels()
    // 通过 Hilt 注入蓝牙连接管理器
    @Inject
    lateinit var bluetoothConnectionManager: BluetoothConnectionManager

    // 蓝牙权限请求码
    private val REQUEST_BLUETOOTH_PERMISSIONS = 1

    // 压力提醒冷却时间
    private var lastAlertTime = 0L

    // Snackbar 状态
    private var snackbarMessage: String? = null
    private var snackbarType: SnackbarType = SnackbarType.Info

    @Inject
    lateinit var performanceMonitor: PerformanceMonitor

    @Inject
    lateinit var cacheManager: CacheManager

    private companion object {
        // 应用级组件初始化标志
        @Volatile
        var isMemoryLeakDetectorInitialized = false
    }

    // 使用Dispatchers.IO替代自定义线程池，简化资源管理
    // 蓝牙数据监听使用IO调度器，缓存清理由 CacheManager 处理




    /**
     * 应用创建时的初始化方法
     * 设置蓝牙适配器、请求权限、初始化UI
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        // 在 super.onCreate 之前配置窗口属性，确保无标题栏
        // 方法1: 使用 NoActionBar 主题
        // 方法2: 代码强制隐藏标题栏（针对某些手机厂商的定制系统）
        
        // 尝试多种方式隐藏标题栏
        requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        
        super.onCreate(savedInstanceState)

        // 初始化蓝牙连接管理器的回调
        // 数据直接传递给 SensorDataViewModel 处理
        bluetoothConnectionManager.onDataReceived = { data ->
            sensorDataViewModel.processReceivedData(
                data,
                shouldRecord = bluetoothViewModel.connectedDevice.value != null
            )
        }
        bluetoothConnectionManager.onError = { message ->
            runOnUiThread {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        }
        bluetoothConnectionManager.onConnectionResult = { success, errorMessage ->
            runOnUiThread {
                if (success) {
                    showSnackbarMessage("蓝牙设备连接成功", SnackbarType.Success)
                } else {
                    showSnackbarMessage(errorMessage ?: "连接失败", SnackbarType.Error)
                }
            }
        }

        // 初始化历史记录ViewModel的回调
        historyRecordViewModel.onError = { message ->
            runOnUiThread {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        }

        // 初始化内存泄漏检测器（应用级组件，从Application获取）
        // 只在首次启动时初始化一次，后续Activity不再重复初始化
        if (!isMemoryLeakDetectorInitialized) {
            (application as SmartShoeApplication).getMemoryLeakDetector().init(application)
            isMemoryLeakDetectorInitialized = true
        }

        // 初始化体重数据
        // 重构：通过 UserProfileViewModel 初始化，不再直接访问 LocalDataSource
        userProfileViewModel.initUserWeight()

        // 初始化登录状态（AuthViewModel 会收集 AuthRepository 的 StateFlow）
        // 不需要手动调用，AuthRepository 初始化时会自动加载状态

        // 初始化压力提醒设置
        // 重构：通过 SensorDataViewModel 初始化，不再直接访问 LocalDataSource
        sensorDataViewModel.initPressureAlertsSetting()

        // 检查和请求必要的蓝牙权限
        checkAndRequestPermissions()

        // 设置Compose UI内容
        // 重构：使用 MainScreen 组件，将UI逻辑从Activity分离
        // 使用数据类封装状态和回调，符合Clean Architecture
        setContent {
            // 收集所有ViewModel状态
            val state = MainScreenState(
                // 传感器数据
                scannedDevices = bluetoothViewModel.scannedDevices.collectAsStateWithLifecycle().value,
                sensorColors = sensorDataViewModel.sensorColors.collectAsStateWithLifecycle().value,
                extraValues = sensorDataViewModel.extraValues.collectAsStateWithLifecycle().value,
                pressureStatuses = sensorDataViewModel.pressureStatuses.collectAsStateWithLifecycle().value,
                historicalData = sensorDataViewModel.historicalData.collectAsStateWithLifecycle().value,
                connectedDevice = bluetoothViewModel.connectedDevice.collectAsStateWithLifecycle().value,
                userWeight = userProfileViewModel.userWeight.collectAsStateWithLifecycle().value,
                // 蓝牙扫描状态
                isScanning = bluetoothViewModel.isScanning.collectAsStateWithLifecycle().value,
                // 蓝牙连接状态
                isConnecting = bluetoothViewModel.isConnecting.collectAsStateWithLifecycle().value,
                connectingDeviceAddress = bluetoothViewModel.connectingDeviceAddress.collectAsStateWithLifecycle().value,
                // 用户认证
                userState = authViewModel.userState.collectAsStateWithLifecycle().value,
                isLoggedIn = authViewModel.userState.collectAsStateWithLifecycle().value.isLoggedIn,
                // 设置
                pressureAlertsEnabled = sensorDataViewModel.pressureAlertsEnabled.collectAsStateWithLifecycle().value,
                hasData = !sensorDataViewModel.isBackupDataEmpty(),
                // 弹窗
                showAlertDialog = sensorDataViewModel.showAlertDialog.collectAsStateWithLifecycle().value,
                alertMessage = sensorDataViewModel.alertMessage.collectAsStateWithLifecycle().value,
                // 历史记录
                historyRecords = historyRecordViewModel.historyRecords.collectAsStateWithLifecycle().value,
                selectedHistoryRecord = historyRecordViewModel.selectedHistoryRecord.collectAsStateWithLifecycle().value,
                recordData = historyRecordViewModel.selectedRecordData.collectAsStateWithLifecycle().value,
                isHistoryLoading = historyRecordViewModel.isHistoryLoading.collectAsStateWithLifecycle().value,
                isRecordDetailLoading = historyRecordViewModel.isRecordDetailLoading.collectAsStateWithLifecycle().value,
                historyStartDate = historyRecordViewModel.historyStartDate.collectAsStateWithLifecycle().value,
                historyEndDate = historyRecordViewModel.historyEndDate.collectAsStateWithLifecycle().value,
                queryExecuted = historyRecordViewModel.queryExecuted.collectAsStateWithLifecycle().value,
                // 导航
                selectedTab = mainViewModel.selectedTab.collectAsStateWithLifecycle().value,
                // Snackbar
                snackbarMessage = snackbarMessage,
                snackbarType = snackbarType
            )

            val callbacks = MainScreenCallbacks(
                // 蓝牙设备
                onScanDevices = { bluetoothViewModel.startScan() },
                onConnectDevice = { device -> bluetoothViewModel.connectDevice(device) },
                onDisconnectDevice = { bluetoothViewModel.disconnectDevice() },
                // 用户资料
                onEditWeight = { weight -> userProfileViewModel.updateUserWeight(weight) },
                onEditProfile = { username, email, password, currentPassword ->
                    authViewModel.updateProfile(
                        currentPassword = currentPassword,
                        newUsername = username,
                        newEmail = email,
                        newPassword = password
                    )
                },
                // 用户认证
                onLogin = { email, password -> authViewModel.login(email, password) },
                onRegister = { username, email, password -> authViewModel.register(username, email, password) },
                onLogout = { authViewModel.logout() },
                // 认证UI状态
                authUiState = authViewModel.uiState.collectAsStateWithLifecycle().value,
                // 设置
                onPressureAlertsChange = { enabled -> sensorDataViewModel.setPressureAlertsEnabled(enabled) },
                // 弹窗
                onDismissAlert = { sensorDataViewModel.dismissAlertDialog() },
                // 缓存和备份
                onClearCache = { clearAllCache() },
                onBackupData = { _, _, onUploadComplete ->
                    // 业务逻辑已封装在ViewModel中
                    sensorDataViewModel.backupData { success ->
                        if (success) {
                            historyRecordViewModel.clearHistoryData()
                        }
                        onUploadComplete(success)
                    }
                },
                // 历史记录
                onQueryHistory = { historyRecordViewModel.queryHistoryRecords() },
                onRecordSelect = { record -> historyRecordViewModel.selectRecord(record) },
                onStartDateChange = { date -> historyRecordViewModel.updateStartDate(date) },
                onEndDateChange = { date -> historyRecordViewModel.updateEndDate(date) },
                onShowDatePicker = { initialDate, onDateSelected ->
                    showDatePicker(initialDate, onDateSelected)
                },
                // 导航
                onTabSelected = { tabIndex -> mainViewModel.selectTab(tabIndex) },
                // Debug
                onGenerateMockData = {
                    sensorDataViewModel.generateMockData()
                    Toast.makeText(this@MainActivity, "已生成模拟数据", Toast.LENGTH_SHORT).show()
                },
                // SettingViewModel
                settingViewModel = settingViewModel,
                // AI助手
                aiAssistantViewModel = aiAssistantViewModel,
                userToken = authViewModel.tokenState.value ?: "",
                // 错误提示
                onShowError = { message ->
                    Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                },
                // AI分析 - 从历史记录页面跳转
                onAiAnalysisClick = { recordId ->
                    // 切换到AI助手页面
                    mainViewModel.selectTab(3)
                    // 触发分析
                    aiAssistantViewModel?.analyzeRecord(recordId, authViewModel.tokenState.value ?: "")
                },
                // Snackbar
                onSnackbarDismiss = { 
                    snackbarMessage = null 
                }
            )

            MainScreen(
                state = state,
                callbacks = callbacks
            )
        }
    }



    /**
     * 显示 Snackbar 消息
     * @param message 消息内容
     * @param type 消息类型
     */
    private fun showSnackbarMessage(message: String, type: SnackbarType = SnackbarType.Info) {
        snackbarMessage = message
        snackbarType = type
    }

    /**
     * 显示日期选择器
     * @param initialDate 初始日期
     * @param onDateSelected 日期选择回调
     */
    private fun showDatePicker(initialDate: Date, onDateSelected: (Date) -> Unit) {
        val calendar = Calendar.getInstance()
        calendar.time = initialDate

        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                onDateSelected(calendar.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    /**
     * 清除所有应用缓存
     * 包括：业务数据、用户偏好设置、文件缓存
     * 
     * 清理流程：
     * - 业务数据清理：由各 ViewModel 处理
     * - 文件缓存清理：由 CacheManager 处理
     * - 用户偏好设置：由 CacheManager 委托 UserPreferencesManager 处理
     */
    private fun clearAllCache() {
        try {
            // 1. 清理业务数据（由各 ViewModel 处理）
            // 重置用户状态
            authViewModel.logout()

            // 重置体重
            userProfileViewModel.updateUserWeight(0f)

            // 清空传感器数据
            sensorDataViewModel.clearSensorData()

            // 清空历史记录数据
            historyRecordViewModel.clearHistoryData()

            // 清空AI助手消息
            aiAssistantViewModel.clearMessages()

            // 清空蓝牙设备列表并断开连接
            bluetoothViewModel.clearDevices()
            bluetoothViewModel.disconnectDevice()

            // 重置传感器显示状态
            sensorDataViewModel.resetSensorDisplayState()

            // 2. 清理用户偏好设置
            cacheManager.clearUserPreferences()

            // 3. 清理文件缓存（由 CacheManager 处理）
            cacheManager.clearFileCache { success, message ->
                val toastMessage = if (success) "缓存已清除" else "清除缓存时出错"
                Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "清除缓存时出错", Toast.LENGTH_SHORT).show()
        }
    }






    /**
     * 检查并请求必要的蓝牙权限
     * 针对Android S（API 31）及以上版本需要特殊权限
     */
    private fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val permissionsNeeded = mutableListOf<String>()
            // 检查并添加缺失的权限
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            // 如果有缺失权限，请求权限
            if (permissionsNeeded.isNotEmpty()) {
                ActivityCompat.requestPermissions(
                    this,
                    permissionsNeeded.toTypedArray(),
                    REQUEST_BLUETOOTH_PERMISSIONS
                )
            }
        }
    }

    /**
     * Activity销毁时清理资源 - 优化协程生命周期管理
     * 确保所有资源被正确释放，防止内存泄漏
     */
    override fun onDestroy() {
        // 1. 先停止内存泄漏检测，避免误报
        (application as? SmartShoeApplication)?.getMemoryLeakDetector()?.unwatch(this, "Activity")

        // 2. 释放蓝牙连接管理器的所有资源
        if (::bluetoothConnectionManager.isInitialized) {
            bluetoothConnectionManager.releaseAllResources()
        }

        // 3. 清空数据缓冲区，释放内存
        sensorDataViewModel.clearSensorData()
        historyRecordViewModel.clearHistoryData()
        bluetoothViewModel.clearDevices()

        // 4. 停止性能监控
        if (::performanceMonitor.isInitialized) {
            performanceMonitor.stopMonitoring()
        }

        // 5. 调用父类方法
        super.onDestroy()

        // 6. 建议系统进行垃圾回收
        System.gc()
    }
}
