package com.example.smartshoe.ui.screen.setting.components.about

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartshoe.ui.theme.AppColors

/**
 * 使用帮助内容 - 步骤式引导
 */
@Composable
fun HelpGuideContent() {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // 步骤列表
        StepItem(number = 1, text = "点击「设备」按钮扫描并连接智能鞋垫")
        Spacer(modifier = Modifier.height(12.dp))
        StepItem(number = 2, text = "连接成功后查看实时压力分布图")
        Spacer(modifier = Modifier.height(12.dp))
        StepItem(number = 3, text = "在「体重」中设置您的体重数据")
        Spacer(modifier = Modifier.height(12.dp))
        StepItem(number = 4, text = "开启「提醒」功能获得压力异常通知")
        Spacer(modifier = Modifier.height(12.dp))
        StepItem(number = 5, text = "使用「备份」功能将数据上传云端")

        Spacer(modifier = Modifier.height(12.dp))

        // 提示卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AppColors.Success.copy(alpha = 0.08f)),
            shape = RoundedCornerShape(10.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 图标容器 - 24dp与步骤数字圆圈大小一致，确保视觉对齐
                Box(
                    modifier = Modifier.size(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = AppColors.Success,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "首次使用请确保蓝牙已开启，并将设备靠近手机",
                    fontSize = 12.sp,
                    color = AppColors.Success.copy(alpha = 0.9f),
                    lineHeight = 18.sp
                )
            }
        }
    }
}

/**
 * 步骤项组件 - 带数字圆圈的步骤指示
 */
@Composable
fun StepItem(number: Int, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 数字圆圈 - 24dp大小，通过paddingLeft与外部44dp图标容器对齐
        // 计算：外部图标容器44dp，圆圈24dp，需要 (44-24)/2 = 10dp 的padding
        Box(
            modifier = Modifier
                .padding(start = 10.dp)
                .size(24.dp)
                .background(AppColors.Primary.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number.toString(),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.Primary
            )
        }
        Spacer(modifier = Modifier.width(22.dp)) // 12dp + 10dp补偿padding
        Text(
            text = text,
            fontSize = 13.sp,
            color = AppColors.OnSurface,
            lineHeight = 20.sp,
            modifier = Modifier.weight(1f)
        )
    }
}
