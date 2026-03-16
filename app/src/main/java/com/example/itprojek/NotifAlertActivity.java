package com.example.itprojek;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

public class NotifAlertActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notif_alert);
        hideSystemNavBar();

        // Back button
        ImageView btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        // Switches with toast feedback
        SwitchCompat switchDry = findViewById(R.id.switch_dry);
        SwitchCompat switchTank = findViewById(R.id.switch_tank);
        SwitchCompat switchMoist = findViewById(R.id.switch_moist);
        SwitchCompat switchTankAlarm = findViewById(R.id.switch_tank_alarm);

        switchDry.setOnCheckedChangeListener((btn, checked) ->
                Toast.makeText(this, getString(R.string.alert_dry_title) + ": " + (checked ? "ON" : "OFF"), Toast.LENGTH_SHORT).show());

        switchTank.setOnCheckedChangeListener((btn, checked) ->
                Toast.makeText(this, getString(R.string.alert_tank_title) + ": " + (checked ? "ON" : "OFF"), Toast.LENGTH_SHORT).show());

        switchMoist.setOnCheckedChangeListener((btn, checked) ->
                Toast.makeText(this, getString(R.string.alert_moist_title) + ": " + (checked ? "ON" : "OFF"), Toast.LENGTH_SHORT).show());

        switchTankAlarm.setOnCheckedChangeListener((btn, checked) ->
                Toast.makeText(this, getString(R.string.alert_tank_alarm_title) + ": " + (checked ? "ON" : "OFF"), Toast.LENGTH_SHORT).show());

        // Bottom Navigation
        setupBottomNav();
    }

    private void setupBottomNav() {
        LinearLayout navHome = findViewById(R.id.nav_home);
        LinearLayout navHistory = findViewById(R.id.nav_history);
        LinearLayout navNotifications = findViewById(R.id.nav_notifications);
        LinearLayout navSettings = findViewById(R.id.nav_settings);

        navHome.setOnClickListener(v -> { startActivity(new Intent(this, MainActivity.class)); finish(); });
        navHistory.setOnClickListener(v -> { startActivity(new Intent(this, HistoryActivity.class)); finish(); });
        navNotifications.setOnClickListener(v -> { startActivity(new Intent(this, NotificationsActivity.class)); finish(); });
        navSettings.setOnClickListener(v -> { startActivity(new Intent(this, SettingsActivity.class)); finish(); });
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemNavBar();
    }

    private void hideSystemNavBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }
}
