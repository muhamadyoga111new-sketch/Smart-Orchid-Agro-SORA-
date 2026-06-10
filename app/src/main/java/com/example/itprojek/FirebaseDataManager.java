package com.example.itprojek;

import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import androidx.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * FirebaseDataManager
 * ─────────────────────────────────────────────────────────────────
 * Membaca data real-time dari IoT sensor SORA.
 *
 * Struktur Database (ditulis oleh IoT device):
 *   status/
 *     kelembapan   : int     → kelembapan tanah real-time
 *     jarak_tangki : int     → jarak sensor ke permukaan air
 *     pompa_status : String  → "MENYALA" / "MATI"
 *     air_rendah   : boolean → air hampir habis
 *     tangki_kosong: boolean → tangki kosong
 *     last_update  : String  → "yyyy-MM-dd HH:mm:ss"
 *
 *   history/{deviceId}/{pushKey}/ → snapshot otomatis saat data berubah
 *     soil       : int
 *     water      : int
 *     pump       : boolean
 *     status     : String  ("Kering" / "Normal" / "Lembap")
 *     timestamp  : String  ("yyyy-MM-dd HH:mm:ss")
 */
public class FirebaseDataManager {

    private static final String TAG    = "SORA-DB";
    private static final String DB_URL =
            "https://sora-app-9f18a-default-rtdb.asia-southeast1.firebasedatabase.app/";

    public interface SensorListener {
        void onSensorUpdated(int soilMoisture, int waterLevel, boolean pumpStatus);
        void onError(String errorMessage);
    }

    // ── Referensi Firebase ───────────────────────────────────────────
    private final DatabaseReference soilRef;    // status/kelembapan
    private final DatabaseReference waterRef;   // status/jarak_tangki
    private final DatabaseReference pumpRef;    // status/pompa_status
    private final DatabaseReference statusRef;  // status/ (root)
    private final DatabaseReference historyRef; // history/{deviceId}

    // ── Konstanta konversi ────────────────────────────────────────────
    /** Jarak sensor ke permukaan air saat tangki KOSONG (cm). Sesuaikan dengan hardware.
     *  Nilai ini = jarak_tangki yang terbaca di Firebase saat tangki_kosong = true.
     *  Tinggi tangki = 25 cm → jarak maksimal saat kosong = 25 cm. */
    private static final int MAX_JARAK_TANGKI = 25;

    // ── Listeners aktif ──────────────────────────────────────────────
    private ValueEventListener statusListener;  // listener node status/ (real-time)

    // ── Nilai terakhir yang diketahui ────────────────────────────────
    private int     lastSoil  = -1; // -1 = belum pernah dapat data
    private int     lastWater = -1;
    private boolean lastPump  = false;
    
    private android.os.Handler pollingHandler;
    private Runnable pollingRunnable;
    private long lastSaveTimeMs = 0; // pengaman anti-duplikasi

    // ── Constructor ──────────────────────────────────────────────────
    public FirebaseDataManager(String deviceId) {
        FirebaseDatabase db = FirebaseDatabase.getInstance(DB_URL);
        DatabaseReference statusRoot = db.getReference("SORA/status");
        soilRef    = statusRoot.child("kelembapan");
        waterRef   = statusRoot.child("jarak_tangki");
        pumpRef    = statusRoot.child("pompa_status");
        statusRef  = statusRoot;
        historyRef = db.getReference("history").child(deviceId);
    }

