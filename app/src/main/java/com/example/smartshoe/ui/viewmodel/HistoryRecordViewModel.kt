package com.example.smartshoe.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartshoe.data.model.SensorDataPoint
import com.example.smartshoe.data.model.SensorDataRecord
import com.example.smartshoe.data.repository.HistoryRecordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

/**
 * 历史记录视图模型
 * 管理历史记录相关的 UI 状态（使用 StateFlow）
 */
@HiltViewModel
class HistoryRecordViewModel @Inject constructor(
    private val repository: HistoryRecordRepository
) : ViewModel(), HistoryRecordRepository.HistoryRecordCallback {

    // UI 状态 - 使用 StateFlow 管理
    private val _historyRecords = MutableStateFlow<List<SensorDataRecord>>(emptyList())
    val historyRecords: StateFlow<List<SensorDataRecord>> = _historyRecords.asStateFlow()

    private val _selectedHistoryRecord = MutableStateFlow<SensorDataRecord?>(null)
    val selectedHistoryRecord: StateFlow<SensorDataRecord?> = _selectedHistoryRecord.asStateFlow()

    private val _selectedRecordData = MutableStateFlow<List<SensorDataPoint>>(emptyList())
    val selectedRecordData: StateFlow<List<SensorDataPoint>> = _selectedRecordData.asStateFlow()

    private val _isHistoryLoading = MutableStateFlow(false)
    val isHistoryLoading: StateFlow<Boolean> = _isHistoryLoading.asStateFlow()

    private val _isRecordDetailLoading = MutableStateFlow(false)
    val isRecordDetailLoading: StateFlow<Boolean> = _isRecordDetailLoading.asStateFlow()

    private val _queryExecuted = MutableStateFlow(false)
    val queryExecuted: StateFlow<Boolean> = _queryExecuted.asStateFlow()

    private val _hasMoreHistoryPages = MutableStateFlow(true)
    val hasMoreHistoryPages: StateFlow<Boolean> = _hasMoreHistoryPages.asStateFlow()

    // 日期范围 - 默认不筛选（null表示不限制日期）
    private val _historyStartDate = MutableStateFlow<Date?>(null)
    val historyStartDate: StateFlow<Date?> = _historyStartDate.asStateFlow()

    private val _historyEndDate = MutableStateFlow<Date?>(null)
    val historyEndDate: StateFlow<Date?> = _historyEndDate.asStateFlow()

    // 错误回调（保留回调机制用于与 Repository 通信）
    var onError: ((String) -> Unit)? = null
    var onRecordsLoaded: (() -> Unit)? = null
    var onRecordDetailLoaded: (() -> Unit)? = null

    init {
        repository.setCallback(this)
    }

    /**
     * 查询历史记录
     */
    fun queryHistoryRecords(page: Int = 0, append: Boolean = false) {
        _queryExecuted.value = true
        if (!append) {
            _historyRecords.value = emptyList()
            _selectedHistoryRecord.value = null
            _selectedRecordData.value = emptyList()
            _hasMoreHistoryPages.value = true
        }
        repository.queryHistoryRecords(page, append, _historyStartDate.value, _historyEndDate.value)
    }

    /**
     * 加载更多历史记录
     */
    fun loadMoreHistoryRecords() {
        if (_isHistoryLoading.value || !_hasMoreHistoryPages.value) return
        repository.loadMoreHistoryRecords(_historyStartDate.value, _historyEndDate.value)
    }

    /**
     * 刷新历史记录
     */
    fun refreshHistoryRecords() {
        repository.refreshHistoryRecords(_historyStartDate.value, _historyEndDate.value)
    }

    /**
     * 加载记录详情
     */
    fun loadRecordDetail(recordId: String) {
        repository.loadRecordDetail(recordId)
    }

    /**
     * 选择记录
     */
    fun selectRecord(record: SensorDataRecord?) {
        _selectedHistoryRecord.value = record
        if (record != null) {
            loadRecordDetail(record.recordId)
        } else {
            _selectedRecordData.value = emptyList()
        }
    }

    /**
     * 更新开始日期
     */
    fun updateStartDate(date: Date?) {
        _historyStartDate.value = date
    }

    /**
     * 更新结束日期
     */
    fun updateEndDate(date: Date?) {
        _historyEndDate.value = date
    }

    /**
     * 重置日期范围
     */
    fun resetDateRange() {
        _historyStartDate.value = HistoryRecordRepository.getDefaultStartDate()
        _historyEndDate.value = HistoryRecordRepository.getDefaultEndDate()
    }

    /**
     * 清空所有历史记录数据
     */
    fun clearHistoryData() {
        _historyRecords.value = emptyList()
        _selectedHistoryRecord.value = null
        _selectedRecordData.value = emptyList()
        _isHistoryLoading.value = false
        _isRecordDetailLoading.value = false
        _queryExecuted.value = false
        _hasMoreHistoryPages.value = true
        repository.clearCache()
    }

    // Callback implementations
    override fun onRecordsLoaded(records: List<SensorDataRecord>, page: Int, hasMorePages: Boolean) {
        viewModelScope.launch {
            if (page == 0) {
                _historyRecords.value = emptyList()
            }
            // 避免重复添加
            val existingIds = _historyRecords.value.map { it.recordId }.toSet()
            val newRecords = records.filter { it.recordId !in existingIds }
            _historyRecords.value = _historyRecords.value + newRecords
            _hasMoreHistoryPages.value = hasMorePages
            onRecordsLoaded?.invoke()
        }
    }

    override fun onRecordDetailLoaded(dataPoints: List<SensorDataPoint>) {
        viewModelScope.launch {
            _selectedRecordData.value = dataPoints
            onRecordDetailLoaded?.invoke()
        }
    }

    override fun onError(message: String) {
        onError?.invoke(message)
    }

    override fun onLoadingStateChanged(isLoading: Boolean) {
        _isHistoryLoading.value = isLoading
    }

    override fun onDetailLoadingStateChanged(isLoading: Boolean) {
        _isRecordDetailLoading.value = isLoading
    }
}
