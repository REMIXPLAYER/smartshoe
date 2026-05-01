package com.example.smartshoe.ui.screen.datarecord

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartshoe.domain.model.SensorDataPoint
import com.example.smartshoe.ui.theme.AppColors

/**
 * 整合的传感器图表卡片
 * 三个图表在一个Card中，使用分隔线分隔，可滚动查看
 */
@Composable
fun CombinedChartsCard(
    historicalData: List<SensorDataPoint>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "实时数据图表",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.Primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            SensorChartItem(
                data = historicalData,
                sensorIndex = 0,
                sensorName = "传感器 1 - 脚掌前部"
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(modifier = Modifier.fillMaxWidth(), color = AppColors.DividerColor, thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            SensorChartItem(
                data = historicalData,
                sensorIndex = 1,
                sensorName = "传感器 2 - 脚弓部"
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(modifier = Modifier.fillMaxWidth(), color = AppColors.DividerColor, thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            SensorChartItem(
                data = historicalData,
                sensorIndex = 2,
                sensorName = "传感器 3 - 脚跟部"
            )
        }
    }
}
