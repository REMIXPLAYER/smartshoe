package com.example.smartshoe.ui.screen.main.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartshoe.ui.screen.main.SnackbarType
import com.example.smartshoe.ui.theme.AppColors

/**
 * 自定义 Snackbar 组件
 * 符合应用设计规范，使用主题色和圆角设计
 */
@Composable
fun CustomSnackbar(
    message: String,
    modifier: Modifier = Modifier,
    type: SnackbarType = SnackbarType.Info
) {
    val (backgroundColor, iconColor, icon) = when (type) {
        SnackbarType.Success -> Triple(
            AppColors.Success.copy(alpha = 0.9f),
            Color.White,
            Icons.Default.CheckCircle
        )
        SnackbarType.Error -> Triple(
            AppColors.Error.copy(alpha = 0.9f),
            Color.White,
            Icons.Default.Close
        )
        SnackbarType.Warning -> Triple(
            AppColors.Warning.copy(alpha = 0.9f),
            AppColors.TextDark,
            Icons.Default.Warning
        )
        SnackbarType.Info -> Triple(
            AppColors.Primary.copy(alpha = 0.9f),
            Color.White,
            Icons.Default.Info
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                color = iconColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
