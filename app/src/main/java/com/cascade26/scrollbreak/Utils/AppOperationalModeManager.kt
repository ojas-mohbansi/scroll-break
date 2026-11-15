package com.cascade26.scrollbreak.Utils

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

/**
 * Manages ScrollBreak's dual operational modes:
 *
 * - LEVEL_1_BASIC_WELLBEING: UsageStatsManager-based time tracking, daily limits, focus sessions.
 *   Does NOT require AccessibilityService. 100% Banking App Compatible.
 *
 * - LEVEL_2_ADVANCED_ACCESSIBILITY: AccessibilityService-based scroll velocity tracking, reel detection,
 *   and browser URL blocking with dynamic package filtering and SensitiveAppPolicy.
 */
object AppOperationalModeManager {

    enum class OperationalMode {
        LEVEL_1_BASIC_WELLBEING,
        LEVEL_2_ADVANCED_ACCESSIBILITY
    }

    const val PREF_OPERATIONAL_MODE = "pref_operational_mode"

    private fun getPrefs(context: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }

    fun getOperationalMode(context: Context): OperationalMode {
        val modeStr = getPrefs(context).getString(PREF_OPERATIONAL_MODE, OperationalMode.LEVEL_2_ADVANCED_ACCESSIBILITY.name)
        return try {
            OperationalMode.valueOf(modeStr ?: OperationalMode.LEVEL_2_ADVANCED_ACCESSIBILITY.name)
        } catch (e: Exception) {
            OperationalMode.LEVEL_2_ADVANCED_ACCESSIBILITY
        }
    }

    fun setOperationalMode(context: Context, mode: OperationalMode) {
        getPrefs(context).edit().putString(PREF_OPERATIONAL_MODE, mode.name).apply()
    }

    fun isLevel1Active(context: Context): Boolean {
        return getOperationalMode(context) == OperationalMode.LEVEL_1_BASIC_WELLBEING
    }

    fun isLevel2Active(context: Context): Boolean {
        return getOperationalMode(context) == OperationalMode.LEVEL_2_ADVANCED_ACCESSIBILITY
    }
}
