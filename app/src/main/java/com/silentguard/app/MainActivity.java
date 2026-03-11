package com.silentguard.app;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ListView appList;
    private Button hideButton;
    private TextView hintText;
    private List<ApplicationInfo> installedApps;
    private List<String> appNames;
    private List<String> packageNames;
    private boolean[] checkedItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        appList = findViewById(R.id.appList);
        hideButton = findViewById(R.id.hideButton);
        hintText = findViewById(R.id.hintText);

        loadInstalledApps();

        hideButton.setOnClickListener(v -> showHideConfirmDialog());
    }

    private void loadInstalledApps() {
        PackageManager pm = getPackageManager();
        installedApps = new ArrayList<>();
        appNames = new ArrayList<>();
        packageNames = new ArrayList<>();

        List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        for (ApplicationInfo app : apps) {
            // Only show user installed apps
            if ((app.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                // Don't show ourselves
                if (!app.packageName.equals(getPackageName())) {
                    installedApps.add(app);
                    appNames.add(pm.getApplicationLabel(app).toString());
                    packageNames.add(app.packageName);
                }
            }
        }

        checkedItems = new boolean[appNames.size()];

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
            android.R.layout.simple_list_item_multiple_choice, appNames);
        appList.setAdapter(adapter);
        appList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        appList.setOnItemClickListener((parent, view, position, id) -> {
            checkedItems[position] = !checkedItems[position];
        });
    }

    private void showHideConfirmDialog() {
        List<String> selected = new ArrayList<>();
        for (int i = 0; i < checkedItems.length; i++) {
            if (checkedItems[i]) selected.add(appNames.get(i));
        }

        if (selected.isEmpty()) {
            Toast.makeText(this, "Select at least one app to hide", Toast.LENGTH_SHORT).show();
            return;
        }

        String appList = "";
        for (String s : selected) appList += "• " + s + "\n";

        new AlertDialog.Builder(this)
            .setTitle("Hide These Apps?")
            .setMessage("These apps will be hidden:\n\n" + appList +
                "\nThis app will also hide itself.\n\n" +
                "📞 To unhide everything, open Phone app and dial *#7777#")
            .setPositiveButton("Hide All", (d, w) -> hideSelectedApps())
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void hideSelectedApps() {
        PackageManager pm = getPackageManager();

        // Hide selected apps
        for (int i = 0; i < checkedItems.length; i++) {
            if (checkedItems[i]) {
                try {
                    pm.setApplicationEnabledSetting(
                        packageNames.get(i),
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        0
                    );
                } catch (Exception e) {
                    Toast.makeText(this, "Cannot hide: " + appNames.get(i), Toast.LENGTH_SHORT).show();
                }
            }
        }

        // Hide ourselves last
        ComponentName component = new ComponentName(this, MainActivity.class);
        pm.setComponentEnabledSetting(
            component,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        );
    }
}
