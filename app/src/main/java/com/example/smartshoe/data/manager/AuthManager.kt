package com.example.smartshoe.data.manager

import android.util.Log
import com.example.smartshoe.data.local.LocalDataSource
import com.example.smartshoe.data.model.UserState
import com.example.smartshoe.data.remote.AuthApiService
import com.example.smartshoe.data.remote.AuthResult
import com.example.smartshoe.data.remote.RequestManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 认证管理器
 * 封装登录、注册、退出登录、修改资料等功能
 * 提供服务器验证和本地状态管理
 *
 * 重构：
 * 1. 使用 Hilt 注入替代单例模式
 * 2. 使用 LocalDataSource 统一管理本地存储
 * 3. 使用挂起函数替代回调，简化异步处理
 * 4. AuthApiService 通过构造函数注入（符合依赖注入最佳实践）
 */
@Singleton
class AuthManager @Inject constructor(
    private val localDataSource: LocalDataSource,
    private val apiService: AuthApiService,
    private val requestManager: RequestManager
) {

    // 用户状态流，使用 StateFlow 替代回调
    private val _userStateFlow: MutableStateFlow<UserState>
    val userStateFlow: StateFlow<UserState>

    init {
        // 初始化时检查token过期状态
        val initialState = initUserState()
        _userStateFlow = MutableStateFlow(initialState)
        userStateFlow = _userStateFlow.asStateFlow()
    }

    /**
     * 初始化用户登录状态
     * 从本地存储读取并验证token
     * 如果token已过期，清除登录状态
     */
    private fun initUserState(): UserState {
        // 检查token是否过期
        if (localDataSource.isTokenExpired()) {
            // 直接清除本地数据，不调用logout()避免循环
            localDataSource.clearUserSession()
            return UserState(isLoggedIn = false)
        }

        val session = localDataSource.getUserSession()
        return if (session != null) {
            val (userState, token) = session
            userState
        } else {
            UserState(isLoggedIn = false)
        }
    }

    /**
     * 处理Token过期（401错误）
     * 自动登出并清除用户状态
     */
    fun handleTokenExpired() {
        logout()
    }

    /**
     * 用户登录
     * @param email 邮箱
     * @param password 密码
     * @return Result<UserState> 登录结果
     */
    suspend fun login(email: String, password: String): Result<UserState> {
        return when (val result = apiService.login(email, password)) {
            is AuthResult.Success -> {
                saveUserState(result.userState, result.token)
                _userStateFlow.value = result.userState
                Result.success(result.userState)
            }
            is AuthResult.Error -> {
                Result.failure(Exception(result.message))
            }
        }
    }

    /**
     * 用户注册
     * @param username 用户名
     * @param email 邮箱
     * @param password 密码
     * @return Result<UserState> 注册结果
     */
    suspend fun register(
        username: String,
        email: String,
        password: String
    ): Result<UserState> {
        return when (val result = apiService.register(username, email, password)) {
            is AuthResult.Success -> {
                saveUserState(result.userState, result.token)
                _userStateFlow.value = result.userState
                Result.success(result.userState)
            }
            is AuthResult.Error -> {
                Result.failure(Exception(result.message))
            }
        }
    }

    /**
     * 退出登录
     * 清除本地存储的登录状态
     */
    fun logout() {
        localDataSource.clearUserSession()
        val loggedOutState = UserState(isLoggedIn = false)
        _userStateFlow.value = loggedOutState
    }

    /**
     * 修改用户资料
     * @param userId 用户ID
     * @param currentPassword 当前密码（用于验证）
     * @param newUsername 新用户名
     * @param newEmail 新邮箱
     * @param newPassword 新密码（可选）
     * @return Result<UserState> 修改结果
     */
    suspend fun updateProfile(
        userId: String,
        currentPassword: String,
        newUsername: String,
        newEmail: String,
        newPassword: String = ""
    ): Result<UserState> {
        return when (val result = apiService.updateProfile(
            userId,
            currentPassword,
            newUsername,
            newEmail,
            newPassword
        )) {
            is AuthResult.Success -> {
                saveUserState(result.userState, result.token)
                _userStateFlow.value = result.userState
                Result.success(result.userState)
            }
            is AuthResult.Error -> {
                Result.failure(Exception(result.message))
            }
        }
    }

    /**
     * 验证Token是否有效
     * @param token 登录令牌
     * @return Boolean 是否有效
     */
    suspend fun verifyToken(token: String): Boolean {
        return when (val result = apiService.verifyToken(token)) {
            is AuthResult.Success -> {
                saveUserState(result.userState, token)
                _userStateFlow.value = result.userState
                true
            }
            is AuthResult.Error -> {
                logout()
                false
            }
        }
    }

    /**
     * 保存用户状态到本地
     * 登录成功后清除API缓存，确保使用新Token的请求不会被旧缓存影响
     */
    private fun saveUserState(userState: UserState, token: String) {
        localDataSource.saveUserSession(userState, token)
        // 清除API请求缓存，避免旧Token的缓存影响新Token的请求
        requestManager.clearCache()
        Log.d("AuthManager", "登录成功，已清除API缓存")
    }

    /**
     * 获取当前用户状态
     */
    fun getCurrentUserState(): UserState {
        return _userStateFlow.value
    }

    /**
     * 检查用户是否已登录
     */
    fun isLoggedIn(): Boolean {
        return _userStateFlow.value.isLoggedIn
    }

    /**
     * 获取当前用户的Token
     * @return Token字符串，未登录返回null
     */
    fun getToken(): String? {
        return localDataSource.getUserSession()?.second
    }

    /**
     * 清除所有用户数据（用于缓存清理）
     */
    fun clearAllUserData() {
        localDataSource.clear()
        _userStateFlow.value = UserState(isLoggedIn = false)
    }
}
