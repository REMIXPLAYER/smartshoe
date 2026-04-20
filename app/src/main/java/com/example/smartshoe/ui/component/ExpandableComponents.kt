package com.example.smartshoe.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.smartshoe.ui.theme.AppColors
import com.example.smartshoe.util.AnimationDefaults

/**
 * 可复用的展开/收起箭头图标
 */
@Composable
fun ExpandableArrowIcon(
    isExpanded: Boolean,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    tint: Color = AppColors.DarkGray,
    size: Dp = 20.dp,
    rotationDegrees: Float = 90f,
    useGraphicsLayer: Boolean = false
) {
    val actualContentDescription = contentDescription
        ?: if (isExpanded) "收起" else "展开"

    if (useGraphicsLayer) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = actualContentDescription,
            tint = tint,
            modifier = modifier
                .size(size)
                .graphicsLayer {
                    rotationZ = if (isExpanded) rotationDegrees else 0f
                }
        )
    } else {
        val rotation by animateFloatAsState(
            targetValue = if (isExpanded) rotationDegrees else 0f,
            animationSpec = tween(
                durationMillis = AnimationDefaults.DURATION_SHORT,
                easing = androidx.compose.animation.core.FastOutSlowInEasing
            ),
            label = "arrow_rotation"
        )

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = actualContentDescription,
            tint = tint,
            modifier = modifier
                .size(size)
                .rotate(rotation)
        )
    }
}
