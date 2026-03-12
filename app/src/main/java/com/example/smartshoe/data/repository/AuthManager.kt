package com.example.smartshoe.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.smartshoe.data.UserState
import com.example.smartshoe.data.remote.AuthApiService
import com.example.smartshoe.data.remote.AuthResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 认证管理器
 * 封装登录、注册、退出登录、修改资料等功能
 * 提供服务器验证和本地状态管理
 */
class AuthManager private constructor(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val apiService: AuthApiService = AuthApiService.create()

    // 统一的协程作用域，使用 SupervisorJob 确保子协程错误不会影响其他协程
    private val authScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // 当前用户状态回调
    var onUserStateChanged: ((UserState) -> Unit)? = null

    companion object {
        private const val PREFS_NAME = "user_preferences"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USERNAME = "username"
        private const val KEY_EMAIL = "email"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_TOKEN = "user_token"
        private const val KEY_PASSWORD = "user_password"

        @Volatile
        private var instance: AuthManager? = null

        fun getInstance(context: Context): AuthManager {
            return instance ?: synchronized(this) {
                instance ?: AuthManager(context.applicationContext).also { instance = it }
            }
        }
    }

    /**
     * 初始化用户登录状态
     * 从本地存储读取并验证token
     */
    fun initUserState(): UserState {
        val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        val token = prefs.getString(KEY_TOKEN, "") ?: ""

        return if (isLoggedIn && token.isNotEmpty()) {
            // 验证token有效性（异步）- 使用统一的协程作用域
            authScope.launch {
                verifyToken(token)
            }

            UserState(
                isLoggedIn = true,
                username = prefs.getString(KEY_USERNAME, "") ?: "",
                email = prefs.getString(KEY_EMAIL, "") ?: "",
                userId = prefs.getString(KEY_USER_ID, "") ?: ""
            )
        } else {
            UserState(isLoggedIn = false)
        }
    }

    /**
     * 用户登录
     * @param email 邮箱
     * @param password 密码
     * @param onResult 登录结果回调
     */
    fun login(
        email: String,
        password: String,
        onResult: (Boolean, String, UserState?) -> Unit
    ) {
        authScope.launch {
            when (val result = apiService.login(email, password)) {
                is AuthResult.Success -> {
                    // 保存登录状态到本地
                    saveUserState(result.userState, result.token)

                    withContext(Dispatchers.Main) {
                        onUserStateChanged?.invoke(result.userState)
                        onResult(true, "登录成功", result.userState)
                    }
                }
                is AuthResult.Error -> {
                    withContext(Dispatchers.Main) {
                        onResult(false, result.message, null)
                    }
                }
            }
        }
    }

    /**
     * 用户注册
     * @param username 用户名
     * @param email 邮箱
     * @param password 密码
     * @param onResult 注册结果回调
     */
    fun register(
        username: String,
        email: String,
        password: String,
        onResult: (Boolean, String, UserState?) -> Unit
    ) {
        authScope.launch {
            when (val result = apiService.register(username, email, password)) {
                is AuthResult.Success -> {
                    // 保存登录状态到本地
                    saveUserState(result.userState, result.token)

                    withContext(Dispatchers.Main) {
                        onUserStateChanged?.invoke(result.userState)
                        onResult(true, "注册成功", result.userState)
                    }
                }
                is AuthResult.Error -> {
                    withContext(Dispatchers.Main) {
                        onResult(false, result.message, null)
                    }
                }
            }
        }
    }

    /**
     * 退出登录
     * 清除本地存储的登录状态
     */
    fun logout() {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, false)
            remove(KEY_TOKEN)
            apply()
        }

        val loggedOutState = UserState(isLoggedIn = false)
        onUserStateChanged?.invoke(loggedOutState)
    }

    /**
     * 修改用户资料
     * @param userId 用户ID
     * @param currentPassword 当前密码（用于验证）
     * @param newUsername 新用户名
     * @param newEmail 新邮箱
     * @param newPassword 新密码（可选）
     * @param onResult 修改结果回调
     */
    fun updateProfile(
        userId: String,
        currentPassword: String,
        newUsername: String,
        newEmail: String,
        newPassword: String = "",
        onResult: (Boolean, String, UserState?) -> Unit
    ) {
        authScope.launch {
            when (val result = apiService.updateProfile(
                userId,
                currentPassword,
                newUsername,
                newEmail,
                newPassword
            )) {
                is AuthResult.Success -> {
                    // 更新本地存储
                    saveUserState(result.userState, result.token)

                    withContext(Dispatchers.Main) {
                        onUserStateChanged?.invoke(result.userState)
                        onResult(true, "资料更新成功", result.userState)
                    }
                }
                is AuthResult.Error -> {
                    withContext(Dispatchers.Main) {
                        onResult(false, result.message, null)
                    }
                }
            }
        }
    }

    /**
     * 验证Token是否有效
     */
    private suspend fun verifyToken(token: String) {
        when (val result = apiService.verifyToken(token)) {
            is AuthResult.Success -> {
                // Token有效，更新用户信息
                saveUserState(result.userState, token)
                withContext(Dispatchers.Main) {
                    onUserStateChanged?.invoke(result.userState)
                }
            }
            is AuthResult.Error -> {
                // Token无效，清除登录状态
                logout()
            }
        }
    }

    /**
     * 保存用户状态到本地
     */
    private fun saveUserState(userState: UserState, token: String) {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_USERNAME, userState.username)
            putString(KEY_EMAIL, userState.email)
            putString(KEY_USER_ID, userState.userId)
            putString(KEY_TOKEN, token)
            apply()
        }
    }

    /**
     * 获取当前用户状态
     */
    fun getCurrentUserState(): UserState {
        val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        return if (isLoggedIn) {
            UserState(
                isLoggedIn = true,
                username = prefs.getString(KEY_USERNAME, "") ?: "",
                email = prefs.getString(KEY_EMAIL, "") ?: "",
                userId = prefs.getString(KEY_USER_ID, "") ?: ""
            )
        } else {
            UserState(isLoggedIn = false)
        }
    }

    /**
     * 检查用户是否已登录
     */
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    /**
     * 清除所有用户数据（用于缓存清理）
     */
    fun clearAllUserData() {
        prefs.edit().clear().apply()
    }
}