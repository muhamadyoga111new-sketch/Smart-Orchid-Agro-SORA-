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
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.materialswitch.MaterialSwitch;

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

        // === 1. Peringatan Tanah Kering (Warning) ===
        MaterialSwitch switchDry = findViewById(R.id.switch_dry);
        setupSwitch(switchDry, "NOTIF_DRY", true, getString(R.string.alert_dry_title));

        // === 2. Peringatan Air Tangki Habis (Critical) ===
        MaterialSwitch switchTank = findViewById(R.id.switch_tank);
        setupSwitch(switchTank, "NOTIF_TANK", true, getString(R.string.alert_tank_title));

        // === 3. Notifikasi Tanah Lembap (Info) ===
        MaterialSwitch switchMoist = findViewById(R.id.switch_moist);
        setupSwitch(switchMoist, "NOTIF_MOIST", false, getString(R.string.alert_moist_title));

        // === 4. Alarm Air Tangki Habis (Alarm) ===
        MaterialSwitch switchTankAlarm = findViewById(R.id.switch_tank_alarm);
        setupSwitch(switchTankAlarm, "NOTIF_TANK_ALARM", true, getString(R.string.alert_tank_alarm_title));

        setupBottomNav();
    }

    private void setupSwitch(MaterialSwitch mSwitch, String key, boolean defaultValue, String title) {
        // Restore state
        mSwitch.setChecked(pref.getBoolean(key, defaultValue));
        
        // Listener with feedback
        mSwitch.setOnCheckedChangeListener((btn, checked) -> {
            pref.saveBoolean(key, checked);
            String message = String.format(getString(checked ? R.string.notif_enabled : R.string.notif_disabled), title);
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        });
    }

    private void applyStatusBarInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(getWindow().getDecorView(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            int statusH  = systemBars.top;
            int base60px = (int) (60 * getResources().getDisplayMetrics().density);

            View headerBg = findViewById(R.id.header_bg);
            if (headerBg != null) {
                headerBg.getLayoutParams().height = base60px + statusH;
                headerBg.requestLayout();
            }

            View btnBack = findViewById(R.id.btn_back);
            if (btnBack != null && btnBack.getLayoutParams() instanceof androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) {
                androidx.constraintlayout.widget.ConstraintLayout.LayoutParams p = (androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) btnBack.getLayoutParams();
                p.topMargin = statusH;
                btnBack.setLayoutParams(p);
            }

            View tvTitle = findViewById(R.id.tv_header_title);
            if (tvTitle != null && tvTitle.getLayoutParams() instanceof androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) {
                androidx.constraintlayout.widget.ConstraintLayout.LayoutParams p = (androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) tvTitle.getLayoutParams();
                p.topMargin = statusH;
                tvTitle.setLayoutParams(p);
            }
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
