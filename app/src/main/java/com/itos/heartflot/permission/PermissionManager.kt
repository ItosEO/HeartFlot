package com.itos.heartflot.permission

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.itos.heartflot.ui.PermissionGuideData
import com.itos.heartflot.ui.PermissionGuideState

class PermissionManager(
    private val activity: ComponentActivity,
    private val onPermissionStateChanged: (PermissionGuideData) -> Unit,
    private val onAllPermissionsGranted: () -> Unit
) {
    
    // 权限状态
    var bluetoothPermissionsGranted: Boolean = false
        private set
    var notificationPermissionGranted: Boolean = false
        private set
    
    // 权限请求启动器
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var batteryOptimizationLauncher: ActivityResultLauncher<Intent>
    
    fun initialize() {
        // 注册权限请求回调
        permissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            handlePermissionResult(permissions)
        }
        
        // 注册电池优化权限请求
        batteryOptimizationLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { _ ->
            if (isIgnoringBatteryOptimizations()) {
                onPermissionStateChanged(PermissionGuideData(state = PermissionGuideState.HIDDEN))
            }
        }
    }
    
    private fun handlePermissionResult(permissions: Map<String, Boolean>) {
        // 检查蓝牙权限（如果本次请求了蓝牙权限，则更新状态；否则保持原状态）
        val bluetoothGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (permissions.containsKey(Manifest.permission.BLUETOOTH_CONNECT) ||
                permissions.containsKey(Manifest.permission.BLUETOOTH_SCAN)) {
                (permissions[Manifest.permission.BLUETOOTH_CONNECT] ?: false) &&
                (permissions[Manifest.permission.BLUETOOTH_SCAN] ?: false)
            } else {
                bluetoothPermissionsGranted
            }
        } else {
            true
        }
        
        // 检查通知权限
        val notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (permissions.containsKey(Manifest.permission.POST_NOTIFICATIONS)) {
                permissions[Manifest.permission.POST_NOTIFICATIONS] ?: false
            } else {
                notificationPermissionGranted
            }
        } else {
            true
        }
        
        bluetoothPermissionsGranted = bluetoothGranted
        notificationPermissionGranted = notificationGranted
        
        // 根据授予的权限决定下一步
        when {
            bluetoothGranted && notificationGranted -> {
                // 所有权限都已授予
                onPermissionStateChanged(PermissionGuideData(state = PermissionGuideState.HIDDEN))
                onAllPermissionsGranted()
            }
            bluetoothGranted && !notificationGranted -> {
                // 蓝牙已授予，通知未授予
                onAllPermissionsGranted()
                showNotificationPermissionGuide()
            }
            !bluetoothGranted -> {
                // 蓝牙未授予
                showBluetoothPermissionGuide()
            }
        }
    }
    
    fun checkAndRequestPermissions() {
        val bluetoothGranted = checkBluetoothPermissions()
        bluetoothPermissionsGranted = bluetoothGranted
        
        if (!bluetoothGranted) {
            requestBluetoothPermissions()
        } else {
            onAllPermissionsGranted()
            
            val notificationGranted = checkNotificationPermission()
            notificationPermissionGranted = notificationGranted
            
            if (!notificationGranted) {
                showNotificationPermissionGuide()
            } else {
                onPermissionStateChanged(PermissionGuideData(state = PermissionGuideState.HIDDEN))
            }
        }
    }
    
    fun updatePermissionStates() {
        val bluetoothGranted = checkBluetoothPermissions()
        val notificationGranted = checkNotificationPermission()
        
        bluetoothPermissionsGranted = bluetoothGranted
        notificationPermissionGranted = notificationGranted
        
        // 不自动显示提示，只更新状态
        // UI 更新由调用方决定
    }
    
    fun checkBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
    
    fun checkNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
    
    fun requestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val permissions = arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )
            permissionLauncher.launch(permissions)
        }
    }
    
    fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissions = arrayOf(Manifest.permission.POST_NOTIFICATIONS)
            permissionLauncher.launch(permissions)
        }
    }
    
    private fun showBluetoothPermissionGuide() {
        onPermissionStateChanged(
            PermissionGuideData(
                state = PermissionGuideState.PERMISSION_NEEDED,
                title = "缺少蓝牙权限",
                message = "应用需要蓝牙权限来连接心率设备，这是核心功能，请务必授予。",
                buttonText = "授予权限",
                onButtonClick = { requestBluetoothPermissions() }
            )
        )
    }
    
    private fun showNotificationPermissionGuide() {
        onPermissionStateChanged(
            PermissionGuideData(
                state = PermissionGuideState.BATTERY_OPTIMIZATION_SUGGESTION,
                title = "建议开启通知权限",
                message = "通知权限用于显示前台服务通知，确保应用在后台正常运行。这不是必需的，但强烈建议开启。",
                buttonText = "授予权限",
                onButtonClick = { requestNotificationPermission() }
            )
        )
    }
    
    fun checkBatteryOptimization(currentGuideState: PermissionGuideState): PermissionGuideData? {
        // 只有在没有其他待处理提示时，才检查电池优化
        if (currentGuideState != PermissionGuideState.HIDDEN) {
            return null
        }
        
        return if (!isIgnoringBatteryOptimizations()) {
            PermissionGuideData(
                state = PermissionGuideState.BATTERY_OPTIMIZATION_SUGGESTION,
                title = "建议关闭电池优化",
                message = "为确保心率监测服务在后台持续运行，建议将本应用加入电池优化白名单。这不会显著增加耗电量。",
                buttonText = "去设置",
                onButtonClick = { requestIgnoreBatteryOptimizations() }
            )
        } else {
            null
        }
    }
    
    fun isIgnoringBatteryOptimizations(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = activity.getSystemService(Context.POWER_SERVICE) as PowerManager
            powerManager.isIgnoringBatteryOptimizations(activity.packageName)
        } else {
            true
        }
    }
    
    fun requestIgnoreBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${activity.packageName}")
                }
                batteryOptimizationLauncher.launch(intent)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                batteryOptimizationLauncher.launch(intent)
            }
        }
    }
}

