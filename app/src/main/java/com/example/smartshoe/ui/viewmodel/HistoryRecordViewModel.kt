package com.example.smartshoe.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartshoe.domain.repository.HistoryRecordRepository
import com.example.smartshoe.domain.model.SensorDataPoint
import com.example.smartshoe.domain.model.SensorDataRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

/**
 * 历史记录视图模型
 * 管理历史记录相关的 UI 状态
 *
 * 重构后：
 * - 不再实现 Repository 回调接口
 * - 直接观察 Repository 的 StateFlow
 * - 所有操作通过挂起函数委托给 Repository
 */
@HiltViewModel
class HistoryRecordViewModel @Inject constructor(
    private val repository: HistoryRecordRepository
) : ViewModel() {

    // 直接暴露 Repository 的 StateFlow，避免重复定义
    val historyRecords: StateFlow<List<SensorDataRecord>> = repository.recordsFlow
    val isHistoryLoading: StateFlow<Boolean> = repository.isLoadingFlow
    val hasMoreHistoryPages: StateFlow<Boolean> = repository.hasMoreFlow
    val selectedRecordData: StateFlow<List<SensorDataPoint>> = repository.recordDetailFlow
    val isRecordDetailLoading: StateFlow<Boolean> = repository.isDetailLoadingFlow

    // 本地 UI 状态
    private val _selectedHistoryRecord = MutableStateFlow<SensorDataRecord?>(null)
    val selectedHistoryRecord: StateFlow<SensorDataRecord?> = _selectedHistoryRecord.asStateFlow()

    private val _historyStartDate = MutableStateFlow<Date?>(null)
    val historyStartDate: StateFlow<Date?> = _historyStartDate.asStateFlow()

    private val _historyEndDate = MutableStateFlow<Date?>(null)
    val historyEndDate: StateFlow<Date?> = _historyEndDate.asStateFlow()

    private val _queryExecuted = MutableStateFlow(false)
    val queryExecuted: StateFlow<Boolean> = _queryExecuted.asStateFlow()

    // 错误状态（从 Repository 错误流映射）
    val errorMessage: StateFlow<String?> = repository.errorFlow

    init {
        // 收集 Repository 错误流，可在此添加额外处理（如日志、统计）
        viewModelScope.launch {
            repository.errorFlow.collect { error ->
                // 错误已通过 errorMessage 暴露给 UI
                // 可在此添加日志记录或埋点
            }
        }
    }

    /**
     * 查询历史记录
     */
    fun queryHistoryRecords(page: Int = 0, append: Boolean = false) {
        _queryExecuted.value = true
        if (!append) {
            _selectedHistoryRecord.value = null
        }
        viewModelScope.launch {
            repository.queryHistoryRecords(
                page = page,
                append = append,
                startDate = _historyStartDate.value,
                endDate = _historyEndDate.value
            )
        }
    }

    /**
     * 加载更多历史记录
     */
    fun loadMoreHistoryRecords() {
        if (isHistoryLoading.value || !hasMoreHistoryPages.value) return
        viewModelScope.launch {
            repository.loadMoreHistoryRecords(
                startDate = _historyStartDate.value,
                endDate = _historyEndDate.value
            )
        }
    }

    /**
     * 刷新历史记录
     */
    fun refreshHistoryRecords() {
        viewModelScope.launch {
            repository.refreshHistoryRecords(
                startDate = _historyStartDate.value,
                endDate = _historyEndDate.value
            )
        }
    }

    /**
     * 选择记录
     */
    fun selectRecord(record: SensorDataRecord?) {
        _selectedHistoryRecord.value = record
        repository.selectRecord(record)
    }

    /**
     * 更新开始日期
     */
    fun updateStartDate(date: Date?) {
        _historyStartDate.value = date
        repository.setDateRange(date, _historyEndDate.value)
    }

    /**
     * 更新结束日期
     */
    fun updateEndDate(date: Date?) {
        _historyEndDate.value = date
        repository.setDateRange(_historyStartDate.value, date)
    }

    /**
     * 重置日期范围
     */
    fun resetDateRange() {
        val start = repository.getDefaultStartDate()
        val end = repository.getDefaultEndDate()
        _historyStartDate.value = start
        _historyEndDate.value = end
        repository.setDateRange(start, end)
    }

    /**
     * 清空所有历史记录数据
     */
    fun clearHistoryData() {
        _selectedHistoryRecord.value = null
        _queryExecuted.value = false
        repository.clearSelection()
        repository.clearCache()
    }
}
