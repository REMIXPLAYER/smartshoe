package com.example.smartshoe.llm

import android.content.Context
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 推理后端类型
 */
sealed class InferenceBackendType {
    object CPU : InferenceBackendType()
    object GPU : InferenceBackendType()
    object NPU : InferenceBackendType()

    fun displayName(): String = when (this) {
        is CPU -> "CPU"
        is GPU -> "GPU"
        is NPU -> "NPU"
    }
}

/**
 * 设备推理能力信息
 */
data class DeviceInferenceCapability(
    val backendType: InferenceBackendType,
    val isSupported: Boolean,
    val deviceName: String,
    val computeUnits: Int,
    val recommendedThreads: Int,
    val supportsQuantization: Boolean,
    val supportsFlashAttention: Boolean
)

/**
 * 设备推理能力检测器
 * 遵循单一职责原则：只负责检测设备能力
 */
@Singleton
class InferenceCapabilityDetector @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * 检测设备最佳的推理后端
     */
    fun detectBestBackend(): DeviceInferenceCapability {
        val deviceName = Build.HARDWARE
        val isEmulator = isRunningOnEmulator()

        return when {
            // 优先检测 NPU (Android NNAPI)
            supportsNNAPI() && !isEmulator -> {
                DeviceInferenceCapability(
                    backendType = InferenceBackendType.NPU,
                    isSupported = true,
                    deviceName = deviceName,
                    computeUnits = getNPUComputeUnits(),
                    recommendedThreads = 2, // NPU 下减少 CPU 线程
                    supportsQuantization = true,
                    supportsFlashAttention = false // NPU 通常不支持 Flash Attention
                )
            }

            // 其次检测 GPU (OpenCL/Vulkan)
            supportsGPU() && !isEmulator -> {
                DeviceInferenceCapability(
                    backendType = InferenceBackendType.GPU,
                    isSupported = true,
                    deviceName = deviceName,
                    computeUnits = getGPUComputeUnits(),
                    recommendedThreads = 2, // GPU 下减少 CPU 线程
                    supportsQuantization = true,
                    supportsFlashAttention = true
                )
            }

            // 默认使用 CPU
            else -> {
                val cpuCores = Runtime.getRuntime().availableProcessors()
                DeviceInferenceCapability(
                    backendType = InferenceBackendType.CPU,
                    isSupported = true,
                    deviceName = deviceName,
                    computeUnits = cpuCores,
                    recommendedThreads = minOf(cpuCores, 4), // CPU 最多 4 线程
                    supportsQuantization = true,
                    supportsFlashAttention = true
                )
            }
        }
    }

    /**
     * 检测是否支持 NNAPI (NPU)
     */
    private fun supportsNNAPI(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && // API 28+
               context.packageManager.hasSystemFeature("android.hardware.neural_networks")
    }

    /**
     * 检测是否支持 GPU 加速
     * 通过检查 GPU 特性来判断
     */
    private fun supportsGPU(): Boolean {
        // 检查 OpenGL ES 版本
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
        val configurationInfo = activityManager?.deviceConfigurationInfo
        val supportsEs32 = configurationInfo?.reqGlEsVersion ?: 0 >= 0x30002

        // 高端 GPU 通常支持计算着色器
        return supportsEs32 && !isLowEndDevice()
    }

    /**
     * 获取 NPU 计算单元数
     */
    private fun getNPUComputeUnits(): Int {
        // 根据设备型号估算
        return when {
            Build.HARDWARE.contains("qcom", ignoreCase = true) -> 4 // 高通 NPU
            Build.HARDWARE.contains("exynos", ignoreCase = true) -> 2 // 三星 NPU
            Build.HARDWARE.contains("kirin", ignoreCase = true) -> 2 // 华为 NPU
            Build.HARDWARE.contains("mt", ignoreCase = true) -> 2 // 联发科 APU
            else -> 1
        }
    }

    /**
     * 获取 GPU 计算单元数
     */
    private fun getGPUComputeUnits(): Int {
        // 根据 GPU 型号估算
        return when {
            Build.HARDWARE.contains("qcom", ignoreCase = true) -> 4 // Adreno
            Build.HARDWARE.contains("exynos", ignoreCase = true) -> 4 // Mali
            Build.HARDWARE.contains("kirin", ignoreCase = true) -> 4 // Mali
            Build.HARDWARE.contains("mt", ignoreCase = true) -> 4 // Mali
            else -> 2
        }
    }

    /**
     * 检测是否为低端设备
     */
    private fun isLowEndDevice(): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
        return activityManager?.isLowRamDevice ?: false
    }

    /**
     * 检测是否在模拟器上运行
     */
    private fun isRunningOnEmulator(): Boolean {
        return (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.PRODUCT.contains("sdk_google")
                || Build.PRODUCT.contains("google_sdk")
                || Build.PRODUCT.contains("sdk")
                || Build.PRODUCT.contains("sdk_x86")
                || Build.PRODUCT.contains("vbox86p")
                || Build.PRODUCT.contains("emulator")
                || Build.PRODUCT.contains("simulator")
    }
}
