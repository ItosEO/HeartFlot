package com.itos.heartflot.viewmodel

import android.bluetooth.BluetoothDevice

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
    val errorMessage: String? = null,
    val isRecording: Boolean = false,
    val showFloatingWindow: Boolean = false,
    val currentSessionId: String? = null
)
