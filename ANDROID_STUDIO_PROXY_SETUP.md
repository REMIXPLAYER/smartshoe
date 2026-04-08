# Android Studio 代理配置指南

## 方法1：通过 Android Studio 设置界面配置（推荐）

1. 打开 Android Studio
2. 进入 **Settings** → **Appearance & Behavior** → **System Settings** → **HTTP Proxy**
3. 选择 **Manual proxy configuration**
4. 配置如下：
   - **HTTP Proxy**: 127.0.0.1
   - **Port**: 7890 (Clash Verge 默认混合端口)
   - 勾选 **No proxy for**: localhost, 127.0.0.1

5. 点击 **Check connection** 测试
6. 重启 Android Studio

## 方法2：通过 gradle.properties 配置

在 `~/.gradle/gradle.properties`（全局）或项目根目录的 `gradle.properties` 中添加：

```properties
# HTTP 代理
systemProp.http.proxyHost=127.0.0.1
systemProp.http.proxyPort=7890
systemProp.http.nonProxyHosts=localhost|127.0.0.1

# HTTPS 代理
systemProp.https.proxyHost=127.0.0.1
systemProp.https.proxyPort=7890
systemProp.https.nonProxyHosts=localhost|127.0.0.1

# SOCKS 代理（如果需要）
systemProp.socksProxyHost=127.0.0.1
systemProp.socksProxyPort=7890
```

## 方法3：终端启动 Android Studio（临时）

在终端中执行：

```bash
# 设置代理环境变量
export HTTP_PROXY=http://127.0.0.1:7890
export HTTPS_PROXY=http://127.0.0.1:7890
export NO_PROXY=localhost,127.0.0.1

# 启动 Android Studio
open -a "Android Studio"
```

## 验证 Clash Verge 端口

1. 打开 Clash Verge
2. 查看 **设置** → **端口设置**
3. 确认 **混合端口** (Mixed Port) 是多少（通常是 7890 或 1080）
4. 如果混合端口不是 7890，请使用实际端口号

## 验证代理是否生效

在 Android Studio 的 Terminal 中执行：

```bash
curl -I https://www.google.com
```

如果能返回 HTTP 200，说明代理配置成功。

## 常见问题

### 1. Gradle 同步失败
如果 Gradle 同步时仍然无法访问网络，尝试：
- File → Invalidate Caches / Restart
- 删除 `~/.gradle/caches/` 目录

### 2. 下载依赖慢
在 `build.gradle.kts` 中添加国内镜像：

```kotlin
repositories {
    google()
    mavenCentral()
    // 阿里云镜像
    maven { url = uri("https://maven.aliyun.com/repository/public") }
    maven { url = uri("https://maven.aliyun.com/repository/google") }
}
```

### 3. 模型下载不走代理
模型下载使用 OkHttp，需要在代码中设置代理：

```kotlin
// ModelDownloadManager.kt 中修改 client 配置
private val client = OkHttpClient.Builder()
    .proxy(Proxy(Proxy.Type.HTTP, InetSocketAddress("127.0.0.1", 7890)))
    .build()
```
