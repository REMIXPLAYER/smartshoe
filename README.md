# SmartShoe App

SmartShoe Android 应用 - 智能鞋垫数据采集与分析系统。

## 🚀 一键式部署

### 环境要求

- **Android Studio**: Hedgehog (2023.1.1) 或更高版本
- **JDK**: Java 17
- **Android SDK**: API 34
- **操作系统**: macOS / Windows / Linux

### 方式一：使用 Android Studio（推荐）

```bash
# 1. Clone 项目
git clone https://github.com/REMIXPLAYER/smartshoe.git

# 2. 用 Android Studio 打开
# 打开 Android Studio -> Open -> 选择 smartshoe 文件夹

# 3. 等待 Gradle 同步
# Android Studio 会自动下载 Gradle 8.7 和所有依赖

# 4. 连接设备或启动模拟器
# 点击 Run 按钮运行应用
```

### 方式二：MacBook 一键设置脚本

```bash
# 1. Clone 项目
git clone https://github.com/REMIXPLAYER/smartshoe.git
cd smartshoe

# 2. 运行 MacBook 设置脚本
chmod +x setup-mac.sh
./setup-mac.sh
```

脚本会自动：
1. ✅ 检查 Java 17 环境
2. ✅ 检查 Android Studio
3. ✅ 创建 local.properties
4. ✅ 构建项目

### 方式三：使用命令行

```bash
# 1. Clone 项目
git clone https://github.com/REMIXPLAYER/smartshoe.git
cd smartshoe

# 2. 使用 Gradle Wrapper 构建
./gradlew assembleDebug

# 3. 安装到连接的设备
./gradlew installDebug
```

## ⚙️ 配置说明

### 服务器地址配置

编辑 `app/build.gradle.kts`：

```kotlin
buildTypes {
    debug {
        buildConfigField(
            "String",
            "BASE_URL",
            "\"http://10.0.2.2:8080/api\""  // Android 模拟器访问本机服务器
        )
    }
}
```

**常见配置**:
- Android 模拟器: `http://10.0.2.2:8080/api`
- 真机（同一WiFi）: `http://192.168.x.x:8080/api`
- 生产服务器: `http://your-domain.com/api`

## 🔧 技术栈

- **语言**: Kotlin
- **UI**: Jetpack Compose
- **架构**: MVVM + Clean Architecture
- **依赖注入**: Hilt
- **图表**: MPAndroidChart
- **最低 SDK**: API 24
- **目标 SDK**: API 34

## 🔗 相关项目

- [SmartShoe Server](https://github.com/REMIXPLAYER/smartshoe-server) - 后端服务
