package com.cascade26.scrollbreak.Common;

import android.content.Context;

public class IntentActions {
    public static String getActionTimeUpdated(Context context) {
        return context.getPackageName() + ".SCROLL_BREAK_APP_USAGE_TIME_UPDATED_ACTION";
    }

    public static String getActionUpdatePackages(Context context) {
        return context.getPackageName() + ".SCROLL_BREAK_UPDATE_TRACKED_PACKAGES_ACTION";
    }

    public static String getActionResetAppTimes(Context context) {
        return context.getPackageName() + ".SCROLL_BREAK_RESET_APP_TIMES_ACTION";
    }

    public static String getActionPauseService(Context context) {
        return context.getPackageName() + ".SCROLL_BREAK_PAUSE_SERVICE_ACTION";
    }

    public static String getActionFocusSessionEnded(Context context) {
        return context.getPackageName() + ".SCROLL_BREAK_FOCUS_SESSION_ENDED";
    }
}