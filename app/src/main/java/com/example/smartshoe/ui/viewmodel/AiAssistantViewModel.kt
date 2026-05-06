package com.example.smartshoe.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartshoe.domain.model.AiConversation
import com.example.smartshoe.domain.model.AiStatusResult
import com.example.smartshoe.domain.model.ChatMessage
import com.example.smartshoe.domain.model.HealthAdviceResult
import com.example.smartshoe.domain.model.SseConnectionState
import com.example.smartshoe.domain.model.SseEvent
import com.example.smartshoe.config.AppConfig
import com.example.smartshoe.domain.model.SensorDataRecord
import com.example.smartshoe.domain.repository.AiAssistantRepository
import com.example.smartshoe.domain.repository.AiConversationRepository
import com.example.smartshoe.domain.repository.HistoryRecordRepository
import com.example.smartshoe.domain.usecase.GenerateConversationTitleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.Collections
import javax.inject.Inject

/**
 * AI 助手 ViewModel
 * 管理 AI 对话和健康建议的状态
 *
 * 架构：ViewModel 直接依赖 Repository 接口（领域层），符合 Clean Architecture
 * - 内层（domain）不依赖外层（data/presentation）
 * - 业务逻辑在 ViewModel 中处理，数据访问通过 Repository 接口抽象
 */
