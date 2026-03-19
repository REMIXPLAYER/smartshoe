package com.example.smartshoe.data.repository

import android.util.Log
import com.example.smartshoe.data.local.LocalDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 用户资料仓库
 * 负责用户个人资料数据的存储和访问
 *
 * 职责：
 * - 用户体重读写
 * - 用户名读写
 * - 首次启动标记管理
 *
 * 注意：不包含任何业务逻辑，只负责数据访问
 */
@Singleton
class UserProfileRepository @Inject constructor(
    private val localDataSource: LocalDataSource
) {

    companion object {
        private const val TAG = "UserProfileRepository"
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_USER_WEIGHT = "user_weight"
        private const val KEY_USERNAME = "username"
    }

    // 用户体重状态流 - 延迟初始化，避免在构造函数中调用方法
    private val _userWeightFlow = MutableStateFlow(0f)
    val userWeightFlow: StateFlow<Float> = _userWeightFlow.asStateFlow()

    init {
        // 初始化时从数据源读取体重
        _userWeightFlow.value = getUserWeight()
    }

    /**
     * 获取用户体重
     */
    fun getUserWeight(): Float {
        return try {
            localDataSource.getUserWeight()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user weight: ${e.message}", e)
            0f
        }
    }

    /**
     * 保存用户体重
     * @param weight 体重值
     * @return 是否保存成功
     */
    fun saveUserWeight(weight: Float): Boolean {
        return try {
            localDataSource.saveUserWeight(weight)
            _userWeightFlow.value = weight
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving user weight: ${e.message}", e)
            false
        }
    }

    /**
     * 获取用户名
     */
    fun getUsername(): String? {
        return try {
            localDataSource.getUsername()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting username: ${e.message}", e)
            null
        }
    }

    /**
     * 保存用户名
     * @param username 用户名
     * @return 是否保存成功
     */
    fun saveUsername(username: String): Boolean {
        return try {
            localDataSource.putString(KEY_USERNAME, username)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving username: ${e.message}", e)
            false
        }
    }

    /**
     * 检查是否是首次启动
     */
    fun isFirstLaunch(): Boolean {
        return localDataSource.getBoolean(KEY_FIRST_LAUNCH, true)
    }

    /**
     * 设置首次启动标记
     * @param isFirst 是否为首次启动
     */
    fun setFirstLaunch(isFirst: Boolean) {
        localDataSource.putBoolean(KEY_FIRST_LAUNCH, isFirst)
    }

    /**
     * 清除所有用户资料数据
     */
    fun clearUserProfile() {
        try {
            // 使用通用的 putFloat 和 putString 来清除数据（设置为默认值）
            localDataSource.saveUserWeight(0f)
            localDataSource.putString(KEY_USERNAME, "")
            _userWeightFlow.value = 0f
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing user profile: ${e.message}", e)
        }
    }
}
