package com.silentguard.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;

public class HiddenService extends Service {

    public static final String CHANNEL_ID = "HiderChannel";
    public static final int NOTIF_ID = 2;
    public static boolean isRunning = false;

    private ShakeDetector shakeDetector;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(NOTIF_ID, buildNotification());
        isRunning = true;

        shakeDetector = new ShakeDetector(this, () -> {
            // Shake detected — unhide everything
            unhideAll();
        });
        shakeDetector.start();
    }

    private void unhideAll() {
        PackageManager pm = getPackageManager();

        // Unhide main activity
        ComponentName component = new ComponentName(this, MainActivity.class);
        pm.setComponentEnabledSetting(
            component,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        );

        // Unhide all user apps that were disabled
        try {
            java.util.List<ApplicationInfo> apps =
                pm.getInstalledApplications(PackageManager.GET_META_DATA);
            for (ApplicationInfo app : apps) {
                if ((app.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
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
        Intent launch = new Intent(this, MainActivity.class);
        launch.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(launch);
    }

    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Running")
            .setContentText("")
            .setSmallIcon(android.R.drawable.ic_menu_preferences)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .build();
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
            CHANNEL_ID, "Hider", NotificationManager.IMPORTANCE_MIN);
        channel.setSound(null, null);
        channel.setShowBadge(false);
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
        if (shakeDetector != null) shakeDetector.stop();
    }
}
