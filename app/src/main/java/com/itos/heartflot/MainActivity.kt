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
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.itos.heartflot.service.HeartRateService
import com.itos.heartflot.ui.HeartRateScreen
import com.itos.heartflot.ui.RecordHistoryScreen
import com.itos.heartflot.ui.theme.HeartFlotTheme
import com.itos.heartflot.viewmodel.HeartRateViewModel

class MainActivity : ComponentActivity() {
    
    private lateinit var viewModel: HeartRateViewModel
    private var heartRateService: HeartRateService? = null
    private var serviceBound = false
    
    private var showHistoryScreen by mutableStateOf(false)
    
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as? HeartRateService.LocalBinder
            heartRateService = localBinder?.getService()
            serviceBound = true
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            heartRateService = null
            serviceBound = false
        }
    }
    
    private lateinit var overlayPermissionLauncher: ActivityResultLauncher<Intent>
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[HeartRateViewModel::class.java]
        
        // 注册悬浮窗权限请求
        overlayPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { _ ->
            // 权限结果在这里处理，但实际检查在使用时进行
        }
        
        // 启动前台服务
        startHeartRateService()
        // 绑定服务
        bindHeartRateService()
        
        setContent {
            HeartFlotTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    if (showHistoryScreen) {
                        RecordHistoryScreen(
                            viewModel = viewModel,
                            onBack = { showHistoryScreen = false }
                        )
                    } else {
                        HeartRateScreen(
                            modifier = Modifier.padding(innerPadding),
                            viewModel = viewModel,
                            onShowHistory = { showHistoryScreen = true },
                            onToggleFloatingWindow = { toggleFloatingWindow() },
                            onRequestOverlayPermission = { requestOverlayPermission() }
                        )
                    }
                }
            }
        }
    }
    
    private fun startHeartRateService() {
        val intent = Intent(this, HeartRateService::class.java).apply {
            action = HeartRateService.ACTION_START
        }
        startForegroundService(intent)
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