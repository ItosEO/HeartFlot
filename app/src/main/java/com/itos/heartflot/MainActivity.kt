package com.itos.heartflot

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.itos.heartflot.service.HeartRateService
import com.itos.heartflot.ui.HeartRateScreen
import com.itos.heartflot.ui.theme.HeartFlotTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // 启动前台服务
        startHeartRateService()
        
        setContent {
            HeartFlotTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    HeartRateScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
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
    
    override fun onDestroy() {
        super.onDestroy()
        // 可选：停止服务
         stopService(Intent(this, HeartRateService::class.java))
    }
}