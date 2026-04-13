package com.example.smartshoe.domain.usecase.sensor

import com.example.smartshoe.domain.model.SensorDataRecord
import com.example.smartshoe.domain.repository.SensorDataRemoteRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 获取用户历史记录列表
 *
 * 职责：封装获取历史记录列表的业务逻辑
 *
 * 注意：使用领域层类型，符合 Clean Architecture
 */
@Singleton
class GetHistoryRecordsUseCase @Inject constructor(
    private val sensorDataRemoteRepository: SensorDataRemoteRepository
) {

    /**
     * 获取用户的历史记录列表
     *
     * @param page 页码，从 0 开始
     * @param size 每页大小
     * @param useCache 是否使用缓存
     * @param onResult 结果回调 (success, message, records, total)
     */
    operator fun invoke(
        page: Int = 0,
        size: Int = 20,
        useCache: Boolean = true,
        onResult: (Boolean, String, List<SensorDataRecord>?, Long) -> Unit
    ) {
        sensorDataRemoteRepository.getUserRecords(
            page = page,
            size = size,
            useCache = useCache,
            onResult = onResult
        )
    }
}
