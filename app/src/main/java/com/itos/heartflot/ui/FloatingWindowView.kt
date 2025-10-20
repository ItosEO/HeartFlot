package com.itos.heartflot.ui

import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.kyant.capsule.ContinuousCapsule

class FloatingWindowView(context: Context) : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    
    private var heartRate by mutableIntStateOf(0)
    private var isRecording by mutableStateOf(false)
    private var onClickListener: (() -> Unit)? = null
    var onMove: ((Float, Float) -> Unit)? = null
    
    private var lastX = 0f
    private var lastY = 0f
    private var isDragging = false
    
    private val lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController: SavedStateRegistryController = SavedStateRegistryController.create(this)
    private val store: ViewModelStore = ViewModelStore()
    
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    override val viewModelStore: ViewModelStore get() = store
    
    val view: ComposeView = ComposeView(context).apply {
        setViewTreeLifecycleOwner(this@FloatingWindowView)
        setViewTreeViewModelStoreOwner(this@FloatingWindowView)
        setViewTreeSavedStateRegistryOwner(this@FloatingWindowView)
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        
        setupTouchListener(this)
        setContent {
            FloatingCapsule(
                heartRate = heartRate,
                isRecording = isRecording
            )
        }
    }
    
    init {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }
    
    fun onCreate() {
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }
    
    fun onStart() {
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }
    
    fun onResume() {
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }
    
    fun onPause() {
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }
    
    fun onStop() {
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }
    
    fun onDestroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        store.clear()
    }
    
    fun updateHeartRate(rate: Int) {
        heartRate = rate
    }
    
    fun updateRecordingState(recording: Boolean) {
        isRecording = recording
    }
    
    fun setOnClickListener(listener: () -> Unit) {
        onClickListener = listener
    }
    
    @SuppressLint("ClickableViewAccessibility")
    private fun setupTouchListener(composeView: ComposeView) {
        composeView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastX = event.rawX
                    lastY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - lastX
                    val dy = event.rawY - lastY
                    val distance = kotlin.math.sqrt(dx * dx + dy * dy)
                    
                    if (distance > 10) {
                        isDragging = true
                        onMove?.invoke(dx, dy)
                        lastX = event.rawX
                        lastY = event.rawY
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        onClickListener?.invoke()
                    }
                    isDragging = false
                    true
                }
                else -> false
            }
        }
    }
}

@Composable
fun FloatingCapsule(
    heartRate: Int,
    isRecording: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(width = 100.dp, height = 48.dp)
            .background(
                color = if (isRecording)
                    Color(0xD9F44336) // 半透明红色 (0.85 alpha)
                else
                    Color(0xD94CAF50), // 半透明绿色 (0.85 alpha)
                shape = ContinuousCapsule()
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = heartRate.toString(),
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

