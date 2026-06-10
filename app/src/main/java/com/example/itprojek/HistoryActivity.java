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

public class HistoryActivity extends BaseDrawerActivity {

    private LinearLayout containerHistory;
    private LinearLayout containerSensor;
    private ScrollView   scrollWatering;
    private ScrollView   scrollSensor;
    private TextView     tabWatering;
    private TextView     tabSensorBtn;
    private View         tabIndicator;
    private static final String DB_URL     = "https://sora-app-9f18a-default-rtdb.asia-southeast1.firebasedatabase.app/";
    private static final String DEVICE_ID  = "ANG-123456";

    private DatabaseReference historyRef;
    private DatabaseReference soilRef;   // status/kelembapan
    private DatabaseReference waterRef;  // status/jarak_tangki
    private DatabaseReference pumpRef;   // status/pompa_status
    private DatabaseReference soraRiwayatRef; // riwayat dari ESP32
    private ValueEventListener historyListener;
    private ValueEventListener sensorHistoryListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        hideSystemNavBar();
        applyStatusBarInsets();
        setupDrawer(); // Inisialisasi sidebar dari BaseDrawerActivity

        containerHistory = findViewById(R.id.container_history);
        containerSensor  = findViewById(R.id.container_sensor);
        scrollWatering   = findViewById(R.id.scroll_watering);
        scrollSensor     = findViewById(R.id.scroll_sensor);
        tabWatering      = findViewById(R.id.tab_watering);
        tabSensorBtn     = findViewById(R.id.tab_sensor);
        tabIndicator     = findViewById(R.id.tab_indicator);

        // Firebase
        FirebaseDatabase db = FirebaseDatabase.getInstance(DB_URL);
        historyRef = db.getReference("history").child(DEVICE_ID);
        // Baca setiap field sensor dari SORA/status/ node
        DatabaseReference statusRoot = db.getReference("SORA/status");
        soilRef  = statusRoot.child("kelembapan");
        waterRef = statusRoot.child("jarak_tangki");
        pumpRef  = statusRoot.child("pompa_status");
        soraRiwayatRef = db.getReference("SORA/riwayat");

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

        // Tombol menu sudah ditangani oleh setupDrawer() di BaseDrawerActivity

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

    // Tab Penyiraman — riwayat penyiraman manual/otomatis (dari ESP32)
    private void fetchHistoryData() {
        historyListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (containerHistory == null) return;
                containerHistory.removeAllViews();
                
                if (!snapshot.exists() || !snapshot.hasChildren()) {
                    showEmptyState(containerHistory, "Belum ada riwayat penyiraman.");
                    return;
                }

                LayoutInflater inflater = LayoutInflater.from(HistoryActivity.this);
                
                List<DataSnapshot> entries = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    entries.add(ds);
                }
                Collections.reverse(entries); // urutkan terbaru di atas

