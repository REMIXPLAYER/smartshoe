package com.example.smartshoe.domain.repository

import com.example.smartshoe.domain.model.SensorDataPoint
import com.example.smartshoe.domain.model.SensorDataRecord
import kotlinx.coroutines.flow.StateFlow
import java.util.Date

/**
 * 历史记录仓库接口
 * 纯 Flow 驱动，消除回调模式
 *
 * 职责：
 * - 查询历史记录（分页）
 * - 加载记录详情
 * - 管理日期范围筛选
 * - 缓存管理
 *
 * 所有状态通过 StateFlow 暴露，调用方通过观察 StateFlow 获取结果
 */
interface HistoryRecordRepository {

    // ========== 查询结果状态流 ==========

    /** 历史记录列表 */
    val recordsFlow: StateFlow<List<SensorDataRecord>>

    /** 是否正在加载 */
    val isLoadingFlow: StateFlow<Boolean>

    /** 是否有更多数据 */
    val hasMoreFlow: StateFlow<Boolean>

    /** 错误信息 */
    val errorFlow: StateFlow<String?>

    /** 当前选中的记录详情数据 */
    val recordDetailFlow: StateFlow<List<SensorDataPoint>>

    /** 是否正在加载详情 */
    val isDetailLoadingFlow: StateFlow<Boolean>

    // ========== 操作命令（挂起函数） ==========

    /**
     * 查询历史记录
     * @param page 页码
     * @param append 是否追加到现有列表
     * @param startDate 开始日期（null表示不限制）
     * @param endDate 结束日期（null表示不限制）
     */
    suspend fun queryHistoryRecords(
        page: Int = 0,
        append: Boolean = false,
        startDate: Date? = null,
        endDate: Date? = null
    )

    /**
     * 加载更多历史记录
     */
    suspend fun loadMoreHistoryRecords(startDate: Date? = null, endDate: Date? = null)

    /**
     * 刷新历史记录
     */
    suspend fun refreshHistoryRecords(startDate: Date? = null, endDate: Date? = null)

    /**
     * 加载记录详情
     * @param recordId 记录ID
     */
    suspend fun loadRecordDetail(recordId: String)

    // ========== 状态管理 ==========

    /**
     * 选择记录（触发详情加载）
     */
    fun selectRecord(record: SensorDataRecord?)

    /**
     * 获取当前选中的记录
     */
    fun getSelectedRecord(): SensorDataRecord?

    /**
     * 清除选中状态
     */
    fun clearSelection()

    /**
     * 设置日期范围
     */
    fun setDateRange(startDate: Date?, endDate: Date?)

    /**
     * 获取默认开始日期
     */
    fun getDefaultStartDate(): Date

    /**
     * 获取默认结束日期
     */
    fun getDefaultEndDate(): Date

    /**
     * 清除缓存
     */
    fun clearCache()
}
