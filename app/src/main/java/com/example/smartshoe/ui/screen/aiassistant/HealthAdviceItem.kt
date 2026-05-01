package com.example.smartshoe.ui.screen.aiassistant

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.smartshoe.config.AppConfig
import com.example.smartshoe.domain.model.HealthAdviceSummary
import com.example.smartshoe.ui.theme.AppColors

/**
 * 健康建议项
 */
@Composable
fun HealthAdviceItem(
    content: String,
    summary: HealthAdviceSummary?,
    model: String,
    generationTimeMs: Long = 0
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = AppColors.HealthAdviceCard
            ),
            shape = RoundedCornerShape(AppConfig.AiAssistant.MESSAGE_CORNER_RADIUS.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🏥 健康建议",
                    style = MaterialTheme.typography.titleSmall,
                    color = AppColors.HealthAdviceTitle
                )
                Spacer(modifier = Modifier.height(8.dp))
                StyledMarkdownText(
                    markdown = content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.DarkGray
                )

                summary?.let {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    HealthAdviceSummaryView(summary = it)
                }
            }
        }

        val footerText = if (generationTimeMs > 0) {
            "$model · ${generationTimeMs}ms"
        } else {
            model
        }
        Text(
            text = footerText,
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.PlaceholderText,
            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
        )
    }
}

/**
 * 健康建议数据摘要
 */
@Composable
fun HealthAdviceSummaryView(summary: HealthAdviceSummary) {
    Column {
        Text(
            text = "📊 数据摘要",
            style = MaterialTheme.typography.labelMedium,
            color = AppColors.HealthAdviceTitle
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SummaryItem("数据点", "${summary.dataPoints}")
            SummaryItem("平均压力", "${summary.averagePressure.toInt()}")
            SummaryItem("最大压力", "${summary.maxPressure}")
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SummaryItem("最小压力", "${summary.minPressure}")
            SummaryItem("异常数", "${summary.anomalyCount}")
            val balanceText = if (summary.pressureBalanced) "✅ 均衡" else "⚠️ 不均衡"
            SummaryItem("压力分布", balanceText)
        }
    }
}

/**
 * 摘要项
 */
@Composable
fun SummaryItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.PlaceholderText
        )
    }
}
