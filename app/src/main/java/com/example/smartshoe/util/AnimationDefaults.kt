package com.example.smartshoe.util

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.IntSize

/**
 * 动画默认配置
 * 统一应用内所有动画的时长和缓动函数
 */
object AnimationDefaults {

    // 标准动画时长
    const val DURATION_SHORT = 150
    const val DURATION_NORMAL = 200
    const val DURATION_MEDIUM = 250
    const val DURATION_LONG = 300

    // 展开/收起动画 - 用于 expandVertically/shrinkVertically
    val expandTween: FiniteAnimationSpec<IntSize> = tween(
        durationMillis = DURATION_MEDIUM,
        easing = androidx.compose.animation.core.FastOutSlowInEasing
    )

    val shrinkTween: FiniteAnimationSpec<IntSize> = tween(
        durationMillis = DURATION_NORMAL,
        easing = androidx.compose.animation.core.FastOutSlowInEasing
    )

    // 淡入淡出动画 - 用于 fadeIn/fadeOut
    val fadeInTween = tween<Float>(
        durationMillis = DURATION_MEDIUM,
        easing = androidx.compose.animation.core.LinearEasing
    )

    val fadeOutTween = tween<Float>(
        durationMillis = DURATION_NORMAL,
        easing = androidx.compose.animation.core.LinearEasing
    )
}
