package com.itos.heartflot.ui

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.itos.heartflot.data.RecordSession
import com.itos.heartflot.ui.theme.AppShapes
import com.itos.heartflot.viewmodel.HeartRateViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun RecordHistoryScreen(
    viewModel: HeartRateViewModel,
    onBack: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope
) {
    with(sharedTransitionScope) {
    val sessions by viewModel.allSessions.collectAsState()
    
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .sharedBounds(
                rememberSharedContentState(key = "history_container"),
                animatedContentScope
            ),
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "历史记录",
                        modifier = Modifier.sharedBounds(
                            rememberSharedContentState(key = "history_title"),
                            animatedContentScope
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "暂无记录",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(sessions, key = { it.sessionId }) { session ->
                    RecordSessionCard(
                        session = session,
                        onDelete = { viewModel.deleteSession(session.sessionId) },
                        onNoteUpdate = { note -> viewModel.updateSessionNote(session.sessionId, note) }
                    )
                }
            }
        }
    }
    }
}

@Composable
fun RecordSessionCard(
    session: RecordSession,
    onDelete: () -> Unit,
    onNoteUpdate: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var noteText by remember(session.note) { mutableStateOf(session.note) }
    var isDeleteConfirming by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.card),
        shape = AppShapes.card,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            // 原有的头部信息区域
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { 
                        isExpanded = !isExpanded
                        isDeleteConfirming = false
                    }
                    .padding(vertical = 8.dp, horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 左侧：设备名和日期
                Column(modifier = Modifier.weight(1.5f)) {
                    Text(
                        text = session.deviceName ?: "未知设备",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.padding(1.dp))
                    Text(
                        text = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(session.startTime)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // 中间：心率统计
                Row(
                    modifier = Modifier.weight(1.7f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CompactHeartRateStat(
                        value = session.minHeartRate,
                        color = Color(0xFF4CAF50) // Green
                    )
                    CompactHeartRateStat(
                        value = session.averageHeartRate,
                        color = Color(0xFF2196F3) // Blue
                    )
                    CompactHeartRateStat(
                        value = session.maxHeartRate,
                        color = Color(0xFFF44336) // Red
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // 右侧：时长和删除按钮
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.weight(1.3f)
                ) {
                Text(
                    text = formatDuration(session.endTime - session.startTime),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 8.dp)
                )
                IconButton(
                    onClick = {
                        if (isDeleteConfirming) {
                            onDelete()
                        } else {
                            isDeleteConfirming = true
                        }
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isDeleteConfirming) Icons.Default.Check else Icons.Default.Delete,
                        contentDescription = if (isDeleteConfirming) "确认删除" else "删除",
                        tint = if (isDeleteConfirming) 
                            MaterialTheme.colorScheme.error 
                        else 
                            MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                }
                }
            }
            
            // 展开的折线图区域
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ),
                exit = shrinkVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 折线图
                    HeartRateChart(
                        session = session,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .padding(horizontal = 8.dp, vertical = 8.dp)
                    )
                    
                    // 备注区域
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 12.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                shape = AppShapes.badge
                            )
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { isDeleteConfirming = false }
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        BasicTextField(
                            value = noteText,
                            onValueChange = { 
                                noteText = it
                                onNoteUpdate(it)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            decorationBox = { innerTextField ->
                                if (noteText.isEmpty()) {
                                    Text(
                                        text = "未设置备注，点击此处输入",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactHeartRateStat(value: Int, color: Color) {
    Box(
        modifier = Modifier
            .background(
                color = color.copy(alpha = 0.2f),
                shape = AppShapes.badge
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun HeartRateChart(
    session: RecordSession,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    
    if (session.records.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "无数据",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }
    
    Canvas(modifier = modifier) {
        val padding = 32.dp.toPx()
        val chartWidth = size.width - padding * 2
        val chartHeight = size.height - padding * 2
        
        val records = session.records
        
        // Y轴: 计算5的倍数范围
        val minHrRaw = session.minHeartRate - 10
        val maxHrRaw = session.maxHeartRate + 10
        val minHr = (minHrRaw / 5) * 5
        val maxHr = ((maxHrRaw + 4) / 5) * 5
        val hrRange = maxHr - minHr
        
        if (hrRange == 0 || records.size < 2) {
            return@Canvas
        }
        
        val timeRange = records.last().timestamp - records.first().timestamp
        
        // 点状虚线样式 (用于Y轴)
        val dottedPathEffect = PathEffect.dashPathEffect(
            intervals = floatArrayOf(1.dp.toPx(), 3.dp.toPx()),
            phase = 0f
        )
        
        // 绘制Y轴刻度和标签 (5的倍数, 横向虚线)
        val yStepCount = hrRange / 5
        for (i in 0..yStepCount) {
            val hr = minHr + (i * 5)
            val y = padding + chartHeight - ((hr - minHr).toFloat() / hrRange) * chartHeight
            
            // 横向点状虚线
            drawLine(
                color = onSurfaceVariantColor.copy(alpha = 0.2f),
                start = Offset(padding, y),
                end = Offset(padding + chartWidth, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = dottedPathEffect
            )
            
            // Y轴标签
            val textLayoutResult = textMeasurer.measure(
                text = hr.toString(),
                style = TextStyle(
                    fontSize = 10.sp,
                    color = onSurfaceVariantColor
                )
            )
            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(
                    padding - textLayoutResult.size.width - 8.dp.toPx(),
                    y - textLayoutResult.size.height / 2
                )
            )
        }
        
        // 绘制X轴时间标签 (相对时间, 竖向实线)
        val xLabels = 4
        for (i in 0..xLabels) {
            val x = padding + (i.toFloat() / xLabels) * chartWidth
            val elapsedMillis = (timeRange * i / xLabels)
            val timeStr = formatElapsedTime(elapsedMillis)
            
            // 竖向实线
            drawLine(
                color = onSurfaceVariantColor.copy(alpha = 0.2f),
                start = Offset(x, padding),
                end = Offset(x, padding + chartHeight),
                strokeWidth = 1.dp.toPx()
            )
            
            // X轴标签
            val textLayoutResult = textMeasurer.measure(
                text = timeStr,
                style = TextStyle(
                    fontSize = 9.sp,
                    color = onSurfaceVariantColor
                )
            )
            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(
                    x - textLayoutResult.size.width / 2,
                    padding + chartHeight + 8.dp.toPx()
                )
            )
        }
        
        // 根据心率值获取颜色
        fun getHeartRateColor(heartRate: Int): Color {
            return when {
                heartRate < 100 -> Color(0xFF4CAF50) // 绿色
                heartRate < 120 -> Color(0xFFFFC107) // 黄色
                else -> Color(0xFFF44336) // 红色
            }
        }
        
        // 分段绘制折线，每段根据心率值着色
        records.forEachIndexed { index, record ->
            if (index < records.size - 1) {
                val nextRecord = records[index + 1]
                
                val x1 = padding + ((record.timestamp - records.first().timestamp).toFloat() / timeRange) * chartWidth
                val y1 = padding + chartHeight - ((record.heartRate - minHr).toFloat() / hrRange) * chartHeight
                
                val x2 = padding + ((nextRecord.timestamp - records.first().timestamp).toFloat() / timeRange) * chartWidth
                val y2 = padding + chartHeight - ((nextRecord.heartRate - minHr).toFloat() / hrRange) * chartHeight
                
                // 使用当前点的心率值决定线段颜色
                val segmentColor = getHeartRateColor(record.heartRate)
                
                drawLine(
                    color = segmentColor,
                    start = Offset(x1, y1),
                    end = Offset(x2, y2),
                    strokeWidth = 2.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
        
        // 绘制数据点
//        records.forEach { record ->
//            val x = padding + ((record.timestamp - records.first().timestamp).toFloat() / timeRange) * chartWidth
//            val y = padding + chartHeight - ((record.heartRate - minHr).toFloat() / hrRange) * chartHeight
//
//            drawCircle(
//                color = primaryColor,
//                radius = 2.dp.toPx(),
//                center = Offset(x, y)
//            )
//        }
    }
}

private fun formatElapsedTime(millis: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(hours)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) - 
                  TimeUnit.HOURS.toSeconds(hours) - 
                  TimeUnit.MINUTES.toSeconds(minutes)
    
    return buildString {
        if (hours > 0) {
            append("${hours}h")
        }
        if (minutes > 0) {
            append("${minutes}m")
        }
        if (seconds > 0 || (hours == 0L && minutes == 0L)) {
            append("${seconds}s")
        }
    }
}

private fun formatDuration(millis: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(minutes)
    return "${minutes}m${seconds}s"
}

