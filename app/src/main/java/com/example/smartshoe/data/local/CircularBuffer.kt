package com.example.smartshoe.data.local

import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * 环形缓冲区实现 - 用于高效管理固定容量的传感器数据
 * 支持O(1)时间复杂度的添加操作
 * 线程安全设计，适用于高频数据写入场景
 *
 * 重构：移除了 Compose 依赖，保持数据层纯净
 * 注：toSnapshotStateList() 方法已移到 UI 层的扩展函数中
 */
class CircularBuffer<T>(private val capacity: Int) {

    private val buffer = Array<Any?>(capacity) { null }
    private var head = 0
    private var count = 0
    private val lock = ReentrantReadWriteLock()

    /**
     * 添加元素到缓冲区 - O(1)时间复杂度
     */
    fun add(element: T) {
        lock.write {
            buffer[head] = element
            head = (head + 1) % capacity
            if (count < capacity) {
                count++
            }
        }
    }

    /**
     * 获取当前元素数量
     */
    fun size(): Int = lock.read { count }

    /**
     * 检查缓冲区是否为空
     */
    fun isEmpty(): Boolean = lock.read { count == 0 }

    /**
     * 检查缓冲区是否已满
     */
    fun isFull(): Boolean = lock.read { count >= capacity }

    /**
     * 获取缓冲区容量
     */
    fun capacity(): Int = capacity

    /**
     * 清空缓冲区
     */
    fun clear() {
        lock.write {
            for (i in buffer.indices) {
                buffer[i] = null
            }
            head = 0
            count = 0
        }
    }

    /**
     * 获取按时间顺序排列的元素列表（从旧到新）
     * 用于图表显示等需要有序数据的场景
     */
    @Suppress("UNCHECKED_CAST")
    fun toOrderedList(): List<T> {
        lock.read {
            if (count == 0) return emptyList()

            val result = ArrayList<T>(count)

            if (count < capacity) {
                val start = (head - count + capacity) % capacity
                for (i in 0 until count) {
                    val index = (start + i) % capacity
                    buffer[index]?.let { result.add(it as T) }
                }
            } else {
                for (i in 0 until capacity) {
                    val index = (head + i) % capacity
                    buffer[index]?.let { result.add(it as T) }
                }
            }

            return result
        }
    }

    /**
     * 获取最新的N个元素（按从新到旧顺序）
     * 优化：使用预计算索引 + addLast，避免 add(0, it) 的 O(n²) 问题
     */
    @Suppress("UNCHECKED_CAST")
    fun getLatest(n: Int): List<T> {
        lock.read {
            val actualN = minOf(n, count)
            if (actualN == 0) return emptyList()

            val result = ArrayList<T>(actualN)

            // 从最新元素开始，按从新到旧顺序遍历
            // 最新元素在 head - 1 位置
            for (i in 0 until actualN) {
                val index = (head - 1 - i + capacity) % capacity
                buffer[index]?.let { result.add(it as T) }
            }

            return result
        }
    }

    /**
     * 获取最新元素
     */
    @Suppress("UNCHECKED_CAST")
    fun getLatest(): T? {
        lock.read {
            if (count == 0) return null
            val index = (head - 1 + capacity) % capacity
            return buffer[index] as T?
        }
    }
}

/**
 * 分页数据缓存管理器
 * 用于历史记录数据的分页加载和缓存
 */
class PagedDataCache<T>(
    private val pageSize: Int = 20,
    private val maxCachedPages: Int = 5
) {
    private val cache = mutableMapOf<Int, List<T>>()
    private val pageAccessOrder = mutableListOf<Int>()

    /**
     * 获取缓存的页面
     */
    fun getPage(page: Int): List<T>? {
        val data = cache[page]
        if (data != null) {
            // 更新访问顺序（LRU策略）
            pageAccessOrder.remove(page)
            pageAccessOrder.add(page)
        }
        return data
    }

    /**
     * 缓存页面数据
     */
    fun putPage(page: Int, data: List<T>) {
        // 检查缓存是否已满
        if (cache.size >= maxCachedPages && !cache.containsKey(page)) {
            // 移除最久未访问的页面
            val oldestPage = pageAccessOrder.removeAt(0)
            cache.remove(oldestPage)
        }

        cache[page] = data
        if (!pageAccessOrder.contains(page)) {
            pageAccessOrder.add(page)
        }
    }

    /**
     * 清除所有缓存
     */
    fun clear() {
        cache.clear()
        pageAccessOrder.clear()
    }

    /**
     * 获取缓存大小
     */
    fun getCachedPageCount(): Int = cache.size

    /**
     * 检查页面是否已缓存
     */
    fun isPageCached(page: Int): Boolean = cache.containsKey(page)
}
