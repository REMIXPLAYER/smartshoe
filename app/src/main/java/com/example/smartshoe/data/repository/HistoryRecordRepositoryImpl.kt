package com.example.smartshoe.data.repository

import com.example.smartshoe.config.AppConfig
import com.example.smartshoe.data.local.PagedDataCache
import com.example.smartshoe.domain.model.SensorDataPoint
import com.example.smartshoe.domain.model.SensorDataRecord
import com.example.smartshoe.domain.repository.HistoryRecordRepository
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * 历史记录仓库实现
 * 纯数据逻辑，不包含任何 Compose UI 状态
 */
@Singleton
class HistoryRecordRepositoryImpl @Inject constructor(
    private val sensorDataRemoteRepository: com.example.smartshoe.domain.repository.SensorDataRemoteRepository
) : HistoryRecordRepository {

    // 分页加载相关
    private var currentHistoryPage = 0
    private val historyPageSize = AppConfig.Cache.HISTORY_PAGE_SIZE
    private val historyCache = PagedDataCache<SensorDataRecord>(
        pageSize = AppConfig.Cache.HISTORY_PAGE_SIZE,
        maxCachedPages = AppConfig.Cache.MAX_CACHED_PAGES
    )

    // 当前缓存的查询参数（用于检测查询条件变化）
    private var cachedStartDate: Date? = null
    private var cachedEndDate: Date? = null

    // 回调
    private var callback: HistoryRecordRepository.HistoryRecordCallback? = null

    // 当前加载的记录列表
    private val _records = mutableListOf<SensorDataRecord>()
    private var _hasMoreRecords = true

    // 当前选中的记录
    private var _selectedRecord: SensorDataRecord? = null

    override fun setCallback(callback: HistoryRecordRepository.HistoryRecordCallback) {
        this.callback = callback
    }

    override fun getHistoryRecords(
        page: Int,
        size: Int,
        startDate: Date?,
        endDate: Date?
    ): List<SensorDataRecord> {
        // 检查查询条件是否变化
        if (hasQueryParamsChanged(startDate, endDate)) {
            historyCache.clear()
            cachedStartDate = startDate
            cachedEndDate = endDate
            _records.clear()
            currentHistoryPage = 0
        }

        // 检查缓存
        if (historyCache.isPageCached(page)) {
            val cachedData = historyCache.getPage(page) ?: emptyList()
            _hasMoreRecords = cachedData.size >= historyPageSize
            return cachedData
        }

        // 异步加载数据
        loadHistoryRecords(page, startDate, endDate)

        return _records.toList()
    }

    private fun loadHistoryRecords(page: Int, startDate: Date?, endDate: Date?) {
        if (startDate != null && endDate != null) {
            sensorDataRemoteRepository.getRecordsByTimeRange(
                startTime = startDate.time,
                endTime = endDate.time,
                page = page,
                size = historyPageSize
            ) { success, message, records, total ->
                handleQueryResult(success, message, records, total, page)
            }
        } else {
            sensorDataRemoteRepository.getUserRecords(
                page = page,
                size = historyPageSize
            ) { success, message, records, total ->
                handleQueryResult(success, message, records, total, page)
            }
        }
    }

    private fun handleQueryResult(
        success: Boolean,
        message: String,
        records: List<SensorDataRecord>?,
        total: Long,
        page: Int
    ) {
        if (success && records != null) {
            // 缓存页面数据
            historyCache.putPage(page, records)

            currentHistoryPage = page
            _hasMoreRecords = (page + 1) * historyPageSize < total

            if (page == 0) {
                _records.clear()
            }
            _records.addAll(records)

            callback?.onRecordsLoaded(_records.toList(), _hasMoreRecords)
        } else {
            callback?.onError(message)
        }
    }

    override suspend fun getHistoryRecordsAsync(
        page: Int,
        size: Int,
        startDate: Date?,
        endDate: Date?
    ): List<SensorDataRecord> {
        // 检查查询条件是否变化
        if (hasQueryParamsChanged(startDate, endDate)) {
            historyCache.clear()
            cachedStartDate = startDate
            cachedEndDate = endDate
            _records.clear()
            currentHistoryPage = 0
        }

        // 检查缓存
        if (historyCache.isPageCached(page)) {
            val cachedData = historyCache.getPage(page) ?: emptyList()
            _hasMoreRecords = cachedData.size >= historyPageSize
            return cachedData
        }

        // 异步加载并等待结果
        return suspendCancellableCoroutine { continuation ->
            val tempCallback = object : HistoryRecordRepository.HistoryRecordCallback {
                override fun onRecordsLoaded(records: List<SensorDataRecord>, hasMore: Boolean) {
                    if (continuation.isActive) {
                        continuation.resume(records)
                    }
                }

                override fun onRecordDetailLoaded(recordId: String, dataPoints: List<SensorDataPoint>) {
                    // 不处理详情回调
                }

                override fun onError(message: String) {
                    if (continuation.isActive) {
                        continuation.resume(emptyList())
                    }
                }
            }

            // 临时设置回调
            val originalCallback = callback
            callback = tempCallback

            loadHistoryRecords(page, startDate, endDate)

            // 恢复原始回调
            continuation.invokeOnCancellation {
                callback = originalCallback
            }
        }
    }

    private fun hasQueryParamsChanged(startDate: Date?, endDate: Date?): Boolean {
        return startDate?.time != cachedStartDate?.time || endDate?.time != cachedEndDate?.time
    }

    override fun getRecordDetail(recordId: String): List<SensorDataPoint> {
        var result: List<SensorDataPoint> = emptyList()

        sensorDataRemoteRepository.getRecordDetail(recordId) { success, message, record, dataPoints ->
            if (success && dataPoints != null) {
                result = dataPoints
                callback?.onRecordDetailLoaded(recordId, dataPoints)
            } else {
                callback?.onError(message)
            }
        }

        return result
    }

    override fun selectRecord(record: SensorDataRecord) {
        _selectedRecord = record
    }

    override fun clearSelection() {
        _selectedRecord = null
    }

    override fun loadMoreRecords(): Boolean {
        if (_hasMoreRecords) {
            loadHistoryRecords(currentHistoryPage + 1, cachedStartDate, cachedEndDate)
        }
        return _hasMoreRecords
    }

    override fun hasMoreRecords(): Boolean {
        return _hasMoreRecords
    }

    override fun refreshRecords() {
        historyCache.clear()
        _records.clear()
        currentHistoryPage = 0
        cachedStartDate = null
        cachedEndDate = null
        loadHistoryRecords(0, null, null)
    }

    override fun setDateRange(startDate: Date?, endDate: Date?) {
        cachedStartDate = startDate
        cachedEndDate = endDate
        refreshRecords()
    }

    override fun clearDateFilter() {
        cachedStartDate = null
        cachedEndDate = null
        refreshRecords()
    }

    override fun queryHistoryRecords(page: Int, append: Boolean, startDate: Date?, endDate: Date?) {
        if (!append) {
            _records.clear()
            currentHistoryPage = 0
        }
        loadHistoryRecords(page, startDate, endDate)
    }

    override fun loadMoreHistoryRecords(startDate: Date?, endDate: Date?) {
        if (_hasMoreRecords) {
            loadHistoryRecords(currentHistoryPage + 1, startDate, endDate)
        }
    }

    override fun refreshHistoryRecords(startDate: Date?, endDate: Date?) {
        historyCache.clear()
        _records.clear()
        currentHistoryPage = 0
        cachedStartDate = startDate
        cachedEndDate = endDate
        loadHistoryRecords(0, startDate, endDate)
    }

    override fun loadRecordDetail(recordId: String) {
        sensorDataRemoteRepository.getRecordDetail(recordId) { success, message, record, dataPoints ->
            if (success && dataPoints != null) {
                callback?.onRecordDetailLoaded(recordId, dataPoints)
            } else {
                callback?.onError(message)
            }
        }
    }

    override fun clearCache() {
        historyCache.clear()
        cachedStartDate = null
        cachedEndDate = null
    }

    override fun getDefaultStartDate(): Date {
        return Date(System.currentTimeMillis() - AppConfig.UI.DEFAULT_START_DATE_OFFSET_DAYS * AppConfig.UI.MS_PER_DAY)
    }

    override fun getDefaultEndDate(): Date {
        return java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 23)
            set(java.util.Calendar.MINUTE, 59)
            set(java.util.Calendar.SECOND, 59)
            set(java.util.Calendar.MILLISECOND, 999)
        }.time
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
}
