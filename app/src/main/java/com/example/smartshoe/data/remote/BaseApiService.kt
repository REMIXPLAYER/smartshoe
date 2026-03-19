package com.example.smartshoe.data.remote

import android.util.Log
import com.example.smartshoe.BuildConfig
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
 * 基础API服务类
 * 提供统一的网络请求封装，包括错误处理、超时设置、响应解析等
 *
 * 重构：使用 BuildConfig.BASE_URL 支持多环境配置
 */
abstract class BaseApiService {

    companion object {
        const val DEFAULT_CONNECT_TIMEOUT = 15000
        const val DEFAULT_READ_TIMEOUT = 15000
        const val LONG_READ_TIMEOUT = 120000

        // 使用 BuildConfig 中的 BASE_URL，支持多环境配置
        val BASE_URL: String = BuildConfig.BASE_URL

        private const val TAG = "BaseApiService"
    }

    protected suspend fun executeGet(
        path: String,
        token: String? = null,
        params: Map<String, Any> = emptyMap(),
        useLongTimeout: Boolean = false
    ): ApiResult<String> {
        // 构建带查询参数的URL
        val urlWithParams = if (params.isNotEmpty()) {
            val queryString = params.entries.joinToString("&") { (key, value) ->
                "${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(value.toString(), "UTF-8")}"
            }
            "$path?$queryString"
        } else {
            path
        }

        return executeRequest(
            path = urlWithParams,
            method = "GET",
            token = token,
            useLongTimeout = useLongTimeout,
            setupConnection = { }
        ) { connection ->
            // GET请求不需要写入数据
        }
    }

    protected suspend fun executePostForm(
        path: String,
        token: String? = null,
        formData: Map<String, String> = emptyMap(),
        useLongTimeout: Boolean = false
    ): ApiResult<String> = executeRequest(
        path = path,
        method = "POST",
        token = token,
        useLongTimeout = useLongTimeout,
        setupConnection = { it.setRequestProperty("Content-Type", "application/x-www-form-urlencoded") }
    ) { connection ->
        val postData = buildFormData(formData)
        OutputStreamWriter(connection.outputStream).use { writer ->
            writer.write(postData)
            writer.flush()
        }
    }

    protected suspend fun executePostJson(
        path: String,
        token: String? = null,
        jsonBody: JSONObject,
        useLongTimeout: Boolean = false
    ): ApiResult<String> = executeRequest(
        path = path,
        method = "POST",
        token = token,
        useLongTimeout = useLongTimeout,
        setupConnection = {
            it.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            it.setRequestProperty("Accept", "application/json")
        }
    ) { connection ->
        val body = jsonBody.toString().toByteArray(Charsets.UTF_8)
        connection.setFixedLengthStreamingMode(body.size)
        connection.outputStream.use { outputStream ->
            outputStream.write(body)
            outputStream.flush()
        }
    }

    protected suspend fun executeDelete(
        path: String,
        token: String
    ): ApiResult<String> = executeRequest(
        path = path,
        method = "DELETE",
        token = token,
        useLongTimeout = false,
        setupConnection = { }
    ) { }

    private suspend fun executeRequest(
        path: String,
        method: String,
        token: String?,
        useLongTimeout: Boolean,
        setupConnection: (HttpURLConnection) -> Unit,
        writeBody: suspend (HttpURLConnection) -> Unit
    ): ApiResult<String> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("$BASE_URL$path")
            connection = url.openConnection() as HttpURLConnection

            connection.apply {
                requestMethod = method
                connectTimeout = DEFAULT_CONNECT_TIMEOUT
                readTimeout = if (useLongTimeout) LONG_READ_TIMEOUT else DEFAULT_READ_TIMEOUT
                token?.let { setRequestProperty("Authorization", "Bearer $it") }
                if (method == "POST" || method == "PUT") doOutput = true
                setupConnection(this)
            }

            writeBody(connection)

            val responseCode = connection.responseCode
            val response = readResponse(connection)

            parseHttpResponse(response, responseCode)
        } catch (e: Exception) {
            Log.e(TAG, "$method request failed: ${e.message}", e)
            ApiResult.Error(NetworkException.from(e))
        } finally {
            connection?.disconnect()
        }
    }

    private fun readResponse(connection: HttpURLConnection): String {
        return try {
            BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
        } catch (e: Exception) {
            connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
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

    private fun buildFormData(formData: Map<String, String>): String {
        return formData.entries.joinToString("&") { (key, value) ->
            "${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(value, "UTF-8")}"
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
