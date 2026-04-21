package com.example.smartshoe.ui.screen

import android.bluetooth.BluetoothDevice
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartshoe.R
import com.example.smartshoe.domain.model.SensorDataPoint
import com.example.smartshoe.domain.model.SensorDataRecord
import com.example.smartshoe.domain.model.UserState
import com.example.smartshoe.ui.component.BottomNavigation.BottomNavigationBar
import com.example.smartshoe.ui.component.CompactDeviceListItem
import com.example.smartshoe.ui.component.ExpandableChevron
import com.example.smartshoe.ui.component.RefreshButton
import com.example.smartshoe.ui.component.getDeviceDisplayName
import com.example.smartshoe.ui.component.sensor.SensorCanvas.InsoleWithSensors
import com.example.smartshoe.ui.screen.DataRecordScreen.DataRecordScreen
import com.example.smartshoe.ui.screen.SettingScreen.SettingsScreen
import com.example.smartshoe.ui.viewmodel.AiAssistantViewModel
import com.example.smartshoe.ui.theme.AppColors
import com.example.smartshoe.ui.theme.AppDimensions
import com.example.smartshoe.ui.theme.AppTypography
import com.example.smartshoe.ui.viewmodel.AuthUiState
import com.example.smartshoe.ui.viewmodel.SettingViewModel
import com.example.smartshoe.util.AnimationDefaults
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

/**
 * 主屏幕组件（简化版）
 * 使用数据类封装状态和回调，符合Clean Architecture
 *
 * @param state UI状态
 * @param callbacks 回调函数
 */
