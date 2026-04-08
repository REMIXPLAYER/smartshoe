package com.example.smartshoe.domain.model

/**
 * LLM 配置领域模型
 */
data class LLMConfig(
    val nThreads: Int = 4,
    val nCtx: Int = 512,
    val maxTokens: Int = 256,
    val temperature: Float = 0.7f,
    val useGpu: Boolean = false,
    val gpuBackend: GpuBackend = GpuBackend.CPU
) {
    enum class GpuBackend {
        CPU,           // 纯 CPU
        GPU_OPENCL,    // OpenCL GPU
        GPU_VULKAN,    // Vulkan GPU
        NPU_NNAPI,     // Android NNAPI NPU
        NPU_QNN        // Qualcomm QNN
    }
}

/**
 * 模型信息领域模型
 */
data class ModelInfo(
    val name: String,
    val url: String,
    val fileName: String,
    val expectedSize: Long,
    val description: String,
    val quantization: QuantizationLevel = QuantizationLevel.Q4_K_M
) {
    enum class QuantizationLevel {
        Q2_K, Q3_K_M, Q4_K_M, Q5_K_M, Q6_K, Q8_0
    }
}

/**
 * 模型状态领域模型
 */
sealed class ModelLoadState {
    object NotDownloaded : ModelLoadState()
    object Downloading : ModelLoadState()
    object Downloaded : ModelLoadState()
    object Loading : ModelLoadState()
    data class Ready(val modelPath: String, val backend: String) : ModelLoadState()
    data class Error(val message: String) : ModelLoadState()
}

/**
 * 生成状态领域模型
 */
sealed class GenerationState {
    object Idle : GenerationState()
    data class Generating(val prompt: String) : GenerationState()
    data class Complete(val response: String, val tokensPerSecond: Float) : GenerationState()
    data class Error(val message: String) : GenerationState()
}

/**
 * 设备能力信息
 */
data class DeviceCapabilities(
    val hasGpu: Boolean,
    val hasNpu: Boolean,
    val gpuBackend: LLMConfig.GpuBackend,
    val recommendedThreads: Int,
    val deviceName: String
)
