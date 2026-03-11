package com.silentguard.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            // Auto-restart service after phone reboot
            Intent serviceIntent = new Intent(context, VolumeLockService.class);
            serviceIntent.setAction(VolumeLockService.ACTION_START);
            context.startForegroundService(serviceIntent);
        }
    }
}
