package com.silentguard.app;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ListView appList;
    private Button hideButton;
    private List<String> appNames = new ArrayList<>();
    private List<String> packageNames = new ArrayList<>();
    private boolean[] checkedItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        appList = findViewById(R.id.appList);
        hideButton = findViewById(R.id.hideButton);

        // Start shake detector service
        Intent serviceIntent = new Intent(this, HiddenService.class);
        startForegroundService(serviceIntent);

        loadInstalledApps();

        hideButton.setOnClickListener(v -> confirmHide());
    }

    private void loadInstalledApps() {
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        for (ApplicationInfo app : apps) {
            if ((app.flags & ApplicationInfo.FLAG_SYSTEM) == 0
                    && !app.packageName.equals(getPackageName())) {
                appNames.add(pm.getApplicationLabel(app).toString());
                packageNames.add(app.packageName);
            }
        }

        checkedItems = new boolean[appNames.size()];
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
            android.R.layout.simple_list_item_multiple_choice, appNames);
        appList.setAdapter(adapter);
        appList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        appList.setOnItemClickListener((p, v, pos, id) -> checkedItems[pos] = !checkedItems[pos]);
    }

    private void confirmHide() {
        List<String> selected = new ArrayList<>();
        for (int i = 0; i < checkedItems.length; i++) {
            if (checkedItems[i]) selected.add(appNames.get(i));
        }

        if (selected.isEmpty()) {
            Toast.makeText(this, "Select apps to hide", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder list = new StringBuilder();
        for (String s : selected) list.append("• ").append(s).append("\n");

        new AlertDialog.Builder(this)
            .setTitle("Hide These Apps?")
            .setMessage("Hiding:\n\n" + list +
                "\nThis app will also hide itself.\n\n" +
                "📳 To unhide: shake phone 5 times quickly")
            .setPositiveButton("Hide All", (d, w) -> hideAll(selected))
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void hideAll(List<String> selected) {
        PackageManager pm = getPackageManager();

        // Hide selected apps
        for (int i = 0; i < checkedItems.length; i++) {
            if (checkedItems[i]) {
                try {
                    pm.setApplicationEnabledSetting(
                        packageNames.get(i),
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED, 0);
                } catch (Exception e) {
                    Toast.makeText(this, "Can't hide: " + appNames.get(i),
                        Toast.LENGTH_SHORT).show();
                }
            }
        }

        // Hide ourselves
        ComponentName me = new ComponentName(this, MainActivity.class);
        pm.setComponentEnabledSetting(me,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP);
    }
}
