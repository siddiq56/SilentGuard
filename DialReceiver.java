package com.silentguard.app;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.TelephonyManager;

public class DialReceiver extends BroadcastReceiver {

    // Secret code to show the app again — dial *#7777# on your phone
    private static final String SECRET_CODE = "*#7777#";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        // Intercept NEW_OUTGOING_CALL to catch dialed numbers
        if (Intent.ACTION_NEW_OUTGOING_CALL.equals(action)) {
            String number = getResultData();
            if (number == null) {
                number = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
            }

            if (number != null && matchesSecretCode(number)) {
                // Cancel the call — don't actually dial
                setResultData(null);

                // Show the app icon again
                showApp(context);

                // Open the app immediately
                Intent launchIntent = new Intent(context, MainActivity.class);
                launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(launchIntent);
            }
        }
    }

    private boolean matchesSecretCode(String number) {
        // Strip formatting — user might dial differently
        String cleaned = number.replaceAll("[^0-9*#]", "");
        return cleaned.equals("*#7777#") || cleaned.equals("7777");
    }

    public static void hideApp(Context context) {
        ComponentName component = new ComponentName(context, MainActivity.class);
        context.getPackageManager().setComponentEnabledSetting(
            component,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        );
    }

    public static void showApp(Context context) {
        ComponentName component = new ComponentName(context, MainActivity.class);
        context.getPackageManager().setComponentEnabledSetting(
            component,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        );
    }
}
