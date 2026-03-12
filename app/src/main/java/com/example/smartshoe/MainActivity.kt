package com.example.smartshoe

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.smartshoe.BottomNavigation.BottomNavigationBar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.util.*
import com.example.smartshoe.data.SensorDataPoint
import com.example.smartshoe.data.UserState
import com.example.smartshoe.data.repository.AuthManager
import com.example.smartshoe.data.repository.BluetoothResourceManager
import com.example.smartshoe.data.repository.DebugManager
import com.example.smartshoe.data.repository.DebugSection
import com.example.smartshoe.data.repository.HistoryRecordManager
import com.example.smartshoe.data.repository.MemoryLeakDetector
import com.example.smartshoe.data.repository.PerformanceMonitor
import com.example.smartshoe.data.repository.SensorDataManager
import com.example.smartshoe.data.repository.SensorDataProcessor
import com.example.smartshoe.draw.DrawSensor.InsoleWithSensors
import com.example.smartshoe.section.MainScreenSection.ExpandableDeviceListSection
import com.example.smartshoe.section.SettingScreenSection
import com.example.smartshoe.section.SettingScreenSection.AboutAppSection
import com.example.smartshoe.section.SettingScreenSection.DeviceAndPreferenceSection
import com.example.smartshoe.section.HistoryScreenSection
import com.example.smartshoe.section.PerformanceMonitorSection
import com.example.smartshoe.data.remote.SensorDataRecord

import com.example.smartshoe.utils.PreferencesUtils
import com.example.smartshoe.section.DataRecordScreenSection
import com.example.smartshoe.section.DataRecordScreenSection.RecordStatusHeader
import com.github.mikephil.charting.BuildConfig
import kotlinx.coroutines.isActive


/**
 * 主活动类，负责应用的生命周期管理和主要功能实现
 * 包含蓝牙连接、数据接收和UI显示等功能
 */
class MainActivity : ComponentActivity() {
    // 扫描到的蓝牙设备列表（使用可观察的状态列表）
    private val scannedDevices = mutableStateListOf<BluetoothDevice>()

    // 蓝牙适配器，用于管理蓝牙功能
    private lateinit var bluetoothAdapter: BluetoothAdapter

    // 蓝牙Socket，用于数据传输
    private var bluetoothSocket: BluetoothSocket? = null

    // HC-06蓝牙模块的标准UUID
    private val hc06UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    // 蓝牙权限请求码
    private val REQUEST_BLUETOOTH_PERMISSIONS = 1

    // 蓝牙设备连接列表
    private var connectedDevice: BluetoothDevice? by mutableStateOf(null)

    // 用户体重，单位千克
    private var userWeight by mutableStateOf(0f)

    // 添加用户状态管理
    private var userState by mutableStateOf(UserState())
    private lateinit var authManager: AuthManager
    private lateinit var sensorDataManager: SensorDataManager

    // 内存泄漏检测器和蓝牙资源管理器
    private lateinit var memoryLeakDetector: MemoryLeakDetector
    private lateinit var bluetoothResourceManager: BluetoothResourceManager
    private lateinit var performanceMonitor: PerformanceMonitor

    // 传感器数据处理器（P2拆分）
    private lateinit var sensorDataProcessor: SensorDataProcessor

    // 历史记录管理器（P3拆分）
    private lateinit var historyRecordManager: HistoryRecordManager

    // 调试管理器（调试功能拆分）
    private lateinit var debugManager: DebugManager

    // 只保留压力提醒相关变量
    private var showAlertDialog by mutableStateOf(false)
    private var alertMessage by mutableStateOf("")
    private var lastAlertTime = 0L
    private val alertCooldown = 5000L // 5秒冷却时间

    // 协程Job引用，用于生命周期管理
    private var dataListeningJob: kotlinx.coroutines.Job? = null
    private var cacheClearJob: kotlinx.coroutines.Job? = null

    // 统一的协程作用域，绑定到 Activity 生命周期
    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // 使用Dispatchers.IO替代自定义线程池，简化资源管理
    // 蓝牙数据监听使用IO调度器，缓存清理也使用IO调度器

