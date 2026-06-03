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

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class NotificationsActivity extends BaseDrawerActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        hideSystemNavBar();
        applyStatusBarInsets();
        setupDrawer(); // Inisialisasi sidebar dari BaseDrawerActivity

        // Tombol menu sudah ditangani oleh setupDrawer() di BaseDrawerActivity

        // Bottom Navigation — tab switch (crossfade)
        LinearLayout navHome = findViewById(R.id.nav_home);
        LinearLayout navHistory = findViewById(R.id.nav_history);
        LinearLayout navSettings = findViewById(R.id.nav_settings);

        applyScaleAnimation(navHome);
        applyScaleAnimation(navHistory);
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

        navSettings.setOnClickListener(v -> {
            startActivity(new Intent(this, SettingsActivity.class));
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        });

        // Fetch Notifications from DB
        fetchNotifications();
    }

    private void fetchNotifications() {
        LinearLayout container = findViewById(R.id.container_notifications);
        TextView tvEmpty = findViewById(R.id.tv_empty_notif);

        PrefManager pref = new PrefManager(this);
        java.util.Set<String> rawSet = pref.getStringSet("LOCAL_NOTIFICATIONS");

        // Buat list & urutkan terbaru di atas (format: timestamp|tipe|judul|pesan)
        java.util.List<String> entries = new java.util.ArrayList<>(rawSet);
        java.util.Collections.sort(entries, java.util.Collections.reverseOrder());

        container.removeAllViews();

        if (entries.isEmpty()) {
            if (tvEmpty != null) tvEmpty.setVisibility(View.VISIBLE);
            return;
        }
        if (tvEmpty != null) tvEmpty.setVisibility(View.GONE);

        for (String entry : entries) {
            String[] parts = entry.split("\\|", 4);
            if (parts.length < 4) continue;

            String timestamp = parts[0];
            String tipeAlert = parts[1];
            String judul     = parts[2];
            String pesan     = parts[3];

            View card = getLayoutInflater().inflate(R.layout.item_notifikasi, container, false);

            TextView tvTitle   = card.findViewById(R.id.tv_notif_title);
            TextView tvTime    = card.findViewById(R.id.tv_notif_time);
            TextView tvMessage = card.findViewById(R.id.tv_notif_message);
            ImageView ivIcon   = card.findViewById(R.id.iv_notif_icon);

            tvTitle.setText(judul);
            tvTime.setText(timestamp);
            tvMessage.setText(pesan);

            // Ikon & warna berdasarkan tipe_alert
            switch (tipeAlert) {
                case "Air Tangki Habis":
                case "Proteksi Pompa Air":
                    ivIcon.setImageResource(R.drawable.ic_warning);
                    ivIcon.setColorFilter(android.graphics.Color.parseColor("#EF5350"));
                    break;
                case "Tanah Kering":
                    ivIcon.setImageResource(R.drawable.ic_warning);
                    ivIcon.setColorFilter(android.graphics.Color.parseColor("#FF8F00"));
                    break;
                case "Tanah Lembab":
                    ivIcon.setImageResource(R.drawable.ic_info);
                    ivIcon.setColorFilter(android.graphics.Color.parseColor("#29B6F6"));
                    break;
                default:
                    ivIcon.setImageResource(R.drawable.ic_notification);
                    ivIcon.clearColorFilter();
                    break;
            }

            container.addView(card);
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
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemNavBar();
    }
}
