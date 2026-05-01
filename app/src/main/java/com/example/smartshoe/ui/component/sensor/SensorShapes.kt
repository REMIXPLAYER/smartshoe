package com.example.smartshoe.ui.component.sensor

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.smartshoe.ui.theme.AppColors
import com.example.smartshoe.ui.theme.AppDimensions

/**
 * 传感器形状定义 - 使用原始贝塞尔曲线形状
 * 这些形状使用相对坐标系统，会根据传入的bounds自动缩放
 */
sealed class SensorShape {
    abstract fun createPath(bounds: Rect): Path

    /**
     * 前掌传感器形状 - 原始贝塞尔曲线形状
     */
    object Forefoot : SensorShape() {
        override fun createPath(bounds: Rect): Path {
            return Path().apply {
                val scaleX = bounds.width / 225f
                val scaleY = bounds.height / 240f
                val baseX = bounds.left
                val baseY = bounds.top

                moveTo(baseX + 30f * scaleX, baseY + 0f * scaleY)
                cubicTo(
                    baseX - 15f * scaleX, baseY + 15f * scaleY,
                    baseX - 30f * scaleX, baseY + 225f * scaleY,
                    baseX + 30f * scaleX, baseY + 240f * scaleY
                )
                cubicTo(
                    baseX + 225f * scaleX, baseY + 225f * scaleY,
                    baseX + 150f * scaleX, baseY + 90f * scaleY,
                    baseX + 45f * scaleX, baseY + 0f * scaleY
                )
                close()
            }
        }
    }

    /**
     * 足弓传感器形状 - 原始贝塞尔曲线形状
     */
    object Arch : SensorShape() {
        override fun createPath(bounds: Rect): Path {
            return Path().apply {
                val scaleX = bounds.width / 127.5f
                val scaleY = bounds.height / 450f
                val baseX = bounds.left
                val baseY = bounds.top

                moveTo(baseX + 30f * scaleX, baseY + 0f * scaleY)
                cubicTo(
                    baseX - 37.5f * scaleX, baseY + 37.5f * scaleY,
                    baseX + 60f * scaleX, baseY + 225f * scaleY,
                    baseX - 52.5f * scaleX, baseY + 375f * scaleY
                )
                cubicTo(
                    baseX - 52.5f * scaleX, baseY + 375f * scaleY,
                    baseX - 45f * scaleX, baseY + 450f * scaleY,
                    baseX + 37.5f * scaleX, baseY + 412.5f * scaleY
                )
                cubicTo(
                    baseX + 105f * scaleX, baseY + 172.5f * scaleY,
                    baseX + 127.5f * scaleX, baseY + 112.5f * scaleY,
                    baseX + 97.5f * scaleX, baseY + 45f * scaleY
                )
                cubicTo(
                    baseX + 82.5f * scaleX, baseY + 15f * scaleY,
                    baseX + 37.5f * scaleX, baseY + 7.5f * scaleY,
                    baseX + 22.5f * scaleX, baseY + 0f * scaleY
                )
                close()
            }
        }
    }

    /**
     * 脚跟传感器形状 - 原始贝塞尔曲线形状
     */
    object Heel : SensorShape() {
        override fun createPath(bounds: Rect): Path {
            return Path().apply {
                val scaleX = bounds.width / 225f
                val scaleY = bounds.height / 190f
                val baseX = bounds.left
                val baseY = bounds.top

                moveTo(baseX + 30f * scaleX, baseY + 0f * scaleY)
                cubicTo(
                    baseX - 15f * scaleX, baseY + 15f * scaleY,
                    baseX - 50f * scaleX, baseY + 180f * scaleY,
                    baseX + 30f * scaleX, baseY + 190f * scaleY
                )
                cubicTo(
                    baseX + 225f * scaleX, baseY + 190f * scaleY,
                    baseX + 120f * scaleX, baseY - 40f * scaleY,
                    baseX + 25f * scaleX, baseY + 0f * scaleY
                )
                close()
            }
        }
    }
}

/**
 * 传感器位置配置
 * 使用相对坐标（0.0-1.0）定义传感器在鞋垫上的位置
 */
data class SensorPosition(
    val relativeX: Float,
    val relativeY: Float,
    val relativeWidth: Float,
    val relativeHeight: Float,
    val shape: SensorShape
) {
    companion object {
        /**
         * 默认传感器位置配置
         */
        val DEFAULT_POSITIONS = listOf(
            SensorPosition(
                relativeX = 0.43f,
                relativeY = 0.25f,
                relativeWidth = 0.22f,
                relativeHeight = 0.19f,
                shape = SensorShape.Forefoot
            ),
            SensorPosition(
                relativeX = 0.64f,
                relativeY = 0.45f,
                relativeWidth = 0.13f,
                relativeHeight = 0.42f,
                shape = SensorShape.Arch
            ),
            SensorPosition(
                relativeX = 0.56f,
                relativeY = 0.85f,
                relativeWidth = 0.23f,
                relativeHeight = 0.15f,
                shape = SensorShape.Heel
            )
        )
    }
}

/**
 * 绘制传感器
 */
fun DrawScope.drawSensor(
    color: Color,
    bounds: Rect,
    shape: SensorShape
) {
    val path = shape.createPath(bounds)

    drawPath(path = path, color = color)

    drawPath(
        path = path,
        color = AppColors.MediumGray.copy(alpha = AppDimensions.SensorBorderAlpha),
        style = Stroke(width = AppDimensions.SensorBorderWidth)
    )
}

/**
 * 计算传感器绘制区域
 */
fun calculateSensorBounds(
    canvasSize: Size,
    position: SensorPosition
): Rect {
    val width = canvasSize.width * position.relativeWidth
    val height = canvasSize.height * position.relativeHeight

    val left = canvasSize.width * position.relativeX - width / 2
    val top = canvasSize.height * position.relativeY - height / 2

    return Rect(left, top, left + width, top + height)
}