                for (DataSnapshot ds : entries) {
                    int before = toInt(ds.child("kelembapan_sebelum").getValue());
                    int after  = toInt(ds.child("kelembapan_sesudah").getValue());
                    Object tObj = ds.child("waktu").getValue();
                    String time = tObj != null ? tObj.toString() : "-";

                    View card = inflater.inflate(R.layout.item_riwayat_card, containerHistory, false);
                    TextView tvDetail = card.findViewById(R.id.tv_history_detail);
                    if (tvDetail != null) {
                        tvDetail.setText(time + "  ·  Lembap: " + before + "% ➔ " + after + "%");
                    }
                    containerHistory.addView(card);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showEmptyState(containerHistory, "Akses ditolak (" + error.getCode() + ").");
            }
        };
        soraRiwayatRef.addValueEventListener(historyListener);
    }

    /** Helper toInt (duplikat dari FirebaseDataManager agar independen) */
    private int toInt(Object val) {
        if (val instanceof Long)    return ((Long) val).intValue();
        if (val instanceof Integer) return (Integer) val;
        if (val instanceof Double)  return ((Double) val).intValue();
        if (val instanceof String)  { try { return Integer.parseInt((String) val); } catch (Exception ignored) {} }
        return 0;
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

    // Tab Data Sensor — menampilkan SEMUA riwayat perubahan sensor dari history/{deviceId}
    private int  currentSoil  = 0;
    private int  currentWater = 0;
    private boolean currentPump = false;
    private DataSnapshot lastHistorySnapshot = null; // cache snapshot riwayat terakhir

    private void fetchSensorHistoryData() {
        // Listener real-time untuk nilai terkini (update kartu atas)
        soilRef.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) {
                currentSoil = toInt(s.getValue());
                updateCurrentSensorCard();
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });
        waterRef.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) {
                Object jarakObj = s.getValue();
                int jarak = jarakObj != null ? toInt(jarakObj) : 25; // MAX_JARAK = 25cm
                currentWater = Math.max(0, Math.min(100, (int) ((25 - jarak) * 100f / 25)));
                updateCurrentSensorCard();
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });
        pumpRef.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) {
                Object v = s.getValue();
                // pompa_status adalah String "MENYALA" atau "MATI"
                if (v instanceof String) currentPump = "MENYALA".equalsIgnoreCase((String) v);
                else if (v instanceof Boolean) currentPump = (Boolean) v;
                else if (v instanceof Long)    currentPump = ((Long) v) != 0;
                updateCurrentSensorCard();
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });

        // Listener riwayat perubahan dari history/{deviceId}
        sensorHistoryListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Hapus entri > 1 jam dari Firebase terlebih dahulu
                purgeOldSensorHistory(snapshot);
                buildSensorHistoryList(snapshot);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError e) {
                android.util.Log.e("SORA-DB", "sensorHistory cancelled: " + e.getMessage());
            }
        };
        historyRef.addValueEventListener(sensorHistoryListener);
    }

    /**
     * Hapus entri riwayat sensor yang lebih dari 1 jam dari Firebase.
     * Timestamp format: "yyyy-MM-dd HH:mm:ss"
     */
    private void purgeOldSensorHistory(DataSnapshot snapshot) {
        if (!snapshot.exists() || !snapshot.hasChildren()) return;
        long cutoff = System.currentTimeMillis() - (60L * 60 * 1000); // 1 jam lalu
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        for (DataSnapshot ds : snapshot.getChildren()) {
            Object tsObj = ds.child("timestamp").getValue();
            if (tsObj == null) continue;
            String ts = tsObj.toString();
            try {
                Date entryDate = sdf.parse(ts);
                if (entryDate != null && entryDate.getTime() < cutoff) {
                    // Hapus dari Firebase
                    ds.getRef().removeValue();
                }
            } catch (ParseException ignored) {}
        }
    }

    /** Kartu TERKINI di bagian atas tab Data Sensor — refresh saat sensor real-time berubah */
    private void updateCurrentSensorCard() {
        if (lastHistorySnapshot != null) {
            buildSensorHistoryList(lastHistorySnapshot);
        }
    }

    /** Bangun daftar riwayat sensor di containerSensor */
    private void buildSensorHistoryList(DataSnapshot snapshot) {
        lastHistorySnapshot = snapshot; // simpan untuk refresh real-time
        if (containerSensor == null) return;
        containerSensor.removeAllViews();

        // Kartu "Pembacaan Terkini" (dari listeners real-time)
        String statusNow;
        if (currentSoil < 30)      statusNow = "Kering";
        else if (currentSoil < 60) statusNow = "Normal";
        else                       statusNow = "Lembap";

        LayoutInflater inflater = LayoutInflater.from(HistoryActivity.this);

        View hdrNow = inflater.inflate(R.layout.item_riwayat_header, containerSensor, false);
        ((TextView) hdrNow.findViewById(R.id.tv_tanggal)).setText("Pembacaan Terkini");
        containerSensor.addView(hdrNow);

        View cardNow     = inflater.inflate(R.layout.item_sensor_card, containerSensor, false);
        TextView tvTimeN = cardNow.findViewById(R.id.tv_sensor_timestamp);
        TextView tvSoilN = cardNow.findViewById(R.id.tv_sensor_kelembapan);
        TextView tvWaterN= cardNow.findViewById(R.id.tv_sensor_air);
        TextView tvStatN = cardNow.findViewById(R.id.tv_sensor_status);
        View accentN     = cardNow.findViewById(R.id.view_sensor_accent);
        if (tvTimeN  != null) tvTimeN.setText("Saat ini");
        if (tvSoilN  != null) tvSoilN.setText(currentSoil + "%");
        if (tvWaterN != null) tvWaterN.setText(currentWater + "%");
        if (tvStatN  != null) tvStatN.setText("Status: " + statusNow + (currentPump ? "  ·  Pompa ON" : "  ·  Pompa OFF"));
        if (accentN  != null) {
            if ("Kering".equals(statusNow))      accentN.setBackgroundColor(Color.parseColor("#EF5350"));
            else if ("Lembap".equals(statusNow)) accentN.setBackgroundColor(Color.parseColor("#42A5F5"));
            else                                 accentN.setBackgroundColor(Color.parseColor("#43A047"));
        }
        containerSensor.addView(cardNow);

        // Riwayat perubahan dari history/{deviceId}
        if (!snapshot.exists() || !snapshot.hasChildren()) return;

        // Kumpulkan, filter hanya ≤ 1 jam, lalu balik (terbaru dulu)
        long cutoff = System.currentTimeMillis() - (60L * 60 * 1000);
        SimpleDateFormat sdfParse = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        List<DataSnapshot> entries = new ArrayList<>();
        for (DataSnapshot ds : snapshot.getChildren()) {
            Object tsObj = ds.child("timestamp").getValue();
            if (tsObj == null) continue;
            try {
                Date entryDate = sdfParse.parse(tsObj.toString());
                if (entryDate != null && entryDate.getTime() >= cutoff) {
                    entries.add(ds);
                }
            } catch (ParseException ignored) {
                entries.add(ds); // kalau format tidak dikenali, tetap tampilkan
            }
        }
        Collections.reverse(entries);

        // Kelompokkan per tanggal
        Map<String, List<DataSnapshot>> grouped = new java.util.LinkedHashMap<>();
        for (DataSnapshot ds : entries) {
            Object tsObj = ds.child("timestamp").getValue();
            String ts = tsObj != null ? tsObj.toString() : "";
            String dateKey = (ts.length() >= 10) ? ts.substring(0, 10) : "Unknown";
            if (!grouped.containsKey(dateKey)) grouped.put(dateKey, new ArrayList<>());
            grouped.get(dateKey).add(ds);
        }

        View hdrHist = inflater.inflate(R.layout.item_riwayat_header, containerSensor, false);
        ((TextView) hdrHist.findViewById(R.id.tv_tanggal)).setText("Riwayat Perubahan");
        containerSensor.addView(hdrHist);

        for (Map.Entry<String, List<DataSnapshot>> group : grouped.entrySet()) {
            // Sub-header tanggal
            View subHdr = inflater.inflate(R.layout.item_riwayat_header, containerSensor, false);
            TextView tvDate = subHdr.findViewById(R.id.tv_tanggal);
            tvDate.setTextSize(13);
            tvDate.setText("  " + formatDateIndo(group.getKey()));
            containerSensor.addView(subHdr);

            for (DataSnapshot ds : group.getValue()) {
                int soil   = toInt(ds.child("soil").getValue());
                int water  = toInt(ds.child("water").getValue());
                Object pObj = ds.child("pump").getValue();
                boolean pump = pObj instanceof Boolean && (Boolean) pObj;
                Object stObj = ds.child("status").getValue();
                String status = stObj != null ? stObj.toString() : "-";
                Object tsObj  = ds.child("timestamp").getValue();
                String ts     = tsObj != null ? tsObj.toString() : "";

                View card     = inflater.inflate(R.layout.item_sensor_card, containerSensor, false);
                TextView tvT  = card.findViewById(R.id.tv_sensor_timestamp);
                TextView tvSo = card.findViewById(R.id.tv_sensor_kelembapan);
                TextView tvWa = card.findViewById(R.id.tv_sensor_air);
                TextView tvSt = card.findViewById(R.id.tv_sensor_status);
                View accent   = card.findViewById(R.id.view_sensor_accent);

                if (tvT  != null) tvT.setText(formatTime(ts));
                if (tvSo != null) tvSo.setText(soil + "%");
                if (tvWa != null) tvWa.setText(water + "%");
                if (tvSt != null) tvSt.setText("Status: " + status + (pump ? "  ·  Pompa ON" : "  ·  Pompa OFF"));
                if (accent != null) {
                    if ("Kering".equalsIgnoreCase(status))      accent.setBackgroundColor(Color.parseColor("#EF5350"));
                    else if ("Lembap".equalsIgnoreCase(status)) accent.setBackgroundColor(Color.parseColor("#42A5F5"));
                    else                                        accent.setBackgroundColor(Color.parseColor("#43A047"));
                }
                containerSensor.addView(card);
            }
        }
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
        if (historyRef != null && historyListener != null) {
            historyRef.removeEventListener(historyListener);
        }
        if (historyRef != null && sensorHistoryListener != null) {
            historyRef.removeEventListener(sensorHistoryListener);
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
            View btnMenu = findViewById(R.id.btn_menu);
            if (btnMenu != null && btnMenu.getLayoutParams() instanceof ConstraintLayout.LayoutParams) {
                ConstraintLayout.LayoutParams p = (ConstraintLayout.LayoutParams) btnMenu.getLayoutParams();
                p.topMargin = statusH;
                btnMenu.setLayoutParams(p);
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

    /** Kembali ke MainActivity dan langsung buka sidebar */
    // Tidak dipakai lagi — sidebar sudah ada langsung di halaman ini

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemNavBar();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }
}
