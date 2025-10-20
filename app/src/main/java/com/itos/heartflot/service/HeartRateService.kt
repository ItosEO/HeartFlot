package com.itos.heartflot.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import com.itos.heartflot.MainActivity
import com.itos.heartflot.R
import com.itos.heartflot.bluetooth.BluetoothHelper
import com.itos.heartflot.data.HeartRateRecord
import com.itos.heartflot.data.RecordDataStore
import com.itos.heartflot.data.RecordSession
import com.itos.heartflot.viewmodel.DeviceInfo
import com.itos.heartflot.viewmodel.HeartRateState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class HeartRateService : Service() {
    
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "heart_rate_service"
        const val ACTION_START = "com.itos.heartflot.action.START"
        const val ACTION_STOP = "com.itos.heartflot.action.STOP"
    }
    
    private val binder = LocalBinder()
    private lateinit var bluetoothHelper: BluetoothHelper
    private lateinit var notificationManager: NotificationManager
    private lateinit var floatingWindowManager: FloatingWindowManager
    private lateinit var dataStore: RecordDataStore
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // --- State Management ---
    private val _state = MutableStateFlow(HeartRateState())
    val state: StateFlow<HeartRateState> = _state.asStateFlow()

    private val discoveredDevices = mutableMapOf<String, DeviceInfo>()
    private val currentRecords = mutableListOf<HeartRateRecord>()
    
    inner class LocalBinder : Binder() {
        fun getService(): HeartRateService = this@HeartRateService
    }
    
    override fun onCreate() {
        super.onCreate()
        
        bluetoothHelper = BluetoothHelper(this)
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        floatingWindowManager = FloatingWindowManager(this)
        dataStore = RecordDataStore(this)
        
        createNotificationChannel()
        setupBluetoothCallbacks()
        setupFloatingWindowClick()
    }
    
    private fun setupFloatingWindowClick() {
        Log.d("HeartRateService", "Setting up floating window click listener")
        floatingWindowManager.setOnClickListener {
            Log.d("HeartRateService", "Floating window clicked!")
            toggleRecording()
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForeground(NOTIFICATION_ID, createNotification())
            }
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }
    
    override fun onBind(intent: Intent): IBinder {
        return binder
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "心率监测服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "持续监测心率数据"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        val contentText = when {
            !_state.value.isConnected -> "未连接"
            _state.value.currentHeartRate > 0 -> "心率: ${_state.value.currentHeartRate} BPM"
            else -> "等待数据..."
        }
        
        val deviceInfo = _state.value.connectedDevice?.name?.let { " - ${it}" } ?: ""
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("心率监测中$deviceInfo")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }
    
    private fun updateNotification() {
        notificationManager.notify(NOTIFICATION_ID, createNotification())
    }
    
    private fun setupBluetoothCallbacks() {
        bluetoothHelper.onDeviceFound = { device, rssi ->
            serviceScope.launch {
                val deviceInfo = DeviceInfo(
                    device = device,
                    name = try { device.name ?: "未知设备" } catch (e: SecurityException) { "未知设备" },
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
            _state.value = _state.value.copy(currentHeartRate = heartRate)
            updateNotification()
            floatingWindowManager.updateHeartRate(heartRate)
            
            if (_state.value.isRecording) {
                currentRecords.add(
                    HeartRateRecord(
                        timestamp = System.currentTimeMillis(),
                        heartRate = heartRate
                    )
                )
            }
        }
        
        bluetoothHelper.onConnectionStateChanged = { connected ->
            val currentState = _state.value
            if (!connected) {
                if (currentState.isRecording) {
                    stopRecordingInternal()
                }
                // 断开连接时自动关闭悬浮窗
                if (floatingWindowManager.isShowing()) {
                    floatingWindowManager.hide()
                }
                _state.value = currentState.copy(
                    isConnected = false,
                    connectedDevice = null,
                    currentHeartRate = 0,
                    showFloatingWindow = false
                )
            } else {
                _state.value = currentState.copy(isConnected = true)
            }
            updateNotification()
        }

        bluetoothHelper.onError = { error ->
            _state.value = _state.value.copy(errorMessage = error, isScanning = false)
        }
    }
    
    fun getBluetoothHelper(): BluetoothHelper = bluetoothHelper

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

        serviceScope.launch {
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
    
    @SuppressLint("MissingPermission")
    fun connectToDevice(deviceInfo: DeviceInfo) {
        stopScan()
        _state.value = _state.value.copy(
            connectedDevice = deviceInfo,
            errorMessage = null
        )
        bluetoothHelper.connect(deviceInfo.device)
    }
    
    fun disconnect() {
        if (_state.value.isRecording) {
            stopRecordingInternal()
        }
        bluetoothHelper.disconnect()
    }
    
    fun showFloatingWindow(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                return false
            }
        }
        floatingWindowManager.show()
        // 同步当前的心率和录制状态到悬浮窗
        floatingWindowManager.updateHeartRate(_state.value.currentHeartRate)
        floatingWindowManager.updateRecordingState(_state.value.isRecording)
        _state.value = _state.value.copy(showFloatingWindow = true)
        return true
    }
    
    fun hideFloatingWindow() {
        floatingWindowManager.hide()
        _state.value = _state.value.copy(showFloatingWindow = false)
    }
    
    fun isFloatingWindowShowing(): Boolean {
        return floatingWindowManager.isShowing()
    }
    
    private fun startRecordingInternal() {
        if (!_state.value.isConnected) {
            Log.d("HeartRateService", "Not connected, cannot start recording")
            return
        }
        
        val sessionId = UUID.randomUUID().toString()
        _state.value = _state.value.copy(
            isRecording = true,
            currentSessionId = sessionId
        )
        floatingWindowManager.updateRecordingState(true)
        Log.d("HeartRateService", "Recording started, sessionId=$sessionId")
    }
    
    private fun stopRecordingInternal() {
        val currentState = _state.value
        val sessionId = currentState.currentSessionId
        
        if (sessionId != null && currentRecords.isNotEmpty()) {
            serviceScope.launch {
                val session = RecordSession(
                    sessionId = sessionId,
                    startTime = currentRecords.first().timestamp,
                    endTime = currentRecords.last().timestamp,
                    deviceName = currentState.connectedDevice?.name,
                    deviceAddress = currentState.connectedDevice?.address,
                    records = currentRecords.toList()
                )
                dataStore.addSession(session)
                currentRecords.clear()
            }
        }
        
        _state.value = currentState.copy(
            isRecording = false,
            currentSessionId = null
        )
        floatingWindowManager.updateRecordingState(false)
    }
    
    fun toggleRecording() {
        Log.d("HeartRateService", "toggleRecording called, isRecording=${_state.value.isRecording}")
        if (_state.value.isRecording) {
            stopRecordingInternal()
        } else {
            startRecordingInternal()
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(errorMessage = null)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (_state.value.isRecording) {
            stopRecordingInternal()
        }
        floatingWindowManager.release()
        bluetoothHelper.release()
        serviceScope.cancel()
    }
}

