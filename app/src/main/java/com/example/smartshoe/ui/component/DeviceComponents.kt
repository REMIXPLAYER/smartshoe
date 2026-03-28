package com.example.smartshoe.ui.component

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartshoe.R
import com.example.smartshoe.ui.theme.AppColors

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
            Color(0xFF4CAF50) else Color.Gray
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
                color = AppColors.OnSurface
            )
            Text(
                text = subtitleText,
                fontSize = 14.sp,
                color = subtitleColor
            )
        }

        ExpandableArrowIcon(
            isExpanded = isExpanded,
            tint = Color.Gray,
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
                color = Color.Gray,
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
 * 设备设置项组件
 */
@Composable
fun DeviceSettingItem(
    device: BluetoothDevice,
    isConnected: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    val containerColor = remember(isConnected) {
        if (isConnected) Color(0xFFE8F5E8) else Color.White
    }

    val iconTint = remember(isConnected) {
        if (isConnected) Color(0xFF4CAF50) else AppColors.Primary
    }

    val textColor = remember(isConnected) {
        if (isConnected) Color(0xFF2E7D32) else AppColors.OnSurface
    }

    val buttonText = remember(isConnected) {
        if (isConnected) "断开" else "连接"
    }

    val buttonColor = remember(isConnected) {
        if (isConnected) Color(0xFFF44336) else AppColors.Primary
    }

    val buttonWidth = remember(isConnected) {
        if (isConnected) 70.dp else 60.dp
    }

    val deviceName = remember(device.name, device.address) {
        getDeviceDisplayName(device)
    }

    val deviceAddress = remember(device.address) {
        device.address ?: "未知地址"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(containerColor)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
            Icon(
                painter = painterResource(R.drawable.bluetooth),
                contentDescription = "蓝牙设备",
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = deviceName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = deviceAddress,
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }

            Button(
                onClick = {
                    if (isConnected) {
                        onDisconnect()
                    } else {
                        onConnect()
                    }
                },
                modifier = Modifier
                    .height(32.dp)
                    .width(buttonWidth),
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonColor
                ),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Text(
                    text = buttonText,
                    fontSize = 12.sp,
                    color = Color.White
                )
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
