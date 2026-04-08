package com.example.smartshoe.llm

object LLMNative {

    // 后端类型常量
    const val BACKEND_CPU = 0
    const val BACKEND_GPU = 1
    const val BACKEND_NPU = 2

    init {
        System.loadLibrary("smartshoe_llm")
    }

    /**
     * 初始化模型
     * @param modelPath 模型文件路径
     * @param nThreads 推理线程数
     * @param nCtx 上下文长度
     * @param backendType 推理后端类型 (0=CPU, 1=GPU, 2=NPU)
     * @return 是否初始化成功
     */
    external fun initModel(modelPath: String, nThreads: Int, nCtx: Int, backendType: Int): Boolean

    /**
     * 生成文本回复
     * @param prompt 输入提示
     * @param maxTokens 最大生成 token 数
     * @param temperature 采样温度
     * @return 生成的文本
     */
    external fun generate(prompt: String, maxTokens: Int, temperature: Float): String

    /**
     * 释放模型资源
     */
    external fun releaseModel()

    /**
     * 检查模型是否已加载
     */
    external fun isModelLoaded(): Boolean

    /**
     * 将 InferenceBackendType 转换为 JNI 常量
     */
    fun InferenceBackendType.toInt(): Int = when (this) {
        is InferenceBackendType.CPU -> BACKEND_CPU
        is InferenceBackendType.GPU -> BACKEND_GPU
        is InferenceBackendType.NPU -> BACKEND_NPU
    }
}
