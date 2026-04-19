package com.example.smartshoe.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.smartshoe.ui.theme.AppColors
import kotlinx.coroutines.delay

/**
 * 刷新按钮状态机
 * 管理按钮的生命周期状态
 */
enum class RefreshButtonState {
    IDLE,       // 空闲状态 - 灰色，静止
    CLICKED,    // 点击瞬间 - Primary色，缩放动画
    SCANNING,   // 扫描中 - Primary色，持续旋转
    COMPLETED   // 完成 - 回到空闲状态
}

/**
 * 刷新按钮组件
 *
 * 动画效果：
 * 1. 点击瞬间：立即变为 Primary 色 + 轻微缩小（按下反馈）
 * 2. 扫描中：持续旋转（360度循环）
 * 3. 扫描结束：平滑过渡回灰色，保持当前角度直到停止
 *
 * @param isScanning 是否正在扫描（由外部ViewModel控制）
 * @param onRefresh 刷新回调
 * @param modifier 修饰符
 * @param size 按钮大小
 * @param iconSize 图标大小
 */
@Composable
fun RefreshButton(
    isScanning: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    iconSize: Dp = 18.dp
) {
    // 内部状态机
    var buttonState by remember { mutableStateOf(RefreshButtonState.IDLE) }

    // 监听外部扫描状态变化
    LaunchedEffect(isScanning) {
        when {
            isScanning && buttonState == RefreshButtonState.IDLE -> {
                // 外部开始扫描，直接进入扫描状态
                buttonState = RefreshButtonState.SCANNING
            }
            isScanning && buttonState == RefreshButtonState.CLICKED -> {
                // 外部确认开始扫描，进入扫描状态
                buttonState = RefreshButtonState.SCANNING
            }
            !isScanning && buttonState == RefreshButtonState.SCANNING -> {
                // 扫描结束，延迟后回到空闲状态（保持视觉连续性）
                delay(300)
                buttonState = RefreshButtonState.IDLE
            }
            !isScanning && buttonState == RefreshButtonState.CLICKED -> {
                // 如果外部状态变为 false 但按钮还在 CLICKED 状态
                // 说明扫描被取消或失败，回到空闲状态
                delay(100)
                buttonState = RefreshButtonState.IDLE
            }
        }
    }

    // 超时保护：如果点击后 500ms 内没有进入 SCANNING，自动回退到 IDLE
    LaunchedEffect(buttonState) {
        if (buttonState == RefreshButtonState.CLICKED) {
            delay(500)
            // 500ms 后检查，如果还在 CLICKED 状态（没有进入 SCANNING），则回退
            if (buttonState == RefreshButtonState.CLICKED) {
                buttonState = RefreshButtonState.IDLE
            }
        }
    }
    
    // 颜色动画 - 平滑过渡
    val targetColor = when (buttonState) {
        RefreshButtonState.IDLE -> AppColors.OnSurface.copy(alpha = 0.5f)
        RefreshButtonState.CLICKED, RefreshButtonState.SCANNING -> AppColors.Primary
        RefreshButtonState.COMPLETED -> AppColors.OnSurface.copy(alpha = 0.5f)
    }
    
    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 200),
        label = "refresh_color"
    )
    
    // 缩放动画 - 点击反馈
    val targetScale = when (buttonState) {
        RefreshButtonState.CLICKED -> 0.85f  // 按下时缩小
        else -> 1f
    }
    
    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(durationMillis = 150),
        label = "refresh_scale"
    )
    
    // 旋转动画 - 仅在扫描时启用
    val infiniteTransition = rememberInfiniteTransition(label = "scanning_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "refresh_rotation"
    )
    
    // 最终旋转角度
    val finalRotation = when (buttonState) {
        RefreshButtonState.SCANNING -> rotation
        else -> 0f
    }
    
    Box(
        modifier = modifier
            .size(size)
            .scale(animatedScale)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = {
                    if (buttonState == RefreshButtonState.IDLE) {
                        buttonState = RefreshButtonState.CLICKED
                        onRefresh()
                    }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = "刷新",
            modifier = Modifier
                .size(iconSize)
                .rotate(finalRotation),
            tint = animatedColor
        )
    }
}
