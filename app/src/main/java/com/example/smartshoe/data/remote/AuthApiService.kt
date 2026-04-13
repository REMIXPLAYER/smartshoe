package com.example.smartshoe.data.remote

import com.example.smartshoe.domain.model.UserState
import okhttp3.OkHttpClient
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 认证API服务接口
 * 提供登录、注册、修改资料等服务器验证功能
 */
interface AuthApiService {

    /**
     * 用户登录
     * @param email 邮箱
     * @param password 密码
     * @return 登录结果，包含UserState和错误信息
     */
    suspend fun login(email: String, password: String): AuthResult

    /**
     * 用户注册
     * @param username 用户名
     * @param email 邮箱
     * @param password 密码
     * @return 注册结果，包含UserState和错误信息
     */
    suspend fun register(username: String, email: String, password: String): AuthResult

    /**
     * 修改用户资料
     * @param userId 用户ID
     * @param currentPassword 当前密码（用于验证）
     * @param newUsername 新用户名
     * @param newEmail 新邮箱
     * @param newPassword 新密码（可选，为空则不修改）
     * @return 修改结果
     */
    suspend fun updateProfile(
        userId: String,
        currentPassword: String,
        newUsername: String,
        newEmail: String,
        newPassword: String = ""
    ): AuthResult

    /**
     * 验证Token是否有效
     * @param token 登录令牌
     * @return 验证结果
     */
    suspend fun verifyToken(token: String): AuthResult
}

/**
 * 认证API服务实现
 * 使用共享的OkHttpClient实例
 */
@Singleton
class AuthApiServiceImpl @Inject constructor(
    private val client: OkHttpClient
) : AuthApiService, BaseApiService() {

    /**
     * 获取共享的OkHttpClient
     */
    override fun getOkHttpClient(): OkHttpClient = client

    companion object {
        private const val LOGIN_PATH = "/auth/login"
        private const val REGISTER_PATH = "/auth/register"
        private const val UPDATE_PROFILE_PATH = "/auth/update-profile"
        private const val VERIFY_TOKEN_PATH = "/auth/verify-token"
    }

    override suspend fun login(email: String, password: String): AuthResult {
        val result = executePostForm(
            path = LOGIN_PATH,
            formData = mapOf(
                "email" to email,
                "password" to password
            )
        )
        return parseAuthResult(result)
    }

    override suspend fun register(
        username: String,
        email: String,
        password: String
    ): AuthResult {
        val result = executePostForm(
            path = REGISTER_PATH,
            formData = mapOf(
                "username" to username,
                "email" to email,
                "password" to password
            )
        )
        return parseAuthResult(result)
    }

    override suspend fun updateProfile(
        userId: String,
        currentPassword: String,
        newUsername: String,
        newEmail: String,
        newPassword: String
    ): AuthResult {
        val formData = mutableMapOf(
            "userId" to userId,
            "currentPassword" to currentPassword,
            "newUsername" to newUsername,
            "newEmail" to newEmail
        )
        if (newPassword.isNotBlank()) {
            formData["newPassword"] = newPassword
        }

        val result = executePostForm(
            path = UPDATE_PROFILE_PATH,
            formData = formData
        )
        return parseAuthResult(result)
    }

    override suspend fun verifyToken(token: String): AuthResult {
        val result = executePostForm(
            path = VERIFY_TOKEN_PATH,
            formData = mapOf("token" to token)
        )
        return parseAuthResult(result)
    }

    /**
     * 解析认证结果
     */
    private fun parseAuthResult(result: ApiResult<String>): AuthResult {
        return when (result) {
            is ApiResult.Success -> parseSuccessResponse(result.data)
            is ApiResult.Error -> AuthResult.Error(
                result.exception.message ?: "网络错误",
                result.exception as? Exception
            )
        }
    }

    /**
     * 解析成功响应
     */
    private fun parseSuccessResponse(response: String): AuthResult {
        return try {
            val json = JSONObject(response)
            val success = json.optBoolean("success", false)

            if (success) {
                val data = json.optJSONObject("data")
                if (data != null) {
                    val userState = UserState(
                        isLoggedIn = true,
                        username = data.optString("username", ""),
                        email = data.optString("email", ""),
                        userId = data.optString("userId", "")
                    )
                    val token = data.optString("token", "")
                    AuthResult.Success(userState, token)
                } else {
                    AuthResult.Error("服务器返回数据格式错误")
                }
            } else {
                val message = json.optString("message", "未知错误")
                AuthResult.Error(message)
            }
        } catch (e: Exception) {
            AuthResult.Error("解析响应失败: ${e.message}", e)
        }
    }
}

/**
 * 认证结果密封类
 * 保持原有接口不变，确保兼容性
 */
sealed class AuthResult {
    data class Success(
        val userState: UserState,
        val token: String = ""
    ) : AuthResult()

    data class Error(
        val message: String,
        val exception: Exception? = null
    ) : AuthResult()
}