    // 便捷属性访问
    private val sensorColors get() = sensorDataProcessor.sensorColors
    private val extraValues get() = sensorDataProcessor.extraValues
    private val historicalData get() = sensorDataProcessor.historicalData
    private val historyRecords get() = historyRecordManager.historyRecords
    private val selectedHistoryRecord get() = historyRecordManager.selectedHistoryRecord
    private val selectedRecordData get() = historyRecordManager.selectedRecordData
    private val isHistoryLoading get() = historyRecordManager.isHistoryLoading
    private val isRecordDetailLoading get() = historyRecordManager.isRecordDetailLoading
    private val queryExecuted get() = historyRecordManager.queryExecuted
    private val hasMoreHistoryPages get() = historyRecordManager.hasMoreHistoryPages
    private val historyStartDate get() = historyRecordManager.historyStartDate
    private val historyEndDate get() = historyRecordManager.historyEndDate
    private var pressureAlertsEnabled
        get() = sensorDataProcessor.pressureAlertsEnabled
        set(value) { sensorDataProcessor.pressureAlertsEnabled = value }


    /**
     * 应用创建时的初始化方法
     * 设置蓝牙适配器、请求权限、初始化UI
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 新的方式获取 BluetoothAdapter（Android 12+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val bluetoothManager = getSystemService(BluetoothManager::class.java)
            bluetoothAdapter = bluetoothManager?.adapter!!
            if (bluetoothAdapter == null) {
                Toast.makeText(this, "设备不支持蓝牙功能", Toast.LENGTH_LONG).show()
            }
        } else {
            // 旧版本仍然使用弃用方法（但需要添加 @SuppressLint 注解）
            @SuppressLint("MissingPermission", "Deprecated")
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter == null) {
                Toast.makeText(this, "设备不支持蓝牙功能", Toast.LENGTH_LONG).show()
            }
        }

        // 初始化AuthManager
        authManager = AuthManager.getInstance(this)
        authManager.onUserStateChanged = { newState ->
            userState = newState
            // 同步Token到SensorDataManager
            if (newState.isLoggedIn) {
                val token = PreferencesUtils.getUserToken(this)
                if (!token.isNullOrEmpty()) {
                    sensorDataManager.saveToken(token)
                }
            } else {
                sensorDataManager.clearToken()
            }
        }

        // 初始化SensorDataManager
        sensorDataManager = SensorDataManager.getInstance(this)

        // 初始化传感器数据处理器（P2拆分）
        sensorDataProcessor = SensorDataProcessor()
        sensorDataProcessor.onPressureAlert = { message ->
            showPressureAlert(message)
        }

        // 初始化历史记录管理器（P3拆分）
        historyRecordManager = HistoryRecordManager(sensorDataManager)
        historyRecordManager.onError = { message ->
            runOnUiThread {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        }

        // 初始化调试管理器（调试功能拆分）
        debugManager = DebugManager(this, sensorDataProcessor)

        // 初始化内存泄漏检测器
        memoryLeakDetector = MemoryLeakDetector.getInstance()
        memoryLeakDetector.init(application)

        // 初始化蓝牙资源管理器
        bluetoothResourceManager = BluetoothResourceManager.getInstance()

        // 初始化性能监控器
        performanceMonitor = PerformanceMonitor.getInstance()

        // 初始化体重数据
        initUserWeight()

        // 初始化登录状态
        userState = authManager.initUserState()
        // 初始化压力提醒设置
        initPressureAlertsSetting()

        // 检查和请求必要的蓝牙权限
        checkAndRequestPermissions()

        // 设置Compose UI内容
        setContent {
            // 使用remember稳定回调引用，避免每次重组创建新的lambda
            val onScanDevicesCallback = remember { { scanForBluetoothDevices() } }
            val onConnectDeviceCallback = remember<(BluetoothDevice) -> Unit> { { device ->
                mainScope.launch(Dispatchers.IO) {
                    connectToBluetoothDevice(device)
                }
            }}
            val onDisconnectDeviceCallback = remember { { disconnectFromBluetoothDevice() } }
            val onEditWeightCallback = remember<(Float) -> Unit> { { weight ->
                userWeight = weight
                saveUserWeight(weight)
            }}
            val onLoginCallback = remember<(String, String) -> Unit> { { email, password ->
                handleLogin(email, password)
            }}
            val onRegisterCallback = remember<(String, String, String) -> Unit> { { username, email, password ->
                handleRegister(username, email, password)
            }}
            val onLogoutCallback = remember { { handleLogout() } }
            val onEditProfileCallback = remember<(String, String, String, String) -> Unit> { { username, email, password, currentPassword ->
                handleEditProfile(username, email, password, currentPassword)
            }}
            val onPressureAlertsChangeCallback = remember<(Boolean) -> Unit> { { enabled ->
                onPressureAlertsChange(enabled)
            }}

            // 使用remember缓存sensorColors，避免每次数据变化都触发重组
            // 只在列表内容真正变化时才更新
            val rememberedSensorColors by remember(sensorColors) {
                derivedStateOf { sensorColors.toList() }
            }

            // 使用derivedStateOf缓存extraValues的计算结果
            // 只在数值变化时才重新计算派生状态
            val derivedExtraValues by remember(extraValues) {
                derivedStateOf { extraValues.toList() }
            }

            SmartShoeAppTheme {
                MainAppScreen(
                    scannedDevices = scannedDevices,
                    sensorColors = rememberedSensorColors,
                    extraValues = derivedExtraValues,
                    connectedDevice = connectedDevice,
                    userWeight = userWeight,
                    onScanDevices = onScanDevicesCallback,
                    onConnectDevice = onConnectDeviceCallback,
                    onDisconnectDevice = onDisconnectDeviceCallback,
                    onEditWeight = onEditWeightCallback,
                    userState = userState,
                    onLogin = onLoginCallback,
                    onRegister = onRegisterCallback,
                    onLogout = onLogoutCallback,
                    onEditProfile = onEditProfileCallback,
                    pressureAlertsEnabled = pressureAlertsEnabled,
                    onPressureAlertsChange = onPressureAlertsChangeCallback
                )

                // 显示压力异常消息弹窗
                if (showAlertDialog) {
                    AlertDialog(
                        onDismissRequest = { showAlertDialog = false },
                        title = { Text("压力异常警告") },
                        text = { Text(alertMessage) },
                        confirmButton = {
                            TextButton(onClick = { showAlertDialog = false }) {
                                Text("确定")
                            }
                        }
                    )
                }
            }
        }
    }



    /**
     * 应用主题定义
     * 使用MaterialTheme包装内容，确保一致的视觉样式
     */
    @Composable
    private fun SmartShoeAppTheme(content: @Composable () -> Unit) {
        MaterialTheme(
            content = content
        )
    }


