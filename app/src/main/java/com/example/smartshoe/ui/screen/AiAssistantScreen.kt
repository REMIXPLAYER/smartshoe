package com.example.smartshoe.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smartshoe.ui.screen.aiassistant.*
import com.example.smartshoe.ui.theme.AppColors
import com.example.smartshoe.ui.theme.AppDimensions
import com.example.smartshoe.ui.viewmodel.AiAssistantViewModel
import com.example.smartshoe.ui.viewmodel.ChatMessage
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * AI助手屏幕主入口
 * 提供与AI模型的对话界面，支持快速/思考模式切换
 * 新增：支持选择历史记录进行数据分析
 *
 * 架构：主Screen文件只负责组合子组件和状态管理，
 * 具体UI实现拆分到 screen/aiassistant/ 目录
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

    // ==================== 智能滚动管理 ====================
    var isFirstLoad by remember { mutableStateOf(true) }
    var isUserActionTriggered by remember { mutableStateOf(false) }
    var isFollowingMode by remember { mutableStateOf(false) }
    var isButtonScrollTriggered by remember { mutableStateOf(false) }

    // 检测用户主动滚动 → 禁用跟随模式
    LaunchedEffect(scrollState) {
        snapshotFlow { scrollState.isScrollInProgress }
            .collect { isScrolling ->
                if (isScrolling && isStreaming && !isButtonScrollTriggered) {
                    isFollowingMode = false
                }
            }
    }

    // 监听滚动结束，重置按钮触发标志
    LaunchedEffect(scrollState.value) {
        snapshotFlow { scrollState.isScrollInProgress }
            .collect { isScrolling ->
                if (!isScrolling) {
                    isButtonScrollTriggered = false
                }
            }
    }

    // 流式传输跟随逻辑
    LaunchedEffect(messages.lastOrNull()?.content) {
        if (!isStreaming || !isFollowingMode) return@LaunchedEffect
        delay(25)
        scrollState.scrollTo(scrollState.maxValue)
    }

    // 用户发送消息/选择历史记录后滚动到底部并启用跟随模式
    LaunchedEffect(messages.lastOrNull()) {
        if (messages.isEmpty()) return@LaunchedEffect
        if (isUserActionTriggered) {
            isUserActionTriggered = false
            isFollowingMode = true
            delay(50)
            scrollState.scrollTo(scrollState.maxValue)
        }
    }

    // 首次加载完成后标记
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
                        isUserActionTriggered = true
                        viewModel.sendMessage(inputText, token)
                        inputText = ""
                    }
                },
                onHistoryClick = {
                    showHistoryBottomSheet = true
                    viewModel.loadHistoryRecords(token)
                },
                onCancel = { viewModel.cancelCurrentRequest() },
                isLoading = uiState.isLoading
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.Background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(paddingValues)
                    .padding(horizontal = AppDimensions.DefaultPadding)
                    .padding(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Spacer(modifier = Modifier.height(76.dp))

                messages.forEach { message ->
                    ChatMessageItem(message = message)
                }

                Spacer(modifier = Modifier.height(100.dp))
            }

            // 悬浮的AI模式选择器
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
                    .padding(horizontal = 16.dp)
                    .padding(top = AppDimensions.DefaultPadding)
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
            val showScrollToBottomButton by remember {
                derivedStateOf {
                    if (isFirstLoad) return@derivedStateOf false
                    if (messages.isEmpty()) return@derivedStateOf false
                    val isNearBottom = scrollState.value >= scrollState.maxValue - 500
                    !isNearBottom
                }
            }

            AnimatedVisibility(
                visible = showScrollToBottomButton,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 90.dp),
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
                isUserActionTriggered = true
                viewModel.analyzeRecord(record.recordId, token)
            }
        )
    }
}
