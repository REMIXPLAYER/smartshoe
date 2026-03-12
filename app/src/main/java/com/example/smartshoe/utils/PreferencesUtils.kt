package com.example.smartshoe.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * SharedPreferences工具类
 * 提供统一的SharedPreferences访问接口，避免重复代码
 */
object PreferencesUtils {

    private const val PREFS_NAME_USER = "user_preferences"
    private const val PREFS_NAME_APP_CACHE = "app_cache"
    private const val PREFS_NAME_LEGACY = "PREFS_NAME"
    private const val PREFS_NAME_PRESSURE_ALERTS = "pressure_alerts_enabled"

    // 键名常量
    const val KEY_USER_TOKEN = "user_token"
    const val KEY_USER_WEIGHT = "user_weight"
    const val KEY_FIRST_LAUNCH = "first_launch"
    const val KEY_PRESSURE_ALERTS_ENABLED = "pressure_alerts_enabled"

    /**
     * 获取用户偏好设置SharedPreferences
     */
    fun getUserPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME_USER, Context.MODE_PRIVATE)
    }

    /**
     * 获取应用缓存SharedPreferences
     */
    fun getAppCachePreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME_APP_CACHE, Context.MODE_PRIVATE)
    }

    /**
     * 获取遗留配置SharedPreferences
     */
    fun getLegacyPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME_LEGACY, Context.MODE_PRIVATE)
    }

    /**
     * 获取压力提醒设置SharedPreferences
     */
    fun getPressureAlertsPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME_PRESSURE_ALERTS, Context.MODE_PRIVATE)
    }

    /**
     * 保存用户Token
     */
    fun saveUserToken(context: Context, token: String) {
        getUserPreferences(context).edit().putString(KEY_USER_TOKEN, token).apply()
    }

    /**
     * 获取用户Token
     */
    fun getUserToken(context: Context): String? {
        return getUserPreferences(context).getString(KEY_USER_TOKEN, null)
    }

    /**
     * 清除用户Token
     */
    fun clearUserToken(context: Context) {
        getUserPreferences(context).edit().remove(KEY_USER_TOKEN).apply()
    }

    /**
     * 保存用户体重
     */
    fun saveUserWeight(context: Context, weight: Float) {
        getUserPreferences(context).edit().putFloat(KEY_USER_WEIGHT, weight).apply()
    }

    /**
     * 获取用户体重
     */
    fun getUserWeight(context: Context, defaultValue: Float = 0f): Float {
        return getUserPreferences(context).getFloat(KEY_USER_WEIGHT, defaultValue)
    }

    /**
     * 检查是否是首次启动
     */
    fun isFirstLaunch(context: Context): Boolean {
        return getUserPreferences(context).getBoolean(KEY_FIRST_LAUNCH, true)
    }

    /**
     * 标记已非首次启动
     */
    fun markNotFirstLaunch(context: Context) {
        getUserPreferences(context).edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
    }

    /**
     * 保存压力提醒设置
     */
    fun savePressureAlertsEnabled(context: Context, enabled: Boolean) {
        getUserPreferences(context).edit().putBoolean(KEY_PRESSURE_ALERTS_ENABLED, enabled).apply()
    }

    /**
     * 获取压力提醒设置
     */
    fun getPressureAlertsEnabled(context: Context, defaultValue: Boolean = true): Boolean {
        return getUserPreferences(context).getBoolean(KEY_PRESSURE_ALERTS_ENABLED, defaultValue)
    }

    /**
     * 清除所有用户偏好设置
     */
    fun clearAllUserPreferences(context: Context) {
        getUserPreferences(context).edit().clear().apply()
    }

    /**
     * 清除所有应用缓存
     */
    fun clearAllAppCache(context: Context) {
        getAppCachePreferences(context).edit().clear().apply()
    }

    /**
     * 清除所有遗留配置
     */
    fun clearAllLegacyPreferences(context: Context) {
        getLegacyPreferences(context).edit().clear().apply()
    }

    /**
     * 清除所有压力提醒设置
     */
    fun clearAllPressureAlertsPreferences(context: Context) {
        getPressureAlertsPreferences(context).edit().clear().apply()
    }

    /**
     * 清除所有SharedPreferences（用于一键清理缓存）
     */
    fun clearAllPreferences(context: Context) {
        clearAllUserPreferences(context)
        clearAllAppCache(context)
        clearAllLegacyPreferences(context)
        clearAllPressureAlertsPreferences(context)
    }
}
