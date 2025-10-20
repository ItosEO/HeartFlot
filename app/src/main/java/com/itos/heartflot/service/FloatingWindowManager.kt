package com.itos.heartflot.service

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import com.itos.heartflot.ui.FloatingWindowView

class FloatingWindowManager(private val context: Context) {
    
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var floatingView: FloatingWindowView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    
    private var isShowing = false
    
    @SuppressLint("ClickableViewAccessibility")
    fun show() {
        if (isShowing) return
        
        floatingView = FloatingWindowView(context).apply {
            onMove = { dx, dy ->
                layoutParams?.let { params ->
                    params.x += dx.toInt()
                    params.y += dy.toInt()
                    try {
                        windowManager.updateViewLayout(this.view, params)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            onCreate()
            onStart()
            onResume()
        }
        
        layoutParams = WindowManager.LayoutParams().apply {
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            format = PixelFormat.TRANSLUCENT
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 100
        }
        
        try {
            windowManager.addView(floatingView?.view, layoutParams)
            isShowing = true
        } catch (e: Exception) {
            e.printStackTrace()
            floatingView?.onDestroy()
            floatingView = null
            isShowing = false
        }
    }
    
    fun hide() {
        if (!isShowing) return
        
        try {
            floatingView?.onPause()
            floatingView?.onStop()
            floatingView?.view?.let {
                windowManager.removeView(it)
            }
            floatingView?.onDestroy()
            isShowing = false
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            floatingView = null
            layoutParams = null
        }
    }
    
    fun updateHeartRate(rate: Int) {
        floatingView?.updateHeartRate(rate)
    }
    
    fun updateRecordingState(recording: Boolean) {
        floatingView?.updateRecordingState(recording)
    }
    
    fun setOnClickListener(listener: () -> Unit) {
        floatingView?.setOnClickListener(listener)
    }
    
    fun isShowing(): Boolean = isShowing
    
    fun release() {
        hide()
    }
}

