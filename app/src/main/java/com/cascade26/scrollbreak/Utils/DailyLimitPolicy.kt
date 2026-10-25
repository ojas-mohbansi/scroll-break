package com.cascade26.scrollbreak.Utils

/** Pure threshold rules shared by daily usage enforcement and unit tests. */
object DailyLimitPolicy {
    @JvmStatic
    fun isTimeLimitExceeded(recordedSeconds: Long, activeSessionSeconds: Long, limitSeconds: Long): Boolean {
        if (limitSeconds <= 0L) return false
        val total = (recordedSeconds.coerceAtLeast(0L) + activeSessionSeconds.coerceAtLeast(0L))
        return total >= limitSeconds
    }

    @JvmStatic
    fun isScrollLimitExceeded(appScrolls: Long, limit: Long): Boolean =
        limit > 0L && appScrolls >= limit

    @JvmStatic
    fun isAppGroupTimeLimitExceeded(
        appTag: String,
        packageToAppTag: Map<String, String>,
        recordedSecondsByPackage: Map<String, Long>,
        activePackage: String?,
        activeSessionSeconds: Long,
        limitSeconds: Long
    ): Boolean {
        if (limitSeconds <= 0L) return false
        val recordedSeconds = recordedSecondsByPackage.entries
            .asSequence()
            .filter { packageToAppTag[it.key] == appTag }
            .sumOf { it.value.coerceAtLeast(0L) }
        val activeSeconds = if (activePackage != null && packageToAppTag[activePackage] == appTag) {
            activeSessionSeconds.coerceAtLeast(0L)
        } else 0L
        return isTimeLimitExceeded(recordedSeconds, activeSeconds, limitSeconds)
    }

    @JvmStatic
    fun isAppGroupScrollLimitExceeded(
        appTag: String,
        packageToAppTag: Map<String, String>,
        scrollCountsByPackage: Map<String, Long>,
        limit: Long
    ): Boolean {
        if (limit <= 0L) return false
        val groupScrolls = scrollCountsByPackage.entries
            .asSequence()
            .filter { packageToAppTag[it.key] == appTag }
            .sumOf { it.value.coerceAtLeast(0L) }
        return isScrollLimitExceeded(groupScrolls, limit)
    }

    @JvmStatic
    fun isAppGroupLimitExceeded(
        settings: AppGroupDailyLimitSettings,
        packageToAppTag: Map<String, String>,
        recordedSecondsByPackage: Map<String, Long>,
        scrollCountsByPackage: Map<String, Long>,
        activePackage: String?,
        activeSessionSeconds: Long
    ): Boolean {
        val timeExceeded = settings.timeEnabled && isAppGroupTimeLimitExceeded(
            settings.appTag,
            packageToAppTag,
            recordedSecondsByPackage,
            activePackage,
            activeSessionSeconds,
            settings.timeLimitMinutes.coerceIn(1, 240).toLong() * 60L
        )
        val scrollExceeded = settings.scrollEnabled && isAppGroupScrollLimitExceeded(
            settings.appTag,
            packageToAppTag,
            scrollCountsByPackage,
            settings.scrollLimit.coerceIn(10, 500).toLong()
        )
        return timeExceeded || scrollExceeded
    }
}
