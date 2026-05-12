package com.example.smartshoe.ui.screen.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartshoe.R
import com.example.smartshoe.domain.model.SensorDataPoint
import com.example.smartshoe.domain.model.SensorDataRecord
import com.example.smartshoe.ui.theme.AppColors
import com.example.smartshoe.util.DateTimeUtils

/**
 * 选中记录的详细图表展示
 */
@Composable
fun SelectedRecordDetail(
    record: SensorDataRecord,
    data: List<SensorDataPoint>,
    onBackClick: () -> Unit,
    onAiAnalysisClick: ((String) -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.leftarrow),
                            contentDescription = "返回",
                            tint = AppColors.Primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = "记录详情",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.Primary
                    )
                }

                if (onAiAnalysisClick != null) {
                    FilledTonalButton(
                        onClick = { onAiAnalysisClick(record.recordId) },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = AppColors.Primary.copy(alpha = 0.1f),
                            contentColor = AppColors.Primary
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
                    ) {
                        Text("AI分析", fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            RecordInfoCard(record = record)

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "传感器数据图表",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.Primary
            )

            val durationText = remember(record.recordId, data.size) {
                if (data.isNotEmpty()) {
                    val durationSeconds = (record.endTime - record.startTime) / 1000
                    val durationStr = when {
                        durationSeconds < 60 -> "${durationSeconds}秒"
                        durationSeconds < 3600 -> "${durationSeconds / 60}分${durationSeconds % 60}秒"
                        else -> "${durationSeconds / 3600}小时${(durationSeconds % 3600) / 60}分"
                    }
                    "数据时长: $durationStr | ${data.size}个数据点"
                } else ""
            }

            if (durationText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = durationText,
                    fontSize = 11.sp,
                    color = AppColors.PlaceholderText
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            SensorChartItem(data = data, record = record, sensorIndex = 0, sensorName = "传感器 1 - 脚掌前部", color = AppColors.Sensor1)
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(modifier = Modifier.fillMaxWidth(), color = AppColors.DividerColor, thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))

            SensorChartItem(data = data, record = record, sensorIndex = 1, sensorName = "传感器 2 - 脚弓部", color = AppColors.Sensor2)
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(modifier = Modifier.fillMaxWidth(), color = AppColors.DividerColor, thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))

            SensorChartItem(data = data, record = record, sensorIndex = 2, sensorName = "传感器 3 - 脚跟部", color = AppColors.Sensor3)
        }
    }
}

/**
 * 记录信息卡片
 */
@Composable
private fun RecordInfoCard(record: SensorDataRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            val (displayStartTime, displayEndTime) = if (record.startTime <= record.endTime) {
                record.startTime to record.endTime
            } else {
                record.endTime to record.startTime
            }

            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "开始时间",
                    fontSize = 11.sp,
                    color = AppColors.PlaceholderText,
                    modifier = Modifier.align(Alignment.CenterStart)
                )
                Text(
                    text = DateTimeUtils.formatDateTime(displayStartTime),
                    fontSize = 12.sp,
                    color = AppColors.DarkGray,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "结束时间",
                    fontSize = 11.sp,
                    color = AppColors.PlaceholderText,
                    modifier = Modifier.align(Alignment.CenterStart)
                )
                Text(
                    text = DateTimeUtils.formatDateTime(displayEndTime),
                    fontSize = 12.sp,
                    color = AppColors.DarkGray,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                color = AppColors.DividerColor,
                thickness = 1.dp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = "${record.dataCount}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.Primary
                    )
                    Text(
                        text = "数据点",
                        fontSize = 10.sp,
                        color = AppColors.PlaceholderText
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${record.interval}ms",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.Primary
                    )
                    Text(
                        text = "采样间隔",
                        fontSize = 10.sp,
                        color = AppColors.PlaceholderText
                    )
                }

                val compressionText = remember(record.compressionRatio) {
                    "${String.format("%.1f", record.compressionRatio * 100)}%"
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = compressionText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.Primary
                    )
                    Text(
                        text = "压缩率",
                        fontSize = 10.sp,
                        color = AppColors.PlaceholderText
                    )
                }
            }
        }
    }
}
