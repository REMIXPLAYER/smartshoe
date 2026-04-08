package com.example.smartshoe.llm

import android.content.Context
import android.os.SystemClock
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LLMManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadManager: ModelDownloadManager
) {
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _modelState = MutableStateFlow<ModelState>(ModelState.NotDownloaded)
    val modelState: StateFlow<ModelState> = _modelState.asStateFlow()

    private val _downloadState = MutableStateFlow<ModelDownloadManager.DownloadState>(
        ModelDownloadManager.DownloadState.Idle
    )
    val downloadState: StateFlow<ModelDownloadManager.DownloadState> = _downloadState.asStateFlow()

    private val _generationState = MutableStateFlow<GenerationState>(GenerationState.Idle)
    val generationState: StateFlow<GenerationState> = _generationState.asStateFlow()

    data class ModelConfig(
        val nThreads: Int = Runtime.getRuntime().availableProcessors(),
        val nCtx: Int = 2048,
        val maxTokens: Int = 512,
        val temperature: Float = 0.7f
    )

    private val config = ModelConfig()
    private var currentModel: ModelDownloadManager.ModelInfo = ModelDownloadManager.DEFAULT_MODEL

    sealed class ModelState {
        object NotDownloaded : ModelState()
        object Downloading : ModelState()
        object Downloaded : ModelState()
        object Loading : ModelState()
        data class Ready(val modelPath: String) : ModelState()
        data class Error(val message: String) : ModelState()
    }

    sealed class GenerationState {
        object Idle : GenerationState()
        data class Generating(val prompt: String) : GenerationState()
        data class Complete(val response: String, val tokensPerSecond: Float) : GenerationState()
        data class Error(val message: String) : GenerationState()
    }

    /**
     * 检查模型状态
     */
    suspend fun checkModelStatus(model: ModelDownloadManager.ModelInfo = ModelDownloadManager.DEFAULT_MODEL) {
        currentModel = model
        _modelState.value = when {
            LLMNative.isModelLoaded() -> {
                val file = downloadManager.getModelFile(model.fileName)
                ModelState.Ready(file.absolutePath)
            }
            downloadManager.isModelReady(model) -> ModelState.Downloaded
            else -> ModelState.NotDownloaded
        }
    }

    /**
     * 下载并加载模型
     */
    fun downloadAndLoadModel(model: ModelDownloadManager.ModelInfo = ModelDownloadManager.DEFAULT_MODEL) {
        if (_modelState.value == ModelState.Downloading ||
            _modelState.value == ModelState.Loading
        ) {
            return
        }

        currentModel = model
        _modelState.value = ModelState.Downloading

        coroutineScope.launch {
            downloadManager.downloadModel(model).collect { state ->
                _downloadState.value = state

                when (state) {
                    is ModelDownloadManager.DownloadState.Success -> {
                        _modelState.value = ModelState.Downloaded
                        loadModel(state.file)
                    }
                    is ModelDownloadManager.DownloadState.Error -> {
                        _modelState.value = ModelState.Error(state.message)
                    }
                    is ModelDownloadManager.DownloadState.Cancelled -> {
                        _modelState.value = ModelState.NotDownloaded
                    }
                    else -> {}
                }
            }
        }
    }

    /**
     * 加载已下载的模型
     */
    fun loadDownloadedModel() {
        if (_modelState.value != ModelState.Downloaded) return

        val modelFile = downloadManager.getModelFile(currentModel.fileName)
        loadModel(modelFile)
    }

    /**
     * 加载模型到内存
     */
    private fun loadModel(modelFile: File) {
        _modelState.value = ModelState.Loading

        coroutineScope.launch(Dispatchers.IO) {
            try {
                val success = LLMNative.initModel(
                    modelFile.absolutePath,
                    config.nThreads,
                    config.nCtx
                )

                _modelState.value = if (success) {
                    ModelState.Ready(modelFile.absolutePath)
                } else {
                    ModelState.Error("模型加载失败")
                }
            } catch (e: Exception) {
                _modelState.value = ModelState.Error(e.message ?: "加载异常")
            }
        }
    }

    /**
     * 取消下载
     */
    fun cancelDownload() {
        downloadManager.cancelDownload()
    }

    /**
     * 生成回复
     */
    fun generate(prompt: String) {
        if (_modelState.value !is ModelState.Ready) {
            _generationState.value = GenerationState.Error("模型未就绪")
            return
        }

        coroutineScope.launch(Dispatchers.IO) {
            _generationState.value = GenerationState.Generating(prompt)

            try {
                val startTime = SystemClock.elapsedRealtime()

                val response = LLMNative.generate(
                    prompt,
                    config.maxTokens,
                    config.temperature
                )

                val duration = SystemClock.elapsedRealtime() - startTime
                val tokensPerSecond = config.maxTokens / (duration / 1000f)

                _generationState.value = GenerationState.Complete(
                    response.trim(),
                    tokensPerSecond
                )
            } catch (e: Exception) {
                _generationState.value = GenerationState.Error(e.message ?: "生成失败")
            }
        }
    }

    /**
     * 删除模型
     */
    fun deleteModel() {
        coroutineScope.launch {
            LLMNative.releaseModel()
            downloadManager.deleteModel(currentModel)
            _modelState.value = ModelState.NotDownloaded
        }
    }

    /**
     * 释放资源
     */
    fun release() {
        LLMNative.releaseModel()
    }
}
