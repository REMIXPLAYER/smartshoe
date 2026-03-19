package com.example.smartshoe.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.smartshoe.data.model.UserState

/**
 * 本地数据源接口
 * 定义本地数据存储的标准操作
 */
interface LocalDataSource {
    // 用户认证相关
    fun saveUserSession(userState: UserState, token: String, expiresIn: Long = 7 * 24 * 60 * 60) // 默认7天
    fun getUserSession(): Pair<UserState, String>?
    fun clearUserSession()
    fun isUserLoggedIn(): Boolean
    fun isTokenExpired(): Boolean

    // 用户资料相关
    fun saveUserProfile(userId: String, username: String, email: String)
    fun getUserId(): String?
    fun getUsername(): String?
    fun getEmail(): String?

    // 用户体重数据
    fun saveUserWeight(weight: Float)
    fun getUserWeight(): Float

    // 通用存储
    fun putString(key: String, value: String)
    fun getString(key: String, defaultValue: String = ""): String
    fun putInt(key: String, value: Int)
    fun getInt(key: String, defaultValue: Int = 0): Int
    fun putBoolean(key: String, value: Boolean)
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean
    fun remove(key: String)
    fun clear()
}

/**
 * SharedPreferences 本地数据源实现
 */
class SharedPreferencesDataSource(context: Context) : LocalDataSource {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "smartshoe_preferences"

        // 用户认证相关键
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USERNAME = "username"
        private const val KEY_EMAIL = "email"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_TOKEN = "user_token"
        private const val KEY_TOKEN_EXPIRES_AT = "token_expires_at"

        // 用户资料相关键
        private const val KEY_USER_WEIGHT = "user_weight"
    }

    override fun saveUserSession(userState: UserState, token: String, expiresIn: Long) {
        val expiresAt = System.currentTimeMillis() + expiresIn * 1000
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, userState.isLoggedIn)
            putString(KEY_USERNAME, userState.username)
            putString(KEY_EMAIL, userState.email)
            putString(KEY_USER_ID, userState.userId)
            putString(KEY_TOKEN, token)
            putLong(KEY_TOKEN_EXPIRES_AT, expiresAt)
            apply()
        }
    }

    override fun getUserSession(): Pair<UserState, String>? {
        val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        if (!isLoggedIn) return null

        val userState = UserState(
            isLoggedIn = true,
            username = prefs.getString(KEY_USERNAME, "") ?: "",
            email = prefs.getString(KEY_EMAIL, "") ?: "",
            userId = prefs.getString(KEY_USER_ID, "") ?: ""
        )
        val token = prefs.getString(KEY_TOKEN, "") ?: ""
        return Pair(userState, token)
    }

    override fun clearUserSession() {
        prefs.edit().apply {
            remove(KEY_IS_LOGGED_IN)
            remove(KEY_USERNAME)
            remove(KEY_EMAIL)
            remove(KEY_USER_ID)
            remove(KEY_TOKEN)
            remove(KEY_TOKEN_EXPIRES_AT)
            apply()
        }
    }

    override fun isTokenExpired(): Boolean {
        val expiresAt = prefs.getLong(KEY_TOKEN_EXPIRES_AT, 0)
        return expiresAt <= System.currentTimeMillis()
    }

    override fun isUserLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    override fun saveUserProfile(userId: String, username: String, email: String) {
        prefs.edit().apply {
            putString(KEY_USER_ID, userId)
            putString(KEY_USERNAME, username)
            putString(KEY_EMAIL, email)
            apply()
        }
    }

    override fun getUserId(): String? {
        return prefs.getString(KEY_USER_ID, null)
    }

    override fun getUsername(): String? {
        return prefs.getString(KEY_USERNAME, null)
    }

    override fun getEmail(): String? {
        return prefs.getString(KEY_EMAIL, null)
    }

    override fun saveUserWeight(weight: Float) {
        prefs.edit().putFloat(KEY_USER_WEIGHT, weight).apply()
    }

    override fun getUserWeight(): Float {
        return prefs.getFloat(KEY_USER_WEIGHT, 0f)
    }

    override fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    override fun getString(key: String, defaultValue: String): String {
        return prefs.getString(key, defaultValue) ?: defaultValue
    }

    override fun putInt(key: String, value: Int) {
        prefs.edit().putInt(key, value).apply()
    }

    override fun getInt(key: String, defaultValue: Int): Int {
        return prefs.getInt(key, defaultValue)
    }

    override fun putBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return prefs.getBoolean(key, defaultValue)
    }

    override fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    override fun clear() {
        prefs.edit().clear().apply()
    }
}
