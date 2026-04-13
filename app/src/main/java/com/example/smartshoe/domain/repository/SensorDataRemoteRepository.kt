package com.example.smartshoe.domain.repository

import com.example.smartshoe.domain.model.SensorDataPoint
import com.example.smartshoe.domain.model.SensorDataRecord

/**
 * 传感器数据远程仓库接口
 *
 * 职责：定义远程传感器数据操作
 * 这是领域层接口，不依赖任何数据层实现细节
 */
interface SensorDataRemoteRepository {

    /**
     * 检查是否已登录
     */
    fun isLoggedIn(): Boolean

    /**
     * 上传传感器数据（带重试机制）
     * @param dataPoints 传感器数据点列表
     * @param onResult 上传结果回调 (success, message, info)
     */
    fun uploadSensorData(
        dataPoints: List<SensorDataPoint>,
        onResult: (Boolean, String, UploadResultInfo?) -> Unit
    )

    /**
     * 获取用户的历史记录列表
     *
     * @param page 页码，从 0 开始
     * @param size 每页大小
     * @param useCache 是否使用缓存
     * @param onResult 结果回调 (success, message, records, total)
     */
    fun getUserRecords(
        page: Int = 0,
        size: Int = 20,
        useCache: Boolean = true,
        onResult: (Boolean, String, List<SensorDataRecord>?, Long) -> Unit
    )

    /**
     * 按时间范围查询历史记录（支持分页）
     */
    fun getRecordsByTimeRange(
        startTime: Long,
        endTime: Long,
        page: Int = 0,
        size: Int = 20,
        useCache: Boolean = true,
        onResult: (Boolean, String, List<SensorDataRecord>?, Long) -> Unit
    )

    /**
     * 获取记录详情
     */
    fun getRecordDetail(
        recordId: String,
        useCache: Boolean = true,
        onResult: (Boolean, String, SensorDataRecord?, List<SensorDataPoint>?) -> Unit
    )

    /**
     * 上传结果信息
     */
    data class UploadResultInfo(
        val recordId: String,
        val dataCount: Int,
        val originalSize: Int,
        val compressedSize: Int,
        val compressionRatio: Float
    )
}
