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
            llama_free_model(model);
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
    
    // 释放旧资源
    if (g_llm_context) {
        g_llm_context->release();
    }
    
    g_llm_context = std::make_unique<LLMContext>();
    
    // 初始化后端
    llama_backend_init();
    
    // 加载模型
    llama_model_params model_params = llama_model_default_params();
    g_llm_context->model = llama_load_model_from_file(path, model_params);
    
    env->ReleaseStringUTFChars(model_path, path);
    
    if (!g_llm_context->model) {
        LOGE("Failed to load model");
        g_llm_context.reset();
        return JNI_FALSE;
    }
    
    // 创建上下文
    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = n_ctx;
    ctx_params.n_threads = n_threads;
    ctx_params.n_threads_batch = n_threads;
    
    g_llm_context->ctx = llama_new_context_with_model(
        g_llm_context->model, 
        ctx_params
    );
    
    if (!g_llm_context->ctx) {
        LOGE("Failed to create context");
        g_llm_context->release();
        g_llm_context.reset();
        return JNI_FALSE;
    }
    
    // 创建采样器链
    g_llm_context->sampler = llama_sampler_chain_init(llama_sampler_chain_default_params());
    llama_sampler_chain_add(g_llm_context->sampler, llama_sampler_init_softmax());
    
    g_llm_context->is_loaded = true;
    LOGI("Model loaded successfully with n_threads=%d, n_ctx=%d", n_threads, n_ctx);
    
    return JNI_TRUE;
}

JNIEXPORT jstring JNICALL
Java_com_example_smartshoe_llm_LLMNative_generate(
    JNIEnv* env,
    jobject /* this */,
    jstring prompt,
    jint max_tokens,
    jfloat temperature
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
        "<|im_start|>system\nYou are a helpful assistant for SmartShoe app.<|im_end|>\n"
        "<|im_start|>user\n" + prompt_str + "<|im_end|>\n"
        "<|im_start|>assistant\n";
    
    // Tokenize
    const int n_prompt_tokens = -llama_tokenize(
        g_llm_context->model,
        formatted_prompt.c_str(),
        formatted_prompt.length(),
        nullptr,
        0,
        true,
        true
    );
    
    if (n_prompt_tokens <= 0) {
        return env->NewStringUTF("Error: Tokenization failed");
    }
    
    std::vector<llama_token> prompt_tokens(n_prompt_tokens);
    if (llama_tokenize(
            g_llm_context->model,
            formatted_prompt.c_str(),
            formatted_prompt.length(),
            prompt_tokens.data(),
            prompt_tokens.size(),
            true,
            true) < 0) {
        return env->NewStringUTF("Error: Tokenization failed");
    }
    
    // 创建 batch
    llama_batch batch = llama_batch_init(prompt_tokens.size(), 0, 1);
    
    for (size_t i = 0; i < prompt_tokens.size(); i++) {
        llama_batch_add(batch, prompt_tokens[i], static_cast<llama_pos>(i), {0}, false);
    }
    batch.logits[batch.n_tokens - 1] = true;
    
    // 解码 prompt
    if (llama_decode(g_llm_context->ctx, batch) != 0) {
        llama_batch_free(batch);
        return env->NewStringUTF("Error: Failed to decode");
    }
    
    llama_batch_free(batch);
    
    // 根据温度配置采样器
    llama_sampler_chain_clear(g_llm_context->sampler);
    llama_sampler_chain_add(g_llm_context->sampler, llama_sampler_init_softmax());
    if (temperature > 0.0f && temperature != 1.0f) {
        llama_sampler_chain_add(g_llm_context->sampler, 
            llama_sampler_init_temperature(temperature));
    }
    
    // 生成
    std::string response;
    llama_token new_token_id;
    int n_pos = prompt_tokens.size();
    
    for (int i = 0; i < max_tokens; i++) {
        new_token_id = llama_sampler_sample(g_llm_context->sampler, 
            g_llm_context->ctx, -1);
        
        if (llama_token_is_eog(g_llm_context->model, new_token_id)) {
            break;
        }
        
        char buf[256];
        int n = llama_token_to_piece(g_llm_context->model, new_token_id, 
            buf, sizeof(buf), 0, false);
        if (n > 0) {
            response.append(buf, n);
        }
        
        llama_batch batch_next = llama_batch_init(1, 0, 1);
        llama_batch_add(batch_next, new_token_id, n_pos++, {0}, true);
        
        if (llama_decode(g_llm_context->ctx, batch_next) != 0) {
            llama_batch_free(batch_next);
            break;
        }
        
        llama_batch_free(batch_next);
    }
    
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
