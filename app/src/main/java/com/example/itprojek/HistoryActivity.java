package com.example.itprojek;

import android.animation.ObjectAnimator;
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
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.LayoutInflater;
import android.graphics.Color;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import androidx.annotation.NonNull;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class HistoryActivity extends AppCompatActivity {

    private LinearLayout containerHistory;
    private LinearLayout containerSensor;
    private ScrollView   scrollWatering;
    private ScrollView   scrollSensor;
    private TextView     tabWatering;
    private TextView     tabSensorBtn;
    private View         tabIndicator;
    private DatabaseReference riwayatRef;
    private ValueEventListener historyListener;
    private ValueEventListener sensorHistoryListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        hideSystemNavBar();
        applyStatusBarInsets();

        containerHistory = findViewById(R.id.container_history);
        containerSensor  = findViewById(R.id.container_sensor);
        scrollWatering   = findViewById(R.id.scroll_watering);
        scrollSensor     = findViewById(R.id.scroll_sensor);
        tabWatering      = findViewById(R.id.tab_watering);
        tabSensorBtn     = findViewById(R.id.tab_sensor);
        tabIndicator     = findViewById(R.id.tab_indicator);

        // Firebase
        FirebaseDatabase db = FirebaseDatabase.getInstance("https://sora-app-9f18a-default-rtdb.asia-southeast1.firebasedatabase.app/");
        riwayatRef = db.getReference();

        fetchHistoryData();
        fetchSensorHistoryData();

        // Tab: Penyiraman
        tabWatering.setOnClickListener(v -> {
            scrollWatering.setVisibility(View.VISIBLE);
            scrollSensor.setVisibility(View.GONE);
            tabWatering.setTextColor(getResources().getColor(R.color.nav_active, getTheme()));
            tabWatering.setTypeface(null, android.graphics.Typeface.BOLD);
            tabSensorBtn.setTextColor(getResources().getColor(R.color.text_sub, getTheme()));
            tabSensorBtn.setTypeface(null, android.graphics.Typeface.NORMAL);
            tabIndicator.post(() ->
                ObjectAnimator.ofFloat(tabIndicator, "translationX", tabIndicator.getTranslationX(), 0f)
                        .setDuration(200).start());
        });

        // Tab: Data Sensor
        tabSensorBtn.setOnClickListener(v -> {
            scrollWatering.setVisibility(View.GONE);
            scrollSensor.setVisibility(View.VISIBLE);
            tabSensorBtn.setTextColor(getResources().getColor(R.color.nav_active, getTheme()));
            tabSensorBtn.setTypeface(null, android.graphics.Typeface.BOLD);
            tabWatering.setTextColor(getResources().getColor(R.color.text_sub, getTheme()));
            tabWatering.setTypeface(null, android.graphics.Typeface.NORMAL);
            tabIndicator.post(() ->
                ObjectAnimator.ofFloat(tabIndicator, "translationX", tabIndicator.getTranslationX(),
                        (float) tabIndicator.getWidth()).setDuration(200).start());
        });

        // Back button
        ImageView btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        // Bottom Navigation
        LinearLayout navHome = findViewById(R.id.nav_home);
        LinearLayout navNotifications = findViewById(R.id.nav_notifications);
        LinearLayout navSettings = findViewById(R.id.nav_settings);

        applyScaleAnimation(navHome);
        applyScaleAnimation(navNotifications);
        applyScaleAnimation(navSettings);

        navHome.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
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

    private void fetchHistoryData() {
        historyListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (containerHistory == null) return;
                containerHistory.removeAllViews(); // Clear dummy/old views

                List<Riwayat> riwayatList = new ArrayList<>();
                int totalFetched = 0;
                int validItems = 0;

                // Dynamically find the table where name == "riwayat_penyiraman"
                DataSnapshot targetDataNode = null;
                for (DataSnapshot tableNode : snapshot.getChildren()) {
                    DataSnapshot nameNode = tableNode.child("name");
                    if (nameNode.exists() && "riwayat_penyiraman".equals(nameNode.getValue(String.class))) {
                        targetDataNode = tableNode.child("data");
                        break;
                    }
                }

                if (targetDataNode == null || !targetDataNode.exists()) {
                    Toast.makeText(HistoryActivity.this, "Tabel riwayat_penyiraman tidak ditemukan di DB!", Toast.LENGTH_LONG).show();
                    return;
                }

                for (DataSnapshot ds : targetDataNode.getChildren()) {
                    totalFetched++;
                    Riwayat r = ds.getValue(Riwayat.class);
                    if (r != null && r.getTanggal() != null) {
                        riwayatList.add(r);
                        validItems++;
                    }
                }

                // Toast removed

                if (riwayatList.isEmpty()) {
                    // Optional: Show empty state text or just return
                    return;
                }

                // Group by Tanggal
                // We use a TreeMap with a custom comparator if we wanted it sorted automatically, 
                // but since Riwayat implements Comparable, let's just group them into a map, 
                // then sort the keys descending.
                Map<String, List<Riwayat>> groupedData = new HashMap<>();
                for (Riwayat r : riwayatList) {
                    if (r.getTanggal() == null) continue;
                    if (!groupedData.containsKey(r.getTanggal())) {
                        groupedData.put(r.getTanggal(), new ArrayList<>());
                    }
                    groupedData.get(r.getTanggal()).add(r);
                }

                // Sort the group keys descending
                List<String> sortedDates = new ArrayList<>(groupedData.keySet());
                Collections.sort(sortedDates, Collections.reverseOrder());

                LayoutInflater inflater = LayoutInflater.from(HistoryActivity.this);

                for (String dateKey : sortedDates) {
                    // Add Header Map
                    View headerView = inflater.inflate(R.layout.item_riwayat_header, containerHistory, false);
                    TextView tvTanggal = headerView.findViewById(R.id.tv_tanggal);
                    tvTanggal.setText(formatDateIndo(dateKey));
                    containerHistory.addView(headerView);

                    // Add items under this date
                    List<Riwayat> items = groupedData.get(dateKey);
                    Collections.sort(items); // Sort by Time descending

                    for (Riwayat item : items) {
                        View cardView = inflater.inflate(R.layout.item_riwayat_card, containerHistory, false);
                        
                        TextView tvDetail = cardView.findViewById(R.id.tv_history_detail);
                        TextView tvMoisture = cardView.findViewById(R.id.tv_history_moisture);
                        TextView tvWater = cardView.findViewById(R.id.tv_history_water);
                        
                        String dur = item.getDurasi_aktual() != null ? item.getDurasi_aktual() : "0";
                        String start = item.getWaktu_mulai() != null && item.getWaktu_mulai().length() >= 5 
                                ? item.getWaktu_mulai().substring(0, 5) : "--:--";
                        String end = item.getWaktu_selesai() != null && item.getWaktu_selesai().length() >= 5 
                                ? item.getWaktu_selesai().substring(0, 5) : "--:--";
                        
                        String beforeM = item.getKelembapan_sebelum() != null ? item.getKelembapan_sebelum() : "--";
                        String afterM = item.getKelembapan_sesudah() != null ? item.getKelembapan_sesudah() : "--";
                        
                        String beforeW = item.getLevel_air_sebelum() != null ? item.getLevel_air_sebelum() : "--";
                        String afterW = item.getLevel_air_sesudah() != null ? item.getLevel_air_sesudah() : "--";
                        
                        tvDetail.setText(start + " - " + end + " · Durasi: " + dur + " menit");
                        if (tvMoisture != null) {
                            tvMoisture.setText("Kelembapan: " + beforeM + "% ➔ " + afterM + "%");
                        }
                        if (tvWater != null) {
                            tvWater.setText("Ketinggian Air: " + beforeW + "% ➔ " + afterW + "%");
                        }
                        
                        containerHistory.addView(cardView);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(HistoryActivity.this, "Gagal memuat riwayat", Toast.LENGTH_SHORT).show();
            }
        };
        riwayatRef.addValueEventListener(historyListener);
    }

    private String formatDateIndo(String dateStr) {
        try {
            SimpleDateFormat sdfIn = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = sdfIn.parse(dateStr);
            SimpleDateFormat sdfOut = new SimpleDateFormat("dd MMMM yyyy", new Locale("id", "ID"));
            return sdfOut.format(date);
        } catch (ParseException e) {
            return dateStr; // fallback if invalid
        }
    }

    // ── Fetch riwayat data sensor dari Firebase ──────────────────────
    private void fetchSensorHistoryData() {
        sensorHistoryListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (containerSensor == null) return;
                containerSensor.removeAllViews();

                DataSnapshot dataSensorTable = null;
                for (DataSnapshot tableNode : snapshot.getChildren()) {
                    DataSnapshot nameNode = tableNode.child("name");
                    if (nameNode.exists() && "data_sensor".equals(nameNode.getValue(String.class))) {
                        dataSensorTable = tableNode.child("data");
                        break;
                    }
                }

                if (dataSensorTable == null || !dataSensorTable.exists()) {
                    showEmptyState(containerSensor, "Belum ada data sensor tersimpan.");
                    return;
                }

                List<DataSensor> sensorList = new ArrayList<>();
                for (DataSnapshot ds : dataSensorTable.getChildren()) {
                    DataSensor sensor = ds.getValue(DataSensor.class);
                    if (sensor != null && "DEV001".equals(sensor.id_perangkat)) {
                        sensorList.add(sensor);
                    }
                }

                if (sensorList.isEmpty()) {
                    showEmptyState(containerSensor, "Belum ada data sensor tersimpan.");
                    return;
                }

                // Sort terbaru dulu
                Collections.sort(sensorList, (a, b) -> {
                    if (a.timestamp == null || b.timestamp == null) return 0;
                    return b.timestamp.compareTo(a.timestamp);
                });

                // Kelompokkan per tanggal (10 karakter pertama timestamp)
                Map<String, List<DataSensor>> grouped = new HashMap<>();
                for (DataSensor s : sensorList) {
                    String dateKey = (s.timestamp != null && s.timestamp.length() >= 10)
                            ? s.timestamp.substring(0, 10) : "Unknown";
                    if (!grouped.containsKey(dateKey)) grouped.put(dateKey, new ArrayList<>());
                    grouped.get(dateKey).add(s);
                }

                List<String> sortedDates = new ArrayList<>(grouped.keySet());
                Collections.sort(sortedDates, Collections.reverseOrder());

                LayoutInflater inflater = LayoutInflater.from(HistoryActivity.this);

                for (String dateKey : sortedDates) {
                    // Header tanggal
                    View headerView = inflater.inflate(R.layout.item_riwayat_header, containerSensor, false);
                    TextView tvTanggal = headerView.findViewById(R.id.tv_tanggal);
                    tvTanggal.setText(formatDateIndo(dateKey));
                    containerSensor.addView(headerView);

                    for (DataSensor s : grouped.get(dateKey)) {
                        View cardView = inflater.inflate(R.layout.item_sensor_card, containerSensor, false);

                        TextView tvTime       = cardView.findViewById(R.id.tv_sensor_timestamp);
                        TextView tvKelembapan = cardView.findViewById(R.id.tv_sensor_kelembapan);
                        TextView tvAir        = cardView.findViewById(R.id.tv_sensor_air);
                        TextView tvStatus     = cardView.findViewById(R.id.tv_sensor_status);
                        View     accent       = cardView.findViewById(R.id.view_sensor_accent);

                        tvTime.setText(formatTime(s.timestamp));
                        tvKelembapan.setText(s.kelembapan_tanah != null ? s.kelembapan_tanah + "%" : "--");
                        tvAir.setText(s.level_air != null ? s.level_air + "%" : "--");

                        String status = (s.status_kelembapan != null) ? s.status_kelembapan : "-";
                        tvStatus.setText("Status: " + status);

                        if (accent != null) {
                            if ("kering".equalsIgnoreCase(status)) {
                                accent.setBackgroundColor(Color.parseColor("#EF5350"));
                            } else if ("lembap".equalsIgnoreCase(status) || "lembab".equalsIgnoreCase(status)) {
                                accent.setBackgroundColor(Color.parseColor("#42A5F5"));
                            } else {
                                accent.setBackgroundColor(Color.parseColor("#43A047"));
                            }
                        }

                        containerSensor.addView(cardView);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showEmptyState(containerSensor, "Gagal memuat data sensor.");
            }
        };
        riwayatRef.addValueEventListener(sensorHistoryListener);
    }

    private void showEmptyState(LinearLayout container, String message) {
        if (container == null) return;
        container.removeAllViews();
        TextView tv = new TextView(this);
        tv.setText(message);
        tv.setTextColor(Color.parseColor("#9E9E9E"));
        tv.setTextSize(14);
        tv.setPadding(0, 48, 0, 0);
        tv.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
        container.addView(tv);
    }

    /** Ambil jam:menit dari timestamp "yyyy-MM-dd HH:mm:ss" atau "yyyy-MM-ddTHH:mm:ss". */
    private String formatTime(String timestamp) {
        if (timestamp == null || timestamp.length() < 16) return "--:--";
        String timePart = timestamp.contains("T")
                ? timestamp.substring(11, 16)
                : timestamp.substring(11, 16);
        return timePart;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (riwayatRef != null && historyListener != null) {
            riwayatRef.removeEventListener(historyListener);
        }
        if (riwayatRef != null && sensorHistoryListener != null) {
            riwayatRef.removeEventListener(sensorHistoryListener);
        }
    }

    private void applyStatusBarInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(getWindow().getDecorView(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            int statusH  = systemBars.top;
            int base60px = (int) (60 * getResources().getDisplayMetrics().density);

            View headerBg = findViewById(R.id.header_bg);
            if (headerBg != null) {
                headerBg.getLayoutParams().height = base60px + statusH;
                headerBg.requestLayout();
            }
            View btnBack = findViewById(R.id.btn_back);
            if (btnBack != null && btnBack.getLayoutParams() instanceof ConstraintLayout.LayoutParams) {
                ConstraintLayout.LayoutParams p = (ConstraintLayout.LayoutParams) btnBack.getLayoutParams();
                p.topMargin = statusH;
                btnBack.setLayoutParams(p);
            }
            View btnSync = findViewById(R.id.btn_sync);
            if (btnSync != null && btnSync.getLayoutParams() instanceof ConstraintLayout.LayoutParams) {
                ConstraintLayout.LayoutParams p = (ConstraintLayout.LayoutParams) btnSync.getLayoutParams();
                p.topMargin = statusH;
                btnSync.setLayoutParams(p);
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
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
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
