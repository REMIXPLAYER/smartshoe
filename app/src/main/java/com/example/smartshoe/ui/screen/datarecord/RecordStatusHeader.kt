package com.example.smartshoe.ui.screen.datarecord

import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartshoe.ui.component.getDeviceDisplayName
import com.example.smartshoe.ui.theme.AppColors

/**
 * 记录状态头部信息
 */
@Composable
fun RecordStatusHeader(
    connectedDevice: BluetoothDevice?,
    dataPointCount: Int,
    modifier: Modifier = Modifier,
    userWeight: Float
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "实时数据记录",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.Primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(
                                    color = if (connectedDevice != null) AppColors.Success else AppColors.PlaceholderText,
                                    shape = CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = if (connectedDevice != null) {
                                "${getDeviceDisplayName(connectedDevice)}已连接，自动记录中"
                            } else {
                                "设备未连接"
                            },
                            fontSize = 14.sp,
                            color = if (connectedDevice != null) AppColors.Success else AppColors.PlaceholderText,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    if (userWeight > 0f) {
                        Text(
                            text = "体重: ${userWeight}kg",
                            fontSize = 12.sp,
                            color = AppColors.PlaceholderText,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    Text(
                        text = "数据点数: $dataPointCount",
                        fontSize = 12.sp,
                        color = AppColors.PlaceholderText
                    )
                }
            }
        }
    }
}
