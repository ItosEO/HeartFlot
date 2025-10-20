package com.itos.heartflot

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import android.widget.Toast
import com.itos.heartflot.permission.PermissionManager
import com.itos.heartflot.service.HeartRateService
import com.itos.heartflot.ui.AppInfoScreen
import com.itos.heartflot.ui.HeartRateScreen
import com.itos.heartflot.ui.PermissionGuideData
import com.itos.heartflot.ui.PermissionGuideState
import com.itos.heartflot.ui.RecordHistoryScreen
import com.itos.heartflot.ui.theme.HeartFlotTheme
import com.itos.heartflot.viewmodel.HeartRateViewModel

class MainActivity : ComponentActivity() {
    
    private lateinit var viewModel: HeartRateViewModel
    private var heartRateService: HeartRateService? = null
    private var serviceBound = false
    
    private var showHistoryScreen by mutableStateOf(false)
    private var showAppInfoScreen by mutableStateOf(false)
    private var lastBackPressTime = 0L
    private val backPressInterval = 2000L // 2秒内连续按返回键才退出
    
    // 权限管理器
    private lateinit var permissionManager: PermissionManager
    
    // 权限引导卡片状态
    private var permissionGuideData by mutableStateOf(
        PermissionGuideData(state = PermissionGuideState.HIDDEN)
    )
    
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as? HeartRateService.LocalBinder
            heartRateService = localBinder?.getService()
            serviceBound = true
            // 将 Service 实例注入 ViewModel
            heartRateService?.let {
                viewModel.setHeartRateService(it)
                // 服务连接成功，检查电池优化
                val batteryGuide = permissionManager.checkBatteryOptimization(permissionGuideData.state)
                batteryGuide?.let { guide -> permissionGuideData = guide }
            }
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            heartRateService = null
            serviceBound = false
            // 服务断开连接，显示引导卡片
            if (permissionManager.bluetoothPermissionsGranted) {
                permissionGuideData = PermissionGuideData(
                    state = PermissionGuideState.SERVICE_NOT_CONNECTED,
                    title = "服务未连接",
                    message = "权限已授予但服务未连接，请尝试重启服务。",
                    buttonText = "重启服务",
                    onButtonClick = { restartService() }
                )
            }
        }
    }
    
    private lateinit var overlayPermissionLauncher: ActivityResultLauncher<Intent>
    
    @OptIn(ExperimentalSharedTransitionApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[HeartRateViewModel::class.java]
        
        // 初始化权限管理器
        permissionManager = PermissionManager(
            activity = this,
            onPermissionStateChanged = { guideData ->
                permissionGuideData = guideData
            },
            onAllPermissionsGranted = {
                if (!serviceBound) {
                    startHeartRateService()
                    bindHeartRateService()
                } else {
                    // 服务已连接，直接检查电池优化
                    val batteryGuide = permissionManager.checkBatteryOptimization(permissionGuideData.state)
                    batteryGuide?.let { permissionGuideData = it }
                }
            }
        )
        permissionManager.initialize()
        
        // 注册悬浮窗权限请求
        overlayPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { _ ->
            // 权限结果在这里处理，但实际检查在使用时进行
        }
        
        // 检查并请求权限
        permissionManager.checkAndRequestPermissions()
        
        setContent {
            HeartFlotTheme {
                // 处理返回键
                BackHandler(enabled = true) {
                    when {
                        showAppInfoScreen -> {
                            // 在应用信息界面，返回主界面
                            showAppInfoScreen = false
                        }
                        showHistoryScreen -> {
                            // 在历史记录界面，返回主界面
                            showHistoryScreen = false
                        }
                        else -> {
                            // 在主界面，二次确认退出
                            handleMainScreenBack()
                        }
                    }
                }
                
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SharedTransitionLayout {
                        AnimatedContent(
                            targetState = when {
                                showAppInfoScreen -> "appInfo"
                                showHistoryScreen -> "history"
                                else -> "main"
                            },
                            transitionSpec = {
                                fadeIn(animationSpec = tween(500, delayMillis = 140)) togetherWith
                                        fadeOut(animationSpec = tween(200))
                            },
                            label = "screen_transition"
                        ) { targetScreen ->
                            when (targetScreen) {
                                "appInfo" -> {
                                    AppInfoScreen(
                                        onBack = { showAppInfoScreen = false },
                                        sharedTransitionScope = this@SharedTransitionLayout,
                                        animatedContentScope = this@AnimatedContent
                                    )
                                }
                                "history" -> {
                                    RecordHistoryScreen(
                                        viewModel = viewModel,
                                        onBack = { showHistoryScreen = false },
                                        sharedTransitionScope = this@SharedTransitionLayout,
                                        animatedContentScope = this@AnimatedContent
                                    )
                                }
                                else -> {
                                    HeartRateScreen(
                                        modifier = Modifier.padding(innerPadding),
                                        viewModel = viewModel,
                                        permissionGuideData = permissionGuideData,
                                        onShowHistory = { showHistoryScreen = true },
                                        onShowAppInfo = { showAppInfoScreen = true },
                                        onToggleFloatingWindow = { toggleFloatingWindow() },
                                        onRequestOverlayPermission = { requestOverlayPermission() },
                                        sharedTransitionScope = this@SharedTransitionLayout,
                                        animatedContentScope = this@AnimatedContent
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    
    private fun startHeartRateService() {
        if (!permissionManager.bluetoothPermissionsGranted) {
            permissionGuideData = PermissionGuideData(
                state = PermissionGuideState.PERMISSION_NEEDED,
                title = "需要蓝牙权限",
                message = "请先授予蓝牙权限以启动心率监测服务。",
                buttonText = "授予权限",
                onButtonClick = { permissionManager.requestBluetoothPermissions() }
            )
            return
        }
        
        try {
            val intent = Intent(this, HeartRateService::class.java).apply {
                action = HeartRateService.ACTION_START
            }
            startForegroundService(intent)
        } catch (e: SecurityException) {
            permissionGuideData = PermissionGuideData(
                state = PermissionGuideState.SERVICE_FAILED,
                title = "服务启动失败",
                message = "由于权限不足，无法启动心率监测服务。请检查应用权限设置。",
                buttonText = "重新尝试",
                onButtonClick = { permissionManager.checkAndRequestPermissions() }
            )
            e.printStackTrace()
        } catch (e: Exception) {
            permissionGuideData = PermissionGuideData(
                state = PermissionGuideState.SERVICE_FAILED,
                title = "服务启动失败",
                message = "启动心率监测服务时出现错误：${e.message ?: "未知错误"}",
                buttonText = "重试",
                onButtonClick = { restartService() }
            )
            e.printStackTrace()
        }
    }
    
    private fun restartService() {
        permissionGuideData = PermissionGuideData(state = PermissionGuideState.HIDDEN)
        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
        }
        heartRateService = null
        startHeartRateService()
        bindHeartRateService()
    }
    
    private fun bindHeartRateService() {
        val intent = Intent(this, HeartRateService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }
    
    private fun toggleFloatingWindow() {
        val service = heartRateService ?: return
        
        if (service.isFloatingWindowShowing()) {
            service.hideFloatingWindow()
        } else {
            if (!service.showFloatingWindow()) {
                // 没有权限，请求权限
                requestOverlayPermission()
            }
        }
    }
    
    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                overlayPermissionLauncher.launch(intent)
            }
        }
    }
    
    private fun handleMainScreenBack() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBackPressTime < backPressInterval) {
            // 两次按返回键间隔小于2秒，退出应用
            finish()
        } else {
            // 第一次按返回键，显示提示
            lastBackPressTime = currentTime
            Toast.makeText(this, "再次返回退出应用", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onResume() {
        super.onResume()
        // 从后台返回时重新检查权限状态
        permissionManager.updatePermissionStates()
        
        // 根据权限状态更新 UI
        if (permissionManager.bluetoothPermissionsGranted && permissionManager.notificationPermissionGranted) {
            // 所有权限已授予
            if (permissionGuideData.state == PermissionGuideState.PERMISSION_NEEDED ||
                permissionGuideData.state == PermissionGuideState.BATTERY_OPTIMIZATION_SUGGESTION) {
                permissionGuideData = PermissionGuideData(state = PermissionGuideState.HIDDEN)
                if (serviceBound) {
                    val batteryGuide = permissionManager.checkBatteryOptimization(permissionGuideData.state)
                    batteryGuide?.let { permissionGuideData = it }
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
        }
        // 可选：停止服务
        // stopService(Intent(this, HeartRateService::class.java))
    }
}