package com.example.smartshoe.data.repository

import com.example.smartshoe.data.manager.AuthManager
import com.example.smartshoe.data.model.UserState
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 认证仓库实现
 * 使用 AuthManager 作为数据源
 *
 * 重构：AuthManager 已使用挂起函数，直接调用无需包装
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authManager: AuthManager
) : AuthRepository {

    override suspend fun login(email: String, password: String): Result<UserState> {
        return authManager.login(email, password)
    }

    override suspend fun register(
        username: String,
        email: String,
        password: String
    ): Result<UserState> {
        return authManager.register(username, email, password)
    }

    override fun logout() {
        authManager.logout()
    }

    override fun getCurrentUser(): UserState? {
        val currentState = authManager.getCurrentUserState()
        return if (currentState.isLoggedIn) {
            currentState
        } else {
            null
        }
    }

    override suspend fun updateProfile(
        userId: String,
        currentPassword: String,
        newUsername: String,
        newEmail: String,
        newPassword: String
    ): Result<UserState> {
        return authManager.updateProfile(
            userId,
            currentPassword,
            newUsername,
            newEmail,
            newPassword
        )
    }

    override fun observeUserState(): Flow<UserState> {
        return authManager.userStateFlow
    }

    override fun getToken(): String? {
        return authManager.getToken()
    }
}
