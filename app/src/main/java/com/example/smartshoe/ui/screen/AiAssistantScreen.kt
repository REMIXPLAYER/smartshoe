package com.example.smartshoe.ui.screen

import com.example.smartshoe.R
import dev.jeziellago.compose.markdowntext.MarkdownText
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smartshoe.config.AppConfig
import com.example.smartshoe.domain.model.HealthAdviceSummary
import com.example.smartshoe.domain.model.SensorDataRecord
import com.example.smartshoe.ui.component.ExpandableChevron
import com.example.smartshoe.ui.theme.AppColors
import com.example.smartshoe.ui.theme.AppDimensions
import com.example.smartshoe.ui.viewmodel.AiAssistantViewModel
import com.example.smartshoe.ui.viewmodel.ChatMessage
import com.example.smartshoe.util.DateTimeUtils
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * AI助手屏幕
 * 提供与AI模型的对话界面，支持快速/思考模式切换
 * 新增：支持选择历史记录进行数据分析
 */
@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun AiAssistantScreen(
    modifier: Modifier = Modifier,
    viewModel: AiAssistantViewModel,
    token: String,
    onShowError: (String) -> Unit
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val enableThinking by viewModel.enableThinking.collectAsStateWithLifecycle()
    val historyRecords by viewModel.historyRecords.collectAsStateWithLifecycle()
    val isLoadingHistory by viewModel.isLoadingHistory.collectAsStateWithLifecycle()
    
    // 方案 B：使用普通 Column + verticalScroll
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    
    // UI 状态变量
    var inputText by remember { mutableStateOf("") }
    var isModeMenuExpanded by remember { mutableStateOf(false) }
    var showHistoryBottomSheet by remember { mutableStateOf(false) }
    
    // 判断是否正在流式传输
    val isStreaming by remember(messages) {
        derivedStateOf {
            messages.isNotEmpty() && messages.last() is ChatMessage.StreamingAi
        }
    }
    
    // 错误处理
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            onShowError(it)
            viewModel.clearError()
        }
    }
    
    // ==================== 智能滚动管理（重构后）====================
    // 核心原则：
    // 1. 用户发送消息/历史记录分析 → 自动进入跟随模式
    // 2. 流式传输时用户拖动查看历史 → 禁用跟随模式，显示"新消息"按钮
    // 3. 点击"新消息"按钮 → 重新进入跟随模式
    // 4. 流式传输结束 → "新消息"变成"回到底部"
    // 5. 页面切换 → 保持当前浏览位置

    // 1. 标记是否是首次加载（页面切换后）
    var isFirstLoad by remember { mutableStateOf(true) }

    // 2. 标记是否由用户操作（发送消息/选择历史记录）触发
    var isUserActionTriggered by remember { mutableStateOf(false) }

    // 3. 标记用户是否启用跟随模式
    // - 发送消息后自动启用
    // - 用户滚动时自动禁用
    // - 点击按钮后重新启用
    var isFollowingMode by remember { mutableStateOf(false) }

    // 4. 标记是否是按钮触发的滚动（用于区分按钮滚动和用户主动滚动）
    var isButtonScrollTriggered by remember { mutableStateOf(false) }

    // 5. 检测用户主动滚动 → 禁用跟随模式
    // 使用 snapshotFlow 监听滚动状态，但忽略按钮触发的滚动
    LaunchedEffect(scrollState) {
        snapshotFlow {
            scrollState.isScrollInProgress
        }.collect { isScrolling ->
            // 用户主动滚动时（且不是按钮触发），禁用跟随模式
            if (isScrolling && isStreaming && !isButtonScrollTriggered) {
                isFollowingMode = false
            }
        }
    }

    // 6. 监听滚动结束，重置按钮触发标志
    LaunchedEffect(scrollState.value) {
        snapshotFlow {
            scrollState.isScrollInProgress
        }.collect { isScrolling ->
            if (!isScrolling) {
                isButtonScrollTriggered = false
            }
        }
    }

    // 7. 流式传输跟随逻辑 - 在跟随模式下自动滚动到底部
    LaunchedEffect(messages.lastOrNull()?.content) {
        // 只有在流式传输且处于跟随模式时才自动滚动
        if (!isStreaming || !isFollowingMode) return@LaunchedEffect

        delay(25)
        scrollState.scrollTo(scrollState.maxValue)
    }

    // 8. 用户发送消息/选择历史记录后滚动到底部并启用跟随模式
    LaunchedEffect(messages.lastOrNull()) {
        if (messages.isEmpty()) return@LaunchedEffect

        if (isUserActionTriggered) {
            isUserActionTriggered = false
            isFollowingMode = true  // 用户操作后启用跟随模式
            delay(50)
            scrollState.scrollTo(scrollState.maxValue)
        }
    }

    // 9. 首次加载完成后标记
    LaunchedEffect(Unit) {
        if (isFirstLoad) {
            delay(300)
            isFirstLoad = false
        }
    }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            ChatInputBar(
                value = inputText,
                onValueChange = { inputText = it },
                onSend = {
                    if (inputText.isNotBlank()) {
                        isUserActionTriggered = true  // 标记用户发送消息
                        viewModel.sendMessage(inputText, token)
                        inputText = ""
                    }
                },
                onHistoryClick = {
                    showHistoryBottomSheet = true
                    viewModel.loadHistoryRecords(token)
                },
                onCancel = { viewModel.cancelCurrentRequest() },  // 取消AI生成
                isLoading = uiState.isLoading
            )
        }
    ) { paddingValues ->
        // 修复问题 1&2：正确的 padding 顺序
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.Background)
        ) {
            // 方案 B：使用 Column + verticalScroll 替代 LazyColumn
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(paddingValues)  // 先应用 Scaffold 的 padding
                    .padding(horizontal = AppDimensions.DefaultPadding)  // 与首页保持一致的左右边距
                    .padding(bottom = 80.dp),  // 为底部输入栏预留空间
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 为悬浮的模式选择器预留空间（与首页蓝牙卡片位置一致，使用 DefaultPadding）
                Spacer(modifier = Modifier.height(76.dp))

                // 消息列表
                messages.forEach { message ->
                    ChatMessageItem(message = message)
                }

                Spacer(modifier = Modifier.height(100.dp))  // 底部额外空间，确保最后一条消息可见
            }

            // 悬浮的AI模式选择器 - 与首页蓝牙卡片保持一致的padding
            AiModeSelectorTopBar(
                enableThinking = enableThinking,
                isExpanded = isModeMenuExpanded,
                onExpandToggle = { isModeMenuExpanded = !isModeMenuExpanded },
                onModeSelected = { enableThinkingMode ->
                    viewModel.setEnableThinking(enableThinkingMode)
                    isModeMenuExpanded = false
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp)  // 与首页蓝牙卡片外部padding一致
                    .padding(top = AppDimensions.DefaultPadding)  // 与首页蓝牙卡片顶部padding一致
            )

            // 加载指示器
            if (uiState.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                )
            }

            // 滚动到底部/新消息按钮
            // 核心逻辑：
            // 1. 流式传输时：始终显示"新消息"按钮（用户可点击返回跟随）
            // 2. 非流式传输：显示"回到底部"按钮
            val showScrollToBottomButton by remember {
                derivedStateOf {
                    // 首次加载完成前不显示按钮
                    if (isFirstLoad) return@derivedStateOf false

                    // 消息为空时不显示
                    if (messages.isEmpty()) return@derivedStateOf false

                    // 始终使用 500px 阈值判断
                    val isNearBottom = scrollState.value >= scrollState.maxValue - 500
                    !isNearBottom
                }
            }

            AnimatedVisibility(
                visible = showScrollToBottomButton,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 90.dp),  // 修复：增加 padding 避免被输入栏遮挡
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it }
            ) {
                ScrollToBottomButton(
                    onClick = {
                        scope.launch {
                            isButtonScrollTriggered = true
                            isFollowingMode = true
                            scrollState.animateScrollTo(scrollState.maxValue)
                        }
                    },
                    isStreaming = isStreaming
                )
            }
        }
    }

    // 历史记录选择底部Sheet
    if (showHistoryBottomSheet) {
        HistorySelectionBottomSheet(
            records = historyRecords,
            isLoading = isLoadingHistory,
            onDismiss = { showHistoryBottomSheet = false },
            onRecordSelected = { record ->
                showHistoryBottomSheet = false
                isUserActionTriggered = true  // 标记用户选择历史记录，触发滚动和跟随
                viewModel.analyzeRecord(record.recordId, token)
            }
        )
    }
}

