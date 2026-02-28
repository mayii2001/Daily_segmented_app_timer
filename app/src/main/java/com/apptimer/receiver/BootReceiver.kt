package com.apptimer.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.apptimer.service.MonitorService

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // 检查用户是否启用了监控
            val prefs = context.getSharedPreferences("app_timer_prefs", Context.MODE_PRIVATE)
            val isMonitoringEnabled = prefs.getBoolean("monitoring_enabled", true) // 默认开启

            if (isMonitoringEnabled) {
                // 开机后自动启动监控服务
                MonitorService.start(context)
            }
        }
    }
}
