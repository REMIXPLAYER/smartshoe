package com.example.smartshoe.ui.component.sensor

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
import com.example.smartshoe.ui.theme.AppColors

/**
 * 传感器画布组件
 * 用于在鞋垫图片上绘制传感器
 */
object SensorCanvas {

    /**
     * 鞋垫与传感器组合组件（带Card包装和数值展示）
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
        Card(
            modifier = modifier,
            elevation = CardDefaults.cardElevation(defaultElevation = cardElevation),
            colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(cardPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 上层: 鞋垫图片 + 传感器绘制层
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
                        val canvasSize = Size(size.width, size.height)

                        sensorPositions.forEachIndexed { index, position ->
                            val color = sensorColors.getOrElse(index) { Color.Gray }
                            val bounds = calculateSensorBounds(canvasSize, position)
                            drawSensor(color, bounds, position.shape)
                        }
                    }
                }

                // 分隔线
                if (sensorValues.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.LightGray.copy(alpha = 0.5f),
                        thickness = 1.dp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // 下层: 传感器数值展示
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
                        color = if (value > 0) AppColors.Title else Color.Gray
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
     * 可配置的鞋垫传感器组件（带Card包装）
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
            colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(cardPadding),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = insoleImageRes),
                    contentDescription = "鞋垫",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale
                )

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
     * 无Card包装的鞋垫传感器组件（原始版本）
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
            Image(
                painter = painterResource(id = R.drawable.insole_image),
                contentDescription = "鞋垫",
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale
            )

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
 * 传感器配置数据类
 */
data class SensorConfig(
    val color: Color,
    val position: SensorPosition
)