@HiltViewModel
class AiAssistantViewModel @Inject constructor(
    val savedStateHandle: SavedStateHandle,
    private val aiAssistantRepository: AiAssistantRepository,
    private val historyRecordRepository: HistoryRecordRepository,
    private val conversationRepository: AiConversationRepository,
    private val generateTitleUseCase: GenerateConversationTitleUseCase
) : ViewModel() {

    companion object {
        private const val KEY_WELCOME_INITIALIZED = "welcome_initialized"
        private const val KEY_CURRENT_CONVERSATION_ID = "current_conversation_id"
    }

    // UI状态
    private val _uiState = MutableStateFlow(AiAssistantUiState())
    val uiState: StateFlow<AiAssistantUiState> = _uiState.asStateFlow()

    // 对话消息列表
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    // AI服务状态
    private val _aiStatus = MutableStateFlow<AiServiceStatus>(AiServiceStatus.Unknown)
    val aiStatus: StateFlow<AiServiceStatus> = _aiStatus.asStateFlow()

    // 深度思考模式开关
    private val _enableThinking = MutableStateFlow(false)
    val enableThinking: StateFlow<Boolean> = _enableThinking.asStateFlow()

    // 历史记录列表（用于选择传感器数据）
    private val _historyRecords = MutableStateFlow<List<SensorDataRecord>>(emptyList())
    val historyRecords: StateFlow<List<SensorDataRecord>> = _historyRecords.asStateFlow()

    // 历史记录加载状态
    private val _isLoadingHistory = MutableStateFlow(false)
    val isLoadingHistory: StateFlow<Boolean> = _isLoadingHistory.asStateFlow()

    // SSE连接状态
    private val _connectionState = MutableStateFlow<SseConnectionState>(SseConnectionState.Idle)
    val connectionState: StateFlow<SseConnectionState> = _connectionState.asStateFlow()

    // 当前SSE任务，用于取消
    private var currentSseJob: Job? = null

    // Mutex 用于保护消息列表的并发修改，防止竞态条件
    private val messagesMutex = Mutex()

    // 错误回调 - ViewModel内部使用
    private var onError: ((String) -> Unit)? = null

    // ==================== 多对话管理 ====================

    // 当前对话ID
    private val _currentConversationId = MutableStateFlow<String?>(null)
    val currentConversationId: StateFlow<String?> = _currentConversationId.asStateFlow()

    // 最后阅读位置
    private val _lastReadPosition = MutableStateFlow(-1f)
    val lastReadPosition: StateFlow<Float> = _lastReadPosition.asStateFlow()

    private val _restoredConversationId = MutableStateFlow<String?>(null)
    val restoredConversationId: StateFlow<String?> = _restoredConversationId.asStateFlow()

    // 所有对话列表
    private val _conversations = MutableStateFlow<List<AiConversation>>(emptyList())
    val conversations: StateFlow<List<AiConversation>> = _conversations.asStateFlow()

    // 预计算的分组对话列表（按时间分类），避免UI层重复计算
    private val _groupedConversations = MutableStateFlow<Map<String, List<AiConversation>>>(emptyMap())
    val groupedConversations: StateFlow<Map<String, List<AiConversation>>> = _groupedConversations.asStateFlow()

    // 对话列表加载状态
    private val _isLoadingConversations = MutableStateFlow(false)
    val isLoadingConversations: StateFlow<Boolean> = _isLoadingConversations.asStateFlow()

    // 搜索关键词
    private val _searchKeyword = MutableStateFlow("")
    val searchKeyword: StateFlow<String> = _searchKeyword.asStateFlow()

    // 延迟更新updatedAt的Job，避免每条消息都触发数据库更新
    private var updateTimeJob: Job? = null

    // 是否显示对话抽屉
    private val _showConversationDrawer = MutableStateFlow(false)
    val showConversationDrawer: StateFlow<Boolean> = _showConversationDrawer.asStateFlow()

    init {
        checkAiStatus()
        loadConversations()
        restoreOrCreateConversation()
    }

    override fun onCleared() {
        super.onCleared()
        currentSseJob?.cancel()
        currentSseJob = null
        updateTimeJob?.cancel()
        updateTimeJob = null
    }

    /**
     * 检查 AI 服务状态
     */
    fun checkAiStatus() {
        viewModelScope.launch {
            _aiStatus.value = AiServiceStatus.Checking
            when (val result = aiAssistantRepository.checkAiStatus()) {
                is AiStatusResult.Success -> {
                    _aiStatus.value = if (result.isAvailable) {
                        AiServiceStatus.Available(result.model)
                    } else {
                        AiServiceStatus.Unavailable
                    }
                }
                is AiStatusResult.Error -> {
                    _aiStatus.value = AiServiceStatus.Error(result.message)
                }
            }
        }
    }

    /**
     * 发送消息给AI（流式版本）
     * 优化：智能节流，平衡流畅度和性能
     * 
     * 节流策略：
     * 1. 标点符号后（。！？.!?）立即更新
     * 2. 换行符后立即更新
     * 3. 累积超过20个字符立即更新
     * 4. 距离上次更新超过50ms时更新
     * 5. 代码块标记（```）立即更新
     */
    fun sendMessage(message: String, token: String) {
        if (message.isBlank()) return

        // 防重入：如果正在加载中，忽略重复请求
        if (_uiState.value.isLoading) {
            return
        }

        // 取消之前的请求
        currentSseJob?.cancel()
        currentSseJob = null

        currentSseJob = viewModelScope.launch {
            // 添加用户消息
            addMessage(ChatMessage.User(message))

            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            // 创建流式AI消息
            val streamingMessage = ChatMessage.StreamingAi("", AppConfig.AiAssistant.DEFAULT_MODEL)
            addMessage(streamingMessage)

            // 使用提取的通用流式处理方法
            val stream = aiAssistantRepository.sendMessageStream(
                message = message,
                token = token,
                enableThinking = _enableThinking.value,
                onStateChange = { state ->
                    _connectionState.value = state
                }
            )

            processSseStream(
                stream = stream,
                onComplete = { fullResponse, modelName, generationTimeMs ->
                    if (fullResponse.isBlank()) {
                        // 如果内容为空，说明服务器未返回有效内容，显示错误信息
                        replaceLastMessage(ChatMessage.Ai(
                            content = "[生成失败: 服务器未返回有效内容]",
                            model = AppConfig.AiAssistant.ERROR_MODEL,
                            generationTimeMs = AppConfig.AiAssistant.DEFAULT_TIMESTAMP
                        ))
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "服务器未返回有效内容"
                        )
                    } else {
                        replaceLastMessage(ChatMessage.Ai(
                            content = fullResponse,
                            model = modelName,
                            generationTimeMs = generationTimeMs
                        ))
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            lastGenerationTimeMs = generationTimeMs
                        )
                    }
                    _connectionState.value = SseConnectionState.Idle
                    currentSseJob = null
                },
                onError = { fullResponse, modelName, errorMessage ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = errorMessage
                    )
                    onError?.invoke(errorMessage)

                    if (fullResponse.isNotEmpty()) {
                        replaceLastMessage(ChatMessage.Ai(
                            content = fullResponse + "\n\n[生成中断: $errorMessage]",
                            model = modelName,
                            generationTimeMs = AppConfig.AiAssistant.DEFAULT_TIMESTAMP
                        ))
                    } else {
                        replaceLastMessage(ChatMessage.Ai(
                            content = "[生成失败: $errorMessage]",
                            model = AppConfig.AiAssistant.ERROR_MODEL,
                            generationTimeMs = AppConfig.AiAssistant.DEFAULT_TIMESTAMP
                        ))
                    }
                    _connectionState.value = SseConnectionState.Error(errorMessage)
                    currentSseJob = null
                }
            )
        }
    }

    /**
     * 取消当前SSE请求
     * 修复：使用 Mutex 防止竞态条件，确保消息列表操作的原子性
     *
     * 性能优化：使用 ArrayList 预分配容量，减少取消时的内存分配
     */
    fun cancelCurrentRequest() {
        // 1. 取消协程任务
        currentSseJob?.cancel()
        currentSseJob = null

        // 2. 重置UI状态
        _uiState.value = _uiState.value.copy(isLoading = false)
        _connectionState.value = SseConnectionState.Idle

        // 3. 使用 Mutex 保护消息列表的修改，防止竞态条件
        viewModelScope.launch {
            messagesMutex.withLock {
                val currentMessages = _messages.value
                val lastMsg = currentMessages.lastOrNull()

                if (lastMsg is ChatMessage.StreamingAi) {
                    if (lastMsg.content.isNotEmpty()) {
                        // 将未完成的流式消息转换为普通消息（带取消标记）
                        // 优化：使用 ArrayList 预分配容量，避免 toMutableList 的扩容开销
                        val newList = ArrayList<ChatMessage>(currentMessages.size).apply {
                            addAll(currentMessages)
                            removeAt(size - 1)
                            add(ChatMessage.Ai(
                                content = lastMsg.content + "\n\n[已停止生成]",
                                model = lastMsg.model,
                                generationTimeMs = 0
                            ))
                        }
                        _messages.value = newList
                    } else {
                        // 如果没有生成任何内容，移除空的流式消息
                        _messages.value = currentMessages.dropLast(1)
                    }
                }
            }
        }
    }

    /**
     * 通用 SSE 流式处理逻辑
     * 提取 sendMessage 和 analyzeRecord 中的公共流式处理代码
     *
     * @param stream SSE 事件流
     * @param onComplete 完成回调，返回 (fullResponse, modelName, generationTimeMs)
     * @param onError 错误回调，返回 (fullResponse, modelName, errorMessage)
     * @return 是否成功完成（true=Complete，false=Error）
     */
    private suspend fun processSseStream(
        stream: Flow<SseEvent>,
        onComplete: suspend (fullResponse: String, modelName: String, generationTimeMs: Long) -> Unit,
        onError: suspend (fullResponse: String, modelName: String, errorMessage: String) -> Unit
    ) {
        var fullResponse = ""
        var modelName = AppConfig.AiAssistant.DEFAULT_MODEL

        // 节流控制变量
        var pendingContent = ""
        var lastUpdateTime = System.currentTimeMillis()
        var charsSinceUpdate = 0

        stream.collect { event ->
            when (event) {
                is SseEvent.Data -> {
                    fullResponse += event.content
                    pendingContent += event.content
                    charsSinceUpdate += event.content.length

                    val currentTime = System.currentTimeMillis()
                    val timeSinceLastUpdate = currentTime - lastUpdateTime

                    val shouldUpdateImmediately = event.content.any { it in AppConfig.AiAssistant.STREAM_IMMEDIATE_UPDATE_CHARS } ||
                            charsSinceUpdate >= AppConfig.AiAssistant.STREAM_MAX_CHARS_BEFORE_UPDATE ||
                            timeSinceLastUpdate >= AppConfig.AiAssistant.STREAM_MIN_UPDATE_INTERVAL_MS

                    if (shouldUpdateImmediately) {
                        val currentMessages = _messages.value
                        val lastMsg = currentMessages.lastOrNull()
                        val timestamp = if (lastMsg is ChatMessage.StreamingAi) {
                            lastMsg.timestamp
                        } else {
                            System.currentTimeMillis()
                        }
                        updateLastMessage(ChatMessage.StreamingAi(
                            content = fullResponse,
                            model = modelName,
                            timestamp = timestamp
                        ))

                        pendingContent = ""
                        lastUpdateTime = currentTime
                        charsSinceUpdate = 0
                    }
                }
                is SseEvent.Complete -> {
                    modelName = event.model
                    val generationTimeMs = event.duration / AppConfig.AiAssistant.NANOSECONDS_TO_MILLISECONDS

                    // 刷新剩余待更新内容
                    if (pendingContent.isNotEmpty()) {
                        val currentMessages = _messages.value
                        val lastMsg = currentMessages.lastOrNull()
                        val timestamp = if (lastMsg is ChatMessage.StreamingAi) {
                            lastMsg.timestamp
                        } else {
                            System.currentTimeMillis()
                        }
                        updateLastMessage(ChatMessage.StreamingAi(
                            content = fullResponse,
                            model = modelName,
                            timestamp = timestamp
                        ))
                    }

                    onComplete(fullResponse, modelName, generationTimeMs)
                }
                is SseEvent.Error -> {
                    // 刷新剩余待更新内容
                    if (pendingContent.isNotEmpty()) {
                        val currentMessages = _messages.value
                        val lastMsg = currentMessages.lastOrNull()
                        val timestamp = if (lastMsg is ChatMessage.StreamingAi) {
                            lastMsg.timestamp
                        } else {
                            System.currentTimeMillis()
                        }
                        updateLastMessage(ChatMessage.StreamingAi(
                            content = fullResponse,
                            model = modelName,
                            timestamp = timestamp
                        ))
                    }

                    onError(fullResponse, modelName, event.message)
                }
            }
        }
    }

    /**
     * 更新最后一条消息（用于流式更新）
     * 优化：使用 Mutex 保护，避免竞态条件
     * 注意：流式消息不保存到数据库，只在内存中更新
     *
     * 性能优化：使用 ArrayList 预分配容量，减少流式更新时的内存分配
     */
    private suspend fun updateLastMessage(message: ChatMessage) {
        messagesMutex.withLock {
            val currentList = _messages.value
            if (currentList.isNotEmpty()) {
                // 优化：使用 ArrayList 预分配容量，避免 toMutableList 的扩容开销
                val newList = ArrayList<ChatMessage>(currentList.size).apply {
                    addAll(currentList)
                    set(size - 1, message)
                }
                _messages.value = newList
            }
        }
    }

    /**
     * 替换最后一条消息（将流式消息替换为最终消息）
     * 优化：使用 Mutex 保护，避免竞态条件
     * 替换后的最终消息会保存到数据库
     *
     * 性能优化：使用 ArrayList 预分配容量，减少替换时的内存分配
     */
    private suspend fun replaceLastMessage(message: ChatMessage) {
        var conversationId: String? = null
        messagesMutex.withLock {
            val currentList = _messages.value
            if (currentList.isNotEmpty()) {
                // 优化：使用 ArrayList 预分配容量，避免 toMutableList 的扩容开销
                val newList = ArrayList<ChatMessage>(currentList.size).apply {
                    addAll(currentList)
                    removeAt(size - 1)
                    add(message)
                }
                _messages.value = newList
                conversationId = _currentConversationId.value
            }
        }

        conversationId?.let { idToSave ->
            if (message !is ChatMessage.StreamingAi) {
                conversationRepository.saveMessage(idToSave, message)
                conversationRepository.updateConversationTime(idToSave)
            }
        }
    }

    // ==================== 对话管理方法 ====================

    /**
     * 加载所有对话列表
     * 在init中调用一次，建立Flow监听
     * 同时预计算分组结果，避免UI层重复计算
     */
    private fun loadConversations() {
        viewModelScope.launch {
            conversationRepository.getAllConversations().collect { list ->
                _conversations.value = list
                _groupedConversations.value = groupConversationsByTime(list)
            }
        }
    }

    /**
     * 按时间分组对话列表
     * 在ViewModel中预计算，避免UI层重复执行
     */
    private fun groupConversationsByTime(conversations: List<AiConversation>): Map<String, List<AiConversation>> {
        val now = System.currentTimeMillis()
        val oneDay = 24 * 60 * 60 * 1000L
        return conversations.groupBy { conversation ->
            val diff = now - conversation.updatedAt
            when {
                diff < oneDay -> "今天"
                diff < 7 * oneDay -> "前7天"
                diff < 30 * oneDay -> "前30天"
                else -> "更早"
            }
        }
    }

    /**
     * 恢复上次对话或创建新对话
     * 优先级：
     * 1. 恢复SavedState中保存的对话ID
     * 2. 切换到数据库中最新对话
     * 3. 创建新对话（首次使用）
     */
    private fun restoreOrCreateConversation() {
        viewModelScope.launch {
            val savedId = savedStateHandle.get<String>(KEY_CURRENT_CONVERSATION_ID)
            if (savedId != null) {
                // 尝试恢复上次对话，并恢复滚动位置
                val conversation = conversationRepository.getAllConversations().first()
                    .find { it.id == savedId }
                conversation?.let {
                    _lastReadPosition.value = it.lastReadPosition
                }
                switchToConversation(savedId)
                return@launch
            }

            // 没有保存的ID，检查数据库中是否已有对话
            val existingConversations = conversationRepository.getAllConversations().first()
            if (existingConversations.isNotEmpty()) {
                val latestConversation = existingConversations.maxByOrNull { it.updatedAt }
                latestConversation?.let {
                    val messageList = conversationRepository.getMessagesByConversationId(it.id).first()
                    _messages.value = messageList
                    _lastReadPosition.value = it.lastReadPosition
                    _currentConversationId.value = it.id
                    savedStateHandle[KEY_CURRENT_CONVERSATION_ID] = it.id
                }
            } else {
                // 首次使用，创建新对话
                createNewConversation()
            }
        }
    }

    /**
     * 创建新对话
     */
    fun createNewConversation() {
        viewModelScope.launch {
            _isLoadingConversations.value = true
            try {
                val newId = conversationRepository.createConversation("新对话")
                savedStateHandle[KEY_CURRENT_CONVERSATION_ID] = newId
                _currentConversationId.value = newId
                _messages.value = emptyList()
                val welcomeMessage = ChatMessage.Ai(
                    "👋 您好！我是您的足部健康AI助手。\n\n" +
                    "我可以通过分析您的智能鞋垫传感器数据，为您提供：\n" +
                    "• 📊 足部压力分布分析\n" +
                    "• ⚠️ 异常步态检测\n" +
                    "• 💡 个性化健康建议\n\n" +
                    "请直接输入您的问题，或选择历史记录进行数据分析。",
                    "AI助手"
                )
                addMessage(welcomeMessage)
                _lastReadPosition.value = -1f
                _restoredConversationId.value = null
            } catch (e: Exception) {
                onError?.invoke("创建对话失败: ${e.message}")
            } finally {
                _isLoadingConversations.value = false
            }
        }
    }

    /**
     * 切换到指定对话
     * 使用first()只获取一次消息列表，避免无限挂起
     */
    fun switchToConversation(conversationId: String) {
        viewModelScope.launch {
            _isLoadingConversations.value = true
            try {
                _currentConversationId.value?.let { currentId ->
                    if (currentId != conversationId) {
                        saveCurrentScrollProgress(currentId, _lastReadPosition.value)
                        _restoredConversationId.value = null
                    }
                }
                val conversation = conversationRepository.getAllConversations().first()
                    .find { it.id == conversationId }
                val messageList = conversationRepository.getMessagesByConversationId(conversationId).first()
                _messages.value = messageList
                conversation?.let {
                    _lastReadPosition.value = it.lastReadPosition
                }
                _currentConversationId.value = conversationId
                savedStateHandle[KEY_CURRENT_CONVERSATION_ID] = conversationId
            } catch (e: Exception) {
                onError?.invoke("切换对话失败: ${e.message}")
            } finally {
                _isLoadingConversations.value = false
            }
        }
    }

    /**
     * 搜索对话
     * 直接更新_conversations，不重复启动Flow collect
     */
    fun searchConversations(keyword: String) {
        _searchKeyword.value = keyword
        viewModelScope.launch {
            try {
                val list = if (keyword.isBlank()) {
                    conversationRepository.getAllConversations().first()
                } else {
                    conversationRepository.searchConversations(keyword).first()
                }
                _conversations.value = list
                _groupedConversations.value = groupConversationsByTime(list)
            } catch (e: Exception) {
                onError?.invoke("搜索对话失败: ${e.message}")
            }
        }
    }

    /**
     * 删除对话
     */
    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            try {
                conversationRepository.deleteConversation(conversationId)
                if (_currentConversationId.value == conversationId) {
                    // 如果删除的是当前对话，切换到最新对话或创建新对话
                    val remainingConversations = conversations.value.filter { it.id != conversationId }
                    if (remainingConversations.isNotEmpty()) {
                        switchToConversation(remainingConversations.first().id)
                    } else {
                        createNewConversation()
                    }
                }
            } catch (e: Exception) {
                onError?.invoke("删除对话失败: ${e.message}")
            }
        }
    }

    /**
     * 更新对话标题（基于第一条用户消息）
     * 使用GenerateConversationTitleUseCase进行智能摘要
     */
    private fun updateConversationTitleFromMessage(content: String) {
        val conversationId = _currentConversationId.value ?: return
        viewModelScope.launch {
            val title = generateTitleUseCase(content)
            try {
                conversationRepository.updateConversationTitle(conversationId, title)
            } catch (e: Exception) {
                // 静默处理，不影响主流程
            }
        }
    }

    /**
     * 获取健康建议
     */
    fun getHealthAdvice(recordId: String, token: String, userAge: Int? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            when (val result = aiAssistantRepository.getHealthAdvice(recordId, token, userAge)) {
                is HealthAdviceResult.Success -> {
                    addMessage(ChatMessage.HealthAdvice(
                        content = result.advice,
                        summary = result.summary,
                        model = result.model,
                        generationTimeMs = result.generationTimeMs
                    ))
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        lastGenerationTimeMs = result.generationTimeMs
                    )
                }
                is HealthAdviceResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                    onError?.invoke(result.message)
                }
            }
        }
    }

    /**
     * 清空对话
     */
    fun clearMessages() {
        _messages.value = emptyList()
    }

    /**
     * 清空AI分析缓存
     */
    fun clearAnalysisCache() {
        analysisCache.clear()
    }

    /**
     * 清空所有AI数据（消息+缓存）
     */
    fun clearAllAiData() {
        updateTimeJob?.cancel()
        updateTimeJob = null
        clearMessages()
        clearAnalysisCache()
        _historyRecords.value = emptyList()
        _uiState.value = AiAssistantUiState()
        _aiStatus.value = AiServiceStatus.Unknown
        _connectionState.value = SseConnectionState.Idle
        currentSseJob?.cancel()
        currentSseJob = null
        savedStateHandle.remove<String>(KEY_CURRENT_CONVERSATION_ID)
        _currentConversationId.value = null
        viewModelScope.launch {
            conversationRepository.deleteAllConversations()
        }
    }

    /**
     * 添加消息到列表
     * 限制最大消息数，防止内存无限增长
     * 策略：保留第一条欢迎消息，移除最旧的用户/AI消息
     * 使用 Mutex 保护，避免竞态条件
     * 同时保存到数据库（流式消息除外）
     *
     * 性能优化：使用 ArrayList 预分配容量，减少添加消息时的内存分配
     */
    private suspend fun addMessage(message: ChatMessage) {
        val currentId = _currentConversationId.value
        val conversations = _conversations.value
        val shouldUpdateTitle = message is ChatMessage.User &&
            (conversations.find { it.id == currentId }?.title == "新对话")
        
        var conversationId: String? = null
        messagesMutex.withLock {
            val currentList = _messages.value
            // 优化：使用 ArrayList 预分配容量，避免列表操作的扩容开销
            val newList = if (currentList.size >= AppConfig.AiAssistant.MAX_MESSAGE_COUNT) {
                ArrayList<ChatMessage>(AppConfig.AiAssistant.MAX_MESSAGE_COUNT).apply {
                    add(currentList[0]) // 保留欢迎消息
                    addAll(currentList.drop(2).takeLast(AppConfig.AiAssistant.MAX_MESSAGE_COUNT - 2))
                    add(message)
                }
            } else {
                ArrayList<ChatMessage>(currentList.size + 1).apply {
                    addAll(currentList)
                    add(message)
                }
            }
            _messages.value = newList
            conversationId = _currentConversationId.value
        }

        conversationId?.let { idToSave ->
            if (message !is ChatMessage.StreamingAi) {
                conversationRepository.saveMessage(idToSave, message)
            }
        }

        // 延迟更新对话时间，避免每条消息都触发数据库更新和Flow重算
        _currentConversationId.value?.let { conversationId ->
            scheduleConversationTimeUpdate(conversationId)
        }

        // 在锁外异步更新标题，实现懒加载，不阻塞消息发送流程
        if (shouldUpdateTitle) {
            updateConversationTitleFromMessage(message.content)
        }
    }

    /**
     * 保存最后阅读位置
     */
    fun saveLastReadPosition(progress: Float) {
        _currentConversationId.value?.let { conversationId ->
            _lastReadPosition.value = progress
            viewModelScope.launch {
                saveCurrentScrollProgress(conversationId, progress)
            }
        }
    }

    private suspend fun saveCurrentScrollProgress(conversationId: String, progress: Float) {
        conversationRepository.updateLastReadPosition(conversationId, progress.coerceIn(0f, 1f))
    }

    fun markConversationRestored(conversationId: String) {
        _restoredConversationId.value = conversationId
    }

    /**
     * 延迟更新对话时间，合并多次更新为一次
     * 避免频繁的数据库写入和Flow重算
     */
    private fun scheduleConversationTimeUpdate(conversationId: String) {
        updateTimeJob?.cancel()
        updateTimeJob = viewModelScope.launch {
            delay(2000) // 延迟2秒，合并短时间内的多次更新
            conversationRepository.updateConversationTime(conversationId)
        }
    }

    /**
     * 缓存的分析结果数据类
     */
    private data class CachedAnalysisResult(
        val advice: String,
        val model: String,
        val generationTimeMs: Long
    )

    /**
     * 分析结果缓存 - 线程安全，带LRU淘汰策略
     * key: recordId, value: 分析结果
     */
    private val analysisCache = Collections.synchronizedMap(
        object : LinkedHashMap<String, CachedAnalysisResult>(
            AppConfig.AiAssistant.CACHE_INITIAL_CAPACITY,
            AppConfig.AiAssistant.CACHE_LOAD_FACTOR,
            AppConfig.AiAssistant.CACHE_ACCESS_ORDER
        ) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CachedAnalysisResult>?): Boolean {
                return size > AppConfig.AiAssistant.MAX_CACHE_SIZE
            }
        }
    )

    /**
     * 清除错误状态
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * 设置深度思考模式开关
     */
    fun setEnableThinking(enabled: Boolean) {
        _enableThinking.value = enabled
    }

    /**
     * 显示对话抽屉
     */
    fun showConversationDrawer() {
        _showConversationDrawer.value = true
    }

    /**
     * 隐藏对话抽屉
     */
    fun hideConversationDrawer() {
        _showConversationDrawer.value = false
    }

    /**
     * 加载用户历史记录列表
     * 统一使用 HistoryRecordRepository 作为入口，与历史记录页面共享缓存
     * 通过 Flow 观察 Repository 状态，确保数据就绪后再更新UI
     *
     * 优化：使用 withTimeoutOrNull 防止 Flow collect 无限挂起
     */
    fun loadHistoryRecords(token: String) {
        _isLoadingHistory.value = true
        viewModelScope.launch {
            try {
                // 先收集一次当前记录列表
                val currentRecords = historyRecordRepository.recordsFlow.value
                if (currentRecords.isNotEmpty()) {
                    _historyRecords.value = currentRecords
                    _isLoadingHistory.value = false
                    return@launch
                }

                // 若为空则触发查询
                historyRecordRepository.queryHistoryRecords(
                    page = 0,
                    append = false,
                    startDate = null,
                    endDate = null
                )

                // 等待加载完成并收集结果，设置30秒超时防止无限挂起
                val records = withTimeoutOrNull(30_000) {
                    historyRecordRepository.recordsFlow.first { records ->
                        records.isNotEmpty() || !historyRecordRepository.isLoadingFlow.value
                    }
                }

                if (records != null) {
                    _historyRecords.value = records
                } else {
                    onError?.invoke("加载历史记录超时，请检查网络连接")
                }
            } catch (e: Exception) {
                onError?.invoke("加载历史记录失败: ${e.message}")
            } finally {
                _isLoadingHistory.value = false
            }
        }
    }

    /**
     * 分析指定记录并获取AI建议（流式版本）
     * 带缓存：避免重复分析同一条记录
     */
    fun analyzeRecord(recordId: String, token: String) {
        // 防重入：如果正在加载中，忽略重复请求
        if (_uiState.value.isLoading) {
            return
        }

        // 取消之前的请求
        currentSseJob?.cancel()
        currentSseJob = null

        currentSseJob = viewModelScope.launch {
            // 检查缓存
            analysisCache[recordId]?.let { cachedResult ->
                addMessage(ChatMessage.User("📊 分析历史记录: $recordId (来自缓存)"))
                addMessage(ChatMessage.Ai(
                    content = cachedResult.advice,
                    model = cachedResult.model,
                    generationTimeMs = cachedResult.generationTimeMs
                ))
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    lastGenerationTimeMs = cachedResult.generationTimeMs
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            // 添加用户消息表示选择了记录
            addMessage(ChatMessage.User("📊 分析历史记录: $recordId"))

            // 创建流式AI消息用于显示分析过程
            val streamingMessage = ChatMessage.StreamingAi("", AppConfig.AiAssistant.DEFAULT_MODEL)
            addMessage(streamingMessage)

            // 使用提取的通用流式处理方法
            val stream = aiAssistantRepository.analyzeRecordStream(
                recordId = recordId,
                token = token,
                enableThinking = _enableThinking.value,
                onStateChange = { state ->
                    _connectionState.value = state
                }
            )

            processSseStream(
                stream = stream,
                onComplete = { fullResponse, modelName, generationTimeMs ->
                    if (fullResponse.isBlank()) {
                        // 如果内容为空，说明服务器未返回有效内容，显示错误信息
                        replaceLastMessage(ChatMessage.Ai(
                            content = "[分析失败: 服务器未返回有效内容]",
                            model = AppConfig.AiAssistant.ERROR_MODEL,
                            generationTimeMs = AppConfig.AiAssistant.DEFAULT_TIMESTAMP
                        ))
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "服务器未返回有效内容"
                        )
                    } else {
                        replaceLastMessage(ChatMessage.Ai(
                            content = fullResponse,
                            model = modelName,
                            generationTimeMs = generationTimeMs
                        ))
                        // 保存到缓存
                        analysisCache[recordId] = CachedAnalysisResult(
                            advice = fullResponse,
                            model = modelName,
                            generationTimeMs = generationTimeMs
                        )
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            lastGenerationTimeMs = generationTimeMs
                        )
                    }
                    _connectionState.value = SseConnectionState.Idle
                    currentSseJob = null
                },
                onError = { fullResponse, modelName, errorMessage ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = errorMessage
                    )
                    onError?.invoke(errorMessage)

                    if (fullResponse.isNotEmpty()) {
                        replaceLastMessage(ChatMessage.Ai(
                            content = fullResponse + "\n\n[分析中断: $errorMessage]",
                            model = modelName,
                            generationTimeMs = AppConfig.AiAssistant.DEFAULT_TIMESTAMP
                        ))
                    } else {
                        replaceLastMessage(ChatMessage.Ai(
                            content = "[分析失败: $errorMessage]",
                            model = AppConfig.AiAssistant.ERROR_MODEL,
                            generationTimeMs = AppConfig.AiAssistant.DEFAULT_TIMESTAMP
                        ))
                    }
                    _connectionState.value = SseConnectionState.Error(errorMessage)
                    currentSseJob = null
                }
            )
        }
    }
}

/**
 * AI助手UI状态
 */
data class AiAssistantUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val lastGenerationTimeMs: Long = 0
)

/**
 * AI服务状态
 */
sealed class AiServiceStatus {
    object Unknown : AiServiceStatus()
    object Checking : AiServiceStatus()
    data class Available(val model: String) : AiServiceStatus()
    object Unavailable : AiServiceStatus()
    data class Error(val message: String) : AiServiceStatus()
}


