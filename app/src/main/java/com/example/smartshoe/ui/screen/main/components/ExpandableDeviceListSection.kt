package com.example.smartshoe.ui.screen.main.components

import android.bluetooth.BluetoothDevice
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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

    val displayDevices = remember(devices, connectedDevice) {
        devices.filter { it.address != connectedDevice?.address }
    }

    var revealedCount by remember { mutableStateOf(0) }
    LaunchedEffect(isExpanded) {
        if (!isExpanded) {
            revealedCount = 0
            return@LaunchedEffect
        }
        if (revealedCount == 0) {
            revealedCount = 1
        }
        while (true) {
            kotlinx.coroutines.delay(100)
            revealedCount++
        }
    }

    // 展开内容固定高度，确保 expandVertically 动画目标值恒定
    // revealedCount 递增只影响内部 LazyColumn 滚动，不改变外层容器高度
    val FIXED_CONTENT_HEIGHT = 420.dp

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 60.dp, max = 520.dp)
        ) {
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
                    Icon(
                        painter = painterResource(R.drawable.bluetooth),
                        contentDescription = "蓝牙设备",
                        tint = AppColors.Primary,
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "蓝牙设备",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.Primary,
                        modifier = Modifier.weight(1f)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (connectedDevice != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
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

                        RefreshButton(
                            isScanning = isScanning,
                            onRefresh = onScanDevices,
                            size = 32.dp,
                            iconSize = 18.dp
                        )

                        ExpandableChevron(
                            isExpanded = isExpanded,
                            size = 24.dp,
                            tint = AppColors.OnSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // 展开动画：列表整体滑动展开，设备项淡入出现
            // 内容固定高度 400dp，expandVertically 目标恒定不抽搐
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                if (displayDevices.isEmpty() && connectedDevice == null) {
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
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(FIXED_CONTENT_HEIGHT)
                    ) {
                        if (connectedDevice != null) {
                            ConnectedDeviceHeader(
                                device = connectedDevice,
                                onDisconnect = onDisconnectDevice
                            )
                        }

                        if (displayDevices.isNotEmpty()) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                itemsIndexed(
                                    items = displayDevices.take(revealedCount),
                                    key = { _, device -> device.address ?: device.hashCode().toString() }
                                ) { _, device ->
                                    AnimatedVisibility(
                                        visible = true,
                                        enter = fadeIn(animationSpec = tween(durationMillis = 500))
                                    ) {
                                        CompactDeviceListItem(
                                            device = device,
                                            isConnected = false,
                                            isConnecting = isConnecting && connectingDeviceAddress == device.address,
                                            isAnyDeviceConnecting = isConnecting,
                                            onConnect = { onConnectDevice(device) },
                                            onDisconnect = onDisconnectDevice
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
