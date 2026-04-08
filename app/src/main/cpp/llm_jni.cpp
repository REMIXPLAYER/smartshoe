#include <jni.h>
#include <string>
#include <memory>
#include <mutex>
#include <vector>
#include <android/log.h>

// llama.cpp headers
#include "llama.h"
#include "common.h"

#define LOG_TAG "SmartShoeLLM"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

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

JNIEXPORT jboolean JNICALL
Java_com_example_smartshoe_llm_LLMNative_initModel(
    JNIEnv* env,
    jobject /* this */,
    jstring model_path,
    jint n_threads,
    jint n_ctx
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
    
    if (g_llm_context) {
        g_llm_context->release();
    }
    
    g_llm_context = std::make_unique<LLMContext>();
    
    llama_backend_init();
    
    llama_model_params model_params = llama_model_default_params();
    g_llm_context->model = llama_model_load_from_file(path, model_params);
    
    env->ReleaseStringUTFChars(model_path, path);
    
    if (!g_llm_context->model) {
        LOGE("Failed to load model");
        g_llm_context.reset();
        return JNI_FALSE;
    }
    
    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = n_ctx;
    ctx_params.n_threads = n_threads;
    ctx_params.n_threads_batch = n_threads;
    
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
    
    // 创建采样器（使用官方示例方式）
    auto sparams = llama_sampler_chain_default_params();
    g_llm_context->sampler = llama_sampler_chain_init(sparams);
    llama_sampler_chain_add(g_llm_context->sampler, llama_sampler_init_greedy());
    
    g_llm_context->is_loaded = true;
    LOGI("Model loaded: n_threads=%d, n_ctx=%d", n_threads, n_ctx);
    
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
    
    // Qwen 对话格式
    std::string formatted_prompt = 
        "<|im_start|>system\nYou are a helpful assistant.<|im_end|>\n"
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
    
    // 生成
    std::string response;
    llama_token new_token_id;
    int n_decode = 0;
    
    for (int i = 0; i < max_tokens; i++) {
        // 采样
        new_token_id = llama_sampler_sample(g_llm_context->sampler, g_llm_context->ctx, -1);
        
        // 检查是否结束（使用 vocab）
        if (llama_vocab_is_eog(vocab, new_token_id)) {
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
            break;
        }
        
        n_decode++;
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
