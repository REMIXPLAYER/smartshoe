package com.example.smartshoe.ui.screen.main.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartshoe.ui.theme.AppColors
import kotlinx.coroutines.delay


@Composable
fun PressureAlertBanner(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var visible by remember(message) { mutableStateOf(false) }
    val launchedOnce = remember { mutableStateOf(false) }

    // 触发滑入动画，4秒后自动关闭
    LaunchedEffect(message) {
        visible = true
        delay(4000)
        visible = false
    }

    // 感知退出动画完成后再通知 ViewModel 清除状态
    // 跳过首次 composition（初始 visible = false）
    LaunchedEffect(visible) {
        if (launchedOnce.value && !visible) {
            delay(300)
            onDismiss()
        }
        launchedOnce.value = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { -it },
        exit = slideOutVertically { -it },
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 80.dp, bottom = 8.dp),
            shape = RoundedCornerShape(12.dp),
            color = AppColors.Surface,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = AppColors.Warning,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                val annotatedMessage = remember(message) {
                    val sensorKeywords = listOf("脚掌前部", "脚弓部", "脚跟部")
                    buildAnnotatedString {
                        var remaining = message
                        while (remaining.isNotEmpty()) {
                            val match = sensorKeywords.firstOrNull { remaining.startsWith(it) }
                            if (match != null) {
                                withStyle(SpanStyle(color = AppColors.Error, fontWeight = FontWeight.Bold)) {
                                    append(match)
                                }
                                remaining = remaining.removePrefix(match)
                            } else {
                                val nextKeywordIndex = sensorKeywords
                                    .mapNotNull { remaining.indexOf(it).takeIf { it >= 0 } }
                                    .minOrNull() ?: remaining.length
                                append(remaining.substring(0, nextKeywordIndex))
                                remaining = remaining.substring(nextKeywordIndex)
                            }
                        }
                    }
                }

                Text(
                    text = annotatedMessage,
                    color = AppColors.OnSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { visible = false },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭",
                        tint = AppColors.PlaceholderText,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
