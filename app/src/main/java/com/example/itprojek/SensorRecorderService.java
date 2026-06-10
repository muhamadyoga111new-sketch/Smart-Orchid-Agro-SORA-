package com.example.itprojek;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import androidx.annotation.NonNull;

/**
 * SensorRecorderService
 * ─────────────────────────────────────────────────────────────────────────────
 * Foreground Service yang berjalan mandiri (bahkan saat aplikasi ditutup).
 * Setiap 5 menit, mencatat snapshot data sensor ke Firebase history/.
 * Setiap kali mencatat, menghapus data history yang lebih dari 1 jam.
 */
public class SensorRecorderService extends Service {

    private static final String TAG            = "SORA-SVC";
    private static final String CHANNEL_ID     = "sora_recorder_channel";
    private static final int    NOTIF_ID       = 1001;
    private static final long   INTERVAL_MS    = 5 * 60 * 1000L;  // 5 menit
    private static final long   RETENTION_MS   = 60 * 60 * 1000L; // 1 jam
    private static final int    MAX_JARAK      = 25;              // cm (tinggi tangki)
    private static final String DB_URL         =
            "https://sora-app-9f18a-default-rtdb.asia-southeast1.firebasedatabase.app/";
    private static final String DEVICE_ID      = "ANG-123456";

    private Handler   handler;
    private Runnable  timerRunnable;

    // Nilai sensor terakhir (dari real-time listener)
    private int     lastSoil  = -1;
    private int     lastWater = -1;
    private boolean lastPump  = false;
    private long    lastSaveMs = 0;

    private DatabaseReference statusRef;
    private DatabaseReference historyRef;
    private ValueEventListener statusListener;

    // ── onCreate ─────────────────────────────────────────────────────────────
    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(NOTIF_ID, buildNotification("Memantau sensor…"));

        FirebaseDatabase db = FirebaseDatabase.getInstance(DB_URL);
        statusRef  = db.getReference("SORA/status");
        historyRef = db.getReference("history").child(DEVICE_ID);

        // Mulai mendengarkan data sensor secara real-time
        startStatusListener();

        // Jadwalkan perekaman tiap 5 menit
        handler = new Handler(Looper.getMainLooper());
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                recordSnapshot();
                purgeOldHistory();
                handler.postDelayed(this, INTERVAL_MS);
            }
        };
        handler.postDelayed(timerRunnable, INTERVAL_MS);
        Log.d(TAG, "Service started — recording every 5 minutes");
    }

    // ── onStartCommand ───────────────────────────────────────────────────────
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // START_STICKY: Android akan restart service ini jika dimatikan sistem
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }

    // ── onDestroy ────────────────────────────────────────────────────────────
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null && timerRunnable != null) {
            handler.removeCallbacks(timerRunnable);
        }
        if (statusRef != null && statusListener != null) {
            statusRef.removeEventListener(statusListener);
        }
        Log.d(TAG, "Service destroyed");
    }

    // ── Real-time listener ───────────────────────────────────────────────────
    private void startStatusListener() {
        statusListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snap) {
                // Kelembapan
                Object soilObj = snap.child("kelembapan").getValue();
                lastSoil = toInt(soilObj);

                // Jarak tangki → persentase air
                Object jarakObj = snap.child("jarak_tangki").getValue();
                int jarak = jarakObj != null ? toInt(jarakObj) : MAX_JARAK;
                lastWater = Math.max(0, Math.min(100,
                        (int) ((MAX_JARAK - jarak) * 100f / MAX_JARAK)));

                // Status pompa
                Object pumpObj = snap.child("pompa_status").getValue();
                if (pumpObj instanceof String)
                    lastPump = "MENYALA".equalsIgnoreCase((String) pumpObj);
                else if (pumpObj instanceof Boolean)
                    lastPump = (Boolean) pumpObj;

                // Perbarui teks notifikasi
                updateNotification("Lembap: " + lastSoil + "% | Air: " + lastWater + "%");

                Log.d(TAG, "Sensor updated: soil=" + lastSoil + " water=" + lastWater);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Status listener cancelled: " + error.getMessage());
            }
        };
        statusRef.addValueEventListener(statusListener);
    }

    // ── Catat snapshot ke Firebase ───────────────────────────────────────────
    private void recordSnapshot() {
        if (lastSoil < 0 || lastWater < 0) {
            Log.d(TAG, "Skip record — data sensor belum tersedia");
            return;
        }
        // Pastikan user masih login
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Log.d(TAG, "Skip record — tidak ada user login");
            return;
        }
        // Anti-duplikasi: minimal 4 menit antar penyimpanan
        long now = System.currentTimeMillis();
        if (now - lastSaveMs < 4 * 60 * 1000L) {
            Log.d(TAG, "Skip record — terlalu cepat");
            return;
        }
        lastSaveMs = now;

        String soilStatus;
        if      (lastSoil < 30) soilStatus = "Kering";
        else if (lastSoil < 60) soilStatus = "Normal";
        else                    soilStatus = "Lembap";

        String ts = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date());

        Map<String, Object> entry = new HashMap<>();
        entry.put("soil",      lastSoil);
        entry.put("water",     lastWater);
        entry.put("pump",      lastPump);
        entry.put("status",    soilStatus);
        entry.put("timestamp", ts);

        historyRef.push().setValue(entry)
                .addOnSuccessListener(a -> Log.d(TAG,
                        "[HISTORY] Tersimpan: soil=" + lastSoil + " water=" + lastWater))
                .addOnFailureListener(e -> Log.e(TAG,
                        "[HISTORY] Gagal: " + e.getMessage()));
    }

    // ── Hapus entri > 1 jam ──────────────────────────────────────────────────
    private void purgeOldHistory() {
        long cutoff = System.currentTimeMillis() - RETENTION_MS;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        historyRef.get().addOnSuccessListener(snap -> {
            if (!snap.exists()) return;
            for (DataSnapshot ds : snap.getChildren()) {
                Object tsObj = ds.child("timestamp").getValue();
                if (tsObj instanceof String) {
                    try {
                        Date d = sdf.parse((String) tsObj);
                        if (d != null && d.getTime() < cutoff) {
                            ds.getRef().removeValue();
                            Log.d(TAG, "[HISTORY] Purged: " + tsObj);
                        }
                    } catch (Exception ignored) {}
                }
            }
        });
    }

    // ── Notifikasi ───────────────────────────────────────────────────────────
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID,
                    "SORA Pemantau Sensor",
                    NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("Mencatat riwayat sensor setiap 5 menit");
            ch.setShowBadge(false);
            getSystemService(NotificationManager.class).createNotificationChannel(ch);
        }
    }

    private Notification buildNotification(String text) {
        Intent tapIntent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, tapIntent,
                PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("SORA — Sistem Penyiraman")
                .setContentText(text)
                .setContentIntent(pi)
                .setSilent(true)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void updateNotification(String text) {
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) nm.notify(NOTIF_ID, buildNotification(text));
    }

    // ── Helper ───────────────────────────────────────────────────────────────
    private int toInt(Object val) {
        if (val instanceof Long)    return ((Long) val).intValue();
        if (val instanceof Integer) return (Integer) val;
        if (val instanceof Double)  return ((Double) val).intValue();
        if (val instanceof String)  {
            try { return Integer.parseInt((String) val); } catch (Exception ignored) {}
        }
        return 0;
    }
}
