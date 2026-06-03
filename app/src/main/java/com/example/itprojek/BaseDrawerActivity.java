package com.example.itprojek;

import android.content.Intent;
import android.os.Build;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.ImageView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;

/**
 * BaseDrawerActivity
 * ─────────────────────────────────────────────────────────────────
 * Activity induk yang menyediakan sidebar (Navigation Drawer) untuk
 * semua halaman dalam aplikasi SORA.
 *
 * Cara pemakaian:
 *   1. Extends BaseDrawerActivity (bukan AppCompatActivity).
 *   2. Panggil setupDrawer() di dalam onCreate() SETELAH setContentView().
 *   3. Layout Activity tidak perlu DrawerLayout — sudah ditangani di sini.
 *
 * Catatan: Layout Activity yang extends class ini HARUS dibungkus
 * dalam DrawerLayout dengan NavigationView (atau gunakan activity_base_drawer.xml
 * sebagai root dan inflate konten ke dalamnya).
 * Lebih mudah: Tambahkan DrawerLayout + NavigationView langsung di masing-masing
 * layout Activity (seperti yang ada di activity_main.xml).
 */
public abstract class BaseDrawerActivity extends AppCompatActivity {

    protected DrawerLayout drawerLayout;
    protected PrefManager  pref;

    /**
     * Panggil di onCreate() setelah setContentView().
     * Menyiapkan drawer, tombol hamburger, header, dan item navigasi.
     */
    protected void setupDrawer() {
        pref = new PrefManager(this);

        drawerLayout = findViewById(R.id.drawer_layout);
        if (drawerLayout == null) return;

        // Hapus overlay gelap saat drawer terbuka
        drawerLayout.setScrimColor(android.graphics.Color.TRANSPARENT);

        NavigationView navView = findViewById(R.id.nav_view);
        if (navView == null) return;

        // Muat foto & nama pengguna ke header drawer
        loadDrawerHeader(navView);

        // Refresh header setiap kali drawer dibuka
        drawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerOpened(View drawerView) {
                loadDrawerHeader(navView);
            }
        });

        // Tombol hamburger ☰ membuka drawer
        ImageView btnMenu = findViewById(R.id.btn_menu);
        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        }

        // Tombol Back: tutup drawer dulu jika sedang terbuka
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        // Handle klik item menu sidebar
        navView.setNavigationItemSelectedListener(item -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            int id = item.getItemId();

            if (id == R.id.drawer_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);

            } else if (id == R.id.drawer_detail) {
                startActivity(new Intent(this, DetailActivity.class));
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);

            } else if (id == R.id.drawer_about) {
                showAboutDialog();

            } else if (id == R.id.drawer_logout) {
                logout();
            }
            return true;
        });
    }

    /** Muat foto profil & nama pengguna ke header sidebar */
    private void loadDrawerHeader(NavigationView navView) {
        View headerView = navView.getHeaderView(0);
        if (headerView == null) return;

        // Sesuaikan paddingTop dengan tinggi status bar aktual
        int statusBarHeight = 0;
        int resId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resId > 0) statusBarHeight = getResources().getDimensionPixelSize(resId);
        int padPx = (int) (12 * getResources().getDisplayMetrics().density);
        headerView.setPadding(padPx, statusBarHeight + padPx, padPx, padPx);

        ImageView ivPhoto = headerView.findViewById(R.id.drawer_user_photo);
        android.widget.TextView tvName = headerView.findViewById(R.id.drawer_user_name);

        // Foto profil dari SharedPreferences
        String b64 = pref.getString("PROFILE_PHOTO_B64", null);
        if (b64 != null && ivPhoto != null) {
            try {
                byte[] bytes = android.util.Base64.decode(b64, android.util.Base64.DEFAULT);
                android.graphics.Bitmap bmp = android.graphics.BitmapFactory
                        .decodeByteArray(bytes, 0, bytes.length);
                ivPhoto.setImageBitmap(bmp);
                ivPhoto.setPadding(0, 0, 0, 0);
                androidx.core.widget.ImageViewCompat.setImageTintList(ivPhoto, null);
            } catch (Exception ignored) {}
        }

        // Nama pengguna
        if (tvName != null) {
            if (pref == null) pref = new PrefManager(this);
            String savedName = pref.getString("PROFILE_DISPLAY_NAME", null);
            if (savedName != null && !savedName.isEmpty()) {
                tvName.setText(savedName);
            } else {
                com.google.firebase.auth.FirebaseUser user =
                        FirebaseAuth.getInstance().getCurrentUser();
                if (user != null && user.getEmail() != null) {
                    tvName.setText(user.getEmail().split("@")[0]);
                }
            }
        }
    }

    /** Dialog Tentang Aplikasi */
    protected void showAboutDialog() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Tentang Aplikasi")
                .setMessage("SORA - Smart Orchid Agro\n\n"
                        + "Sistem Penyiraman Anggrek Otomatis\n"
                        + "berbasis IoT.\n\n"
                        + "Tim Pengembang:\n"
                        + "- Muhammad Yoga\n"
                        + "- Muhammad Naufal Nijami\n"
                        + "- Muhammad Rhojani\n"
                        + "- Devi Pusparina\n"
                        + "- Nurlaila\n\n"
                        + "Versi: 1.0.0\n"
                        + "© 2026 SORA Team")
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .setCancelable(true)
                .show();
    }

    /** Konfirmasi logout */
    private void logout() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Konfirmasi")
                .setMessage("Yakin ingin keluar dari akun?")
                .setPositiveButton("LANJUTKAN", (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                })
                .setNegativeButton("BATAL", (dialog, which) -> dialog.dismiss())
                .setCancelable(true)
                .show();
    }

    /** Sembunyikan navigation bar sistem */
    protected void hideSystemNavBar() {
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