/**
 * 滚动到底部按钮
 */
@Composable
private fun ScrollToBottomButton(
    onClick: () -> Unit,
    isStreaming: Boolean
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier
            .shadow(4.dp, CircleShape),
        shape = CircleShape,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = AppColors.Surface,
            contentColor = AppColors.Primary
        )
    ) {
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = "滚动到底部",
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = if (isStreaming) "新消息" else "回到底部",
            fontSize = 14.sp
        )
    }
}

/**
 * AI模式选择器 - 悬浮设计，展开时覆盖消息内容
 * 样式与蓝牙设备管理器保持一致（方案A：现代简洁风）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AiModeSelectorTopBar(
    enableThinking: Boolean,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    onModeSelected: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(
                    min = 60.dp,
                    max = if (isExpanded) 280.dp else 60.dp
                )
        ) {
            // 标题栏 - 统一60.dp高度，与蓝牙卡片保持一致
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onExpandToggle
                    )
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // AI图标 - 使用 minds 图标
                Icon(
                    painter = painterResource(R.drawable.minds),
                    contentDescription = "AI模式",
                    tint = AppColors.Primary,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                // 主标题 - 统一16.sp SemiBold Primary色
                Text(
                    text = "AI 模式",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.Primary,
                    modifier = Modifier.weight(1f)
                )

                // 右侧：当前模式标签 + 展开图标
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 当前模式标签 - 使用方案B：蓝紫对比
                    val modeLabel = if (enableThinking) "深度思考" else "快速响应"
                    val modeColor = if (enableThinking)
                        AppColors.AiModeDeep      // 明亮紫 - 深度思考
                    else
                        AppColors.AiModeQuick     // 深蓝 - 快速响应

                    Surface(
                        color = modeColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = modeLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = modeColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    // 统一展开图标（使用ExpandableChevron）
                    ExpandableChevron(
                        isExpanded = isExpanded,
                        size = 24.dp,
                        tint = AppColors.OnSurface.copy(alpha = 0.6f)
                    )
                }
            }

            // 展开列表
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(
                    animationSpec = tween(300)
                ),
                exit = shrinkVertically(
                    animationSpec = tween(300)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp)
                ) {
                    ModeOptionItem(
                        title = "快速响应",
                        subtitle = "快速生成回答，适合日常咨询",
                        isSelected = !enableThinking,
                        onClick = { onModeSelected(false) }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ModeOptionItem(
                        title = "深度思考",
                        subtitle = "深入分析问题，适合复杂健康咨询",
                        isSelected = enableThinking,
                        onClick = { onModeSelected(true) }
                    )
                }
            }
        }
    }
}

/**
 * 模式选项项
 */
