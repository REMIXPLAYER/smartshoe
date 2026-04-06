package com.example.smartshoe.ui.screen

import android.bluetooth.BluetoothDevice
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.draw.shadow
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
import com.example.smartshoe.data.model.UserState
import com.example.smartshoe.ui.component.DeviceSettingItem
import com.example.smartshoe.ui.component.ExpandableArrowIcon
import com.example.smartshoe.ui.component.SmartShoeTextField
import com.example.smartshoe.ui.component.VersionDetailItem
import com.example.smartshoe.ui.theme.AppColors
import com.example.smartshoe.ui.theme.AppIcon
import com.example.smartshoe.ui.theme.AppIcons
import com.example.smartshoe.ui.viewmodel.AuthUiState
import com.example.smartshoe.ui.viewmodel.SettingViewModel
import com.example.smartshoe.ui.viewmodel.UploadStatus
import com.example.smartshoe.util.AnimationDefaults

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
                        .background(Color.White, CircleShape)
                        .border(3.dp, Color.White.copy(alpha = 0.5f), CircleShape),
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
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (userState.isLoggedIn) userState.email else "点击登录账户",
                        color = Color.White.copy(alpha = 0.85f),
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
                            painter = painterResource(R.drawable.edit),
                            contentDescription = "编辑",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else {
                    // 未登录时显示箭头提示
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "登录",
                        tint = Color.White.copy(alpha = 0.7f),
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
            colors = CardDefaults.cardColors(containerColor = Color.White),
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
                    indicatorColor = if (isConnected) AppColors.Success else Color.Gray,
                    isActive = isConnected
                )
                
                // 分隔线
                StatsDivider()
                
                // 体重数据
                StatItem(
                    icon = R.drawable.man,
                    label = "体重数据",
                    value = if (weight > 0) "${weight}kg" else "未设置",
                    indicatorColor = if (weight > 0) AppColors.Primary else Color.Gray,
                    isActive = weight > 0
                )
                
                // 分隔线
                StatsDivider()
                
                // 备份状态
                val (backupText, backupColor) = when (uploadStatus) {
                    UploadStatus.UPLOADING -> "上传中" to AppColors.Primary
                    UploadStatus.SUCCESS -> "已备份" to AppColors.Success
                    UploadStatus.FAILED -> "失败" to AppColors.Error
                    else -> "未备份" to Color.Gray
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
                    tint = if (isActive) indicatorColor else Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isActive) indicatorColor else Color.Gray
            )
        }
    }

    @Composable
    fun StatsDivider() {
        HorizontalDivider(
            modifier = Modifier
                .height(50.dp)
                .width(1.dp),
            color = Color(0xFFEEEEEE)
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
            colors = CardDefaults.cardColors(containerColor = Color.White),
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
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))

                        // 设备列表
                        if (scannedDevices.isEmpty() && connectedDevice == null) {
                            Text(
                                text = "暂无可用设备，请确保蓝牙已开启",
                                fontSize = 14.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            // 已连接设备
                            connectedDevice?.let { device ->
                                Text(
                                    text = "已连接设备",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = AppColors.Success
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                DeviceSettingItem(
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
                                    DeviceSettingItem(
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
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
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
                                        tint = AppColors.Success,
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
                                        tint = Color.Gray,
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
                                    color = if (userWeight > 0) AppColors.Primary else Color.Gray
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
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
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
                                    color = Color.Gray
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
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))

                        // 备份状态提示
                        Text(
                            text = when {
                                !isLoggedIn -> "请先登录后再备份数据"
                                !hasData -> "暂无数据可备份"
                                else -> "选择要备份的数据"
                            },
                            fontSize = 14.sp,
                            color = if (!isLoggedIn || !hasData) Color.Gray else AppColors.OnSurface
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 备份按钮
                        Button(
                            onClick = {
                                if (isLoggedIn && hasData) {
                                    onBackupClick()
                                }
                            },
                            enabled = isLoggedIn && hasData && uploadStatus != UploadStatus.UPLOADING,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppColors.Primary
                            )
                        ) {
                            when (uploadStatus) {
                                UploadStatus.UPLOADING -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("上传中...")
                                }
                                UploadStatus.SUCCESS -> {
                                    Text("备份成功")
                                }
                                UploadStatus.FAILED -> {
                                    Text("备份失败，重试")
                                }
                                else -> {
                                    Text("立即备份到云端")
                                }
                            }
                        }

                        // TODO: 后续增加本地记录选择功能
                        /*
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "选择要上传的记录：",
                            fontSize = 14.sp,
                            color = AppColors.OnSurface
                        )
                        // 这里可以添加本地记录列表供用户选择
                        */
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
                    tint = if (isActive) Color.White else AppColors.Primary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                color = if (isActive) AppColors.Primary else Color.Gray,
                fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal
            )
        }
    }

    // ==================== 设置列表（复用AboutApp内容，整合在一个卡片内） ====================
    @Composable
    fun SettingsList(
        isVersionExpanded: Boolean,
        isHelpExpanded: Boolean,
        isPrivacyExpanded: Boolean,
        onVersionExpandedChange: (Boolean) -> Unit,
        onHelpExpandedChange: (Boolean) -> Unit,
        onPrivacyExpandedChange: (Boolean) -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // 版本信息
                AboutAppItem(
                    appIcon = AppIcons.Info,
                    title = "版本信息",
                    subtitle = "v1.0.0",
                    isExpanded = isVersionExpanded,
                    onExpandedChange = onVersionExpandedChange,
                    isLastItem = false
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        VersionDetailItem("应用名称", "举足凝健")
                        Spacer(modifier = Modifier.height(8.dp))
                        VersionDetailItem("版本号", "v1.0.0")
                        Spacer(modifier = Modifier.height(8.dp))
                        VersionDetailItem("构建日期", "2026年4月")
                        Spacer(modifier = Modifier.height(8.dp))
                        VersionDetailItem("开发者", "SmartShoe Team")
                    }
                }

                // 分隔线
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = Color(0xFFEEEEEE)
                )

                // 使用帮助
                AboutAppItem(
                    appIcon = AppIcons.Help,
                    title = "使用帮助",
                    subtitle = "操作指南与常见问题",
                    isExpanded = isHelpExpanded,
                    onExpandedChange = onHelpExpandedChange,
                    isLastItem = false
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "使用指南:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = AppColors.OnSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "1. 点击「设备」按钮扫描并连接智能鞋垫\n" +
                                    "2. 连接成功后查看实时压力分布图\n" +
                                    "3. 在「体重」中设置您的体重数据\n" +
                                    "4. 开启「提醒」功能获得压力异常通知\n" +
                                    "5. 使用「备份」功能将数据上传云端",
                            fontSize = 13.sp,
                            color = Color.DarkGray,
                            lineHeight = 20.sp
                        )
                    }
                }

                // 分隔线
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = Color(0xFFEEEEEE)
                )

                // 隐私政策
                AboutAppItem(
                    appIcon = AppIcons.Security,
                    title = "隐私政策",
                    subtitle = "查看隐私保护条款",
                    isExpanded = isPrivacyExpanded,
                    onExpandedChange = onPrivacyExpandedChange,
                    isLastItem = true
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "最后更新日期: 2026年4月",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "本应用尊重并保护您的隐私权。我们仅在必要时收集和使用您的个人信息，以提供和改进我们的服务。\n\n" +
                                    "我们收集的信息包括蓝牙设备名称、传感器数据和连接状态，这些信息仅用于实时显示压力分布和生成历史数据图表。\n\n" +
                                    "我们承诺不会与第三方共享您的个人数据，除非获得您的明确同意或法律要求。",
                            fontSize = 13.sp,
                            color = Color.DarkGray,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun AboutAppItem(
        appIcon: AppIcon,
        title: String,
        subtitle: String,
        isExpanded: Boolean,
        onExpandedChange: (Boolean) -> Unit,
        isLastItem: Boolean,
        expandedContent: @Composable () -> Unit
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
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
                // 根据图标类型显示不同的图标
                when (appIcon) {
                    is AppIcon.MaterialIcon -> {
                        Icon(
                            imageVector = appIcon.icon,
                            contentDescription = title,
                            tint = AppColors.Primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    is AppIcon.ResourceIcon -> {
                        Icon(
                            painter = painterResource(id = appIcon.resId),
                            contentDescription = title,
                            tint = AppColors.Primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = AppColors.OnSurface
                    )
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                ExpandableArrowIcon(
                    isExpanded = isExpanded,
                    useGraphicsLayer = true
                )
            }

            // 展开内容
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(
                    animationSpec = AnimationDefaults.expandTween
                ) + fadeIn(animationSpec = AnimationDefaults.fadeInTween),
                exit = shrinkVertically(
                    animationSpec = AnimationDefaults.shrinkTween
                ) + fadeOut(animationSpec = AnimationDefaults.fadeOutTween)
            ) {
                expandedContent()
            }
        }
    }

    // ==================== 清除缓存按钮 ====================
    @Composable
    fun ClearCacheButton(
        onClearCache: () -> Unit,
        settingViewModel: SettingViewModel? = null
    ) {
        val showClearCacheConfirm = settingViewModel?.showClearCacheConfirm?.collectAsStateWithLifecycle()?.value ?: false

        Button(
            onClick = { settingViewModel?.showClearCacheConfirmDialog() },
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Error),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = "清除",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("清除应用缓存", color = Color.White, fontSize = 16.sp)
        }

        // 清除缓存确认对话框
        if (showClearCacheConfirm) {
            AlertDialog(
                onDismissRequest = { settingViewModel?.hideClearCacheConfirmDialog() },
                title = { Text("清除缓存确认") },
                text = {
                    Text("确定要清除所有缓存数据吗？这将包括：\n• 用户数据\n• 蓝牙设备列表\n• 传感器数据\n• 体重数据\n• 连接状态\n• 临时文件\n\n此操作不可撤销。")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onClearCache()
                            settingViewModel?.hideClearCacheConfirmDialog()
                        }
                    ) {
                        Text("确认清除", color = AppColors.Error)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { settingViewModel?.hideClearCacheConfirmDialog() }
                    ) {
                        Text("取消")
                    }
                }
            )
        }
    }

    // ==================== 登录/注册对话框（保持原有实现） ====================
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
                colors = CardDefaults.cardColors(containerColor = Color.White),
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
                        leadingIcon = Icons.Default.Email
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
                        visibilityOffIcon = painterResource(R.drawable.visibility_off)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { onLogin(email, password) },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.Primary
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
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
                        Text("还没有账户？", fontSize = 14.sp, color = Color.Gray)
                        TextButton(onClick = onSwitchToRegister) {
                            Text("立即注册", color = AppColors.Primary, fontSize = 14.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("取消", color = Color.Gray)
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
                colors = CardDefaults.cardColors(containerColor = Color.White),
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
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
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
                        Text("已有账户？", fontSize = 14.sp, color = Color.Gray)
                        TextButton(onClick = onSwitchToLogin) {
                            Text("立即登录", color = AppColors.Primary, fontSize = 14.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("取消", color = Color.Gray)
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
                colors = CardDefaults.cardColors(containerColor = Color.White),
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
                    SmartShoeTextField.Username(
                        value = username,
                        onValueChange = { viewModel?.onEditProfileUsernameChange(it) },
                        label = "用户名",
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = Icons.Default.Person
                    )
                    Spacer(modifier = Modifier.height(12.dp))
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
                            tint = Color.Gray,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "邮箱地址",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = userState.email,
                                fontSize = 16.sp,
                                color = AppColors.OnSurface
                            )
                        }
                    }
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
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { onSave(username, userState.email, newPassword, currentPassword) },
                        enabled = !isLoading && username.isNotBlank() && currentPassword.isNotBlank() && passwordsMatch,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.Primary
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
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
                        Text("取消", color = Color.Gray)
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
                            .height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "退出登录",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("退出登录", color = Color.White)
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
        // 对话框状态
        val showLoginDialog = settingViewModel?.showLoginDialog?.collectAsStateWithLifecycle()?.value ?: false
        val showRegisterDialog = settingViewModel?.showRegisterDialog?.collectAsStateWithLifecycle()?.value ?: false
        val isEditExpanded = settingViewModel?.isEditProfileExpanded?.collectAsStateWithLifecycle()?.value ?: false
        val uploadStatus = settingViewModel?.uploadStatus?.collectAsStateWithLifecycle()?.value ?: UploadStatus.IDLE
        val errorMessage = settingViewModel?.errorMessage?.collectAsStateWithLifecycle()?.value

        // 设置列表展开状态
        var isVersionExpanded by rememberSaveable { mutableStateOf(false) }
        var isHelpExpanded by rememberSaveable { mutableStateOf(false) }
        var isPrivacyExpanded by rememberSaveable { mutableStateOf(false) }

        // 监听认证状态
        LaunchedEffect(authUiState) {
            when (authUiState) {
                is AuthUiState.Success -> settingViewModel?.handleAuthCompleted(success = true)
                is AuthUiState.Error -> settingViewModel?.handleAuthCompleted(success = false)
                else -> {}
            }
        }

        // 显示错误
        errorMessage?.let { message ->
            onShowError?.invoke(message)
            settingViewModel?.clearError()
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

            // 设置列表（复用AboutApp内容）
            item {
                SettingsList(
                    isVersionExpanded = isVersionExpanded,
                    isHelpExpanded = isHelpExpanded,
                    isPrivacyExpanded = isPrivacyExpanded,
                    onVersionExpandedChange = { isVersionExpanded = it },
                    onHelpExpandedChange = { isHelpExpanded = it },
                    onPrivacyExpandedChange = { isPrivacyExpanded = it }
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 清除缓存按钮
            item {
                ClearCacheButton(
                    onClearCache = onClearCache,
                    settingViewModel = settingViewModel
                )
            }
        }

        // 登录对话框
        if (showLoginDialog) {
            LoginDialog(
                onDismiss = { settingViewModel?.hideLoginDialog() },
                onLogin = { email, password ->
                    settingViewModel?.validateLoginForm()?.let { error ->
                        settingViewModel?.showError(error)
                        return@LoginDialog
                    }
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
                        settingViewModel?.showError(error)
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
