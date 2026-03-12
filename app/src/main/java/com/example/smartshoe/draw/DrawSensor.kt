package com.example.smartshoe.draw

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartshoe.R
import com.example.smartshoe.UIConstants

object DrawSensor {

    /**
     * =============================================================================
     * 鞋垫与传感器组合组件（带Card包装和数值展示）
     * =============================================================================
     *
     * 布局结构:
     * 1. 外层: Card卡片（保持与其他组件一致的设计风格）
     * 2. 内层: Column容器
     *    - 上层: 鞋垫图片 + 传感器绘制层 (Box)
     *    - 分隔线
     *    - 下层: 传感器数值展示 (Row)
     *
     * 尺寸关系:
     * - Card大小由 modifier 参数控制
     * - 鞋垫图片和传感器绘制层填满上部空间
     * - 传感器位置相对于鞋垫图片的宽高比例计算
     *
     * @param modifier 组件尺寸修饰符 (控制整个Card的大小)
     *                 例如: Modifier.size(300.dp) 或 Modifier.fillMaxWidth()
     *
     * @param sensorColors 传感器颜色列表 (3个颜色对应3个传感器)
     *                    索引0=前掌, 索引1=足弓, 索引2=脚跟
     *
     * @param sensorValues 传感器数值列表 (3个数值)
     *                    索引0=前掌, 索引1=足弓, 索引2=脚跟
     *
     * @param sensorPositions 传感器位置配置列表
     *                       默认使用 SensorPosition.DEFAULT_POSITIONS
     *                       如需调整位置，传入自定义的 SensorPosition 列表
     *
     * @param contentScale 鞋垫图片缩放模式
     *                     ContentScale.Fit = 自适应，保持比例
     *                     ContentScale.FillBounds = 填充整个区域（可能变形）
     *                     ContentScale.Crop = 裁剪填充
     *
     * @param cardElevation Card阴影高度（默认4.dp，与其他组件一致）
     *
     * @param cardPadding Card内边距（默认12.dp）
     */
    @Composable
    fun InsoleWithSensors(
        modifier: Modifier = Modifier,
        sensorColors: List<Color>,
        sensorValues: List<Int> = emptyList(),
        sensorPositions: List<SensorPosition> = SensorPosition.DEFAULT_POSITIONS,
        contentScale: ContentScale = ContentScale.Fit,
        cardElevation: androidx.compose.ui.unit.Dp = 4.dp,
        cardPadding: androidx.compose.ui.unit.Dp = 12.dp
    ) {
        // =====================================================================
        // Card包装层 - 保持与其他组件一致的设计风格
        // =====================================================================
        Card(
            modifier = modifier,
            elevation = CardDefaults.cardElevation(defaultElevation = cardElevation),
            colors = CardDefaults.cardColors(containerColor = UIConstants.SurfaceColor),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(cardPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // =====================================================================
                // 上层: 鞋垫图片 + 传感器绘制层
                // =====================================================================
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    // 鞋垫图片
                    Image(
                        painter = painterResource(id = R.drawable.insole_image),
                        contentDescription = "鞋垫",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = contentScale
                    )

                    // 传感器绘制层
                    Canvas(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // 获取画布尺寸（与鞋垫图片显示尺寸相同）
                        val canvasSize = Size(size.width, size.height)

                        // 遍历绘制每个传感器
                        sensorPositions.forEachIndexed { index, position ->
                            val color = sensorColors.getOrElse(index) { Color.Gray }

                            // 计算传感器在此画布上的实际绘制区域
                            val bounds = calculateSensorBounds(canvasSize, position)

                            // 绘制传感器形状
                            drawSensor(color, bounds, position.shape)
                        }
                    }
                }

                // =====================================================================
                // 分隔线
                // =====================================================================
                if (sensorValues.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.LightGray.copy(alpha = 0.5f),
                        thickness = 1.dp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // =====================================================================
                    // 下层: 传感器数值展示
                    // =====================================================================
                    SensorValuesRow(sensorValues = sensorValues)
                }
            }
        }
    }

    /**
     * 传感器数值展示行
     */
    @Composable
    private fun SensorValuesRow(sensorValues: List<Int>) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            sensorValues.forEachIndexed { index, value ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "传感器 ${index + 1}",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        value.toString(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (value > 0) UIConstants.TitleColor else Color.Gray
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        "单位",
                        fontSize = 12.sp,
                        color = Color.LightGray
                    )
                }
            }
        }
    }

    /**
     * =============================================================================
     * 可配置的鞋垫传感器组件（带Card包装）
     * =============================================================================
     * 支持自定义传感器数量、位置和形状
     *
     * 使用示例:
     * ```
     * CustomInsoleWithSensors(
     *     modifier = Modifier.size(300.dp),
     *     sensorConfigs = listOf(
     *         SensorConfig(
     *             color = Color.Red,
     *             position = SensorPosition(
     *                 relativeX = 0.5f,    // 水平中央
     *                 relativeY = 0.3f,    // 上部30%
     *                 relativeWidth = 0.25f,
     *                 relativeHeight = 0.15f,
     *                 shape = SensorShape.Forefoot
     *             )
     *         )
     *     )
     * )
     * ```
     *
     * @param cardElevation Card阴影高度（默认4.dp）
     * @param cardPadding Card内边距（默认12.dp）
     */
    @Composable
    fun CustomInsoleWithSensors(
        modifier: Modifier = Modifier,
        sensorConfigs: List<SensorConfig>,
        insoleImageRes: Int = R.drawable.insole_image,
        contentScale: ContentScale = ContentScale.Fit,
        cardElevation: androidx.compose.ui.unit.Dp = 4.dp,
        cardPadding: androidx.compose.ui.unit.Dp = 12.dp
    ) {
        Card(
            modifier = modifier,
            elevation = CardDefaults.cardElevation(defaultElevation = cardElevation),
            colors = CardDefaults.cardColors(containerColor = UIConstants.SurfaceColor),
            shape = RoundedCornerShape(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(cardPadding),
                contentAlignment = Alignment.Center
            ) {
                // 鞋垫图片层
                Image(
                    painter = painterResource(id = insoleImageRes),
                    contentDescription = "鞋垫",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale
                )

                // 传感器绘制层
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val canvasSize = Size(size.width, size.height)

                    sensorConfigs.forEach { config ->
                        val bounds = calculateSensorBounds(canvasSize, config.position)
                        drawSensor(config.color, bounds, config.position.shape)
                    }
                }
            }
        }
    }

    /**
     * =============================================================================
     * 无Card包装的鞋垫传感器组件（原始版本）
     * =============================================================================
     * 如需自定义外部容器，使用此版本
     */
    @Composable
    fun InsoleWithSensorsRaw(
        modifier: Modifier = Modifier,
        sensorColors: List<Color>,
        sensorPositions: List<SensorPosition> = SensorPosition.DEFAULT_POSITIONS,
        contentScale: ContentScale = ContentScale.Fit
    ) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            // 鞋垫图片
            Image(
                painter = painterResource(id = R.drawable.insole_image),
                contentDescription = "鞋垫",
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale
            )

            // 传感器绘制层
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val canvasSize = Size(size.width, size.height)

                sensorPositions.forEachIndexed { index, position ->
                    val color = sensorColors.getOrElse(index) { Color.Gray }
                    val bounds = calculateSensorBounds(canvasSize, position)
                    drawSensor(color, bounds, position.shape)
                }
            }
        }
    }
}

/**
 * =============================================================================
 * 传感器配置数据类
 * =============================================================================
 * 用于 CustomInsoleWithSensors 组件
 */
data class SensorConfig(
    val color: Color,           // 传感器显示颜色
    val position: SensorPosition // 传感器位置和形状配置
)
