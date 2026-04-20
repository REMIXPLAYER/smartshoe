package com.example.smartshoe.ui.component

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.example.smartshoe.ui.theme.AppColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 设备相关组件集合
 * 合并：DeviceListComponents.kt + DeviceSettingItem.kt
 */

/**
 * 设备列表头部组件
 */
@Composable
fun DeviceListHeader(
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    connectedDevice: BluetoothDevice?
) {
    val subtitleText = remember(connectedDevice?.address) {
        if (connectedDevice != null && connectedDevice.address != null) {
            "已连接：${getDeviceDisplayName(connectedDevice)}"
        } else "未连接设备"
    }

    val subtitleColor = remember(connectedDevice?.address) {
        if (connectedDevice != null && connectedDevice.address != null)
            AppColors.Success else AppColors.DarkGray
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onExpandedChange(!isExpanded) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.bluetooth),
            contentDescription = "蓝牙设备",
            tint = AppColors.Primary,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                "蓝牙设备管理",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = AppColors.OnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitleText,
                fontSize = 14.sp,
                color = subtitleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        ExpandableArrowIcon(
            isExpanded = isExpanded,
            tint = AppColors.DarkGray,
            size = 20.dp,
            useGraphicsLayer = false
        )
    }
}

/**
 * 设备列表内容组件
 */
@Composable
fun DeviceListContent(
    scannedDevices: List<BluetoothDevice>,
    connectedDevice: BluetoothDevice?,
    onConnectDevice: (BluetoothDevice) -> Unit,
    onDisconnectDevice: () -> Unit,
    deviceSettingItem: @Composable (BluetoothDevice, Boolean, () -> Unit, () -> Unit) -> Unit
) {
    Column {
        if (scannedDevices.isEmpty()) {
            Text(
                "未找到设备，请在主页点击扫描",
                fontSize = 12.sp,
                color = AppColors.DarkGray,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        } else {
            val connectedAddress = connectedDevice?.address

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                scannedDevices.forEach { device ->
                    key(device.address ?: device.hashCode()) {
                        if (device.address != null) {
                            val isDeviceConnected = remember(connectedAddress, device.address) {
                                connectedAddress == device.address
                            }

                            deviceSettingItem(
                                device,
                                isDeviceConnected,
                                {
                                    if (device.address != null) {
                                        onConnectDevice(device)
                                    }
                                },
                                onDisconnectDevice
                            )
                        }
                    }
                }
            }
        }

    }
}

/**
 * 获取设备显示名称
 * 优先使用设备名称，如果名称为空则使用MAC地址
 * 作为公共函数，供其他模块使用
 */
@SuppressLint("MissingPermission")
fun getDeviceDisplayName(device: BluetoothDevice): String {
    return when {
        !device.name.isNullOrBlank() -> device.name!!
        !device.address.isNullOrBlank() -> formatMacAddress(device.address!!)
        else -> "未知设备"
    }
}

/**
 * 格式化MAC地址，使其更易读
 * 例如：04:34:C3:05:7E:45 -> 设备 04:34:C3:05:7E:45
 */
fun formatMacAddress(macAddress: String): String {
    return "设备 $macAddress"
}

/**
 * 紧凑设备列表项 - 使用与 ModeOptionItem 一致的样式
 * 点击整个项进行连接/断开操作
 * 选中（连接）时显示 Success 色背景，未选中（断开）时透明背景
 * 添加点击防抖动，防止重复点击导致内存泄漏
 */
@Composable
fun CompactDeviceListItem(
    device: BluetoothDevice,
    isConnected: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val displayName = getDeviceDisplayName(device)
    val address = device.address ?: "未知地址"
    
    // 点击防抖动状态
    var isClickEnabled by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = isClickEnabled,
                onClick = {
                    // 防抖动：禁用点击 500ms
                    isClickEnabled = false
                    coroutineScope.launch {
                        delay(500)
                        isClickEnabled = true
                    }
                    
                    if (isConnected) {
                        onDisconnect()
                    } else {
                        onConnect()
                    }
                }
            ),
        shape = RoundedCornerShape(8.dp),
        color = if (isConnected) AppColors.Primary.copy(alpha = 0.1f) else Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧：连接状态指示器（圆形）
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(
                        if (isConnected) AppColors.Primary else Color.Transparent
                    )
                    .border(
                        width = 2.dp,
                        color = if (isConnected) AppColors.Primary else AppColors.OnSurface.copy(alpha = 0.3f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isConnected) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 中间：设备信息（名称 + 地址）
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = displayName,
                    fontSize = 14.sp,
                    fontWeight = if (isConnected) FontWeight.Medium else FontWeight.Normal,
                    color = if (isConnected) AppColors.Primary else AppColors.OnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = address,
                    fontSize = 12.sp,
                    color = AppColors.OnSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}
