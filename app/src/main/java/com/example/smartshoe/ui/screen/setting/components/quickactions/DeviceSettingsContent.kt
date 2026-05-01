package com.example.smartshoe.ui.screen.setting.components.quickactions

import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartshoe.ui.component.CompactDeviceListItem
import com.example.smartshoe.ui.theme.AppColors

/**
 * 设备设置内容
 */
@Composable
fun DeviceSettingsContent(
    scannedDevices: List<BluetoothDevice>,
    connectedDevice: BluetoothDevice?,
    onConnectDevice: (BluetoothDevice) -> Unit,
    onDisconnectDevice: () -> Unit
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
                color = AppColors.PlaceholderText,
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
                    useNewStyle = true,  // 使用新样式：左侧connect_device图标，右侧disconnect_device图标
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
