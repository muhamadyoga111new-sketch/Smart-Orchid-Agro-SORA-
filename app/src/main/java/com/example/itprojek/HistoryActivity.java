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
    private DatabaseReference riwayatRef;
    private ValueEventListener historyListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        hideSystemNavBar();
        applyStatusBarInsets();

        containerHistory = findViewById(R.id.container_history);

        // Firebase Initialization
        FirebaseDatabase db = FirebaseDatabase.getInstance("https://sora-app-9f18a-default-rtdb.asia-southeast1.firebasedatabase.app/");
        // Fetch from ROOT, since PHPMyAdmin arrays can shuffle indexes
        riwayatRef = db.getReference();

        fetchHistoryData();

        // Back button
        ImageView btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        // Bottom Navigation — tab switch (crossfade)
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
                        String dur = item.getDurasi_aktual() != null ? item.getDurasi_aktual() : "0";
                        String start = item.getWaktu_mulai() != null && item.getWaktu_mulai().length() >= 5 
                                ? item.getWaktu_mulai().substring(0, 5) : "--:--";
                        String end = item.getWaktu_selesai() != null && item.getWaktu_selesai().length() >= 5 
                                ? item.getWaktu_selesai().substring(0, 5) : "--:--";
                        
                        tvDetail.setText(start + " - " + end + " · Durasi: " + dur + " menit");
                        
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (riwayatRef != null && historyListener != null) {
            riwayatRef.removeEventListener(historyListener);
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
