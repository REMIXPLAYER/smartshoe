package com.example.smartshoe.ui.screen

import android.bluetooth.BluetoothDevice
import android.util.Log
import androidx.compose.animation.AnimatedVisibility

import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartshoe.R
import com.example.smartshoe.data.model.SensorDataPoint
import com.example.smartshoe.data.model.UserState
import com.example.smartshoe.ui.component.BottomNavigation.BottomNavigationBar
import com.example.smartshoe.ui.component.ExpandableArrowIcon
import com.example.smartshoe.ui.component.getDeviceDisplayName
import com.example.smartshoe.ui.component.sensor.SensorCanvas.InsoleWithSensors
import com.example.smartshoe.ui.screen.AIAssistantScreen
import com.example.smartshoe.ui.screen.DataRecordScreen.DataRecordScreen
import com.example.smartshoe.ui.screen.SettingScreen.SettingsScreen
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
    val pressureStatuses: List<com.example.smartshoe.util.PressureStatus> = emptyList(),
    val historicalData: List<SensorDataPoint> = emptyList(),
    val connectedDevice: BluetoothDevice? = null,
    val userWeight: Float = 0f,
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
    val historyRecords: List<com.example.smartshoe.data.model.SensorDataRecord> = emptyList(),
    val selectedHistoryRecord: com.example.smartshoe.data.model.SensorDataRecord? = null,
    val recordData: List<SensorDataPoint> = emptyList(),
    val isHistoryLoading: Boolean = false,
    val isRecordDetailLoading: Boolean = false,
    val historyStartDate: Date? = null,
    val historyEndDate: Date? = null,
    val queryExecuted: Boolean = false,
    // 导航
    val selectedTab: Int = 0
)

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
    val onRecordSelect: (com.example.smartshoe.data.model.SensorDataRecord?) -> Unit = {},
    val onStartDateChange: (Date?) -> Unit = {},
    val onEndDateChange: (Date?) -> Unit = {},
    val onShowDatePicker: ((Date, (Date) -> Unit) -> Unit)? = null,
    // 导航
    val onTabSelected: (Int) -> Unit = {},
    // Debug
    val onGenerateMockData: () -> Unit = {},
    // SettingViewModel
    val settingViewModel: SettingViewModel? = null,
    // 错误提示
    val onShowError: ((String) -> Unit)? = null
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
    SmartShoeAppTheme {
        MainAppScreen(
            state = state,
            callbacks = callbacks
        )

        // 显示压力异常消息弹窗
        if (state.showAlertDialog) {
            AlertDialog(
                onDismissRequest = callbacks.onDismissAlert,
                title = { Text("压力异常警告") },
                text = { Text(state.alertMessage) },
                confirmButton = {
                    TextButton(onClick = callbacks.onDismissAlert) {
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
    callbacks: MainScreenCallbacks
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
                        queryExecuted = state.queryExecuted,
                        onShowDatePicker = callbacks.onShowDatePicker
                    )
                }

                3 -> { // AI助手页面
                    AIAssistantScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
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
    pressureStatuses: List<com.example.smartshoe.util.PressureStatus> = emptyList(),
    onScanDevices: () -> Unit,
    connectedDevice: BluetoothDevice?,
    onConnectDevice: (BluetoothDevice) -> Unit,
    onDisconnectDevice: () -> Unit
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
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = AppDimensions.DefaultPadding)
        )
    }
}

/**
 * 设备列表区域组件
 * 显示扫描到的蓝牙设备列表，支持滚动查看，包含扫描按钮
 * 展开时悬浮在其他组件上方
 */
@Composable
fun ExpandableDeviceListSection(
    devices: List<BluetoothDevice>,
    connectedDevice: BluetoothDevice?,
    onConnectDevice: (BluetoothDevice) -> Unit,
    onDisconnectDevice: () -> Unit,
    onScanDevices: () -> Unit,
    modifier: Modifier = Modifier
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clickable { isExpanded = !isExpanded }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.bluetooth),
                        contentDescription = "蓝牙设备",
                        tint = AppColors.Primary,
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "蓝牙设备 (${devices.size})",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = AppColors.OnSurface
                        )

                        if (connectedDevice != null) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "已连接：${getDeviceDisplayName(connectedDevice)}",
                                fontSize = 12.sp,
                                color = Color(0xFF4CAF50)
                            )
                        }
                    }

                    Button(
                        onClick = onScanDevices,
                        modifier = Modifier
                            .height(36.dp)
                            .padding(end = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.Primary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "扫描",
                            fontSize = 13.sp,
                            color = Color.White
                        )
                    }

                    ExpandableArrowIcon(
                        isExpanded = isExpanded,
                        size = 24.dp,
                        useGraphicsLayer = false
                    )
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
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "未找到设备，请点击扫描按钮",
                                fontSize = 14.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            items(devices) { device ->
                                CompactDeviceListItem(
                                    device = device,
                                    isConnected = connectedDevice?.address == device.address,
                                    onConnect = { onConnectDevice(device) },
                                    onDisconnect = onDisconnectDevice
                                )
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
                            }
                        }
                    }

                }
            }
        }
    }
}

/**
 * 紧凑设备列表项
 */
@Composable
fun CompactDeviceListItem(
    device: BluetoothDevice,
    isConnected: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = getDeviceDisplayName(device),
                    color = if (isConnected) Color(0xFF2E7D32) else AppColors.OnSurface,
                    fontSize = 16.sp,
                    fontWeight = if (isConnected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (isConnected) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = device.address ?: "未知地址",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Button(
                onClick = {
                    if (isConnected) {
                        onDisconnect()
                    } else {
                        onConnect()
                    }
                },
                modifier = Modifier
                    .height(36.dp)
                    .width(80.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isConnected) Color(0xFFF44336) else AppColors.Primary
                )
            ) {
                Text(
                    text = if (isConnected) "断开" else "连接",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
