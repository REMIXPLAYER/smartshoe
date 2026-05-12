package com.example.smartshoe.data.manager


import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import com.example.smartshoe.config.AppConfig
import com.example.smartshoe.di.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
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
 * 支持两种蓝牙连接方式：
 * 1. Classic Bluetooth (SPP/RFCOMM) - 用于传统蓝牙模块
 * 2. BLE GATT - 用于低功耗蓝牙模块（如JDY-31、CC2541等）
 *
 * 遵循 Clean Architecture 原则，将蓝牙连接逻辑从 Activity 中分离
 * 使用 @ApplicationScope 确保协程作用域在应用生命周期内有效
 *
 * 数据流向（Classic Bluetooth）：
 * 硬件 → BluetoothSocket → inputStream.read() → 原始字符串
 * → dataChannel.trySend() → dispatchJob → _rawDataFlow.emit()
 * → SensorDataViewModel.collect() → SensorDataRepository.processReceivedData()
 *
 * 数据流向（BLE GATT）：
 * 硬件 → BluetoothGatt.onCharacteristicChanged() → 原始字符串
 * → dataChannel.trySend() → dispatchJob → _rawDataFlow.emit()
 * → SensorDataViewModel.collect() → SensorDataRepository.processReceivedData()
 */
@Singleton
class BluetoothConnectionManager @Inject constructor(
    private val bluetoothAdapter: BluetoothAdapter?,
    private val resourceManager: BluetoothResourceManager,
    @ApplicationScope private val connectionScope: CoroutineScope,
    @ApplicationContext private val appContext: Context
) {

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

    // BLE GATT 实例
    private var bleGatt: BluetoothGatt? = null

    // BLE UART 服务和特征引用
    private var bleUartService: BluetoothGattService? = null
    private var bleTxChar: BluetoothGattCharacteristic? = null

    // 是否为 BLE 连接
    @Volatile
    private var isBleConnection = false

    // BLE 连接结果（用于协程等待）- 使用 CompletableDeferred 替代 Object.wait/notifyAll
    // 优势：与 Kotlin 协程 withTimeoutOrNull 原生兼容，可被正确取消
    // 每次连接尝试前创建新实例
    private var bleConnectResult: CompletableDeferred<Boolean>? = null
    private var bleServiceDeferred: CompletableDeferred<Boolean>? = null

    // 连接取消信号 - 用于中断正在进行的连接
    @Volatile
    private var isConnectionCancelled = false

    companion object {
        private const val TAG = "BluetoothConnManager"
        private val SPP_UUID: UUID = UUID.fromString(AppConfig.Bluetooth.SPP_UUID)
        private val BLE_UART_SERVICE_UUID: UUID = UUID.fromString(AppConfig.Bluetooth.BLE_UART_SERVICE_UUID)
        private val BLE_UART_CHAR_UUID: UUID = UUID.fromString(AppConfig.Bluetooth.BLE_UART_CHAR_UUID)
        private val BLE_CLIENT_CHAR_CONFIG: UUID = UUID.fromString(AppConfig.Bluetooth.BLE_CLIENT_CHARACTERISTIC_CONFIG)
        private const val CONNECTION_KEEPALIVE_TIMEOUT_MS = 10000L
        private const val KEEPALIVE_CHECK_INTERVAL_MS = 5000L
        // 重试间隔（毫秒）
        private const val RETRY_DELAY_MS = 500L
        // HC-06 标准 RFCOMM 通道
        private const val RFCOMM_CHANNEL = 1
        // BLE 连接超时（毫秒）
        private const val BLE_CONNECT_TIMEOUT_MS = 3000L
        // BLE 服务发现超时（毫秒）
        private const val BLE_SERVICE_DISCOVERY_TIMEOUT_MS = 3000L
    }

    // 蓝牙发现广播接收器
    private var discoveryReceiver: BroadcastReceiver? = null

    // 扫描批处理协程（每 300ms 批量更新一次 StateFlow，减少 UI 重组频率）
    private var scanningJob: Job? = null
    // 设备缓冲列表（接收线程写入，批处理协程消费）
    private val deviceBuffer = mutableListOf<BluetoothDevice>()

    /**
     * 扫描蓝牙设备（发现附近可用设备）
     * 使用 startDiscovery 扫描附近的蓝牙设备，
     * 同时也会包含已配对设备
     */
    @SuppressLint("MissingPermission")
    fun scanDevices() {
        if (bluetoothAdapter?.isEnabled != true) {
            emitError("蓝牙未开启")
            return
        }

        // 取消之前的扫描协程和接收器
        scanningJob?.cancel()
        scanningJob = null
        unregisterDiscoveryReceiver()

        _isScanning.value = true
        synchronized(deviceBuffer) { deviceBuffer.clear() }

        val currentDevices = _scannedDevices.value
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter.bondedDevices

        val newDeviceList = if (pairedDevices != null && pairedDevices.isNotEmpty()) {
            val mergedList = currentDevices.toMutableList()
            pairedDevices.forEach { pairedDevice ->
                if (mergedList.none { it.address == pairedDevice.address }) {
                    mergedList.add(pairedDevice)
                }
            }
            Log.d(TAG, "Merged paired devices: ${pairedDevices.size}, total: ${mergedList.size}")
            mergedList
        } else {
            currentDevices
        }

        if (newDeviceList != _scannedDevices.value) {
            _scannedDevices.value = newDeviceList
        }

        try {
            val receiver = createDiscoveryReceiver()
            discoveryReceiver = receiver
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            }
            appContext.registerReceiver(receiver, filter)

            if (!bluetoothAdapter.startDiscovery()) {
                Log.e(TAG, "startDiscovery returned false")
                emitError("启动扫描失败")
                _isScanning.value = false
                unregisterDiscoveryReceiver()
                return
            }
            Log.d(TAG, "Bluetooth discovery started")

            scanningJob = connectionScope.launch {
                try {
                    while (isActive) {
                        flushDeviceBuffer()
                        if (!_isScanning.value) break
                        delay(300)
                    }
                } finally {
                    flushDeviceBuffer()
                    unregisterDiscoveryReceiver()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting discovery: ${e.message}")
            emitError("启动扫描失败: ${e.message}")
            _isScanning.value = false
            unregisterDiscoveryReceiver()
        }
    }

    /**
     * 创建蓝牙发现广播接收器
     *
     * 优化：设备发现后不直接更新 StateFlow（会导致高频 UI 重组），
     * 而是写入 deviceBuffer，由 scanDevices() 启动的批处理协程
     * 每 300ms 批量刷新一次 _scannedDevices。
     */
    @SuppressLint("MissingPermission")
    private fun createDiscoveryReceiver(): BroadcastReceiver {
        return object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val device: BluetoothDevice? =
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                            } else {
                                @Suppress("DEPRECATION")
                                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                            }
                        device?.let { newDevice ->
                            synchronized(deviceBuffer) {
                                deviceBuffer.add(newDevice)
                            }
                            Log.d(TAG, "Buffered device: ${newDevice.name} (${newDevice.address})")
                        }
                    }
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                        Log.d(TAG, "Bluetooth discovery finished")
                        _isScanning.value = false
                    }
                }
            }
        }
    }

    /**
     * 停止扫描并注销接收器
     */
    @SuppressLint("MissingPermission")
    fun stopScan() {
        scanningJob?.cancel()
        scanningJob = null
        try {
            bluetoothAdapter?.cancelDiscovery()
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling discovery: ${e.message}")
        }
        flushDeviceBuffer()
        unregisterDiscoveryReceiver()
        _isScanning.value = false
    }

    /**
     * 将 deviceBuffer 中的设备批量刷新到 _scannedDevices
     * 线程安全：接收器（主线程）写入 buffer，此方法在协程或主线程中消费
     */
    private fun flushDeviceBuffer() {
        if (synchronized(deviceBuffer) { deviceBuffer.isEmpty() }) return

        val devices: List<BluetoothDevice>
        synchronized(deviceBuffer) {
            devices = deviceBuffer.toList()
            deviceBuffer.clear()
        }

        val currentList = _scannedDevices.value.toMutableList()
        devices.forEach { device ->
            if (currentList.none { it.address == device.address }) {
                currentList.add(device)
                Log.d(TAG, "Flushed device: ${device.name} (${device.address})")
            }
        }
        if (currentList.size != _scannedDevices.value.size) {
            _scannedDevices.value = currentList
        }
    }

    /**
     * 注销发现广播接收器
     */
    private fun unregisterDiscoveryReceiver() {
        try {
            discoveryReceiver?.let {
                appContext.unregisterReceiver(it)
                discoveryReceiver = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receiver: ${e.message}")
        }
    }

    // 连接操作锁，防止并发连接
    private val connectionLock = Any()

    // ─────────────────────────────────────────────────────────────────────────
    // BLE GATT 回调
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * BLE GATT 回调
     * 处理 BLE 连接状态变化、服务发现、特征读写和通知
     */
    private val bleGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d(TAG, "BLE connected, starting service discovery")
                    bleConnectResult?.complete(true)
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d(TAG, "BLE disconnected (status: $status)")
                    bleConnectResult?.complete(status == 0)
                    if (status != 0 && _isConnecting.value) {
                        emitError("BLE连接断开: status=$status")
                    }
                    cleanupBleConnection()
                }
                else -> {
                    Log.d(TAG, "BLE state changed: $newState")
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "BLE services discovered")
                setupBleUartService(gatt)
                bleServiceDeferred?.complete(true)
            } else {
                Log.e(TAG, "BLE service discovery failed: $status")
                bleServiceDeferred?.complete(false)
                emitError("BLE服务发现失败: $status")
                disconnectBleDevice()
            }
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val data = characteristic.getStringValue(0)
                if (!data.isNullOrEmpty()) {
                    lastDataTime = System.currentTimeMillis()
                    dataChannel?.trySend(data)
                }
            }
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            val data = characteristic.getStringValue(0)
            if (!data.isNullOrEmpty()) {
                lastDataTime = System.currentTimeMillis()
                dataChannel?.trySend(data)
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.w(TAG, "BLE write failed: $status")
                emitError("BLE数据发送失败")
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BLE 连接方法
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 连接 BLE 蓝牙设备
     *
     * 使用 BLE GATT 协议连接，适用于 JDY-31、CC2541 等 BLE UART 模块。
     * 连接流程：
     * 1. connectGatt() 建立 GATT 连接
     * 2. 等待 onConnectionStateChange() 回调
     * 3. discoverServices() 发现服务
     * 4. 找到 UART 服务 (FFE0) 和特征 (FFE1)
     * 5. 启用通知 (setCharacteristicNotification)
     * 6. 连接完成，开始数据收发
     */
    @SuppressLint("MissingPermission")
    private suspend fun connectBleDevice(device: BluetoothDevice): Boolean {
        if (bluetoothAdapter?.isEnabled != true) {
            emitError("蓝牙未开启")
            emitConnectionResult(false, "蓝牙未开启")
            return false
        }

        synchronized(connectionLock) {
            if (_isConnecting.value) {
                Log.w(TAG, "Already connecting to a device, please wait")
                emitError("正在连接其他设备，请稍候")
                emitConnectionResult(false, "正在连接其他设备，请稍候")
                return false
            }

            if (_connectedDevice.value?.address == device.address && isBleConnection) {
                emitConnectionResult(true, null)
                return true
            }

            _isConnecting.value = true
            _connectingDeviceAddress.value = device.address
        }

        return withContext(Dispatchers.IO) {
            try {
                disconnectDevice()
                delay(RETRY_DELAY_MS)

                // disconnectDevice() 会设置 isConnectionCancelled=true，
                // 重置标志为本次连接做好准备
                isConnectionCancelled = false

                // 创建新的 deferred 实例供本次连接使用
                bleConnectResult = CompletableDeferred()
                bleServiceDeferred = CompletableDeferred()

                Log.d(TAG, "Connecting BLE device: ${device.name} (${device.address})")
                val gatt = device.connectGatt(appContext, false, bleGattCallback, BluetoothDevice.TRANSPORT_LE)

                if (gatt == null) {
                    emitError("创建GATT连接失败")
                    emitConnectionResult(false, "创建GATT连接失败")
                    return@withContext false
                }

                bleGatt = gatt

                val connected = withTimeoutOrNull(BLE_CONNECT_TIMEOUT_MS) {
                    bleConnectResult?.await() ?: false
                }

                if (isConnectionCancelled) {
                    Log.d(TAG, "BLE connection cancelled by user")
                    cleanupBleConnection()
                    emitConnectionResult(false, "连接已取消")
                    return@withContext false
                }

                if (connected != true) {
                    Log.e(TAG, "BLE connect timeout or failed")
                    cleanupBleConnection()
                    emitError(if (connected == null) "BLE连接超时" else "BLE连接失败")
                    emitConnectionResult(false, if (connected == null) "连接超时" else "连接失败")
                    return@withContext false
                }

                // 检查是否已取消
                if (isConnectionCancelled) {
                    Log.d(TAG, "BLE connection cancelled after connect")
                    cleanupBleConnection()
                    emitConnectionResult(false, "连接已取消")
                    return@withContext false
                }

                val serviceFound = withTimeoutOrNull(BLE_SERVICE_DISCOVERY_TIMEOUT_MS) {
                    bleServiceDeferred?.await()
                }

                if (isConnectionCancelled) {
                    Log.d(TAG, "BLE connection cancelled during service discovery")
                    cleanupBleConnection()
                    emitConnectionResult(false, "连接已取消")
                    return@withContext false
                }

                if (serviceFound != true) {
                    Log.e(TAG, "BLE service discovery timeout")
                    cleanupBleConnection()
                    emitError("BLE服务发现超时")
                    emitConnectionResult(false, "服务发现超时")
                    return@withContext false
                }

                if (bleUartService == null || bleTxChar == null) {
                    Log.e(TAG, "BLE UART service/characteristic not found")
                    cleanupBleConnection()
                    emitError("未找到UART服务，该设备可能不是BLE串口模块")
                    emitConnectionResult(false, "未找到UART服务")
                    return@withContext false
                }

                isBleConnection = true
                _connectedDevice.value = device
                lastDataTime = System.currentTimeMillis()
                startBleDataListening(device.address)

                Log.d(TAG, "BLE device connected: ${device.name} (${device.address})")
                emitConnectionResult(true, null)
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error connecting BLE device: ${e.message}")
                cleanupBleConnection()
                emitError("BLE连接失败: ${e.message}")
                emitConnectionResult(false, "连接失败: ${e.message}")
                false
            } finally {
                synchronized(connectionLock) {
                    _isConnecting.value = false
                    _connectingDeviceAddress.value = null
                }
            }
        }
    }

    /**
     * 设置 BLE UART 服务
     * 查找 FFE0 服务和 FFE1 特征，并启用通知
     */
    @SuppressLint("MissingPermission")
    private fun setupBleUartService(gatt: BluetoothGatt) {
        val uartService = gatt.getService(BLE_UART_SERVICE_UUID)
        if (uartService == null) {
            Log.w(TAG, "BLE UART service not found")
            return
        }

        bleUartService = uartService
        Log.d(TAG, "Found BLE UART service: ${uartService.uuid}")

        val txChar = uartService.getCharacteristic(BLE_UART_CHAR_UUID)
        if (txChar == null) {
            Log.w(TAG, "BLE UART TX characteristic not found")
            return
        }

        bleTxChar = txChar
        Log.d(TAG, "Found BLE TX characteristic: ${txChar.uuid}")

        // 启用通知
        val success = gatt.setCharacteristicNotification(txChar, true)
        if (success) {
            // 写入描述符以启用通知
            val descriptor = txChar.getDescriptor(BLE_CLIENT_CHAR_CONFIG)
            if (descriptor != null) {
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(descriptor)
                Log.d(TAG, "BLE notification enabled")
            }
        } else {
            Log.w(TAG, "Failed to enable BLE notification")
        }
    }

    /**
     * 开始 BLE 数据监听
     * BLE 数据通过 onCharacteristicChanged 回调接收，
     * 桥接到 Channel → Flow 以复用现有数据分发架构
     */
    private fun startBleDataListening(deviceAddress: String) {
        val channel = Channel<String>(Channel.CONFLATED)
        dataChannel = channel

        // BLE 数据分发协程
        val dispatchJob = connectionScope.launch {
            try {
                for (data in channel) {
                    if (!isActive) break
                    _rawDataFlow.emit(data)
                }
            } catch (e: Exception) {
                if (!isActive) return@launch
                Log.e(TAG, "Error in BLE data dispatch: ${e.message}")
            }
        }

        // BLE 连接保活协程
        val keepaliveJob = connectionScope.launch {
            try {
                while (isActive && isBleConnection) {
                    delay(KEEPALIVE_CHECK_INTERVAL_MS)
                    val timeSinceLastData = System.currentTimeMillis() - lastDataTime
                    if (timeSinceLastData > CONNECTION_KEEPALIVE_TIMEOUT_MS) {
                        Log.w(TAG, "No BLE data for ${timeSinceLastData}ms, connection may be dead")
                        emitError("BLE连接无响应，请重新连接")
                        disconnectBleDevice()
                        break
                    }
                }
            } catch (e: Exception) {
                if (!isActive) return@launch
                Log.e(TAG, "Error in BLE keepalive: ${e.message}")
            }
        }

        resourceManager.registerJob(deviceAddress + "_ble_dispatch", dispatchJob)
        resourceManager.registerJob(deviceAddress + "_ble_keepalive", keepaliveJob)
    }

    /**
     * 断开 BLE 设备连接
     */
    @SuppressLint("MissingPermission")
    private fun disconnectBleDevice() {
        isBleConnection = false
        _connectedDevice.value?.let { device ->
            resourceManager.releaseDeviceResources(device.address + "_ble_dispatch")
            resourceManager.releaseDeviceResources(device.address + "_ble_keepalive")
        }
        cleanupBleConnection()
        _connectedDevice.value = null
        Log.d(TAG, "BLE device disconnected")
    }

    /**
     * 清理 BLE 连接资源
     */
    private fun cleanupBleConnection() {
        try {
            bleGatt?.let { gatt ->
                bleUartService = null
                bleTxChar = null
                gatt.close()
                Log.d(TAG, "BLE GATT closed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error closing BLE GATT: ${e.message}")
        }
        bleGatt = null
        isBleConnection = false
        bleConnectResult = null
        bleServiceDeferred = null
    }

    /**
     * 发送 BLE 数据
     */
    @SuppressLint("MissingPermission")
    fun sendBleData(data: ByteArray): Boolean {
        val characteristic = bleTxChar ?: return false
        val gatt = bleGatt ?: return false

        return try {
            characteristic.value = data
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            gatt.writeCharacteristic(characteristic)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending BLE data: ${e.message}")
            emitError("BLE数据发送错误: ${e.message}")
            false
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 统一连接入口（自动检测协议）
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 自动检测并连接蓝牙设备
     *
     * 自动识别设备支持的蓝牙协议：
     * 1. 优先尝试 BLE GATT 连接（适配 JDY-31、CC2541 等 BLE UART 模块）
     * 2. 如果 BLE 连接失败（非 BLE 设备或未找到 UART 服务），
     *    自动回退到 Classic Bluetooth SPP 连接
     *
     * 数据流（无论哪种协议）：
     * 硬件 → dataChannel → dispatchJob → _rawDataFlow.emit()
     * → SensorDataViewModel.collect() → SensorDataRepository.processReceivedData()
     *
     * 这是对外统一连接接口，上层组件无需关心设备协议类型
     */
    @SuppressLint("MissingPermission")
    suspend fun connectDevice(device: BluetoothDevice): Boolean {
        Log.d(TAG, "Auto-connecting to ${device.name} (${device.address})")

        if (bluetoothAdapter?.isEnabled != true) {
            emitError("蓝牙未开启")
            emitConnectionResult(false, "蓝牙未开启")
            return false
        }

        // 在连接设备之前，必须先取消正在进行的蓝牙扫描/发现
        // 根据Android文档：如果发现正在进行，GATT连接可能会失败
        if (_isScanning.value) {
            Log.d(TAG, "Cancelling active discovery before connecting")
            stopScan()
            delay(RETRY_DELAY_MS)
        }

        synchronized(connectionLock) {
            if (_isConnecting.value) {
                Log.w(TAG, "Already connecting to a device, please wait")
                emitError("正在连接其他设备，请稍候")
                emitConnectionResult(false, "正在连接其他设备，请稍候")
                return false
            }

            if (_connectedDevice.value?.address == device.address) {
                emitConnectionResult(true, null)
                return true
            }
        }

        try {
            disconnectDevice()
            delay(RETRY_DELAY_MS)

            // 重置取消标志，为即将开始的连接尝试做准备
            // disconnectDevice() 不会再重置此标志
            isConnectionCancelled = false

            // 策略 1：先尝试 BLE GATT
            val bleSuccess = connectBleDevice(device)
            if (bleSuccess) {
                Log.d(TAG, "Connected via BLE GATT for ${device.address}")
                return true
            }

            // 检查是否被取消（用户点击断开）
            if (isConnectionCancelled) {
                Log.d(TAG, "Connection cancelled after BLE attempt")
                emitConnectionResult(false, "连接已取消")
                return false
            }

            cleanupBleConnection()
            isBleConnection = false

            synchronized(connectionLock) {
                _isConnecting.value = true
                _connectingDeviceAddress.value = device.address
            }

            // 策略 2：回退到 Classic Bluetooth SPP
            Log.d(TAG, "BLE failed, falling back to Classic Bluetooth SPP for ${device.address}")
            val classicSuccess = connectClassicDevice(device)
            if (classicSuccess) {
                Log.d(TAG, "Connected via Classic Bluetooth for ${device.address}")
                return true
            }

            Log.e(TAG, "All connection attempts failed for ${device.address}")
            emitError("自动连接失败：BLE 和 Classic Bluetooth 均无法连接")
            emitConnectionResult(false, "自动连接失败")
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Error in auto-connect: ${e.message}")
            emitError("自动连接失败: ${e.message}")
            emitConnectionResult(false, "连接失败: ${e.message}")
            return false
        } finally {
            synchronized(connectionLock) {
                _isConnecting.value = false
                _connectingDeviceAddress.value = null
            }
            // 重置取消标志，为下次连接做准备
            isConnectionCancelled = false
        }
    }

    /**
     * 统一发送数据
     * 自动根据当前连接类型选择 BLE 或 Classic Bluetooth 发送方式
     */
    fun sendData(data: ByteArray): Boolean {
        if (isBleConnection) {
            return sendBleData(data)
        }
        _connectedDevice.value?.let { device ->
            val socket = resourceManager.getSocket(device.address)
            if (socket != null && socket.isConnected) {
                return try {
                    val outputStream = socket.outputStream
                    outputStream.write(data)
                    outputStream.flush()
                    true
                } catch (e: Exception) {
                    Log.e(TAG, "Error sending data via Classic Bluetooth: ${e.message}")
                    emitError("数据发送错误: ${e.message}")
                    false
                }
            }
        }
        return false
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Classic Bluetooth 连接方法
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 连接 Classic Bluetooth 设备（原始 SPP 逻辑）
     *
     * 连接策略（反射方式优先以兼容 HC-06 等 SDP 实现不完整的设备）：
     * 1. 反射 Insecure + 通道 1（HC-06 最高概率兼容）
     * 2. 反射 Secure + 通道 1（备用反射方式）
     * 3. 标准 Secure SPP UUID（标准设备兼容）
     * 4. 标准 Insecure SPP UUID（标准设备兼容）
     */
    @SuppressLint("MissingPermission")
    private suspend fun connectClassicDevice(device: BluetoothDevice): Boolean = withContext(Dispatchers.IO) {
        try {
            var lastError: String? = null
            var successSocket: BluetoothSocket? = null

            val attempts = listOf(
                Triple("reflection insecure ch$RFCOMM_CHANNEL", true, RFCOMM_CHANNEL),
                Triple("reflection secure ch$RFCOMM_CHANNEL", false, RFCOMM_CHANNEL),
                Triple("standard secure SPP UUID", false, null),
                Triple("standard insecure SPP UUID", true, null),
            )

            for ((label, insecure, channel) in attempts) {
                if (successSocket != null) break

                // 检查是否已取消
                if (isConnectionCancelled) {
                    Log.d(TAG, "Classic connection cancelled by user")
                    emitConnectionResult(false, "连接已取消")
                    return@withContext false
                }

                Log.d(TAG, "Trying $label")

                val result = withTimeoutOrNull(10000) {
                    attemptConnect(device, insecure, useReflectionChannel = channel)
                }

                when {
                    result == null -> {
                        lastError = "连接超时"
                        Log.w(TAG, "$label timed out")
                    }
                    result.isSuccess -> {
                        successSocket = result.getOrNull()
                        Log.d(TAG, "Connected with $label")
                    }
                    else -> {
                        lastError = result.exceptionOrNull()?.message
                        Log.d(TAG, "$label failed: $lastError")
                    }
                }

                if (successSocket == null) {
                    delay(RETRY_DELAY_MS)
                }
            }

            if (successSocket == null) {
                val errorMsg = if (lastError != null) "连接失败: $lastError" else "连接失败"
                Log.w(TAG, "All Classic attempts failed for ${device.address}")
                emitError("$errorMsg。如已配对，请进入系统蓝牙设置中取消配对（取消保存）后重试")
                emitConnectionResult(false, errorMsg)
                return@withContext false
            }

            resourceManager.registerSocket(device.address, successSocket)
            _connectedDevice.value = device
            lastDataTime = System.currentTimeMillis()
            startDataListening(device.address, successSocket)
            Log.d(TAG, "Connected to device: ${device.name} (${device.address})")
            emitConnectionResult(true, null)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to device: ${e.message}")
            emitError("连接失败: ${e.message}")
            emitConnectionResult(false, "连接失败: ${e.message}")
            false
        }
    }

    /**
     * 使用反射方式创建 RFCOMM Socket（指定通道，绕过 SDP 协商）
     */
    @SuppressLint("MissingPermission")
    private fun createRfcommSocketViaReflection(device: BluetoothDevice, channel: Int, insecure: Boolean = false): BluetoothSocket? {
        return try {
            val methodName = if (insecure) "createInsecureRfcommSocket" else "createRfcommSocket"
            val method = device.javaClass.getMethod(methodName, Int::class.java)
            val socket = method.invoke(device, channel) as BluetoothSocket
            Log.d(TAG, "Created ${if (insecure) "insecure " else ""}socket via reflection (channel: $channel)")
            socket
        } catch (e: Exception) {
            Log.e(TAG, "Reflection ${if (insecure) "insecure " else ""}socket creation failed: ${e.message}")
            null
        }
    }

    /**
     * 尝试建立蓝牙连接（单次尝试）
     */
    @SuppressLint("MissingPermission")
    private suspend fun attemptConnect(
        device: BluetoothDevice,
        insecure: Boolean,
        useReflectionChannel: Int? = null
    ): Result<BluetoothSocket> {
        return withContext(Dispatchers.IO) {
            val socket = if (useReflectionChannel != null) {
                createRfcommSocketViaReflection(device, useReflectionChannel, insecure)
            } else {
                try {
                    if (insecure) {
                        device.createInsecureRfcommSocketToServiceRecord(SPP_UUID)
                    } else {
                        device.createRfcommSocketToServiceRecord(SPP_UUID)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Standard ${if (insecure) "insecure " else ""}socket creation failed: ${e.message}")
                    null
                }
            }

            if (socket == null) {
                return@withContext Result.failure(Exception("创建Socket失败"))
            }

            try {
                socket.connect()
                Log.d(TAG, "Socket connected successfully (method: ${
                    if (useReflectionChannel != null) "reflection $RFCOMM_CHANNEL ${if (insecure) "insecure" else "secure"}"
                    else "standard ${if (insecure) "insecure" else "secure"} SPP UUID"
                })")
                Result.success(socket)
            } catch (e: Exception) {
                Log.e(TAG, "Socket connect error: ${e.message}")
                closeSocketSafely(socket)
                Result.failure(e)
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
     * 支持 Classic Bluetooth 和 BLE GATT 两种方式
     *
     * 同时会取消正在进行的连接尝试（通过 isConnectionCancelled 标志）
     * 注意：isConnectionCancelled 不会在此方法内重置，
     * 由调用方 connectDevice() 的 finally 块负责重置
     */
    fun disconnectDevice() {
        // 设置取消标志，通知正在进行的连接流程中断
        isConnectionCancelled = true

        // 完成 deferred，让等待中的连接协程立即退出
        // 使用 CompletableDeferred 后，await() 是协程挂起点，
        // complete() 会立即恢复等待的协程，无需 Java 锁中断
        bleConnectResult?.complete(false)
        bleServiceDeferred?.complete(false)

        if (isBleConnection) {
            disconnectBleDevice()
        } else {
            _connectedDevice.value?.let { device ->
                resourceManager.releaseDeviceResources(device.address)
                resourceManager.releaseDeviceResources(device.address + "_dispatch")
                resourceManager.releaseDeviceResources(device.address + "_keepalive")
                _connectedDevice.value = null
                Log.d(TAG, "Disconnected from device: ${device.address}")
            }
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
                        // Socket 已关闭或超时，自动清理连接
                        if (e.message?.contains("read failed", ignoreCase = true) == true ||
                            e.message?.contains("socket closed", ignoreCase = true) == true ||
                            e.message?.contains("timeout", ignoreCase = true) == true
                        ) {
                            Log.d(TAG, "Socket connection lost, cleaning up resources")
                            disconnectDevice()
                        }
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
        stopScan()
        _scannedDevices.value = emptyList()
    }

    /**
     * 释放所有资源
     * 注意：不清除 connectionScope，因为它是应用级别的协程作用域
     * 只清理当前连接相关的资源和回调
     */
    fun releaseAllResources() {
        // 停止扫描
        stopScan()

        // 断开当前连接（同时处理 Classic 和 BLE）
        disconnectDevice()

        // 额外清理 BLE 资源
        cleanupBleConnection()

        // 释放资源管理器中的所有资源
        resourceManager.releaseAllResources()

        // 注意：不取消 connectionScope，因为它是应用级别的
        // 由 Hilt 管理的 @ApplicationScope 协程作用域会在应用结束时自动清理

        Log.d(TAG, "All resources released (connection scope preserved)")
    }
}
