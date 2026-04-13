package com.example.smartshoe.data.remote

import android.util.Log
import com.example.smartshoe.BuildConfig
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.retryWhen
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import org.json.JSONObject
import java.util.concurrent.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SSE连接状态
 */
sealed class SseConnectionState {
    object Idle : SseConnectionState()
    object Connecting : SseConnectionState()
    object Connected : SseConnectionState()
    data class Error(val message: String) : SseConnectionState()
    object Closed : SseConnectionState()
}

/**
 * SSE客户端
 * 用于接收服务器发送的流式事件，支持自动重连
 */
@Singleton
class SseClient @Inject constructor(
    private val client: OkHttpClient
) {

    private val eventSourceFactory = EventSources.createFactory(client)

    companion object {
        private const val TAG = "SseClient"
        private const val MAX_RETRY_COUNT = 3
        private const val RETRY_DELAY_MS = 1000L
    }

    /**
     * 流式AI对话
     *
     * @param message 用户消息
     * @param token 用户Token
     * @param enableThinking 是否启用深度思考模式
     * @param onStateChange 连接状态变化回调
     * @return 流式响应
     */
    fun chatStream(
        message: String,
        token: String,
        enableThinking: Boolean = false,
        onStateChange: ((SseConnectionState) -> Unit)? = null
    ): Flow<SseEvent> = createSseFlow(
        requestBuilder = {
            val baseUrl = BuildConfig.BASE_URL.trimEnd('/')
            val url = "$baseUrl/ai/chat/stream"

            Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $token")
                .header("Accept", "text/event-stream")
                .header("Content-Type", "application/json")
                .post(
                    RequestBody.create(
                        "application/json".toMediaTypeOrNull(),
                        JSONObject().apply {
                            put("message", message)
                            put("enableThinking", enableThinking)
                        }.toString()
                    )
                )
                .build()
        },
        onStateChange = onStateChange
    )

    /**
     * 流式足部压力分析
     *
     * @param recordId 记录ID
     * @param token 用户Token
     * @param enableThinking 是否启用深度思考模式
     * @param onStateChange 连接状态变化回调
     * @return 流式响应
     */
    fun analysisStream(
        recordId: String,
        token: String,
        enableThinking: Boolean = false,
        onStateChange: ((SseConnectionState) -> Unit)? = null
    ): Flow<SseEvent> = createSseFlow(
        requestBuilder = {
            val baseUrl = BuildConfig.BASE_URL.trimEnd('/')
            val url = "$baseUrl/ai/analysis/stream?recordId=$recordId&enableThinking=$enableThinking"

            Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $token")
                .header("Accept", "text/event-stream")
                .post(RequestBody.create(null, ""))
                .build()
        },
        onStateChange = onStateChange
    )

    /**
     * 创建SSE流，支持自动重连
     */
    private fun createSseFlow(
        requestBuilder: () -> Request,
        onStateChange: ((SseConnectionState) -> Unit)? = null
    ): Flow<SseEvent> = callbackFlow {
        onStateChange?.invoke(SseConnectionState.Connecting)

        val request = requestBuilder()

        val listener = object : EventSourceListener() {
            override fun onOpen(eventSource: EventSource, response: Response) {
                Log.d(TAG, "SSE connection opened")
                onStateChange?.invoke(SseConnectionState.Connected)
            }

            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                when (type) {
                    null, "message" -> {
                        val sendResult = trySend(SseEvent.Data(data))
                        if (sendResult.isFailure) {
                            Log.w(TAG, "Failed to send data event, channel may be closed")
                        }
                    }
                    "complete" -> {
                        runCatching {
                            val json = JSONObject(data)
                            trySend(SseEvent.Complete(
                                model = json.optString("model", "unknown"),
                                duration = json.optLong("duration", 0)
                            ))
                        }.getOrElse {
                            Log.e(TAG, "Failed to parse complete event: ${it.message}")
                            trySend(SseEvent.Error("解析完成事件失败: ${it.message}"))
                        }
                        close()
                    }
                    "error" -> {
                        val errorMessage = runCatching {
                            // 尝试解析为JSON
                            val json = JSONObject(data)
                            json.optString("message", data)
                        }.getOrElse {
                            // 如果不是JSON，直接使用原始字符串
                            data
                        }
                        trySend(SseEvent.Error(errorMessage))
                        close()
                    }
                }
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                Log.e(TAG, "SSE failure: ${t?.message}", t)
                val errorMessage = t?.message ?: "Connection failed"
                trySend(SseEvent.Error(errorMessage))
                onStateChange?.invoke(SseConnectionState.Error(errorMessage))
                close(t)
            }

            override fun onClosed(eventSource: EventSource) {
                Log.d(TAG, "SSE connection closed")
                onStateChange?.invoke(SseConnectionState.Closed)
                close()
            }
        }

        val eventSource = eventSourceFactory.newEventSource(request, listener)

        awaitClose {
            Log.d(TAG, "Closing SSE connection")
            eventSource.cancel()
            onStateChange?.invoke(SseConnectionState.Closed)
        }
    }.retryWhen { cause, attempt ->
        if (attempt < MAX_RETRY_COUNT && cause !is CancellationException) {
            Log.w(TAG, "Retrying SSE connection (${attempt + 1}/$MAX_RETRY_COUNT) after ${RETRY_DELAY_MS}ms")
            onStateChange?.invoke(SseConnectionState.Connecting)
            delay(RETRY_DELAY_MS * (attempt + 1)) // 指数退避
            true
        } else {
            Log.e(TAG, "Max retries reached or cancellation, giving up")
            false
        }
    }
}

/**
 * SSE事件密封类
 */
sealed class SseEvent {
    /**
     * 数据事件 - 包含AI生成的文本片段
     */
    data class Data(val content: String) : SseEvent()

    /**
     * 完成事件 - AI生成完成
     */
    data class Complete(val model: String, val duration: Long) : SseEvent()

    /**
     * 错误事件
     */
    data class Error(val message: String) : SseEvent()
}
