package com.example.smartshoe.llm

import android.content.Context
import android.os.SystemClock
import com.example.smartshoe.llm.LLMNative.toInt
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LLMManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadManager: ModelDownloadManager,
    private val capabilityDetector: InferenceCapabilityDetector
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

    // 检测设备推理能力
    private val deviceCapability by lazy {
        capabilityDetector.detectBestBackend()
    }

    data class ModelConfig(
        val nThreads: Int = 4,
        val nCtx: Int = 512,
        val maxTokens: Int = 256,
        val temperature: Float = 0.7f,
        val backendType: InferenceBackendType = InferenceBackendType.CPU
    )

    private var config: ModelConfig = ModelConfig()
    private var currentModel: ModelDownloadManager.ModelInfo = ModelDownloadManager.DEFAULT_MODEL

    /**
     * 获取当前使用的推理后端信息
     */
    fun getCurrentBackend(): DeviceInferenceCapability = deviceCapability

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
            android.util.Log.d("LLMManager", "Already downloading or loading, skip")
            return
        }

        currentModel = model
        _modelState.value = ModelState.Downloading
        android.util.Log.d("LLMManager", "Starting download for model: ${model.fileName}")

        coroutineScope.launch {
            downloadManager.downloadModel(model).collect { state ->
                android.util.Log.d("LLMManager", "Download state: ${state.javaClass.simpleName}")
                _downloadState.value = state

                when (state) {
                    is ModelDownloadManager.DownloadState.Success -> {
                        android.util.Log.d("LLMManager", "Download success, file: ${state.file.absolutePath}, exists=${state.file.exists()}")
                        _modelState.value = ModelState.Downloaded
                        loadModel(state.file)
                    }
                    is ModelDownloadManager.DownloadState.Error -> {
                        android.util.Log.e("LLMManager", "Download error: ${state.message}")
                        // 下载失败，检查是否已经有下载好的文件
                        val modelFile = downloadManager.getModelFile(currentModel.fileName)
                        if (modelFile.exists() && modelFile.length() > 1024 * 1024) {
                            android.util.Log.d("LLMManager", "Existing model file found, switching to Downloaded state")
                            _modelState.value = ModelState.Downloaded
                        } else {
                            _modelState.value = ModelState.NotDownloaded
                        }
                    }
                    is ModelDownloadManager.DownloadState.Cancelled -> {
                        android.util.Log.d("LLMManager", "Download cancelled")
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
        android.util.Log.d("LLMManager", "loadDownloadedModel called, current state: ${_modelState.value.javaClass.simpleName}")
        
        // 允许从 Downloaded 或 Error 状态加载
        if (_modelState.value != ModelState.Downloaded && 
            _modelState.value !is ModelState.Error) {
            android.util.Log.d("LLMManager", "Cannot load from current state")
            return
        }

        val modelFile = downloadManager.getModelFile(currentModel.fileName)
        android.util.Log.d("LLMManager", "Loading model file: ${modelFile.absolutePath}, exists=${modelFile.exists()}")
        
        if (!modelFile.exists()) {
            android.util.Log.e("LLMManager", "Model file does not exist!")
            _modelState.value = ModelState.NotDownloaded
            _downloadState.value = ModelDownloadManager.DownloadState.Error(
                "模型文件不存在，请重新下载",
                retryable = true
            )
            return
        }
        
        loadModel(modelFile)
    }

    /**
     * 加载模型到内存
     */
    private fun loadModel(modelFile: File) {
        _modelState.value = ModelState.Loading
        
        // 检测设备能力并配置最佳后端
        val capability = deviceCapability
        config = ModelConfig(
            nThreads = capability.recommendedThreads,
            nCtx = 512,
            maxTokens = 256,
            temperature = 0.7f,
            backendType = capability.backendType
        )
        
        android.util.Log.d("LLMManager", "Loading model from: ${modelFile.absolutePath}")
        android.util.Log.d("LLMManager", "File exists: ${modelFile.exists()}, Size: ${modelFile.length()} bytes")
        android.util.Log.d("LLMManager", "Using backend: ${capability.backendType.displayName()}, threads: ${capability.recommendedThreads}")

        coroutineScope.launch(Dispatchers.IO) {
            try {
                val success = LLMNative.initModel(
                    modelFile.absolutePath,
                    config.nThreads,
                    config.nCtx,
                    config.backendType.toInt()
                )

                if (success) {
                    android.util.Log.d("LLMManager", "Model loaded successfully with ${capability.backendType.displayName()}")
                    _modelState.value = ModelState.Ready(modelFile.absolutePath)
                } else {
                    android.util.Log.e("LLMManager", "Model init returned false")
                    // 如果 GPU/NPU 失败，回退到 CPU
                    if (capability.backendType != InferenceBackendType.CPU) {
                        android.util.Log.d("LLMManager", "Falling back to CPU backend")
                        val cpuSuccess = LLMNative.initModel(
                            modelFile.absolutePath,
                            4, // CPU 使用 4 线程
                            config.nCtx,
                            LLMNative.BACKEND_CPU
                        )
                        if (cpuSuccess) {
                            _modelState.value = ModelState.Ready(modelFile.absolutePath)
                            return@launch
                        }
                    }
                    // 加载失败但文件存在，保持 Downloaded 状态让用户可以选择重试或删除
                    _modelState.value = ModelState.Downloaded
                    _downloadState.value = ModelDownloadManager.DownloadState.Error(
                        "模型加载失败，请检查模型文件是否完整",
                        retryable = true
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("LLMManager", "Exception during model loading", e)
                // 加载失败但文件存在，保持 Downloaded 状态
                _modelState.value = ModelState.Downloaded
                _downloadState.value = ModelDownloadManager.DownloadState.Error(
                    "加载异常: ${e.message}",
                    retryable = true
                )
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
