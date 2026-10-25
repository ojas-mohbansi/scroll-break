package com.cascade26.scrollbreak.Receivers;

import static com.cascade26.scrollbreak.Utils.Utils.isAccessibilityPermissionGranted;
import static com.cascade26.scrollbreak.Utils.Utils.isKeepAlive;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.cascade26.scrollbreak.Utils.AlarmUtils;
import com.cascade26.scrollbreak.Utils.WarningUtils;

import java.util.Objects;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (isKeepAlive(context) && Objects.equals(intent.getAction(), Intent.ACTION_BOOT_COMPLETED)) {
            if (!isAccessibilityPermissionGranted(context)) {
                WarningUtils.showPermissionWarning(context);
                AlarmUtils.scheduleAlarm(context);
            }
        }
    }
}


