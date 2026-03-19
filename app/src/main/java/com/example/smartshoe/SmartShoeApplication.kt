package com.example.smartshoe

import android.app.Application
import com.example.smartshoe.data.manager.MemoryLeakDetector
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * SmartShoe 应用入口
 * 使用 @HiltAndroidApp 启用 Hilt 依赖注入
 *
 * 应用级初始化：
 * - MemoryLeakDetector: 全局内存泄漏检测，监听所有Activity生命周期
 */
@HiltAndroidApp
class SmartShoeApplication : Application() {

    /**
     * Hilt EntryPoint 用于在Application中获取依赖
     */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ApplicationEntryPoint {
        fun memoryLeakDetector(): MemoryLeakDetector
    }

    override fun onCreate() {
        super.onCreate()

        // 初始化应用级组件
        initApplicationLevelComponents()
    }

    /**
     * 初始化应用级组件
     * 这些组件只需要在应用启动时初始化一次
     */
    private fun initApplicationLevelComponents() {
        // 注意：Hilt在Application.onCreate时还未完成初始化
        // 延迟到第一个Activity创建时再初始化MemoryLeakDetector
        // 或者使用EntryPoint手动获取
    }

    /**
     * 获取内存泄漏检测器实例
     * 供第一个Activity调用以完成初始化
     */
    fun getMemoryLeakDetector(): MemoryLeakDetector {
        val entryPoint = dagger.hilt.EntryPoints.get(this, ApplicationEntryPoint::class.java)
        return entryPoint.memoryLeakDetector()
    }
}
