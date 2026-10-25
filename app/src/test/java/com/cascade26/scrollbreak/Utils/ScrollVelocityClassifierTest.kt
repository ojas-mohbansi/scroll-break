package com.cascade26.scrollbreak.Utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScrollVelocityClassifierTest {
    @Test
    fun triggersAfterThirtyScrollsWithinRollingMinute() {
        val classifier = ScrollVelocityClassifier()
        repeat(ScrollVelocityClassifier.THRESHOLD_SCROLLS - 1) { index ->
            assertFalse(classifier.recordScroll(1_000L + index * 1_000L))
        }

        assertTrue(classifier.recordScroll(30_000L))
        assertEquals(ScrollVelocityClassifier.THRESHOLD_SCROLLS, classifier.scrollCount(30_000L))
    }

    @Test
    fun expiredScrollsFallOutOfRollingMinute() {
        val classifier = ScrollVelocityClassifier()
        repeat(ScrollVelocityClassifier.THRESHOLD_SCROLLS) { index ->
            classifier.recordScroll(1_000L + index * 1_000L)
        }

        assertEquals(29, classifier.scrollCount(61_001L))
        assertTrue(classifier.recordScroll(61_001L))
        assertEquals(30, classifier.scrollCount(61_001L))
    }

    @Test
    fun stableContentClearsAccumulatedScrollsAfterTwentySeconds() {
        val classifier = ScrollVelocityClassifier()
        repeat(10) { classifier.recordScroll(1_000L + it) }

        assertFalse(classifier.observeContent("collection:item-4", 1_000L))
        assertFalse(classifier.observeContent("collection:item-4", 20_999L))
        assertTrue(classifier.observeContent("collection:item-4", 21_000L))
        assertEquals(0, classifier.scrollCount(21_000L))
    }

    @Test
    fun resetClearsWindowAndStableContentTracking() {
        val classifier = ScrollVelocityClassifier()
        repeat(ScrollVelocityClassifier.THRESHOLD_SCROLLS) { classifier.recordScroll(1_000L + it) }
        classifier.observeContent("collection:item-4", 1_000L)

        classifier.reset()

        assertEquals(0, classifier.scrollCount(2_000L))
        assertFalse(classifier.observeContent("collection:item-4", 21_000L))
        assertFalse(classifier.recordScroll(21_000L))
    }
}

class ScrollInterventionPolicyTest {
    @Test
    fun ordinaryContentShowsAfterScrollSettles() {
        assertEquals(
            ScrollInterventionPolicy.Decision.SHOW,
            ScrollInterventionPolicy.decide(
                likelyLongForm = false,
                stableLongFormContent = false,
                elapsedSinceThresholdMs = 2_000L,
                settledForMs = ScrollInterventionPolicy.SETTLE_WINDOW_MS
            )
        )
    }

    @Test
    fun longFormContentWaitsFullPauseAndCancelsWhenStable() {
        assertEquals(
            ScrollInterventionPolicy.Decision.WAIT,
            ScrollInterventionPolicy.decide(
                likelyLongForm = true,
                stableLongFormContent = true,
                elapsedSinceThresholdMs = 5_000L,
                settledForMs = 5_000L
            )
        )
        assertEquals(
            ScrollInterventionPolicy.Decision.CANCEL_AS_INTENTIONAL,
            ScrollInterventionPolicy.decide(
                likelyLongForm = true,
                stableLongFormContent = true,
                elapsedSinceThresholdMs = ScrollVelocityClassifier.STABLE_CONTENT_MS,
                settledForMs = ScrollVelocityClassifier.STABLE_CONTENT_MS
            )
        )
    }

    @Test
    fun longFormScrollingStillTriggersAtMaximumWait() {
        assertEquals(
            ScrollInterventionPolicy.Decision.SHOW,
            ScrollInterventionPolicy.decide(
                likelyLongForm = true,
                stableLongFormContent = false,
                elapsedSinceThresholdMs = ScrollVelocityClassifier.STABLE_CONTENT_MS,
                settledForMs = 0L
            )
        )
    }

