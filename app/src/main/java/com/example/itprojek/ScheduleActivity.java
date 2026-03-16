package com.example.itprojek;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.google.android.material.button.MaterialButton;

import java.util.Locale;

public class ScheduleActivity extends AppCompatActivity {

    private final boolean[] selectedDays = {true, true, true, true, true, true};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);
        hideSystemNavBar();

        // Back button
        ImageView btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        // Auto watering switch
        SwitchCompat switchAuto = findViewById(R.id.switch_auto_watering);
        switchAuto.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Toast.makeText(this, "Penyiraman Otomatis: " + (isChecked ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
        });

        // Time picker
        TextView tvTime = findViewById(R.id.tv_time);
        LinearLayout layoutTime = findViewById(R.id.layout_time);
        layoutTime.setOnClickListener(v -> {
            TimePickerDialog picker = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                tvTime.setText(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute));
            }, 7, 0, true);
            picker.show();
        });

        // Day selectors
        int[] dayIds = {R.id.day_sen, R.id.day_sel, R.id.day_rab, R.id.day_kam, R.id.day_jum, R.id.day_sab};
        for (int i = 0; i < dayIds.length; i++) {
            TextView dayView = findViewById(dayIds[i]);
            final int index = i;
            updateDayStyle(dayView, selectedDays[index]);
            dayView.setOnClickListener(v -> {
                selectedDays[index] = !selectedDays[index];
                updateDayStyle(dayView, selectedDays[index]);
            });
        }

        // Duration seekbar
        TextView tvDurasi = findViewById(R.id.tv_durasi_value);
        SeekBar seekBarDurasi = findViewById(R.id.seekbar_durasi);
        seekBarDurasi.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int value = progress + 5; // min 5, max 30
                tvDurasi.setText(value + " menit");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Kalibrasi button
        MaterialButton btnKalibrasi = findViewById(R.id.btn_kalibrasi);
        btnKalibrasi.setOnClickListener(v ->
                startActivity(new Intent(this, CalibrationActivity.class)));

        // Bottom Navigation
        setupBottomNav();
    }

    private void updateDayStyle(TextView dayView, boolean selected) {
        if (selected) {
            dayView.setBackgroundResource(R.drawable.bg_card);
            dayView.setTextColor(getResources().getColor(R.color.white, null));
        } else {
            dayView.setBackgroundResource(R.drawable.bg_emergency);
            dayView.setTextColor(getResources().getColor(R.color.text_sub, null));
        }
    }

    private void setupBottomNav() {
        LinearLayout navHome = findViewById(R.id.nav_home);
        LinearLayout navHistory = findViewById(R.id.nav_history);
        LinearLayout navNotifications = findViewById(R.id.nav_notifications);
        LinearLayout navSettings = findViewById(R.id.nav_settings);

        navHome.setOnClickListener(v -> { startActivity(new Intent(this, MainActivity.class)); finish(); });
        navHistory.setOnClickListener(v -> { startActivity(new Intent(this, HistoryActivity.class)); finish(); });
        navNotifications.setOnClickListener(v -> { startActivity(new Intent(this, NotificationsActivity.class)); finish(); });
        navSettings.setOnClickListener(v -> { startActivity(new Intent(this, SettingsActivity.class)); finish(); });
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
