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
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.activity.OnBackPressedCallback;

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
    private MaterialSwitch      switchEmergency;
    private boolean             isEmergencyUpdating = false;
    private boolean             isPumpUpdating = false;

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

        // Handle Back Press with OnBackPressedDispatcher
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

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
                } else if (id == R.id.drawer_profile) {
                    startActivity(new Intent(this, ProfileActivity.class));
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                } else if (id == R.id.drawer_detail) {
                    startActivity(new Intent(this, DetailActivity.class));
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
            // They are constrained to TOP and BOTTOM of header_bg, so this centers them in the 60dp area
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

        // === Sensor cards → DetailActivity ===
        com.google.android.material.card.MaterialCardView cardMoisture = findViewById(R.id.card_moisture);
        com.google.android.material.card.MaterialCardView cardWater    = findViewById(R.id.card_water);
        android.view.View.OnClickListener goToDetail = v -> {
            startActivity(new Intent(this, DetailActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        };
        if (cardMoisture != null) cardMoisture.setOnClickListener(goToDetail);
        if (cardWater    != null) cardWater.setOnClickListener(goToDetail);

        // === Pump toggle — terhubung ke Firebase Realtime Database ===
        switchPump   = findViewById(R.id.switch_pump);
        tvPumpStatus = findViewById(R.id.tv_pump_status);

        // Nilai awal dari SharedPreferences
        boolean pumpSaved = pref.getBoolean("STATUS_POMPA", false);
        switchPump.setChecked(pumpSaved);
        updatePumpStatusUI(tvPumpStatus, pumpSaved);

        switchPump.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isPumpUpdating) return;

            String message = isChecked ? "Yakin ingin menyalakan pompa?" : "Yakin ingin mematikan pompa?";

            new AlertDialog.Builder(this)
                    .setTitle("Konfirmasi")
                    .setMessage(message)
                    .setPositiveButton("LANJUTKAN", (dialog, which) -> {
                        pref.saveBoolean("STATUS_POMPA", isChecked);
                        updatePumpStatusUI(tvPumpStatus, isChecked);
                        // Tulis perubahan pompa ke Firebase
                        dataManager.setPumpStatus(isChecked);
                        dialog.dismiss();
                    })
                    .setNegativeButton("BATAL", (dialog, which) -> {
                        isPumpUpdating = true;
                        switchPump.setChecked(!isChecked);
                        isPumpUpdating = false;
                        dialog.dismiss();
                    })
                    .setCancelable(false)
                    .show();
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

                View waterFill = findViewById(R.id.water_fill);
                View tankOutline = findViewById(R.id.tank_outline);
                if (waterFill != null && tankOutline != null) {
                    Runnable updateHeight = () -> {
                        waterFill.getLayoutParams().height = (int) (tankOutline.getHeight() * (waterLevel / 100f));
                        waterFill.requestLayout();
                    };
                    if (tankOutline.getHeight() > 0) {
                        updateHeight.run();
                    } else {
                        tankOutline.post(updateHeight);
                    }
                }

                // Sync status pompa dari Firebase ke UI
                if (switchPump.isChecked() != pumpStatus) {
                    isPumpUpdating = true;
                    switchPump.setChecked(pumpStatus);
                    isPumpUpdating = false;
                    pref.saveBoolean("STATUS_POMPA", pumpStatus);
                    updatePumpStatusUI(tvPumpStatus, pumpStatus);
                }
            }

            @Override
            public void onError(String errorMessage) {
                android.util.Log.w("SORA-DB", "Realtime DB error: " + errorMessage);
            }
        });

        // === Emergency Stop Section ===
        switchEmergency = findViewById(R.id.switch_emergency);
        switchEmergency.setTrackTintList(ContextCompat.getColorStateList(this, R.color.switch_track_emergency));
        
        switchEmergency.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isEmergencyUpdating) return;

            String message = isChecked ? "Yakin ingin mengaktifkan Emergency Stop?" : "Yakin ingin mematikan Emergency Stop?";

            new AlertDialog.Builder(this)
                    .setTitle("Konfirmasi")
                    .setMessage(message)
                    .setPositiveButton("LANJUTKAN", (dialog, which) -> {
                        // Jalankan logic emergency
                        if (isChecked) {
                            isPumpUpdating = true;
                            switchPump.setChecked(false);
                            isPumpUpdating = false;
                            dataManager.setPumpStatus(false);
                        }
                        dialog.dismiss();
                    })
                    .setNegativeButton("BATAL", (dialog, which) -> {
                        // Kembalikan ke state sebelumnya tanpa memicu listener lagi
                        isEmergencyUpdating = true;
                        switchEmergency.setChecked(!isChecked);
                        isEmergencyUpdating = false;
                        dialog.dismiss();
                    })
                    .setCancelable(false)
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
        if (dataManager != null) dataManager.stopListening();
    }

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

    private void logout() {
        new AlertDialog.Builder(this)
                .setTitle("Konfirmasi")
                .setMessage("Yakin ingin keluar dari akun?")
                .setPositiveButton("LANJUTKAN", (dialog, which) -> {
                    if (dataManager != null) dataManager.stopListening();
                    FirebaseAuth.getInstance().signOut();
                    goToLogin();
                })
                .setNegativeButton("BATAL", (dialog, which) -> dialog.dismiss())
                .setCancelable(true)
                .show();
    }

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