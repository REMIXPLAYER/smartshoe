package com.example.smartshoe.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartshoe.data.repository.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 用户资料视图模型
 * 管理用户体重等个人设置
 *
 * 重构：使用 UserProfileRepository 替代直接访问 LocalDataSource
 * 符合 Clean Architecture 分层原则
 */
@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val userProfileRepository: UserProfileRepository
) : ViewModel() {

    // 用户体重
    private val _userWeight = MutableStateFlow(userProfileRepository.getUserWeight())
    val userWeight: StateFlow<Float> = _userWeight.asStateFlow()

    // 用户名
    private val _username = MutableStateFlow(userProfileRepository.getUsername() ?: "")
    val username: StateFlow<String> = _username.asStateFlow()

    // 错误信息
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        // 初始化时加载保存的体重
        viewModelScope.launch {
            _userWeight.value = userProfileRepository.getUserWeight()
        }
    }

    /**
     * 更新用户体重
     * 业务逻辑：验证体重值并保存
     */
    fun updateUserWeight(weight: Float) {
        viewModelScope.launch {
            val success = userProfileRepository.saveUserWeight(weight)
            if (success) {
                _userWeight.value = weight
                _errorMessage.value = null
            } else {
                _errorMessage.value = "保存体重失败"
            }
        }
    }

    /**
     * 获取用户体重
     */
    fun getUserWeight(): Float {
        return userProfileRepository.getUserWeight()
    }

    /**
     * 清除错误信息
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * 初始化用户体重数据
     * 业务逻辑：检查首次启动标记并加载保存的体重
     */
    fun initUserWeight() {
        viewModelScope.launch {
            // 检查是否是第一次启动
            val isFirstLaunch = userProfileRepository.isFirstLaunch()

            // 读取保存的体重
            val savedWeight = userProfileRepository.getUserWeight()
            if (savedWeight > 0f) {
                _userWeight.value = savedWeight
            }

            // 标记已经不是第一次启动
            if (isFirstLaunch) {
                userProfileRepository.setFirstLaunch(false)
            }
        }
    }
}
