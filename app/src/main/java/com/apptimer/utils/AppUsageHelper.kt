package com.apptimer.utils

import android.app.ActivityManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build

object AppUsageHelper {

    fun isAppInForeground(context: Context, targetPackageName: String): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningProcesses = activityManager.runningAppProcesses ?: return false
        return runningProcesses.any { process ->
            process.processName == targetPackageName &&
                process.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
        }
    }

    fun getCurrentForegroundApp(context: Context): String? {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 60_000
        val homePackage = getHomePackage(context)

        val events = usageStatsManager.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()
        var lastForegroundPackage: String? = null
        var lastEventTime = 0L

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val isForegroundEvent = event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                    event.eventType == UsageEvents.Event.ACTIVITY_RESUMED)

            if (
                isForegroundEvent &&
                !event.packageName.isNullOrBlank() &&
                event.timeStamp >= lastEventTime
            ) {
                lastForegroundPackage = event.packageName
                lastEventTime = event.timeStamp
            }
        }

        if (
            !lastForegroundPackage.isNullOrBlank() &&
            lastForegroundPackage != homePackage &&
            endTime - lastEventTime <= 5_000
        ) {
            return lastForegroundPackage
        }

        val usageStats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_BEST,
            startTime,
            endTime
        )

        val latestUsage = usageStats
            ?.filter { !it.packageName.isNullOrBlank() && it.packageName != homePackage }
            ?.maxByOrNull { it.lastTimeUsed }

        if (latestUsage == null || endTime - latestUsage.lastTimeUsed > 5_000) {
            return null
        }

        return latestUsage.packageName
    }

    private fun getHomePackage(context: Context): String? {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        return context.packageManager.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo
            ?.packageName
    }

    fun getAppName(context: Context, packageName: String): String {
        return try {
            val packageManager = context.packageManager
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }

    fun getInstalledUserApps(context: Context): List<ApplicationInfo> {
        val packageManager = context.packageManager
        val apps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        return apps.filter {
            (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 &&
                it.packageName != context.packageName
        }
    }
}