@Composable
private fun ModeOptionItem(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) AppColors.Primary.copy(alpha = 0.1f) else Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) AppColors.Primary else Color.Transparent
                    )
                    .border(
                        width = 2.dp,
                        color = if (isSelected) AppColors.Primary else AppColors.OnSurface.copy(alpha = 0.3f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                    color = if (isSelected) AppColors.Primary else AppColors.OnSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = AppColors.OnSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

/**
 * 聊天消息项
 */
@Composable
private fun ChatMessageItem(message: ChatMessage) {
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
private fun UserMessageItem(content: String) {
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
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/**
 * 预处理Markdown文本，修复常见的格式问题
 *
 * 修复规则：
 * 1. 标题符号后添加空格：###1. -> ### 1.
 * 2. 移除多余的空行
 * 3. 统一列表项格式
 */
private fun preprocessMarkdown(content: String): String = content
    // 修复标题：在行首匹配1-6个#，如果后面紧跟非空白字符，则添加空格
    .replace(Regex("^(#{1,6})([^#\\s])", RegexOption.MULTILINE), "$1 $2")
    // 修复多个#号后紧跟数字的情况：###1 -> ### 1
    .replace(Regex("(#{3,})(\\d)"), "$1 $2")
    // 移除多余的空行（3个及以上换行符合并为2个）
    .replace(Regex("\n{3,}"), "\n\n")
    // 统一列表项格式：移除前置空格，统一为 "- "
    .replace(Regex("^\\s*[-*]\\s*", RegexOption.MULTILINE), "- ")

/**
 * 自定义Markdown渲染组件，带预处理和样式优化
 */
@Composable
private fun StyledMarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Black,
    style: androidx.compose.ui.text.TextStyle = LocalTextStyle.current
) {
    val processedMarkdown = remember(markdown) { preprocessMarkdown(markdown) }

    MarkdownText(
        markdown = processedMarkdown,
        modifier = modifier,
        style = style.copy(
            color = color,
            lineHeight = 22.sp
        ),
        // 自定义链接颜色
        linkColor = AppColors.Primary
    )
}

/**
 * AI消息项
 */
@Composable
private fun AiMessageItem(content: String, model: String, generationTimeMs: Long = 0) {
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
                color = Color.Black,
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
            color = Color.Gray,
            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
        )
    }
}

/**
 * 流式AI消息项
 */
@Composable
private fun StreamingAiMessageItem(content: String, model: String) {
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
                    color = Color.Black,
                    style = MaterialTheme.typography.bodyMedium
                )
                BlinkingCursor()
            }
        }
        Text(
            text = "生成中...",
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
        )
    }
}

