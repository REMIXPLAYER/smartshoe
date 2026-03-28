package com.example.smartshoe.util

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.IntSize

/**
 * 动画默认配置
 * 统一应用内所有动画的时长和缓动函数
 * 
 * 重构：合并AnimationPerformanceConfig功能，提供高性能模式支持
 */
object AnimationDefaults {

    // 标准动画时长
    const val DURATION_INSTANT = 100
    const val DURATION_SHORT = 150
    const val DURATION_NORMAL = 200
    const val DURATION_MEDIUM = 250
    const val DURATION_LONG = 300

    // 是否启用高性能模式（减少动画复杂度）
    var isHighPerformanceMode: Boolean = false
        set(value) {
            field = value
            updateAnimationSpecs()
        }

    // 当前使用的动画时长
    val currentExpandDuration: Int
        get() = if (isHighPerformanceMode) DURATION_SHORT else DURATION_MEDIUM

    val currentFadeDuration: Int
        get() = if (isHighPerformanceMode) DURATION_SHORT else DURATION_NORMAL

    // 动态动画配置
    private var _expandSpec: FiniteAnimationSpec<IntSize> = createExpandSpec(DURATION_MEDIUM)
    private var _shrinkSpec: FiniteAnimationSpec<IntSize> = createShrinkSpec(DURATION_NORMAL)

    val expandTween: FiniteAnimationSpec<IntSize> get() = _expandSpec
    val shrinkTween: FiniteAnimationSpec<IntSize> get() = _shrinkSpec

    // 淡入淡出动画
    val fadeInTween = tween<Float>(
        durationMillis = DURATION_MEDIUM,
        easing = androidx.compose.animation.core.LinearEasing
    )

    val fadeOutTween = tween<Float>(
        durationMillis = DURATION_NORMAL,
        easing = androidx.compose.animation.core.LinearEasing
    )

    // 弹簧动画配置
    val springExpandSpec: FiniteAnimationSpec<IntSize> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )

    val springShrinkSpec: FiniteAnimationSpec<IntSize> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )

    // 高性能动画配置
    private val highPerformanceExpandSpec: FiniteAnimationSpec<IntSize> = tween(
        durationMillis = DURATION_SHORT,
        easing = androidx.compose.animation.core.LinearEasing
    )

    private val highPerformanceShrinkSpec: FiniteAnimationSpec<IntSize> = tween(
        durationMillis = DURATION_SHORT,
        easing = androidx.compose.animation.core.LinearEasing
    )

    private fun updateAnimationSpecs() {
        if (isHighPerformanceMode) {
            _expandSpec = highPerformanceExpandSpec
            _shrinkSpec = highPerformanceShrinkSpec
        } else {
            _expandSpec = createExpandSpec(DURATION_MEDIUM)
            _shrinkSpec = createShrinkSpec(DURATION_NORMAL)
        }
    }

    private fun createExpandSpec(duration: Int): FiniteAnimationSpec<IntSize> = tween(
        durationMillis = duration,
        easing = androidx.compose.animation.core.FastOutSlowInEasing
    )

    private fun createShrinkSpec(duration: Int): FiniteAnimationSpec<IntSize> = tween(
        durationMillis = duration,
        easing = androidx.compose.animation.core.FastOutSlowInEasing
    )

    /**
     * 获取优化的展开动画配置
     */
    fun getOptimizedExpandSpec(useSpring: Boolean = false): FiniteAnimationSpec<IntSize> {
        return when {
            isHighPerformanceMode -> highPerformanceExpandSpec
            useSpring -> springExpandSpec
            else -> _expandSpec
        }
    }

    /**
     * 获取优化的收起动画配置
     */
    fun getOptimizedShrinkSpec(useSpring: Boolean = false): FiniteAnimationSpec<IntSize> {
        return when {
            isHighPerformanceMode -> highPerformanceShrinkSpec
            useSpring -> springShrinkSpec
            else -> _shrinkSpec
        }
    }
}
