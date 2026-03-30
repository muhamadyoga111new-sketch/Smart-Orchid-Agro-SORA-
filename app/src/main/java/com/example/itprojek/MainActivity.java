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
        if (navView != null) {
            navView.setCheckedItem(R.id.drawer_home);
            navView.setNavigationItemSelectedListener(item -> {
                int id = item.getItemId();
                drawerLayout.closeDrawer(GravityCompat.START);

                drawerLayout.postDelayed(() -> {
                    if (id == R.id.drawer_history) {
                        startActivity(new Intent(this, HistoryActivity.class));
                    } else if (id == R.id.drawer_notifications) {
                        startActivity(new Intent(this, NotificationsActivity.class));
                    } else if (id == R.id.drawer_settings) {
                        startActivity(new Intent(this, SettingsActivity.class));
                    }
                }, 300);
                return true;
            });
        }

        // Hamburger menu button
        ImageView btnMenu = findViewById(R.id.btn_menu);
        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        }

        // Fix status bar overlap and header size
        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.statusBars());
                v.setPadding(systemBars.left, 0, systemBars.right, 0);

                View headerBg = findViewById(R.id.header_bg);
                if (headerBg != null) {
                    // Paksa tinggi header menjadi 60dp + status bar
                    int baseDp = 60;
                    int basePx = (int) (baseDp * getResources().getDisplayMetrics().density);
                    headerBg.getLayoutParams().height = basePx + systemBars.top;
                    headerBg.requestLayout();
                }
                return insets;
            });
        }

        // Navigation bell
        ImageView btnNotification = findViewById(R.id.btn_notification_top);
        if (btnNotification != null) {
            btnNotification.setOnClickListener(v -> startActivity(new Intent(this, NotificationsActivity.class)));
        }

        // === Pump toggle ===
        MaterialSwitch switchPump = findViewById(R.id.switch_pump);
        TextView tvPumpStatus = findViewById(R.id.tv_pump_status);

        if (switchPump != null && tvPumpStatus != null) {
            boolean pumpSaved = pref.getBoolean("STATUS_POMPA", false);
            switchPump.setChecked(pumpSaved);
            updatePumpStatusUI(tvPumpStatus, pumpSaved);

            switchPump.setOnCheckedChangeListener((buttonView, isChecked) -> {
                pref.saveBoolean("STATUS_POMPA", isChecked);
                updatePumpStatusUI(tvPumpStatus, isChecked);
            });
        }

        // Emergency Stop
        LinearLayout emergencyPanel = findViewById(R.id.emergency_panel);
        if (emergencyPanel != null) {
            emergencyPanel.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("Konfirmasi")
                        .setMessage("Matikan semua pompa?")
                        .setPositiveButton("Ya", (dialog, which) -> {
                            if (switchPump != null) switchPump.setChecked(false);
                        })
                        .setNegativeButton("Batal", null)
                        .show();
            });
        }

        // Bottom Navigation
        LinearLayout navHistory = findViewById(R.id.nav_history);
        LinearLayout navNotifications = findViewById(R.id.nav_notifications);
        LinearLayout navSettings = findViewById(R.id.nav_settings);

        if (navHistory != null) navHistory.setOnClickListener(v -> startActivity(new Intent(this, HistoryActivity.class)));
        if (navNotifications != null) navNotifications.setOnClickListener(v -> startActivity(new Intent(this, NotificationsActivity.class)));
        if (navSettings != null) navSettings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
    }

    private void updatePumpStatusUI(TextView tvPumpStatus, boolean isOn) {
        if (isOn) {
            tvPumpStatus.setText("ON");
            tvPumpStatus.setTextColor(Color.parseColor("#43A047"));
        } else {
            tvPumpStatus.setText("OFF");
            tvPumpStatus.setTextColor(Color.parseColor("#EF5350"));
        }
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private void hideSystemNavBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        }
    }
}