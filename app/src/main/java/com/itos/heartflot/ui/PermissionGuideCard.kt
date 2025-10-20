package com.itos.heartflot.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.itos.heartflot.ui.theme.AppShapes

enum class PermissionGuideState {
    HIDDEN,
    PERMISSION_NEEDED,
    SERVICE_NOT_CONNECTED,
    SERVICE_FAILED,
    BATTERY_OPTIMIZATION_SUGGESTION
}

data class PermissionGuideData(
    val state: PermissionGuideState,
    val title: String = "",
    val message: String = "",
    val buttonText: String = "",
    val onButtonClick: () -> Unit = {}
)

@Composable
fun PermissionGuideCard(
    data: PermissionGuideData,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = data.state != PermissionGuideState.HIDDEN,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        // 根据状态选择配色
        val (containerColor, onContainerColor, buttonColor, buttonTextColor) = when (data.state) {
            PermissionGuideState.BATTERY_OPTIMIZATION_SUGGESTION -> {
                // 建议类：使用 tertiary 配色（较柔和）
                listOf(
                    MaterialTheme.colorScheme.tertiaryContainer,
                    MaterialTheme.colorScheme.onTertiaryContainer,
                    MaterialTheme.colorScheme.tertiary,
                    MaterialTheme.colorScheme.onTertiary
                )
            }
            else -> {
                // 错误/警告类：使用 error 配色
                listOf(
                    MaterialTheme.colorScheme.errorContainer,
                    MaterialTheme.colorScheme.onErrorContainer,
                    MaterialTheme.colorScheme.error,
                    MaterialTheme.colorScheme.onError
                )
            }
        }
        
        Column(
            modifier = modifier
                .fillMaxWidth()
                .background(
                    color = containerColor,
                    shape = AppShapes.card
                )
                .padding(20.dp)
        ) {
            Text(
                text = data.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = onContainerColor
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = data.message,
                fontSize = 14.sp,
                color = onContainerColor.copy(alpha = 0.85f),
                lineHeight = 20.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = data.onButtonClick,
                    shape = AppShapes.button,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonColor,
                        contentColor = buttonTextColor
                    )
                ) {
                    Text(
                        text = data.buttonText,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

