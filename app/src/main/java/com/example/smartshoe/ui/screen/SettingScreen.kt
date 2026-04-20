package com.example.smartshoe.ui.screen

import android.bluetooth.BluetoothDevice
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smartshoe.R
import com.example.smartshoe.domain.model.UserState
import com.example.smartshoe.ui.component.CompactDeviceListItem
import com.example.smartshoe.ui.component.ExpandableArrowIcon
import com.example.smartshoe.ui.component.SmartShoeTextField
import com.example.smartshoe.ui.theme.AppColors
import com.example.smartshoe.ui.theme.AppIcon
import com.example.smartshoe.ui.theme.AppIcons
import com.example.smartshoe.ui.viewmodel.AuthUiState
import com.example.smartshoe.ui.viewmodel.SettingViewModel
import com.example.smartshoe.ui.viewmodel.UploadStatus

object SettingScreen {

    // ==================== 头部蓝色区域 ====================
    @Composable
    fun ProfileHeader(
        userState: UserState,
        connectedDevice: BluetoothDevice?,
        userWeight: Float,
        uploadStatus: UploadStatus,
        onEditProfileClick: () -> Unit
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .padding(horizontal = 16.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(AppColors.Primary, AppColors.PrimaryDark)
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            // 用户信息（可点击）
            Row(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 24.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onEditProfileClick
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 头像
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(AppColors.Background, CircleShape)
                        .border(3.dp, AppColors.Background.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (userState.username.isNotEmpty()) 
                            userState.username.take(1).uppercase() 
                        else "?",
                        color = AppColors.Primary,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (userState.isLoggedIn) userState.username else "未登录",
                        color = AppColors.Background,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (userState.isLoggedIn) userState.email else "点击登录账户",
                        color = AppColors.Background.copy(alpha = 0.85f),
                        fontSize = 14.sp
                    )
                }

                // 编辑资料按钮（仅登录后显示）
                if (userState.isLoggedIn) {
                    IconButton(
                        onClick = onEditProfileClick,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "编辑",
                            tint = AppColors.Background,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else {
                    // 未登录时显示箭头提示
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "登录",
                        tint = AppColors.Background.copy(alpha = 0.7f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // 悬浮统计卡片
            StatsCard(
                isConnected = connectedDevice != null,
                weight = userWeight,
                uploadStatus = uploadStatus,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp)
                    .offset(y = 40.dp)
            )
        }
    }

    // ==================== 统计卡片 ====================
    @Composable
    fun StatsCard(
        isConnected: Boolean,
        weight: Float,
        uploadStatus: UploadStatus,
        modifier: Modifier = Modifier
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .height(90.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = AppColors.Background),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 设备状态
                StatItem(
                    icon = R.drawable.bluetooth,
                    label = "设备状态",
                    value = if (isConnected) "已连接" else "未连接",
                    indicatorColor = if (isConnected) AppColors.Primary else AppColors.DarkGray,
                    isActive = isConnected
                )
                
                // 分隔线
                StatsDivider()

                // 体重数据
                StatItem(
                    icon = R.drawable.man,
                    label = "体重数据",
                    value = if (weight > 0) "${weight}kg" else "未设置",
                    indicatorColor = if (weight > 0) AppColors.Primary else AppColors.DarkGray,
                    isActive = weight > 0
                )
                
                // 分隔线
                StatsDivider()

                // 备份状态
                val (backupText, backupColor) = when (uploadStatus) {
                    UploadStatus.UPLOADING -> "上传中" to AppColors.Primary
                    UploadStatus.SUCCESS -> "已备份" to AppColors.Primary
                    UploadStatus.FAILED -> "失败" to AppColors.Error
                    else -> "未备份" to AppColors.DarkGray
                }
                StatItem(
                    icon = R.drawable.cloud,
                    label = "云端备份",
                    value = backupText,
                    indicatorColor = backupColor,
                    isActive = uploadStatus == UploadStatus.SUCCESS
                )
            }
        }
    }

    @Composable
    fun StatItem(
        icon: Int,
        label: String,
        value: String,
        indicatorColor: Color,
        isActive: Boolean
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            // 图标带背景圆圈
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (isActive) indicatorColor.copy(alpha = 0.1f) else AppColors.Background,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = label,
                    tint = if (isActive) indicatorColor else AppColors.DarkGray,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isActive) indicatorColor else AppColors.DarkGray
            )
        }
    }

    @Composable
    fun StatsDivider() {
        HorizontalDivider(
            modifier = Modifier
                .height(50.dp)
                .width(1.dp),
            color = AppColors.LightGray
        )
    }

    // ==================== 可展开快捷功能区 ====================
    @Composable
    fun ExpandableQuickActions(
        scannedDevices: List<BluetoothDevice>,
        connectedDevice: BluetoothDevice?,
        onConnectDevice: (BluetoothDevice) -> Unit,
        onDisconnectDevice: () -> Unit,
        userWeight: Float,
        onEditWeight: (Float) -> Unit,
        pressureAlertsEnabled: Boolean,
        onPressureAlertsChange: (Boolean) -> Unit,
        onBackupClick: () -> Unit,
        uploadStatus: UploadStatus,
        isLoggedIn: Boolean,
        hasData: Boolean,
        settingViewModel: SettingViewModel? = null
    ) {
        // 展开状态
        var expandedItem by rememberSaveable { mutableStateOf<String?>(null) }

        // 从ViewModel获取体重编辑状态
        val isEditingWeight = settingViewModel?.isEditingWeight?.collectAsStateWithLifecycle()?.value ?: false
        val weightInput = settingViewModel?.weightInput?.collectAsStateWithLifecycle()?.value ?: ""

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = AppColors.Background),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // 快捷功能入口行（始终显示）
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    QuickActionButton(
                        icon = R.drawable.bluetooth,
                        label = "设备",
                        isActive = expandedItem == "device",
                        onClick = {
                            expandedItem = if (expandedItem == "device") null else "device"
                        }
                    )
                    QuickActionButton(
                        icon = R.drawable.man,
                        label = "体重",
                        isActive = expandedItem == "weight",
                        onClick = {
                            expandedItem = if (expandedItem == "weight") null else "weight"
                            if (expandedItem == "weight") {
                                settingViewModel?.startEditingWeight(userWeight)
                            }
                        }
                    )
                    QuickActionButton(
                        icon = R.drawable.warning,
                        label = "提醒",
                        isActive = expandedItem == "alert",
                        onClick = {
                            expandedItem = if (expandedItem == "alert") null else "alert"
                        }
                    )
                    QuickActionButton(
                        icon = R.drawable.cloud,
                        label = "备份",
                        isActive = expandedItem == "backup",
                        onClick = {
                            expandedItem = if (expandedItem == "backup") null else "backup"
                        }
                    )
                }

                // 设备展开内容
                AnimatedVisibility(
                    visible = expandedItem == "device",
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = AppColors.LightGray.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))

                        // 设备列表
                        if (scannedDevices.isEmpty() && connectedDevice == null) {
                            Text(
                                text = "暂无可用设备，请确保蓝牙已开启",
                                fontSize = 14.sp,
                                color = AppColors.DarkGray,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            // 已连接设备
                            connectedDevice?.let { device ->
                                Text(
                                    text = "已连接设备",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = AppColors.Primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                CompactDeviceListItem(
                                    device = device,
                                    isConnected = true,
                                    onConnect = {},
                                    onDisconnect = onDisconnectDevice
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            // 可用设备列表
                            if (scannedDevices.isNotEmpty()) {
                                Text(
                                    text = "可用设备",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = AppColors.OnSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                scannedDevices.forEach { device ->
                                    CompactDeviceListItem(
                                        device = device,
                                        isConnected = false,
                                        onConnect = { onConnectDevice(device) },
                                        onDisconnect = {}
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }

                // 体重展开内容
                AnimatedVisibility(
                    visible = expandedItem == "weight",
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = AppColors.LightGray.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))

                        if (isEditingWeight) {
                            // 编辑模式
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "体重：",
                                    fontSize = 16.sp,
                                    color = AppColors.OnSurface
                                )
                                BasicTextField(
                                    value = weightInput,
                                    onValueChange = { settingViewModel?.onWeightInputChange(it) },
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Decimal
                                    ),
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontSize = 16.sp,
                                        color = AppColors.OnSurface
                                    ),
                                    modifier = Modifier
                                        .width(80.dp)
                                        .height(40.dp)
                                        .border(
                                            width = 1.dp,
                                            color = AppColors.Primary,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                                Text(
                                    text = " kg",
                                    fontSize = 16.sp,
                                    color = AppColors.OnSurface
                                )

                                Spacer(modifier = Modifier.weight(1f))

                                // 确认按钮
                                IconButton(
                                    onClick = {
                                        val weight = settingViewModel?.validateWeightInput()
                                        if (weight != null) {
                                            onEditWeight(weight)
                                            expandedItem = null
                                        }
                                    }
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.check),
                                        contentDescription = "确认",
                                        tint = AppColors.Primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                // 取消按钮
                                IconButton(
                                    onClick = {
                                        settingViewModel?.cancelEditingWeight()
                                        expandedItem = null
                                    }
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.close),
                                        contentDescription = "取消",
                                        tint = AppColors.DarkGray,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        } else {
                            // 显示当前体重
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "当前体重：",
                                    fontSize = 16.sp,
                                    color = AppColors.OnSurface
                                )
                                Text(
                                    text = if (userWeight > 0) "${userWeight} kg" else "未设置",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (userWeight > 0) AppColors.Primary else AppColors.DarkGray
                                )
                            }
                        }
                    }
                }

                // 提醒展开内容
                AnimatedVisibility(
                    visible = expandedItem == "alert",
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = AppColors.LightGray.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "压力异常提醒",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = AppColors.OnSurface
                                )
                                Text(
                                    text = "检测到异常压力时通知",
                                    fontSize = 12.sp,
                                    color = AppColors.DarkGray
                                )
                            }
                            Switch(
                                checked = pressureAlertsEnabled,
                                onCheckedChange = onPressureAlertsChange,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = AppColors.Primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                    }
                }

                // 备份展开内容
                AnimatedVisibility(
                    visible = expandedItem == "backup",
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = AppColors.LightGray.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))

                        // 备份状态行（文字在左，按钮/状态在右）
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 左侧状态文字
                            Column {
                                Text(
                                    text = "数据备份",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = AppColors.OnSurface
                                )
                                Text(
                                    text = when {
                                        !isLoggedIn -> "请先登录"
                                        !hasData -> "暂无数据"
                                        uploadStatus == UploadStatus.UPLOADING -> "上传中..."
                                        uploadStatus == UploadStatus.SUCCESS -> "上传成功"
                                        uploadStatus == UploadStatus.FAILED -> "上传失败"
                                        else -> "点击备份"
                                    },
                                    fontSize = 12.sp,
                                    color = when (uploadStatus) {
                                        UploadStatus.SUCCESS -> AppColors.Primary
                                        UploadStatus.FAILED -> AppColors.Error
                                        else -> AppColors.DarkGray
                                    }
                                )
                            }

                            // 右侧按钮或状态图标
                            when (uploadStatus) {
                                UploadStatus.UPLOADING -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = AppColors.Primary,
                                        strokeWidth = 2.dp
                                    )
                                }
                                UploadStatus.SUCCESS -> {
                                    // 绿色成功勾选图标
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(AppColors.Success, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.check),
                                            contentDescription = "备份成功",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                UploadStatus.FAILED -> {
                                    // 失败重试按钮
                                    TextButton(
                                        onClick = { if (isLoggedIn && hasData) onBackupClick() },
                                        enabled = isLoggedIn && hasData
                                    ) {
                                        Text(
                                            "重试",
                                            color = if (isLoggedIn && hasData) AppColors.Error else AppColors.DarkGray,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                                else -> {
                                    // 未上传状态显示上传图标（无背景，只改变图标颜色）
                                    Icon(
                                        painter = painterResource(R.drawable.upload),
                                        contentDescription = "备份",
                                        tint = if (isLoggedIn && hasData) AppColors.Primary else AppColors.DarkGray.copy(alpha = 0.3f),
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clickable(
                                                enabled = isLoggedIn && hasData,
                                                onClick = { onBackupClick() }
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun QuickActionButton(
        icon: Int,
        label: String,
        isActive: Boolean,
        onClick: () -> Unit
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        if (isActive) AppColors.Primary else AppColors.Primary.copy(alpha = 0.1f),
                        RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = label,
                    tint = if (isActive) AppColors.Background else AppColors.Primary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                color = if (isActive) AppColors.Primary else AppColors.DarkGray,
                fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal
            )
        }
    }

    // ==================== 增强版设置列表（液态科技风格）====================
    @Composable
    fun SettingsList(
        isVersionExpanded: Boolean,
        isHelpExpanded: Boolean,
        isPrivacyExpanded: Boolean,
        isClearCacheExpanded: Boolean,
        onVersionExpandedChange: (Boolean) -> Unit,
        onHelpExpandedChange: (Boolean) -> Unit,
        onPrivacyExpandedChange: (Boolean) -> Unit,
        onClearCacheExpandedChange: (Boolean) -> Unit,
        onClearCache: () -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = AppColors.Background),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // 版本信息
                AboutAppItem(
                    appIcon = AppIcons.Info,
                    title = "版本信息",
                    subtitle = "v1.0.0",
                    isExpanded = isVersionExpanded,
                    onExpandedChange = onVersionExpandedChange
                ) {
                    VersionInfoContent()
                }

                GradientDivider()

                // 使用帮助
                AboutAppItem(
                    appIcon = AppIcons.Help,
                    title = "使用帮助",
                    subtitle = "操作指南与常见问题",
                    isExpanded = isHelpExpanded,
                    onExpandedChange = onHelpExpandedChange
                ) {
                    HelpGuideContent()
                }

                GradientDivider()

                // 隐私政策
                AboutAppItem(
                    appIcon = AppIcons.Security,
                    title = "隐私政策",
                    subtitle = "查看隐私保护条款",
                    isExpanded = isPrivacyExpanded,
                    onExpandedChange = onPrivacyExpandedChange
                ) {
                    PrivacyPolicyContent()
                }

                GradientDivider()

                // 清除缓存（红色主题）
                AboutAppItem(
                    appIcon = AppIcons.Delete,
                    title = "清除缓存",
                    subtitle = "清除应用本地数据",
                    isExpanded = isClearCacheExpanded,
                    onExpandedChange = onClearCacheExpandedChange,
                    iconBackground = AppColors.Error.copy(alpha = 0.1f),
                    iconTint = AppColors.Error
                ) {
                    ClearCacheContent(onClearCache = onClearCache)
                }
            }
        }
    }

    // ==================== 增强版 AboutAppItem（与QuickActionButton风格一致）====================
    @Composable
    fun AboutAppItem(
        appIcon: AppIcon,
        title: String,
        subtitle: String,
        isExpanded: Boolean,
        onExpandedChange: (Boolean) -> Unit,
        isLastItem: Boolean = false,
        iconBackground: Color = AppColors.Primary.copy(alpha = 0.1f),
        iconTint: Color = AppColors.Primary,
        expandedContent: @Composable () -> Unit
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 标题行（可点击）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onExpandedChange(!isExpanded) }
                    )
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 图标容器 - 与QuickActionButton风格一致（纯色背景）
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            color = iconBackground,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    when (appIcon) {
                        is AppIcon.MaterialIcon -> {
                            Icon(
                                imageVector = appIcon.icon,
                                contentDescription = title,
                                tint = iconTint,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        is AppIcon.ResourceIcon -> {
                            Icon(
                                painter = painterResource(id = appIcon.resId),
                                contentDescription = title,
                                tint = iconTint,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.width(14.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = if (isExpanded) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isExpanded) AppColors.Primary else AppColors.OnSurface
                    )
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = AppColors.DarkGray
                    )
                }
                
                // 箭头动画（与ExpandableArrowIcon一致）
                ExpandableArrowIcon(
                    isExpanded = isExpanded,
                    useGraphicsLayer = true
                )
            }

            // 展开内容 - 带左侧强调线和交错动画
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeIn(
                    animationSpec = tween(200)
                ),
                exit = shrinkVertically(
                    animationSpec = tween(250, easing = FastOutSlowInEasing)
                ) + fadeOut(
                    animationSpec = tween(150)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    expandedContent()
                }
            }
        }
    }

    // ==================== 辅助组件 ====================
    /**
     * 渐变分割线
     */
    @Composable
    private fun GradientDivider() {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(1.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            AppColors.LightGray.copy(alpha = 0.6f),
                            AppColors.LightGray.copy(alpha = 0.6f),
                            Color.Transparent
                        ),
                        startX = 0f,
                        endX = 1000f
                    )
                )
        )
    }

    /**
     * 交错动画内容容器 - 用于展开内容的逐行淡入效果
     */
    @Composable
    private fun StaggeredColumn(
        modifier: Modifier = Modifier,
        verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(8.dp),
        content: @Composable ColumnScope.() -> Unit
    ) {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = verticalArrangement
        ) {
            content()
        }
    }

    // ==================== AboutAppSection 内容组件 ====================

    /**
     * 版本信息内容 - 卡片式信息展示
     */
    @Composable
    private fun VersionInfoContent() {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 应用信息卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AppColors.Primary.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    InfoRow(icon = R.drawable.icon_foot, label = "应用名称", value = "举足凝健")
                    HorizontalDivider(color = AppColors.LightGray.copy(alpha = 0.5f))
                    InfoRow(iconVector = Icons.Default.Info, label = "版本号", value = "v1.2.0")
                    HorizontalDivider(color = AppColors.LightGray.copy(alpha = 0.5f))
                    InfoRow(icon = R.drawable.update, label = "构建日期", value = "2026年4月")
                    HorizontalDivider(color = AppColors.LightGray.copy(alpha = 0.5f))
                    InfoRow(icon = R.drawable.man, label = "开发者", value = "SmartShoe Team")
                }
            }

            // 版权信息
            Text(
                text = "© 2026 SmartShoe Team. All rights reserved.",
                fontSize = 11.sp,
                color = AppColors.DarkGray.copy(alpha = 0.7f),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }

    /**
     * 信息行组件 - 带资源图标的键值对展示
     */
    @Composable
    private fun InfoRow(icon: Int, label: String, value: String) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = AppColors.Primary.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    fontSize = 13.sp,
                    color = AppColors.DarkGray
                )
            }
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = AppColors.OnSurface
            )
        }
    }

    /**
     * 信息行组件 - 带矢量图标的键值对展示
     */
    @Composable
    private fun InfoRow(iconVector: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = null,
                    tint = AppColors.Primary.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    fontSize = 13.sp,
                    color = AppColors.DarkGray
                )
            }
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = AppColors.OnSurface
            )
        }
    }

    /**
     * 使用帮助内容 - 步骤式引导
     */
    @Composable
    private fun HelpGuideContent() {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {

            // 步骤列表
            StepItem(number = 1, text = "点击「设备」按钮扫描并连接智能鞋垫")
            Spacer(modifier = Modifier.height(12.dp))
            StepItem(number = 2, text = "连接成功后查看实时压力分布图")
            Spacer(modifier = Modifier.height(12.dp))
            StepItem(number = 3, text = "在「体重」中设置您的体重数据")
            Spacer(modifier = Modifier.height(12.dp))
            StepItem(number = 4, text = "开启「提醒」功能获得压力异常通知")
            Spacer(modifier = Modifier.height(12.dp))
            StepItem(number = 5, text = "使用「备份」功能将数据上传云端")

            Spacer(modifier = Modifier.height(12.dp))

            // 提示卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AppColors.Success.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(10.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 图标容器 - 24dp与步骤数字圆圈大小一致，确保视觉对齐
                    Box(
                        modifier = Modifier.size(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = AppColors.Success,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "首次使用请确保蓝牙已开启，并将设备靠近手机",
                        fontSize = 12.sp,
                        color = AppColors.Success.copy(alpha = 0.9f),
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }

    /**
     * 步骤项组件 - 带数字圆圈的步骤指示
     */
    @Composable
    private fun StepItem(number: Int, text: String) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 数字圆圈 - 24dp大小，通过paddingLeft与外部44dp图标容器对齐
            // 计算：外部图标容器44dp，圆圈24dp，需要 (44-24)/2 = 10dp 的padding
            Box(
                modifier = Modifier
                    .padding(start = 10.dp)
                    .size(24.dp)
                    .background(AppColors.Primary.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = number.toString(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.Primary
                )
            }
            Spacer(modifier = Modifier.width(22.dp)) // 12dp + 10dp补偿padding
            Text(
                text = text,
                fontSize = 13.sp,
                color = AppColors.OnSurface,
                lineHeight = 20.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }

    /**
     * 隐私政策内容 - 分段式展示
     */
    @Composable
    private fun PrivacyPolicyContent() {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 更新时间标签
            Card(
                colors = CardDefaults.cardColors(containerColor = AppColors.AiModeDeep.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Text(
                    text = "最后更新：2026年4月",
                    fontSize = 11.sp,
                    color = AppColors.AiModeDeep,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            // 隐私政策段落
            PrivacySection(
                title = "信息收集",
                content = "我们仅在必要时收集和使用您的个人信息，以提供和改进我们的服务。"
            )

            PrivacySection(
                title = "数据用途",
                content = "我们收集的信息包括蓝牙设备名称、传感器数据和连接状态，这些信息仅用于实时显示压力分布和生成历史数据图表。"
            )

            PrivacySection(
                title = "数据保护",
                content = "我们承诺不会与第三方共享您的个人数据，除非获得您的明确同意或法律要求。"
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 联系信息
            Text(
                text = "如有疑问，请联系开发团队",
                fontSize = 11.sp,
                color = AppColors.DarkGray.copy(alpha = 0.7f)
            )
        }
    }

    /**
     * 隐私政策段落组件
     */
    @Composable
    private fun PrivacySection(title: String, content: String) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.OnSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = content,
                fontSize = 12.sp,
                color = AppColors.DarkGray,
                lineHeight = 18.sp
            )
        }
    }

    /**
     * 清除缓存内容 - 图标化列表展示
     */
    @Composable
    private fun ClearCacheContent(onClearCache: () -> Unit) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 警告卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AppColors.Error.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(10.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = AppColors.Error,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "清除缓存将删除以下数据，此操作不可恢复",
                        fontSize = 12.sp,
                        color = AppColors.Error.copy(alpha = 0.9f),
                        lineHeight = 18.sp
                    )
                }
            }

            // 数据列表
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AppColors.LightGray.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(10.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ClearCacheItem(icon = Icons.Default.Person, text = "用户登录信息")
                    ClearCacheItem(icon = R.drawable.bluetooth, text = "蓝牙设备列表")
                    ClearCacheItem(icon = R.drawable.history, text = "传感器历史数据")
                    ClearCacheItem(icon = R.drawable.man, text = "体重设置")
                    ClearCacheItem(icon = R.drawable.analytics, text = "临时文件")
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 清除按钮
            Button(
                onClick = onClearCache,
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Error),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(10.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "清除",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("确认清除缓存", color = AppColors.Background, fontSize = 14.sp)
            }
        }
    }

    /**
     * 清除缓存项组件
     */
    @Composable
    private fun ClearCacheItem(icon: Any, text: String) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (icon) {
                is Int -> {
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = null,
                        tint = AppColors.DarkGray.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
                else -> {
                    Icon(
                        imageVector = icon as androidx.compose.ui.graphics.vector.ImageVector,
                        contentDescription = null,
                        tint = AppColors.DarkGray.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = text,
                fontSize = 13.sp,
                color = AppColors.DarkGray
            )
        }
    }

    // ==================== 登录/注册对话框（保持原有实现）====================
    @Composable
    private fun LoginDialog(
        onDismiss: () -> Unit,
        onLogin: (String, String) -> Unit,
        onSwitchToRegister: () -> Unit,
        viewModel: SettingViewModel? = null
    ) {
        val email = viewModel?.loginEmail?.collectAsStateWithLifecycle()?.value ?: ""
        val password = viewModel?.loginPassword?.collectAsStateWithLifecycle()?.value ?: ""
        val passwordVisible = viewModel?.loginPasswordVisible?.collectAsStateWithLifecycle()?.value ?: false
        val isLoading = viewModel?.isLoginLoading?.collectAsStateWithLifecycle()?.value ?: false
        val loginError = viewModel?.loginError?.collectAsStateWithLifecycle()?.value
        val loginErrorField = viewModel?.loginErrorField?.collectAsStateWithLifecycle()?.value
        val scrollState = rememberScrollState()

        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false
            )
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = AppColors.Background),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .verticalScroll(scrollState)
                ) {
                    Text(
                        text = "用户登录",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.Primary
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    SmartShoeTextField.Email(
                        value = email,
                        onValueChange = { viewModel?.onLoginEmailChange(it) },
                        label = "邮箱地址",
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = Icons.Default.Email,
                        isError = loginError != null && 
                            (loginErrorField == SettingViewModel.LoginErrorField.EMAIL || 
                             loginErrorField == SettingViewModel.LoginErrorField.BOTH),
                        errorMessage = if (loginErrorField == SettingViewModel.LoginErrorField.EMAIL || 
                                          loginErrorField == SettingViewModel.LoginErrorField.BOTH) loginError else null
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SmartShoeTextField.Password(
                        value = password,
                        onValueChange = { viewModel?.onLoginPasswordChange(it) },
                        label = "密码",
                        passwordVisible = passwordVisible,
                        onPasswordVisibilityChange = { viewModel?.onLoginPasswordVisibilityChange(!passwordVisible) },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = Icons.Default.Lock,
                        visibilityIcon = painterResource(R.drawable.visibility),
                        visibilityOffIcon = painterResource(R.drawable.visibility_off),
                        isError = loginError != null && 
                            (loginErrorField == SettingViewModel.LoginErrorField.PASSWORD || 
                             loginErrorField == SettingViewModel.LoginErrorField.BOTH),
                        errorMessage = if (loginErrorField == SettingViewModel.LoginErrorField.PASSWORD || 
                                          loginErrorField == SettingViewModel.LoginErrorField.BOTH) loginError else null
                    )
                    // 显示全局错误提示（当错误不关联特定字段时）
                    if (loginError != null && loginErrorField == null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = loginError,
                            fontSize = 14.sp,
                            color = AppColors.Error,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { onLogin(email, password) },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.Primary
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = AppColors.Background,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("登录", fontSize = 16.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("还没有账户？", fontSize = 14.sp, color = AppColors.DarkGray)
                        TextButton(onClick = onSwitchToRegister) {
                            Text("立即注册", color = AppColors.Primary, fontSize = 14.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("取消", color = AppColors.DarkGray)
                    }
                }
            }
        }
    }

    @Composable
    private fun RegisterDialog(
        onDismiss: () -> Unit,
        onRegister: (String, String, String) -> Unit,
        onSwitchToLogin: () -> Unit,
        viewModel: SettingViewModel? = null
    ) {
        val username = viewModel?.registerUsername?.collectAsStateWithLifecycle()?.value ?: ""
        val email = viewModel?.registerEmail?.collectAsStateWithLifecycle()?.value ?: ""
        val password = viewModel?.registerPassword?.collectAsStateWithLifecycle()?.value ?: ""
        val confirmPassword = viewModel?.registerConfirmPassword?.collectAsStateWithLifecycle()?.value ?: ""
        val passwordVisible = viewModel?.registerPasswordVisible?.collectAsStateWithLifecycle()?.value ?: false
        val isLoading = viewModel?.isRegisterLoading?.collectAsStateWithLifecycle()?.value ?: false
        val passwordsMatch = password == confirmPassword || password.isEmpty()
        val scrollState = rememberScrollState()

        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false
            )
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = AppColors.Background),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .verticalScroll(scrollState)
                ) {
                    Text(
                        text = "用户注册",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.Primary
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    SmartShoeTextField.Username(
                        value = username,
                        onValueChange = { viewModel?.onRegisterUsernameChange(it) },
                        label = "用户名",
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = Icons.Default.Person
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SmartShoeTextField.Email(
                        value = email,
                        onValueChange = { viewModel?.onRegisterEmailChange(it) },
                        label = "邮箱地址",
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = Icons.Default.Email
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SmartShoeTextField.Password(
                        value = password,
                        onValueChange = { viewModel?.onRegisterPasswordChange(it) },
                        label = "密码",
                        passwordVisible = passwordVisible,
                        onPasswordVisibilityChange = { viewModel?.onRegisterPasswordVisibilityChange(!passwordVisible) },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = Icons.Default.Lock,
                        visibilityIcon = painterResource(R.drawable.visibility),
                        visibilityOffIcon = painterResource(R.drawable.visibility_off)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SmartShoeTextField.Password(
                        value = confirmPassword,
                        onValueChange = { viewModel?.onRegisterConfirmPasswordChange(it) },
                        label = "确认密码",
                        passwordVisible = passwordVisible,
                        onPasswordVisibilityChange = { viewModel?.onRegisterPasswordVisibilityChange(!passwordVisible) },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = Icons.Default.Lock,
                        isError = !passwordsMatch && confirmPassword.isNotBlank(),
                        errorMessage = if (!passwordsMatch && confirmPassword.isNotBlank()) "密码不匹配" else null,
                        visibilityIcon = painterResource(R.drawable.visibility),
                        visibilityOffIcon = painterResource(R.drawable.visibility_off)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { onRegister(username, email, password) },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.Primary
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = AppColors.Background,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("注册", fontSize = 16.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("已有账户？", fontSize = 14.sp, color = AppColors.DarkGray)
                        TextButton(onClick = onSwitchToLogin) {
                            Text("立即登录", color = AppColors.Primary, fontSize = 14.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("取消", color = AppColors.DarkGray)
                    }
                }
            }
        }
    }

    // ==================== 编辑资料对话框 ====================
    @Composable
    private fun EditProfileDialog(
        userState: UserState,
        onDismiss: () -> Unit,
        onSave: (String, String, String, String) -> Unit,
        onLogout: () -> Unit,
        viewModel: SettingViewModel? = null
    ) {
        val username = viewModel?.editProfileUsername?.collectAsStateWithLifecycle()?.value ?: userState.username
        val currentPassword = viewModel?.editProfileCurrentPassword?.collectAsStateWithLifecycle()?.value ?: ""
        val newPassword = viewModel?.editProfileNewPassword?.collectAsStateWithLifecycle()?.value ?: ""
        val confirmPassword = viewModel?.editProfileConfirmPassword?.collectAsStateWithLifecycle()?.value ?: ""
        val passwordVisible = viewModel?.editProfilePasswordVisible?.collectAsStateWithLifecycle()?.value ?: false
        val isLoading = viewModel?.isEditProfileLoading?.collectAsStateWithLifecycle()?.value ?: false
        val passwordsMatch = newPassword == confirmPassword || newPassword.isEmpty()
        val scrollState = rememberScrollState()

        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false
            )
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = AppColors.Background),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .verticalScroll(scrollState)
                ) {
                    Text(
                        text = "编辑资料",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.Primary
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    // 邮箱只显示，不可编辑
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = AppColors.DarkGray,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "邮箱地址",
                                fontSize = 12.sp,
                                color = AppColors.DarkGray
                            )
                            Text(
                                text = userState.email,
                                fontSize = 16.sp,
                                color = AppColors.OnSurface
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    SmartShoeTextField.Username(
                        value = username,
                        onValueChange = { viewModel?.onEditProfileUsernameChange(it) },
                        label = "用户名（可选）",
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = Icons.Default.Person
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SmartShoeTextField.Password(
                        value = currentPassword,
                        onValueChange = { viewModel?.onEditProfileCurrentPasswordChange(it) },
                        label = "当前密码（验证身份）",
                        passwordVisible = passwordVisible,
                        onPasswordVisibilityChange = { viewModel?.onEditProfilePasswordVisibilityChange(!passwordVisible) },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = Icons.Default.Lock,
                        visibilityIcon = painterResource(R.drawable.visibility),
                        visibilityOffIcon = painterResource(R.drawable.visibility_off)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SmartShoeTextField.Password(
                        value = newPassword,
                        onValueChange = { viewModel?.onEditProfileNewPasswordChange(it) },
                        label = "新密码（留空不修改）",
                        passwordVisible = passwordVisible,
                        onPasswordVisibilityChange = { viewModel?.onEditProfilePasswordVisibilityChange(!passwordVisible) },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = Icons.Default.Lock,
                        visibilityIcon = painterResource(R.drawable.visibility),
                        visibilityOffIcon = painterResource(R.drawable.visibility_off)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SmartShoeTextField.Password(
                        value = confirmPassword,
                        onValueChange = { viewModel?.onEditProfileConfirmPasswordChange(it) },
                        label = "确认新密码",
                        passwordVisible = passwordVisible,
                        onPasswordVisibilityChange = { viewModel?.onEditProfilePasswordVisibilityChange(!passwordVisible) },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = Icons.Default.Lock,
                        isError = !passwordsMatch && confirmPassword.isNotBlank(),
                        errorMessage = if (!passwordsMatch && confirmPassword.isNotBlank()) "密码不匹配" else null,
                        visibilityIcon = painterResource(R.drawable.visibility),
                        visibilityOffIcon = painterResource(R.drawable.visibility_off)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "提示：修改资料需要输入当前密码验证",
                        fontSize = 12.sp,
                        color = AppColors.DarkGray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { onSave(username, userState.email, newPassword, currentPassword) },
                        enabled = !isLoading && currentPassword.isNotBlank() && passwordsMatch,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.Primary
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = AppColors.Background,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("保存修改", fontSize = 16.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("取消", color = AppColors.DarkGray)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 退出登录按钮
                    Button(
                        onClick = {
                            onLogout()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.Error
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "退出登录",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("退出登录", color = AppColors.Background)
                    }
                }
            }
        }
    }

    // ==================== 主入口 ====================
    @Composable
    fun SettingsScreen(
        modifier: Modifier = Modifier,
        scannedDevices: List<BluetoothDevice>,
        connectedDevice: BluetoothDevice?,
        userWeight: Float,
        onConnectDevice: (BluetoothDevice) -> Unit,
        onDisconnectDevice: () -> Unit,
        onClearCache: () -> Unit,
        onEditWeight: (Float) -> Unit,
        userState: UserState,
        onLogin: (String, String) -> Unit,
        onRegister: (String, String, String) -> Unit,
        onLogout: () -> Unit,
        onEditProfile: (String, String, String, String) -> Unit,
        pressureAlertsEnabled: Boolean,
        onPressureAlertsChange: (Boolean) -> Unit,
        onBackupData: (Boolean, String, (Boolean) -> Unit) -> Unit,
        isLoggedIn: Boolean,
        hasData: Boolean,
        // 调试功能：生成模拟数据（当前未在设置页面使用，保留供外部调用）
        onGenerateMockData: () -> Unit = {},
        settingViewModel: SettingViewModel? = null,
        onShowError: ((String) -> Unit)? = null,
        authUiState: AuthUiState = AuthUiState.Idle
    ) {
        // 对话框状态 - 使用collectAsStateWithLifecycle自动处理生命周期
        val showLoginDialog by settingViewModel?.showLoginDialog?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(false) }
        val showRegisterDialog by settingViewModel?.showRegisterDialog?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(false) }
        val isEditExpanded by settingViewModel?.isEditProfileExpanded?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(false) }
        val uploadStatus by settingViewModel?.uploadStatus?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(UploadStatus.IDLE) }
        val errorMessage = settingViewModel?.errorMessage?.collectAsStateWithLifecycle()?.value

        // 设置列表展开状态
        var isVersionExpanded by rememberSaveable { mutableStateOf(false) }
        var isHelpExpanded by rememberSaveable { mutableStateOf(false) }
        var isPrivacyExpanded by rememberSaveable { mutableStateOf(false) }
        var isClearCacheExpanded by rememberSaveable { mutableStateOf(false) }

        // 监听认证状态
        LaunchedEffect(authUiState) {
            when (authUiState) {
                is AuthUiState.Success -> settingViewModel?.handleAuthCompleted(success = true)
                is AuthUiState.Error -> {
                    val errorMessage = (authUiState as AuthUiState.Error).message
                    // 根据 HTTP 状态码和错误消息统一处理登录错误
                    // 错误格式可能是 "[401] 登录失败" 或纯文本消息
                    val httpCodeRegex = Regex("\\[(\\d+)]")
                    val httpCode = httpCodeRegex.find(errorMessage)?.groupValues?.get(1)?.toIntOrNull()
                    val cleanMessage = errorMessage.replace(httpCodeRegex, "").trim()
                    
                    when {
                        // HTTP 401 - 未授权（账号或密码错误）
                        httpCode == 401 -> {
                            settingViewModel?.setLoginError(
                                message = "邮箱或密码错误",
                                field = SettingViewModel.LoginErrorField.BOTH
                            )
                        }
                        // HTTP 403 - 禁止访问（账户被禁用）
                        httpCode == 403 -> {
                            settingViewModel?.setLoginError(
                                message = "账户已被禁用",
                                field = SettingViewModel.LoginErrorField.BOTH
                            )
                        }
                        // HTTP 4xx - 客户端错误（请求格式问题等）
                        httpCode != null && httpCode in 400..499 -> {
                            settingViewModel?.setLoginError(
                                message = cleanMessage.ifEmpty { "请求参数错误" },
                                field = SettingViewModel.LoginErrorField.BOTH
                            )
                        }
                        // HTTP 5xx - 服务器错误
                        httpCode != null && httpCode in 500..599 -> {
                            settingViewModel?.setLoginError(
                                message = "服务器错误，请稍后重试",
                                field = null
                            )
                        }
                        // 网络连接错误
                        cleanMessage.contains("连接", ignoreCase = true) ||
                        cleanMessage.contains("网络", ignoreCase = true) ||
                        cleanMessage.contains("timeout", ignoreCase = true) ||
                        cleanMessage.contains("无法连接", ignoreCase = true) -> {
                            settingViewModel?.setLoginError(
                                message = "网络连接失败，请检查网络设置",
                                field = null
                            )
                        }
                        // 其他未知错误
                        else -> {
                            settingViewModel?.setLoginError(
                                message = cleanMessage.ifEmpty { "登录失败，请重试" },
                                field = null
                            )
                        }
                    }
                    settingViewModel?.handleAuthCompleted(success = false)
                }
                else -> {}
            }
        }

        // 显示全局错误（使用 Snackbar 等，不使用 Toast）
        errorMessage?.let { message ->
            onShowError?.invoke(message)
            settingViewModel.clearError()
        }

        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(AppColors.Background),
            contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
        ) {
            // 头部蓝色区域
            item {
                ProfileHeader(
                    userState = userState,
                    connectedDevice = connectedDevice,
                    userWeight = userWeight,
                    uploadStatus = uploadStatus,
                    onEditProfileClick = {
                        if (userState.isLoggedIn) {
                            settingViewModel?.showEditProfileDialog()
                        } else {
                            settingViewModel?.showLoginDialog()
                        }
                    }
                )
            }

            // 留出悬浮卡片的空间
            item {
                Spacer(modifier = Modifier.height(50.dp))
            }

            // 可展开快捷功能区
            item {
                ExpandableQuickActions(
                    scannedDevices = scannedDevices,
                    connectedDevice = connectedDevice,
                    onConnectDevice = onConnectDevice,
                    onDisconnectDevice = onDisconnectDevice,
                    userWeight = userWeight,
                    onEditWeight = onEditWeight,
                    pressureAlertsEnabled = pressureAlertsEnabled,
                    onPressureAlertsChange = onPressureAlertsChange,
                    onBackupClick = {
                        if (settingViewModel?.canBackup(isLoggedIn, hasData) == true) {
                            settingViewModel.setUploadStatus(UploadStatus.UPLOADING)
                            onBackupData(true, "") { success ->
                                settingViewModel.setUploadStatus(
                                    if (success) UploadStatus.SUCCESS else UploadStatus.FAILED
                                )
                            }
                        }
                    },
                    uploadStatus = uploadStatus,
                    isLoggedIn = isLoggedIn,
                    hasData = hasData,
                    settingViewModel = settingViewModel
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 设置列表（复用AboutApp内容，包含清除缓存）
            item {
                // 互斥展开：展开一项时自动收起其他项
                fun expandOnly(target: String) {
                    isVersionExpanded = target == "version"
                    isHelpExpanded = target == "help"
                    isPrivacyExpanded = target == "privacy"
                    isClearCacheExpanded = target == "clearCache"
                }

                SettingsList(
                    isVersionExpanded = isVersionExpanded,
                    isHelpExpanded = isHelpExpanded,
                    isPrivacyExpanded = isPrivacyExpanded,
                    isClearCacheExpanded = isClearCacheExpanded,
                    onVersionExpandedChange = { expandOnly(if (it) "version" else "") },
                    onHelpExpandedChange = { expandOnly(if (it) "help" else "") },
                    onPrivacyExpandedChange = { expandOnly(if (it) "privacy" else "") },
                    onClearCacheExpandedChange = { expandOnly(if (it) "clearCache" else "") },
                    onClearCache = onClearCache
                )
            }
        }

        // 登录对话框
        if (showLoginDialog) {
            LoginDialog(
                onDismiss = { settingViewModel?.hideLoginDialog() },
                onLogin = { email, password ->
                    // 移除前端验证，直接调用后端
                    settingViewModel?.setLoginLoading(true)
                    onLogin(email, password)
                },
                onSwitchToRegister = { settingViewModel?.switchToRegister() },
                viewModel = settingViewModel
            )
        }

        // 注册对话框
        if (showRegisterDialog) {
            RegisterDialog(
                onDismiss = { settingViewModel?.hideRegisterDialog() },
                onRegister = { username, email, password ->
                    settingViewModel?.validateRegisterForm()?.let { error ->
                        settingViewModel.showError(error)
                        return@RegisterDialog
                    }
                    settingViewModel?.setRegisterLoading(true)
                    onRegister(username, email, password)
                },
                onSwitchToLogin = { settingViewModel?.switchToLogin() },
                viewModel = settingViewModel
            )
        }

        // 编辑资料对话框
        if (isEditExpanded && userState.isLoggedIn) {
            // 初始化编辑表单
            LaunchedEffect(Unit) {
                settingViewModel?.initEditProfileForm(userState)
            }
            
            EditProfileDialog(
                userState = userState,
                onDismiss = { settingViewModel?.toggleEditProfileExpanded() },
                onSave = { username, email, newPassword, currentPassword ->
                    onEditProfile(username, email, newPassword, currentPassword)
                },
                onLogout = onLogout,
                viewModel = settingViewModel
            )
        }
    }
}
