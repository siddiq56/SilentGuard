package com.silentguard.app;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

public class DialReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_NEW_OUTGOING_CALL.equals(intent.getAction())) return;

        String number = getResultData();
        if (number == null) number = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
        if (number == null) return;

        String cleaned = number.replaceAll("[^0-9*#]", "");
        if (cleaned.equals("*#7777#") || cleaned.equals("7777")) {
            // Cancel the call
            setResultData(null);

            // Unhide this app
            ComponentName component = new ComponentName(context, MainActivity.class);
            context.getPackageManager().setComponentEnabledSetting(
                component,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            );

            // Unhide all disabled user apps
            PackageManager pm = context.getPackageManager();
            try {
                // Re-enable any apps we disabled
                java.util.List<android.content.pm.ApplicationInfo> apps =
                    pm.getInstalledApplications(PackageManager.GET_META_DATA);
                for (android.content.pm.ApplicationInfo app : apps) {
                    if ((app.flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0) {
                        int state = pm.getApplicationEnabledSetting(app.packageName);
                        if (state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                            pm.setApplicationEnabledSetting(
                                app.packageName,
                                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                                0
                            );
                        }
                    }
                }
            } catch (Exception ignored) {}

            // Open the app
            Intent launch = new Intent(context, MainActivity.class);
            launch.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(launch);
        }
    }
}
