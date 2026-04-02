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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    // ── Device ID (sesuaikan dengan ID perangkat IoT Anda) ──
    private static final String DEVICE_ID = "ANG-123456";

    private PrefManager         pref;
    private DrawerLayout        drawerLayout;
    private FirebaseDataManager dataManager;
    private MaterialSwitch      switchPump;
    private TextView            tvPumpStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pref = new PrefManager(this);
        hideSystemNavBar();

        // Cek sesi Firebase — jika tidak ada, kembali ke Login
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            goToLogin();
            return;
        }

        // === Firebase Realtime Database ===
        dataManager = new FirebaseDataManager(DEVICE_ID);

        // === Navigation Drawer Setup ===
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navView = findViewById(R.id.nav_view);
        navView.setCheckedItem(R.id.drawer_home);

        // Hamburger menu button opens drawer
        ImageView btnMenu = findViewById(R.id.btn_menu);
        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // Handle drawer menu clicks
        navView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            drawerLayout.closeDrawer(GravityCompat.START);
            drawerLayout.postDelayed(() -> {
                if (id == R.id.drawer_home) {
                    // Already on home
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

        // Apply status bar insets to header
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            v.setPadding(systemBars.left, 0, systemBars.right, 0);

            View headerBg = findViewById(R.id.header_bg);
            if (headerBg != null) {
                // Return header height to standard 60dp + status bar height
                int basePx = (int) (60 * getResources().getDisplayMetrics().density);
                headerBg.getLayoutParams().height = basePx + systemBars.top;
                headerBg.requestLayout();
            }

            // Align all three items (menu, notification, and title) down by status bar height
            // so they are perfectly centered in the visible part of the header
            View btnMenuRef = findViewById(R.id.btn_menu);
            if (btnMenuRef != null) {
                ((androidx.constraintlayout.widget.ConstraintLayout.LayoutParams)
                        btnMenuRef.getLayoutParams()).topMargin = systemBars.top;
                btnMenuRef.requestLayout();
            }

            View btnNotifView = findViewById(R.id.btn_notification_top);
            if (btnNotifView != null) {
                ((androidx.constraintlayout.widget.ConstraintLayout.LayoutParams)
                        btnNotifView.getLayoutParams()).topMargin = systemBars.top;
                btnNotifView.requestLayout();
            }

            View tvTitle = findViewById(R.id.tv_title);
            if (tvTitle != null) {
                ((androidx.constraintlayout.widget.ConstraintLayout.LayoutParams)
                        tvTitle.getLayoutParams()).topMargin = systemBars.top;
                tvTitle.requestLayout();
            }

            return insets;
        });

        // Notification bell
        ImageView btnNotification = findViewById(R.id.btn_notification_top);
        btnNotification.setOnClickListener(v -> {
            startActivity(new Intent(this, NotificationsActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        // === Pump toggle — terhubung ke Firebase Realtime Database ===
        switchPump   = findViewById(R.id.switch_pump);
        tvPumpStatus = findViewById(R.id.tv_pump_status);

        // Nilai awal dari SharedPreferences (sementara sebelum Firebase terbaca)
        boolean pumpSaved = pref.getBoolean("STATUS_POMPA", false);
        switchPump.setChecked(pumpSaved);
        updatePumpStatusUI(tvPumpStatus, pumpSaved);

        switchPump.setOnCheckedChangeListener((buttonView, isChecked) -> {
            pref.saveBoolean("STATUS_POMPA", isChecked);
            updatePumpStatusUI(tvPumpStatus, isChecked);
            // Tulis perubahan pompa ke Firebase (dibaca IoT device)
            dataManager.setPumpStatus(isChecked);
        });

        // === Sensor data real-time dari Firebase ===
        dataManager.listenSensorData(new FirebaseDataManager.SensorListener() {
            @Override
            public void onSensorUpdated(int soilMoisture, int waterLevel, boolean pumpStatus) {
                // Update UI sensor kelembapan tanah
                TextView tvMoistureValue = findViewById(R.id.tv_moisture_value);
                if (tvMoistureValue != null) {
                    tvMoistureValue.setText(soilMoisture + "%");
                }

                // Update UI level air
                TextView tvWaterValue = findViewById(R.id.tv_water_value);
                if (tvWaterValue != null) {
                    tvWaterValue.setText(waterLevel + "%");
                }

                // Sync status pompa dari Firebase ke UI
                if (switchPump.isChecked() != pumpStatus) {
                    switchPump.setChecked(pumpStatus);
                    pref.saveBoolean("STATUS_POMPA", pumpStatus);
                    updatePumpStatusUI(tvPumpStatus, pumpStatus);
                }
            }

            @Override
            public void onError(String errorMessage) {
                // Tampilkan error ringan tanpa crash
                android.util.Log.w("SORA-DB", "Realtime DB error: " + errorMessage);
            }
        });

        // Emergency Stop panel
        LinearLayout emergencyPanel = findViewById(R.id.emergency_panel);
        applyScaleAnimation(emergencyPanel);
        emergencyPanel.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.confirm_emergency_title))
                    .setMessage(getString(R.string.confirm_emergency_msg))
                    .setPositiveButton(getString(R.string.btn_yes), (dialog, which) -> {
                        switchPump.setChecked(false);
                        dataManager.setPumpStatus(false);
                        dialog.dismiss();
                    })
                    .setNegativeButton(getString(R.string.btn_cancel), (d, w) -> d.dismiss())
                    .setCancelable(true)
                    .show();
        });

        // Bottom Navigation
        LinearLayout navHistory       = findViewById(R.id.nav_history);
        LinearLayout navNotifications = findViewById(R.id.nav_notifications);
        LinearLayout navSettings      = findViewById(R.id.nav_settings);

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Hentikan listener Realtime Database agar tidak memory leak
        if (dataManager != null) dataManager.stopListening();
    }

    // ─────────────────────────────────────────────
    //  Shows the "Tentang Aplikasi" dialog
    // ─────────────────────────────────────────────
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

    // ─────────────────────────────────────────────
    //  Logout via Firebase Auth
    // ─────────────────────────────────────────────
    private void logout() {
        new AlertDialog.Builder(this)
                .setTitle("Keluar")
                .setMessage("Apakah Anda yakin ingin keluar dari akun?")
                .setPositiveButton("Keluar", (dialog, which) -> {
                    if (dataManager != null) dataManager.stopListening();
                    FirebaseAuth.getInstance().signOut();
                    goToLogin();
                })
                .setNegativeButton("Batal", (dialog, which) -> dialog.dismiss())
                .setCancelable(true)
                .show();
    }

    // ─────────────────────────────────────────────
    //  Navigasi ke LoginActivity, bersihkan back stack
    // ─────────────────────────────────────────────
    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }

    private void updatePumpStatusUI(TextView tvPumpStatus, boolean isOn) {
        tvPumpStatus.setText(isOn ? getString(R.string.on) : getString(R.string.off));
        tvPumpStatus.setTextColor(Color.parseColor(isOn ? "#43A047" : "#EF5350"));
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