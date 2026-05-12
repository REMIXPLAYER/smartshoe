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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartshoe.ui.component.CompactDeviceListItem
import com.example.smartshoe.ui.theme.AppColors

@Composable
fun DeviceSettingsContent(
    connectedDevice: BluetoothDevice?,
    onDisconnectDevice: () -> Unit
) {
    Column {
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = AppColors.LightGray.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(12.dp))

        if (connectedDevice != null) {
            CompactDeviceListItem(
                device = connectedDevice,
                isConnected = true,
                useNewStyle = true,
                onConnect = { },
                onDisconnect = onDisconnectDevice
            )
        } else {
            Text(
                text = "暂无可用设备，请返回首页选择设备连接",
                fontSize = 14.sp,
                color = AppColors.PlaceholderText,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}