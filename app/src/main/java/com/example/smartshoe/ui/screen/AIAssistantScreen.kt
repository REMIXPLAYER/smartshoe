package com.example.smartshoe.ui.screen

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smartshoe.llm.LLMManager
import com.example.smartshoe.R
import com.example.smartshoe.llm.ModelDownloadManager
import com.example.smartshoe.ui.theme.AppColors
import com.example.smartshoe.ui.viewmodel.AIAssistantViewModel

@Composable
fun AIAssistantScreen(
    viewModel: AIAssistantViewModel = hiltViewModel()
) {
    val modelState by viewModel.modelState.collectAsStateWithLifecycle()
    val downloadState by viewModel.downloadState.collectAsStateWithLifecycle()
    val generationState by viewModel.generationState.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()

    var inputText by remember { mutableStateOf(TextFieldValue()) }
    val listState = rememberLazyListState()

    // 自动滚动
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // 下载对话框
    val showDownloadDialog = downloadState is ModelDownloadManager.DownloadState.Downloading ||
            downloadState is ModelDownloadManager.DownloadState.Checking ||
            downloadState is ModelDownloadManager.DownloadState.Verifying ||
            (downloadState is ModelDownloadManager.DownloadState.Error &&
                    modelState is LLMManager.ModelState.Downloading)

    if (showDownloadDialog) {
        ModelDownloadDialog(
            state = downloadState,
            onDismiss = { /* 下载中禁止关闭 */ },
            onRetry = { viewModel.downloadModel() },
            onCancel = { viewModel.cancelDownload() }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // 状态栏
        ModelStatusBar(
            state = modelState,
            onDownloadClick = { viewModel.downloadModel() }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 消息列表
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages) { message ->
                MessageBubble(message)
            }

            if (generationState is LLMManager.GenerationState.Generating) {
                item { TypingIndicator() }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 输入框
        ChatInput(
            value = inputText,
            onValueChange = { inputText = it },
            onSend = {
                if (inputText.text.isNotBlank()) {
                    viewModel.sendMessage(inputText.text)
                    inputText = TextFieldValue()
                }
            },
            enabled = modelState is LLMManager.ModelState.Ready &&
                    generationState !is LLMManager.GenerationState.Generating
        )
    }
}

@Composable
private fun ModelStatusBar(
    state: LLMManager.ModelState,
    onDownloadClick: () -> Unit
) {
    Surface(
        color = when (state) {
            is LLMManager.ModelState.Ready -> AppColors.Success.copy(alpha = 0.1f)
            is LLMManager.ModelState.Error -> AppColors.Error.copy(alpha = 0.1f)
            else -> AppColors.Primary.copy(alpha = 0.1f)
        },
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val (text, color) = when (state) {
                is LLMManager.ModelState.NotDownloaded ->
                    "模型未下载 (~336MB)" to Color.Gray
                is LLMManager.ModelState.Downloading ->
                    "下载中..." to AppColors.Primary
                is LLMManager.ModelState.Downloaded ->
                    "已下载，点击加载" to AppColors.Primary
                is LLMManager.ModelState.Loading ->
                    "加载中..." to AppColors.Primary
                is LLMManager.ModelState.Ready ->
                    "AI 助手就绪" to AppColors.Success
                is LLMManager.ModelState.Error ->
                    (state as LLMManager.ModelState.Error).message to AppColors.Error
            }

            Text(
                text = text,
                color = color,
                style = MaterialTheme.typography.labelMedium
            )

            // 操作按钮
            when (state) {
                is LLMManager.ModelState.NotDownloaded -> {
                    Button(
                        onClick = onDownloadClick,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.download),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("下载", style = MaterialTheme.typography.labelSmall)
                    }
                }

                is LLMManager.ModelState.Downloaded -> {
                    Button(
                        onClick = onDownloadClick,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("加载模型", style = MaterialTheme.typography.labelSmall)
                    }
                }

                else -> {}
            }
        }
    }
}

