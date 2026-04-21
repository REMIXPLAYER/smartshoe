package com.example.smartshoe.ui.screen.main.components

import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartshoe.R
import com.example.smartshoe.ui.component.getDeviceDisplayName
import com.example.smartshoe.ui.theme.AppColors

/**
 * 已连接设备详情头部
 * 展开蓝牙设备列表后显示，展示详细的连接设备信息
 * 左侧显示connect_device图标，右侧显示disconnect_device图标用于断开连接
 */
@Composable
fun ConnectedDeviceHeader(
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
            IconButton(
                onClick = onDisconnect,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.disconnect_device),
                    contentDescription = "断开连接",
                    tint = AppColors.Error,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
