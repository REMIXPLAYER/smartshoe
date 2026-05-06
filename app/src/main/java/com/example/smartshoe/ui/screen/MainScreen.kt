package com.example.smartshoe.ui.screen

import android.bluetooth.BluetoothDevice
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*

import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.smartshoe.domain.model.SensorDataPoint
import com.example.smartshoe.domain.model.SensorDataRecord
import com.example.smartshoe.ui.component.BottomNavigation.BottomNavigationBar
import com.example.smartshoe.ui.component.ConversationDrawer
import com.example.smartshoe.ui.component.sensor.SensorCanvas.InsoleWithSensors
import com.example.smartshoe.ui.screen.DataRecordScreen
import com.example.smartshoe.ui.screen.SettingScreen.SettingsScreen
import com.example.smartshoe.ui.screen.main.MainScreenCallbacks
import com.example.smartshoe.ui.screen.main.MainScreenState
import com.example.smartshoe.ui.screen.main.SnackbarType
import com.example.smartshoe.ui.screen.main.components.AppTopBar
import com.example.smartshoe.ui.screen.main.components.CustomSnackbar
import com.example.smartshoe.ui.screen.main.components.ExpandableDeviceListSection
import com.example.smartshoe.ui.theme.AppColors
import com.example.smartshoe.ui.theme.AppDimensions
import com.example.smartshoe.ui.theme.AppTypography
import java.util.Date

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
    // 列表类型参数已经是稳定的（data class + immutable list），直接使用
    // 当 state 对象变化但列表内容不变时，Compose 会自动跳过重组
    val stableScannedDevices = state.scannedDevices
    // 优化：使用合并的 sensorUiState 获取传感器数据，减少重组
    val stableSensorColors = state.sensorUiState.colors
    val stableExtraValues = state.sensorUiState.values
    val stablePressureStatuses = state.sensorUiState.statuses

    // AI助手ViewModel和状态
    val aiViewModel = callbacks.aiAssistantViewModel
    val conversations = aiViewModel?.conversations?.collectAsStateWithLifecycle()?.value ?: emptyList()
    val groupedConversations = aiViewModel?.groupedConversations?.collectAsStateWithLifecycle()?.value ?: emptyMap()
    val currentConversationId = aiViewModel?.currentConversationId?.collectAsStateWithLifecycle()?.value
    val searchKeyword = aiViewModel?.searchKeyword?.collectAsStateWithLifecycle()?.value ?: ""
    val showConversationDrawer = aiViewModel?.showConversationDrawer?.collectAsStateWithLifecycle()?.value ?: false

    // TopBar按钮点击处理
    val onMenuClick: () -> Unit = {
        aiViewModel?.showConversationDrawer()
    }
    val onNewConversation: () -> Unit = {
        if (conversations.isNotEmpty()) {
            // 有对话记录时：跳转到AI助手页面并新增对话
            callbacks.onTabSelected(3)
            aiViewModel?.createNewConversation()
        } else {
            // 没有对话记录时：只跳转到AI助手页面，不新增对话
            callbacks.onTabSelected(3)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            AppTopBar(
                onMenuClick = onMenuClick,
                onNewConversation = onNewConversation
            )
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
                        historicalData = state.sensorUiState.historicalData,
                        connectedDevice = state.connectedDevice,
                        userWeight = state.userWeight,
                    )
                }

                2 -> { // 历史记录页面
                    HistoryScreen(
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
                    aiViewModel?.let { viewModel ->
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

    // 对话列表抽屉 - 全局显示，所有页面都能滑出
    aiViewModel?.let { viewModel ->
        ConversationDrawer(
            isOpen = showConversationDrawer,
            groupedConversations = groupedConversations,
            currentConversationId = currentConversationId,
            searchKeyword = searchKeyword,
            onSearchChange = { viewModel.searchConversations(it) },
            onConversationClick = { conversationId ->
                // 切换到AI助手页面并加载对话
                callbacks.onTabSelected(3)
                viewModel.switchToConversation(conversationId)
            },
            onNewConversation = {
                // 跳转到AI助手页面并新增对话
                callbacks.onTabSelected(3)
                viewModel.createNewConversation()
            },
            onDeleteConversation = { conversationId ->
                viewModel.deleteConversation(conversationId)
            },
            onClose = { viewModel.hideConversationDrawer() }
        )
    }
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


