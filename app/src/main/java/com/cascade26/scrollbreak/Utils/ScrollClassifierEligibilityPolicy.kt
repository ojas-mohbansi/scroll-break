package com.cascade26.scrollbreak.Utils

/** Keeps the optional scroll intervention out of focus and existing blocking flows. */
internal object ScrollClassifierEligibilityPolicy {
    @JvmStatic
    fun canRun(
        enabled: Boolean,
        trackedPackage: Boolean,
        focusRunning: Boolean,
        customBlocked: Boolean,
        packageBypassed: Boolean,
        graceActive: Boolean,
        blockerIntensity: Int,
        dailyLimitExceeded: Boolean,
        appRestricted: Boolean
    ): Boolean {
        if (!enabled || !trackedPackage || focusRunning || customBlocked || packageBypassed || graceActive) return false
        if (blockerIntensity == 3 && dailyLimitExceeded) return false
        return !appRestricted || blockerIntensity == 0
    }
}
