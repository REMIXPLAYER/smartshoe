package com.example.smartshoe.ui.screen.aiassistant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.smartshoe.config.AppConfig
import com.example.smartshoe.ui.theme.AppColors

/**
 * AI消息项
 */
@Composable
fun AiMessageItem(content: String, model: String, generationTimeMs: Long = 0) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = AppColors.AiMessage,
                    shape = RoundedCornerShape(
                        AppConfig.AiAssistant.MESSAGE_CORNER_RADIUS.dp,
                        AppConfig.AiAssistant.MESSAGE_CORNER_RADIUS.dp,
                        AppConfig.AiAssistant.MESSAGE_CORNER_RADIUS.dp,
                        4.dp
                    )
                )
                .padding(12.dp)
                .widthIn(max = AppConfig.AiAssistant.MAX_MESSAGE_WIDTH_DP.dp)
        ) {
            StyledMarkdownText(
                markdown = content,
                color = AppColors.DarkGray,
                style = MaterialTheme.typography.bodyMedium
            )
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
 * 流式AI消息项
 * 使用节流Markdown渲染以减少流式输出时的CPU开销
 */
@Composable
fun StreamingAiMessageItem(content: String, model: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = AppColors.AiMessage,
                    shape = RoundedCornerShape(
                        AppConfig.AiAssistant.MESSAGE_CORNER_RADIUS.dp,
                        AppConfig.AiAssistant.MESSAGE_CORNER_RADIUS.dp,
                        AppConfig.AiAssistant.MESSAGE_CORNER_RADIUS.dp,
                        4.dp
                    )
                )
                .padding(12.dp)
                .widthIn(max = AppConfig.AiAssistant.MAX_MESSAGE_WIDTH_DP.dp)
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                StyledMarkdownText(
                    markdown = content,
                    isStreaming = true,
                    color = AppColors.DarkGray,
                    style = MaterialTheme.typography.bodyMedium
                )
                BlinkingCursor()
            }
        }
        Text(
            text = "生成中...",
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.PlaceholderText,
            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
        )
    }
}
