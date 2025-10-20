package com.itos.heartflot.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.itos.heartflot.ui.theme.AppShapes
import com.itos.heartflot.viewmodel.DeviceInfo
import com.itos.heartflot.viewmodel.HeartRateViewModel

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun HeartRateScreen(
    modifier: Modifier = Modifier,
    viewModel: HeartRateViewModel = viewModel(),
    onShowHistory: () -> Unit = {},
    onToggleFloatingWindow: () -> Unit = {},
    onRequestOverlayPermission: () -> Unit = {},
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }
    
    var permissionsGranted by remember { mutableStateOf(false) }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        permissionsGranted = results.values.all { it }
    }
    
    LaunchedEffect(Unit) {
        permissionsGranted = permissions.all {
            ContextCompat.checkSelfPermission(context, it) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        if (!permissionsGranted) {
            permissionLauncher.launch(permissions)
        }
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // 心率显示区域
            HeartRateDisplay(
                heartRate = state.currentHeartRate,
                isConnected = state.isConnected
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 状态信息
            StatusInfo(
                isConnected = state.isConnected,
                connectedDevice = state.connectedDevice
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 操作按钮
            ActionButtons(
                isScanning = state.isScanning,
                isConnected = state.isConnected,
                permissionsGranted = permissionsGranted,
                onStartScan = { viewModel.startScan() },
                onStopScan = { viewModel.stopScan() },
                onDisconnect = { viewModel.disconnect() },
                onRequestPermissions = { permissionLauncher.launch(permissions) }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 记录与悬浮窗控制
            RecordControls(
                isConnected = state.isConnected,
                isRecording = state.isRecording,
                onToggleRecording = { viewModel.toggleRecording() },
                onShowHistory = onShowHistory,
                onToggleFloatingWindow = onToggleFloatingWindow,
                sharedTransitionScope = sharedTransitionScope,
                animatedContentScope = animatedContentScope
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 设备列表 - 仅未连接时显示
            if (!state.isConnected) {
                DeviceList(
                    devices = state.nearbyDevices,
                    isScanning = state.isScanning,
                    onDeviceClick = { device ->
                        viewModel.connectToDevice(device)
                    }
                )
            }
        }
        
        // 错误提示
        state.errorMessage?.let { error ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                action = {
                    Text(
                        text = "关闭",
                        modifier = Modifier.clickable { viewModel.clearError() }
                    )
                }
            ) {
                Text(error)
            }
        }
    }
}

@Composable
fun HeartRateDisplay(
    heartRate: Int,
    isConnected: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = AppShapes.card
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "实时心率",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = if (isConnected) heartRate.toString() else "--",
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
                Text(
                    text = " BPM",
                    fontSize = 24.sp,
                    modifier = Modifier.padding(bottom = 8.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun StatusInfo(
    isConnected: Boolean,
    connectedDevice: DeviceInfo?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = AppShapes.card
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("连接状态:")
                Text(
                    text = if (isConnected) "已连接" else "未连接",
                    color = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
            
            if (connectedDevice != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("设备:")
                    Text(connectedDevice.name)
                }
            }
        }
    }
}

@Composable
fun ActionButtons(
    isScanning: Boolean,
    isConnected: Boolean,
    permissionsGranted: Boolean,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onDisconnect: () -> Unit,
    onRequestPermissions: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (!permissionsGranted) {
            Button(
                onClick = onRequestPermissions,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = AppShapes.button,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp
                )
            ) {
                Text(
                    text = "请求权限",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        } else {
            Button(
                onClick = if (isScanning) onStopScan else onStartScan,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                enabled = !isConnected,
                shape = AppShapes.button,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.38f)
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp
                )
            ) {
                Text(
                    text = if (isScanning) "停止扫描" else "扫描设备",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isConnected) 
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    else 
                        MaterialTheme.colorScheme.onSurface
                )
            }
            
            Button(
                onClick = onDisconnect,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                enabled = isConnected,
                shape = AppShapes.button,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.38f)
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp
                )
            ) {
                Text(
                    text = "断开连接",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (!isConnected) 
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    else 
                        MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
fun DeviceList(
    devices: List<DeviceInfo>,
    isScanning: Boolean,
    onDeviceClick: (DeviceInfo) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = AppShapes.card
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "附近设备 (${devices.size})",
                    style = MaterialTheme.typography.titleMedium
                )
                
                if (isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (devices.isEmpty() && !isScanning) {
                Text(
                    text = "点击扫描设备开始查找心率设备",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(devices) { device ->
                        DeviceItem(
                            device = device,
                            onClick = { onDeviceClick(device) }
                        )
                        if (device != devices.last()) {
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeviceItem(
    device: DeviceInfo,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = device.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = device.address,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        
        Text(
            text = "${device.rssi} dBm",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .clip(AppShapes.badge)
                .background(color = MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun RecordControls(
    isConnected: Boolean,
    isRecording: Boolean,
    onToggleRecording: () -> Unit,
    onShowHistory: () -> Unit,
    onToggleFloatingWindow: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope
) {
    with(sharedTransitionScope) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 记录控制按钮
        Button(
            onClick = onToggleRecording,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = isConnected,
            shape = AppShapes.button,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRecording) 
                    MaterialTheme.colorScheme.error 
                else 
                    MaterialTheme.colorScheme.primary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.38f)
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp
            )
        ) {
            Text(
                text = if (isRecording) "停止记录" else "开始记录",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        
        // 悬浮窗和历史记录按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalButton(
                onClick = onToggleFloatingWindow,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = AppShapes.button,
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = "悬浮窗",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "悬浮窗",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            FilledTonalButton(
                onClick = onShowHistory,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .sharedBounds(
                        rememberSharedContentState(key = "history_container"),
                        animatedContentScope
                    ),
                shape = AppShapes.button,
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp
                )
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "历史记录",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "历史记录",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.sharedBounds(
                        rememberSharedContentState(key = "history_title"),
                        animatedContentScope
                    )
                )
            }
        }
    }
    }
}
