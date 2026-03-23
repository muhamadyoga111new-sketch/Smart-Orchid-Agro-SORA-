package com.example.itprojek;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class NotifAlertActivity extends AppCompatActivity {

    private PrefManager pref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notif_alert);
        hideSystemNavBar();
        applyStatusBarInsets();

        pref = new PrefManager(this);

        // Back button
        ImageView btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        // === All 4 switches with persistence ===
        // Pattern: restore value FIRST, then attach listener

        SwitchCompat switchDry = findViewById(R.id.switch_dry);
        switchDry.setChecked(pref.getBoolean("NOTIF_DRY", true));
        switchDry.setOnCheckedChangeListener((btn, checked) -> {
            pref.saveBoolean("NOTIF_DRY", checked);
            Toast.makeText(this, getString(R.string.alert_dry_title) + ": " + (checked ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
        });

        SwitchCompat switchTank = findViewById(R.id.switch_tank);
        switchTank.setChecked(pref.getBoolean("NOTIF_TANK", true));
        switchTank.setOnCheckedChangeListener((btn, checked) -> {
            pref.saveBoolean("NOTIF_TANK", checked);
            Toast.makeText(this, getString(R.string.alert_tank_title) + ": " + (checked ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
        });

        SwitchCompat switchMoist = findViewById(R.id.switch_moist);
        switchMoist.setChecked(pref.getBoolean("NOTIF_MOIST", false));
        switchMoist.setOnCheckedChangeListener((btn, checked) -> {
            pref.saveBoolean("NOTIF_MOIST", checked);
            Toast.makeText(this, getString(R.string.alert_moist_title) + ": " + (checked ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
        });

        SwitchCompat switchTankAlarm = findViewById(R.id.switch_tank_alarm);
        switchTankAlarm.setChecked(pref.getBoolean("NOTIF_TANK_ALARM", false));
        switchTankAlarm.setOnCheckedChangeListener((btn, checked) -> {
            pref.saveBoolean("NOTIF_TANK_ALARM", checked);
            Toast.makeText(this, getString(R.string.alert_tank_alarm_title) + ": " + (checked ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
        });

        // Bottom Navigation — tab switch (crossfade)
        setupBottomNav();
    }

    private void applyStatusBarInsets() {
        View headerBg = findViewById(R.id.header_bg);
        ViewCompat.setOnApplyWindowInsetsListener(headerBg, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            v.getLayoutParams().height = (int) (60 * getResources().getDisplayMetrics().density) + systemBars.top;
            v.setPadding(0, systemBars.top, 0, 0);
            v.requestLayout();
            return insets;
        });
    }

    private void setupBottomNav() {
        LinearLayout navHome = findViewById(R.id.nav_home);
        LinearLayout navHistory = findViewById(R.id.nav_history);
        LinearLayout navNotifications = findViewById(R.id.nav_notifications);
        LinearLayout navSettings = findViewById(R.id.nav_settings);

        applyScaleAnimation(navHome);
        applyScaleAnimation(navHistory);
        applyScaleAnimation(navNotifications);
        applyScaleAnimation(navSettings);

        navHome.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        });
        navHistory.setOnClickListener(v -> {
            startActivity(new Intent(this, HistoryActivity.class));
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        });
        navNotifications.setOnClickListener(v -> {
            startActivity(new Intent(this, NotificationsActivity.class));
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        });
        navSettings.setOnClickListener(v -> {
            startActivity(new Intent(this, SettingsActivity.class));
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        });
    }

    private void applyScaleAnimation(View view) {
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    Animation sd = AnimationUtils.loadAnimation(this, R.anim.scale_down);
                    sd.setFillAfter(true);
                    v.startAnimation(sd);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    Animation su = AnimationUtils.loadAnimation(this, R.anim.scale_up);
                    su.setFillAfter(true);
                    v.startAnimation(su);
                    break;
            }
            return false;
        });
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
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
