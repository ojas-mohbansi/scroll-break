package com.cascade26.scrollbreak.Receivers;

import static com.cascade26.scrollbreak.Utils.Utils.isAccessibilityPermissionGranted;
import static com.cascade26.scrollbreak.Utils.Utils.isKeepAlive;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.cascade26.scrollbreak.Services.AppUsageAccessibilityService;
import com.cascade26.scrollbreak.Utils.AlarmUtils;
import com.cascade26.scrollbreak.Utils.WarningUtils;

public class NotificationDismissBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        boolean enabled = isAccessibilityPermissionGranted(context);
        if (!isKeepAlive(context)) return;
        Intent restoreIntent = new Intent(AppUsageAccessibilityService.RESTORE_NOTIFICATION);
        restoreIntent.setPackage(context.getPackageName());
        context.sendBroadcast(restoreIntent);
        if (enabled) {
            if (!AppUsageAccessibilityService.serviceStatus) {
                WarningUtils.showPermissionWarning(context);
            }
        } else {
            WarningUtils.showPermissionWarning(context);
            AlarmUtils.scheduleAlarm(context);
        }
    }
}