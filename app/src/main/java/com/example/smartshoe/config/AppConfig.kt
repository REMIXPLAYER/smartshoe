package com.example.smartshoe.config

/**
 * 应用配置类
 * 集中管理所有硬编码的魔法数字和配置参数
 *
 * 遵循 Clean Architecture 原则，将配置与业务逻辑分离
 */
object AppConfig {

    /**
     * 蓝牙配置
     */
    object Bluetooth {
        // HC-06蓝牙模块的标准UUID (SPP协议)
        const val SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB"

        // 蓝牙数据缓冲区大小 (字节)
        const val BUFFER_SIZE = 1024

        // 蓝牙资源清理间隔 (毫秒)
        const val CLEANUP_INTERVAL_MS = 30000L
    }

    /**
     * 传感器数据配置
     */
    object Sensor {
        // 历史数据缓冲区大小 (2分钟数据，每秒10个采样点)
        const val HISTORICAL_BUFFER_SIZE = 1200

        // 备份数据缓冲区大小 (30分钟数据)
        const val BACKUP_BUFFER_SIZE = 18000

        // 记录间隔 (毫秒)
        const val RECORDING_INTERVAL_MS = 100L

        // 压力异常阈值
        const val PRESSURE_ALERT_THRESHOLD = 4000

        // 传感器最大值 (12位ADC)
        const val SENSOR_MAX_VALUE = 4095

        // 模拟数据生成默认值
        const val DEFAULT_MOCK_DATA_COUNT = 10000
        const val DEFAULT_MOCK_TIME_RANGE_MINUTES = 15

        // 传感器3替代方案配置
        // 当 SENSOR3_USE_CALCULATED_VALUE = true 时，传感器3的值 = (传感器1 + 传感器2) / 2
        // 硬件修复后，将此值设为 false 即可恢复原始数据
        const val SENSOR3_USE_CALCULATED_VALUE = true
    }

    /**
     * 数据上传配置
     */
    object Upload {
        // 每批上传的数据点数量
        const val BATCH_SIZE = 100

        // 最大重试次数
        const val MAX_RETRY_COUNT = 3

        // 重试延迟基数 (毫秒)
        const val RETRY_DELAY_MS = 2000L

        // 备份时最大数据点数
        const val MAX_BACKUP_POINTS = 1000

        // 最小保留数据点数
        const val MIN_DATA_POINTS = 100

        // 批次间延迟 (毫秒)
        const val BATCH_DELAY_MS = 100L

        // 上传状态重置延迟 (毫秒)
        const val STATUS_RESET_DELAY_MS = 3000L

        // 默认采样间隔 (毫秒)
        const val DEFAULT_INTERVAL_MS = 100

        // 数据量警告阈值
        const val LARGE_DATA_WARNING_THRESHOLD = 10000
    }

    /**
     * 缓存配置
     */
    object Cache {
        // 历史记录页面大小
        const val HISTORY_PAGE_SIZE = 20

        // 最大缓存页数
        const val MAX_CACHED_PAGES = 5

        // 记录列表缓存时长 (毫秒)
        const val RECORDS_CACHE_DURATION_MS = 60000L // 1分钟

        // 记录详情缓存时长 (毫秒)
        const val DETAIL_CACHE_DURATION_MS = 120000L // 2分钟

        // 时间范围查询缓存时长 (毫秒)
        const val TIME_RANGE_CACHE_DURATION_MS = 300000L // 5分钟
    }

    /**
     * 性能监控配置
     */
    object Performance {
        // FPS采样间隔 (毫秒)
        const val FPS_SAMPLE_INTERVAL_MS = 1000L

        // 内存采样间隔 (毫秒)
        const val MEMORY_SAMPLE_INTERVAL_MS = 2000L

        // CPU采样间隔 (毫秒)
        const val CPU_SAMPLE_INTERVAL_MS = 3000L

        // 最大FPS值
        const val MAX_FPS = 120

        // FPS卡顿阈值
        const val FPS_STUTTER_THRESHOLD = 55

        // 内存单位转换
        const val BYTES_PER_KB = 1024L
        const val BYTES_PER_MB = 1024L * 1024L
        const val BYTES_PER_GB = 1024L * 1024L * 1024L

        // 综合评分满分
        const val MAX_SCORE = 100

        // 内存警告阈值 (MB)
        const val MEMORY_WARNING_THRESHOLD_MB = 100
        const val MEMORY_CRITICAL_THRESHOLD_MB = 50
    }

    /**
     * 调试测试配置
     */
    object Debug {
        // 性能测试时长 (毫秒)
        const val PERFORMANCE_TEST_DURATION_MS = 60000L

        // 预热时长 (毫秒)
        const val WARMUP_DURATION_MS = 5000L

        // 采样间隔 (毫秒)
        const val SAMPLE_INTERVAL_MS = 1000L

        // 压力测试数据量
        const val STRESS_TEST_DATA_COUNT = 10000
        const val STRESS_TEST_LARGE_DATA_COUNT = 20000
        const val STRESS_TEST_TIME_RANGE_MINUTES = 30

        // 性能测试阈值 (毫秒)
        const val ADD_PERFORMANCE_THRESHOLD_MS = 1000L
        const val READ_PERFORMANCE_THRESHOLD_MS = 500L

        // 延迟测试 (毫秒)
        const val DELAY_SHORT_MS = 100L
        const val DELAY_MEDIUM_MS = 500L

        // 回归测试数据量
        const val REGRESSION_TEST_SMALL_COUNT = 100
        const val REGRESSION_TEST_MEDIUM_COUNT = 1000
        const val REGRESSION_TEST_LARGE_COUNT = 5000
        const val REGRESSION_TEST_BUFFER_SIZE = 18000
    }

    /**
     * UI配置
     */
    object UI {
        // 压力提醒冷却时间 (毫秒)
        const val ALERT_COOLDOWN_MS = 5000L

        // 默认开始日期偏移 (昨天)
        const val DEFAULT_START_DATE_OFFSET_DAYS = 1

        // 时间单位转换
        const val MS_PER_SECOND = 1000L
        const val MS_PER_MINUTE = 60 * 1000L
        const val MS_PER_HOUR = 60 * 60 * 1000L
        const val MS_PER_DAY = 24 * 60 * 60 * 1000L

        // 百分比计算
        const val PERCENTAGE_BASE = 100.0
    }
}
