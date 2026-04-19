package com.example.smartshoe.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartshoe.domain.model.AiStatusResult
import com.example.smartshoe.domain.model.HealthAdviceResult
import com.example.smartshoe.domain.model.SseConnectionState
import com.example.smartshoe.domain.model.SseEvent
import com.example.smartshoe.config.AppConfig
import com.example.smartshoe.domain.model.SensorDataRecord
import com.example.smartshoe.domain.usecase.ai.AnalyzeRecordUseCase
import com.example.smartshoe.domain.usecase.ai.CheckAiStatusUseCase
import com.example.smartshoe.domain.usecase.ai.GetHealthAdviceUseCase
import com.example.smartshoe.domain.usecase.ai.SendMessageUseCase
import com.example.smartshoe.domain.usecase.sensor.GetHistoryRecordsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.lang.ref.WeakReference
import java.util.Collections
import javax.inject.Inject

/**
 * AI 助手 ViewModel
 * 管理 AI 对话和健康建议的状态
 * 
 * 重构：使用 UseCase 解耦，符合 Clean Architecture
 */
@HiltViewModel
class AiAssistantViewModel @Inject constructor(
    val savedStateHandle: SavedStateHandle,
    private val sendMessageUseCase: SendMessageUseCase,
    private val getHealthAdviceUseCase: GetHealthAdviceUseCase,
    private val checkAiStatusUseCase: CheckAiStatusUseCase,
    private val analyzeRecordUseCase: AnalyzeRecordUseCase,
    private val getHistoryRecordsUseCase: GetHistoryRecordsUseCase
) : ViewModel() {

    companion object {
        private const val KEY_WELCOME_INITIALIZED = "welcome_initialized"
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

    // 历史记录列表（用于选择）
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

    // 错误回调 - 使用WeakReference避免内存泄漏
    private var onErrorRef: WeakReference<((String) -> Unit)>? = null
    
    var onError: ((String) -> Unit)?
        get() = onErrorRef?.get()
        set(value) {
            onErrorRef = value?.let { WeakReference(it) }
        }

    init {
        checkAiStatus()
        initWelcomeMessage()
    }

    /**
     * 检查 AI 服务状态
     */
    fun checkAiStatus() {
        viewModelScope.launch {
            _aiStatus.value = AiServiceStatus.Checking
            when (val result = checkAiStatusUseCase()) {
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
     * 优化：批量更新减少UI重组，支持取消，保留错误消息
     */
    fun sendMessage(message: String, token: String) {
        if (message.isBlank()) return

        // 取消之前的请求
        currentSseJob?.cancel()

        currentSseJob = viewModelScope.launch {
            // 添加用户消息
            addMessage(ChatMessage.User(message))

            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            // 创建流式AI消息
            val streamingMessage = ChatMessage.StreamingAi("", AppConfig.AiAssistant.DEFAULT_MODEL)
            addMessage(streamingMessage)

            var fullResponse = ""
            var modelName = AppConfig.AiAssistant.DEFAULT_MODEL

            // 使用 UseCase 进行流式传输，符合 Clean Architecture
            sendMessageUseCase(
                message = message,
                token = token,
                enableThinking = _enableThinking.value,
                onStateChange = { state ->
                    _connectionState.value = state
                }
            ).collect { event ->
                when (event) {
                    is SseEvent.Data -> {
                        // 累积响应内容
                        fullResponse += event.content

                        // 关键改进：移除节流机制，每次内容变化都立即更新 UI
                        // 这样 snapshotFlow 才能正确检测到变化并触发滚动
                        val currentMessages = _messages.value
                        val lastMsg = currentMessages.lastOrNull()
                        val timestamp = if (lastMsg is ChatMessage.StreamingAi) {
                            lastMsg.timestamp  // 保持 timestamp 不变，确保 LazyColumn 的 key 稳定
                        } else {
                            System.currentTimeMillis()
                        }
                        updateLastMessage(ChatMessage.StreamingAi(
                            content = fullResponse,
                            model = modelName,
                            timestamp = timestamp
                        ))
                    }
                    is SseEvent.Complete -> {
                        modelName = event.model
                        val generationTimeMs = event.duration / AppConfig.AiAssistant.NANOSECONDS_TO_MILLISECONDS
                        // 直接将流式消息转换为普通AI消息，避免两次更新
                        replaceLastMessage(ChatMessage.Ai(
                            content = fullResponse,
                            model = modelName,
                            generationTimeMs = generationTimeMs
                        ))
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            lastGenerationTimeMs = generationTimeMs
                        )
                        _connectionState.value = SseConnectionState.Idle
                        currentSseJob = null  // 重置任务引用
                    }
                    is SseEvent.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = event.message
                        )
                        onError?.invoke(event.message)
                        // 将流式消息转换为错误消息，而不是移除
                        if (fullResponse.isNotEmpty()) {
                            replaceLastMessage(ChatMessage.Ai(
                                content = fullResponse + "\n\n[生成中断: ${event.message}]",
                                model = modelName,
                                generationTimeMs = AppConfig.AiAssistant.DEFAULT_TIMESTAMP
                            ))
                        } else {
                            replaceLastMessage(ChatMessage.Ai(
                                content = "[生成失败: ${event.message}]",
                                model = AppConfig.AiAssistant.ERROR_MODEL,
                                generationTimeMs = AppConfig.AiAssistant.DEFAULT_TIMESTAMP
                            ))
                        }
                        _connectionState.value = SseConnectionState.Error(event.message)
                        currentSseJob = null  // 重置任务引用
                    }
                }
            }
        }
    }

    /**
     * 取消当前SSE请求
     * 修复：使用 Mutex 防止竞态条件，确保消息列表操作的原子性
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
                        val newList = currentMessages.toMutableList().apply {
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
     * 更新最后一条消息（用于流式更新）
     * 优化：使用 Mutex 保护，避免竞态条件
     */
    private suspend fun updateLastMessage(message: ChatMessage) {
        messagesMutex.withLock {
            val currentList = _messages.value
            if (currentList.isNotEmpty()) {
                val newList = currentList.toMutableList().apply {
                    this[size - 1] = message
                }
                _messages.value = newList
            }
        }
    }

    /**
     * 替换最后一条消息
     * 优化：使用 Mutex 保护，避免竞态条件
     */
    private suspend fun replaceLastMessage(message: ChatMessage) {
        messagesMutex.withLock {
            val currentList = _messages.value
            if (currentList.isNotEmpty()) {
                val newList = currentList.toMutableList().apply {
                    removeAt(size - 1)
                    add(message)
                }
                _messages.value = newList
            }
        }
    }

    /**
     * 初始化欢迎消息（在ViewModel init中调用）
     * 使用SavedStateHandle确保配置变化后不会重复添加
     * 使用 Mutex 保护消息列表操作
     */
    private fun initWelcomeMessage() {
        // 检查是否已经初始化过（使用SavedStateHandle持久化）
        val isWelcomeInitialized = savedStateHandle.get<Boolean>(KEY_WELCOME_INITIALIZED) ?: false

        if (!isWelcomeInitialized && _messages.value.isEmpty()) {
            viewModelScope.launch {
                addMessage(ChatMessage.Ai(
                    "👋 您好！我是您的足部健康AI助手。\n\n" +
                    "我可以通过分析您的智能鞋垫传感器数据，为您提供：\n" +
                    "• 📊 足部压力分布分析\n" +
                    "• ⚠️ 异常步态检测\n" +
                    "• 💡 个性化健康建议\n\n" +
                    "请直接输入您的问题，或前往历史记录页面选择数据获取健康建议。",
                    "AI助手"
                ))
            }
            // 标记已初始化
            savedStateHandle[KEY_WELCOME_INITIALIZED] = true
        }
    }

    /**
     * 获取健康建议
     */
    fun getHealthAdvice(recordId: String, token: String, userAge: Int? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            when (val result = getHealthAdviceUseCase(recordId, token, userAge)) {
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
     * 添加消息到列表
     * 限制最大消息数，防止内存无限增长
     * 策略：保留第一条欢迎消息，移除最旧的用户/AI消息
     * 使用 Mutex 保护，避免竞态条件
     */
    private suspend fun addMessage(message: ChatMessage) {
        messagesMutex.withLock {
            val currentList = _messages.value
            val newList = if (currentList.size >= AppConfig.AiAssistant.MAX_MESSAGE_COUNT) {
                // 保留第一条欢迎消息，移除第二条（最旧的用户/AI消息）
                listOf(currentList[0]) + currentList.drop(2).takeLast(AppConfig.AiAssistant.MAX_MESSAGE_COUNT - 2) + message
            } else {
                currentList + message
            }
            _messages.value = newList
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
     * 加载用户历史记录列表
     * 使用 UseCase 解耦，符合 Clean Architecture
     */
    fun loadHistoryRecords(token: String) {
        _isLoadingHistory.value = true
        getHistoryRecordsUseCase(
            page = 0,
            size = 20,
            useCache = true,
            onResult = { success, message, records, total ->
                if (success && records != null) {
                    _historyRecords.value = records
                } else {
                    onError?.invoke("加载历史记录失败: $message")
                }
                _isLoadingHistory.value = false
            }
        )
    }

    /**
     * 分析指定记录并获取AI建议（流式版本）
     * 带缓存：避免重复分析同一条记录
     */
    fun analyzeRecord(recordId: String, token: String) {
        // 取消之前的请求
        currentSseJob?.cancel()

        viewModelScope.launch {
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

            var fullResponse = ""
            var modelName = AppConfig.AiAssistant.DEFAULT_MODEL
            var updateCounter = 0

            // 使用 UseCase 进行流式分析，符合 Clean Architecture
            currentSseJob = launch {
                analyzeRecordUseCase(
                    recordId = recordId,
                    token = token,
                    enableThinking = _enableThinking.value,
                    onStateChange = { state ->
                        _connectionState.value = state
                    }
                ).collect { event ->
                    when (event) {
                        is SseEvent.Data -> {
                            fullResponse += event.content
                            updateCounter++

                            // 批量更新UI
                            if (updateCounter >= AppConfig.AiAssistant.UPDATE_INTERVAL ||
                                event.content.contains(Regex("[。！？.!?\\n]"))
                            ) {
                                // 使用相同的timestamp更新，确保LazyColumn的key稳定
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
                                updateCounter = 0
                            }
                        }
                        is SseEvent.Complete -> {
                            modelName = event.model
                            val generationTimeMs = event.duration / AppConfig.AiAssistant.NANOSECONDS_TO_MILLISECONDS
                            // 直接将流式消息转换为AI消息，避免两次更新
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
                            _connectionState.value = SseConnectionState.Idle
                            currentSseJob = null  // 重置任务引用
                        }
                        is SseEvent.Error -> {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = event.message
                            )
                            onError?.invoke(event.message)
                            // 保留已生成的内容
                            if (fullResponse.isNotEmpty()) {
                                replaceLastMessage(ChatMessage.Ai(
                                    content = fullResponse + "\n\n[分析中断: ${event.message}]",
                                    model = modelName,
                                    generationTimeMs = AppConfig.AiAssistant.DEFAULT_TIMESTAMP
                                ))
                            } else {
                                replaceLastMessage(ChatMessage.Ai(
                                    content = "[分析失败: ${event.message}]",
                                    model = AppConfig.AiAssistant.ERROR_MODEL,
                                    generationTimeMs = AppConfig.AiAssistant.DEFAULT_TIMESTAMP
                                ))
                            }
                            _connectionState.value = SseConnectionState.Error(event.message)
                            currentSseJob = null  // 重置任务引用
                        }
                    }
                }
            }
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

/**
 * 对话消息
 */
sealed class ChatMessage {
    abstract val content: String
    abstract val timestamp: Long

    data class User(
        override val content: String,
        override val timestamp: Long = System.currentTimeMillis()
    ) : ChatMessage()

    data class Ai(
        override val content: String,
        val model: String,
        val generationTimeMs: Long = 0,
        override val timestamp: Long = System.currentTimeMillis()
    ) : ChatMessage()

    data class HealthAdvice(
        override val content: String,
        val summary: com.example.smartshoe.domain.model.HealthAdviceSummary?,
        val model: String,
        val generationTimeMs: Long = 0,
        override val timestamp: Long = System.currentTimeMillis()
    ) : ChatMessage()

    /**
     * 流式AI消息 - 用于实时显示生成中的内容
     */
    data class StreamingAi(
        override val content: String,
        val model: String,
        override val timestamp: Long = System.currentTimeMillis()
    ) : ChatMessage()
}
