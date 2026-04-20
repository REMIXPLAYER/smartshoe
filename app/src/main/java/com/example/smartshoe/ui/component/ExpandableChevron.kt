package com.example.smartshoe.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.smartshoe.ui.theme.AppColors

/**
 * 统一展开/收起箭头图标组件
 * 所有可展开卡片统一使用此组件以保持视觉一致性
 *
 * 动画效果：
 * - 收起时：右箭头（→）
 * - 展开时：下箭头（↓）
 *
 * @param isExpanded 是否展开状态
 * @param modifier 修饰符
 * @param size 图标大小
 * @param tint 图标颜色
 */
@Composable
fun ExpandableChevron(
    isExpanded: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = AppColors.OnSurface.copy(alpha = 0.6f)
) {
    // 收起时旋转0度（右箭头），展开时旋转90度（下箭头）
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 90f else 0f,
        label = "chevron_rotation"
    )

    Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = if (isExpanded) "收起" else "展开",
        modifier = modifier
            .rotate(rotation)
            .then(Modifier.size(size)),
        tint = tint
    )
}
