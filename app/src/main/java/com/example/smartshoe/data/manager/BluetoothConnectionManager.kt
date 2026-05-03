package com.example.smartshoe.data.manager


import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import com.example.smartshoe.config.AppConfig
import com.example.smartshoe.di.ApplicationScope
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.InputStream
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 蓝牙连接管理器
 * 负责蓝牙设备的扫描、连接、数据监听
 *
 * 遵循 Clean Architecture 原则，将蓝牙连接逻辑从 Activity 中分离
 * 使用 @ApplicationScope 确保协程作用域在应用生命周期内有效
 *
 * 数据流向（重构后）：
 * 硬件 → BluetoothSocket → inputStream.read() → 原始字符串
 * → dataChannel.trySend() → dispatchJob → _rawDataFlow.emit()
 * → SensorDataViewModel.collect() → SensorDataRepository.processReceivedData()
 */
@Singleton
class BluetoothConnectionManager @Inject constructor(
    private val bluetoothAdapter: BluetoothAdapter?,
    private val resourceManager: BluetoothResourceManager,
    @ApplicationScope private val connectionScope: CoroutineScope
) {

    /**
     * 检查蓝牙权限
     */
    private fun hasBluetoothPermission(): Boolean {
        return true // 权限检查在 Activity 层处理，这里假设已授权
    }

    // 扫描到的设备列表
    private val _scannedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val scannedDevices: StateFlow<List<BluetoothDevice>> = _scannedDevices.asStateFlow()

    // 已连接设备
    private val _connectedDevice = MutableStateFlow<BluetoothDevice?>(null)
    val connectedDevice: StateFlow<BluetoothDevice?> = _connectedDevice.asStateFlow()

    // 是否正在扫描
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    // 连接状态
    private val _isConnecting = MutableStateFlow(false)
    val isConnecting: StateFlow<Boolean> = _isConnecting.asStateFlow()

    // 当前正在连接的设备地址
    private val _connectingDeviceAddress = MutableStateFlow<String?>(null)
    val connectingDeviceAddress: StateFlow<String?> = _connectingDeviceAddress.asStateFlow()

    // 蓝牙错误状态流（替代 onError 回调）
    private val _errorFlow = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val errorFlow: SharedFlow<String> = _errorFlow.asSharedFlow()

    // 蓝牙连接结果流（替代 onConnectionResult 回调）
    private val _connectionResultFlow = MutableSharedFlow<Pair<Boolean, String?>>(extraBufferCapacity = 16)
    val connectionResultFlow: SharedFlow<Pair<Boolean, String?>> = _connectionResultFlow.asSharedFlow()

    // 原始蓝牙数据帧流（替代 onDataReceived 回调）
    private val _rawDataFlow = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val rawDataFlow: SharedFlow<String> = _rawDataFlow.asSharedFlow()

    // 蓝牙数据通道 - 用于协程间数据传递（每次连接时创建，支持多设备）
    // 注意：当前业务场景为单设备连接，但为 future-proof 设计为每次连接创建新 Channel
    private var dataChannel: Channel<String>? = null

    // 最后收到数据的时间戳（用于连接保活）
    // 使用 @Volatile 确保多线程可见性（readJob 写，keepaliveJob 读）
    @Volatile
    private var lastDataTime = 0L

    companion object {
        private const val TAG = "BluetoothConnManager"
        // 使用 AppConfig 中的 UUID
        private val SPP_UUID: UUID = UUID.fromString(AppConfig.Bluetooth.SPP_UUID)
        // 连接保活超时：10秒未收到数据认为连接已断开
        private const val CONNECTION_KEEPALIVE_TIMEOUT_MS = 10000L
        // 保活检测间隔
        private const val KEEPALIVE_CHECK_INTERVAL_MS = 5000L
    }

    /**
     * 扫描蓝牙设备
     * 返回已配对的设备列表
     */
    @SuppressLint("MissingPermission")
    fun scanDevices() {
        if (bluetoothAdapter?.isEnabled != true) {
            emitError("蓝牙未开启")
            return
        }

        connectionScope.launch {
            _isScanning.value = true
            _scannedDevices.value = emptyList()

            try {
                // 获取已配对设备
                val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter.bondedDevices
                pairedDevices?.let { devices ->
                    _scannedDevices.value = devices.toList()
                    Log.d(TAG, "Found ${devices.size} paired devices")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error scanning devices: ${e.message}")
                emitError("扫描设备失败: ${e.message}")
            } finally {
                _isScanning.value = false
            }
        }
    }

    // 连接操作锁，防止并发连接
    private val connectionLock = Any()

    /**
     * 连接蓝牙设备
     * 添加超时处理和并发控制
     */
    @SuppressLint("MissingPermission")
    suspend fun connectDevice(device: BluetoothDevice): Boolean = withContext(Dispatchers.IO) {
        // 检查蓝牙适配器状态
        if (bluetoothAdapter?.isEnabled != true) {
            emitError("蓝牙未开启")
            emitConnectionResult(false, "蓝牙未开启")
            return@withContext false
        }

        // 防止并发连接 - 使用 StateFlow 进行状态管理
        synchronized(connectionLock) {
            if (_isConnecting.value) {
                Log.w(TAG, "Already connecting to a device, please wait")
                emitError("正在连接其他设备，请稍候")
                emitConnectionResult(false, "正在连接其他设备，请稍候")
                return@withContext false
            }

            // 检查是否已经连接到该设备
            if (_connectedDevice.value?.address == device.address) {
                Log.w(TAG, "Already connected to this device")
                emitConnectionResult(true, null)
                return@withContext true
            }

            _isConnecting.value = true
            _connectingDeviceAddress.value = device.address
        }

        try {
            // 断开当前连接
            disconnectDevice()

            // 创建 Socket
            val socket = device.createRfcommSocketToServiceRecord(SPP_UUID)

            // 连接（带超时）
            val connectJob = launch {
                try {
                    socket.connect()
                } catch (e: Exception) {
                    Log.e(TAG, "Socket connect error: ${e.message}")
                    throw e
                }
            }

            // 等待连接完成或超时（10秒）
            val connectResult = withTimeoutOrNull(10000) {
                connectJob.join()
                true
            }

            if (connectResult == null) {
                // 连接超时
                connectJob.cancel()
                closeSocketSafely(socket)
                Log.e(TAG, "Connection timeout")
                emitError("连接超时，请重试")
                emitConnectionResult(false, "连接超时，请重试")
                return@withContext false
            }

            // 检查连接是否成功
            if (!socket.isConnected) {
                closeSocketSafely(socket)
                Log.e(TAG, "Socket not connected after connect()")
                emitError("连接失败")
                emitConnectionResult(false, "连接失败")
                return@withContext false
            }

            // 注册到资源管理器
            resourceManager.registerSocket(device.address, socket)

            // 更新连接状态
            _connectedDevice.value = device

            // 重置保活时间戳
            lastDataTime = System.currentTimeMillis()

            // 开始数据监听
            startDataListening(device.address, socket)

            Log.d(TAG, "Connected to device: ${device.name} (${device.address})")
            emitConnectionResult(true, null)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to device: ${e.message}")
            emitError("连接失败: ${e.message}")
            emitConnectionResult(false, "连接失败: ${e.message}")
            false
        } finally {
            synchronized(connectionLock) {
                _isConnecting.value = false
                _connectingDeviceAddress.value = null
            }
        }
    }

    /**
     * 安全关闭Socket
     */
    private fun closeSocketSafely(socket: BluetoothSocket?) {
        try {
            socket?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing socket: ${e.message}")
        }
    }

    /**
     * 断开蓝牙设备连接
     */
    fun disconnectDevice() {
        _connectedDevice.value?.let { device ->
            resourceManager.releaseDeviceResources(device.address)
            resourceManager.releaseDeviceResources(device.address + "_dispatch")
            resourceManager.releaseDeviceResources(device.address + "_keepalive")
            _connectedDevice.value = null
            Log.d(TAG, "Disconnected from device: ${device.address}")
        }
    }

    /**
     * 开始监听蓝牙数据
     * 优化：使用 CONFLATED Channel 避免背压阻塞，增加连接保活
     * 每次连接创建新的 Channel，支持多设备场景
     */
    private fun startDataListening(deviceAddress: String, socket: BluetoothSocket) {
        // 为当前连接创建新的 Channel
        val channel = Channel<String>(Channel.CONFLATED)
        dataChannel = channel

        // 启动数据读取协程
        val readJob = connectionScope.launch {
            try {
                val inputStream: InputStream = socket.inputStream
                resourceManager.registerInputStream(deviceAddress, inputStream)

                val buffer = ByteArray(AppConfig.Bluetooth.BUFFER_SIZE)
                var bytes: Int

                while (isActive && socket.isConnected) {
                    try {
                        bytes = inputStream.read(buffer)
                        if (bytes > 0) {
                            // 更新最后收到数据的时间戳
                            lastDataTime = System.currentTimeMillis()

                            val data = String(buffer, 0, bytes)
                            // 使用 trySend 避免 Channel 满时阻塞读取协程
                            val result = channel.trySend(data)
                            if (!result.isSuccess) {
                                Log.w(TAG, "Data channel full, dropping data")
                            }
                        }
                    } catch (e: Exception) {
                        if (!isActive) {
                            Log.d(TAG, "Data listening cancelled for device: $deviceAddress")
                            break
                        }
                        Log.e(TAG, "Error reading data: ${e.message}")
                        emitError("数据读取错误: ${e.message}")
                        break
                    }
                }
            } catch (e: Exception) {
                if (!isActive) {
                    Log.d(TAG, "Data listening cancelled for device: $deviceAddress")
                    return@launch
                }
                Log.e(TAG, "Error in data listening: ${e.message}")
                emitError("数据监听错误: ${e.message}")
            } finally {
                Log.d(TAG, "Data listening ended for device: $deviceAddress")
                channel.close() // 关闭 Channel，通知 dispatchJob 结束
            }
        }

        // 启动数据分发协程 - 通过 Channel 消费数据，转发到 SharedFlow
        val dispatchJob = connectionScope.launch {
            try {
                for (data in channel) {
                    if (!isActive) break
                    _rawDataFlow.emit(data)
                }
            } catch (e: Exception) {
                if (!isActive) {
                    Log.d(TAG, "Data dispatch cancelled for device: $deviceAddress")
                    return@launch
                }
                Log.e(TAG, "Error dispatching data: ${e.message}")
            }
        }

        // 启动连接保活协程
        val keepaliveJob = connectionScope.launch {
            try {
                while (isActive && socket.isConnected) {
                    delay(KEEPALIVE_CHECK_INTERVAL_MS)
                    val timeSinceLastData = System.currentTimeMillis() - lastDataTime
                    if (timeSinceLastData > CONNECTION_KEEPALIVE_TIMEOUT_MS) {
                        Log.w(TAG, "No data for ${timeSinceLastData}ms, connection may be dead")
                        emitError("连接无响应，请重新连接")
                        disconnectDevice()
                        break
                    }
                }
            } catch (e: Exception) {
                if (!isActive) {
                    Log.d(TAG, "Keepalive check cancelled for device: $deviceAddress")
                    return@launch
                }
                Log.e(TAG, "Error in keepalive check: ${e.message}")
            }
        }

        resourceManager.registerJob(deviceAddress, readJob)
        resourceManager.registerJob(deviceAddress + "_dispatch", dispatchJob)
        resourceManager.registerJob(deviceAddress + "_keepalive", keepaliveJob)
    }

    /**
     * 发送错误到错误流（线程安全）
     */
    private fun emitError(message: String) {
        val result = _errorFlow.tryEmit(message)
        if (!result) {
            Log.w(TAG, "Error flow buffer full, dropping error: $message")
        }
    }

    /**
     * 发送连接结果到结果流（线程安全）
     */
    private fun emitConnectionResult(success: Boolean, message: String?) {
        val result = _connectionResultFlow.tryEmit(Pair(success, message))
        if (!result) {
            Log.w(TAG, "Connection result flow buffer full")
        }
    }

    /**
     * 清空设备列表
     */
    fun clearDevices() {
        _scannedDevices.value = emptyList()
    }

    /**
     * 释放所有资源
     * 注意：不清除 connectionScope，因为它是应用级别的协程作用域
     * 只清理当前连接相关的资源和回调
     */
    fun releaseAllResources() {
        // 断开当前连接
        disconnectDevice()

        // 释放资源管理器中的所有资源
        resourceManager.releaseAllResources()

        // 注意：不取消 connectionScope，因为它是应用级别的
        // 由 Hilt 管理的 @ApplicationScope 协程作用域会在应用结束时自动清理

        Log.d(TAG, "All resources released (connection scope preserved)")
    }
}
