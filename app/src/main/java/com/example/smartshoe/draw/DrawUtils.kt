package com.example.smartshoe.draw

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.smartshoe.UIConstants

/**
 * =============================================================================
 * 传感器形状定义 - 使用原始贝塞尔曲线形状
 * =============================================================================
 * 这些形状使用相对坐标系统，会根据传入的bounds自动缩放
 */
sealed class SensorShape {
    abstract fun createPath(bounds: Rect): Path

    /**
     * 前掌传感器形状 - 原始贝塞尔曲线形状
     * 基于原始drawCustomShape1的曲线定义
     */
    object Forefoot : SensorShape() {
        override fun createPath(bounds: Rect): Path {
            return Path().apply {
                // 使用相对坐标计算实际绘制位置
                // 原始形状参考尺寸: 宽225f, 高240f
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
     * 基于原始drawCustomShape2的曲线定义
     */
    object Arch : SensorShape() {
        override fun createPath(bounds: Rect): Path {
            return Path().apply {
                // 原始形状参考尺寸: 宽127.5f, 高450f
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
     * 基于原始drawCustomShape3的曲线定义
     */
    object Heel : SensorShape() {
        override fun createPath(bounds: Rect): Path {
            return Path().apply {
                // 原始形状参考尺寸: 宽225f, 高190f
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
 * =============================================================================
 * 传感器位置配置
 * =============================================================================
 * 使用相对坐标（0.0-1.0）定义传感器在鞋垫上的位置
 *
 * 坐标系说明:
 * - relativeX: 传感器中心相对于鞋垫宽度的位置 (0.0=左边缘, 0.5=中央, 1.0=右边缘)
 * - relativeY: 传感器中心相对于鞋垫高度的位置 (0.0=顶部, 0.5=中央, 1.0=底部)
 * - relativeWidth: 传感器宽度相对于鞋垫宽度的比例
 * - relativeHeight: 传感器高度相对于鞋垫高度的比例
 */
data class SensorPosition(
    val relativeX: Float,      // 传感器中心X位置 (0.0-1.0)
    val relativeY: Float,      // 传感器中心Y位置 (0.0-1.0)
    val relativeWidth: Float,  // 传感器宽度比例 (0.0-1.0)
    val relativeHeight: Float, // 传感器高度比例 (0.0-1.0)
    val shape: SensorShape     // 传感器形状
) {
    companion object {
        /**
         * =============================================================================
         * 默认传感器位置配置
         * =============================================================================
         * 这些参数控制传感器在鞋垫图片上的位置和大小
         *
         * 如需调整传感器位置，修改以下参数：
         * - relativeX: 左右位置（增大向右移动，减小向左移动）
         * - relativeY: 上下位置（增大向下移动，减小向上移动）
         * - relativeWidth/Height: 传感器大小
         */
        val DEFAULT_POSITIONS = listOf(
            // -------------------------------------------------------------------------
            // 传感器1：前掌传感器（脚掌前部）
            // -------------------------------------------------------------------------
            SensorPosition(
                relativeX = 0.43f,      // 水平位置：鞋垫中央
                relativeY = 0.25f,      // 垂直位置：鞋垫上部25%处
                relativeWidth = 0.22f,  // 宽度：鞋垫宽度的40%
                relativeHeight = 0.19f, // 高度：鞋垫高度的20%
                shape = SensorShape.Forefoot
            ),

            // -------------------------------------------------------------------------
            // 传感器2：足弓传感器（脚弓部）
            // -------------------------------------------------------------------------
            SensorPosition(
                relativeX = 0.64f,      // 水平位置：稍微偏右（脚弓在内侧）
                relativeY = 0.45f,      // 垂直位置：鞋垫中部
                relativeWidth = 0.13f,  // 宽度：较窄
                relativeHeight = 0.42f, // 高度：较长
                shape = SensorShape.Arch
            ),

            // -------------------------------------------------------------------------
            // 传感器3：脚跟传感器（脚跟部）
            // -------------------------------------------------------------------------
            SensorPosition(
                relativeX = 0.56f,      // 水平位置：鞋垫中央
                relativeY = 0.85f,      // 垂直位置：鞋垫下部82%处
                relativeWidth = 0.23f,  // 宽度：鞋垫宽度的45%
                relativeHeight = 0.15f, // 高度：鞋垫高度的18%
                shape = SensorShape.Heel
            )
        )
    }
}

/**
 * =============================================================================
 * 绘制传感器
 * =============================================================================
 * @param color 传感器填充颜色
 * @param bounds 传感器绘制区域（由calculateSensorBounds计算）
 * @param shape 传感器形状
 */
fun DrawScope.drawSensor(
    color: Color,
    bounds: Rect,
    shape: SensorShape
) {
    val path = shape.createPath(bounds)

    // 绘制填充
    drawPath(path = path, color = color)

    // 绘制边框
    drawPath(
        path = path,
        color = Color.Gray.copy(alpha = UIConstants.SensorBorderAlpha),
        style = Stroke(width = UIConstants.SensorBorderWidth)
    )
}

/**
 * =============================================================================
 * 计算传感器绘制区域
 * =============================================================================
 * 根据画布尺寸和传感器位置配置，计算传感器实际的绘制矩形区域
 *
 * @param canvasSize 画布尺寸（与鞋垫图片尺寸相同）
 * @param position 传感器位置配置
 * @return 传感器绘制区域（Rect）
 */
fun calculateSensorBounds(
    canvasSize: Size,
    position: SensorPosition
): Rect {
    // 计算传感器实际尺寸
    val width = canvasSize.width * position.relativeWidth
    val height = canvasSize.height * position.relativeHeight

    // 计算传感器左上角位置（基于中心点）
    val left = canvasSize.width * position.relativeX - width / 2
    val top = canvasSize.height * position.relativeY - height / 2

    return Rect(left, top, left + width, top + height)
}


