package com.example.smartshoe.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartshoe.data.model.UserState
import com.example.smartshoe.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 认证视图模型
 * 管理用户认证相关的UI状态和逻辑
 *
 * 重构：添加 Token 管理功能，消除 MainActivity 直接访问 LocalDataSource
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    // Token 状态流，供其他 ViewModel 监听
    private val _tokenState = MutableStateFlow<String?>(null)
    val tokenState: StateFlow<String?> = _tokenState.asStateFlow()

    // UI状态
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // 用户状态
    private val _userState = MutableStateFlow(authRepository.getCurrentUser() ?: UserState())
    val userState: StateFlow<UserState> = _userState.asStateFlow()

    init {
        // 监听用户状态变化
        viewModelScope.launch {
            authRepository.observeUserState().collect { user ->
                _userState.value = user
                // 同步更新 Token 状态
                _tokenState.value = authRepository.getToken()
            }
        }
    }

    /**
     * 获取当前 Token
     */
    fun getToken(): String? = authRepository.getToken()

    /**
     * 获取用户会话信息
     * @return Pair<userId, token> 或 null
     */
    fun getUserSession(): Pair<String, String>? {
        val userId = _userState.value.userId
        val token = authRepository.getToken()
        return if (userId.isNotEmpty() && !token.isNullOrEmpty()) {
            Pair(userId, token)
        } else null
    }

    /**
     * 用户登录
     */
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.login(email, password)
                .onSuccess { user ->
                    _userState.value = user
                    _uiState.value = AuthUiState.Success
                }
                .onFailure { error ->
                    _uiState.value = AuthUiState.Error(error.message ?: "登录失败")
                }
        }
    }

    /**
     * 用户注册
     */
    fun register(username: String, email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.register(username, email, password)
                .onSuccess { user ->
                    _userState.value = user
                    _uiState.value = AuthUiState.Success
                }
                .onFailure { error ->
                    _uiState.value = AuthUiState.Error(error.message ?: "注册失败")
                }
        }
    }

    /**
     * 用户登出
     */
    fun logout() {
        authRepository.logout()
        _userState.value = UserState()
        _uiState.value = AuthUiState.Idle
    }

    /**
     * 更新用户资料
     */
    fun updateProfile(
        userId: String,
        currentPassword: String,
        newUsername: String,
        newEmail: String,
        newPassword: String
    ) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.updateProfile(userId, currentPassword, newUsername, newEmail, newPassword)
                .onSuccess { user ->
                    _userState.value = user
                    _uiState.value = AuthUiState.Success
                }
                .onFailure { error ->
                    _uiState.value = AuthUiState.Error(error.message ?: "更新失败")
                }
        }
    }

    /**
     * 清除错误状态
     */
    fun clearError() {
        if (_uiState.value is AuthUiState.Error) {
            _uiState.value = AuthUiState.Idle
        }
    }
}

/**
 * 认证UI状态密封类
 */
sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    object Success : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}
