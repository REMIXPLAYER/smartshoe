package com.example.smartshoe.data.model

import androidx.compose.runtime.Stable

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

/**
 * 用户登录请求
 */
data class LoginRequest(
    val email: String,
    val password: String
)

/**
 * 用户注册请求
 */
data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String
)

/**
 * 认证响应
 */
data class AuthResponse(
    val success: Boolean,
    val message: String,
    val userId: String? = null,
    val username: String? = null,
    val email: String? = null
)

/**
 * 用户资料信息
 */
data class UserProfile(
    val userId: String,
    val username: String,
    val email: String,
    val weight: Float? = null,
    val height: Float? = null,
    val age: Int? = null,
    val gender: String? = null
)
