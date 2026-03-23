package com.example.itprojek;

import android.app.TimePickerDialog;
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
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

import java.util.Locale;

public class ScheduleActivity extends AppCompatActivity {

    private PrefManager pref;
    private boolean[] selectedDays = new boolean[6];
    private static final String[] DAY_KEYS = {
            "JADWAL_SEN", "JADWAL_SEL", "JADWAL_RAB",
            "JADWAL_KAM", "JADWAL_JUM", "JADWAL_SAB"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);
        hideSystemNavBar();
        applyStatusBarInsets();

        pref = new PrefManager(this);

        // Back button
        ImageView btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        // === Auto watering switch with persistence ===
        SwitchCompat switchAuto = findViewById(R.id.switch_auto_watering);
        // Restore FIRST
        boolean autoSaved = pref.getBoolean("AUTO_WATERING", false);
        switchAuto.setChecked(autoSaved);
        // THEN attach listener
        switchAuto.setOnCheckedChangeListener((buttonView, isChecked) -> {
            pref.saveBoolean("AUTO_WATERING", isChecked);
            Toast.makeText(this, "Penyiraman Otomatis: " + (isChecked ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
        });

        // === Time picker with persistence ===
        TextView tvTime = findViewById(R.id.tv_time);
        String savedTime = pref.getString("JADWAL_WAKTU", "07:00");
        tvTime.setText(savedTime);

        LinearLayout layoutTime = findViewById(R.id.layout_time);
        layoutTime.setOnClickListener(v -> {
            // Parse saved time for initial picker values
            String currentTime = tvTime.getText().toString();
            int hour = 7, min = 0;
            try {
                String[] parts = currentTime.split(":");
                hour = Integer.parseInt(parts[0]);
                min = Integer.parseInt(parts[1]);
            } catch (Exception ignored) {}

            int finalHour = hour;
            int finalMin = min;
            TimePickerDialog picker = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                String timeStr = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
                tvTime.setText(timeStr);
                pref.saveString("JADWAL_WAKTU", timeStr);
            }, finalHour, finalMin, true);
            picker.show();
        });

        // === Day selectors with persistence ===
        int[] dayIds = {R.id.day_sen, R.id.day_sel, R.id.day_rab, R.id.day_kam, R.id.day_jum, R.id.day_sab};
        for (int i = 0; i < dayIds.length; i++) {
            TextView dayView = findViewById(dayIds[i]);
            final int index = i;
            // Restore saved day selection (default: all true)
            selectedDays[index] = pref.getBoolean(DAY_KEYS[index], true);
            updateDayStyle(dayView, selectedDays[index]);
            dayView.setOnClickListener(v -> {
                selectedDays[index] = !selectedDays[index];
                pref.saveBoolean(DAY_KEYS[index], selectedDays[index]);
                updateDayStyle(dayView, selectedDays[index]);
            });
        }

        // === Duration seekbar with persistence ===
        TextView tvDurasi = findViewById(R.id.tv_durasi_value);
        SeekBar seekBarDurasi = findViewById(R.id.seekbar_durasi);
        // Restore saved duration (default: 10 menit → progress = 5, karena value = progress + 5)
        int savedDurasi = pref.getInt("JADWAL_DURASI", 5); // progress value
        seekBarDurasi.setProgress(savedDurasi);
        tvDurasi.setText((savedDurasi + 5) + " menit");

        seekBarDurasi.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int value = progress + 5;
                tvDurasi.setText(value + " menit");
                if (fromUser) {
                    pref.saveInt("JADWAL_DURASI", progress);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                // Save final value when user lifts finger
                pref.saveInt("JADWAL_DURASI", seekBar.getProgress());
            }
        });

        // Kalibrasi button — push navigation (slide)
        MaterialButton btnKalibrasi = findViewById(R.id.btn_kalibrasi);
        applyScaleAnimation(btnKalibrasi);
        btnKalibrasi.setOnClickListener(v -> {
            startActivity(new Intent(this, CalibrationActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
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

    private void updateDayStyle(TextView dayView, boolean selected) {
        if (selected) {
            dayView.setBackgroundResource(R.drawable.bg_day_selected);
            dayView.setTextColor(getResources().getColor(R.color.white, null));
        } else {
            dayView.setBackgroundResource(R.drawable.bg_day_unselected);
            dayView.setTextColor(getResources().getColor(R.color.day_unselected_text, null));
        }
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
