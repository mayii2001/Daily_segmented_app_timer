package com.apptimer.ui

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import com.apptimer.R

class CircularProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var progress = 100f // 0-100，表示剩余时间百分比
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rectF = RectF()

    private val strokeWidth = 20f

    // 动画相关
    private var remainingSeconds: Long = 0
    private var totalSeconds: Long = 0
    private val handler = Handler(Looper.getMainLooper())
    private var animationRunnable: Runnable? = null

    init {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = strokeWidth
        paint.strokeCap = Paint.Cap.ROUND

        backgroundPaint.style = Paint.Style.STROKE
        backgroundPaint.strokeWidth = strokeWidth
        backgroundPaint.color = Color.parseColor("#33FFFFFF")
    }

    /**
     * 设置剩余时间并启动动画
     */
    fun startCountdown(remainingTime: Long, totalTime: Long) {
        // 停止之前的动画
        stopCountdown()

        remainingSeconds = remainingTime
        totalSeconds = totalTime

        // 计算初始进度
        progress = if (totalSeconds > 0) {
            (remainingSeconds.toFloat() / totalSeconds.toFloat() * 100f).coerceIn(0f, 100f)
        } else {
            0f
        }

        // 启动倒计时动画
        animationRunnable = object : Runnable {
            override fun run() {
                if (remainingSeconds > 0) {
                    remainingSeconds--
                    progress = if (totalSeconds > 0) {
                        (remainingSeconds.toFloat() / totalSeconds.toFloat() * 100f).coerceIn(0f, 100f)
                    } else {
                        0f
                    }
                    invalidate()
                    handler.postDelayed(this, 1000) // 每秒更新一次
                }
            }
        }
        handler.post(animationRunnable!!)
        invalidate()
    }

    /**
     * 停止倒计时动画
     */
    fun stopCountdown() {
        animationRunnable?.let {
            handler.removeCallbacks(it)
            animationRunnable = null
        }
    }

    fun setProgress(percentage: Float) {
        progress = percentage.coerceIn(0f, 100f)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f
        val radius = (Math.min(width, height) / 2f) - strokeWidth

        rectF.set(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius
        )

        // 绘制背景圆环
        canvas.drawArc(rectF, 0f, 360f, false, backgroundPaint)

        // 根据进度设置颜色
        paint.color = when {
            progress > 50 -> context.getColor(R.color.progress_green)
            progress > 20 -> context.getColor(R.color.progress_yellow)
            else -> context.getColor(R.color.progress_red)
        }

        // 绘制进度弧（从顶部开始，顺时针）
        val sweepAngle = (progress / 100f) * 360f
        canvas.drawArc(rectF, -90f, sweepAngle, false, paint)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = 120 // 默认大小
        setMeasuredDimension(size, size)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopCountdown()
    }
}
