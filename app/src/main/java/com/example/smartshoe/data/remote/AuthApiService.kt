package com.example.smartshoe.data.remote

import com.example.smartshoe.data.UserState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

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

    companion object {
        // 生产环境地址（云服务器）
         //private const val BASE_URL = "http://39.97.37.162:8080/api"

        // 测试环境地址（本地服务器）
         private const val BASE_URL = "http://10.0.2.2:8080/api"

        private const val LOGIN_URL = "$BASE_URL/auth/login"
        private const val REGISTER_URL = "$BASE_URL/auth/register"
        private const val UPDATE_PROFILE_URL = "$BASE_URL/auth/update-profile"
        private const val VERIFY_TOKEN_URL = "$BASE_URL/auth/verify-token"

        // 连接超时时间（毫秒）
        private const val CONNECT_TIMEOUT = 15000
        private const val READ_TIMEOUT = 15000

        fun create(): AuthApiService = AuthApiServiceImpl()

        /**
         * 设置服务器基础URL（用于切换环境）
         * @param baseUrl 服务器基础URL，例如：http://192.168.1.100:8080/api
         */
        fun setBaseUrl(baseUrl: String) {
            // 可以通过反射或其他方式动态修改，这里仅作示例
            // 实际项目中可以使用BuildConfig或配置文件
        }
    }

    private class AuthApiServiceImpl : AuthApiService {

        override suspend fun login(email: String, password: String): AuthResult = withContext(Dispatchers.IO) {
            try {
                val url = URL(LOGIN_URL)
                val connection = url.openConnection() as HttpURLConnection

                connection.apply {
                    requestMethod = "POST"
                    doOutput = true
                    setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                    connectTimeout = CONNECT_TIMEOUT
                    readTimeout = READ_TIMEOUT
                }

                // 发送POST数据
                val postData = StringBuilder().apply {
                    append("email=${URLEncoder.encode(email, "UTF-8")}")
                    append("&password=${URLEncoder.encode(password, "UTF-8")}")
                }.toString()

                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(postData)
                    writer.flush()
                }

                val responseCode = connection.responseCode
                val response = readResponse(connection)

                connection.disconnect()

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    parseSuccessResponse(response)
                } else {
                    parseErrorResponse(response)
                }
            } catch (e: Exception) {
                AuthResult.Error("网络错误: ${e.message}", e)
            }
        }

        override suspend fun register(username: String, email: String, password: String): AuthResult = withContext(Dispatchers.IO) {
            try {
                val url = URL(REGISTER_URL)
                val connection = url.openConnection() as HttpURLConnection

                connection.apply {
                    requestMethod = "POST"
                    doOutput = true
                    setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                    connectTimeout = CONNECT_TIMEOUT
                    readTimeout = READ_TIMEOUT
                }

                // 发送POST数据
                val postData = StringBuilder().apply {
                    append("username=${URLEncoder.encode(username, "UTF-8")}")
                    append("&email=${URLEncoder.encode(email, "UTF-8")}")
                    append("&password=${URLEncoder.encode(password, "UTF-8")}")
                }.toString()

                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(postData)
                    writer.flush()
                }

                val responseCode = connection.responseCode
                val response = readResponse(connection)

                connection.disconnect()

                if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                    parseSuccessResponse(response)
                } else {
                    parseErrorResponse(response)
                }
            } catch (e: Exception) {
                AuthResult.Error("网络错误: ${e.message}", e)
            }
        }

        override suspend fun updateProfile(
            userId: String,
            currentPassword: String,
            newUsername: String,
            newEmail: String,
            newPassword: String
        ): AuthResult = withContext(Dispatchers.IO) {
            try {
                val url = URL(UPDATE_PROFILE_URL)
                val connection = url.openConnection() as HttpURLConnection

                connection.apply {
                    requestMethod = "POST"
                    doOutput = true
                    setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                    connectTimeout = CONNECT_TIMEOUT
                    readTimeout = READ_TIMEOUT
                }

                // 发送POST数据
                val postData = StringBuilder().apply {
                    append("userId=${URLEncoder.encode(userId, "UTF-8")}")
                    append("&currentPassword=${URLEncoder.encode(currentPassword, "UTF-8")}")
                    append("&newUsername=${URLEncoder.encode(newUsername, "UTF-8")}")
                    append("&newEmail=${URLEncoder.encode(newEmail, "UTF-8")}")
                    if (newPassword.isNotBlank()) {
                        append("&newPassword=${URLEncoder.encode(newPassword, "UTF-8")}")
                    }
                }.toString()

                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(postData)
                    writer.flush()
                }

                val responseCode = connection.responseCode
                val response = readResponse(connection)

                connection.disconnect()

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    parseSuccessResponse(response)
                } else {
                    parseErrorResponse(response)
                }
            } catch (e: Exception) {
                AuthResult.Error("网络错误: ${e.message}", e)
            }
        }

        override suspend fun verifyToken(token: String): AuthResult = withContext(Dispatchers.IO) {
            try {
                val url = URL(VERIFY_TOKEN_URL)
                val connection = url.openConnection() as HttpURLConnection

                connection.apply {
                    requestMethod = "POST"
                    doOutput = true
                    setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                    connectTimeout = CONNECT_TIMEOUT
                    readTimeout = READ_TIMEOUT
                }

                // 发送POST数据
                val postData = "token=${URLEncoder.encode(token, "UTF-8")}"

                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(postData)
                    writer.flush()
                }

                val responseCode = connection.responseCode
                val response = readResponse(connection)

                connection.disconnect()

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    parseSuccessResponse(response)
                } else {
                    parseErrorResponse(response)
                }
            } catch (e: Exception) {
                AuthResult.Error("网络错误: ${e.message}", e)
            }
        }

        private fun readResponse(connection: HttpURLConnection): String {
            return try {
                BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                    reader.readText()
                }
            } catch (e: Exception) {
                // 如果inputStream读取失败，尝试读取errorStream
                BufferedReader(InputStreamReader(connection.errorStream)).use { reader ->
                    reader.readText()
                }
            }
        }

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

        private fun parseErrorResponse(response: String): AuthResult {
            return try {
                val json = JSONObject(response)
                val message = json.optString("message", "请求失败")
                AuthResult.Error(message)
            } catch (e: Exception) {
                AuthResult.Error("请求失败: $response")
            }
        }
    }
}

/**
 * 认证结果密封类
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