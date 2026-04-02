package com.example.itprojek;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import androidx.annotation.NonNull;

/**
 * FirebaseDataManager
 * ─────────────────────────────────────────────────────────────────
 * Helper class untuk membaca dan menulis data sensor SORA
 * dari/ke Firebase Realtime Database.
 *
 * Struktur Database:
 * sora-app/
 * └── sensors/
 *     └── {deviceId}/
 *         ├── soilMoisture   : int  (0–100 %)
 *         ├── waterLevel     : int  (0–100 %)
 *         ├── pumpStatus     : boolean
 *         └── lastUpdated    : String (ISO-8601)
 *
 * Cara pakai:
 *   FirebaseDataManager dm = new FirebaseDataManager("ANG-123456");
 *   dm.listenSensorData(new FirebaseDataManager.SensorListener() { ... });
 *   dm.setPumpStatus(true);
 */
public class FirebaseDataManager {

    // ── Callback interface ──────────────────────────────────────────
    public interface SensorListener {
        void onSensorUpdated(int soilMoisture, int waterLevel, boolean pumpStatus);
        void onError(String errorMessage);
    }

    // ── Konstanta path ──────────────────────────────────────────────
    private static final String DB_URL =
            "https://sora-app-9f18a-default-rtdb.asia-southeast1.firebasedatabase.app/";

    private static final String PATH_SENSORS = "sensors";

    // ── Fields ──────────────────────────────────────────────────────
    private final DatabaseReference sensorRef;
    private ValueEventListener      activeListener;

    // ── Constructor ─────────────────────────────────────────────────
    public FirebaseDataManager(String deviceId) {
        FirebaseDatabase db = FirebaseDatabase.getInstance(DB_URL);
        sensorRef = db.getReference(PATH_SENSORS).child(deviceId);
    }

    // ── Mulai dengarkan perubahan data sensor secara real-time ───────
    public void listenSensorData(SensorListener listener) {
        activeListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Ambil nilai dengan default aman jika null
                Integer soil   = snapshot.child("soilMoisture").getValue(Integer.class);
                Integer water  = snapshot.child("waterLevel").getValue(Integer.class);
                Boolean pump   = snapshot.child("pumpStatus").getValue(Boolean.class);

                listener.onSensorUpdated(
                        soil  != null ? soil  : 0,
                        water != null ? water : 0,
                        pump  != null && pump
                );
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onError(error.getMessage());
            }
        };
        sensorRef.addValueEventListener(activeListener);
    }

    // ── Berhenti mendengarkan (panggil di onDestroy / onPause) ───────
    public void stopListening() {
        if (activeListener != null) {
            sensorRef.removeEventListener(activeListener);
            activeListener = null;
        }
    }

    // ── Tulis status pompa ke Firebase (dibaca oleh IoT device) ──────
    public void setPumpStatus(boolean isOn) {
        sensorRef.child("pumpStatus").setValue(isOn);
    }

    // ── Tulis data sensor (dipakai oleh IoT device / simulator) ─────
    public void updateSensorData(int soilMoisture, int waterLevel) {
        sensorRef.child("soilMoisture").setValue(soilMoisture);
        sensorRef.child("waterLevel").setValue(waterLevel);
        sensorRef.child("lastUpdated")
                 .setValue(new java.text.SimpleDateFormat(
                         "yyyy-MM-dd'T'HH:mm:ss",
                         java.util.Locale.getDefault())
                         .format(new java.util.Date()));
    }

    // ── Ambil UID user yang sedang login ──────────────────────────────
    public static String getCurrentUserId() {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            return FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
        return null;
    }
}
