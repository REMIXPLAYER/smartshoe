cmake_minimum_required(VERSION 3.22.1)
project("smartshoe_llm")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# 设置 llama.cpp 路径（后续添加子模块）
set(LLAMA_CPP_DIR ${CMAKE_CURRENT_SOURCE_DIR}/llama.cpp)

# 检查 llama.cpp 是否存在
if(EXISTS ${LLAMA_CPP_DIR}/CMakeLists.txt)
    add_subdirectory(${LLAMA_CPP_DIR} llama.cpp)
else()
    message(WARNING "llama.cpp not found at ${LLAMA_CPP_DIR}. Please add it as a submodule.")
endif()

# 创建 JNI 共享库
add_library(
    smartshoe_llm
    SHARED
    llm_jni.cpp
)

# 链接库
target_link_libraries(
    smartshoe_llm
    android
    log
)

# 如果 llama.cpp 存在，链接它
if(EXISTS ${LLAMA_CPP_DIR}/CMakeLists.txt)
    target_link_libraries(smartshoe_llm llama common)
    target_include_directories(smartshoe_llm PRIVATE ${LLAMA_CPP_DIR})
endif()

# ARM64 优化
if(ANDROID_ABI STREQUAL "arm64-v8a")
    target_compile_options(smartshoe_llm PRIVATE 
        -O3 
        -march=armv8.2-a+fp16+dotprod
        -DNDEBUG
    )
endif()

# 其他架构优化
if(ANDROID_ABI STREQUAL "armeabi-v7a")
    target_compile_options(smartshoe_llm PRIVATE -O3 -mfpu=neon)
endif()
