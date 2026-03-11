package com.silentguard.app;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private Button toggleButton;
    private Button hideButton;
    private TextView statusText;
    private TextView descText;
    private LinearLayout setupCard;
    private TextView setupStatusText;
    private Button setupButton;
    private boolean isRunning = false;

    private ActivityResultLauncher<String> notificationPermissionLauncher;
    private ActivityResultLauncher<String> phonePermissionLauncher;
    private ActivityResultLauncher<Intent> batteryOptLauncher;

    private static final int SETUP_STEP_NOTIFICATION = 1;
    private static final int SETUP_STEP_PHONE = 2;
    private static final int SETUP_STEP_BATTERY = 3;
    private static final int SETUP_STEP_DONE = 4;
    private int currentSetupStep = SETUP_STEP_NOTIFICATION;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toggleButton = findViewById(R.id.toggleButton);
        hideButton = findViewById(R.id.hideButton);
        statusText = findViewById(R.id.statusText);
        descText = findViewById(R.id.descText);
        setupCard = findViewById(R.id.setupCard);
        setupStatusText = findViewById(R.id.setupStatusText);
        setupButton = findViewById(R.id.setupButton);

        notificationPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            granted -> {
                if (granted) proceedToNextStep();
                else showPermissionDeniedDialog("Notifications",
                    "SilentGuard needs notifications to stay alive in background.");
            }
        );

        phonePermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            granted -> proceedToNextStep()
        );

        batteryOptLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> proceedToNextStep()
        );

        isRunning = VolumeLockService.isRunning;
        checkSetupAndUpdateUI();

        toggleButton.setOnClickListener(v -> {
            if (isRunning) stopVolumeLock();
            else startVolumeLock();
        });

        hideButton.setOnClickListener(v -> showHideConfirmDialog());
    }

    @Override
    protected void onResume() {
        super.onResume();
        isRunning = VolumeLockService.isRunning;
        checkSetupAndUpdateUI();
    }

    // ── Setup Flow ───────────────────────────────────────────────────────────

    private void checkSetupAndUpdateUI() {
        if (!isNotificationPermissionGranted()) {
            currentSetupStep = SETUP_STEP_NOTIFICATION;
            showSetupCard("Step 1 of 3: Allow Notifications",
                "SilentGuard needs to show a notification so Android keeps it running in the background.",
                "Allow Notifications");
        } else if (!isPhonePermissionGranted()) {
            currentSetupStep = SETUP_STEP_PHONE;
            showSetupCard("Step 2 of 3: Allow Phone Access",
                "This lets SilentGuard detect when you dial the secret code *#7777# to unhide the app after hiding it.\n\nYou can skip this but the dial-to-unhide feature won't work.",
                "Allow Phone Access");
        } else if (!isBatteryOptimizationIgnored()) {
            currentSetupStep = SETUP_STEP_BATTERY;
            showSetupCard("Step 3 of 3: Disable Battery Restriction",
                "ColorOS kills background apps aggressively. On the next screen, find SilentGuard and tap \"Don't optimise\".",
                "Disable Battery Restriction");
        } else {
            currentSetupStep = SETUP_STEP_DONE;
            hideSetupCard();
            updateMainUI();
        }
    }

    private void handleCurrentSetupStep() {
        switch (currentSetupStep) {
            case SETUP_STEP_NOTIFICATION: requestNotificationPermission(); break;
            case SETUP_STEP_PHONE:        requestPhonePermission();        break;
            case SETUP_STEP_BATTERY:      requestBatteryOptimizationExemption(); break;
        }
    }

    private void proceedToNextStep() { checkSetupAndUpdateUI(); }

    private void showSetupCard(String title, String desc, String buttonLabel) {
        setupCard.setVisibility(View.VISIBLE);
        toggleButton.setVisibility(View.GONE);
        hideButton.setVisibility(View.GONE);
        setupStatusText.setText(title + "\n\n" + desc);
        setupButton.setText(buttonLabel);
        setupButton.setOnClickListener(v -> handleCurrentSetupStep());
    }

    private void hideSetupCard() {
        setupCard.setVisibility(View.GONE);
        toggleButton.setVisibility(View.VISIBLE);
        hideButton.setVisibility(View.VISIBLE);
    }

    // ── Permissions ──────────────────────────────────────────────────────────

    private boolean isNotificationPermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private boolean isPhonePermissionGranted() {
        return ContextCompat.checkSelfPermission(this,
            android.Manifest.permission.PROCESS_OUTGOING_CALLS) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean isBatteryOptimizationIgnored() {
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        return pm.isIgnoringBatteryOptimizations(getPackageName());
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
        } else {
            proceedToNextStep();
        }
    }

    private void requestPhonePermission() {
        phonePermissionLauncher.launch(android.Manifest.permission.PROCESS_OUTGOING_CALLS);
    }

    private void requestBatteryOptimizationExemption() {
        new AlertDialog.Builder(this)
            .setTitle("Disable Battery Restriction")
            .setMessage("On the next screen:\n\n1. Tap \"Battery optimisation\"\n2. Find SilentGuard\n3. Select \"Don't optimise\"\n4. Tap Done")
            .setPositiveButton("Open Settings", (d, w) -> {
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                batteryOptLauncher.launch(intent);
            })
            .setCancelable(false)
            .show();
    }

    private void showPermissionDeniedDialog(String permName, String reason) {
        new AlertDialog.Builder(this)
            .setTitle(permName + " Permission Needed")
            .setMessage(reason)
            .setPositiveButton("Try Again", (d, w) -> handleCurrentSetupStep())
            .setNegativeButton("Skip", (d, w) -> proceedToNextStep())
            .show();
    }

    // ── Hide Feature ─────────────────────────────────────────────────────────

    private void showHideConfirmDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Hide App Icon?")
            .setMessage("The SilentGuard icon will vanish from your home screen and app drawer.\n\n" +
                "✅ The service keeps running while hidden.\n\n" +
                "📞 To bring it back:\nOpen your Phone app and dial  *#7777#\n" +
                "(The call won't connect — it's intercepted)")
            .setPositiveButton("Hide It", (d, w) -> {
                if (!isRunning) startVolumeLock(); // ensure service is running before hiding
                DialReceiver.hideApp(this);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    // ── Service Control ──────────────────────────────────────────────────────

    private void startVolumeLock() {
        Intent intent = new Intent(this, VolumeLockService.class);
        intent.setAction(VolumeLockService.ACTION_START);
        startForegroundService(intent);
        isRunning = true;
        updateMainUI();
    }

    private void stopVolumeLock() {
        Intent intent = new Intent(this, VolumeLockService.class);
        intent.setAction(VolumeLockService.ACTION_STOP);
        startService(intent);
        isRunning = false;
        updateMainUI();
    }

    private void updateMainUI() {
        if (isRunning) {
            toggleButton.setText("Stop");
            toggleButton.setBackgroundColor(0xFFE53935);
            statusText.setText("🔇 Volume Locked at 0");
            statusText.setTextColor(0xFF4CAF50);
            descText.setText("Running silently in background.\nVolume is being held at zero.");
        } else {
            toggleButton.setText("Start");
            toggleButton.setBackgroundColor(0xFF1976D2);
            statusText.setText("⏸ Inactive");
            statusText.setTextColor(0xFF9E9E9E);
            descText.setText("Tap Start to lock volume at zero.\nWorks silently in the background.");
        }
    }
}
