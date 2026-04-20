package com.example.smartshoe.ui.screen.setting.components.quickactions

import android.bluetooth.BluetoothDevice
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smartshoe.R
import com.example.smartshoe.ui.component.CompactDeviceListItem
import com.example.smartshoe.ui.theme.AppColors
import com.example.smartshoe.ui.viewmodel.SettingViewModel
import com.example.smartshoe.ui.viewmodel.UploadStatus
import com.example.smartshoe.util.AnimationDefaults

/**
 * 可展开快捷功能区
 */
@Composable
fun QuickActionsSection(
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
                enter = androidx.compose.animation.expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                DeviceSettingsContent(
                    scannedDevices = scannedDevices,
                    connectedDevice = connectedDevice,
                    onConnectDevice = onConnectDevice,
                    onDisconnectDevice = onDisconnectDevice
                )
            }

            // 体重展开内容
            AnimatedVisibility(
                visible = expandedItem == "weight",
                enter = androidx.compose.animation.expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                WeightSettingsContent(
                    isEditingWeight = isEditingWeight,
                    weightInput = weightInput,
                    userWeight = userWeight,
                    onWeightInputChange = { settingViewModel?.onWeightInputChange(it) },
                    onConfirm = {
                        val weight = settingViewModel?.validateWeightInput()
                        if (weight != null) {
                            onEditWeight(weight)
                            expandedItem = null
                        }
                    },
                    onCancel = {
                        settingViewModel?.cancelEditingWeight()
                        expandedItem = null
                    }
                )
            }

            // 提醒展开内容
            AnimatedVisibility(
                visible = expandedItem == "alert",
                enter = androidx.compose.animation.expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                ReminderSettingsContent(
                    pressureAlertsEnabled = pressureAlertsEnabled,
                    onPressureAlertsChange = onPressureAlertsChange
                )
            }

            // 备份展开内容
            AnimatedVisibility(
                visible = expandedItem == "backup",
                enter = androidx.compose.animation.expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                BackupSettingsContent(
                    isLoggedIn = isLoggedIn,
                    hasData = hasData,
                    uploadStatus = uploadStatus,
                    onBackupClick = onBackupClick
                )
            }
        }
    }
}

/**
 * 快捷按钮组件
 */
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
