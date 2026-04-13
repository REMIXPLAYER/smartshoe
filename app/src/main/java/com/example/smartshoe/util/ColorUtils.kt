package com.example.smartshoe.util

import androidx.compose.ui.graphics.Color
import com.example.smartshoe.config.AppConfig
import com.example.smartshoe.domain.model.PressureStatus
import kotlin.math.pow

/**
 * 颜色工具类
 * 提供颜色计算、插值功能和压力状态评估
 */
object ColorUtils {

    // ========== 颜色配置 ==========
    // 新的压力状态颜色：绿色=正常，橙色=偏高，红色=过高
    val COLOR_NORMAL = Color(0xFF4CAF50)   // 绿色 - 正常压力
    val COLOR_HIGH = Color(0xFFFF9800)     // 橙色 - 偏高压力
    val COLOR_CRITICAL = Color(0xFFF44336) // 红色 - 过高压力
    val COLOR_DEFAULT = Color.Gray         // 默认颜色
    val COLOR_ZERO = Color(0xFF9E9E9E)     // 浅灰色 - 数值为0时显示

    // 兼容旧版本的颜色定义
    val COLOR_LOW = COLOR_NORMAL
    val COLOR_MID_LOW = Color(0xFF81C784)
    val COLOR_MID_HIGH = COLOR_HIGH

    // ========== 压力阈值配置 ==========
    // 从 AppConfig 读取阈值配置
    val THRESHOLD_NORMAL = AppConfig.Sensor.COLOR_THRESHOLD_NORMAL
    val THRESHOLD_HIGH = AppConfig.Sensor.COLOR_THRESHOLD_HIGH

    // ========== 加权平均配置 ==========
    // 从 AppConfig 读取加权平均配置
    val DEFAULT_WINDOW_SIZE = AppConfig.Sensor.WEIGHTED_WINDOW_SIZE
    val DEFAULT_WEIGHT_DECAY = AppConfig.Sensor.WEIGHT_DECAY_FACTOR

    /**
     * 根据压力值计算显示颜色（渐变渲染版本）
     * 使用渐变色平滑过渡，而非三档硬切换
     *
     * @param value 压力值 (0-4095)
     * @return 对应的颜色
     */
    fun calculateColorFromPressure(value: Int): Color {
        // 数值为0时显示浅灰色
        if (value == 0) {
            return COLOR_ZERO
        }

        val normalizedValue = value.coerceIn(0, 4095) / 4095f

        return when {
            normalizedValue < THRESHOLD_NORMAL -> {
                // 低压力区域：从浅绿色渐变到绿色
                val t = normalizedValue / THRESHOLD_NORMAL
                lerpColor(COLOR_LOW, COLOR_MID_LOW, t)
            }
            normalizedValue < THRESHOLD_HIGH -> {
                // 中压力区域：从绿色渐变到橙色
                val t = (normalizedValue - THRESHOLD_NORMAL) / (THRESHOLD_HIGH - THRESHOLD_NORMAL)
                lerpColor(COLOR_MID_LOW, COLOR_MID_HIGH, t)
            }
            else -> {
                // 高压力区域：从橙色渐变到红色
                val t = (normalizedValue - THRESHOLD_HIGH) / (1f - THRESHOLD_HIGH)
                lerpColor(COLOR_MID_HIGH, COLOR_HIGH, t)
            }
        }
    }

    /**
     * 计算加权平均值
     * 使用指数衰减权重，越新的数据权重越高
     *
     * @param values 数值列表（时间顺序，最新的在最后）
     * @param decayFactor 衰减系数 (0-1)，越小表示旧数据权重衰减越快
     * @return 加权平均值
     */
    fun calculateWeightedAverage(values: List<Int>, decayFactor: Float = DEFAULT_WEIGHT_DECAY): Float {
        if (values.isEmpty()) return 0f
        if (values.size == 1) return values[0].toFloat()

        var totalWeight = 0f
        var weightedSum = 0f

        // 从最新的数据开始遍历（权重最高）
        for (i in values.indices) {
            val value = values[values.size - 1 - i]  // 倒序访问，最新的在前
            val weight = decayFactor.pow(i)  // 使用 Float 的 pow 扩展函数
            weightedSum += value * weight
            totalWeight += weight
        }

        return if (totalWeight > 0) weightedSum / totalWeight else 0f
    }

    /**
     * 计算滑动窗口加权平均值
     *
     * @param values 数值列表
     * @param windowSize 窗口大小
     * @param decayFactor 衰减系数
     * @return 窗口内的加权平均值
     */
    fun calculateSlidingWindowAverage(
        values: List<Int>,
        windowSize: Int = DEFAULT_WINDOW_SIZE,
        decayFactor: Float = DEFAULT_WEIGHT_DECAY
    ): Float {
        if (values.isEmpty()) return 0f

        val window = values.takeLast(windowSize.coerceAtMost(values.size))
        return calculateWeightedAverage(window, decayFactor)
    }

    /**
     * 获取压力状态等级
     *
     * @param normalizedValue 归一化压力值 (0-1)
     * @return 压力状态等级
     */
    fun getPressureStatus(normalizedValue: Float): PressureStatus {
        return when {
            normalizedValue < THRESHOLD_NORMAL -> PressureStatus.NORMAL
            normalizedValue < THRESHOLD_HIGH -> PressureStatus.HIGH
            else -> PressureStatus.CRITICAL
        }
    }

    /**
     * 根据压力值获取压力状态
     *
     * @param value 压力值 (0-4095)
     * @return 压力状态
     */
    fun getPressureStatusFromValue(value: Int): PressureStatus {
        if (value == 0) return PressureStatus.NONE
        val normalizedValue = value.coerceIn(0, 4095) / 4095f
        return getPressureStatus(normalizedValue)
    }

    /**
     * 根据加权平均值获取压力状态
     *
     * @param weightedAverage 加权平均压力值
     * @return 压力状态
     */
    fun getPressureStatusFromAverage(weightedAverage: Float): PressureStatus {
        if (weightedAverage <= 0) return PressureStatus.NONE
        val normalizedValue = weightedAverage.coerceIn(0f, 4095f) / 4095f
        return getPressureStatus(normalizedValue)
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
     * 获取压力等级描述（旧版本，兼容用）
     *
     * @param value 压力值
     * @return 压力等级描述
     */
    fun getPressureLevelDescription(value: Int): String {
        return getPressureStatusFromValue(value).description
    }

}
