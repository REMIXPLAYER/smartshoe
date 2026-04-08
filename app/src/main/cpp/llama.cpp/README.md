# llama.cpp 占位符

此目录需要放置 llama.cpp 源码才能编译 JNI 层。

## 添加方法

### 方法1：Git 子模块（推荐）
```bash
cd /Users/remixplay/StudioProjects/smartshoe
rm -rf app/src/main/cpp/llama.cpp
git submodule add https://github.com/ggerganov/llama.cpp.git app/src/main/cpp/llama.cpp
cd app/src/main/cpp/llama.cpp
git checkout b1  # 使用稳定版本
```

### 方法2：手动下载
1. 下载 llama.cpp 源码: https://github.com/ggerganov/llama.cpp/archive/refs/tags/b1.zip
2. 解压到 `app/src/main/cpp/llama.cpp`

## 验证
确保目录包含以下文件：
- CMakeLists.txt
- llama.h
- common.h
- llama.cpp
