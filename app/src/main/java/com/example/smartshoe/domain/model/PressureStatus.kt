package com.example.smartshoe.domain.model

/**
 * 压力状态枚举（领域层）
 *
 * 职责：定义传感器压力的各种状态
 * 这是纯领域层枚举，不依赖任何框架或数据层
 */
enum class PressureStatus(val description: String) {
    NONE("无压力"),
    NORMAL("正常"),
    HIGH("偏高"),
    CRITICAL("过高")
}
