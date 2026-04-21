package com.example.smartshoe.data.manager


import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import com.example.smartshoe.config.AppConfig
import com.example.smartshoe.di.ApplicationScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

    // 数据接收回调
    var onDataReceived: ((String) -> Unit)? = null

    // 错误回调
    var onError: ((String) -> Unit)? = null

    // 连接结果回调
    var onConnectionResult: ((Boolean, String?) -> Unit)? = null

    companion object {
        private const val TAG = "BluetoothConnManager"
        // 使用 AppConfig 中的 UUID
        private val SPP_UUID: UUID = UUID.fromString(AppConfig.Bluetooth.SPP_UUID)
    }

    /**
     * 扫描蓝牙设备
     * 返回已配对的设备列表
     */
    @SuppressLint("MissingPermission")
    fun scanDevices() {
        if (bluetoothAdapter?.isEnabled != true) {
            onError?.invoke("蓝牙未开启")
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
                onError?.invoke("扫描设备失败: ${e.message}")
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
            onError?.invoke("蓝牙未开启")
            onConnectionResult?.invoke(false, "蓝牙未开启")
            return@withContext false
        }

        // 防止并发连接 - 使用 StateFlow 进行状态管理
        synchronized(connectionLock) {
            if (_isConnecting.value) {
                Log.w(TAG, "Already connecting to a device, please wait")
                onError?.invoke("正在连接其他设备，请稍候")
                onConnectionResult?.invoke(false, "正在连接其他设备，请稍候")
                return@withContext false
            }
            
            // 检查是否已经连接到该设备
            if (_connectedDevice.value?.address == device.address) {
                Log.w(TAG, "Already connected to this device")
                onConnectionResult?.invoke(true, null)
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
                onError?.invoke("连接超时，请重试")
                onConnectionResult?.invoke(false, "连接超时，请重试")
                return@withContext false
            }

            // 检查连接是否成功
            if (!socket.isConnected) {
                closeSocketSafely(socket)
                Log.e(TAG, "Socket not connected after connect()")
                onError?.invoke("连接失败")
                onConnectionResult?.invoke(false, "连接失败")
                return@withContext false
            }

            // 注册到资源管理器
            resourceManager.registerSocket(device.address, socket)

            // 更新连接状态
            _connectedDevice.value = device

            // 开始数据监听
            startDataListening(device.address, socket)

            Log.d(TAG, "Connected to device: ${device.name} (${device.address})")
            onConnectionResult?.invoke(true, null)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to device: ${e.message}")
            onError?.invoke("连接失败: ${e.message}")
            onConnectionResult?.invoke(false, "连接失败: ${e.message}")
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
            _connectedDevice.value = null
            Log.d(TAG, "Disconnected from device: ${device.address}")
        }
    }

    /**
     * 开始监听蓝牙数据
     * 优化：正确处理协程取消，避免取消时触发错误回调
     */
    private fun startDataListening(deviceAddress: String, socket: BluetoothSocket) {
        val job = connectionScope.launch {
            try {
                val inputStream: InputStream = socket.inputStream
                resourceManager.registerInputStream(deviceAddress, inputStream)

                val buffer = ByteArray(AppConfig.Bluetooth.BUFFER_SIZE)
                var bytes: Int

                while (isActive && socket.isConnected) {
                    try {
                        bytes = inputStream.read(buffer)
                        if (bytes > 0) {
                            val data = String(buffer, 0, bytes)
                            onDataReceived?.invoke(data)
                        }
                    } catch (e: Exception) {
                        // 检查协程是否仍然活跃，避免取消时触发错误
                        if (!isActive) {
                            Log.d(TAG, "Data listening cancelled for device: $deviceAddress")
                            break
                        }
                        Log.e(TAG, "Error reading data: ${e.message}")
                        onError?.invoke("数据读取错误: ${e.message}")
                        break
                    }
                }
            } catch (e: Exception) {
                // 检查协程是否仍然活跃，避免取消时触发错误
                if (!isActive) {
                    Log.d(TAG, "Data listening cancelled for device: $deviceAddress")
                    return@launch
                }
                Log.e(TAG, "Error in data listening: ${e.message}")
                onError?.invoke("数据监听错误: ${e.message}")
            } finally {
                Log.d(TAG, "Data listening ended for device: $deviceAddress")
            }
        }

        resourceManager.registerJob(deviceAddress, job)
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
        // 清理回调，防止内存泄漏
        onDataReceived = null
        onError = null
        
        // 断开当前连接
        disconnectDevice()
        
        // 释放资源管理器中的所有资源
        resourceManager.releaseAllResources()
        
        // 注意：不取消 connectionScope，因为它是应用级别的
        // 由 Hilt 管理的 @ApplicationScope 协程作用域会在应用结束时自动清理
        
        Log.d(TAG, "All resources released (connection scope preserved)")
    }
}
