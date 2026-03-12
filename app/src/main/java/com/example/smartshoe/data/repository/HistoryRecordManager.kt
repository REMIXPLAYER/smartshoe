package com.example.smartshoe.data.repository

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.smartshoe.data.PagedDataCache
import com.example.smartshoe.data.SensorDataPoint
import com.example.smartshoe.data.remote.SensorDataRecord
import java.util.Calendar
import java.util.Date

/**
 * 历史记录管理器
 * 负责历史记录的查询、缓存、分页加载等操作
 */
class HistoryRecordManager(
    private val sensorDataManager: SensorDataManager
) {
    // 历史记录相关状态
    val historyRecords = mutableStateListOf<SensorDataRecord>()
    var selectedHistoryRecord by mutableStateOf<SensorDataRecord?>(null)
    val selectedRecordData = mutableStateListOf<SensorDataPoint>()
    var isHistoryLoading by mutableStateOf(false)
    var isRecordDetailLoading by mutableStateOf(false)
    var queryExecuted by mutableStateOf(false)

    // 分页加载相关
    private var currentHistoryPage = 0
    private val historyPageSize = 20
    var hasMoreHistoryPages by mutableStateOf(true)
    private val historyCache = PagedDataCache<SensorDataRecord>(pageSize = 20, maxCachedPages = 5)

    // 日期范围 - 默认为null，表示查询所有记录
    var historyStartDate by mutableStateOf<Date?>(null)
    var historyEndDate by mutableStateOf<Date?>(null)

    // 回调
    var onError: ((String) -> Unit)? = null
    var onRecordsLoaded: (() -> Unit)? = null
    var onRecordDetailLoaded: (() -> Unit)? = null

    /**
     * 查询历史记录
     * @param page 页码，从0开始
     * @param append 是否追加到现有列表（加载更多）
     */
    fun queryHistoryRecords(page: Int = 0, append: Boolean = false) {
        // 检查缓存
        if (!append && historyCache.isPageCached(page)) {
            val cachedData = historyCache.getPage(page)
            if (cachedData != null) {
                historyRecords.clear()
                historyRecords.addAll(cachedData)
                queryExecuted = true
                onRecordsLoaded?.invoke()
                return
            }
        }

        isHistoryLoading = true
        if (!append) {
            historyRecords.clear()
            selectedHistoryRecord = null
            selectedRecordData.clear()
            currentHistoryPage = 0
            hasMoreHistoryPages = true
        }
        queryExecuted = true

        // 根据是否有日期范围选择查询方式
        if (historyStartDate != null && historyEndDate != null) {
            // 按时间范围查询
            sensorDataManager.getRecordsByTimeRange(
                startTime = historyStartDate!!.time,
                endTime = historyEndDate!!.time,
                page = page,
                size = historyPageSize
            ) { success, message, records, total ->
                handleQueryResult(success, message, records, total, page, append)
            }
        } else {
            // 查询所有记录
            sensorDataManager.getUserRecords(
                page = page,
                size = historyPageSize
            ) { success, message, records, total ->
                handleQueryResult(success, message, records, total, page, append)
            }
        }
    }

    /**
     * 处理查询结果
     */
    private fun handleQueryResult(
        success: Boolean,
        message: String,
        records: List<SensorDataRecord>?,
        total: Long,
        page: Int,
        append: Boolean
    ) {
        isHistoryLoading = false
        if (success && records != null) {
            // 缓存页面数据
            historyCache.putPage(page, records)

            if (append) {
                // 追加模式：避免重复添加
                val existingIds = historyRecords.map { it.recordId }.toSet()
                val newRecords = records.filter { it.recordId !in existingIds }
                historyRecords.addAll(newRecords)
            } else {
                historyRecords.addAll(records)
            }

            // 更新分页状态
            currentHistoryPage = page
            hasMoreHistoryPages = (page + 1) * historyPageSize < total
            onRecordsLoaded?.invoke()
        } else {
            onError?.invoke(message)
        }
    }

    /**
     * 加载更多历史记录
     */
    fun loadMoreHistoryRecords() {
        if (isHistoryLoading || !hasMoreHistoryPages) return
        queryHistoryRecords(currentHistoryPage + 1, append = true)
    }

    /**
     * 刷新历史记录（清除缓存并重新加载）
     */
    fun refreshHistoryRecords() {
        historyCache.clear()
        queryHistoryRecords(page = 0, append = false)
    }

    /**
     * 加载记录详情
     */
    fun loadRecordDetail(recordId: String) {
        isRecordDetailLoading = true
        selectedRecordData.clear()

        sensorDataManager.getRecordDetail(recordId) { success, message, record, dataPoints ->
            isRecordDetailLoading = false
            if (success && dataPoints != null) {
                selectedRecordData.addAll(dataPoints)
                onRecordDetailLoaded?.invoke()
            } else {
                onError?.invoke(message)
            }
        }
    }

    /**
     * 选择记录
     */
    fun selectRecord(record: SensorDataRecord?) {
        selectedHistoryRecord = record
        if (record != null) {
            loadRecordDetail(record.recordId)
        } else {
            selectedRecordData.clear()
        }
    }

    /**
     * 清空所有历史记录数据
     */
    fun clearHistoryData() {
        historyRecords.clear()
        selectedHistoryRecord = null
        selectedRecordData.clear()
        isHistoryLoading = false
        isRecordDetailLoading = false
        historyCache.clear()
        currentHistoryPage = 0
        hasMoreHistoryPages = true
    }

    /**
     * 重置日期范围为默认值
     */
    fun resetDateRange() {
        historyStartDate = getDefaultStartDate()
        historyEndDate = getDefaultEndDate()
    }

    /**
     * 更新开始日期
     */
    fun updateStartDate(date: Date) {
        historyStartDate = date
    }

    /**
     * 更新结束日期
     */
    fun updateEndDate(date: Date) {
        historyEndDate = date
    }

    /**
     * 获取用户的传感器数据记录列表
     */
    fun getUserSensorRecords(
        page: Int = 0,
        onResult: (Boolean, String, List<SensorDataRecord>?) -> Unit
    ) {
        sensorDataManager.getUserRecords(page = page) { success, message, records, total ->
            onResult(success, "$message (共${total}条)", records)
        }
    }

    /**
     * 清除缓存（用于上传成功后刷新数据）
     */
    fun clearCache() {
        historyCache.clear()
    }

    companion object {
        /**
         * 获取默认开始日期（昨天）
         */
        fun getDefaultStartDate(): Date {
            return Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000)
        }

        /**
         * 获取默认结束日期（今天23:59:59）
         */
        fun getDefaultEndDate(): Date {
            return Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.time
        }
    }
}
