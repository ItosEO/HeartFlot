package com.itos.heartflot.viewmodel

import android.app.Application
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.itos.heartflot.bluetooth.BluetoothHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DeviceInfo(
    val device: BluetoothDevice,
    val name: String,
    val address: String,
    val rssi: Int
)

data class HeartRateState(
    val currentHeartRate: Int = 0,
    val isConnected: Boolean = false,
    val isScanning: Boolean = false,
    val connectedDevice: DeviceInfo? = null,
    val nearbyDevices: List<DeviceInfo> = emptyList(),
    val errorMessage: String? = null
)

class HeartRateViewModel(application: Application) : AndroidViewModel(application) {
    
    private val bluetoothHelper = BluetoothHelper(application)
    
    private val _state = MutableStateFlow(HeartRateState())
    val state: StateFlow<HeartRateState> = _state.asStateFlow()
    
    private val discoveredDevices = mutableMapOf<String, DeviceInfo>()
    private var noDataTimeoutJob: Job? = null
    
    companion object {
        private const val NO_DATA_TIMEOUT_MS = 5_000L
    }
    
    init {
        setupBluetoothCallbacks()
    }
    
    private fun setupBluetoothCallbacks() {
        bluetoothHelper.onDeviceFound = { device, rssi ->
            viewModelScope.launch {
                val deviceInfo = DeviceInfo(
                    device = device,
                    name = try {
                        device.name ?: "未知设备"
                    } catch (e: SecurityException) {
                        "未知设备"
                    },
                    address = device.address,
                    rssi = rssi
                )
                
                discoveredDevices[device.address] = deviceInfo
                _state.value = _state.value.copy(
                    nearbyDevices = discoveredDevices.values.sortedByDescending { it.rssi }
                )
            }
        }
        
        bluetoothHelper.onHeartRateReceived = { heartRate ->
            viewModelScope.launch {
                _state.value = _state.value.copy(
                    currentHeartRate = heartRate,
                    errorMessage = null
                )
                resetNoDataTimeout()
            }
        }
        
        bluetoothHelper.onConnectionStateChanged = { connected ->
            viewModelScope.launch {
                if (connected) {
                    resetNoDataTimeout()
                } else {
                    cancelNoDataTimeout()
                }
                _state.value = _state.value.copy(
                    isConnected = connected,
                    errorMessage = if (!connected) "连接已断开" else null
                )
            }
        }
        
        bluetoothHelper.onError = { error ->
            viewModelScope.launch {
                _state.value = _state.value.copy(
                    errorMessage = error,
                    isScanning = false
                )
            }
        }
    }
    
    fun startScan() {
        if (!bluetoothHelper.hasPermissions()) {
            _state.value = _state.value.copy(errorMessage = "缺少蓝牙权限")
            return
        }
        
        if (!bluetoothHelper.isBluetoothEnabled()) {
            _state.value = _state.value.copy(errorMessage = "请先开启蓝牙")
            return
        }
        
        discoveredDevices.clear()
        _state.value = _state.value.copy(
            isScanning = true,
            nearbyDevices = emptyList(),
            errorMessage = null
        )
        
        bluetoothHelper.startScan()
        
        // 10秒后自动停止扫描
        viewModelScope.launch {
            kotlinx.coroutines.delay(10000)
            if (_state.value.isScanning) {
                stopScan()
            }
        }
    }
    
    fun stopScan() {
        bluetoothHelper.stopScan()
        _state.value = _state.value.copy(isScanning = false)
    }
    
    fun connectToDevice(deviceInfo: DeviceInfo) {
        stopScan()
        bluetoothHelper.connect(deviceInfo.device)
        _state.value = _state.value.copy(
            connectedDevice = deviceInfo,
            errorMessage = null
        )
    }
    
    fun disconnect() {
        cancelNoDataTimeout()
        bluetoothHelper.disconnect()
        _state.value = _state.value.copy(
            connectedDevice = null,
            currentHeartRate = 0
        )
    }
    
    private fun resetNoDataTimeout() {
        cancelNoDataTimeout()
        noDataTimeoutJob = viewModelScope.launch {
            kotlinx.coroutines.delay(NO_DATA_TIMEOUT_MS)
            if (_state.value.isConnected) {
                _state.value = _state.value.copy(
                    errorMessage = "5秒无心率数据，自动断开连接"
                )
                disconnect()
            }
        }
    }
    
    private fun cancelNoDataTimeout() {
        noDataTimeoutJob?.cancel()
        noDataTimeoutJob = null
    }
    
    fun clearError() {
        _state.value = _state.value.copy(errorMessage = null)
    }
    
    fun getBluetoothHelper(): BluetoothHelper = bluetoothHelper
    
    override fun onCleared() {
        super.onCleared()
        cancelNoDataTimeout()
        bluetoothHelper.release()
    }
}

