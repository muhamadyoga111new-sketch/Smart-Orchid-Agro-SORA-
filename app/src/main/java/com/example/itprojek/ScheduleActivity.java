package com.example.itprojek;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.Locale;

public class ScheduleActivity extends AppCompatActivity {

    private PrefManager pref;
    private final boolean[] selectedDays = new boolean[6];
    private static final String[] DAY_KEYS = {
            "JADWAL_SEN", "JADWAL_SEL", "JADWAL_RAB",
            "JADWAL_KAM", "JADWAL_JUM", "JADWAL_SAB"
    };

    private LinearLayout layoutContent;
    private TextView tvSwitchStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);
        hideSystemNavBar();
        applyStatusBarInsets();

        pref = new PrefManager(this);
        layoutContent = findViewById(R.id.layoutContent);
        tvSwitchStatus = findViewById(R.id.tv_switch_status);

        // Back button
        final ImageView btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        // === Auto watering switch with persistence & state management ===
        final MaterialSwitch switchAuto = findViewById(R.id.switch_auto_watering);
        final boolean autoSaved = pref.getBoolean("AUTO_WATERING", true);
        
        // Initial state
        switchAuto.setChecked(autoSaved);
        updateUIState(autoSaved);

        switchAuto.setOnCheckedChangeListener((buttonView, isChecked) -> {
            pref.saveBoolean("AUTO_WATERING", isChecked);
            updateUIState(isChecked);
            final String status = isChecked ? getString(R.string.on) : getString(R.string.off);
            Toast.makeText(this, getString(R.string.auto_watering_status, status), Toast.LENGTH_SHORT).show();
        });

        // === Time picker with persistence ===
        final TextView tvTime = findViewById(R.id.tv_time);
        final String savedTime = pref.getString("JADWAL_WAKTU", "07:00");
        tvTime.setText(savedTime);

        final LinearLayout layoutTime = findViewById(R.id.layout_time);
        layoutTime.setOnClickListener(v -> {
            final String currentTime = tvTime.getText().toString();
            int hour = 7, min = 0;
            try {
                final String[] parts = currentTime.split(":");
                hour = Integer.parseInt(parts[0]);
                min = Integer.parseInt(parts[1]);
            } catch (Exception ignored) {}

            final TimePickerDialog picker = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                final String timeStr = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
                tvTime.setText(timeStr);
                pref.saveString("JADWAL_WAKTU", timeStr);
            }, hour, min, true);
            picker.show();
        });

        // === Day selectors with persistence ===
        final int[] dayIds = {R.id.day_sen, R.id.day_sel, R.id.day_rab, R.id.day_kam, R.id.day_jum, R.id.day_sab};
        for (int i = 0; i < dayIds.length; i++) {
            final TextView dayView = findViewById(dayIds[i]);
            final int index = i;
            selectedDays[index] = pref.getBoolean(DAY_KEYS[index], true);
            updateDayStyle(dayView, selectedDays[index]);
            dayView.setOnClickListener(v -> {
                selectedDays[index] = !selectedDays[index];
                pref.saveBoolean(DAY_KEYS[index], selectedDays[index]);
                updateDayStyle(dayView, selectedDays[index]);
            });
        }

        // === Duration seekbar with persistence ===
        final TextView tvDurasi = findViewById(R.id.tv_durasi_value);
        final SeekBar seekBarDurasi = findViewById(R.id.seekbar_durasi);
        final int savedDurasi = pref.getInt("JADWAL_DURASI", 5);
        seekBarDurasi.setProgress(savedDurasi);
        tvDurasi.setText(getString(R.string.durasi_value_format, savedDurasi + 5));

        seekBarDurasi.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                final int value = progress + 5;
                tvDurasi.setText(getString(R.string.durasi_value_format, value));
                if (fromUser) {
                    pref.saveInt("JADWAL_DURASI", progress);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                pref.saveInt("JADWAL_DURASI", seekBar.getProgress());
            }
        });

        // Kalibrasi button
        final MaterialButton btnKalibrasi = findViewById(R.id.btn_kalibrasi);
        applyScaleAnimation(btnKalibrasi);
        btnKalibrasi.setOnClickListener(v -> {
            startActivity(new Intent(this, CalibrationActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        setupBottomNav();
    }

    private void updateUIState(boolean isChecked) {
        layoutContent.setAlpha(isChecked ? 1.0f : 0.5f);
        setEnabledRecursive(layoutContent, isChecked);
        tvSwitchStatus.setText(isChecked ? getString(R.string.auto_watering_on) : getString(R.string.auto_watering_off));
    }

    private void setEnabledRecursive(@NonNull View view, boolean enabled) {
        view.setEnabled(enabled);
        if (view instanceof ViewGroup) {
            final ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                final View child = group.getChildAt(i);
                if (child != null) {
                    setEnabledRecursive(child, enabled);
                }
            }
        }
    }

    private void applyStatusBarInsets() {
        final View headerBg = findViewById(R.id.header_bg);
        ViewCompat.setOnApplyWindowInsetsListener(headerBg, (v, insets) -> {
            final Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            v.getLayoutParams().height = (int) (60 * getResources().getDisplayMetrics().density) + systemBars.top;
            v.setPadding(0, systemBars.top, 0, 0);
            v.requestLayout();
            return insets;
        });
    }

    private void updateDayStyle(@NonNull TextView dayView, boolean selected) {
        if (selected) {
            dayView.setBackgroundResource(R.drawable.bg_day_selected);
            dayView.setTextColor(getResources().getColor(R.color.white, null));
        } else {
            dayView.setBackgroundResource(R.drawable.bg_day_unselected);
            dayView.setTextColor(getResources().getColor(R.color.day_unselected_text, null));
        }
    }

    private void setupBottomNav() {
        final LinearLayout navHome = findViewById(R.id.nav_home);
        final LinearLayout navHistory = findViewById(R.id.nav_history);
        final LinearLayout navNotifications = findViewById(R.id.nav_notifications);
        final LinearLayout navSettings = findViewById(R.id.nav_settings);

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

    private void applyScaleAnimation(@NonNull View view) {
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    final Animation sd = AnimationUtils.loadAnimation(this, R.anim.scale_down);
                    sd.setFillAfter(true);
                    v.startAnimation(sd);
                    break;
                case MotionEvent.ACTION_UP:
                    v.performClick();
                case MotionEvent.ACTION_CANCEL:
                    final Animation su = AnimationUtils.loadAnimation(this, R.anim.scale_up);
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
            final WindowInsetsController controller = getWindow().getInsetsController();
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
