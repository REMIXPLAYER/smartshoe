package com.example.smartshoe.data.repository

import android.util.Log
import com.example.smartshoe.config.AppConfig
import com.example.smartshoe.data.local.LocalDataSource
import com.example.smartshoe.data.remote.SensorDataApiService
import com.example.smartshoe.di.ApplicationScope
import com.example.smartshoe.domain.model.SensorDataPoint
import com.example.smartshoe.domain.model.SensorDataRecord
import com.example.smartshoe.domain.repository.AuthRepository
import com.example.smartshoe.domain.repository.HistoryRecordRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 历史记录仓库实现
 * 纯 Flow 驱动，消除回调模式
 *
 * 职责：
 * - 协调远程和本地数据源
 * - 管理查询状态（加载中、错误、结果）
 * - 分页加载和缓存
 *
 * 状态管理：
 * - 所有状态通过 StateFlow 暴露
 * - 操作命令为挂起函数，在内部作用域执行
 */
@Singleton
class HistoryRecordRepositoryImpl @Inject constructor(
    private val localDataSource: LocalDataSource,
    private val apiService: SensorDataApiService,
    private val authRepository: AuthRepository,
    @ApplicationScope private val applicationScope: CoroutineScope
) : HistoryRecordRepository {

    companion object {
        private const val TAG = "HistoryRecordRepo"
    }

    private val _recordsFlow = MutableStateFlow<List<SensorDataRecord>>(emptyList())
    override val recordsFlow: StateFlow<List<SensorDataRecord>> = _recordsFlow.asStateFlow()

    private val _isLoadingFlow = MutableStateFlow(false)
    override val isLoadingFlow: StateFlow<Boolean> = _isLoadingFlow.asStateFlow()

    private val _hasMoreFlow = MutableStateFlow(true)
    override val hasMoreFlow: StateFlow<Boolean> = _hasMoreFlow.asStateFlow()

    private val _errorFlow = MutableStateFlow<String?>(null)
    override val errorFlow: StateFlow<String?> = _errorFlow.asStateFlow()

    private val _recordDetailFlow = MutableStateFlow<List<SensorDataPoint>>(emptyList())
    override val recordDetailFlow: StateFlow<List<SensorDataPoint>> = _recordDetailFlow.asStateFlow()

    private val _isDetailLoadingFlow = MutableStateFlow(false)
    override val isDetailLoadingFlow: StateFlow<Boolean> = _isDetailLoadingFlow.asStateFlow()

    // ========== 内部状态 ==========

    private var currentPage = 0
    private var selectedRecord: SensorDataRecord? = null
    private var startDate: Date? = null
    private var endDate: Date? = null

    // 分页数据缓存
    private val historyCache = PagedDataCache<SensorDataRecord>(AppConfig.Cache.MAX_CACHED_PAGES)

    // ========== 操作命令（挂起函数） ==========

    override suspend fun queryHistoryRecords(
        page: Int,
        append: Boolean,
        startDate: Date?,
        endDate: Date?
    ) {
        updateDateRange(startDate, endDate)

        if (!append) {
            currentPage = 0
            _recordsFlow.value = emptyList()
            _hasMoreFlow.value = true
            historyCache.clear()
        }

        _isLoadingFlow.value = true
        _errorFlow.value = null

        try {
            val result = fetchRecordsFromRemote(currentPage, this.startDate, this.endDate)
            handleRecordsResult(result, append)
        } catch (e: Exception) {
            Log.e(TAG, "Query error: ${e.message}")
            _errorFlow.value = "查询失败: ${e.message}"
        } finally {
            _isLoadingFlow.value = false
        }
    }

    override suspend fun loadMoreHistoryRecords(startDate: Date?, endDate: Date?) {
        if (_isLoadingFlow.value || !_hasMoreFlow.value) return

        updateDateRange(startDate, endDate)
        currentPage++

        _isLoadingFlow.value = true
        _errorFlow.value = null

        try {
            val result = fetchRecordsFromRemote(currentPage, this.startDate, this.endDate)
            handleRecordsResult(result, append = true)
        } catch (e: Exception) {
            Log.e(TAG, "Load more error: ${e.message}")
            _errorFlow.value = "加载更多失败: ${e.message}"
            currentPage-- // 回退页码
        } finally {
            _isLoadingFlow.value = false
        }
    }

    override suspend fun refreshHistoryRecords(startDate: Date?, endDate: Date?) {
        queryHistoryRecords(page = 0, append = false, startDate = startDate, endDate = endDate)
    }

    override suspend fun loadRecordDetail(recordId: String) {
        _isDetailLoadingFlow.value = true
        _errorFlow.value = null

        try {
            val token = authRepository.getToken()
            if (token.isNullOrEmpty()) {
                _errorFlow.value = "请先登录"
                return
            }

            when (val result = apiService.getRecordDetail(recordId, token, useCache = true)) {
                is com.example.smartshoe.data.remote.SensorDataDetailResult.Success -> {
                    _recordDetailFlow.value = result.dataPoints
                }
                is com.example.smartshoe.data.remote.SensorDataDetailResult.Error -> {
                    _errorFlow.value = result.message
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Load detail error: ${e.message}")
            _errorFlow.value = "加载详情失败: ${e.message}"
        } finally {
            _isDetailLoadingFlow.value = false
        }
    }

    // ========== 状态管理 ==========

    override fun selectRecord(record: SensorDataRecord?) {
        selectedRecord = record
        if (record != null) {
            applicationScope.launch {
                loadRecordDetail(record.recordId)
            }
        } else {
            _recordDetailFlow.value = emptyList()
        }
    }

    override fun getSelectedRecord(): SensorDataRecord? = selectedRecord

    override fun clearSelection() {
        selectedRecord = null
        _recordDetailFlow.value = emptyList()
    }

    override fun setDateRange(startDate: Date?, endDate: Date?) {
        updateDateRange(startDate, endDate)
    }

    override fun getDefaultStartDate(): Date {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, -30)
        return calendar.time
    }

    override fun getDefaultEndDate(): Date {
        return Calendar.getInstance().time
    }

    override fun clearCache() {
        historyCache.clear()
        currentPage = 0
    }

    // ========== 私有方法 ==========

    private fun updateDateRange(startDate: Date?, endDate: Date?) {
        this.startDate = startDate
        this.endDate = endDate
    }

    private suspend fun fetchRecordsFromRemote(
        page: Int,
        startDate: Date?,
        endDate: Date?
    ): com.example.smartshoe.data.remote.SensorDataRecordsResult {
        val token = authRepository.getToken()
            ?: return com.example.smartshoe.data.remote.SensorDataRecordsResult.Error("请先登录")

        return if (startDate != null && endDate != null) {
            apiService.getRecordsByTimeRange(
                token = token,
                startTime = startDate.time,
                endTime = endDate.time,
                page = page,
                size = AppConfig.Cache.HISTORY_PAGE_SIZE,
                useCache = true
            )
        } else {
            apiService.getUserRecords(
                token = token,
                page = page,
                size = AppConfig.Cache.HISTORY_PAGE_SIZE,
                useCache = true
            )
        }
    }

    private fun handleRecordsResult(
        result: com.example.smartshoe.data.remote.SensorDataRecordsResult,
        append: Boolean
    ) {
        when (result) {
            is com.example.smartshoe.data.remote.SensorDataRecordsResult.Success -> {
                val records = result.records
                val hasMore = records.size >= AppConfig.Cache.HISTORY_PAGE_SIZE

                // 更新缓存
                historyCache.put(currentPage, records)

                // 更新状态
                _recordsFlow.value = if (append) {
                    val existingIds = _recordsFlow.value.map { it.recordId }.toSet()
                    _recordsFlow.value + records.filter { it.recordId !in existingIds }
                } else {
                    records
                }
                _hasMoreFlow.value = hasMore
            }
            is com.example.smartshoe.data.remote.SensorDataRecordsResult.Error -> {
                _errorFlow.value = result.message
            }
        }
    }

    // ========== 数据缓存 ==========

    /**
     * 分页数据缓存
     */
    private class PagedDataCache<T>(private val maxPages: Int) {
        private val cache = LinkedHashMap<Int, List<T>>(maxPages, 0.75f, true)

        fun put(page: Int, data: List<T>) {
            if (cache.size >= maxPages) {
                val oldestKey = cache.keys.firstOrNull()
                oldestKey?.let { cache.remove(it) }
            }
            cache[page] = data
        }

        fun get(page: Int): List<T>? = cache[page]

        fun clear() {
            cache.clear()
        }
    }
}
