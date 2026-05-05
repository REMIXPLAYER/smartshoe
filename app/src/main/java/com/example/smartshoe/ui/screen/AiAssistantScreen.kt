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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smartshoe.ui.screen.aiassistant.*
import com.example.smartshoe.ui.theme.AppColors
import com.example.smartshoe.ui.theme.AppDimensions
import com.example.smartshoe.ui.viewmodel.AiAssistantViewModel
import com.example.smartshoe.domain.model.ChatMessage
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

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
    val currentConversationId by viewModel.currentConversationId.collectAsStateWithLifecycle()
    val lastReadPosition by viewModel.lastReadPosition.collectAsStateWithLifecycle()
    val restoredConversationId by viewModel.restoredConversationId.collectAsStateWithLifecycle()

    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    var inputText by remember { mutableStateOf("") }
    var isModeMenuExpanded by remember { mutableStateOf(false) }
    var showHistoryBottomSheet by remember { mutableStateOf(false) }

    val isStreaming by remember(messages) {
        derivedStateOf {
            messages.isNotEmpty() && messages.last() is ChatMessage.StreamingAi
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            onShowError(it)
            viewModel.clearError()
        }
    }

    // ==================== 智能滚动管理 ====================
    var isUserActionTriggered by remember { mutableStateOf(false) }
    var isFollowingMode by remember { mutableStateOf(false) }
    var isButtonScrollTriggered by remember { mutableStateOf(false) }
    var isScrollRestored by remember(currentConversationId) { mutableStateOf(false) }

    val isScrollPositionRestored = restoredConversationId == currentConversationId

    LaunchedEffect(scrollState) {
        snapshotFlow { scrollState.isScrollInProgress }
            .collect { isScrolling ->
                if (isScrolling && isStreaming && !isButtonScrollTriggered) {
                    isFollowingMode = false
                }
                if (!isScrolling) {
                    isButtonScrollTriggered = false
                }
            }
    }

    LaunchedEffect(messages.lastOrNull()?.content) {
        if (!isStreaming || !isFollowingMode) return@LaunchedEffect
        delay(25)
        scrollState.scrollTo(scrollState.maxValue)
    }

    LaunchedEffect(messages.lastOrNull()) {
        if (messages.isEmpty()) return@LaunchedEffect
        if (isUserActionTriggered) {
            isUserActionTriggered = false
            isFollowingMode = true
            delay(50)
            scrollState.scrollTo(scrollState.maxValue)
        }
    }

    LaunchedEffect(currentConversationId, messages.isNotEmpty()) {
        val convId = currentConversationId ?: return@LaunchedEffect
        if (isScrollRestored) return@LaunchedEffect
        if (messages.isEmpty()) return@LaunchedEffect

        if (scrollState.maxValue <= 0) {
            withTimeoutOrNull(2000L) {
                snapshotFlow { scrollState.maxValue }.first { it > 0 }
            } ?: run {
                isScrollRestored = true
                viewModel.markConversationRestored(convId)
                return@LaunchedEffect
            }
        }

        if (lastReadPosition < 0f) {
            scrollState.scrollTo(scrollState.maxValue)
            isFollowingMode = true
        } else {
            val targetPosition = (scrollState.maxValue * lastReadPosition).toInt()
            scrollState.scrollTo(targetPosition)
            isFollowingMode = lastReadPosition >= 0.9f
        }
        isScrollRestored = true
        viewModel.markConversationRestored(convId)
    }

    LaunchedEffect(currentConversationId) {
        snapshotFlow { scrollState.isScrollInProgress }
            .collect { isScrolling ->
                if (!isScrolling && restoredConversationId == currentConversationId && scrollState.maxValue > 0) {
                    val progress = scrollState.value.toFloat() / scrollState.maxValue.toFloat()
                    viewModel.saveLastReadPosition(progress)
                }
            }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (scrollState.maxValue > 0) {
                val progress = scrollState.value.toFloat() / scrollState.maxValue.toFloat()
                viewModel.saveLastReadPosition(progress)
            }
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
                    .padding(bottom = 80.dp)
                    .graphicsLayer { alpha = if (isScrollRestored) 1f else 0f },
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
                    if (messages.isEmpty()) return@derivedStateOf false
                    if (!isScrollRestored) return@derivedStateOf false
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