    /**
     * 主应用屏幕组件
     * 组合所有UI组件的根布局，使用Scaffold作为基本框架
     * 使用@Stable注解优化参数传递，减少不必要的重组
     */
    @Composable
    private fun MainAppScreen(
        scannedDevices: List<BluetoothDevice>,
        sensorColors: List<Color>,
        extraValues: List<Int>,
        connectedDevice: BluetoothDevice?,
        userWeight: Float, // 新增体重参数
        onScanDevices: () -> Unit,
        onConnectDevice: (BluetoothDevice) -> Unit,
        onDisconnectDevice: () -> Unit, // 添加断开连接的方法
        onEditWeight: (Float) -> Unit, // 新增编辑体重回调
        onEditProfile: (String, String, String,String) -> Unit,
        userState: UserState,// 新增用户相关参数
        onLogin: (String, String) -> Unit,
        onRegister: (String, String, String) -> Unit,
        onLogout: () -> Unit,
        pressureAlertsEnabled: Boolean,// 新增压力提醒参数
        onPressureAlertsChange: (Boolean) -> Unit
    ) {
        // 使用remember稳定列表参数，避免不必要的重组
        val stableScannedDevices by remember(scannedDevices) {
            derivedStateOf { scannedDevices }
        }
        val stableSensorColors by remember(sensorColors) {
            derivedStateOf { sensorColors }
        }
        val stableExtraValues by remember(extraValues) {
            derivedStateOf { extraValues }
        }
        // 添加导航状态管理
        var selectedTab by remember { mutableStateOf(0) }

        Scaffold(
            topBar = {
                AppTopBar()
            },
            bottomBar = {
                // 底部导航栏
                BottomNavigationBar(
                    selectedTab = selectedTab,
                    onTabSelected = { tabIndex -> selectedTab = tabIndex }
                )
            },
            content = { innerPadding ->
                // 根据选中的标签显示不同内容
                when (selectedTab) {
                    0 -> { // 主页面
                        MainContent(
                            modifier = Modifier.padding(innerPadding),
                            scannedDevices = stableScannedDevices,
                            sensorColors = stableSensorColors,
                            extraValues = stableExtraValues,
                            onScanDevices = onScanDevices,
                            onConnectDevice = onConnectDevice,
                            onDisconnectDevice = onDisconnectDevice,
                            connectedDevice = connectedDevice,
                        )
                    }

                    1 -> { // 数据记录页面
                        DataRecordScreen(
                            modifier = Modifier.padding(innerPadding),
                            historicalData = historicalData,
                            connectedDevice = connectedDevice,
                            userWeight = userWeight, // 传递体重
                        )
                    }

                    2 -> { // 历史记录页面
                        // 使用 remember 稳定回调，避免重组时创建新的 lambda
                        val onQueryClickCallback = remember { { historyRecordManager.queryHistoryRecords() } }
                        val onRecordSelectCallback = remember<(SensorDataRecord?) -> Unit> { { record ->
                            historyRecordManager.selectRecord(record)
                        }}

                        HistoryScreenSection.HistoryScreen(
                            modifier = Modifier.padding(innerPadding),
                            records = historyRecordManager.historyRecords,
                            selectedRecord = historyRecordManager.selectedHistoryRecord,
                            recordData = historyRecordManager.selectedRecordData,
                            isLoading = historyRecordManager.isHistoryLoading,
                            isRecordDetailLoading = historyRecordManager.isRecordDetailLoading,
                            startDate = historyRecordManager.historyStartDate,
                            endDate = historyRecordManager.historyEndDate,
                            onStartDateChange = { historyRecordManager.historyStartDate = it },
                            onEndDateChange = { historyRecordManager.historyEndDate = it },
                            onQueryClick = onQueryClickCallback,
                            onRecordSelect = onRecordSelectCallback,
                            queryExecuted = historyRecordManager.queryExecuted
                        )
                    }

                    3 -> { // 设置页面
                        // 使用derivedStateOf优化复杂计算
                        val isLoggedIn by remember(userState) {
                            derivedStateOf { userState.isLoggedIn }
                        }
                        val hasData by remember(sensorDataProcessor.getBackupDataSize()) {
                            derivedStateOf { !sensorDataProcessor.isBackupDataEmpty() }
                        }

                        SettingsScreen(
                            modifier = Modifier.padding(innerPadding),
                            scannedDevices = stableScannedDevices,
                            connectedDevice = connectedDevice,
                            userWeight = userWeight,
                            onConnectDevice = onConnectDevice,
                            onDisconnectDevice = onDisconnectDevice,
                            onClearCache = { clearAppCache() },
                            onEditWeight = { weight ->
                                this@MainActivity.userWeight = weight
                                saveUserWeight(weight)
                            },
                            userState = userState,
                            onLogin = onLogin,
                            onRegister = onRegister,
                            onLogout = onLogout,
                            onEditProfile = onEditProfile,
                            pressureAlertsEnabled = pressureAlertsEnabled,
                            onPressureAlertsChange = onPressureAlertsChange,
                            onBackupData = { _, _, onUploadComplete ->
                                // 使用sensorDataProcessor上传，包含30分钟的数据
                                val dataPoints = sensorDataProcessor.getBackupDataForUpload()
                                val dataCount = dataPoints.size

                                // 检查数据量，如果超过10000点提示用户
                                if (dataCount > 10000) {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "数据量较大($dataCount)，上传可能需要一些时间，请耐心等待",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }

                                uploadSensorDataToServer(
                                    dataPoints = dataPoints,
                                    onResult = { success, message ->
                                        runOnUiThread {
                                            onUploadComplete(success)
                                        }
                                    }
                                )
                            },
                            isLoggedIn = isLoggedIn,
                            hasData = hasData
                        )
                    }
                }
            }
        )
    }