    // ── Real-time listener dari node status/ ─────────────────────────
    /**
     * Dengarkan perubahan real-time pada seluruh node status/.
     * Konversi jarak_tangki → persentase air: (MAX - jarak) / MAX * 100
     */
    public void listenSensorData(SensorListener listener) {
        stopListening();

        statusListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Baca kelembapan langsung
                int soil = toInt(snapshot.child("kelembapan").getValue());

                // Konversi jarak_tangki → persentase air
                Object jarakObj = snapshot.child("jarak_tangki").getValue();
                int jarak = jarakObj != null ? toInt(jarakObj) : MAX_JARAK_TANGKI;
                int water = Math.max(0, Math.min(100,
                        (int) ((MAX_JARAK_TANGKI - jarak) * 100f / MAX_JARAK_TANGKI)));

                // Baca status pompa
                Object pObj = snapshot.child("pompa_status").getValue();
                boolean pump = false;
                if (pObj instanceof String)  pump = "MENYALA".equalsIgnoreCase((String) pObj);
                else if (pObj instanceof Boolean) pump = (Boolean) pObj;

                lastSoil  = soil;
                lastWater = water;
                lastPump  = pump;

                Log.d(TAG, "status/ updated: kelembapan=" + soil
                        + " jarak=" + jarak + " air%=" + water
                        + " pompa=" + (pump ? "MENYALA" : "MATI"));

                listener.onSensorUpdated(lastSoil, lastWater, lastPump);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onError("status/ listener cancelled: " + error.getMessage());
            }
        };
        statusRef.addValueEventListener(statusListener);
        // Timer riwayat dijalankan oleh SensorRecorderService (bukan di sini)
    }

    /** Real-time listener khusus pompa (delegate ke statusListener) */
    public void startPumpListener(SensorListener listener) {
        // Pompa sudah dibaca dalam listenSensorData() via status/pompa_status
        // Tidak perlu listener terpisah — tidak melakukan apa-apa
    }

    /** Hentikan semua listener */
    public void stopListening() {
        if (statusListener != null) {
            statusRef.removeEventListener(statusListener);
            statusListener = null;
        }
        if (pollingHandler != null && pollingRunnable != null) {
            pollingHandler.removeCallbacks(pollingRunnable);
            pollingRunnable = null;
            Log.d(TAG, "History timer stopped");
        }
    }

    /** Tulis status pompa → dibaca IoT device */
    public void setPumpStatus(boolean isOn) {
        // Tulis sebagai String "MENYALA"/"MATI" sesuai format Firebase
        pumpRef.setValue(isOn ? "MENYALA" : "MATI");
        lastPump = isOn;
    }

    /** Tulis data sensor manual (simulator/testing) */
    public void updateSensorData(int soil, int water) {
        soilRef.setValue(soil);
        waterRef.setValue(water);
    }

    // ── Simpan snapshot riwayat ke history/{deviceId}/{pushKey} ──────
    private void saveHistorySnapshot() {
        if (lastSoil < 0 || lastWater < 0) return; // data belum lengkap

        // Anti-duplikasi: minimal 4 menit antar penyimpanan
        long now = System.currentTimeMillis();
        if (now - lastSaveTimeMs < 4 * 60 * 1000L) {
            Log.d(TAG, "[HISTORY] Skip — terlalu cepat dari penyimpanan sebelumnya");
            return;
        }
        lastSaveTimeMs = now;

        String status;
        if (lastSoil < 30)      status = "Kering";
        else if (lastSoil < 60) status = "Normal";
        else                    status = "Lembap";

        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date());

        Map<String, Object> entry = new HashMap<>();
        entry.put("soil",      lastSoil);
        entry.put("water",     lastWater);
        entry.put("pump",      lastPump);
        entry.put("status",    status);
        entry.put("timestamp", timestamp);

        historyRef.push().setValue(entry)
                .addOnSuccessListener(a -> Log.d(TAG, "[HISTORY] Tersimpan: soil=" + lastSoil + " water=" + lastWater + " pump=" + lastPump))
                .addOnFailureListener(e -> Log.e(TAG, "[HISTORY] Gagal simpan: " + e.getMessage()));
    }

    // ── Hapus entri riwayat yang lebih dari 1 jam ────────────────────
    private void purgeOldHistory() {
        long cutoff = System.currentTimeMillis() - (60L * 60 * 1000); // 1 jam lalu
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

        historyRef.get().addOnSuccessListener(snapshot -> {
            if (!snapshot.exists()) return;
            for (DataSnapshot ds : snapshot.getChildren()) {
                Object tsObj = ds.child("timestamp").getValue();
                if (tsObj instanceof String) {
                    try {
                        java.util.Date d = sdf.parse((String) tsObj);
                        if (d != null && d.getTime() < cutoff) {
                            ds.getRef().removeValue();
                            Log.d(TAG, "[HISTORY] Purged: " + tsObj);
                        }
                    } catch (Exception ignored) {}
                }
            }
        });
    }

    // ── Getter referensi ─────────────────────────────────────────────
    public DatabaseReference getSoilRef()    { return soilRef; }    // status/kelembapan
    public DatabaseReference getWaterRef()   { return waterRef; }   // status/jarak_tangki
    public DatabaseReference getPumpRef()    { return pumpRef; }    // status/pompa_status
    public DatabaseReference getStatusRef()  { return statusRef; }  // status/
    public DatabaseReference getHistoryRef() { return historyRef; }

    public static String getCurrentUserId() {
        if (FirebaseAuth.getInstance().getCurrentUser() != null)
            return FirebaseAuth.getInstance().getCurrentUser().getUid();
        return null; // jangan fallback ke hardcoded ID
    }

    // ── Helper ───────────────────────────────────────────────────────
    private int toInt(Object val) {
        if (val instanceof Long)    return ((Long) val).intValue();
        if (val instanceof Integer) return (Integer) val;
        if (val instanceof Double)  return ((Double) val).intValue();
        if (val instanceof String)  { try { return Integer.parseInt((String) val); } catch (Exception ignored) {} }
        return 0;
    }
}
