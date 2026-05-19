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
 *   sensors/
 *     soil   : int     → kelembapan tanah real-time
 *     water  : int     → level air real-time
 *     pump   : boolean → status pompa (dua arah)
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
    private final DatabaseReference soilRef;    // sensors/soil
    private final DatabaseReference waterRef;   // sensors/water
    private final DatabaseReference pumpRef;    // sensors/pump
    private final DatabaseReference historyRef; // history/{deviceId}

    // ── Listeners aktif ──────────────────────────────────────────────
    private ValueEventListener soilListener;
    private ValueEventListener waterListener;
    private ValueEventListener pumpListener;

    // ── Nilai terakhir yang diketahui ────────────────────────────────
    private int     lastSoil  = -1; // -1 = belum pernah dapat data
    private int     lastWater = -1;
    private boolean lastPump  = false;
    
    private android.os.Handler pollingHandler;
    private Runnable pollingRunnable;

    // ── Constructor ──────────────────────────────────────────────────
    public FirebaseDataManager(String deviceId) {
        FirebaseDatabase db = FirebaseDatabase.getInstance(DB_URL);
        DatabaseReference sensorsRoot = db.getReference("sensors");
        soilRef    = sensorsRoot.child("soil");
        waterRef   = sensorsRoot.child("water");
        pumpRef    = sensorsRoot.child("pump");
        historyRef = db.getReference("history").child(deviceId);
    }

    // ── Sensor polling (tiap 3 menit) ─────────────────────
    public void listenSensorData(SensorListener listener) {
        stopListening();

        if (pollingHandler == null) {
            pollingHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        }

        pollingRunnable = new Runnable() {
            @Override
            public void run() {
                fetchSensorDataOnce(new SensorListener() {
                    @Override
                    public void onSensorUpdated(int soilMoisture, int waterLevel, boolean pumpStatus) {
                        boolean changed = (soilMoisture != lastSoil && lastSoil != -1) || 
                                          (waterLevel != lastWater && lastWater != -1);
                        lastSoil = soilMoisture;
                        lastWater = waterLevel;
                        lastPump = pumpStatus;
                        Log.d(TAG, "polling: soil=" + lastSoil + " water=" + lastWater + " changed=" + changed);
                        listener.onSensorUpdated(lastSoil, lastWater, lastPump);
                        if (changed) saveHistorySnapshot();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        listener.onError("polling error: " + errorMessage);
                    }
                });
                
                // Jadwalkan ulang untuk 3 menit (180.000 ms)
                pollingHandler.postDelayed(this, 3 * 60 * 1000);
            }
        };

        // Jalankan pengambilan pertama kali seketika
        pollingHandler.post(pollingRunnable);
    }

    /** Fetch sekali – dipakai jika masih butuh polling manual */
    public void fetchSensorDataOnce(SensorListener listener) {
        soilRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) {
                lastSoil = toInt(s.getValue());
                waterRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot sw) {
                        lastWater = toInt(sw.getValue());
                        Log.d(TAG, "fetch-once soil=" + lastSoil + " water=" + lastWater);
                        listener.onSensorUpdated(lastSoil, lastWater, lastPump);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {
                        listener.onError(e.getMessage());
                    }
                });
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {
                listener.onError(e.getMessage());
            }
        });
    }

    /** Real-time listener khusus pompa */
    public void startPumpListener(SensorListener listener) {
        if (pumpListener != null) pumpRef.removeEventListener(pumpListener);
        pumpListener = new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) {
                Object v = s.getValue();
                if (v instanceof Boolean)  lastPump = (Boolean) v;
                else if (v instanceof Long) lastPump = ((Long) v) != 0;
                Log.d(TAG, "pump=" + lastPump);
                listener.onSensorUpdated(Math.max(lastSoil, 0), Math.max(lastWater, 0), lastPump);
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {
                listener.onError(e.getMessage());
            }
        };
        pumpRef.addValueEventListener(pumpListener);
    }

    /** Hentikan semua listener */
    public void stopListening() {
        if (soilListener  != null) { soilRef.removeEventListener(soilListener);   soilListener  = null; }
        if (waterListener != null) { waterRef.removeEventListener(waterListener); waterListener = null; }
        if (pumpListener  != null) { pumpRef.removeEventListener(pumpListener);   pumpListener  = null; }
        if (pollingHandler != null && pollingRunnable != null) {
            pollingHandler.removeCallbacks(pollingRunnable);
            pollingRunnable = null;
        }
    }

    /** Tulis status pompa → dibaca IoT device */
    public void setPumpStatus(boolean isOn) {
        pumpRef.setValue(isOn);
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
                .addOnSuccessListener(a -> Log.d(TAG, "History saved: soil=" + lastSoil))
                .addOnFailureListener(e -> Log.e(TAG, "History save FAILED: " + e.getMessage()));
    }

    // ── Getter referensi ─────────────────────────────────────────────
    public DatabaseReference getSoilRef()    { return soilRef; }
    public DatabaseReference getWaterRef()   { return waterRef; }
    public DatabaseReference getPumpRef()    { return pumpRef; }
    public DatabaseReference getHistoryRef() { return historyRef; }

    public static String getCurrentUserId() {
        if (FirebaseAuth.getInstance().getCurrentUser() != null)
            return FirebaseAuth.getInstance().getCurrentUser().getUid();
        return "USR002";
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
