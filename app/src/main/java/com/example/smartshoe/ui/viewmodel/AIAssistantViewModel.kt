package com.example.smartshoe.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartshoe.llm.LLMManager
import com.example.smartshoe.llm.ModelDownloadManager
import com.example.smartshoe.ui.screen.Message
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AIAssistantViewModel @Inject constructor(
    private val llmManager: LLMManager
) : ViewModel() {

    val modelState: StateFlow<LLMManager.ModelState> = llmManager.modelState
    val downloadState: StateFlow<ModelDownloadManager.DownloadState> = llmManager.downloadState
    val generationState: StateFlow<LLMManager.GenerationState> = llmManager.generationState

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    init {
        // 检查模型状态
        viewModelScope.launch {
            llmManager.checkModelStatus()
        }

        // 监听生成结果
        viewModelScope.launch {
            generationState.collect { state ->
                when (state) {
                    is LLMManager.GenerationState.Complete -> {
                        _messages.update { it + Message(state.response, isUser = false) }
                    }
                    is LLMManager.GenerationState.Error -> {
                        _messages.update { it + Message("错误: ${state.message}", isUser = false) }
                    }
                    else -> {}
                }
            }
        }
    }

    fun sendMessage(text: String) {
        _messages.update { it + Message(text, isUser = true) }
        llmManager.generate(text)
    }

    fun downloadModel() {
        when (modelState.value) {
            is LLMManager.ModelState.NotDownloaded -> {
                llmManager.downloadAndLoadModel()
            }
            is LLMManager.ModelState.Downloaded,
            is LLMManager.ModelState.Error -> {
                // 已下载或加载失败时，重新尝试加载
                llmManager.loadDownloadedModel()
            }
            else -> {}
        }
    }

    fun deleteModel() {
        llmManager.deleteModel()
    }

    fun cancelDownload() {
        llmManager.cancelDownload()
    }

    fun clearError() {
        // 清除错误状态，重置下载状态
        viewModelScope.launch {
            llmManager.checkModelStatus()
        }
    }

    override fun onCleared() {
        super.onCleared()
        llmManager.release()
    }
}
