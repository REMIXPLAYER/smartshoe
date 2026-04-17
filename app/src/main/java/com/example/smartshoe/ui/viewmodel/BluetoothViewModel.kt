package com.example.smartshoe.ui.viewmodel

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartshoe.data.manager.BluetoothConnectionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 蓝牙视图模型
 * 作为 UI 和 BluetoothConnectionManager 之间的桥梁
 * 
 * 重构：不再直接管理蓝牙状态，而是委托给 BluetoothConnectionManager
 * 只暴露 StateFlow 给 UI 层，保持向后兼容
 */
@HiltViewModel
class BluetoothViewModel @Inject constructor(
    val savedStateHandle: SavedStateHandle,
    private val bluetoothConnectionManager: BluetoothConnectionManager
) : ViewModel() {

    companion object {
        private const val KEY_ERROR_CALLBACK_SET = "error_callback_set"
    }

    // 扫描到的设备列表 - 从 Manager 转发
    val scannedDevices: StateFlow<List<BluetoothDevice>> = bluetoothConnectionManager.scannedDevices

    // 已连接设备 - 从 Manager 转发
    val connectedDevice: StateFlow<BluetoothDevice?> = bluetoothConnectionManager.connectedDevice

    // 是否正在扫描 - 从 Manager 转发
    val isScanning: StateFlow<Boolean> = bluetoothConnectionManager.isScanning

    init {
        // 使用SavedStateHandle避免配置变更时重复设置回调
        val isCallbackSet = savedStateHandle.get<Boolean>(KEY_ERROR_CALLBACK_SET) ?: false
        if (!isCallbackSet) {
            // 将 Manager 的错误转发到 ViewModel
            bluetoothConnectionManager.onError = { message ->
                // 可以通过 StateFlow 或其他方式暴露给 UI
            }
            savedStateHandle[KEY_ERROR_CALLBACK_SET] = true
        }
    }

    /**
     * 开始扫描蓝牙设备
     * 委托给 BluetoothConnectionManager
     */
    fun startScan() {
        bluetoothConnectionManager.scanDevices()
    }

    /**
     * 停止扫描
     * 由 Manager 自动管理，此方法保留向后兼容
     */
    @Deprecated(
        "扫描状态由 BluetoothConnectionManager 自动管理，无需手动调用",
        ReplaceWith(""),
        DeprecationLevel.WARNING
    )
    fun stopScan() {
        // Manager 会自动处理扫描状态
    }

    /**
     * 连接设备
     * 委托给 BluetoothConnectionManager
     */
    fun connectDevice(device: BluetoothDevice) {
        viewModelScope.launch {
            bluetoothConnectionManager.connectDevice(device)
        }
    }

    /**
     * 断开连接
     * 委托给 BluetoothConnectionManager
     */
    fun disconnectDevice() {
        bluetoothConnectionManager.disconnectDevice()
    }

    /**
     * 清空设备列表
     * 委托给 BluetoothConnectionManager
     */
    fun clearDevices() {
        bluetoothConnectionManager.clearDevices()
    }

    override fun onCleared() {
        super.onCleared()
        // 清理回调，防止内存泄漏
        bluetoothConnectionManager.onError = null
        bluetoothConnectionManager.onDataReceived = null
        
        // 断开蓝牙连接，释放资源
        disconnectDevice()
    }
}
