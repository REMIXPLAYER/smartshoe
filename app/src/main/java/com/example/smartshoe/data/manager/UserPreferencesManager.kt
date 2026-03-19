package com.example.smartshoe.data.manager

import com.example.smartshoe.data.local.LocalDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 用户偏好设置管理器
 * 统一管理用户偏好设置，消除直接访问 LocalDataSource 的问题
 *
 * 职责：
 * - 压力提醒开关
 * - 首次启动标记
 * - 其他用户偏好设置
 */
@Singleton
class UserPreferencesManager @Inject constructor(
    private val localDataSource: LocalDataSource
) {

    companion object {
        private const val KEY_PRESSURE_ALERTS_ENABLED = "pressure_alerts_enabled"
        private const val KEY_FIRST_LAUNCH = "first_launch"
    }

    // 压力提醒状态流
    private val _pressureAlertsEnabled = MutableStateFlow(getPressureAlertsEnabled())
    val pressureAlertsEnabled: StateFlow<Boolean> = _pressureAlertsEnabled.asStateFlow()

    /**
     * 获取压力提醒启用状态
     */
    fun getPressureAlertsEnabled(): Boolean {
        return localDataSource.getBoolean(KEY_PRESSURE_ALERTS_ENABLED, true) // 默认开启
    }

    /**
     * 设置压力提醒启用状态
     */
    fun setPressureAlertsEnabled(enabled: Boolean) {
        localDataSource.putBoolean(KEY_PRESSURE_ALERTS_ENABLED, enabled)
        _pressureAlertsEnabled.value = enabled
    }

    /**
     * 检查是否是首次启动
     */
    fun isFirstLaunch(): Boolean {
        return localDataSource.getBoolean(KEY_FIRST_LAUNCH, true)
    }

    /**
     * 设置首次启动标记
     */
    fun setFirstLaunch(isFirst: Boolean) {
        localDataSource.putBoolean(KEY_FIRST_LAUNCH, isFirst)
    }

    /**
     * 清除所有偏好设置
     */
    fun clearPreferences() {
        localDataSource.remove(KEY_PRESSURE_ALERTS_ENABLED)
        localDataSource.remove(KEY_FIRST_LAUNCH)
        _pressureAlertsEnabled.value = true
    }
}
