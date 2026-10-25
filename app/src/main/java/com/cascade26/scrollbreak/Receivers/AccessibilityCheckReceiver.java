package com.cascade26.scrollbreak.Receivers;

import static com.cascade26.scrollbreak.Utils.Utils.isAccessibilityPermissionGranted;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.cascade26.scrollbreak.Utils.AlarmUtils;
import com.cascade26.scrollbreak.Utils.WarningUtils;

public class AccessibilityCheckReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!isAccessibilityPermissionGranted(context)) {
            WarningUtils.showPermissionWarning(context);
        } else {
            if (WarningUtils.isWarningVisible(context)) WarningUtils.remove(context);
        }
        AlarmUtils.scheduleAlarm(context);
    }
}