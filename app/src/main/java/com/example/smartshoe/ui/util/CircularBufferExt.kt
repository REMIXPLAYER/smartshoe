package com.example.smartshoe.ui.util

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.example.smartshoe.data.local.CircularBuffer

/**
 * CircularBuffer 的 UI 层扩展函数
 * 将数据层与 UI 层分离，符合 Clean Architecture 原则
 */

/**
 * 将 CircularBuffer 转换为 SnapshotStateList 用于 Compose
 * 只更新变化的部分，避免全量重建
 */
fun <T> CircularBuffer<T>.toSnapshotStateList(existingList: SnapshotStateList<T>? = null): SnapshotStateList<T> {
    val orderedList = this.toOrderedList()

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
