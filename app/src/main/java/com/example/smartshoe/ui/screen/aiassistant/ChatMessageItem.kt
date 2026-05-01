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
import com.example.smartshoe.ui.viewmodel.ChatMessage

/**
 * 聊天消息项 - 根据消息类型分发到对应的渲染组件
 */
@Composable
fun ChatMessageItem(message: ChatMessage) {
    when (message) {
        is ChatMessage.User -> UserMessageItem(content = message.content)
        is ChatMessage.Ai -> AiMessageItem(
            content = message.content,
            model = message.model,
            generationTimeMs = message.generationTimeMs
        )
        is ChatMessage.HealthAdvice -> HealthAdviceItem(
            content = message.content,
            summary = message.summary,
            model = message.model,
            generationTimeMs = message.generationTimeMs
        )
        is ChatMessage.StreamingAi -> StreamingAiMessageItem(
            content = message.content,
            model = message.model
        )
    }
}

/**
 * 用户消息项
 */
@Composable
fun UserMessageItem(content: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = AppColors.UserMessage,
                    shape = RoundedCornerShape(
                        AppConfig.AiAssistant.MESSAGE_CORNER_RADIUS.dp,
                        AppConfig.AiAssistant.MESSAGE_CORNER_RADIUS.dp,
                        4.dp,
                        AppConfig.AiAssistant.MESSAGE_CORNER_RADIUS.dp
                    )
                )
                .padding(12.dp)
                .widthIn(max = AppConfig.AiAssistant.MAX_MESSAGE_WIDTH_DP.dp)
        ) {
            Text(
                text = content,
                color = AppColors.CardBackground,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
