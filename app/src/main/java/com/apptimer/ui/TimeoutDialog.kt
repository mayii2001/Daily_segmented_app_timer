package com.apptimer.ui

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.apptimer.R
import com.apptimer.database.AppDatabase
import com.apptimer.service.MonitorService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import java.text.SimpleDateFormat
import java.util.Date

class TimeoutDialog : AppCompatActivity() {

    private lateinit var database: AppDatabase
    private var packageName: String? = null
    private var timeLimitSeconds: Long = 0L
    private var periodStartTime: Long = 0L
    private var periodEndTime: Long = 0L
    private var periodUsageSeconds: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_timeout)

        database = AppDatabase.getDatabase(this)
        packageName = intent.getStringExtra("packageName")
        val periodHours = intent.getIntExtra("periodHours", 1).coerceAtLeast(1)
        timeLimitSeconds = intent.getLongExtra("timeLimitSeconds", 0L)
        periodStartTime = intent.getLongExtra("periodStartTime", 0L)
        periodEndTime = intent.getLongExtra("periodEndTime", 0L)
        periodUsageSeconds = intent.getLongExtra("periodUsageSeconds", 0L).coerceAtLeast(0L)

        if (periodStartTime <= 0L || periodEndTime <= periodStartTime) {
            periodEndTime = System.currentTimeMillis()
            periodStartTime = periodEndTime - periodHours * 60L * 60L * 1000L
        }

        val messageText = findViewById<TextView>(R.id.messageText)
        messageText.text =
            "在 ${formatTime(periodStartTime)} - ${formatTime(periodEndTime)} 时段内，限制时长 ${formatLimitMinutes(timeLimitSeconds)} 分钟已到"

        findViewById<Button>(R.id.gotItButton).setOnClickListener {
            goHome()
        }

        findViewById<Button>(R.id.extendButton).setOnClickListener {
            extendTime()
        }
    }

    private fun formatTime(timestamp: Long): String {
        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        return formatter.format(Date(timestamp))
    }

    private fun formatLimitMinutes(limitSeconds: Long): String {
        val minutes = limitSeconds / 60.0
        return if (minutes % 1.0 == 0.0) {
            minutes.toInt().toString()
        } else {
            String.format(Locale.US, "%.1f", minutes)
        }
    }

    private fun goHome() {
        sendDialogDismissedSignal()
        finish()
    }

    private fun extendTime() {
        packageName?.let { pkg ->
            CoroutineScope(Dispatchers.IO).launch {
                val currentLimit = database.appLimitDao().getLimitByPackage(pkg)
                currentLimit?.let {
                    val newLimit = periodUsageSeconds + 300L
                    database.appLimitDao().updateLimit(it.copy(timeLimit = newLimit))
                }

                withContext(Dispatchers.Main) {
                    MonitorService.refresh(this@TimeoutDialog)
                    sendDialogDismissedSignal()
                    finish()
                }
            }
        } ?: run {
            sendDialogDismissedSignal()
            finish()
        }
    }

    private fun sendDialogDismissedSignal() {
        val intent = android.content.Intent(this, MonitorService::class.java).apply {
            action = MonitorService.ACTION_DIALOG_DISMISSED
        }
        startService(intent)
    }

    override fun onBackPressed() {
        // blocked
    }
}
