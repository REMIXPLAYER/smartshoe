package com.example.smartshoe.ui.screen.main.components

import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartshoe.R
import com.example.smartshoe.ui.component.getDeviceDisplayName
import com.example.smartshoe.ui.theme.AppColors
import com.example.smartshoe.ui.theme.AppDimensions
import com.example.smartshoe.ui.theme.AppTypography

/**
 * 已连接设备详情头部
 * 展开蓝牙设备列表后显示，展示详细的连接设备信息
 * 左侧显示connect_device图标，右侧显示disconnect_device图标用于断开连接
 *
 * 布局对齐规则：
 * - 左侧图标与下方 CompactDeviceListItem 的圆形指示器中心对齐
 * - 文字左边距与下方设备列表项的文字左对齐
 */
@Composable
fun ConnectedDeviceHeader(
    device: BluetoothDevice,
    onDisconnect: () -> Unit
) {
    val displayName = remember(device) { getDeviceDisplayName(device) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppDimensions.MediumPadding, vertical = 8.dp),
        color = AppColors.Primary.copy(alpha = 0.08f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = AppDimensions.MediumPadding,
                    vertical = AppDimensions.ContentVerticalPadding
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧：已连接设备指示器（实心圆形，与下方 CompactDeviceListItem 对齐）
            Box(
                modifier = Modifier
                    .size(AppDimensions.IndicatorSize)
                    .clip(CircleShape)
                    .background(AppColors.Primary),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(AppDimensions.SmallPadding)
                        .clip(CircleShape)
                        .background(AppColors.CardBackground)
                )
            }

            Spacer(modifier = Modifier.width(AppDimensions.MediumPadding))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    fontSize = AppTypography.CaptionTextSize,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.Primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = device.address ?: "未知地址",
                    fontSize = AppTypography.SmallTextSize,
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
                    modifier = Modifier.size(AppDimensions.IconSizeStandard)
                )
            }
        }
    }
}
