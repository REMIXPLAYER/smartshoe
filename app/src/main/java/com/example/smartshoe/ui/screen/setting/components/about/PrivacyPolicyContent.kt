package com.example.smartshoe.ui.screen.setting.components.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartshoe.ui.theme.AppColors

/**
 * 隐私政策内容 - 分段式展示
 */
@Composable
fun PrivacyPolicyContent() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 更新时间标签
        Card(
            colors = CardDefaults.cardColors(containerColor = AppColors.AiModeDeep.copy(alpha = 0.08f)),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Text(
                text = "最后更新：2026年4月",
                fontSize = 11.sp,
                color = AppColors.AiModeDeep,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        // 隐私政策段落
        PrivacySection(
            title = "信息收集",
            content = "我们仅在必要时收集和使用您的个人信息，以提供和改进我们的服务。"
        )

        PrivacySection(
            title = "数据用途",
            content = "我们收集的信息包括蓝牙设备名称、传感器数据和连接状态，这些信息仅用于实时显示压力分布和生成历史数据图表。"
        )

        PrivacySection(
            title = "数据保护",
            content = "我们承诺不会与第三方共享您的个人数据，除非获得您的明确同意或法律要求。"
        )

        Spacer(modifier = Modifier.height(4.dp))

        // 联系信息
        Text(
            text = "如有疑问，请联系开发团队",
            fontSize = 11.sp,
            color = AppColors.PlaceholderText.copy(alpha = 0.7f)
        )
    }
}

/**
 * 隐私政策段落组件
 */
@Composable
fun PrivacySection(title: String, content: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.OnSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = content,
            fontSize = 12.sp,
            color = AppColors.PlaceholderText,
            lineHeight = 18.sp
        )
    }
}
