package com.cascade26.scrollbreak.Utils;

import android.content.Intent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.IOException;

public class ScrollBrakeVpnService extends VpnService {
    private static final String TAG = "ScrollBrakeVpnService";
    private ParcelFileDescriptor vpnInterface = null;
    private Thread vpnThread = null;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (vpnThread == null) {
            vpnThread = new Thread(this::runVpn, "ScrollBrakeVpnThread");
            vpnThread.start();
        }
        return START_STICKY;
    }

    private void runVpn() {
        try {
            Builder builder = new Builder();
            builder.addAddress("10.0.0.2", 24);
            builder.addDnsServer("10.0.0.1");
            builder.addRoute("0.0.0.0", 0);
            builder.setSession("ScrollBrake DNS Protection");
            builder.setConfigureIntent(null);

            vpnInterface = builder.establish();
            if (vpnInterface == null) {
                Log.e(TAG, "Failed to establish VPN interface");
                return;
            }

            Log.i(TAG, "VPN service established successfully.");

        } catch (Exception e) {
            Log.e(TAG, "Error running VPN service", e);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (vpnInterface != null) {
            try {
                vpnInterface.close();
            } catch (IOException ignored) {}
            vpnInterface = null;
        }
        if (vpnThread != null) {
            vpnThread.interrupt();
            vpnThread = null;
        }
    }
}
