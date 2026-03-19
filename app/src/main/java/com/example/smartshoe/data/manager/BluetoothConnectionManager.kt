package com.example.smartshoe.data.manager

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import com.example.smartshoe.config.AppConfig
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
 */
@Singleton
class BluetoothConnectionManager @Inject constructor(
    private val bluetoothAdapter: BluetoothAdapter?,
    private val resourceManager: BluetoothResourceManager
) {

    private val connectionScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // 扫描到的设备列表
    private val _scannedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val scannedDevices: StateFlow<List<BluetoothDevice>> = _scannedDevices.asStateFlow()

    // 已连接设备
    private val _connectedDevice = MutableStateFlow<BluetoothDevice?>(null)
    val connectedDevice: StateFlow<BluetoothDevice?> = _connectedDevice.asStateFlow()

    // 是否正在扫描
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    // 数据接收回调
    var onDataReceived: ((String) -> Unit)? = null

    // 错误回调
    var onError: ((String) -> Unit)? = null

    companion object {
        private const val TAG = "BluetoothConnManager"
        // 使用 AppConfig 中的 UUID
        private val SPP_UUID: UUID = UUID.fromString(AppConfig.Bluetooth.SPP_UUID)
    }

    /**
     * 扫描蓝牙设备
     * 返回已配对的设备列表
     */
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

    /**
     * 连接蓝牙设备
     */
    suspend fun connectDevice(device: BluetoothDevice): Boolean = withContext(Dispatchers.IO) {
        try {
            // 断开当前连接
            disconnectDevice()

            // 创建 Socket
            val socket = device.createRfcommSocketToServiceRecord(SPP_UUID)

            // 连接
            socket.connect()

            // 注册到资源管理器
            resourceManager.registerSocket(device.address, socket)

            // 更新连接状态
            _connectedDevice.value = device

            // 开始数据监听
            startDataListening(device.address, socket)

            Log.d(TAG, "Connected to device: ${device.name} (${device.address})")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to device: ${e.message}")
            onError?.invoke("连接失败: ${e.message}")
            false
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
                        if (isActive) {
                            Log.e(TAG, "Error reading data: ${e.message}")
                            onError?.invoke("数据读取错误: ${e.message}")
                        }
                        break
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in data listening: ${e.message}")
                onError?.invoke("数据监听错误: ${e.message}")
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
     */
    fun releaseAllResources() {
        disconnectDevice()
        resourceManager.releaseAllResources()
        connectionScope.cancel()
    }
}
