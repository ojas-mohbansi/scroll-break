package com.cascade26.scrollbreak.Utils

import android.content.SharedPreferences

/** Independent daily limits for one tracked app group (including its package variants). */
data class AppGroupDailyLimitSettings(
    val appTag: String,
    val timeEnabled: Boolean = false,
    val timeLimitMinutes: Int = DEFAULT_TIME_LIMIT_MINUTES,
    val scrollEnabled: Boolean = false,
    val scrollLimit: Int = DEFAULT_SCROLL_LIMIT
) {
    companion object {
        const val DEFAULT_TIME_LIMIT_MINUTES = 60
        const val DEFAULT_SCROLL_LIMIT = 100

        @JvmStatic
        fun defaults(appTag: String) = AppGroupDailyLimitSettings(appTag = appTag)
    }
}

/** Reads and writes only the new per-app keys; legacy global keys are intentionally ignored. */
object PerAppDailyLimitPreferences {
    data class AppGroup(val tag: String, val displayName: String)

    @JvmField
    val APP_GROUPS = listOf(
        AppGroup("insta", "Instagram"),
        AppGroup("yt", "YouTube"),
        AppGroup("snap", "Snapchat"),
        AppGroup("facebook", "Facebook"),
        AppGroup("linkedin", "LinkedIn"),
        AppGroup("reddit", "Reddit"),
        AppGroup("tiktok", "TikTok"),
        AppGroup("twitter", "X")
    )

    private const val KEY_PREFIX = "pref_temp_lock_app_"

    @JvmStatic
    fun settings(preferences: SharedPreferences, appTag: String): AppGroupDailyLimitSettings =
        AppGroupDailyLimitSettings(
            appTag = appTag,
            timeEnabled = preferences.getBoolean(key(appTag, "time_enabled"), false),
            timeLimitMinutes = preferences.getInt(
                key(appTag, "time_minutes"),
                AppGroupDailyLimitSettings.DEFAULT_TIME_LIMIT_MINUTES
            ).coerceIn(1, 240),
            scrollEnabled = preferences.getBoolean(key(appTag, "scroll_enabled"), false),
            scrollLimit = preferences.getInt(
                key(appTag, "scroll_count"),
                AppGroupDailyLimitSettings.DEFAULT_SCROLL_LIMIT
            ).coerceIn(10, 500)
        )

    @JvmStatic
    fun save(preferences: SharedPreferences, settings: AppGroupDailyLimitSettings) {
        preferences.edit()
            .putBoolean(key(settings.appTag, "time_enabled"), settings.timeEnabled)
            .putInt(key(settings.appTag, "time_minutes"), settings.timeLimitMinutes.coerceIn(1, 240))
            .putBoolean(key(settings.appTag, "scroll_enabled"), settings.scrollEnabled)
            .putInt(key(settings.appTag, "scroll_count"), settings.scrollLimit.coerceIn(10, 500))
            .apply()
    }

    private fun key(appTag: String, suffix: String) = "${KEY_PREFIX}${appTag}_${suffix}"
}
