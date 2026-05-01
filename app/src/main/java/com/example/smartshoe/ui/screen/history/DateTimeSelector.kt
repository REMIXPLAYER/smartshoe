package com.example.smartshoe.ui.screen.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartshoe.domain.model.SensorDataRecord
import com.example.smartshoe.ui.theme.AppColors
import com.example.smartshoe.util.DateTimeUtils
import java.util.Calendar
import java.util.Date

/**
 * 日期选择器 - 包含记录列表
 */
@Composable
fun DateTimeSelector(
    startDate: Date?,
    endDate: Date?,
    onStartDateChange: (Date?) -> Unit,
    onEndDateChange: (Date?) -> Unit,
    onQueryClick: () -> Unit,
    isLoading: Boolean,
    records: List<SensorDataRecord>,
    selectedRecord: SensorDataRecord?,
    queryExecuted: Boolean = false,
    onRecordToggle: (SensorDataRecord) -> Unit,
    onShowDatePicker: ((Date, (Date) -> Unit) -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "选择时间范围",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Primary
                )

                if (startDate != null || endDate != null) {
                    FilledTonalButton(
                        onClick = {
                            onStartDateChange(null)
                            onEndDateChange(null)
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = AppColors.Primary.copy(alpha = 0.1f),
                            contentColor = AppColors.Primary
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("清除", fontSize = 13.sp)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        onShowDatePicker?.invoke(startDate ?: Date()) { newDate ->
                            val calendar = Calendar.getInstance()
                            calendar.time = newDate
                            calendar.set(Calendar.HOUR_OF_DAY, 0)
                            calendar.set(Calendar.MINUTE, 0)
                            calendar.set(Calendar.SECOND, 0)
                            calendar.set(Calendar.MILLISECOND, 0)
                            onStartDateChange(calendar.time)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "开始日期",
                            fontSize = 12.sp,
                            color = AppColors.PlaceholderText
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = startDate?.let { DateTimeUtils.formatDate(it.time) } ?: "未选择",
                                fontSize = 13.sp,
                                color = if (startDate != null) AppColors.MediumGray else AppColors.PlaceholderText
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = {
                        onShowDatePicker?.invoke(endDate ?: Date()) { newDate ->
                            val calendar = Calendar.getInstance()
                            calendar.time = newDate
                            calendar.set(Calendar.HOUR_OF_DAY, 23)
                            calendar.set(Calendar.MINUTE, 59)
                            calendar.set(Calendar.SECOND, 59)
                            calendar.set(Calendar.MILLISECOND, 999)
                            onEndDateChange(calendar.time)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "结束日期",
                            fontSize = 12.sp,
                            color = AppColors.PlaceholderText
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = endDate?.let { DateTimeUtils.formatDate(it.time) } ?: "未选择",
                                fontSize = 13.sp,
                                color = if (endDate != null) AppColors.MediumGray else AppColors.PlaceholderText
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onQueryClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary),
                shape = RoundedCornerShape(8.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = if (isLoading) "查询中..." else if (startDate == null && endDate == null) "查询所有记录" else "查询历史记录")
            }

            if (records.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "查询结果 (${records.size}条记录)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.PlaceholderText,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                RecordList(
                    records = records,
                    selectedRecord = selectedRecord,
                    onRecordToggle = onRecordToggle
                )
            } else if (!isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (queryExecuted) "该时间段内没有数据记录" else "暂无数据，请选择时间范围查询",
                        fontSize = 14.sp,
                        color = AppColors.PlaceholderText
                    )
                }
            }
        }
    }
}
