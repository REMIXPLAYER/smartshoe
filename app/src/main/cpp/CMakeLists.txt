cmake_minimum_required(VERSION 3.22.1)
project("smartshoe_llm")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# 设置 llama.cpp 路径
set(LLAMA_CPP_DIR ${CMAKE_CURRENT_SOURCE_DIR}/llama.cpp)

# 检查 llama.cpp 是否存在
if(EXISTS ${LLAMA_CPP_DIR}/CMakeLists.txt)
    # 这些选项通过 Gradle 传递，这里不需要重复设置
    # 但为了保险起见，如果未设置则使用默认值
    if(NOT DEFINED LLAMA_BUILD_COMMON)
        set(LLAMA_BUILD_COMMON ON)
    endif()
    
    if(NOT DEFINED LLAMA_METAL)
        set(LLAMA_METAL OFF)
    endif()
    
    if(NOT DEFINED LLAMA_ACCELERATE)
        set(LLAMA_ACCELERATE OFF)
    endif()
    
    # 禁用 llama.cpp 中不需要的功能
    set(LLAMA_BUILD_TESTS OFF CACHE BOOL "Build tests" FORCE)
    set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "Build examples" FORCE)
    set(LLAMA_BUILD_SERVER OFF CACHE BOOL "Build server" FORCE)
    
    # 禁用 HTTPS/SSL 支持（避免 OpenSSL 依赖）
    set(LLAMA_CURL OFF CACHE BOOL "Use libcurl" FORCE)
    
    # 禁用 Git 版本检测
    set(LLAMA_BUILD_NUMBER 0 CACHE STRING "Build number" FORCE)
    set(LLAMA_BUILD_COMMIT "unknown" CACHE STRING "Build commit" FORCE)
    
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
    
    # 新版 llama.cpp 头文件路径
    target_include_directories(smartshoe_llm PRIVATE 
        ${LLAMA_CPP_DIR}/include
        ${LLAMA_CPP_DIR}/common
        ${LLAMA_CPP_DIR}
    )
endif()

# ARM64 性能优化（Android）
if(ANDROID_ABI STREQUAL "arm64-v8a")
    target_compile_options(smartshoe_llm PRIVATE 
        -O3                                    # 最高优化级别
        -DNDEBUG                               # 禁用调试
        -march=armv8.2-a+fp16+dotprod+i8mm     # 启用 FP16、DOTPROD、I8MM 指令集
        -ffast-math                            # 快速数学运算
        -funroll-loops                         # 循环展开
        -fomit-frame-pointer                   # 省略帧指针
    )
    
    # 启用 ARM I8MM 支持（矩阵乘法加速）
    add_definitions(-D__ARM_FEATURE_MATMUL_INT8)
    
    message(STATUS "ARM64 optimizations enabled: FP16, DOTPROD, I8MM")
endif()

# ARMv7 优化
if(ANDROID_ABI STREQUAL "armeabi-v7a")
    target_compile_options(smartshoe_llm PRIVATE 
        -O3 
        -mfpu=neon
        -mfloat-abi=softfp
    )
    message(STATUS "ARMv7 NEON optimizations enabled")
endif()

# 链接时优化
set(CMAKE_INTERPROCEDURAL_OPTIMIZATION TRUE)
