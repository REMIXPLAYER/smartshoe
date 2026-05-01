package com.example.smartshoe.ui.screen.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartshoe.domain.model.SensorDataRecord
import com.example.smartshoe.ui.theme.AppColors
import com.example.smartshoe.util.DateTimeUtils

/**
 * 记录列表
 */
@Composable
fun RecordList(
    records: List<SensorDataRecord>,
    selectedRecord: SensorDataRecord?,
    onRecordToggle: (SensorDataRecord) -> Unit
) {
    val itemHeight = 56.dp
    val maxListHeight = itemHeight * 6 + 48.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = maxListHeight)
            .verticalScroll(rememberScrollState())
    ) {
        records.forEach { record ->
            val isSelected = selectedRecord?.recordId == record.recordId

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                onClick = { onRecordToggle(record) },
                colors = CardDefaults.cardColors(containerColor = AppColors.CardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = DateTimeUtils.formatMonthDayHourMinute(record.startTime),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.Primary
                        )
                        Text(
                            text = "${record.dataCount}个数据点 | 压缩率: ${String.format("%.1f", record.compressionRatio * 100)}%",
                            fontSize = 12.sp,
                            color = AppColors.PlaceholderText
                        )
                    }

                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "已选中",
                            tint = AppColors.Primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}
