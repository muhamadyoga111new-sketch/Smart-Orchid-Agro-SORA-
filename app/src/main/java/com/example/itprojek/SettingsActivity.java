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

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        hideSystemNavBar();
        applyStatusBarInsets();

        // Back button
        ImageView btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        // Settings menu items — push navigation (slide like Instagram detail)
        LinearLayout itemSchedule = findViewById(R.id.item_schedule);
        LinearLayout itemCalibration = findViewById(R.id.item_calibration);
        LinearLayout itemNotifAlert = findViewById(R.id.item_notif_alert);

        applyScaleAnimation(itemSchedule);
        applyScaleAnimation(itemCalibration);
        applyScaleAnimation(itemNotifAlert);

        itemSchedule.setOnClickListener(v -> {
            startActivity(new Intent(this, ScheduleActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        itemCalibration.setOnClickListener(v -> {
            startActivity(new Intent(this, CalibrationActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        itemNotifAlert.setOnClickListener(v -> {
            startActivity(new Intent(this, NotifAlertActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        // Bottom Navigation — tab switch (crossfade)
        LinearLayout navHome = findViewById(R.id.nav_home);
        LinearLayout navHistory = findViewById(R.id.nav_history);
        LinearLayout navNotifications = findViewById(R.id.nav_notifications);

        applyScaleAnimation(navHome);
        applyScaleAnimation(navHistory);
        applyScaleAnimation(navNotifications);

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
            if (btnBack != null && btnBack.getLayoutParams() instanceof ConstraintLayout.LayoutParams) {
                ConstraintLayout.LayoutParams p = (ConstraintLayout.LayoutParams) btnBack.getLayoutParams();
                p.topMargin = statusH;
                btnBack.setLayoutParams(p);
            }
            View tvTitle = findViewById(R.id.tv_header_title);
            if (tvTitle != null && tvTitle.getLayoutParams() instanceof ConstraintLayout.LayoutParams) {
                ConstraintLayout.LayoutParams p = (ConstraintLayout.LayoutParams) tvTitle.getLayoutParams();
                p.topMargin = statusH;
                tvTitle.setLayoutParams(p);
            }
            return insets;
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
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }
}
