package com.example.smartshoe.data.manager

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.lang.ref.ReferenceQueue
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 内存泄漏检测器
 * 用于检测Activity、Fragment等组件的内存泄漏
 * 
 * 重构：使用 Hilt 注入替代单例模式
 */
@Singleton
class MemoryLeakDetector @Inject constructor(
    @ApplicationContext private val context: android.content.Context
) : Application.ActivityLifecycleCallbacks {

    private val referenceQueue = ReferenceQueue<Any>()
    private val watchedReferences = ConcurrentHashMap<String, KeyedWeakReference>()
    private val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    private var isInitialized = false

    companion object {
        private const val TAG = "MemoryLeakDetector"
        private const val WATCH_DELAY_MS = 5000L // 5秒后检查
        private const val RETAINED_THRESHOLD = 2 // 保留引用阈值
    }

    /**
     * 初始化内存泄漏检测器
     */
    fun init(application: Application) {
        if (isInitialized) return
        application.registerActivityLifecycleCallbacks(this)
        startLeakDetection()
        isInitialized = true
        Log.d(TAG, "MemoryLeakDetector initialized")
    }

    /**
     * 开始泄漏检测循环
     */
    private fun startLeakDetection() {
        scheduler.scheduleWithFixedDelay({
            detectLeaks()
        }, WATCH_DELAY_MS, WATCH_DELAY_MS, TimeUnit.MILLISECONDS)
    }

    /**
     * 检测内存泄漏
     */
    private fun detectLeaks() {
        // 处理引用队列中的对象
        var ref: KeyedWeakReference?
        while (referenceQueue.poll().also { ref = it as? KeyedWeakReference } != null) {
            val key = ref?.key
            if (key != null) {
                watchedReferences.remove(key)
                Log.d(TAG, "Object $key was properly garbage collected")
            }
        }

        // 检查可能泄漏的对象
        val retainedRefs = watchedReferences.values.filter { ref ->
            System.currentTimeMillis() - ref.watchTime > WATCH_DELAY_MS
        }

        retainedRefs.forEach { ref ->
            val key = ref.key
            val retainedCount = ref.retainedCount.incrementAndGet()

            if (retainedCount >= RETAINED_THRESHOLD) {
                Log.w(TAG, "Potential memory leak detected: $key (retained $retainedCount times)")
                // 可以在这里添加更多诊断信息，如堆栈跟踪
                dumpHeapIfNeeded(key)
            }
        }
    }

    /**
     * 监视对象是否被正确回收
     */
    fun watch(obj: Any, name: String) {
        val key = "${obj.javaClass.simpleName}@${System.identityHashCode(obj)}_$name"
        val ref = KeyedWeakReference(key, obj, referenceQueue)
        watchedReferences[key] = ref
        Log.d(TAG, "Watching object: $key")
    }

    /**
     * 停止监视对象
     */
    fun unwatch(obj: Any, name: String) {
        val key = "${obj.javaClass.simpleName}@${System.identityHashCode(obj)}_$name"
        watchedReferences.remove(key)
        Log.d(TAG, "Stopped watching object: $key")
    }

    /**
     * 在检测到泄漏时导出堆转储
     */
    private fun dumpHeapIfNeeded(key: String) {
        // 实际项目中可以使用 LeakCanary 或类似工具导出堆转储
        // 这里仅记录日志
        Log.w(TAG, "Consider dumping heap for analysis: $key")
    }

    /**
     * 获取当前监视的引用数量
     */
    fun getWatchedReferenceCount(): Int = watchedReferences.size

    /**
     * 清理所有监视的引用
     */
    fun clearAllWatchedReferences() {
        watchedReferences.clear()
        Log.d(TAG, "All watched references cleared")
    }

    /**
     * 销毁检测器
     */
    fun destroy() {
        scheduler.shutdown()
        watchedReferences.clear()
        Log.d(TAG, "MemoryLeakDetector destroyed")
    }

    // Activity生命周期回调
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        watch(activity, "Activity")
    }

    override fun onActivityDestroyed(activity: Activity) {
        // Activity销毁后，延迟检查是否被正确回收
        scheduler.schedule({
            unwatch(activity, "Activity")
        }, WATCH_DELAY_MS, TimeUnit.MILLISECONDS)
    }

    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    /**
     * 带键值的弱引用
     */
    private class KeyedWeakReference(
        val key: String,
        referent: Any,
        referenceQueue: ReferenceQueue<in Any>
    ) : WeakReference<Any>(referent, referenceQueue) {
        val watchTime = System.currentTimeMillis()
        val retainedCount = java.util.concurrent.atomic.AtomicInteger(0)
    }
}
