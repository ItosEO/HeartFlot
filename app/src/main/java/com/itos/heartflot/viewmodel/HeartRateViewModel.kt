package com.itos.heartflot.viewmodel

import android.app.Application
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.itos.heartflot.data.HeartRateRecord
import com.itos.heartflot.data.RecordDataStore
import com.itos.heartflot.data.RecordSession
import com.itos.heartflot.service.HeartRateService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class HeartRateViewModel(application: Application) : AndroidViewModel(application) {
    
    private val dataStore = RecordDataStore(application)
    private var heartRateService: HeartRateService? = null
    
    private val _state = MutableStateFlow(HeartRateState())
    val state: StateFlow<HeartRateState> = _state.asStateFlow()
    
    private val _allSessions = MutableStateFlow<List<RecordSession>>(emptyList())
    val allSessions: StateFlow<List<RecordSession>> = _allSessions.asStateFlow()
    
    private val currentRecords = mutableListOf<HeartRateRecord>()
    
    private val discoveredDevices = mutableMapOf<String, DeviceInfo>()
    private var noDataTimeoutJob: kotlinx.coroutines.Job? = null
    
    companion object {
        private const val NO_DATA_TIMEOUT_MS = 5_000L
    }
    
    init {
        loadSessions()
    }
    
    fun setHeartRateService(service: HeartRateService) {
        this.heartRateService = service
        observeServiceState()
    }

    private fun observeServiceState() {
        viewModelScope.launch {
            heartRateService?.state?.collect { serviceState ->
                _state.value = serviceState
            }
        }
    }

    private fun loadSessions() {
        viewModelScope.launch {
            dataStore.sessions.collect { sessions ->
                _allSessions.value = sessions
            }
        }
    }
    
    fun startScan() {
        heartRateService?.startScan()
    }
    
    fun stopScan() {
        heartRateService?.stopScan()
    }
    
    fun connectToDevice(deviceInfo: DeviceInfo) {
        heartRateService?.connectToDevice(deviceInfo)
    }
    
    fun disconnect() {
        heartRateService?.disconnect()
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
        heartRateService?.clearError()
    }
    
    fun getBluetoothHelper(): com.itos.heartflot.bluetooth.BluetoothHelper? = heartRateService?.getBluetoothHelper()
    
    fun startRecording() {
        if (!_state.value.isConnected) {
            _state.value = _state.value.copy(errorMessage = "请先连接设备")
            return
        }
        
        val sessionId = UUID.randomUUID().toString()
        _state.value = _state.value.copy(
            isRecording = true,
            currentSessionId = sessionId,
            errorMessage = null
        )
    }
    
    fun stopRecording() {
        val currentState = _state.value
        val sessionId = currentState.currentSessionId
        val device = currentState.connectedDevice
        
        if (sessionId != null && currentRecords.isNotEmpty()) {
            viewModelScope.launch {
                val session = RecordSession(
                    sessionId = sessionId,
                    startTime = currentRecords.first().timestamp,
                    endTime = currentRecords.last().timestamp,
                    deviceName = device?.name,
                    deviceAddress = device?.address,
                    records = currentRecords.toList()
                )
                dataStore.addSession(session)
                currentRecords.clear()
            }
        }
        
        _state.value = _state.value.copy(
            isRecording = false,
            currentSessionId = null
        )
    }
    
    fun toggleRecording() {
        heartRateService?.toggleRecording()
    }
    
    fun showFloatingWindow() {
        _state.value = _state.value.copy(showFloatingWindow = true)
    }
    
    fun hideFloatingWindow() {
        _state.value = _state.value.copy(showFloatingWindow = false)
    }
    
    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            dataStore.deleteSession(sessionId)
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        // ViewModel清除时不再需要释放蓝牙帮助类
    }
}

