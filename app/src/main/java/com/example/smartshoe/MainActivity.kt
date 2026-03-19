package com.example.smartshoe

import android.Manifest
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
import javax.inject.Inject
import com.example.smartshoe.data.manager.BluetoothConnectionManager
import com.example.smartshoe.data.manager.CacheManager
import com.example.smartshoe.data.manager.PerformanceMonitor
import com.example.smartshoe.ui.viewmodel.AuthViewModel
import com.example.smartshoe.ui.viewmodel.BluetoothViewModel
import com.example.smartshoe.ui.viewmodel.HistoryRecordViewModel
import com.example.smartshoe.ui.viewmodel.MainViewModel
import com.example.smartshoe.ui.viewmodel.SensorDataViewModel
import com.example.smartshoe.ui.viewmodel.UserProfileViewModel
import com.example.smartshoe.ui.screen.MainScreen
import com.example.smartshoe.ui.screen.MainScreenState
import com.example.smartshoe.ui.screen.MainScreenCallbacks


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
    // 通过 Hilt 注入蓝牙连接管理器
    @Inject
    lateinit var bluetoothConnectionManager: BluetoothConnectionManager

    // 蓝牙权限请求码
    private val REQUEST_BLUETOOTH_PERMISSIONS = 1

    // 压力提醒冷却时间
    private var lastAlertTime = 0L

    // 通过Hilt注入管理器
    // 重构：移除不必要的Manager注入，通过ViewModel访问
    // @Inject lateinit var authManager: AuthManager
    // @Inject lateinit var sensorDataManager: SensorDataManager
    // memoryLeakDetector已从Application获取，不再在这里注入
    // @Inject lateinit var memoryLeakDetector: MemoryLeakDetector
    // @Inject lateinit var bluetoothResourceManager: BluetoothResourceManager
    @Inject
    lateinit var performanceMonitor: PerformanceMonitor

    // 缓存管理器
    @Inject
    lateinit var cacheManager: CacheManager

    // localDataSource不再直接访问，通过Manager/Repository访问
    // @Inject lateinit var localDataSource: LocalDataSource

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

        // 初始化登录状态（AuthViewModel 会收集 AuthManager 的 StateFlow）
        // 不需要手动调用，AuthManager 初始化时会自动加载状态

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
                historicalData = sensorDataViewModel.historicalData.collectAsStateWithLifecycle().value,
                connectedDevice = bluetoothViewModel.connectedDevice.collectAsStateWithLifecycle().value,
                userWeight = userProfileViewModel.userWeight.collectAsStateWithLifecycle().value,
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
                selectedTab = mainViewModel.selectedTab.collectAsStateWithLifecycle().value
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
                        userId = authViewModel.userState.value.userId,
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
                // 设置
                onPressureAlertsChange = { enabled -> sensorDataViewModel.setPressureAlertsEnabled(enabled) },
                // 弹窗
                onDismissAlert = { sensorDataViewModel.dismissAlertDialog() },
                // 缓存和备份
                onClearCache = { clearAllCache() },
                onBackupData = { forceUpload, uploadType, onUploadComplete ->
                    val dataPoints = sensorDataViewModel.getBackupDataForUpload()
                    sensorDataViewModel.uploadDataToServer(
                        dataPoints = dataPoints,
                        onResult = { success, _ ->
                            if (success) historyRecordViewModel.clearHistoryData()
                            onUploadComplete(success)
                        }
                    )
                },
                // 历史记录
                onQueryHistory = { historyRecordViewModel.queryHistoryRecords() },
                onRecordSelect = { record -> historyRecordViewModel.selectRecord(record) },
                onStartDateChange = { date -> historyRecordViewModel.updateStartDate(date) },
                onEndDateChange = { date -> historyRecordViewModel.updateEndDate(date) },
                // 导航
                onTabSelected = { tabIndex -> mainViewModel.selectTab(tabIndex) },
                // Debug
                onGenerateMockData = {
                    sensorDataViewModel.generateMockData()
                    Toast.makeText(this@MainActivity, "已生成模拟数据", Toast.LENGTH_SHORT).show()
                }
            )

            MainScreen(
                state = state,
                callbacks = callbacks
            )
        }
    }



    /**
     * 清除所有缓存
     * 包括：用户数据、传感器数据、历史记录、蓝牙设备、文件缓存等
     *
     * 职责拆分：
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
        super.onDestroy()

        // 1. 断开蓝牙连接
        bluetoothViewModel.disconnectDevice()

        // 2. 清空数据缓冲区，释放内存
        // ViewModel 会自动处理生命周期，不需要手动清理
        // 但我们可以清空数据
        sensorDataViewModel.clearSensorData()
        historyRecordViewModel.clearHistoryData()
        bluetoothViewModel.clearDevices()

        // 3. 停止内存泄漏检测（从Application获取）
        (application as? SmartShoeApplication)?.getMemoryLeakDetector()?.unwatch(this, "Activity")

        // 4. 停止性能监控
        if (::performanceMonitor.isInitialized) {
            performanceMonitor.stopMonitoring()
        }

        // 5. 建议系统进行垃圾回收
        System.gc()
    }
}
