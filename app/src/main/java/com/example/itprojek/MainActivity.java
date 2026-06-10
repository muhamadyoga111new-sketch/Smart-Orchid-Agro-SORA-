package com.example.itprojek;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.activity.OnBackPressedCallback;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    // ── Device ID (sesuaikan dengan ID perangkat IoT Anda) ──
    private static final String DEVICE_ID = "ANG-123456";
    private static final String DB_URL =
            "https://sora-app-9f18a-default-rtdb.asia-southeast1.firebasedatabase.app/";
    private static final int MAX_JARAK = 15; // cm saat tangki kosong

    private PrefManager         pref;
    private DrawerLayout        drawerLayout;
    private FirebaseDataManager dataManager;
    private int                 lastSoilMoisture = 0;
    private MaterialSwitch      switchPump;
    private TextView            tvPumpStatus;
    private MaterialSwitch      switchAutoMode;
    private TextView            tvAutoThreshold;
    private boolean             isPumpUpdating = false;
    private boolean             isAutoMode = false;
    private boolean             isAutoUpdating = false;

    // ── Firebase real-time listener (menggantikan polling 1 menit) ──
    // tidak ada lagi Handler/Runnable — Firebase langsung push setiap ada perubahan

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pref = new PrefManager(this);
        hideSystemNavBar();

        // Cek sesi Firebase — jika tidak ada, kembali ke Login
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            goToLogin();
            return;
        }

        // === Firebase Realtime Database ===
        dataManager = new FirebaseDataManager(DEVICE_ID);

        // === Mulai background service pencatat riwayat sensor (5 menit sekali) ===
        Intent serviceIntent = new Intent(this, SensorRecorderService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        // === Minta izin notifikasi (Android 13+) ===
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 100);
            }
        }

        // Subscribe ke topik FCM agar mendapat push notification dari ESP32 walau aplikasi ditutup
        com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic("sora_alerts")
            .addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    android.util.Log.e("SORA-FCM", "Gagal subscribe ke topik sora_alerts");
                } else {
                    android.util.Log.d("SORA-FCM", "Berhasil subscribe ke topik sora_alerts");
                }
            });

        // === Navigation Drawer Setup ===
        drawerLayout = findViewById(R.id.drawer_layout);
        drawerLayout.setScrimColor(android.graphics.Color.TRANSPARENT); // tanpa overlay gelap
        NavigationView navView = findViewById(R.id.nav_view);

        // Muat foto & nama pengguna ke header drawer
        loadDrawerHeader(navView);

        // Refresh header setiap kali drawer dibuka
        drawerLayout.addDrawerListener(new androidx.drawerlayout.widget.DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerOpened(View drawerView) {
                loadDrawerHeader(navView);
            }
        });

        // Handle Back Press with OnBackPressedDispatcher
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        // Hamburger menu button opens drawer
        ImageView btnMenu = findViewById(R.id.btn_menu);
        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // Jika Activity lain mengirim flag OPEN_DRAWER, langsung buka sidebar
        if (getIntent().getBooleanExtra("OPEN_DRAWER", false)) {
            drawerLayout.post(() -> drawerLayout.openDrawer(GravityCompat.START));
        }

        // Handle drawer menu clicks
        navView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            drawerLayout.closeDrawer(GravityCompat.START);
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

        // Apply status bar insets to header
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            v.setPadding(systemBars.left, 0, systemBars.right, 0);

            View headerBg = findViewById(R.id.header_bg);
            if (headerBg != null) {
                // Return header height to standard 60dp + status bar height
                int basePx = (int) (60 * getResources().getDisplayMetrics().density);
                headerBg.getLayoutParams().height = basePx + systemBars.top;
                headerBg.requestLayout();
            }

            // Align all three items (menu, notification, and title) down by status bar height
            // They are constrained to TOP and BOTTOM of header_bg, so this centers them in the 60dp area
            View btnMenuRef = findViewById(R.id.btn_menu);
            if (btnMenuRef != null) {
                ((androidx.constraintlayout.widget.ConstraintLayout.LayoutParams)
                        btnMenuRef.getLayoutParams()).topMargin = systemBars.top;
                btnMenuRef.requestLayout();
            }

            View btnNotifView = findViewById(R.id.btn_notification_top);
            if (btnNotifView != null) {
                ((androidx.constraintlayout.widget.ConstraintLayout.LayoutParams)
                        btnNotifView.getLayoutParams()).topMargin = systemBars.top;
                btnNotifView.requestLayout();
            }

            View tvTitle = findViewById(R.id.tv_title);
            if (tvTitle != null) {
                ((androidx.constraintlayout.widget.ConstraintLayout.LayoutParams)
                        tvTitle.getLayoutParams()).topMargin = systemBars.top;
                tvTitle.requestLayout();
            }


            return insets;
        });

        // Notification bell
        ImageView btnNotification = findViewById(R.id.btn_notification_top);
        btnNotification.setOnClickListener(v -> {
            startActivity(new Intent(this, NotificationsActivity.class));
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });

        // === Sensor cards → DetailActivity ===
        com.google.android.material.card.MaterialCardView cardMoisture = findViewById(R.id.card_moisture);
        com.google.android.material.card.MaterialCardView cardWater    = findViewById(R.id.card_water);
        android.view.View.OnClickListener goToDetail = v -> {
            startActivity(new Intent(this, DetailActivity.class));
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        };
        if (cardMoisture != null) cardMoisture.setOnClickListener(goToDetail);
        if (cardWater    != null) cardWater.setOnClickListener(goToDetail);

        // === Pump toggle — terhubung ke Firebase Realtime Database ===
        switchPump   = findViewById(R.id.switch_pump);
        tvPumpStatus = findViewById(R.id.tv_pump_status);

        // Nilai awal dari SharedPreferences
        boolean pumpSaved = pref.getBoolean("STATUS_POMPA", false);
        switchPump.setChecked(pumpSaved);
        updatePumpStatusUI(tvPumpStatus, pumpSaved);

        switchPump.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isPumpUpdating) return;

            String message = isChecked ? "Yakin ingin menyalakan pompa?" : "Yakin ingin mematikan pompa?";

            new AlertDialog.Builder(this)
                    .setTitle("Konfirmasi")
                    .setMessage(message)
                    .setPositiveButton("LANJUTKAN", (dialog, which) -> {
                        pref.saveBoolean("STATUS_POMPA", isChecked);
                        updatePumpStatusUI(tvPumpStatus, isChecked);
                        // Tulis perubahan pompa ke Firebase
                        dataManager.setPumpStatus(isChecked);
                        dialog.dismiss();
                    })
                    .setNegativeButton("BATAL", (dialog, which) -> {
                        isPumpUpdating = true;
                        switchPump.setChecked(!isChecked);
                        isPumpUpdating = false;
                        dialog.dismiss();
                    })
                    .setCancelable(false)
                    .show();
        });

        // === Auto Mode Switch ===
        switchAutoMode   = findViewById(R.id.switch_auto_mode);
        tvAutoThreshold  = findViewById(R.id.tv_auto_threshold);

        // Baca status auto mode dari SharedPreferences
        isAutoMode = pref.getBoolean("AUTO_MODE", false);
        isAutoUpdating = true;
        switchAutoMode.setChecked(isAutoMode);
        isAutoUpdating = false;
        switchPump.setEnabled(!isAutoMode);
        updateAutoThresholdLabel();

        switchAutoMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isAutoUpdating) return;
            isAutoMode = isChecked;
            pref.saveBoolean("AUTO_MODE", isAutoMode);
            // Disable/enable manual pump switch
            switchPump.setEnabled(!isAutoMode);
            updateAutoThresholdLabel();
            if (isAutoMode) {
                // Langsung cek kondisi berdasarkan nilai sensor terakhir
                checkAutoWatering(lastSoilMoisture);
            }
        });

        // Tampilkan nilai sensor terakhir dari SharedPreferences saat startup
        int savedSoil  = pref.getInt("LAST_SOIL", 0);
        int savedWater = pref.getInt("LAST_WATER", 0);
        updateSensorUI(savedSoil, savedWater);

        // === Sensor Real-time dari Firebase ===
        FirebaseDataManager.SensorListener sensorCallback = new FirebaseDataManager.SensorListener() {
            @Override
            public void onSensorUpdated(int soilMoisture, int waterLevel, boolean pumpStatus) {
                lastSoilMoisture = soilMoisture;
                // Simpan nilai terbaru ke preferences
                pref.saveInt("LAST_SOIL",  soilMoisture);
                pref.saveInt("LAST_WATER", waterLevel);
                updateSensorUI(soilMoisture, waterLevel);
                // Cek kondisi sensor dan kirim notifikasi lokal jika diperlukan
                checkAndNotify(soilMoisture, waterLevel);

                if (isAutoMode) {
                    checkAutoWatering(soilMoisture);
                    // Tetap reflect perubahan pompa dari hardware di UI
                    if (switchPump.isChecked() != pumpStatus) {
                        isPumpUpdating = true;
                        switchPump.setChecked(pumpStatus);
                        isPumpUpdating = false;
                        updatePumpStatusUI(tvPumpStatus, pumpStatus);
                    }
                } else {
                    // Mode manual: sync pompa dari Firebase ke UI
                    if (switchPump.isChecked() != pumpStatus) {
                        isPumpUpdating = true;
                        switchPump.setChecked(pumpStatus);
                        isPumpUpdating = false;
                        pref.saveBoolean("STATUS_POMPA", pumpStatus);
                        updatePumpStatusUI(tvPumpStatus, pumpStatus);
                    }
                }
            }

            @Override
            public void onError(String errorMessage) {
                android.util.Log.w("SORA-DB", "Sensor error: " + errorMessage);
                runOnUiThread(() -> {
                    android.widget.Toast.makeText(MainActivity.this, "Firebase Error: " + errorMessage, android.widget.Toast.LENGTH_LONG).show();
                });
            }
        };

        // Daftarkan listener real-time ke node status/
        dataManager.listenSensorData(sensorCallback);
        dataManager.startPumpListener(sensorCallback);



        // Bottom Navigation
        LinearLayout navHistory       = findViewById(R.id.nav_history);
        LinearLayout navNotifications = findViewById(R.id.nav_notifications);
        LinearLayout navSettings      = findViewById(R.id.nav_settings);

        applyScaleAnimation(navHistory);
        applyScaleAnimation(navNotifications);
        applyScaleAnimation(navSettings);

        navHistory.setOnClickListener(v -> {
            startActivity(new Intent(this, HistoryActivity.class));
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });
        navNotifications.setOnClickListener(v -> {
            startActivity(new Intent(this, NotificationsActivity.class));
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });
        navSettings.setOnClickListener(v -> {
            startActivity(new Intent(this, SettingsActivity.class));
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Hentikan semua Firebase listeners (soil, water, pump real-time)
        if (dataManager != null) dataManager.stopListening();
    }

    /** Muat foto & nama pengguna ke header Navigation Drawer */
    private void loadDrawerHeader(com.google.android.material.navigation.NavigationView navView) {
        View headerView = navView.getHeaderView(0);
        if (headerView == null) return;

        // ── Sesuaikan paddingTop dengan tinggi status bar aktual ─
        int statusBarHeight = 0;
        int resId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resId > 0) statusBarHeight = getResources().getDimensionPixelSize(resId);
        int padPx = (int) (12 * getResources().getDisplayMetrics().density);
        headerView.setPadding(padPx, statusBarHeight + padPx, padPx, padPx);

        ImageView ivPhoto  = headerView.findViewById(R.id.drawer_user_photo);
        android.widget.TextView tvName = headerView.findViewById(R.id.drawer_user_name);

        // ── Foto profil dari SharedPreferences ──────────────────
        String b64 = pref.getString("PROFILE_PHOTO_B64", null);
        if (b64 != null && ivPhoto != null) {
            try {
                byte[] bytes = android.util.Base64.decode(b64, android.util.Base64.DEFAULT);
                android.graphics.Bitmap bmp = android.graphics.BitmapFactory
                        .decodeByteArray(bytes, 0, bytes.length);
                ivPhoto.setImageBitmap(bmp);
                ivPhoto.setPadding(0, 0, 0, 0);
                // Hapus tint XML agar foto tidak berwarna hijau
                androidx.core.widget.ImageViewCompat.setImageTintList(ivPhoto, null);
            } catch (Exception ignored) {}
        }

        // ── Nama pengguna ────────────────────────────────────────
        if (tvName != null) {
            String savedName = pref.getString("PROFILE_DISPLAY_NAME", null);
            if (savedName != null && !savedName.isEmpty()) {
                tvName.setText(savedName);
            } else {
                com.google.firebase.auth.FirebaseUser user =
                        com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
                if (user != null && user.getEmail() != null) {
                    // Tampilkan bagian sebelum @ dari email
                    String emailName = user.getEmail().split("@")[0];
                    tvName.setText(emailName);
                }
            }
        }
    }

    private void showAboutDialog() {
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

    private void logout() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Konfirmasi")
                .setMessage("Yakin ingin keluar dari akun?")
                .setPositiveButton("LANJUTKAN", (dialog, which) -> {
                    if (dataManager != null) dataManager.stopListening();
                    FirebaseAuth.getInstance().signOut();
                    goToLogin();
                })
                .setNegativeButton("BATAL", (dialog, which) -> dialog.dismiss())
                .setCancelable(true)
                .show();
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }

    private void updatePumpStatusUI(TextView tvPumpStatus, boolean isOn) {
        tvPumpStatus.setText(isOn ? getString(R.string.on) : getString(R.string.off));
        tvPumpStatus.setTextColor(Color.parseColor(isOn ? "#43A047" : "#EF5350"));
    }

    /**
     * Perbarui tampilan UI sensor (kelembapan tanah & level air).
     * Dipanggil setiap kali polling 1 menit berhasil.
     */
    private void updateSensorUI(int soilMoisture, int waterLevel) {
        // Gauge kelembapan
        SoilGaugeView gauge = findViewById(R.id.soil_gauge);
        if (gauge != null) gauge.setPercentage(soilMoisture);

        // Nilai level air
        TextView tvWaterValue = findViewById(R.id.tv_water_value);
        if (tvWaterValue != null) tvWaterValue.setText(waterLevel + "%");

        // Tangki air visual
        View waterFill   = findViewById(R.id.water_fill);
        View tankOutline = findViewById(R.id.tank_outline);
        if (waterFill != null && tankOutline != null) {
            Runnable updateHeight = () -> {
                waterFill.getLayoutParams().height = (int) (tankOutline.getHeight() * (waterLevel / 100f));
                waterFill.requestLayout();
            };
            if (tankOutline.getHeight() > 0) {
                updateHeight.run();
            } else {
                tankOutline.post(updateHeight);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Notifikasi Lokal — dicek setiap kali sensor data masuk
    // ─────────────────────────────────────────────────────────────────
    private static final String CHANNEL_ID    = "sora_alerts";
    private static final String CHANNEL_NAME  = "SORA Peringatan";
    // Cooldown agar notifikasi tidak spam (5 menit per tipe)
    private long lastNotifTimeTank  = 0;
    private long lastNotifTimeDry   = 0;
    private long lastNotifTimeMoist = 0;
    private static final long NOTIF_COOLDOWN_MS = 5 * 60 * 1000; // 5 menit

    /**
     * Periksa kondisi sensor dan kirim notifikasi lokal sesuai toggle pengaturan.
     * Dipanggil setiap kali data sensor diperbarui dari Firebase.
     */
    private void checkAndNotify(int soilMoisture, int waterLevel) {
        long now = System.currentTimeMillis();
        int thresholdKering = pref.getInt("KALIBRASI_KERING", 30);
        int thresholdNormal = pref.getInt("KALIBRASI_NORMAL", 60);

        // --- Air Tangki Habis ---
        if (pref.getBoolean("NOTIF_TANK", true) && waterLevel <= 0) {
            if (now - lastNotifTimeTank > NOTIF_COOLDOWN_MS) {
                lastNotifTimeTank = now;
                sendLocalNotification(
                        getString(R.string.alert_tank_title),
                        "Air tangki habis! Segera isi ulang tangki air.",
                        "Air Tangki Habis", 1001);
            }
        }

        // --- Proteksi Pompa (alarm jika tangki habis & pompa ON) ---
        if (pref.getBoolean("NOTIF_TANK_ALARM", true) && waterLevel <= 0 && switchPump.isChecked()) {
            if (now - lastNotifTimeTank > NOTIF_COOLDOWN_MS) {
                lastNotifTimeTank = now;
                sendLocalNotification(
                        getString(R.string.alert_tank_alarm_title),
                        "Pompa menyala saat tangki kosong! Pompa dimatikan otomatis.",
                        "Proteksi Pompa Air", 1002);
                // Matikan pompa otomatis untuk proteksi
                isPumpUpdating = true;
                switchPump.setChecked(false);
                isPumpUpdating = false;
                pref.saveBoolean("STATUS_POMPA", false);
                updatePumpStatusUI(tvPumpStatus, false);
                dataManager.setPumpStatus(false);
            }
        }

        // --- Tanah Kering ---
        if (pref.getBoolean("NOTIF_DRY", true) && soilMoisture < thresholdKering) {
            if (now - lastNotifTimeDry > NOTIF_COOLDOWN_MS) {
                lastNotifTimeDry = now;
                sendLocalNotification(
                        getString(R.string.alert_dry_title),
                        "Kelembapan tanah sangat rendah (" + soilMoisture + "%)! Segera lakukan penyiraman.",
                        "Tanah Kering", 1003);
            }
        }

        // --- Tanah Lembab (kembali normal) ---
        if (pref.getBoolean("NOTIF_MOIST", false) && soilMoisture >= thresholdNormal) {
            if (now - lastNotifTimeMoist > NOTIF_COOLDOWN_MS) {
                lastNotifTimeMoist = now;
                sendLocalNotification(
                        getString(R.string.alert_moist_title),
                        "Tanah sudah kembali lembab (" + soilMoisture + "%). Penyiraman dapat dihentikan.",
                        "Tanah Lembab", 1004);
            }
        }
    }

    /** Kirim notifikasi sistem Android lokal + simpan ke SharedPreferences */
    private void sendLocalNotification(String title, String body, String tipeAlert, int notifId) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm == null) return;

        // Buat channel (diperlukan Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Peringatan sistem penyiraman SORA");
            nm.createNotificationChannel(channel);
        }

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(
                this, notifId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                        .setAutoCancel(true)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setContentIntent(pi);

        nm.notify(notifId, builder.build());
        android.util.Log.d("SORA-NOTIF", "Notifikasi dikirim: " + title);

        // Simpan ke SharedPreferences lokal
        saveNotificationLocally(title, body, tipeAlert);
    }

    /** Simpan notifikasi ke SharedPreferences lokal (andal, tidak perlu Firebase rules) */
    private void saveNotificationLocally(String title, String body, String tipeAlert) {
        String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                java.util.Locale.getDefault()).format(new java.util.Date());

        // Format: "timestamp|tipe_alert|judul|pesan"
        String entry = timestamp + "|" + tipeAlert + "|" + title + "|" + body;

        // Baca daftar yang sudah ada (pakai copy untuk hindari bug SharedPreferences)
        java.util.Set<String> existing = pref.getStringSet("LOCAL_NOTIFICATIONS");
        java.util.LinkedHashSet<String> updated = new java.util.LinkedHashSet<>(existing);
        updated.add(entry);

        // Batasi maksimum 50 notifikasi
        while (updated.size() > 50) {
            updated.remove(updated.iterator().next());
        }

        pref.saveStringSet("LOCAL_NOTIFICATIONS", updated);
        android.util.Log.d("SORA-NOTIF", "Notif disimpan lokal: " + title + " (total: " + updated.size() + ")");
    }


    /**
     * Cek apakah penyiraman otomatis perlu diaktifkan/dinonaktifkan
     * berdasarkan nilai kelembapan tanah dan threshold kalibrasi.
     *
     * Logika:
     *   - soilMoisture < 60  → pompa ON  (tanah terlalu kering)
     *   - soilMoisture > 80  → pompa OFF (tanah sudah cukup lembap)
     *   - di antara keduanya               → biarkan status pompa saat ini
     *
     * Emergency Stop selalu mengesampingkan auto-watering.
     */
    private void checkAutoWatering(int soilMoisture) {
        if (!isAutoMode) return;

        int thresholdKering = 60;
        int thresholdNormal = 80;

        boolean currentPump = switchPump.isChecked();

        if (soilMoisture < thresholdKering && !currentPump) {
            // Tanah kering → nyalakan pompa
            android.util.Log.d("SORA-AUTO", "Auto: Tanah kering (" + soilMoisture + "% < " + thresholdKering + "%), pompa ON");
            isPumpUpdating = true;
            switchPump.setChecked(true);
            isPumpUpdating = false;
            pref.saveBoolean("STATUS_POMPA", true);
            updatePumpStatusUI(tvPumpStatus, true);
            dataManager.setPumpStatus(true);
        } else if (soilMoisture > thresholdNormal && currentPump) {
            // Tanah sudah lembap → matikan pompa
            android.util.Log.d("SORA-AUTO", "Auto: Tanah lembap (" + soilMoisture + "% > " + thresholdNormal + "%), pompa OFF");
            isPumpUpdating = true;
            switchPump.setChecked(false);
            isPumpUpdating = false;
            pref.saveBoolean("STATUS_POMPA", false);
            updatePumpStatusUI(tvPumpStatus, false);
            dataManager.setPumpStatus(false);
        }
    }

    /**
     * Update label info threshold di bawah switch Mode Otomatis.
     */
    private void updateAutoThresholdLabel() {
        if (tvAutoThreshold == null) return;
        if (isAutoMode) {
            tvAutoThreshold.setText("Siram jika tanah < 60%, stop > 80%");
        } else {
            tvAutoThreshold.setText("Siram jika tanah < 60%");
        }
    }

    private void applyScaleAnimation(View view) {
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    Animation scaleDown = AnimationUtils.loadAnimation(this, R.anim.scale_down);
                    scaleDown.setFillAfter(true);
                    v.startAnimation(scaleDown);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    Animation scaleUp = AnimationUtils.loadAnimation(this, R.anim.scale_up);
                    scaleUp.setFillAfter(true);
                    v.startAnimation(scaleUp);
                    break;
            }
            return false;
        });
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