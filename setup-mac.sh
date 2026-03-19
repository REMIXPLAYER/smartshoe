#!/bin/bash

# SmartShoe App MacBook 一键设置脚本
set -e

echo "========================================"
echo "  SmartShoe App - MacBook 设置脚本"
echo "========================================"

# 检查 Java
echo "[1/4] 检查 Java 环境..."
if ! command -v java &> /dev/null; then
    echo "❌ 错误：未找到 Java，请安装 Java 17"
    exit 1
fi
echo "✅ Java 已安装"

# 检查 Android Studio
echo "[2/4] 检查 Android Studio..."
if [ -d "/Applications/Android Studio.app" ]; then
    echo "✅ Android Studio 已安装"
else
    echo "⚠️  请从 https://developer.android.com/studio 下载安装"
fi

# 创建 local.properties
echo "[3/4] 检查本地配置..."
if [ ! -f "local.properties" ]; then
    echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties
    echo "✅ 已创建 local.properties"
else
    echo "✅ local.properties 已存在"
fi

# 构建项目
echo "[4/4] 构建项目..."
./gradlew clean build -x test

echo ""
echo "========================================"
echo "  ✅ 设置完成！"
echo "========================================"
echo ""
echo "接下来："
echo "1. 用 Android Studio 打开项目"
echo "2. 连接设备或启动模拟器"
echo "3. 点击 Run 按钮运行"
