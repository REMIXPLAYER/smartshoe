package com.example.smartshoe.data.repository

import com.example.smartshoe.data.model.UserState
import kotlinx.coroutines.flow.Flow

/**
 * 认证仓库接口
 * 定义认证相关的数据操作
 */
interface AuthRepository {
    /**
     * 用户登录
     * @param email 邮箱
     * @param password 密码
     * @return 登录结果
     */
    suspend fun login(email: String, password: String): Result<UserState>
    
    /**
     * 用户注册
     * @param username 用户名
     * @param email 邮箱
     * @param password 密码
     * @return 注册结果
     */
    suspend fun register(username: String, email: String, password: String): Result<UserState>
    
    /**
     * 用户登出
     */
    fun logout()
    
    /**
     * 获取当前用户
     * @return 当前用户状态，未登录返回 null
     */
    fun getCurrentUser(): UserState?
    
    /**
     * 更新用户资料
     * @param userId 用户ID
     * @param currentPassword 当前密码
     * @param newUsername 新用户名
     * @param newEmail 新邮箱
     * @param newPassword 新密码
     * @return 更新结果
     */
    suspend fun updateProfile(
        userId: String,
        currentPassword: String,
        newUsername: String,
        newEmail: String,
        newPassword: String
    ): Result<UserState>
    
    /**
     * 观察用户状态变化
     * @return 用户状态流
     */
    fun observeUserState(): Flow<UserState>

    /**
     * 获取当前用户的Token
     * @return Token字符串，未登录返回null
     */
    fun getToken(): String?
}
