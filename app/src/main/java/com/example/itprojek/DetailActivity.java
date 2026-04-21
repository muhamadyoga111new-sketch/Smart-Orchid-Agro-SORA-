package com.example.itprojek;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DetailActivity extends AppCompatActivity {

    // ── Konstanta ──
    private static final String DEVICE_ID = "ANG-123456";

    // ── Firebase ──
    private FirebaseDataManager dataManager;

    // ── Views ──
    private TextView    tvConnectionStatus, tvLastUpdated;
    private TextView    tvMoistureDetail, tvSoilStatus;
    private TextView    tvWaterDetail, tvWaterStatus;
    private ProgressBar pbMoisture, pbWater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        hideSystemNavBar();
        applyStatusBarInsets();

        // ── Validasi sesi ──
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
            return;
        }

        // ── Bind Views ──
        tvConnectionStatus = findViewById(R.id.tv_connection_status);
        tvLastUpdated      = findViewById(R.id.tv_last_updated);
        tvMoistureDetail   = findViewById(R.id.tv_moisture_detail);
        tvSoilStatus       = findViewById(R.id.tv_soil_status);
        tvWaterDetail      = findViewById(R.id.tv_water_detail);
        tvWaterStatus      = findViewById(R.id.tv_water_status);
        pbMoisture         = findViewById(R.id.pb_moisture);
        pbWater            = findViewById(R.id.pb_water);

        // ── Tombol back ──
        ImageView btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });

        // ── Firebase Realtime Data ──
        dataManager = new FirebaseDataManager(DEVICE_ID);
        dataManager.listenSensorData(new FirebaseDataManager.SensorListener() {
            @Override
            public void onSensorUpdated(int soilMoisture, int waterLevel, boolean pumpStatus) {
                runOnUiThread(() -> {
                    updateSoilMoisture(soilMoisture);
                    updateWaterLevel(waterLevel);
                    updateTimestamp();
                    setConnectionOnline();
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> setConnectionOffline(errorMessage));
            }
        });

        // ── Bottom Navigation ──
        LinearLayout navHome          = findViewById(R.id.nav_home);
        LinearLayout navHistory       = findViewById(R.id.nav_history);
        LinearLayout navNotifications = findViewById(R.id.nav_notifications);
        LinearLayout navSettings      = findViewById(R.id.nav_settings);

        navHome.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        });
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

    // ─────────────────────────────────────────────
    //  Update UI: Kelembapan Tanah
    // ─────────────────────────────────────────────
    private void updateSoilMoisture(int value) {
        tvMoistureDetail.setText(String.valueOf(value));
        pbMoisture.setProgress(value);

        if (value < 30) {
            tvSoilStatus.setText(getString(R.string.kering));
            tvSoilStatus.setTextColor(Color.parseColor("#EF5350"));
            pbMoisture.setProgressTintList(
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#EF5350")));
        } else if (value <= 70) {
            tvSoilStatus.setText(getString(R.string.normal));
            tvSoilStatus.setTextColor(Color.parseColor("#66BB6A"));
            pbMoisture.setProgressTintList(
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#66BB6A")));
        } else {
            tvSoilStatus.setText(getString(R.string.lembap));
            tvSoilStatus.setTextColor(Color.parseColor("#42A5F5"));
            pbMoisture.setProgressTintList(
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#42A5F5")));
        }
    }

    // ─────────────────────────────────────────────
    //  Update UI: Level Air
    // ─────────────────────────────────────────────
    private void updateWaterLevel(int value) {
        tvWaterDetail.setText(String.valueOf(value));
        pbWater.setProgress(value);

        if (value < 20) {
            tvWaterStatus.setText(getString(R.string.air_rendah));
            tvWaterStatus.setTextColor(Color.parseColor("#EF5350"));
            pbWater.setProgressTintList(
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#EF5350")));
        } else {
            tvWaterStatus.setText(getString(R.string.air_normal));
            tvWaterStatus.setTextColor(Color.parseColor("#42A5F5"));
            pbWater.setProgressTintList(
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#42A5F5")));
        }
    }

    // ─────────────────────────────────────────────
    //  Timestamp update terakhir
    // ─────────────────────────────────────────────
    private void updateTimestamp() {
        String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        tvLastUpdated.setText("Update: " + time);
    }

    // ─────────────────────────────────────────────
    //  Status koneksi
    // ─────────────────────────────────────────────
    private void setConnectionOnline() {
        tvConnectionStatus.setText(getString(R.string.status_connected));
        tvConnectionStatus.setTextColor(Color.parseColor("#43A047"));
    }

    private void setConnectionOffline(String reason) {
        tvConnectionStatus.setText("Offline");
        tvConnectionStatus.setTextColor(Color.parseColor("#EF5350"));
        android.util.Log.w("SORA-Detail", "Firebase error: " + reason);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dataManager != null) dataManager.stopListening();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemNavBar();
    }

    // ─────────────────────────────────────────────
    //  Sesuaikan title & back button dengan status bar
    // ─────────────────────────────────────────────
    private void applyStatusBarInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(getWindow().getDecorView(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            int statusH  = systemBars.top;
            
            View headerBg = findViewById(R.id.header_bg);
            if (headerBg != null) {
                int base60px = (int) (60 * getResources().getDisplayMetrics().density);
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

    // ─────────────────────────────────────────────
    //  Sembunyikan navigation bar sistem
    // ─────────────────────────────────────────────
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
