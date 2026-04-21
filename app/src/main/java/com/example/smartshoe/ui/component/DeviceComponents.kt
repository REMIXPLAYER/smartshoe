package com.example.smartshoe.ui.component

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.graphicsLayer
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
 * 
 * @param isConnecting 是否正在连接（用于显示连接动画）
 * @param isAnyDeviceConnecting 是否有其他设备正在连接（用于禁用点击）
 * @param useNewStyle 是否使用新样式（设置页面用），true时左侧显示connect_device图标，右侧显示disconnect_device图标
 */
@Composable
fun CompactDeviceListItem(
    device: BluetoothDevice,
    isConnected: Boolean,
    isConnecting: Boolean = false,
    isAnyDeviceConnecting: Boolean = false,
    useNewStyle: Boolean = false,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val displayName = getDeviceDisplayName(device)
    val address = device.address ?: "未知地址"
    
    // 点击防抖动状态
    var isClickEnabled by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()
    
    // 连接动画 - 脉冲效果
    val infiniteTransition = rememberInfiniteTransition(label = "connecting_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = isClickEnabled && !isConnecting && !isAnyDeviceConnecting,
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
        color = when {
            isConnected -> AppColors.Primary.copy(alpha = 0.1f)
            isConnecting -> AppColors.Primary.copy(alpha = 0.05f)
            else -> Color.Transparent
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧：连接状态指示器或图标
            if (useNewStyle && isConnected) {
                // 新样式：使用 connect_device 图标
                Icon(
                    painter = painterResource(R.drawable.connect_device),
                    contentDescription = "已连接设备",
                    tint = AppColors.Primary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                // 旧样式：圆形指示器
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isConnected -> AppColors.Primary
                                isConnecting -> AppColors.Primary.copy(alpha = pulseAlpha)
                                else -> Color.Transparent
                            }
                        )
                        .border(
                            width = 2.dp,
                            color = when {
                                isConnected -> AppColors.Primary
                                isConnecting -> AppColors.Primary.copy(alpha = pulseAlpha)
                                else -> AppColors.OnSurface.copy(alpha = 0.3f)
                            },
                            shape = CircleShape
                        )
                        .graphicsLayer {
                            if (isConnecting) {
                                scaleX = pulseScale
                                scaleY = pulseScale
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isConnected -> {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                        }
                        isConnecting -> {
                            // 连接中显示旋转的进度指示器
                            CircularProgressIndicator(
                                modifier = Modifier.size(10.dp),
                                color = AppColors.Primary,
                                strokeWidth = 2.dp
                            )
                        }
                    }
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
                    fontWeight = when {
                        isConnected || isConnecting -> FontWeight.Medium
                        else -> FontWeight.Normal
                    },
                    color = when {
                        isConnected -> AppColors.Primary
                        isConnecting -> AppColors.Primary
                        else -> AppColors.OnSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (isConnecting) "连接中..." else address,
                    fontSize = 12.sp,
                    color = when {
                        isConnecting -> AppColors.Primary.copy(alpha = 0.8f)
                        else -> AppColors.OnSurface.copy(alpha = 0.6f)
                    }
                )
            }
            
            // 右侧：连接状态图标或断开按钮
            when {
                useNewStyle && isConnected -> {
                    // 新样式：使用 disconnect_device 图标作为断开按钮
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
                isConnected -> {
                    // 旧样式：蓝牙图标
                    Icon(
                        painter = painterResource(R.drawable.bluetooth),
                        contentDescription = "已连接",
                        tint = AppColors.Success,
                        modifier = Modifier.size(20.dp)
                    )
                }
                isConnecting -> {
                    // 连接中不显示额外图标，避免视觉混乱
                }
            }
        }
    }
}
