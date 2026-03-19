package com.example.smartshoe.data.model

import androidx.compose.runtime.Immutable

/**
 * 传感器数据点，包含数值和时间戳
 * 使用@Immutable注解帮助Compose编译器优化重组
 */
@Immutable
data class SensorDataPoint(
    val timestamp: Long,
    val sensor1: Int,
    val sensor2: Int,
    val sensor3: Int
) {
    /**
     * 根据传感器索引获取对应的数值
     * @param sensorIndex 传感器索引 (0, 1, 2)
     * @return 对应的传感器数值
     */
    fun getValue(sensorIndex: Int): Int {
        return when (sensorIndex) {
            0 -> sensor1
            1 -> sensor2
            2 -> sensor3
            else -> throw IllegalArgumentException("Invalid sensor index: $sensorIndex, must be 0, 1, or 2")
        }
    }
}

/**
 * 传感器数据记录
 * 表示一次完整的数据采集记录
 */
@Immutable
data class SensorDataRecord(
    val recordId: String,
    val startTime: Long,
    val endTime: Long,
    val dataCount: Int,
    val interval: Int,
    val createdAt: String,
    val originalSize: Int = 0,
    val compressedSize: Int = 0,
    val compressionRatio: Float = 0f
)

/**
 * 传感器数据记录结果密封类
 */
sealed class SensorDataRecordsResult {
    data class Success(
        val records: List<SensorDataRecord>,
        val total: Long,
        val page: Int,
        val size: Int
    ) : SensorDataRecordsResult()

    data class Error(val message: String) : SensorDataRecordsResult()
}

/**
 * 传感器数据详情结果密封类
 */
sealed class SensorDataDetailResult {
    data class Success(
        val record: SensorDataRecord,
        val dataPoints: List<SensorDataPoint>
    ) : SensorDataDetailResult()

    data class Error(val message: String) : SensorDataDetailResult()
}

/**
 * 传感器数据操作结果密封类
 */
sealed class SensorDataResult {
    data class Success(val message: String) : SensorDataResult()
    data class Error(val message: String) : SensorDataResult()
}