@Composable
fun MainScreen(
    state: MainScreenState,
    callbacks: MainScreenCallbacks
) {
    val snackbarHostState = remember { SnackbarHostState() }
    
    // 显示 Snackbar
    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            callbacks.onSnackbarDismiss()
        }
    }
    
    SmartShoeAppTheme {
        Scaffold(
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.padding(bottom = 80.dp)
                ) { data ->
                    CustomSnackbar(
                        message = data.visuals.message,
                        type = state.snackbarType
                    )
                }
            }
        ) { innerPadding ->
            MainAppScreen(
                state = state,
                callbacks = callbacks,
                modifier = Modifier.padding(innerPadding)
            )
        }

        // 显示压力异常消息弹窗
        if (state.showAlertDialog) {
            AlertDialog(
                onDismissRequest = callbacks.onDismissAlert,
                title = { Text("压力异常警告") },
                text = { Text(state.alertMessage) },
                confirmButton = {
                    TextButton(
                        onClick = callbacks.onDismissAlert,
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
                    ) {
                        Text("确定")
                    }
                }
            )
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
 */
@Composable
private fun MainAppScreen(
    state: MainScreenState,
    callbacks: MainScreenCallbacks,
    modifier: Modifier = Modifier
) {
    // 使用remember稳定列表参数，避免不必要的重组
    val stableScannedDevices by remember(state.scannedDevices) {
        derivedStateOf { state.scannedDevices }
    }
    val stableSensorColors by remember(state.sensorColors) {
        derivedStateOf { state.sensorColors }
    }
    val stableExtraValues by remember(state.extraValues) {
        derivedStateOf { state.extraValues }
    }
    val stablePressureStatuses by remember(state.pressureStatuses) {
        derivedStateOf { state.pressureStatuses }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            AppTopBar()
        },
        bottomBar = {
            // 底部导航栏
            BottomNavigationBar(
                selectedTab = state.selectedTab,
                onTabSelected = callbacks.onTabSelected
            )
        },
        content = { innerPadding ->
            // 根据选中的标签显示不同内容
            when (state.selectedTab) {
                0 -> { // 主页面
                    MainContent(
                        modifier = Modifier.padding(innerPadding),
                        scannedDevices = stableScannedDevices,
                        sensorColors = stableSensorColors,
                        extraValues = stableExtraValues,
                        pressureStatuses = stablePressureStatuses,
                        onScanDevices = callbacks.onScanDevices,
                        onConnectDevice = callbacks.onConnectDevice,
                        onDisconnectDevice = callbacks.onDisconnectDevice,
                        connectedDevice = state.connectedDevice,
                        isScanning = state.isScanning,
                        isConnecting = state.isConnecting,
                        connectingDeviceAddress = state.connectingDeviceAddress
                    )
                }

                1 -> { // 数据记录页面
                    DataRecordScreen(
                        modifier = Modifier.padding(innerPadding),
                        historicalData = state.historicalData,
                        connectedDevice = state.connectedDevice,
                        userWeight = state.userWeight,
                    )
                }

                2 -> { // 历史记录页面
                    HistoryScreen.HistoryScreen(
                        modifier = Modifier.padding(innerPadding),
                        records = state.historyRecords,
                        selectedRecord = state.selectedHistoryRecord,
                        recordData = state.recordData,
                        isLoading = state.isHistoryLoading,
                        isRecordDetailLoading = state.isRecordDetailLoading,
                        startDate = state.historyStartDate,
                        endDate = state.historyEndDate,
                        onStartDateChange = callbacks.onStartDateChange,
                        onEndDateChange = callbacks.onEndDateChange,
                        onQueryClick = callbacks.onQueryHistory,
                        onRecordSelect = callbacks.onRecordSelect,
                        onAiAnalysisClick = callbacks.onAiAnalysisClick,
                        queryExecuted = state.queryExecuted,
                        onShowDatePicker = callbacks.onShowDatePicker
                    )
                }

                3 -> { // AI助手页面
                    callbacks.aiAssistantViewModel?.let { viewModel ->
                        AiAssistantScreen(
                            modifier = Modifier.padding(innerPadding),
                            viewModel = viewModel,
                            token = callbacks.userToken,
                            onShowError = callbacks.onShowError ?: {}
                        )
                    }
                }

                4 -> { // 设置页面
                    Log.d("BackupDebug", "MainScreen: 传递 onBackupData=${callbacks.onBackupData}")
                    SettingsScreen(
                        modifier = Modifier.padding(innerPadding),
                        scannedDevices = stableScannedDevices,
                        connectedDevice = state.connectedDevice,
                        userWeight = state.userWeight,
                        onConnectDevice = callbacks.onConnectDevice,
                        onDisconnectDevice = callbacks.onDisconnectDevice,
                        onClearCache = callbacks.onClearCache,
                        onEditWeight = callbacks.onEditWeight,
                        userState = state.userState,
                        onLogin = callbacks.onLogin,
                        onRegister = callbacks.onRegister,
                        onLogout = callbacks.onLogout,
                        onEditProfile = callbacks.onEditProfile,
                        pressureAlertsEnabled = state.pressureAlertsEnabled,
                        onPressureAlertsChange = callbacks.onPressureAlertsChange,
                        onBackupData = callbacks.onBackupData,
                        isLoggedIn = state.isLoggedIn,
                        hasData = state.hasData,
                        onGenerateMockData = callbacks.onGenerateMockData,
                        settingViewModel = callbacks.settingViewModel,
                        onShowError = callbacks.onShowError,
                        authUiState = callbacks.authUiState
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
                fontSize = AppTypography.TitleTextSize
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = AppColors.TopAppBar,
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
    pressureStatuses: List<com.example.smartshoe.domain.model.PressureStatus> = emptyList(),
    onScanDevices: () -> Unit,
    connectedDevice: BluetoothDevice?,
    onConnectDevice: (BluetoothDevice) -> Unit,
    onDisconnectDevice: () -> Unit,
    isScanning: Boolean,
    isConnecting: Boolean = false,
    connectingDeviceAddress: String? = null
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .padding(horizontal = AppDimensions.DefaultPadding)
    ) {
        // 主要内容区域（鞋垫可视化 + 传感器数值）
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(AppDimensions.DefaultPadding))

            // 设备列表占位区域（固定高度）
            Spacer(modifier = Modifier.height(60.dp))

            Spacer(modifier = Modifier.height(AppDimensions.DefaultPadding))

            // 下方：鞋垫可视化（包含传感器数值展示）
            InsoleWithSensors(
                sensorColors = sensorColors,
                sensorValues = extraValues,
                pressureStatuses = pressureStatuses,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            Spacer(modifier = Modifier.height(AppDimensions.DefaultPadding))
        }

        // 悬浮的设备列表（展开时会覆盖下方内容）
        ExpandableDeviceListSection(
            devices = scannedDevices,
            connectedDevice = connectedDevice,
            onConnectDevice = onConnectDevice,
            onDisconnectDevice = onDisconnectDevice,
            onScanDevices = onScanDevices,
            isScanning = isScanning,
            isConnecting = isConnecting,
            connectingDeviceAddress = connectingDeviceAddress,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = AppDimensions.DefaultPadding)
        )
    }
}

/**
 * 设备列表区域组件
 * 显示扫描到的蓝牙设备列表，支持滚动查看，包含刷新按钮
 * 展开时悬浮在其他组件上方
 * 样式与 AI模式选择器 保持一致（方案A：现代简洁风）
 */
@Composable
fun ExpandableDeviceListSection(
    devices: List<BluetoothDevice>,
    connectedDevice: BluetoothDevice?,
    onConnectDevice: (BluetoothDevice) -> Unit,
    onDisconnectDevice: () -> Unit,
    onScanDevices: () -> Unit,
    isScanning: Boolean,
    modifier: Modifier = Modifier,
    isConnecting: Boolean = false,
    connectingDeviceAddress: String? = null
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(
                    min = 60.dp,
                    max = if (isExpanded) 350.dp else 60.dp
                )
        ) {
            // 标题栏 - 统一60.dp高度，解决双行信息挤压问题
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { isExpanded = !isExpanded },
                color = AppColors.Surface,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 蓝牙图标
                    Icon(
                        painter = painterResource(R.drawable.bluetooth),
                        contentDescription = "蓝牙设备",
                        tint = AppColors.Primary,
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    // 主标题 - 统一16.sp SemiBold Primary色
                    Text(
                        text = "蓝牙设备",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.Primary,
                        modifier = Modifier.weight(1f)
                    )

                    // 右侧区域：连接状态 + 刷新按钮 + 展开图标
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 连接状态标签（单行显示，避免挤压主标题）
                        if (connectedDevice != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                // 状态指示点
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(AppColors.Success)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "已连接",
                                    fontSize = 12.sp,
                                    color = AppColors.Success,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } else {
                            Text(
                                text = "未连接",
                                fontSize = 12.sp,
                                color = AppColors.OnSurface.copy(alpha = 0.5f)
                            )
                        }

                        // 刷新按钮 - 使用新的RefreshButton组件
                        RefreshButton(
                            isScanning = isScanning,
                            onRefresh = onScanDevices,
                            size = 32.dp,
                            iconSize = 18.dp
                        )

                        // 统一展开图标（使用ExpandableChevron）
                        ExpandableChevron(
                            isExpanded = isExpanded,
                            size = 24.dp,
                            tint = AppColors.OnSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(
                    animationSpec = AnimationDefaults.expandTween
                ),
                exit = shrinkVertically(
                    animationSpec = AnimationDefaults.shrinkTween
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (devices.isEmpty()) {
                        // 空状态优化
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "未找到设备",
                                    fontSize = 14.sp,
                                    color = AppColors.OnSurface.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = "点击刷新按钮开始扫描",
                                    fontSize = 12.sp,
                                    color = AppColors.OnSurface.copy(alpha = 0.4f)
                                )
                            }
                        }
                    } else {
                        // 已连接设备详情头部（展开后显示）
                        if (connectedDevice != null) {
                            ConnectedDeviceHeader(
                                device = connectedDevice,
                                onDisconnect = onDisconnectDevice
                            )
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            // 过滤掉已连接的设备，避免重复显示
                            val displayDevices = devices.filter { it.address != connectedDevice?.address }
                            
                            items(displayDevices) { device ->
                                val deviceAddress = device.address
                                val isThisDeviceConnecting = isConnecting && connectingDeviceAddress == deviceAddress
                                
                                CompactDeviceListItem(
                                    device = device,
                                    isConnected = false, // 这里都是未连接的设备
                                    isConnecting = isThisDeviceConnecting,
                                    isAnyDeviceConnecting = isConnecting,
                                    onConnect = { onConnectDevice(device) },
                                    onDisconnect = onDisconnectDevice
                                )
                                if (device != displayDevices.last()) {
                                    HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 已连接设备详情头部
 * 展开蓝牙设备列表后显示，展示详细的连接设备信息
 * 左侧显示connect_device图标，右侧显示disconnect_device图标用于断开连接
 */
@Composable
private fun ConnectedDeviceHeader(
    device: BluetoothDevice,
    onDisconnect: () -> Unit
) {
    val displayName = getDeviceDisplayName(device)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        color = AppColors.Primary.copy(alpha = 0.08f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧：已连接设备图标
            Icon(
                painter = painterResource(R.drawable.connect_device),
                contentDescription = "已连接设备",
                tint = AppColors.Primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.OnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = device.address ?: "未知地址",
                    fontSize = 11.sp,
                    color = AppColors.OnSurface.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            // 右侧：断开连接图标按钮
            Icon(
                painter = painterResource(R.drawable.disconnect_device),
                contentDescription = "断开连接",
                tint = AppColors.Error,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onDisconnect() }
            )
        }
    }
}

/**
 * 自定义 Snackbar 组件
 * 符合应用设计规范，使用主题色和圆角设计
 */
@Composable
fun CustomSnackbar(
    message: String,
    modifier: Modifier = Modifier,
    type: SnackbarType = SnackbarType.Info
) {
    val (backgroundColor, iconColor, icon) = when (type) {
        SnackbarType.Success -> Triple(
            AppColors.Success.copy(alpha = 0.9f),
            Color.White,
            Icons.Default.CheckCircle
        )
        SnackbarType.Error -> Triple(
            AppColors.Error.copy(alpha = 0.9f),
            Color.White,
            Icons.Default.Close
        )
        SnackbarType.Warning -> Triple(
            AppColors.Warning.copy(alpha = 0.9f),
            AppColors.TextDark,
            Icons.Default.Warning
        )
        SnackbarType.Info -> Triple(
            AppColors.Primary.copy(alpha = 0.9f),
            Color.White,
            Icons.Default.Info
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                color = iconColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
