package com.cascade26.scrollbreak.Utils

/** Pure timing policy used after the service has detected rapid scrolling. */
internal object ScrollInterventionPolicy {
    enum class Decision { WAIT, SHOW, CANCEL_AS_INTENTIONAL }

    @JvmStatic
    fun decide(
        likelyLongForm: Boolean,
        stableLongFormContent: Boolean,
        elapsedSinceThresholdMs: Long,
        settledForMs: Long,
        settleWindowMs: Long = SETTLE_WINDOW_MS,
        maximumWaitMs: Long = ScrollVelocityClassifier.STABLE_CONTENT_MS
    ): Decision {
        if (likelyLongForm) {
            if (elapsedSinceThresholdMs < maximumWaitMs) return Decision.WAIT
            return if (stableLongFormContent) Decision.CANCEL_AS_INTENTIONAL else Decision.SHOW
        }
        return if (settledForMs >= settleWindowMs || elapsedSinceThresholdMs >= maximumWaitMs) {
            Decision.SHOW
        } else {
            Decision.WAIT
        }
    }

    @JvmStatic
    fun hasSemanticStoppingCue(
        fullyVisibleTargetItem: Boolean,
        toIndex: Int,
        itemCount: Int,
        scrollX: Int,
        maxScrollX: Int,
        scrollY: Int,
        maxScrollY: Int,
        previousVideoControlState: String?,
        currentVideoControlState: String?
    ): Boolean {
        if (fullyVisibleTargetItem) return true
        if (itemCount > 0 && toIndex >= itemCount - 1) return true
        if ((maxScrollY > 0 && scrollY >= maxScrollY)
            || (maxScrollX > 0 && scrollX >= maxScrollX)) return true
        return !previousVideoControlState.isNullOrEmpty()
            && !currentVideoControlState.isNullOrEmpty()
            && previousVideoControlState != currentVideoControlState
    }

    const val SETTLE_WINDOW_MS = 1_200L
}
