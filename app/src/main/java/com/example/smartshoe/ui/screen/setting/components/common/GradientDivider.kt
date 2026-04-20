package com.example.smartshoe.ui.screen.setting.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.smartshoe.ui.theme.AppColors

/**
 * 渐变分割线
 */
@Composable
fun GradientDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(1.dp)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        AppColors.LightGray.copy(alpha = 0.6f),
                        AppColors.LightGray.copy(alpha = 0.6f),
                        Color.Transparent
                    ),
                    startX = 0f,
                    endX = 1000f
                )
            )
    )
}
