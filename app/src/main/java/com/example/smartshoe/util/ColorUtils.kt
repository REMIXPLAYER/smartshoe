package com.example.smartshoe.util

import androidx.compose.ui.graphics.Color

/**
 * 颜色工具类
 * 提供颜色计算和插值功能
 */
object ColorUtils {

    // ColorHunt 配色方案
    val COLOR_LOW = Color(0xFFA3D78A)      // 淡绿色 - 低压力
    val COLOR_MID_LOW = Color(0xFFC1E59F)  // 浅绿色 - 中低压力
    val COLOR_MID_HIGH = Color(0xFFFF937E) // 橙色 - 中高压力
    val COLOR_HIGH = Color(0xFFFF5555)     // 红色 - 高压力
    val COLOR_DEFAULT = Color.Gray         // 默认颜色

    /**
     * 根据压力值计算显示颜色
     * 压力值越低越接近淡绿色，越高越接近红色
     *
     * @param value 压力值 (0-4095)
     * @return 对应的颜色
     */
    fun calculateColorFromPressure(value: Int): Color {
        val normalizedValue = value.coerceIn(0, 4095) / 4095f

        return when {
            normalizedValue < 0.33f -> {
                val t = normalizedValue / 0.33f
                lerpColor(COLOR_LOW, COLOR_MID_LOW, t)
            }
            normalizedValue < 0.66f -> {
                val t = (normalizedValue - 0.33f) / 0.33f
                lerpColor(COLOR_MID_LOW, COLOR_MID_HIGH, t)
            }
            else -> {
                val t = (normalizedValue - 0.66f) / 0.34f
                lerpColor(COLOR_MID_HIGH, COLOR_HIGH, t)
            }
        }
    }

    /**
     * 线性插值计算两个颜色之间的过渡色
     *
     * @param start 起始颜色
     * @param end 结束颜色
     * @param fraction 插值比例 (0.0 - 1.0)
     * @return 插值后的颜色
     */
    fun lerpColor(start: Color, end: Color, fraction: Float): Color {
        val clampedFraction = fraction.coerceIn(0f, 1f)
        return Color(
            red = start.red + (end.red - start.red) * clampedFraction,
            green = start.green + (end.green - start.green) * clampedFraction,
            blue = start.blue + (end.blue - start.blue) * clampedFraction,
            alpha = start.alpha + (end.alpha - start.alpha) * clampedFraction
        )
    }

    /**
     * 根据多个压力值计算颜色列表
     *
     * @param values 压力值列表
     * @return 对应的颜色列表
     */
    fun calculateColorsFromPressures(values: List<Int>): List<Color> {
        return values.map { calculateColorFromPressure(it) }
    }

    /**
     * 获取压力等级描述
     *
     * @param value 压力值
     * @return 压力等级描述
     */
    fun getPressureLevelDescription(value: Int): String {
        val normalizedValue = value.coerceIn(0, 4095) / 4095f
        return when {
            normalizedValue < 0.33f -> "正常"
            normalizedValue < 0.66f -> "偏高"
            else -> "过高"
        }
    }
}
