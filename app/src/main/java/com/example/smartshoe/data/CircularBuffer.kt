package com.example.smartshoe.data

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import java.util.concurrent.atomic.AtomicInteger

/**
 * 环形缓冲区实现 - 用于高效管理固定容量的传感器数据
 * 支持O(1)时间复杂度的添加操作
 * 线程安全设计，适用于高频数据写入场景
 */
class CircularBuffer<T>(private val capacity: Int) {

    private val buffer = Array<Any?>(capacity) { null }
    private val head = AtomicInteger(0) // 写入位置
    private val count = AtomicInteger(0) // 当前元素数量
    private var tail = 0 // 读取起始位置（仅在获取快照时使用）

    /**
     * 添加元素到缓冲区 - O(1)时间复杂度
     */
    fun add(element: T) {
        val currentHead = head.getAndIncrement()
        val index = currentHead % capacity
        buffer[index] = element

        // 更新元素数量（不超过容量）
        val currentCount = count.get()
        if (currentCount < capacity) {
            count.incrementAndGet()
        } else {
            // 缓冲区已满，移动tail
            tail = (currentHead + 1) % capacity
        }
    }

    /**
     * 获取当前元素数量
     */
    fun size(): Int = count.get()

    /**
     * 检查缓冲区是否为空
     */
    fun isEmpty(): Boolean = count.get() == 0

    /**
     * 检查缓冲区是否已满
     */
    fun isFull(): Boolean = count.get() >= capacity

    /**
     * 获取缓冲区容量
     */
    fun capacity(): Int = capacity

    /**
     * 清空缓冲区
     */
    fun clear() {
        for (i in buffer.indices) {
            buffer[i] = null
        }
        head.set(0)
        count.set(0)
        tail = 0
    }

    /**
     * 获取按时间顺序排列的元素列表（从旧到新）
     * 用于图表显示等需要有序数据的场景
     */
    @Suppress("UNCHECKED_CAST")
    fun toOrderedList(): List<T> {
        val currentCount = count.get()
        if (currentCount == 0) return emptyList()

        val result = ArrayList<T>(currentCount)
        val currentHead = head.get()

        if (currentCount < capacity) {
            // 缓冲区未满，从0到currentHead-1
            for (i in 0 until currentHead) {
                buffer[i]?.let { result.add(it as T) }
            }
        } else {
            // 缓冲区已满，从tail开始遍历
            val currentTail = currentHead % capacity
            for (i in 0 until capacity) {
                val index = (currentTail + i) % capacity
                buffer[index]?.let { result.add(it as T) }
            }
        }

        return result
    }

    /**
     * 获取最新的N个元素
     */
    @Suppress("UNCHECKED_CAST")
    fun getLatest(n: Int): List<T> {
        val currentCount = count.get()
        val actualN = minOf(n, currentCount)
        if (actualN == 0) return emptyList()

        val result = ArrayList<T>(actualN)
        val currentHead = head.get()

        for (i in 1..actualN) {
            val index = (currentHead - i + capacity) % capacity
            buffer[index]?.let { result.add(0, it as T) } // 插入头部保持顺序
        }

        return result
    }

    /**
     * 获取最新元素
     */
    @Suppress("UNCHECKED_CAST")
    fun getLatest(): T? {
        if (isEmpty()) return null
        val currentHead = head.get()
        val index = (currentHead - 1 + capacity) % capacity
        return buffer[index] as T?
    }

    /**
     * 转换为SnapshotStateList用于Compose - 优化版本
     * 只更新变化的部分，避免全量重建
     */
    fun toSnapshotStateList(existingList: SnapshotStateList<T>? = null): SnapshotStateList<T> {
        val orderedList = toOrderedList()

        if (existingList == null) {
            // 创建新列表
            return mutableStateListOf<T>().apply {
                addAll(orderedList)
            }
        }

        // 增量更新现有列表
        val currentSize = existingList.size
        val newSize = orderedList.size

        if (newSize > currentSize) {
            // 添加新元素
            existingList.addAll(orderedList.subList(currentSize, newSize))
        } else if (newSize < currentSize) {
            // 移除多余元素（环形缓冲区覆盖旧数据）
            val removeCount = currentSize - newSize
            repeat(removeCount) {
                if (existingList.isNotEmpty()) {
                    existingList.removeAt(0)
                }
            }
            // 更新可能变化的元素
            for (i in 0 until newSize) {
                if (existingList[i] != orderedList[i]) {
                    existingList[i] = orderedList[i]
                }
            }
        } else {
            // 数量相同，检查并更新变化的元素
            for (i in 0 until newSize) {
                if (existingList[i] != orderedList[i]) {
                    existingList[i] = orderedList[i]
                }
            }
        }

        return existingList
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
