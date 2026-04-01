package com.example.itprojek;

import android.content.Intent;
import android.graphics.Color;
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
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity {

    private PrefManager pref;
    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pref = new PrefManager(this);
        hideSystemNavBar();

        // === Navigation Drawer Setup ===
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navView = findViewById(R.id.nav_view);

        // Set "Beranda" as checked (current page)
        navView.setCheckedItem(R.id.drawer_home);

        // Hamburger menu button opens drawer
        ImageView btnMenu = findViewById(R.id.btn_menu);
        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // Handle drawer menu clicks
        navView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            drawerLayout.closeDrawer(GravityCompat.START);

            // Delay navigation slightly so drawer animation finishes smoothly
            drawerLayout.postDelayed(() -> {
                if (id == R.id.drawer_home) {
                    // Already on home, do nothing
                } else if (id == R.id.drawer_history) {
                    startActivity(new Intent(this, HistoryActivity.class));
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                } else if (id == R.id.drawer_notifications) {
                    startActivity(new Intent(this, NotificationsActivity.class));
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                } else if (id == R.id.drawer_settings) {
                    startActivity(new Intent(this, SettingsActivity.class));
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                } else if (id == R.id.drawer_about) {
                    showAboutDialog();
                } else if (id == R.id.drawer_logout) {
                    logout();
                }
            }, 300);

            return true;
        });

        // Apply status bar insets to header content + resize header
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            v.setPadding(systemBars.left, 0, systemBars.right, 0);

            // Extend header_bg behind status bar — add status bar height to base 130dp
            View headerBg = findViewById(R.id.header_bg);
            if (headerBg != null) {
                int baseDp = 130;
                int basePx = (int) (baseDp * getResources().getDisplayMetrics().density);
                headerBg.getLayoutParams().height = basePx + systemBars.top;
                headerBg.requestLayout();
            }

            if (btnMenu != null) {
                ((androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) btnMenu.getLayoutParams()).topMargin = systemBars.top + 8;
                btnMenu.requestLayout();
            }
            View btnNotifView = findViewById(R.id.btn_notification_top);
            if (btnNotifView != null) {
                ((androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) btnNotifView.getLayoutParams()).topMargin = systemBars.top + 8;
                btnNotifView.requestLayout();
            }
            return insets;
        });

        // Notification bell — push navigation (slide)
        ImageView btnNotification = findViewById(R.id.btn_notification_top);
        btnNotification.setOnClickListener(v -> {
            startActivity(new Intent(this, NotificationsActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        // === Pump toggle with SharedPreferences ===
        MaterialSwitch switchPump = findViewById(R.id.switch_pump);
        TextView tvPumpStatus = findViewById(R.id.tv_pump_status);

        boolean pumpSaved = pref.getBoolean("STATUS_POMPA", false);
        switchPump.setChecked(pumpSaved);
        updatePumpStatusUI(tvPumpStatus, pumpSaved);

        switchPump.setOnCheckedChangeListener((buttonView, isChecked) -> {
            pref.saveBoolean("STATUS_POMPA", isChecked);
            updatePumpStatusUI(tvPumpStatus, isChecked);
        });

        // Emergency Stop panel with scale animation
        LinearLayout emergencyPanel = findViewById(R.id.emergency_panel);
        applyScaleAnimation(emergencyPanel);
        emergencyPanel.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.confirm_emergency_title))
                    .setMessage(getString(R.string.confirm_emergency_msg))
                    .setPositiveButton(getString(R.string.btn_yes), (dialog, which) -> {
                        switchPump.setChecked(false);
                        dialog.dismiss();
                    })
                    .setNegativeButton(getString(R.string.btn_cancel), (dialog, which) -> dialog.dismiss())
                    .setCancelable(true)
                    .show();
        });

        // Bottom Navigation — tab switch (crossfade)
        LinearLayout navHistory = findViewById(R.id.nav_history);
        LinearLayout navNotifications = findViewById(R.id.nav_notifications);
        LinearLayout navSettings = findViewById(R.id.nav_settings);

        applyScaleAnimation(navHistory);
        applyScaleAnimation(navNotifications);
        applyScaleAnimation(navSettings);

        navHistory.setOnClickListener(v -> {
            startActivity(new Intent(this, HistoryActivity.class));
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });

        navNotifications.setOnClickListener(v -> {
            startActivity(new Intent(this, NotificationsActivity.class));
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });

        navSettings.setOnClickListener(v -> {
            startActivity(new Intent(this, SettingsActivity.class));
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });
    }

    /** Shows the "Tentang Aplikasi" dialog */
    private void showAboutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Tentang Aplikasi")
                .setMessage("SORA - Smart Orchid Agro\n\n"
                        + "Sistem Penyiraman Anggrek Otomatis\n"
                        + "berbasis IoT.\n\n"
                        + "Versi: 1.0.0\n"
                        + "© 2024 SORA Team")
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .setCancelable(true)
                .show();
    }

    /** Konfirmasi logout lalu hapus sesi dan kembali ke LoginActivity */
    private void logout() {
        new AlertDialog.Builder(this)
                .setTitle("Keluar")
                .setMessage("Apakah Anda yakin ingin keluar dari akun?")
                .setPositiveButton("Keluar", (dialog, which) -> {
                    pref.saveBoolean("IS_LOGGED_IN", false);
                    pref.remove("USER_EMAIL");
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                    finish();
                })
                .setNegativeButton("Batal", (dialog, which) -> dialog.dismiss())
                .setCancelable(true)
                .show();
    }

    private void updatePumpStatusUI(TextView tvPumpStatus, boolean isOn) {
        if (isOn) {
            tvPumpStatus.setText(getString(R.string.on));
            tvPumpStatus.setTextColor(Color.parseColor("#43A047"));
        } else {
            tvPumpStatus.setText(getString(R.string.off));
            tvPumpStatus.setTextColor(Color.parseColor("#EF5350"));
        }
    }

    private void applyScaleAnimation(View view) {
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    Animation scaleDown = AnimationUtils.loadAnimation(this, R.anim.scale_down);
                    scaleDown.setFillAfter(true);
                    v.startAnimation(scaleDown);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    Animation scaleUp = AnimationUtils.loadAnimation(this, R.anim.scale_up);
                    scaleUp.setFillAfter(true);
                    v.startAnimation(scaleUp);
                    break;
            }
            return false;
        });
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
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