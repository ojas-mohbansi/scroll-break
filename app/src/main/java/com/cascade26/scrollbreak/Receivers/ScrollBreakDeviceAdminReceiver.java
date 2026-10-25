package com.cascade26.scrollbreak.Receivers;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.cascade26.scrollbreak.R;

/**
 * Device Administration receiver for Scroll Break.
 *
 * When the user grants Device Admin rights, Android requires the user to
 * deactivate this receiver before uninstalling the app. It does not prevent
 * force-stopping the app or guarantee that its services remain active.
 *
 * This is the approved, Play-Store-compliant mechanism for self-protection.
 */
public class ScrollBreakDeviceAdminReceiver extends DeviceAdminReceiver {

    @Override
    public void onEnabled(@NonNull Context context, @NonNull Intent intent) {
    }

    @Override
    public void onDisabled(@NonNull Context context, @NonNull Intent intent) {
        Toast.makeText(context,
                "Admin permission revoked — app can now be uninstalled.",
                Toast.LENGTH_LONG).show();
    }

    @Override
    public CharSequence onDisableRequested(@NonNull Context context, @NonNull Intent intent) {
        return "Revoking this permission will allow the app to be uninstalled. Are you sure?";
    }
}