    /**
     * 顶部应用栏组件
     * 显示应用标题和品牌颜色
     */
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun AppTopBar() {
        TopAppBar(
            title = {
                Text(
                    "足底压力可视化",
                    fontSize = UIConstants.TitleTextSize
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = UIConstants.TopAppBarColor,
                titleContentColor = Color.White
            )
        )
    }

    /**
     * 主内容区域组件
     * 垂直布局：设备列表在上方（可悬浮展开），鞋垫可视化在下方
     */
    @Composable
    private fun MainContent(
        modifier: Modifier = Modifier,
        scannedDevices: List<BluetoothDevice>,
        sensorColors: List<Color>,
        extraValues: List<Int>,
        onScanDevices: () -> Unit,
        connectedDevice: BluetoothDevice?,
        onConnectDevice: (BluetoothDevice) -> Unit,
        onDisconnectDevice: () -> Unit
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(UIConstants.BackgroundColor)
                .padding(horizontal = UIConstants.DefaultPadding)
        ) {
            // 主要内容区域（鞋垫可视化 + 传感器数值）
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(UIConstants.DefaultPadding))

                // 设备列表占位区域（固定高度）
                Spacer(modifier = Modifier.height(60.dp))

                Spacer(modifier = Modifier.height(UIConstants.DefaultPadding))

                // 下方：鞋垫可视化（包含传感器数值展示）
                InsoleWithSensors(
                    sensorColors = sensorColors,
                    sensorValues = extraValues,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )

                Spacer(modifier = Modifier.height(UIConstants.DefaultPadding))
            }

