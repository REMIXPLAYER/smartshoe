package com.example.smartshoe.ui.screen.setting.components.about

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartshoe.ui.theme.AppColors
import com.example.smartshoe.ui.theme.AppIcon
import com.example.smartshoe.ui.theme.AppIcons

/**
 * 可展开的应用项组件
 */
@Composable
fun AboutAppItem(
    appIcon: AppIcon,
    title: String,
    subtitle: String,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    iconBackground: Color = AppColors.Primary.copy(alpha = 0.1f),
    iconTint: Color = AppColors.Primary,
    expandedContent: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // 标题行（可点击）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { onExpandedChange(!isExpanded) }
                )
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标容器 - 纯色背景
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = iconBackground,
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                when (appIcon) {
                    is AppIcon.MaterialIcon -> {
                        Icon(
                            imageVector = appIcon.icon,
                            contentDescription = title,
                            tint = iconTint,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    is AppIcon.ResourceIcon -> {
                        Icon(
                            painter = painterResource(id = appIcon.resId),
                            contentDescription = title,
                            tint = iconTint,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = if (isExpanded) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (isExpanded) AppColors.Primary else AppColors.OnSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = AppColors.PlaceholderText
                )
            }

            // 箭头动画
            val arrowRotation by animateFloatAsState(
                targetValue = if (isExpanded) 90f else 0f,
                animationSpec = tween(
                    durationMillis = 250,
                    easing = FastOutSlowInEasing
                ),
                label = "arrow_rotation"
            )

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = if (isExpanded) "收起" else "展开",
                tint = if (isExpanded) AppColors.Primary else AppColors.PlaceholderText,
                modifier = Modifier
                    .size(20.dp)
                    .rotate(arrowRotation)
            )
        }

        // 展开内容
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeIn(
                animationSpec = tween(200)
            ),
            exit = shrinkVertically(
                animationSpec = tween(250, easing = FastOutSlowInEasing)
            ) + fadeOut(
                animationSpec = tween(150)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                expandedContent()
            }
        }
    }
}
