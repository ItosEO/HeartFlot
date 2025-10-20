package com.itos.heartflot.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.itos.heartflot.MainActivity
import com.itos.heartflot.R
import com.itos.heartflot.bluetooth.BluetoothHelper

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
    
    private var currentHeartRate: Int = 0
    private var connectedDeviceName: String? = null
    private var isConnected: Boolean = false
    
    inner class LocalBinder : Binder() {
        fun getService(): HeartRateService = this@HeartRateService
    }
    
    override fun onCreate() {
        super.onCreate()
        
        bluetoothHelper = BluetoothHelper(this)
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        
        createNotificationChannel()
        setupBluetoothCallbacks()
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
            !isConnected -> "未连接"
            currentHeartRate > 0 -> "心率: $currentHeartRate BPM"
            else -> "等待数据..."
        }
        
        val deviceInfo = connectedDeviceName?.let { " - $it" } ?: ""
        
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
        bluetoothHelper.onHeartRateReceived = { heartRate ->
            currentHeartRate = heartRate
            updateNotification()
        }
        
        bluetoothHelper.onConnectionStateChanged = { connected ->
            isConnected = connected
            if (!connected) {
                connectedDeviceName = null
                currentHeartRate = 0
            }
            updateNotification()
        }
    }
    
    fun getBluetoothHelper(): BluetoothHelper = bluetoothHelper
    
    @SuppressLint("MissingPermission")
    fun connectToDevice(device: BluetoothDevice) {
        connectedDeviceName = try {
            device.name ?: device.address
        } catch (e: SecurityException) {
            device.address
        }
        bluetoothHelper.connect(device)
    }
    
    fun disconnect() {
        bluetoothHelper.disconnect()
        connectedDeviceName = null
        currentHeartRate = 0
        isConnected = false
    }
    
    override fun onDestroy() {
        super.onDestroy()
        bluetoothHelper.release()
    }
}

