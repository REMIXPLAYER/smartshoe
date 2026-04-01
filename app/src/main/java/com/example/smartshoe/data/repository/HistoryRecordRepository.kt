package com.example.smartshoe.data.repository

import com.example.smartshoe.data.local.PagedDataCache
import com.example.smartshoe.data.manager.SensorDataManager
import com.example.smartshoe.data.model.SensorDataPoint
import com.example.smartshoe.data.model.SensorDataRecord
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 历史记录仓库
 * 纯数据逻辑，不包含任何 Compose UI 状态
 */
@Singleton
class HistoryRecordRepository @Inject constructor(
    private val sensorDataManager: SensorDataManager
) {
    // 分页加载相关
    private var currentHistoryPage = 0
    private val historyPageSize = 20
    private val historyCache = PagedDataCache<SensorDataRecord>(pageSize = 20, maxCachedPages = 5)

    // 当前缓存的查询参数（用于检测查询条件变化）
    private var cachedStartDate: Date? = null
    private var cachedEndDate: Date? = null

    // 回调接口
    interface HistoryRecordCallback {
        fun onRecordsLoaded(records: List<SensorDataRecord>, page: Int, hasMorePages: Boolean)
        fun onRecordDetailLoaded(dataPoints: List<SensorDataPoint>)
        fun onError(message: String)
        fun onLoadingStateChanged(isLoading: Boolean)
        fun onDetailLoadingStateChanged(isLoading: Boolean)
    }

    private var callback: HistoryRecordCallback? = null

    fun setCallback(callback: HistoryRecordCallback?) {
        this.callback = callback
    }

    /**
     * 查询历史记录
     * @param page 页码，从0开始
     * @param append 是否追加到现有列表（加载更多）
     * @param startDate 开始日期，null表示不限制
     * @param endDate 结束日期，null表示不限制
     */
    fun queryHistoryRecords(
        page: Int = 0,
        append: Boolean = false,
        startDate: Date? = null,
        endDate: Date? = null
    ) {
        // 检查查询条件是否变化（日期范围变化时需要强制刷新）
        if (hasQueryParamsChanged(startDate, endDate)) {
            historyCache.clear()
            cachedStartDate = startDate
            cachedEndDate = endDate
            // 日期变化时强制刷新，不使用任何缓存
            forceRefreshHistoryRecords(startDate, endDate)
            return
        }

        // 检查缓存
        if (!append && historyCache.isPageCached(page)) {
            val cachedData = historyCache.getPage(page)
            if (cachedData != null) {
                callback?.onRecordsLoaded(cachedData, page, hasMorePages(cachedData.size))
                return
            }
        }

        callback?.onLoadingStateChanged(true)

        // 根据是否有日期范围选择查询方式
        if (startDate != null && endDate != null) {
            sensorDataManager.getRecordsByTimeRange(
                startTime = startDate.time,
                endTime = endDate.time,
                page = page,
                size = historyPageSize
            ) { success, message, records, total ->
                handleQueryResult(success, message, records, total, page, append)
            }
        } else {
            sensorDataManager.getUserRecords(
                page = page,
                size = historyPageSize
            ) { success, message, records, total ->
                handleQueryResult(success, message, records, total, page, append)
            }
        }
    }

    /**
     * 强制刷新历史记录（不使用缓存）
     * 在查询参数变化时调用，确保获取最新数据
     */
    private fun forceRefreshHistoryRecords(startDate: Date?, endDate: Date?) {
        callback?.onLoadingStateChanged(true)

        if (startDate != null && endDate != null) {
            sensorDataManager.getRecordsByTimeRange(
                startTime = startDate.time,
                endTime = endDate.time,
                page = 0,
                size = historyPageSize,
                useCache = false  // 关键：不使用缓存
            ) { success, message, records, total ->
                handleQueryResult(success, message, records, total, page = 0, append = false)
            }
        } else {
            sensorDataManager.getUserRecords(
                page = 0,
                size = historyPageSize,
                useCache = false  // 关键：不使用缓存
            ) { success, message, records, total ->
                handleQueryResult(success, message, records, total, page = 0, append = false)
            }
        }
    }

    /**
     * 检查查询参数是否变化
     */
    private fun hasQueryParamsChanged(startDate: Date?, endDate: Date?): Boolean {
        return startDate?.time != cachedStartDate?.time || endDate?.time != cachedEndDate?.time
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
        callback?.onLoadingStateChanged(false)
        if (success && records != null) {
            // 缓存页面数据
            historyCache.putPage(page, records)

            currentHistoryPage = page
            val hasMorePages = (page + 1) * historyPageSize < total
            callback?.onRecordsLoaded(records, page, hasMorePages)
        } else {
            callback?.onError(message)
        }
    }

    private fun hasMorePages(currentSize: Int): Boolean {
        return currentSize >= historyPageSize
    }

    /**
     * 加载更多历史记录
     */
    fun loadMoreHistoryRecords(startDate: Date? = null, endDate: Date? = null) {
        queryHistoryRecords(currentHistoryPage + 1, append = true, startDate = startDate, endDate = endDate)
    }

    /**
     * 刷新历史记录（清除缓存并重新加载）
     */
    fun refreshHistoryRecords(startDate: Date? = null, endDate: Date? = null) {
        historyCache.clear()
        queryHistoryRecords(page = 0, append = false, startDate = startDate, endDate = endDate)
    }

    /**
     * 加载记录详情
     */
    fun loadRecordDetail(recordId: String) {
        callback?.onDetailLoadingStateChanged(true)

        sensorDataManager.getRecordDetail(recordId) { success, message, record, dataPoints ->
            callback?.onDetailLoadingStateChanged(false)
            if (success && dataPoints != null) {
                callback?.onRecordDetailLoaded(dataPoints)
            } else {
                callback?.onError(message)
            }
        }
    }

    /**
     * 获取缓存的记录（用于避免重复加载）
     */
    fun getCachedRecords(page: Int): List<SensorDataRecord>? {
        return historyCache.getPage(page)
    }

    /**
     * 检查页面是否已缓存
     */
    fun isPageCached(page: Int): Boolean {
        return historyCache.isPageCached(page)
    }

    /**
     * 清除缓存
     */
    fun clearCache() {
        historyCache.clear()
        cachedStartDate = null
        cachedEndDate = null
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
            return java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 23)
                set(java.util.Calendar.MINUTE, 59)
                set(java.util.Calendar.SECOND, 59)
                set(java.util.Calendar.MILLISECOND, 999)
            }.time
        }
    }
}