            // 悬浮的设备列表（展开时会覆盖下方内容）
            ExpandableDeviceListSection(
                devices = scannedDevices,
                connectedDevice = connectedDevice,
                onConnectDevice = onConnectDevice,
                onDisconnectDevice = onDisconnectDevice,
                onScanDevices = onScanDevices,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(top = UIConstants.DefaultPadding)
            )
        }
    }



    /**
     * 数据记录页面 - 始终显示三个传感器的数据图表
     */
    @Composable
    private fun DataRecordScreen(
        modifier: Modifier = Modifier,
        historicalData: List<SensorDataPoint>,
        userWeight: Float, // 新增体重参数
        connectedDevice: BluetoothDevice?
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(UIConstants.BackgroundColor)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 状态信息栏
            RecordStatusHeader(
                connectedDevice = connectedDevice,
                dataPointCount = historicalData.size,
                userWeight = userWeight, // 传递体重
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            // 三个传感器图表整合在一个卡片中
            DataRecordScreenSection.CombinedChartsCard(
                historicalData = historicalData,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    @Composable
    private fun SettingsScreen(
        modifier: Modifier = Modifier,
        scannedDevices: List<BluetoothDevice>,
        connectedDevice: BluetoothDevice?,
        onConnectDevice: (BluetoothDevice) -> Unit,
        onDisconnectDevice: () -> Unit,
        onClearCache: () -> Unit,
        userWeight: Float, // 新增体重参数
        onEditWeight: (Float) -> Unit, // 新增编辑体重回调
        // 新增参数
        userState: UserState,
        onLogin: (String, String) -> Unit,
        onRegister: (String, String, String) -> Unit,
        onLogout: () -> Unit,
        onEditProfile: (String, String, String,String) -> Unit,
        pressureAlertsEnabled: Boolean,// 新增压力提醒参数
        onPressureAlertsChange: (Boolean) -> Unit,
        onBackupData: (Boolean, String, (Boolean) -> Unit) -> Unit,
        isLoggedIn: Boolean,
        hasData: Boolean
    ) {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(UIConstants.BackgroundColor),
            contentPadding = PaddingValues(vertical = 16.dp, horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { SettingScreenSection.AccountSettingsSection(
                userState = userState,
                onLogin = { email, password -> handleLogin(email, password) },
                onRegister = { username, email, password -> handleRegister(username, email, password) },
                onLogout = { handleLogout() },
                onEditProfile = {username, email, password,currentPassword -> handleEditProfile(username, email, password,currentPassword)}
            ) }
            item {
                // 设备与偏好设置组合卡片
                DeviceAndPreferenceSection(
                    scannedDevices = scannedDevices,
                    connectedDevice = connectedDevice,
                    onConnectDevice = onConnectDevice,
                    onDisconnectDevice = onDisconnectDevice,
                    userWeight = userWeight,
                    onEditWeight = onEditWeight,
                    pressureAlertsEnabled = pressureAlertsEnabled,
                    onPressureAlertsChange = onPressureAlertsChange,
                    onClearCache = onClearCache,
                    onBackupData = onBackupData,
                    isLoggedIn = isLoggedIn,
                    hasData = hasData
                )
            }
            item { AboutAppSection() }
            item {
                DebugSection(
                    onGenerateMockData = {
                        debugManager.generateMockData {
                            Toast.makeText(this@MainActivity, "已生成模拟数据", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onRunPerformanceTest = {
                        // 性能测试逻辑已在DebugSection内部处理
                    },
                    onRunRegressionTest = {
                        // 回归测试逻辑已在DebugSection内部处理
                    }
                )
            }
            item {
                // 性能监控面板（仅在Debug模式下显示）
                if (BuildConfig.DEBUG) {
                    PerformanceMonitorSection.PerformanceMonitorPanel(
                        context = this@MainActivity,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    /**
     * 清除应用缓存
     * 包括：蓝牙设备缓存、传感器数据等
     */
    private fun clearAppCache() {
        try {
            // 清除所有SharedPreferences（使用工具类统一处理）
            PreferencesUtils.clearAllPreferences(this)

            // 重置用户状态
            userState = UserState(isLoggedIn = false)

            // 重置体重
            userWeight = 0f

            // 清空传感器数据（使用P2拆分的处理器）
            sensorDataProcessor.clearSensorData()

            // 清空历史记录数据（使用P3拆分的管理器）
            historyRecordManager.clearHistoryData()

            // 清空蓝牙设备列表
            scannedDevices.clear()

            // 重置连接设备
            connectedDevice = null

            // 重置传感器显示状态
            sensorDataProcessor.resetSensorDisplayState()

            // 关闭蓝牙连接（如果存在）
            bluetoothSocket?.let { socket ->
                try {
                    socket.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                } finally {
                    bluetoothSocket = null
                }
            }

            // 取消数据监听任务
            dataListeningJob?.cancel()
            dataListeningJob = null

            // 清除文件缓存（安全方式）- 使用Dispatchers.IO
            cacheClearJob?.cancel() // 取消之前的清理任务
            cacheClearJob = mainScope.launch(Dispatchers.IO) {
                try {
                    cacheDir.listFiles()?.forEach { file ->
                        if (isActive) { // 检查协程是否被取消
                            if (file.isDirectory) {
                                file.deleteRecursively()
                            } else {
                                file.delete()
                            }
                        }
                    }

                    externalCacheDir?.listFiles()?.forEach { file ->
                        if (isActive) { // 检查协程是否被取消
                            if (file.isDirectory) {
                                file.deleteRecursively()
                            } else {
                                file.delete()
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // 清除AuthManager中的用户数据
            authManager.clearAllUserData()

            // 显示清除成功提示
            Toast.makeText(this, "缓存已清除", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            e.printStackTrace()
            runOnUiThread {
                Toast.makeText(this, "清除缓存时出错", Toast.LENGTH_SHORT).show()
            }
        }
    }






    /**
     * 断开蓝牙设备连接
     */
    private fun disconnectFromBluetoothDevice() {
        try {
            bluetoothSocket?.let { socket ->
                socket.close()
                bluetoothSocket = null
            }
            connectedDevice = null

            // 重置传感器显示状态（使用P2拆分的处理器）
            sensorDataProcessor.resetSensorDisplayState()

            // 可断开连接时清除历史数据
            sensorDataProcessor.clearSensorData()

            runOnUiThread {
                Toast.makeText(this, "已断开连接", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            runOnUiThread {
                Toast.makeText(this, "断开连接失败", Toast.LENGTH_SHORT).show()
            }
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
     * 扫描蓝牙设备
     * 只扫描已配对的设备（简化版本）
     */
    private fun scanForBluetoothDevices() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // 清空设备列表
            scannedDevices.clear()

            // 获取已配对设备
            val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter.bondedDevices
            pairedDevices?.let {
                if (it.isNotEmpty()) {
                    scannedDevices.addAll(it)
                    Toast.makeText(this, "找到 ${it.size} 个已配对设备", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "未找到已配对设备", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "没有蓝牙扫描权限", Toast.LENGTH_SHORT).show()
            checkAndRequestPermissions() // 重新请求权限
        }
    }

    /**
     * 连接到蓝牙设备
     * @param device 要连接的蓝牙设备
     */
    @SuppressLint("MissingPermission")
    private suspend fun connectToBluetoothDevice(device: BluetoothDevice) {
        // 使用Dispatchers.IO进行蓝牙连接操作
        withContext(Dispatchers.IO) {
            try {
                // 先断开现有连接
                disconnectFromBluetoothDevice()

                // 创建新的连接
                val socket = device.createRfcommSocketToServiceRecord(hc06UUID)
                socket.connect()
                bluetoothSocket = socket
                connectedDevice = device

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "连接成功: ${device.name}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                startListeningForData(socket.inputStream)
            } catch (e: IOException) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "连接失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * 开始监听蓝牙数据输入流 - 优化线程管理
     * @param inputStream 蓝牙输入流
     */
    private fun startListeningForData(inputStream: InputStream) {
        // 取消之前的监听任务（如果有）
        dataListeningJob?.cancel()

        // 使用Dispatchers.IO进行蓝牙数据监听
        dataListeningJob = mainScope.launch(Dispatchers.IO) {
            val buffer = ByteArray(1024) // 数据缓冲区
            while (isActive) { // 使用isActive检查协程是否被取消
                try {
                    // 读取数据
                    val bytesRead = inputStream.read(buffer)
                    if (bytesRead > 0) {
                        // 将字节数据转换为字符串
                        val data = String(buffer, 0, bytesRead)
                        // 在主线程处理接收到的数据
                        withContext(Dispatchers.Main) {
                            onDataReceived(data)
                        }
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                    break // 发生异常时退出循环
                }
            }
        }
    }

    /**
     * 处理接收到的蓝牙数据
     * @param data 接收到的字符串数据
     */
    private fun onDataReceived(data: String) {
        // 使用P2拆分的传感器数据处理器处理数据
        sensorDataProcessor.processReceivedData(data, shouldRecord = connectedDevice != null)
    }

    // 登录处理逻辑 - 使用AuthManager进行服务器验证
    /**
     * 处理用户登录
     * @param email 邮箱
     * @param password 密码
     */
    private fun handleLogin(email: String, password: String) {
        authManager.login(email, password) { success, message, userStateResult ->
            if (success) {
                userStateResult?.let {
                    userState = it
                }
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 处理用户注册
     * @param username 用户名
     * @param email 邮箱
     * @param password 密码
     */
    private fun handleRegister(username: String, email: String, password: String) {
        authManager.register(username, email, password) { success, message, userStateResult ->
            if (success) {
                userStateResult?.let {
                    userState = it
                }
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 处理退出登录
     */
    private fun handleLogout() {
        authManager.logout()
        userState = UserState(isLoggedIn = false)
    }

    /**
     * 处理修改用户资料
     * @param newUsername 新用户名
     * @param newEmail 新邮箱
     * @param newPassword 新密码（可选）
     * @param currentPassword 当前密码（用于验证）
     */
    private fun handleEditProfile(
        newUsername: String,
        newEmail: String,
        newPassword: String,
        currentPassword: String
    ) {
        val currentUserId = userState.userId
        if (currentUserId.isEmpty()) {
            return
        }

        authManager.updateProfile(
            userId = currentUserId,
            currentPassword = currentPassword,
            newUsername = newUsername,
            newEmail = newEmail,
            newPassword = newPassword
        ) { success, message, userStateResult ->
            if (success) {
                userStateResult?.let {
                    userState = it
                }
            }
        }
    }

    /**
     * 显示压力异常消息弹窗
     */
    private fun showPressureAlert(message: String) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastAlertTime < alertCooldown) return

        lastAlertTime = currentTime
        alertMessage = message
        showAlertDialog = true
    }

    private fun onPressureAlertsChange(enabled: Boolean) {
        pressureAlertsEnabled = enabled
        // 保存设置到SharedPreferences
        PreferencesUtils.savePressureAlertsEnabled(this, enabled)
    }

    /**
     * 初始化压力提醒设置
     */
    private fun initPressureAlertsSetting() {
        pressureAlertsEnabled = PreferencesUtils.getPressureAlertsEnabled(this, true) // 默认开启
    }

    /**
     * 初始化用户体重数据
     */
    private fun initUserWeight() {
        // 检查是否是第一次启动
        val isFirstLaunch = PreferencesUtils.isFirstLaunch(this)

        // 读取保存的体重
        userWeight = PreferencesUtils.getUserWeight(this, 0f)

        // 标记已经不是第一次启动
        if (isFirstLaunch) {
            PreferencesUtils.markNotFirstLaunch(this)
        }
    }

    /**
     * 保存用户体重
     */
    private fun saveUserWeight(weight: Float) {
        PreferencesUtils.saveUserWeight(this, weight)
    }

    /**
     * 上传传感器数据到服务器
     * 需要用户登录后才能上传
     */
    private fun uploadSensorDataToServer(
        dataPoints: List<SensorDataPoint>,
        onResult: (Boolean, String) -> Unit
    ) {
        if (!userState.isLoggedIn) {
            onResult(false, "请先登录后再上传数据")
            return
        }

        if (dataPoints.isEmpty()) {
            onResult(false, "没有数据可上传")
            return
        }

        // 调用SensorDataManager上传
        sensorDataManager.uploadSensorData(dataPoints) { success, message, info ->
            if (success && info != null) {
                val compressionInfo = String.format(
                    "压缩率: %.1f%% (${info.originalSize}B → ${info.compressedSize}B)",
                    (1 - info.compressionRatio) * 100
                )
                // 上传成功后清除历史记录缓存（使用P3拆分的管理器），确保下次查询能获取最新数据
                historyRecordManager.clearCache()
                onResult(true, "上传成功! $compressionInfo")
            } else {
                onResult(false, message)
            }
        }
    }

    /**
     * 获取用户的传感器数据记录列表
     */
    private fun getUserSensorRecords(
        page: Int = 0,
        onResult: (Boolean, String, List<com.example.smartshoe.data.remote.SensorDataRecord>?) -> Unit
    ) {
        if (!userState.isLoggedIn) {
            onResult(false, "请先登录", null)
            return
        }

        sensorDataManager.getUserRecords(page = page) { success, message, records, total ->
            onResult(success, "$message (共${total}条)", records)
        }
    }

    /**
     * Activity销毁时清理资源 - 优化协程生命周期管理
     * 确保所有资源被正确释放，防止内存泄漏
     */
    override fun onDestroy() {
        super.onDestroy()

        // 1. 取消数据监听协程
        dataListeningJob?.cancel()
        dataListeningJob = null

        // 2. 取消缓存清理协程
        cacheClearJob?.cancel()
        cacheClearJob = null

        // 3. 释放蓝牙资源（使用蓝牙资源管理器）
        connectedDevice?.address?.let { deviceAddress ->
            bluetoothResourceManager.releaseDeviceResources(deviceAddress)
        }

        // 4. 关闭蓝牙Socket
        bluetoothSocket?.let { socket ->
            try {
                if (socket.isConnected) {
                    socket.close()
                }
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                bluetoothSocket = null
            }
        }

        // 5. 清空数据缓冲区，释放内存
        if (::sensorDataProcessor.isInitialized) {
            sensorDataProcessor.clearSensorData()
        }
        if (::historyRecordManager.isInitialized) {
            historyRecordManager.clearHistoryData()
        }
        scannedDevices.clear()

        // 6. 取消统一的协程作用域
        mainScope.cancel()

        // 7. 停止内存泄漏检测
        memoryLeakDetector.unwatch(this, "Activity")

        // 8. 停止性能监控
        if (::performanceMonitor.isInitialized) {
            performanceMonitor.stopMonitoring()
        }

        // 9. 建议系统进行垃圾回收
        System.gc()
    }
}
