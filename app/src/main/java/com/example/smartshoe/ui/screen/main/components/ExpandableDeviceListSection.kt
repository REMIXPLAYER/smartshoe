package com.example.smartshoe.ui.screen.main.components

import android.bluetooth.BluetoothDevice
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartshoe.R
import com.example.smartshoe.ui.component.CompactDeviceListItem
import com.example.smartshoe.ui.component.ExpandableChevron
import com.example.smartshoe.ui.component.RefreshButton
import com.example.smartshoe.ui.theme.AppColors
import com.example.smartshoe.util.AnimationDefaults

/**
 * 设备列表区域组件
 * 显示扫描到的蓝牙设备列表，支持滚动查看，包含刷新按钮
 * 展开时悬浮在其他组件上方
 * 样式与 AI模式选择器 保持一致（方案A：现代简洁风）
 */
@Composable
fun ExpandableDeviceListSection(
    devices: List<BluetoothDevice>,
    connectedDevice: BluetoothDevice?,
    onConnectDevice: (BluetoothDevice) -> Unit,
    onDisconnectDevice: () -> Unit,
    onScanDevices: () -> Unit,
    isScanning: Boolean,
    modifier: Modifier = Modifier,
    isConnecting: Boolean = false,
    connectingDeviceAddress: String? = null
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(
                    min = 60.dp,
                    max = if (isExpanded) 520.dp else 60.dp
                )
        ) {
            // 标题栏 - 统一60.dp高度，解决双行信息挤压问题
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { isExpanded = !isExpanded },
                color = AppColors.Surface,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 蓝牙图标
                    Icon(
                        painter = painterResource(R.drawable.bluetooth),
                        contentDescription = "蓝牙设备",
                        tint = AppColors.Primary,
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    // 主标题 - 统一16.sp SemiBold Primary色
                    Text(
                        text = "蓝牙设备",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.Primary,
                        modifier = Modifier.weight(1f)
                    )

                    // 右侧区域：连接状态 + 刷新按钮 + 展开图标
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 连接状态标签（单行显示，避免挤压主标题）
                        if (connectedDevice != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                // 状态指示点
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(AppColors.Success)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "已连接",
                                    fontSize = 12.sp,
                                    color = AppColors.Success,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } else {
                            Text(
                                text = "未连接",
                                fontSize = 12.sp,
                                color = AppColors.OnSurface.copy(alpha = 0.5f)
                            )
                        }

                        // 刷新按钮 - 使用新的RefreshButton组件
                        RefreshButton(
                            isScanning = isScanning,
                            onRefresh = onScanDevices,
                            size = 32.dp,
                            iconSize = 18.dp
                        )

                        // 统一展开图标（使用ExpandableChevron）
                        ExpandableChevron(
                            isExpanded = isExpanded,
                            size = 24.dp,
                            tint = AppColors.OnSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(
                    animationSpec = AnimationDefaults.expandTween
                ),
                exit = shrinkVertically(
                    animationSpec = AnimationDefaults.shrinkTween
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (devices.isEmpty()) {
                        // 空状态优化
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "未找到设备",
                                    fontSize = 14.sp,
                                    color = AppColors.OnSurface.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = "点击刷新按钮开始扫描",
                                    fontSize = 12.sp,
                                    color = AppColors.OnSurface.copy(alpha = 0.4f)
                                )
                            }
                        }
                    } else {
                        // 已连接设备详情头部（展开后显示）
                        if (connectedDevice != null) {
                            ConnectedDeviceHeader(
                                device = connectedDevice,
                                onDisconnect = onDisconnectDevice
                            )
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 380.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            // 过滤掉已连接的设备，避免重复显示
                            val displayDevices = devices.filter { it.address != connectedDevice?.address }
                            
                            items(displayDevices) { device ->
                                val deviceAddress = device.address
                                val isThisDeviceConnecting = isConnecting && connectingDeviceAddress == deviceAddress
                                
                                CompactDeviceListItem(
                                    device = device,
                                    isConnected = false, // 这里都是未连接的设备
                                    isConnecting = isThisDeviceConnecting,
                                    isAnyDeviceConnecting = isConnecting,
                                    onConnect = { onConnectDevice(device) },
                                    onDisconnect = onDisconnectDevice
                                )
                                if (device != displayDevices.last()) {
                                    HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
