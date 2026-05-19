package com.example.itprojek;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.activity.OnBackPressedCallback;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

public class ProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        hideSystemNavBar();

        // Handle Back Press with OnBackPressedDispatcher
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            }
        });

        // Handle StatusBar insets for the header
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

        // Setup Back Button
        ImageView btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });

        // Setup Firebase User Info based on PHPMyAdmin imported database format
        TextView tvUserEmail = findViewById(R.id.tv_user_email);
        TextView tvUserId = findViewById(R.id.tv_user_id);
        TextView tvUserName = findViewById(R.id.tv_user_name);
        TextView tvUserStatus = findViewById(R.id.tv_user_status);

        String targetUserId = "USR002"; // Fallback Test ID
        tvUserId.setText(targetUserId);

        FirebaseDatabase.getInstance("https://sora-app-9f18a-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference().addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                com.google.firebase.database.DataSnapshot penggunaTable = null;
                for (com.google.firebase.database.DataSnapshot tableNode : snapshot.getChildren()) {
                    com.google.firebase.database.DataSnapshot nameNode = tableNode.child("name");
                    if (nameNode.exists() && "pengguna".equals(nameNode.getValue(String.class))) {
                        penggunaTable = tableNode.child("data");
                        break;
                    }
                }

                if (penggunaTable != null && penggunaTable.exists()) {
                    for (com.google.firebase.database.DataSnapshot ds : penggunaTable.getChildren()) {
                        Pengguna user = ds.getValue(Pengguna.class);
                        if (user != null && targetUserId.equals(user.id_pengguna)) {
                            tvUserEmail.setText(user.email != null ? user.email : "No Email");
                            if (tvUserName != null) tvUserName.setText(user.nama_lengkap != null ? user.nama_lengkap : "Anggota SORA");
                            if (tvUserStatus != null) tvUserStatus.setText(user.status_akun != null ? user.status_akun : "-");
                            break;
                        }
                    }
                }
            }

            @Override
            public void onCancelled(com.google.firebase.database.DatabaseError error) {
                // Log error
            }
        });

        // Setup Custom Logout Button
        MaterialCardView btnLogout = findViewById(R.id.card_logout);
        btnLogout.setOnClickListener(v -> logout());

        // Setup Bottom Navigation
        setupBottomNavigation();
    }

    private void setupBottomNavigation() {
        LinearLayout navHome = findViewById(R.id.nav_home);
        LinearLayout navHistory = findViewById(R.id.nav_history);
        LinearLayout navNotifications = findViewById(R.id.nav_notifications);
        LinearLayout navSettings = findViewById(R.id.nav_settings);

        navHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
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

    private void logout() {
        new AlertDialog.Builder(this)
                .setTitle("Konfirmasi")
                .setMessage("Yakin ingin keluar dari akun?")
                .setPositiveButton("LANJUTKAN", (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                    finish();
                })
                .setNegativeButton("BATAL", (dialog, which) -> dialog.dismiss())
                .setCancelable(true)
                .show();
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
