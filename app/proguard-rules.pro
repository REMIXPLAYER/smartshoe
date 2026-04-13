# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ============================================
# Compose 相关保留规则
# ============================================
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.material3.** { *; }

# 保留 @Composable 函数
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# 保留 Preview 函数（只在debug中使用，但保留以防万一）
-keepclassmembers class * {
    @androidx.compose.ui.tooling.preview.Preview <methods>;
}

# ============================================
# Kotlin 相关保留规则
# ============================================
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**

# 保留 Kotlin 协程
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# ============================================
# Hilt 依赖注入保留规则
# ============================================
-keep class * extends dagger.hilt.android.HiltActivity {}
-keep class * extends dagger.hilt.android.HiltFragment {}
-keep class * extends androidx.hilt.lifecycle.HiltViewModel {}

# 保留 Hilt 生成的组件
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.internal.GeneratedComponent {}

# 保留 @Inject 注解的构造函数
-keepclassmembers @javax.inject.Inject class * {
    <init>(...);
}

# 保留 @Module 和 @Provides
-keep @dagger.Module class * { *; }
-keepclassmembers class * {
    @dagger.Provides <methods>;
}

# ============================================
# 数据模型保留规则
# ============================================
# 保留数据类（用于JSON序列化/反序列化）
-keep class com.example.smartshoe.data.model.** { *; }
-keep class com.example.smartshoe.domain.** { *; }

# 保留密封类及其子类
-keep class com.example.smartshoe.data.remote.*Result { *; }
-keep class com.example.smartshoe.data.remote.*Result$* { *; }

# ============================================
# 日志移除规则
# ============================================
# 移除 Debug、Verbose、Info 级别的日志（保留 Warn 和 Error 用于线上问题排查）
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static boolean isLoggable(java.lang.String, int);
}

# 如果需要完全移除所有日志（包括 Warn 和 Error），取消下面的注释：
# -assumenosideeffects class android.util.Log {
#     public static *** d(...);
#     public static *** v(...);
#     public static *** i(...);
#     public static *** w(...);
#     public static *** e(...);
#     public static boolean isLoggable(java.lang.String, int);
# }

# ============================================
# 网络相关保留规则
# ============================================
# 保留 OkHttp 和 SSE 相关类
-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# 保留 Retrofit/OkHttp 的 Service 接口方法
-keepclassmembers interface * {
    @retrofit2.http.* <methods>;
}

# ============================================
# 性能优化规则
# ============================================
# 移除未使用的资源
-dontwarn android.content.res.Resources

# 优化字符串
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-dontpreverify

# ============================================
# 其他保留规则
# ============================================
# 保留枚举类
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 保留 Parcelable 实现
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# 保留 Serializable 实现
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