/**
 * 闪烁光标组件
 */
@Composable
private fun BlinkingCursor() {
    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursor_alpha"
    )

    Box(
        modifier = Modifier
            .padding(start = 2.dp, bottom = 2.dp)
            .size(width = 2.dp, height = 16.dp)
            .alpha(alpha)
            .background(AppColors.Cursor)
    )
}

/**
 * 健康建议项
 */
@Composable
private fun HealthAdviceItem(
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
                    color = Color.Black
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
            color = Color.Gray,
            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
        )
    }
}

/**
 * 健康建议数据摘要
 */
@Composable
private fun HealthAdviceSummaryView(summary: HealthAdviceSummary) {
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
private fun SummaryItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray
        )
    }
}

// ==================== 历史记录选择底部Sheet ====================

/**
 * 历史记录选择底部Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistorySelectionBottomSheet(
    records: List<SensorDataRecord>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onRecordSelected: (SensorDataRecord) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppColors.Background,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        // 使用LazyColumn作为根布局，避免与ModalBottomSheet的嵌套滚动
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            // 标题栏
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "选择历史记录",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.Primary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = AppColors.OnSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // 说明文字
            item {
                Text(
                    text = "选择一个历史记录进行AI健康分析",
                    fontSize = 14.sp,
                    color = AppColors.OnSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // 内容区域
            when {
                isLoading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = AppColors.Primary)
                        }
                    }
                }
                records.isEmpty() -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = AppColors.OnSurface.copy(alpha = 0.3f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "暂无历史记录",
                                    fontSize = 16.sp,
                                    color = AppColors.OnSurface.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = "请先采集一些数据",
                                    fontSize = 14.sp,
                                    color = AppColors.OnSurface.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }
                }
                else -> {
                    // 记录列表 - 使用items直接渲染，避免嵌套LazyColumn
                    items(
                        items = records,
                        key = { it.recordId }
                    ) { record ->
                        HistoryRecordItem(
                            record = record,
                            onClick = { onRecordSelected(record) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 历史记录项 - 与HistoryScreen样式保持一致
 * 右侧添加"分析"按钮用于AI助手页面
 */
@Composable
private fun HistoryRecordItem(
    record: SensorDataRecord,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // 与HistoryScreen使用相同的时间格式
                Text(
                    text = DateTimeUtils.formatMonthDayHourMinute(record.startTime),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Primary
                )
                // 与HistoryScreen使用相同的副标题格式：数据点 + 压缩率
                Text(
                    text = "${record.dataCount}个数据点 | 压缩率: ${String.format("%.1f", record.compressionRatio * 100)}%",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            // 右侧"分析"按钮
            FilledTonalButton(
                onClick = onClick,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = AppColors.Primary.copy(alpha = 0.1f),
                    contentColor = AppColors.Primary
                )
            ) {
                Text("分析", fontSize = 13.sp)
            }
        }
    }
}

/**
 * 底部输入栏 - 圆角卡片外框，内侧无圆角输入框，无点击阴影动画
 */
@Composable
private fun ChatInputBar(
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
                androidx.compose.foundation.text.BasicTextField(
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
                                    color = Color.White,
                                    shape = RoundedCornerShape(2.dp)
                                )
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "发送",
                            tint = if (value.isNotBlank())
                                Color.White
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
