package com.example.smartshoe.data

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

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
)

/**
 * 用户状态数据类
 * 使用@Stable注解表示该类是稳定的，帮助Compose优化重组
 */
@Stable
data class UserState(
    val isLoggedIn: Boolean = false,
    val username: String = "",
    val email: String = "",
    val userId: String = ""
)