package com.itos.heartflot.ui

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.itos.heartflot.BuildConfig
import com.itos.heartflot.R
import com.itos.heartflot.ui.theme.AnimationDurations
import com.itos.heartflot.ui.theme.AppShapes
import com.itos.heartflot.ui.theme.HeartRed40
import com.itos.heartflot.ui.theme.HeartRed80
import com.itos.heartflot.ui.theme.HeartRedGrey40
import com.itos.heartflot.ui.theme.HeartRedGrey80
import com.kyant.capsule.ContinuousRoundedRectangle

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun AppInfoScreen(
    onBack: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope
) {
    with(sharedTransitionScope) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .sharedBounds(
                rememberSharedContentState(key = "appinfo_container"),
                animatedContentScope,
                boundsTransform = { _, _ -> tween(durationMillis = AnimationDurations.SCREEN_TRANSITION_DURATION) }
            ),
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "应用信息",
                        modifier = Modifier.sharedBounds(
                            rememberSharedContentState(key = "appinfo_title"),
                            animatedContentScope,
                            boundsTransform = { _, _ -> tween(durationMillis = AnimationDurations.SCREEN_TRANSITION_DURATION) }
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 应用信息卡片
            AppInfoCard()
            
            // 作者信息卡片
            AuthorInfoCard()
            
            // 技术栈卡片
            TechStackSection()
        }
    }
    }
}

@Composable
private fun AppInfoCard() {
    val isDarkTheme = isSystemInDarkTheme()
    val heartRed = if (isDarkTheme) HeartRed80 else HeartRed40
    val heartRedGrey = if (isDarkTheme) HeartRedGrey80 else HeartRedGrey40
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = AppShapes.card,
        colors = CardDefaults.cardColors(
            containerColor = heartRed
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 应用图标
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(ContinuousRoundedRectangle(16.dp))
                    .background(heartRedGrey),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "应用图标",
                    modifier = Modifier.size(80.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 应用名称
            Text(
                text = "HeartFlot",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // 版本号
            Text(
                text = BuildConfig.VERSION_NAME,
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun AuthorInfoCard() {
    val context = LocalContext.current
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = AppShapes.card
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("coolmarket://u/3287595"))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // 如果酷安未安装，打开浏览器
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.coolapk.com/u/3287595"))
                        context.startActivity(intent)
                    }
                }
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "作者",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "ItosEO",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_coolapk),
                contentDescription = "酷安",
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TechStackSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = AppShapes.card
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "技术栈",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val techCategories = listOf(
                "语言与框架" to listOf("Kotlin", "Jetpack Compose"),
                "架构与模式" to listOf("ViewModel", "Coroutines", "Flow"),
                "数据存储" to listOf("DataStore Preferences"),
                "蓝牙通信" to listOf("Bluetooth LE", "Heart Rate Profile"),
                "UI设计" to listOf("Material Design 3", "Capsule")
            )
            
            techCategories.forEachIndexed { index, (category, items) ->
                TechCategory(category, items)
                if (index < techCategories.size - 1) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TechCategory(category: String, items: List<String>) {
    Column {
        Text(
            text = category,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onTertiaryContainer
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items.forEach { item ->
                TechChip(item)
            }
        }
    }
}

@Composable
private fun TechChip(text: String) {
    Box(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                shape = AppShapes.badge
            )
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onTertiaryContainer
        )
    }
}
