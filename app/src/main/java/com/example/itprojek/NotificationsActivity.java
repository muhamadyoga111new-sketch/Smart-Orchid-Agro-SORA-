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

public class NotificationsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        hideSystemNavBar();
        applyStatusBarInsets();

        // Back button
        ImageView btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

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
        com.google.firebase.database.FirebaseDatabase db = com.google.firebase.database.FirebaseDatabase.getInstance("https://sora-app-9f18a-default-rtdb.asia-southeast1.firebasedatabase.app/");
        db.getReference().addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                com.google.firebase.database.DataSnapshot notifTable = null;
                for (com.google.firebase.database.DataSnapshot tableNode : snapshot.getChildren()) {
                    com.google.firebase.database.DataSnapshot nameNode = tableNode.child("name");
                    if (nameNode.exists() && "notifikasi".equals(nameNode.getValue(String.class))) {
                        notifTable = tableNode.child("data");
                        break;
                    }
                }

                if (notifTable != null && notifTable.exists()) {
                    java.util.List<Notifikasi> dataList = new java.util.ArrayList<>();
                    for (com.google.firebase.database.DataSnapshot ds : notifTable.getChildren()) {
                        Notifikasi notif = ds.getValue(Notifikasi.class);
                        if (notif != null && "USR002".equals(notif.id_pengguna)) {
                            dataList.add(notif);
                        }
                    }

                    java.util.Collections.sort(dataList);

                    container.removeAllViews();
                    for (Notifikasi n : dataList) {
                        View card = getLayoutInflater().inflate(R.layout.item_notifikasi, container, false);

                        TextView tvTitle = card.findViewById(R.id.tv_notif_title);
                        TextView tvTime = card.findViewById(R.id.tv_notif_time);
                        TextView tvMessage = card.findViewById(R.id.tv_notif_message);
                        ImageView ivIcon = card.findViewById(R.id.iv_notif_icon);

                        tvTitle.setText(n.judul != null ? n.judul : "Notifikasi");
                        tvTime.setText(n.timestamp != null ? n.timestamp : "");
                        tvMessage.setText(n.pesan != null ? n.pesan : "");

                        // Dynamic icon/colors based on type
                        if ("Tanah Kering".equals(n.tipe_alert) || "Tangki Rendah".equals(n.tipe_alert)) {
                            ivIcon.setImageResource(R.drawable.ic_warning);
                            ivIcon.setColorFilter(android.graphics.Color.parseColor("#EF5350"));
                        } else if ("Kelembapan Tinggi".equals(n.tipe_alert)) {
                            ivIcon.setImageResource(R.drawable.ic_info);
                            ivIcon.setColorFilter(android.graphics.Color.parseColor("#29B6F6"));
                        } else {
                            ivIcon.setImageResource(R.drawable.ic_notification);
                        }

                        container.addView(card);
                    }
                }
            }

            @Override
            public void onCancelled(com.google.firebase.database.DatabaseError error) {
            }
        });
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
