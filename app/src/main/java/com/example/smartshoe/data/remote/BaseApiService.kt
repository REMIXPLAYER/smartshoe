package com.example.smartshoe.data.remote

import com.example.smartshoe.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * 基础API服务类（OkHttp版本）
 * 支持注入共享的OkHttpClient实例，与SseClient共享连接池
 * 提供更现代、更高效的HTTP请求处理
 */
abstract class BaseApiService {

    companion object {
        // 连接超时：10秒
        const val DEFAULT_CONNECT_TIMEOUT = 10L
        // 普通读取超时：30秒
        const val DEFAULT_READ_TIMEOUT = 30L
        // AI请求读取超时：120秒
        const val LONG_READ_TIMEOUT = 120L

        // 使用BuildConfig中的BASE_URL
        val BASE_URL: String = BuildConfig.BASE_URL

        private const val TAG = "BaseApiService"

        /**
         * 创建默认的OkHttpClient配置
         * 用于创建共享client实例
         */
        fun createDefaultOkHttpClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(DEFAULT_CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(DEFAULT_READ_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()
        }
    }

    /**
     * 获取OkHttpClient实例
     * 子类可以覆盖此方法以注入共享的client
     * 默认创建新的client实例（向后兼容）
     */
    protected open fun getOkHttpClient(): OkHttpClient = createDefaultOkHttpClient()

    /**
     * 执行GET请求
     */
    protected suspend fun executeGet(
        path: String,
        token: String? = null,
        params: Map<String, Any> = emptyMap(),
        useLongTimeout: Boolean = false
    ): ApiResult<String> = withContext(Dispatchers.IO) {
        try {
            // 构建URL
            val urlBuilder = StringBuilder("$BASE_URL$path")
            if (params.isNotEmpty()) {
                urlBuilder.append("?")
                params.entries.joinTo(urlBuilder, "&") { (key, value) ->
                    "${key}=${value}"
                }
            }

            val request = Request.Builder()
                .url(urlBuilder.toString())
                .apply {
                    token?.let { header("Authorization", "Bearer $it") }
                }
                .build()

            executeRequest(request, useLongTimeout)
        } catch (e: Exception) {
            ApiResult.Error(NetworkException.from(e))
        }
    }

    /**
     * 执行POST表单请求
     */
    protected suspend fun executePostForm(
        path: String,
        token: String? = null,
        formData: Map<String, String> = emptyMap(),
        useLongTimeout: Boolean = false
    ): ApiResult<String> = withContext(Dispatchers.IO) {
        try {
            val formBody = FormBody.Builder().apply {
                formData.forEach { (key, value) ->
                    add(key, value)
                }
            }.build()

            val request = Request.Builder()
                .url("$BASE_URL$path")
                .post(formBody)
                .apply {
                    token?.let { header("Authorization", "Bearer $it") }
                }
                .build()

            executeRequest(request, useLongTimeout)
        } catch (e: Exception) {
            ApiResult.Error(NetworkException.from(e))
        }
    }

    /**
     * 执行POST JSON请求
     */
    protected suspend fun executePostJson(
        path: String,
        token: String? = null,
        jsonBody: JSONObject,
        useLongTimeout: Boolean = false
    ): ApiResult<String> = withContext(Dispatchers.IO) {
        try {
            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val requestBody = jsonBody.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url("$BASE_URL$path")
                .post(requestBody)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .apply {
                    token?.let { header("Authorization", "Bearer $it") }
                }
                .build()

            executeRequest(request, useLongTimeout)
        } catch (e: Exception) {
            ApiResult.Error(NetworkException.from(e))
        }
    }

    /**
     * 执行DELETE请求
     */
    protected suspend fun executeDelete(
        path: String,
        token: String
    ): ApiResult<String> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL$path")
                .delete()
                .header("Authorization", "Bearer $token")
                .build()

            executeRequest(request, false)
        } catch (e: Exception) {
            ApiResult.Error(NetworkException.from(e))
        }
    }

    /**
     * 执行请求
     * 使用getOkHttpClient()获取client实例，支持共享连接池
     */
    private suspend fun executeRequest(
        request: Request,
        useLongTimeout: Boolean
    ): ApiResult<String> = withContext(Dispatchers.IO) {
        val baseClient = getOkHttpClient()

        // 如果需要长超时，创建新的builder并修改超时设置
        val client = if (useLongTimeout) {
            baseClient.newBuilder()
                .readTimeout(LONG_READ_TIMEOUT, TimeUnit.SECONDS)
                .build()
        } else {
            baseClient
        }

        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                parseHttpResponse(body, response.code)
            }
        } catch (e: IOException) {
            ApiResult.Error(NetworkException.NetworkIOException(e.message ?: "网络请求失败"))
        }
    }

    private fun parseHttpResponse(response: String, code: Int): ApiResult<String> {
        return when (code) {
            in 200..299 -> ApiResult.Success(response)
            401 -> ApiResult.Error(NetworkException.UnauthorizedException())
            in 400..499 -> ApiResult.Error(
                NetworkException.ClientException(code, parseErrorMessage(response))
            )
            in 500..599 -> ApiResult.Error(
                NetworkException.ServerException(code, parseErrorMessage(response))
            )
            else -> ApiResult.Error(
                NetworkException.UnknownException("HTTP $code: ${parseErrorMessage(response)}")
            )
        }
    }

    private fun parseErrorMessage(response: String): String {
        return try {
            JSONObject(response).optString("message", JSONObject(response).optString("error", "请求失败"))
        } catch (e: Exception) {
            response.take(100)
        }
    }
}