    @Test
    fun semanticCuesIncludeVisibleItemListEndAndScrollEnd() {
        assertTrue(ScrollInterventionPolicy.hasSemanticStoppingCue(
            fullyVisibleTargetItem = true,
            toIndex = 3, itemCount = 10,
            scrollX = 0, maxScrollX = 0, scrollY = 200, maxScrollY = 500,
            previousVideoControlState = null, currentVideoControlState = null
        ))
        assertTrue(ScrollInterventionPolicy.hasSemanticStoppingCue(
            fullyVisibleTargetItem = false,
            toIndex = 9, itemCount = 10,
            scrollX = 0, maxScrollX = 0, scrollY = 100, maxScrollY = 500,
            previousVideoControlState = null, currentVideoControlState = null
        ))
        assertTrue(ScrollInterventionPolicy.hasSemanticStoppingCue(
            fullyVisibleTargetItem = false,
            toIndex = 2, itemCount = 10,
            scrollX = 0, maxScrollX = 0, scrollY = 500, maxScrollY = 500,
            previousVideoControlState = null, currentVideoControlState = null
        ))
    }

    @Test
    fun semanticCuesIncludeVideoStateChangeButIgnoreUnchangedState() {
        assertTrue(ScrollInterventionPolicy.hasSemanticStoppingCue(
            fullyVisibleTargetItem = false,
            toIndex = -1, itemCount = 0,
            scrollX = 0, maxScrollX = 0, scrollY = 0, maxScrollY = 0,
            previousVideoControlState = "play=false", currentVideoControlState = "play=true"
        ))
        assertFalse(ScrollInterventionPolicy.hasSemanticStoppingCue(
            fullyVisibleTargetItem = false,
            toIndex = -1, itemCount = 0,
            scrollX = 0, maxScrollX = 0, scrollY = 0, maxScrollY = 0,
            previousVideoControlState = "play=true", currentVideoControlState = "play=true"
        ))
    }
}

class ScrollClassifierEligibilityPolicyTest {
    @Test
    fun doesNotRunDuringLockInOrWhenOptInIsOff() {
        assertFalse(ScrollClassifierEligibilityPolicy.canRun(
            enabled = true, trackedPackage = true, focusRunning = true,
            customBlocked = false, packageBypassed = false, graceActive = false,
            blockerIntensity = 0, dailyLimitExceeded = false, appRestricted = false
        ))
        assertFalse(ScrollClassifierEligibilityPolicy.canRun(
            enabled = false, trackedPackage = true, focusRunning = false,
            customBlocked = false, packageBypassed = false, graceActive = false,
            blockerIntensity = 0, dailyLimitExceeded = false, appRestricted = false
        ))
    }

    @Test
    fun leavesExistingRestrictedAndDailyLimitBlockersInControl() {
        assertFalse(ScrollClassifierEligibilityPolicy.canRun(
            enabled = true, trackedPackage = true, focusRunning = false,
            customBlocked = false, packageBypassed = false, graceActive = false,
            blockerIntensity = 2, dailyLimitExceeded = false, appRestricted = true
        ))
        assertFalse(ScrollClassifierEligibilityPolicy.canRun(
            enabled = true, trackedPackage = true, focusRunning = false,
            customBlocked = false, packageBypassed = false, graceActive = false,
            blockerIntensity = 3, dailyLimitExceeded = true, appRestricted = false
        ))
    }

    @Test
    fun allowsOptedInUnblockedTrackedApps() {
        assertTrue(ScrollClassifierEligibilityPolicy.canRun(
            enabled = true, trackedPackage = true, focusRunning = false,
            customBlocked = false, packageBypassed = false, graceActive = false,
            blockerIntensity = 0, dailyLimitExceeded = false, appRestricted = false
        ))
    }
}
