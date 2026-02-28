package com.apptimer.service

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import com.apptimer.R
import com.apptimer.ui.CircularProgressView

class OverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var progressView: CircularProgressView? = null

    private var isShowing = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        android.util.Log.d("OverlayService", "onStartCommand: action=${intent?.action}")
        when (intent?.action) {
            "UPDATE_OVERLAY" -> {
                val remainingTime = intent.getLongExtra("remainingTime", 0)
                val totalTime = intent.getLongExtra("totalTime", 1)
                android.util.Log.d("OverlayService", "UPDATE_OVERLAY: remaining=$remainingTime, total=$totalTime")
                updateProgress(remainingTime, totalTime)
            }
            "HIDE_OVERLAY" -> {
                android.util.Log.d("OverlayService", "HIDE_OVERLAY")
                hideOverlay()
            }
            else -> {
                android.util.Log.d("OverlayService", "showOverlay")
                showOverlay()
            }
        }
        return START_STICKY
    }

    private var params: WindowManager.LayoutParams? = null

    private fun showOverlay() {
        android.util.Log.d("OverlayService", "showOverlay: isShowing=$isShowing")
        if (isShowing && overlayView != null) return

        // 如果之前有view，先尝试移除
        hideOverlay()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_timer, null)
        progressView = overlayView?.findViewById(R.id.progressView)

        android.util.Log.d("OverlayService", "overlayView=$overlayView, progressView=$progressView")

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 20
            y = 100
        }

        // 添加拖动功能
        overlayView?.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params?.x ?: 0
                        initialY = params?.y ?: 0
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params?.x = initialX + (initialTouchX - event.rawX).toInt()
                        params?.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager?.updateViewLayout(overlayView, params)
                        return true
                    }
                }
                return false
            }
        })

        try {
            windowManager?.addView(overlayView, params)
            isShowing = true
            android.util.Log.d("OverlayService", "Overlay added successfully")
        } catch (e: Exception) {
            android.util.Log.e("OverlayService", "Failed to add overlay", e)
            isShowing = false
            e.printStackTrace()
        }
    }

    private fun hideOverlay() {
        if (overlayView != null) {
            try {
                windowManager?.removeView(overlayView)
            } catch (e: Exception) {
                // 可能已经不存在
            }
        }
        overlayView = null
        progressView = null
        isShowing = false
    }

    private fun updateProgress(remainingTime: Long, totalTime: Long) {
        android.util.Log.d("OverlayService", "updateProgress: remaining=$remainingTime, total=$totalTime, isShowing=$isShowing")

        // 每次都重新获取 view 引用，确保使用最新的
        progressView = overlayView?.findViewById(R.id.progressView)

        if (!isShowing || overlayView == null) {
            showOverlay()
        }

        // 让圆环自己处理倒计时动画
        progressView?.startCountdown(remainingTime, totalTime)
        android.util.Log.d("OverlayService", "startCountdown called, progressView=$progressView")
    }

    override fun onDestroy() {
        super.onDestroy()
        hideOverlay()
    }
}
