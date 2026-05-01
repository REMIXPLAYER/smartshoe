package com.example.smartshoe.ui.screen.setting.components.header

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartshoe.ui.theme.AppColors
import com.example.smartshoe.ui.viewmodel.UploadStatus

/**
 * 统计卡片组件
 */
@Composable
fun StatsCard(
    isConnected: Boolean,
    weight: Float,
    uploadStatus: UploadStatus,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(90.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.Background),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 设备状态
            StatItem(
                icon = com.example.smartshoe.R.drawable.bluetooth,
                label = "设备状态",
                value = if (isConnected) "已连接" else "未连接",
                indicatorColor = if (isConnected) AppColors.Primary else AppColors.PlaceholderText,
                isActive = isConnected
            )

            // 分隔线
            StatsDivider()

            // 体重数据
            StatItem(
                icon = com.example.smartshoe.R.drawable.man,
                label = "体重数据",
                value = if (weight > 0) "${weight}kg" else "未设置",
                indicatorColor = if (weight > 0) AppColors.Primary else AppColors.PlaceholderText,
                isActive = weight > 0
            )

            // 分隔线
            StatsDivider()

            // 备份状态
            val (backupText, backupColor) = when (uploadStatus) {
                UploadStatus.UPLOADING -> "上传中" to AppColors.Primary
                UploadStatus.SUCCESS -> "已备份" to AppColors.Primary
                UploadStatus.FAILED -> "失败" to AppColors.Error
                else -> "未备份" to AppColors.PlaceholderText
            }
            StatItem(
                icon = com.example.smartshoe.R.drawable.cloud,
                label = "云端备份",
                value = backupText,
                indicatorColor = backupColor,
                isActive = uploadStatus == UploadStatus.SUCCESS
            )
        }
    }
}

/**
 * 统计项组件
 */
@Composable
fun StatItem(
    icon: Int,
    label: String,
    value: String,
    indicatorColor: Color,
    isActive: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        // 图标带背景圆圈
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = if (isActive) indicatorColor.copy(alpha = 0.1f) else AppColors.Background,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = label,
                tint = if (isActive) indicatorColor else AppColors.PlaceholderText,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (isActive) indicatorColor else AppColors.PlaceholderText
        )
    }
}

/**
 * 统计分隔线
 */
@Composable
fun StatsDivider() {
    HorizontalDivider(
        modifier = Modifier
            .height(50.dp)
            .width(1.dp),
        color = AppColors.LightGray
    )
}
