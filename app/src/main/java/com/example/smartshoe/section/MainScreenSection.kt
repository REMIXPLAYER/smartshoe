package com.example.smartshoe.section

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartshoe.R
import com.example.smartshoe.UIConstants

/**
 * 获取设备显示名称的辅助函数
 * 优先使用设备名称，如果名称为空则使用MAC地址
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

object MainScreenSection {
    /**
     * 设备列表区域组件
     * 显示扫描到的蓝牙设备列表，支持滚动查看，包含扫描按钮
     * 展开时悬浮在其他组件上方
     */
    @Composable
     fun ExpandableDeviceListSection(
        devices: List<BluetoothDevice>,
        connectedDevice: BluetoothDevice?,
        onConnectDevice: (BluetoothDevice) -> Unit,
        onDisconnectDevice: () -> Unit,
        onScanDevices: () -> Unit, // 新增扫描回调
        modifier: Modifier = Modifier
    ) {
        var isExpanded by remember { mutableStateOf(false) }

        // 优化图标旋转动画
        val arrowRotation by animateFloatAsState(
            targetValue = if (isExpanded) 90f else 0f,
            animationSpec = tween(durationMillis = 300),
            label = "arrow_rotation"
        )

        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = UIConstants.SurfaceColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(
                        min = 60.dp, // 固定最小高度
                        max = if (isExpanded) 350.dp else 60.dp // 展开时增加高度
                    )
            ) {
                // 标题栏（始终显示）- 使用固定高度避免图标移动
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp) // 固定标题栏高度
                        .clickable { isExpanded = !isExpanded }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.bluetooth),
                            contentDescription = "蓝牙设备",
                            tint = UIConstants.PrimaryColor,
                            modifier = Modifier.size(24.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "蓝牙设备 (${devices.size})",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = UIConstants.TextColorDark
                            )

                            // 只有当有连接设备时才显示连接状态
                            if (connectedDevice != null) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "已连接：${getDeviceDisplayName(connectedDevice)}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF4CAF50)
                                )
                            }
                        }

                        // 连接按钮
                        Button(
                            onClick = onScanDevices,
                            modifier = Modifier
                                .height(36.dp)
                                .padding(end = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = UIConstants.PrimaryColor
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "扫描",
                                fontSize = 13.sp,
                                color = Color.White
                            )
                        }

                        // 使用旋转箭头并添加平滑动画
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = if (isExpanded) "收起" else "展开",
                            tint = Color.Gray,
                            modifier = Modifier
                                .size(24.dp)
                                .graphicsLayer {
                                    rotationZ = arrowRotation
                                }
                        )
                    }
                }

                // 设备列表（展开时显示）
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically(
                        animationSpec = tween(durationMillis = 250)
                    ),
                    exit = shrinkVertically(
                        animationSpec = tween(durationMillis = 200)
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (devices.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(80.dp), // 固定空状态高度
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "未找到设备，请点击扫描按钮",
                                    fontSize = 14.sp,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 200.dp), // 限制最大高度
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                items(devices) { device ->
                                    CompactDeviceListItem(
                                        device = device,
                                        isConnected = connectedDevice?.address == device.address,
                                        onConnect = { onConnectDevice(device) },
                                        onDisconnect = onDisconnectDevice
                                    )
                                    HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
                                }
                            }
                        }

                        // 连接状态指示器 - 只在有连接设备且列表展开时显示
                        if (connectedDevice != null && isExpanded) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(Color.Green, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        "设备已连接",
                                        fontSize = 14.sp,
                                        color = Color.Green,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 紧凑型设备列表项
     */
    @Composable
     fun CompactDeviceListItem(
        device: BluetoothDevice,
        isConnected: Boolean,
        onConnect: () -> Unit,
        onDisconnect: () -> Unit
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp) // 固定高度
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 设备信息
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = getDeviceDisplayName(device),
                        color = if (isConnected) Color(0xFF2E7D32) else UIConstants.TextColorDark,
                        fontSize = 16.sp,
                        fontWeight = if (isConnected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isConnected) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = device.address ?: "未知地址",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // 连接/断开按钮
                Button(
                    onClick = {
                        if (isConnected) {
                            onDisconnect()
                        } else {
                            onConnect()
                        }
                    },
                    modifier = Modifier
                        .height(36.dp)
                        .width(if (isConnected) 80.dp else 80.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isConnected) Color(0xFFF44336) else UIConstants.PrimaryColor
                    )
                ) {
                    Text(
                        text = if (isConnected) "断开" else "连接",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

}