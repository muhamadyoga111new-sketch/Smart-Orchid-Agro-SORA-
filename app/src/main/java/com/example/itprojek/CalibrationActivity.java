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
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

public class CalibrationActivity extends AppCompatActivity {

    private PrefManager pref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calibration);
        hideSystemNavBar();
        applyStatusBarInsets();

        pref = new PrefManager(this);

        // Back button
        ImageView btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        // === Kering seekbar with persistence ===
        SeekBar seekKering = findViewById(R.id.seekbar_kering);
        TextView tvKering = findViewById(R.id.tv_kering_value);
        int savedKering = pref.getInt("KALIBRASI_KERING", 30);
        seekKering.setProgress(savedKering);
        tvKering.setText(savedKering + "%");

        seekKering.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvKering.setText(progress + "%");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                pref.saveInt("KALIBRASI_KERING", seekBar.getProgress());
            }
        });

        // === Normal seekbar with persistence ===
        SeekBar seekNormal = findViewById(R.id.seekbar_normal);
        TextView tvNormal = findViewById(R.id.tv_normal_value);
        int savedNormal = pref.getInt("KALIBRASI_NORMAL", 60);
        seekNormal.setProgress(savedNormal);
        tvNormal.setText(savedNormal + "%");

        seekNormal.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvNormal.setText(progress + "%");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                pref.saveInt("KALIBRASI_NORMAL", seekBar.getProgress());
            }
        });

        // === Lembap seekbar with persistence ===
        SeekBar seekLembap = findViewById(R.id.seekbar_lembap);
        TextView tvLembap = findViewById(R.id.tv_lembap_value);
        int savedLembap = pref.getInt("KALIBRASI_LEMBAP", 80);
        seekLembap.setProgress(savedLembap);
        tvLembap.setText(savedLembap + "%");

        seekLembap.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvLembap.setText(progress + "%");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                pref.saveInt("KALIBRASI_LEMBAP", seekBar.getProgress());
            }
        });

        // Save button — saves all 3 seekbars explicitly + toast
        MaterialButton btnSave = findViewById(R.id.btn_save);
        applyScaleAnimation(btnSave);
        btnSave.setOnClickListener(v -> {
            pref.saveInt("KALIBRASI_KERING", seekKering.getProgress());
            pref.saveInt("KALIBRASI_NORMAL", seekNormal.getProgress());
            pref.saveInt("KALIBRASI_LEMBAP", seekLembap.getProgress());
            Toast.makeText(this, getString(R.string.saved_toast), Toast.LENGTH_SHORT).show();
            finish();
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
