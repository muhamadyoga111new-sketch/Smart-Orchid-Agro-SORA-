package com.example.itprojek;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        hideSystemNavBar();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        // Notification bell in header
        ImageView btnNotification = findViewById(R.id.btn_notification_top);
        btnNotification.setOnClickListener(v -> {
            startActivity(new Intent(this, NotificationsActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        // Emergency Stop panel — Peak-End Rule: confirmation dialog for critical action
        LinearLayout emergencyPanel = findViewById(R.id.emergency_panel);
        emergencyPanel.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.confirm_emergency_title))
                    .setMessage(getString(R.string.confirm_emergency_msg))
                    .setPositiveButton(getString(R.string.btn_yes), (dialog, which) -> {
                        // Emergency stop activated
                        dialog.dismiss();
                    })
                    .setNegativeButton(getString(R.string.btn_cancel), (dialog, which) -> {
                        dialog.dismiss();
                    })
                    .setCancelable(true)
                    .show();
        });

        // Bottom Navigation
        LinearLayout navHistory = findViewById(R.id.nav_history);
        LinearLayout navNotifications = findViewById(R.id.nav_notifications);
        LinearLayout navSettings = findViewById(R.id.nav_settings);

        navHistory.setOnClickListener(v -> {
            startActivity(new Intent(this, HistoryActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        navNotifications.setOnClickListener(v -> {
            startActivity(new Intent(this, NotificationsActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        navSettings.setOnClickListener(v -> {
            startActivity(new Intent(this, SettingsActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemNavBar();
        }
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