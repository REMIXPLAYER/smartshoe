package com.example.smartshoe.domain.repository

import com.example.smartshoe.domain.model.SensorDataPoint
import com.example.smartshoe.domain.model.SensorDataRecord
import java.util.Date

/**
 * 历史记录仓库接口
 *
 * 职责：定义历史记录相关的数据操作
 * 这是领域层接口，不依赖任何数据层实现细节
 */
interface HistoryRecordRepository {

    /**
     * 设置回调接口
     * @param callback 回调接口实例
     */
    fun setCallback(callback: HistoryRecordCallback)

    /**
     * 获取历史记录列表
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @param startDate 开始日期（可选）
     * @param endDate 结束日期（可选）
     * @return 历史记录列表
     */
    fun getHistoryRecords(
        page: Int = 0,
        size: Int = 20,
        startDate: Date? = null,
        endDate: Date? = null
    ): List<SensorDataRecord>

    /**
     * 获取记录详情
     * @param recordId 记录ID
     * @return 记录数据点列表
     */
    fun getRecordDetail(recordId: String): List<SensorDataPoint>

    /**
     * 选择记录
     * @param record 记录对象
     */
    fun selectRecord(record: SensorDataRecord)

    /**
     * 清除选择
     */
    fun clearSelection()

    /**
     * 加载更多记录
     * @return 是否有更多记录
     */
    fun loadMoreRecords(): Boolean

    /**
     * 检查是否还有更多记录
     */
    fun hasMoreRecords(): Boolean

    /**
     * 刷新记录列表
     */
    fun refreshRecords()

    /**
     * 设置日期范围筛选
     * @param startDate 开始日期
     * @param endDate 结束日期
     */
    fun setDateRange(startDate: Date?, endDate: Date?)

    /**
     * 清除日期筛选
     */
    fun clearDateFilter()

    /**
     * 查询历史记录
     * @param page 页码
     * @param append 是否追加
     * @param startDate 开始日期
     * @param endDate 结束日期
     */
    fun queryHistoryRecords(page: Int = 0, append: Boolean = false, startDate: Date? = null, endDate: Date? = null)

    /**
     * 加载更多历史记录
     * @param startDate 开始日期
     * @param endDate 结束日期
     */
    fun loadMoreHistoryRecords(startDate: Date? = null, endDate: Date? = null)

    /**
     * 刷新历史记录
     * @param startDate 开始日期
     * @param endDate 结束日期
     */
    fun refreshHistoryRecords(startDate: Date? = null, endDate: Date? = null)

    /**
     * 加载记录详情
     * @param recordId 记录ID
     */
    fun loadRecordDetail(recordId: String)

    /**
     * 清除缓存
     */
    fun clearCache()

    /**
     * 获取默认开始日期
     */
    fun getDefaultStartDate(): Date

    /**
     * 获取默认结束日期
     */
    fun getDefaultEndDate(): Date

    /**
     * 历史记录回调接口
     */
    interface HistoryRecordCallback {
        /**
         * 记录列表加载完成
         * @param records 记录列表
         * @param hasMore 是否有更多记录
         */
        fun onRecordsLoaded(records: List<SensorDataRecord>, hasMore: Boolean)

        /**
         * 记录详情加载完成
         * @param recordId 记录ID
         * @param dataPoints 数据点列表
         */
        fun onRecordDetailLoaded(recordId: String, dataPoints: List<SensorDataPoint>)

        /**
         * 发生错误
         * @param message 错误信息
         */
        fun onError(message: String)
    }
}