@Composable
private fun ChatInput(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onSend: () -> Unit,
    enabled: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("输入问题...") },
            enabled = enabled,
            singleLine = true
        )

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
            onClick = onSend,
            enabled = enabled && value.text.isNotBlank()
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "发送"
            )
        }
    }
}

@Composable
private fun MessageBubble(message: Message) {
    val isUser = message.isUser
    val backgroundColor = if (isUser) AppColors.Primary else Color(0xFFF5F5F5)
    val textColor = if (isUser) Color.White else Color.Black

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(
            color = backgroundColor,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Text(
                text = message.content,
                color = textColor,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@Composable
private fun TypingIndicator() {
    Row(
        modifier = Modifier.padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(3) { index ->
            val alpha by rememberInfiniteTransition(label = "").animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(index * 200)
                ),
                label = ""
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .alpha(alpha)
                    .background(Color.Gray, MaterialTheme.shapes.small)
            )
        }
    }
}

@Composable
fun ModelDownloadDialog(
    state: ModelDownloadManager.DownloadState,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onCancel: () -> Unit
) {
    val dismissOnBackPress = state is ModelDownloadManager.DownloadState.Error ||
            state is ModelDownloadManager.DownloadState.Cancelled

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = dismissOnBackPress,
            dismissOnClickOutside = dismissOnBackPress
        )
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .width(320.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (state) {
                    is ModelDownloadManager.DownloadState.Idle,
                    is ModelDownloadManager.DownloadState.Checking -> {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("准备下载...")
                    }

                    is ModelDownloadManager.DownloadState.Downloading -> {
                        DownloadProgressContent(state)

                        Spacer(modifier = Modifier.height(16.dp))

                        TextButton(onClick = onCancel) {
                            Text("取消下载")
                        }
                    }

                    is ModelDownloadManager.DownloadState.Verifying -> {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("验证文件完整性...")
                    }

                    is ModelDownloadManager.DownloadState.Success -> {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = AppColors.Success,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("下载完成！")
                    }

                    is ModelDownloadManager.DownloadState.Error -> {
                        Icon(
                            painter = painterResource(R.drawable.error),
                            contentDescription = null,
                            tint = AppColors.Error,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            state.message,
                            textAlign = TextAlign.Center,
                            color = AppColors.Error
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextButton(onClick = onDismiss) {
                                Text("关闭")
                            }

                            if (state.retryable) {
                                Button(onClick = onRetry) {
                                    Text("重试")
                                }
                            }
                        }
                    }

                    is ModelDownloadManager.DownloadState.Cancelled -> {
                        Text("下载已取消")
                        Spacer(modifier = Modifier.height(16.dp))
                        TextButton(onClick = onDismiss) {
                            Text("关闭")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadProgressContent(state: ModelDownloadManager.DownloadState.Downloading) {
    val progress = state.progress
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        label = "progress"
    )

    Text(
        text = "下载模型文件",
        style = MaterialTheme.typography.titleMedium
    )

    Spacer(modifier = Modifier.height(16.dp))

    LinearProgressIndicator(
        progress = { animatedProgress },
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp),
        strokeCap = StrokeCap.Round
    )

    Spacer(modifier = Modifier.height(12.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "${(progress * 100).toInt()}%",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "${formatBytes(state.downloadedBytes)} / ${formatBytes(state.totalBytes)}",
            style = MaterialTheme.typography.bodyMedium
        )
    }

    if (state.speedKbps > 0) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "速度: ${formatSpeed(state.speedKbps)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatBytes(bytes: Long): String {
    val mb = bytes / (1024 * 1024)
    return "$mb MB"
}

private fun formatSpeed(kbps: Double): String {
    return when {
        kbps > 1024 -> String.format("%.2f MB/s", kbps / 1024)
        else -> String.format("%.0f KB/s", kbps)
    }
}

data class Message(
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
