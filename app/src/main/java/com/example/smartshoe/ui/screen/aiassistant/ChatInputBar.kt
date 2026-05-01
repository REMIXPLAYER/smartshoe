package com.example.smartshoe.ui.screen.aiassistant

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.smartshoe.config.AppConfig
import com.example.smartshoe.ui.theme.AppColors

/**
 * 底部输入栏 - 圆角卡片外框，内侧无圆角输入框，无点击阴影动画
 */
@Composable
fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onHistoryClick: () -> Unit,
    onCancel: () -> Unit,  // 取消AI生成回调
    isLoading: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(AppConfig.AiAssistant.MESSAGE_CORNER_RADIUS.dp),
            colors = CardDefaults.cardColors(
                containerColor = AppColors.Surface
            ),
            // 固定elevation，不随状态变化
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 历史按钮 - 使用普通Icon去除点击阴影
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.List,
                    contentDescription = "选择历史记录",
                    tint = if (isLoading)
                        AppColors.OnSurface.copy(alpha = 0.3f)
                    else
                        AppColors.Primary,
                    modifier = Modifier
                        .size(40.dp)
                        .padding(8.dp)
                        .clickable(
                            enabled = !isLoading,
                            indication = null,  // 去除点击阴影
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = onHistoryClick
                        )
                )

                Spacer(modifier = Modifier.width(4.dp))

                // 使用BasicTextField，统一圆角样式
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    enabled = !isLoading,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = AppColors.OnSurface
                    ),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    color = AppColors.Background,
                                    shape = RoundedCornerShape(AppConfig.AiAssistant.MESSAGE_CORNER_RADIUS.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (value.isEmpty()) {
                                Text(
                                    text = "输入消息或选择历史记录分析...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AppColors.OnSurface.copy(alpha = 0.5f)
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                Spacer(modifier = Modifier.width(8.dp))

                // 发送/停止按钮 - 使用普通Icon去除点击阴影
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .padding(8.dp)
                        .background(
                            color = if (isLoading || value.isNotBlank())
                                AppColors.Primary
                            else
                                Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable(
                            enabled = isLoading || value.isNotBlank(),
                            indication = null,  // 去除点击阴影
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = {
                                if (isLoading) {
                                    onCancel()  // 加载中点击 = 取消生成
                                } else {
                                    onSend()    // 非加载中点击 = 发送消息
                                }
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        // 加载中显示方形停止图标
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(
                                    color = AppColors.CardBackground,
                                    shape = RoundedCornerShape(2.dp)
                                )
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "发送",
                            tint = if (value.isNotBlank())
                                AppColors.CardBackground
                            else
                                AppColors.OnSurface.copy(alpha = 0.3f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
