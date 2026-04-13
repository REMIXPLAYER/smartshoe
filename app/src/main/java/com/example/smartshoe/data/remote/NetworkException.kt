package com.example.smartshoe.data.remote

import com.example.smartshoe.domain.model.UserState

/**
 * 网络层异常基类
 * 统一网络错误类型，便于错误处理和用户提示
 */
sealed class NetworkException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause) {

    /**
     * 连接超时异常
     */
    class ConnectTimeoutException(
        message: String = "连接超时，请检查网络",
        cause: Throwable? = null
    ) : NetworkException(message, cause)

    /**
     * 读取超时异常
     */
    class ReadTimeoutException(
        message: String = "读取数据超时，请稍后重试",
        cause: Throwable? = null
    ) : NetworkException(message, cause)

    /**
     * 连接失败异常（无法连接到服务器）
     */
    class ConnectionFailedException(
        message: String = "无法连接到服务器，请检查网络",
        cause: Throwable? = null
    ) : NetworkException(message, cause)

    /**
     * 服务器错误异常（HTTP 5xx）
     */
    class ServerException(
        val code: Int,
        message: String = "服务器错误",
        cause: Throwable? = null
    ) : NetworkException("[$code] $message", cause)

    /**
     * 客户端错误异常（HTTP 4xx）
     */
    class ClientException(
        val code: Int,
        message: String = "请求错误",
        cause: Throwable? = null
    ) : NetworkException("[$code] $message", cause)

    /**
     * 未授权异常（HTTP 401）
     */
    class UnauthorizedException(
        message: String = "登录已过期，请重新登录",
        cause: Throwable? = null
    ) : NetworkException(message, cause)

    /**
     * 数据解析异常
     */
    class ParseException(
        message: String = "数据解析失败",
        cause: Throwable? = null
    ) : NetworkException(message, cause)

    /**
     * 网络IO异常
     */
    class NetworkIOException(
        message: String = "网络IO错误",
        cause: Throwable? = null
    ) : NetworkException(message, cause)

    /**
     * 未知网络异常
     */
    class UnknownException(
        message: String = "未知网络错误",
        cause: Throwable? = null
    ) : NetworkException(message, cause)

    companion object {
        /**
         * 根据异常类型创建对应的 NetworkException
         */
        fun from(throwable: Throwable): NetworkException {
            return when (throwable) {
                is NetworkException -> throwable
                is java.net.SocketTimeoutException -> {
                    ConnectTimeoutException(throwable.message ?: "连接超时", throwable)
                }
                is java.net.ConnectException -> {
                    ConnectionFailedException(throwable.message ?: "连接失败", throwable)
                }
                is java.net.SocketException -> {
                    ConnectionFailedException("网络连接中断: ${throwable.message}", throwable)
                }
                is java.io.IOException -> {
                    ConnectionFailedException(throwable.message ?: "IO错误", throwable)
                }
                else -> UnknownException(throwable.message ?: "未知错误", throwable)
            }
        }
    }
}

/**
 * 网络请求结果包装类
 * 替代原有的 AuthResult、SensorDataUploadResult 等
 */
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val exception: NetworkException) : ApiResult<Nothing>()

    /**
     * 获取成功数据，失败时返回 null
     */
    fun getOrNull(): T? = (this as? Success)?.data

    /**
     * 获取错误信息，成功时返回 null
     */
    fun exceptionOrNull(): NetworkException? = (this as? Error)?.exception

    /**
     * 是否成功
     */
    val isSuccess: Boolean get() = this is Success

    /**
     * 是否失败
     */
    val isError: Boolean get() = this is Error

    /**
     * 映射成功数据
     */
    inline fun <R> map(transform: (T) -> R): ApiResult<R> {
        return when (this) {
            is Success -> Success(transform(data))
            is Error -> this
        }
    }

    /**
     * 处理成功和失败两种情况
     */
    inline fun fold(
        onSuccess: (T) -> Unit,
        onError: (NetworkException) -> Unit
    ) {
        when (this) {
            is Success -> onSuccess(data)
            is Error -> onError(exception)
        }
    }
}

/**
 * 将旧的 AuthResult 转换为新的 ApiResult
 */
fun AuthResult.toApiResult(): ApiResult<Pair<UserState, String>> {
    return when (this) {
        is AuthResult.Success -> ApiResult.Success(userState to token)
        is AuthResult.Error -> ApiResult.Error(
            NetworkException.UnknownException(message, exception)
        )
    }
}
