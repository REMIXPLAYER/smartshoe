# Qwen-0.5B LLM 集成说明

## 项目结构

```
app/src/main/
├── cpp/
│   ├── CMakeLists.txt          # CMake 构建配置
│   ├── llm_jni.cpp             # JNI 接口实现
│   └── llama.cpp/              # llama.cpp 子模块（需手动添加）
├── java/com/example/smartshoe/
│   ├── llm/
│   │   ├── LLMNative.kt        # JNI 接口声明
│   │   ├── ModelDownloadManager.kt  # 模型下载管理
│   │   └── LLMManager.kt       # LLM 管理器
│   └── ui/
│       ├── screen/
│       │   └── AIAssistantScreen.kt  # AI 助手界面
│       └── viewmodel/
│           └── AIAssistantViewModel.kt
```

## 前置条件

### 1. 添加 llama.cpp 子模块

```bash
cd /Users/remixplay/StudioProjects/smartshoe
rm -rf app/src/main/cpp/llama.cpp
git submodule add https://github.com/ggerganov/llama.cpp.git app/src/main/cpp/llama.cpp
cd app/src/main/cpp/llama.cpp
git checkout b1  # 使用稳定版本
```

### 2. 配置阿里云 OSS

编辑 `app/src/main/java/com/example/smartshoe/llm/ModelDownloadManager.kt`：

```kotlin
val DEFAULT_MODEL = ModelInfo(
    name = "Qwen-0.5B-Instruct",
    url = "https://your-bucket.oss-cn-beijing.aliyuncs.com/models/qwen-0.5b-instruct-q4_k_m.gguf",
    fileName = "qwen-0.5b-instruct-q4_k_m.gguf",
    expectedSize = 352_321_632,  // 根据实际文件大小修改
    expectedHash = "your-sha256-hash",  // 可选：文件校验
    description = "通义千问 0.5B 量化模型"
)
```

### 3. 准备模型文件

1. 下载 Qwen-0.5B-Instruct 模型
2. 使用 llama.cpp 转换为 GGUF 格式并量化：

```bash
# 转换
python convert_hf_to_gguf.py Qwen-0.5B-Instruct \
    --outfile qwen-0.5b-instruct-f16.gguf \
    --outtype f16

# 量化 (Q4_K_M)
./quantize qwen-0.5b-instruct-f16.gguf \
    qwen-0.5b-instruct-q4_k_m.gguf \
    Q4_K_M
```

3. 上传到阿里云 OSS

## 使用方法

### 在设置页面添加 AI 助手入口

在 `SettingScreen.kt` 中添加：

```kotlin
// 在设置列表中添加
item {
    Button(
        onClick = { /* 导航到 AI 助手页面 */ }
    ) {
        Text("AI 助手")
    }
}
```

### 导航到 AI 助手页面

```kotlin
// 使用 Compose Navigation
composable("ai_assistant") {
    AIAssistantScreen()
}
```

## 工作流程

1. **首次使用**：
   - 显示"模型未下载"
   - 用户点击"下载"按钮
   - 显示下载进度（支持断点续传）
   - 下载完成后自动加载模型

2. **已下载未加载**：
   - 显示"已下载，点击加载"
   - 用户点击后加载模型到内存

3. **模型就绪**：
   - 显示"AI 助手就绪"
   - 用户可以输入问题获取回复

## 注意事项

1. **存储空间**：模型文件约 336MB，需要额外 600MB 缓存空间
2. **网络环境**：建议在 WiFi 环境下下载
3. **内存要求**：模型运行时约需 1-1.5GB RAM
4. **阿里云 OSS**：
   - 开启 CDN 加速
   - 配置 CORS 跨域
   - 设置断点续传支持（Range 请求）

## 故障排除

| 问题 | 解决方案 |
|------|---------|
| 下载失败 | 检查网络连接和 OSS URL |
| 模型加载失败 | 检查 llama.cpp 是否正确添加 |
| JNI 错误 | 确保 CMake 配置正确，ndk 已安装 |
| 内存不足 | 关闭其他应用，或减小 nCtx 参数 |

## 回退方法

如果需要回退到之前版本：

```bash
git reset --hard backup-before-llm
```

## APK 体积

- 基础 APK：~15MB
- 模型文件：~336MB（用户下载）
