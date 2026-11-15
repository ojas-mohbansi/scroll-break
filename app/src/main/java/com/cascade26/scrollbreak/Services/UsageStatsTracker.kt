package com.cascade26.scrollbreak.Services

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process
import java.util.Calendar

/**
 * Provides foreground package tracking and usage time calculations using Android's
 * UsageStatsManager API. Enables Level 1 (Basic Digital Wellbeing) functionality
 * without requiring AccessibilityService to be enabled.
 */
object UsageStatsTracker {

    /**
     * Checks if PACKAGE_USAGE_STATS permission is granted by the user.
     */
    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Retrieves the current foreground package name using UsageEvents.
     */
    fun getForegroundPackageName(context: Context): String? {
        if (!hasUsageStatsPermission(context)) return null

        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return null
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 10_000L // Look back 10 seconds

        val usageEvents = usm.queryEvents(startTime, endTime) ?: return null
        val event = UsageEvents.Event()
        var lastForegroundPkg: String? = null
        var lastEventTime: Long = 0

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND ||
                event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                if (event.timeStamp >= lastEventTime) {
                    lastForegroundPkg = event.packageName
                    lastEventTime = event.timeStamp
                }
            }
        }
        return lastForegroundPkg
    }

    /**
     * Calculates the total usage duration (in milliseconds) for a package today (since 00:00).
     */
    fun getTodayAppUsageDurationMs(context: Context, packageName: String): Long {
        return getAllAppUsageDurationsToday(context)[packageName] ?: 0L
    }

    /**
     * Queries and computes total usage duration for all apps today (since 00:00).
     */
    fun getAllAppUsageDurationsToday(context: Context): Map<String, Long> {
        if (!hasUsageStatsPermission(context)) return emptyMap()

        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return emptyMap()
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        val usageStatsList = usm.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        ) ?: return emptyMap()

        val usageMap = HashMap<String, Long>()
        for (stat in usageStatsList) {
            val totalTime = stat.totalTimeInForeground
            if (totalTime > 0) {
                val current = usageMap[stat.packageName] ?: 0L
                usageMap[stat.packageName] = current + totalTime
            }
        }
        return usageMap
    }
}
