package com.apptimer.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.apptimer.MainActivity
import com.apptimer.database.AppDatabase
import com.apptimer.database.entities.UsageHistory
import com.apptimer.ui.TimeoutDialog
import com.apptimer.utils.AppUsageHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar

class MonitorService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var monitorJob: Job? = null
    private lateinit var database: AppDatabase
    private val myPackageName by lazy { packageName }

    private var currentApp: String? = null
    private var sessionStartTime: Long = 0
    private var isOverlayShowing = false
    private var lastMonitoredApp: String? = null
    private var isDialogShowing = false

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "monitor_service_channel"
        private const val CHECK_INTERVAL = 500L

        const val ACTION_REFRESH = "com.apptimer.REFRESH_LIMITS"
        const val ACTION_DIALOG_DISMISSED = "com.apptimer.DIALOG_DISMISSED"

        const val PREFS_NAME = "app_timer_prefs"
        const val KEY_TIME_PERIOD = "time_period_hours"
        const val KEY_PERIOD_START_TIME = "period_start_time"

        fun start(context: Context) {
            val intent = Intent(context, MonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, MonitorService::class.java))
        }

        fun refresh(context: Context) {
            val intent = Intent(context, MonitorService::class.java).apply {
                action = ACTION_REFRESH
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getDatabase(this)
        createNotificationChannel()
        runCatching {
            startForeground(NOTIFICATION_ID, createNotification())
        }.onFailure { error ->
            android.util.Log.e("MonitorService", "startForeground failed", error)
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_REFRESH -> {
                isDialogShowing = false
                serviceScope.launch { safeCheckCurrentApp() }
            }
            ACTION_DIALOG_DISMISSED -> isDialogShowing = false
            else -> startMonitoring()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopMonitoring()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startMonitoring() {
        monitorJob?.cancel()
        monitorJob = serviceScope.launch {
            safeCheckCurrentApp()
            while (isActive) {
                safeCheckCurrentApp()
                delay(CHECK_INTERVAL)
            }
        }
    }

    private suspend fun safeCheckCurrentApp() {
        runCatching {
            checkCurrentApp()
        }.onFailure { error ->
            android.util.Log.e("MonitorService", "checkCurrentApp failed", error)
            handleLeaveLimitedApp()
        }
    }

    private fun stopMonitoring() {
        monitorJob?.cancel()
        saveCurrentSession()
        hideOverlayIfNeeded()
    }

    private suspend fun checkCurrentApp() {
        val foregroundApp = AppUsageHelper.getCurrentForegroundApp(this)

        if (foregroundApp.isNullOrBlank()) {
            if (currentApp != null) {
                processLimitedApp(currentApp!!)
            }
            return
        }

        if (foregroundApp == myPackageName) {
            handleLeaveLimitedApp()
            return
        }

        if (currentApp != null && currentApp != foregroundApp) {
            saveCurrentSession()
        }
        processLimitedApp(foregroundApp)
    }

    private suspend fun processLimitedApp(packageName: String) {
        val limit = database.appLimitDao().getLimitByPackage(packageName)
        if (limit == null || !limit.enabled || limit.timeLimit <= 0) {
            if (currentApp != null) {
                handleLeaveLimitedApp()
            }
            return
        }

        if (currentApp != packageName) {
            currentApp = packageName
            sessionStartTime = System.currentTimeMillis()
        }

        val periodHours = getPeriodHours()
        val periodStart = getCurrentPeriodStart(periodHours)
        val periodUsage = getPeriodUsage(packageName, periodStart)
        val remainingTime = limit.timeLimit - periodUsage

        if (remainingTime > 0 && isDialogShowing) {
            isDialogShowing = false
        }

        if (!isOverlayShowing || lastMonitoredApp != packageName) {
            updateOverlay(packageName, remainingTime, limit.timeLimit)
            isOverlayShowing = true
            lastMonitoredApp = packageName
        } else {
            updateOverlay(packageName, remainingTime, limit.timeLimit)
        }

        updateNotification(
            "Monitoring ${limit.appName}: ${remainingTime.coerceAtLeast(0) / 60}m left"
        )

        val appStableForMs = System.currentTimeMillis() - sessionStartTime
        if (remainingTime <= 0 && !isDialogShowing && appStableForMs >= 3_000) {
            isDialogShowing = true
            hideOverlayIfNeeded()
            showTimeoutDialog(
                packageName = packageName,
                periodStartTime = periodStart,
                periodHours = periodHours,
                periodUsageSeconds = periodUsage,
                remainingTimeSeconds = remainingTime
            )
        }
    }

    private fun handleLeaveLimitedApp() {
        if (currentApp != null) {
            saveCurrentSession()
        }
        isDialogShowing = false
        hideOverlayIfNeeded()
        lastMonitoredApp = null
    }

    private fun getPeriodHours(): Int {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_TIME_PERIOD, 1).coerceAtLeast(1)
    }

    private fun getCurrentPeriodStart(periodHours: Int): Long {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val periodMillis = periodHours * 60L * 60L * 1000L
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dayStart = calendar.timeInMillis
        val elapsed = (now - dayStart).coerceAtLeast(0L)
        val periodIndex = elapsed / periodMillis
        val periodStart = dayStart + periodIndex * periodMillis

        prefs.edit().putLong(KEY_PERIOD_START_TIME, periodStart).apply()
        return periodStart
    }

    private suspend fun getPeriodUsage(packageName: String, periodStart: Long): Long {
        val dbUsage = database.usageHistoryDao().getTotalDurationSince(packageName, periodStart) ?: 0L
        val currentSessionDuration = if (currentApp == packageName && sessionStartTime > 0) {
            (System.currentTimeMillis() - sessionStartTime) / 1000
        } else {
            0L
        }
        return dbUsage + currentSessionDuration
    }

    private fun saveCurrentSession() {
        val app = currentApp ?: return
        val startTime = sessionStartTime
        if (startTime <= 0) return

        val endTime = System.currentTimeMillis()
        val duration = (endTime - startTime) / 1000

        currentApp = null
        sessionStartTime = 0

        if (duration <= 0) {
            return
        }

        val history = UsageHistory(
            packageName = app,
            startTime = startTime,
            endTime = endTime,
            duration = duration
        )

        serviceScope.launch {
            runCatching {
                database.usageHistoryDao().insertHistory(history)
            }
        }
    }

    private fun updateOverlay(packageName: String, remainingTime: Long, totalTime: Long) {
        val intent = Intent(this, OverlayService::class.java).apply {
            action = "UPDATE_OVERLAY"
            putExtra("packageName", packageName)
            putExtra("remainingTime", remainingTime)
            putExtra("totalTime", totalTime)
        }
        startService(intent)
    }

    private fun hideOverlayIfNeeded() {
        if (!isOverlayShowing) return
        val intent = Intent(this, OverlayService::class.java).apply {
            action = "HIDE_OVERLAY"
        }
        startService(intent)
        isOverlayShowing = false
    }

    private fun showTimeoutDialog(
        packageName: String,
        periodStartTime: Long,
        periodHours: Int,
        periodUsageSeconds: Long,
        remainingTimeSeconds: Long
    ) {
        serviceScope.launch {
            val limit = database.appLimitDao().getLimitByPackage(packageName)
            val timeLimitSeconds = limit?.timeLimit ?: 0L
            val periodEndTime = periodStartTime + periodHours * 60L * 60L * 1000L

            val intent = Intent(this@MonitorService, TimeoutDialog::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra("packageName", packageName)
                putExtra("periodHours", periodHours)
                putExtra("timeLimitSeconds", timeLimitSeconds)
                putExtra("periodStartTime", periodStartTime)
                putExtra("periodEndTime", periodEndTime)
                putExtra("periodUsageSeconds", periodUsageSeconds)
                putExtra("remainingTimeSeconds", remainingTimeSeconds)
            }
            startActivity(intent)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "App Monitor",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors app usage time"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("App monitor running")
            .setContentText("Monitoring limited apps")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("App monitor running")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
