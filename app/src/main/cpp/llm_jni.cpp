#include <jni.h>
#include <string>
#include <memory>
#include <mutex>
#include <vector>
#include <android/log.h>
#include <sys/stat.h>
#include <sched.h>

// llama.cpp headers
#include "llama.h"
#include "common.h"
#include "ggml.h"

#define LOG_TAG "SmartShoeLLM"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

// 全局模型上下文（线程安全）
struct LLMContext {
    llama_model* model = nullptr;
    llama_context* ctx = nullptr;
    llama_sampler* sampler = nullptr;
    std::mutex mutex;
    bool is_loaded = false;
    
    ~LLMContext() {
        release();
    }
    
    void release() {
        std::lock_guard<std::mutex> lock(mutex);
        if (sampler) {
            llama_sampler_free(sampler);
            sampler = nullptr;
        }
        if (ctx) {
            llama_free(ctx);
            ctx = nullptr;
        }
        if (model) {
            llama_model_free(model);
            model = nullptr;
        }
        is_loaded = false;
    }
};

static std::unique_ptr<LLMContext> g_llm_context;

extern "C" {

// 后端类型定义
enum class BackendType {
    CPU = 0,
    GPU = 1,
    NPU = 2
};

JNIEXPORT jboolean JNICALL
Java_com_example_smartshoe_llm_LLMNative_initModel(
    JNIEnv* env,
    jobject /* this */,
    jstring model_path,
    jint n_threads,
    jint n_ctx,
    jint backend_type
) {
    if (!model_path) {
        LOGE("Model path is null");
        return JNI_FALSE;
    }
    
    const char* path = env->GetStringUTFChars(model_path, nullptr);
    if (!path) {
        LOGE("Failed to get model path string");
        return JNI_FALSE;
    }
    
    BackendType backend = static_cast<BackendType>(backend_type);
    const char* backend_name = backend == BackendType::GPU ? "GPU" : 
                               backend == BackendType::NPU ? "NPU" : "CPU";
    
    LOGI("Loading model from: %s", path);
    LOGI("Backend: %s, Threads: %d, Context: %d", backend_name, n_threads, n_ctx);
    
    // 检查文件是否存在
    struct stat file_stat;
    if (stat(path, &file_stat) != 0) {
        LOGE("Model file does not exist: %s", path);
        env->ReleaseStringUTFChars(model_path, path);
        return JNI_FALSE;
    }
    LOGI("Model file size: %ld bytes", file_stat.st_size);
    
    if (g_llm_context) {
        g_llm_context->release();
    }
    
    g_llm_context = std::make_unique<LLMContext>();
    
    LOGI("Initializing llama backend...");
    llama_backend_init();
    
    // 配置模型参数
    LOGI("Loading model with %s backend...", backend_name);
    llama_model_params model_params = llama_model_default_params();
    
    // 根据后端类型配置模型加载参数
    // 启用 GPU 加速（如果编译时启用了 Vulkan）
    #ifdef GGML_USE_VULKAN
    if (backend == BackendType::GPU) {
        LOGI("Attempting to use Vulkan GPU backend");
        // llama.cpp 会自动检测并使用 Vulkan 后端
        // 如果设备支持，ggml 会将计算 offload 到 GPU
    }
    #else
    LOGI("Vulkan support not compiled in, using CPU backend");
    #endif
    
    g_llm_context->model = llama_model_load_from_file(path, model_params);
    
    env->ReleaseStringUTFChars(model_path, path);
    
    if (!g_llm_context->model) {
        LOGE("Failed to load model - llama_model_load_from_file returned null");
        g_llm_context.reset();
        return JNI_FALSE;
    }
    LOGI("Model loaded successfully");
    
    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = n_ctx;
    ctx_params.n_threads = n_threads;
    ctx_params.n_threads_batch = n_threads;
    
    // 启用 KV cache 量化，减少内存带宽和功耗
    ctx_params.type_k = GGML_TYPE_Q4_0;   // key cache 量化到 4bit
    ctx_params.type_v = GGML_TYPE_Q4_0;   // value cache 量化到 4bit
    
    // 启用 Flash Attention（如果设备支持）
    // 使用正确的 API: flash_attn_type 字段
    ctx_params.flash_attn_type = LLAMA_FLASH_ATTN_TYPE_ENABLED;
    
    g_llm_context->ctx = llama_init_from_model(
        g_llm_context->model, 
        ctx_params
    );
    
    if (!g_llm_context->ctx) {
        LOGE("Failed to create context");
        g_llm_context->release();
        g_llm_context.reset();
        return JNI_FALSE;
    }
    
    // 创建采样器链，添加多种采样策略防止重复
    auto sparams = llama_sampler_chain_default_params();
    g_llm_context->sampler = llama_sampler_chain_init(sparams);

    // 1. 重复惩罚 - 防止生成重复内容（关键！）
    // 参数：惩罚窗口大小, 重复惩罚系数, 频率惩罚, 存在惩罚
    llama_sampler_chain_add(g_llm_context->sampler,
        llama_sampler_init_penalties(64, 1.1f, 0.0f, 0.0f));

    // 2. Top-K 采样 - 只从概率最高的前 20 个 token 中选择（更保守）
    llama_sampler_chain_add(g_llm_context->sampler, llama_sampler_init_top_k(20));

    // 3. Top-P (nucleus) 采样 - 只从累积概率前 95% 的 token 中选择（更稳定）
    llama_sampler_chain_add(g_llm_context->sampler, llama_sampler_init_top_p(0.95f, 1));

    // 4. 温度采样 - 降低随机性，使输出更稳定
    llama_sampler_chain_add(g_llm_context->sampler, llama_sampler_init_temp(0.6f));

    // 5. 最终选择 - 从筛选后的候选中随机选择
    llama_sampler_chain_add(g_llm_context->sampler, llama_sampler_init_dist(42));
    
    g_llm_context->is_loaded = true;
    LOGI("Model loaded: backend=%s, n_threads=%d, n_ctx=%d", backend_name, n_threads, n_ctx);
    
    return JNI_TRUE;
}

JNIEXPORT jstring JNICALL
Java_com_example_smartshoe_llm_LLMNative_generate(
    JNIEnv* env,
    jobject /* this */,
    jstring prompt,
    jint max_tokens,
    jfloat /* temperature */
) {
    if (!g_llm_context) {
        return env->NewStringUTF("Error: Model not initialized");
    }
    
    std::lock_guard<std::mutex> lock(g_llm_context->mutex);
    
    if (!g_llm_context->is_loaded) {
        return env->NewStringUTF("Error: Model not loaded");
    }
    
    if (!prompt) {
        return env->NewStringUTF("Error: Prompt is null");
    }
    
    const char* prompt_cstr = env->GetStringUTFChars(prompt, nullptr);
    if (!prompt_cstr) {
        return env->NewStringUTF("Error: Failed to get prompt");
    }
    
    std::string prompt_str(prompt_cstr);
    env->ReleaseStringUTFChars(prompt, prompt_cstr);
    
    // Qwen 对话格式 - 使用中文系统提示，明确要求中文回复
    std::string formatted_prompt = 
        "<|im_start|>system\n你是一个智能助手，请用中文回答用户的问题。回答要简洁、准确、有帮助。<|im_end|>\n"
        "<|im_start|>user\n" + prompt_str + "<|im_end|>\n"
        "<|im_start|>assistant\n";
    
    // 获取 vocab（关键！）
    const llama_vocab* vocab = llama_model_get_vocab(g_llm_context->model);
    
    // Tokenize - 使用 vocab 而不是 model
    const int n_prompt = -llama_tokenize(vocab, formatted_prompt.c_str(), formatted_prompt.size(), nullptr, 0, true, true);
    
    if (n_prompt <= 0) {
        return env->NewStringUTF("Error: Tokenization failed");
    }
    
    std::vector<llama_token> prompt_tokens(n_prompt);
    if (llama_tokenize(vocab, formatted_prompt.c_str(), formatted_prompt.size(), prompt_tokens.data(), prompt_tokens.size(), true, true) < 0) {
        return env->NewStringUTF("Error: Tokenization failed");
    }
    
    // 使用 llama_batch_get_one（官方示例方式）
    llama_batch batch = llama_batch_get_one(prompt_tokens.data(), prompt_tokens.size());
    
    // 解码 prompt
    if (llama_decode(g_llm_context->ctx, batch) != 0) {
        return env->NewStringUTF("Error: Failed to decode prompt");
    }
    
    // 生成 - 使用批处理优化
    std::string response;
    llama_token new_token_id;
    int n_decode = 0;
    
    // 预分配响应空间，避免频繁扩容
    response.reserve(max_tokens * 4);
    
    // 获取 Qwen 的特殊 token ID - 使用 llama_tokenize
    std::vector<llama_token> im_end_tokens(4);
    std::vector<llama_token> im_start_tokens(4);
    int n_im_end = llama_tokenize(vocab, "<|im_end|>", 10, im_end_tokens.data(), im_end_tokens.size(), false, true);
    int n_im_start = llama_tokenize(vocab, "<|im_start|>", 11, im_start_tokens.data(), im_start_tokens.size(), false, true);
    llama_token im_end_token = (n_im_end == 1) ? im_end_tokens[0] : -1;
    llama_token im_start_token = (n_im_start == 1) ? im_start_tokens[0] : -1;
    
    LOGI("Qwen special tokens - im_end: %d, im_start: %d", im_end_token, im_start_token);
    
    // 使用更高效的生成循环
    for (int i = 0; i < max_tokens; i++) {
        // 采样
        new_token_id = llama_sampler_sample(g_llm_context->sampler, g_llm_context->ctx, -1);
        
        // 检查是否结束（多种方式）
        // 1. 标准 EOG token
        if (llama_vocab_is_eog(vocab, new_token_id)) {
            LOGI("Detected EOG token at position %d", i);
            break;
        }
        
        // 2. Qwen 特殊 token
        if (im_end_token >= 0 && new_token_id == im_end_token) {
            LOGI("Detected Qwen im_end token at position %d", i);
            break;
        }
        if (im_start_token >= 0 && new_token_id == im_start_token) {
            LOGI("Detected Qwen im_start token at position %d", i);
            break;
        }
        
        // 3. 检查响应中是否包含结束标记
        if (response.find("<|im_end|>") != std::string::npos || 
            response.find("<|im_start|>") != std::string::npos) {
            LOGI("Detected Qwen tag in response at position %d", i);
            break;
        }
        
        // 转换为文本（使用 vocab）
        char buf[256];
        int n = llama_token_to_piece(vocab, new_token_id, buf, sizeof(buf), 0, true);
        if (n > 0) {
            response.append(buf, n);
        }
        
        // 准备下一个 batch
        batch = llama_batch_get_one(&new_token_id, 1);
        
        if (llama_decode(g_llm_context->ctx, batch) != 0) {
            LOGE("llama_decode failed at position %d", i);
            break;
        }
        
        n_decode++;
        
        // 每 16 个 token 让出时间片，避免阻塞 UI
        if (i % 16 == 0) {
            sched_yield();
        }
    }
    
    LOGI("Generated %d tokens", n_decode);
    return env->NewStringUTF(response.c_str());
}

JNIEXPORT void JNICALL
Java_com_example_smartshoe_llm_LLMNative_releaseModel(
    JNIEnv* /* env */,
    jobject /* this */
) {
    if (g_llm_context) {
        g_llm_context->release();
        llama_backend_free();
        LOGI("Model resources released");
    }
}

JNIEXPORT jboolean JNICALL
Java_com_example_smartshoe_llm_LLMNative_isModelLoaded(
    JNIEnv* /* env */,
    jobject /* this */
) {
    return (g_llm_context && g_llm_context->is_loaded) ? JNI_TRUE : JNI_FALSE;
}

} // extern "C"
