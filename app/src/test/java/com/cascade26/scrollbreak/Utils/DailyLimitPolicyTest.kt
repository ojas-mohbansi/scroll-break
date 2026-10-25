package com.cascade26.scrollbreak.Utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyLimitPolicyTest {
    @Test
    fun timeLimitIncludesCurrentUnflushedSessionAndTriggersAtBoundary() {
        assertFalse(DailyLimitPolicy.isTimeLimitExceeded(3_599, 0, 3_600))
        assertTrue(DailyLimitPolicy.isTimeLimitExceeded(3_599, 1, 3_600))
        assertTrue(DailyLimitPolicy.isTimeLimitExceeded(3_600, 1, 3_600))
    }

    @Test
    fun scrollLimitIsPerAppValueAndTriggersAtBoundary() {
        assertFalse(DailyLimitPolicy.isScrollLimitExceeded(99, 100))
        assertTrue(DailyLimitPolicy.isScrollLimitExceeded(100, 100))
        assertTrue(DailyLimitPolicy.isScrollLimitExceeded(101, 100))
    }

    @Test
    fun zeroThresholdDoesNotImmediatelyLockOut() {
        assertFalse(DailyLimitPolicy.isTimeLimitExceeded(10, 10, 0))
        assertFalse(DailyLimitPolicy.isScrollLimitExceeded(10, 0))
    }

    @Test
    fun perAppSettingsStartDisabledWithExistingDefaultThresholds() {
        val settings = AppGroupDailyLimitSettings.defaults("insta")

        assertFalse(settings.timeEnabled)
        assertFalse(settings.scrollEnabled)
        assertEquals(60, settings.timeLimitMinutes)
        assertEquals(100, settings.scrollLimit)
    }

    @Test
    fun catalogContainsExactlyTheEightExistingAppGroups() {
        assertEquals(
            listOf("insta", "yt", "snap", "facebook", "linkedin", "reddit", "tiktok", "twitter"),
            PerAppDailyLimitPreferences.APP_GROUPS.map { it.tag }
        )
    }

    @Test
    fun variantPackagesShareTimeAndScrollLimitsWithinTheirGroup() {
        val settings = AppGroupDailyLimitSettings(
            appTag = "reddit",
            timeEnabled = true,
            timeLimitMinutes = 60,
            scrollEnabled = true,
            scrollLimit = 100
        )
        val groups = mapOf(
            "com.reddit.frontpage" to "reddit",
            "ml.docilealligator.infinityforreddit" to "reddit",
            "com.instagram.android" to "insta"
        )

        assertTrue(DailyLimitPolicy.isAppGroupLimitExceeded(
            settings.copy(scrollEnabled = false),
            groups,
            mapOf("com.reddit.frontpage" to 1_800L, "ml.docilealligator.infinityforreddit" to 1_800L),
            emptyMap(),
            null,
            0L
        ))
        assertTrue(DailyLimitPolicy.isAppGroupLimitExceeded(
            settings.copy(timeEnabled = false),
            groups,
            emptyMap(),
            mapOf("com.reddit.frontpage" to 30L, "ml.docilealligator.infinityforreddit" to 70L),
            null,
            0L
        ))
        assertFalse(DailyLimitPolicy.isAppGroupLimitExceeded(
            settings.copy(scrollEnabled = false),
            groups,
            mapOf("com.instagram.android" to 50_000L),
            emptyMap(),
            null,
            0L
        ))
    }

    @Test
    fun activeSessionIsAddedOnlyToItsOwnAppGroupAndThresholdIsInclusive() {
        val settings = AppGroupDailyLimitSettings(
            appTag = "insta",
            timeEnabled = true,
            timeLimitMinutes = 1,
            scrollEnabled = false,
            scrollLimit = 100
        )
        val groups = mapOf("com.instagram.android" to "insta", "com.facebook.katana" to "facebook")
        val recorded = mapOf("com.instagram.android" to 59L)

        assertFalse(DailyLimitPolicy.isAppGroupLimitExceeded(
            settings, groups, recorded, emptyMap(), "com.facebook.katana", 1L
        ))
        assertTrue(DailyLimitPolicy.isAppGroupLimitExceeded(
            settings, groups, recorded, emptyMap(), "com.instagram.android", 1L
        ))

        assertFalse(DailyLimitPolicy.isAppGroupTimeLimitExceeded(
            "insta",
            groups,
            mapOf("com.facebook.katana" to 3_600L),
            null,
            0L,
            60L
        ))
    }

    @Test
    fun disabledMetricsAndOtherGroupsDoNotTriggerLockout() {
        val settings = AppGroupDailyLimitSettings.defaults("twitter")
        val groups = mapOf("com.twitter.android" to "twitter", "com.facebook.katana" to "facebook")

        assertFalse(DailyLimitPolicy.isAppGroupLimitExceeded(
            settings,
            groups,
            mapOf("com.twitter.android" to 1_000_000L),
            mapOf("com.twitter.android" to 1_000_000L, "com.facebook.katana" to 1_000_000L),
            "com.twitter.android",
            1_000_000L
        ))
    }
}
