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
    private final DatabaseReference rootRef;
    private final DatabaseReference sensorRef;
    private final DatabaseReference pumpRef; // Direct reference for real-time pump relay
    private ValueEventListener      activeListener;
    private ValueEventListener      pumpListener;

    private int lastKnownSoil = 0;
    private int lastKnownWater = 0;
    private boolean lastKnownPump = false;

    // ── Constructor ─────────────────────────────────────────────────
    public FirebaseDataManager(String deviceId) {
        FirebaseDatabase db = FirebaseDatabase.getInstance(DB_URL);
        // Listen to the root to dynamically find the 'data_sensor' history table
        rootRef = db.getReference();
        // Specifically map to the device node
        sensorRef = db.getReference(PATH_SENSORS).child(deviceId);
        // Specific live node for two-way hardware pump toggle
        pumpRef = sensorRef.child("pumpStatus");
    }

    // ── Mulai dengarkan perubahan data sensor secara real-time ───────
    public void listenSensorData(SensorListener listener) {
        
        // 1. Listen for Historical Sensor Data (Soil/Water) from PHPMyAdmin array
        activeListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                DataSnapshot dataSensorTable = null;
                for (DataSnapshot tableNode : snapshot.getChildren()) {
                    DataSnapshot nameNode = tableNode.child("name");
                    if (nameNode.exists() && "data_sensor".equals(nameNode.getValue(String.class))) {
                        dataSensorTable = tableNode.child("data");
                        break;
                    }
                }

                if (dataSensorTable != null && dataSensorTable.exists()) {
                    DataSensor latestSensor = null;
                    for (DataSnapshot ds : dataSensorTable.getChildren()) {
                        DataSensor sensor = ds.getValue(DataSensor.class);
                        // Using DEV001 to sync with the SQL dump records
                        if (sensor != null && "DEV001".equals(sensor.id_perangkat)) {
                            if (latestSensor == null || sensor.timestamp.compareTo(latestSensor.timestamp) > 0) {
                                latestSensor = sensor;
                            }
                        }
                    }

                    if (latestSensor != null) {
                        try { lastKnownSoil = Integer.parseInt(latestSensor.kelembapan_tanah); } catch(Exception ignored){}
                        try { lastKnownWater = Integer.parseInt(latestSensor.level_air); } catch(Exception ignored){}
                        listener.onSensorUpdated(lastKnownSoil, lastKnownWater, lastKnownPump);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onError(error.getMessage());
            }
        };
        rootRef.addValueEventListener(activeListener);

        // 2. Listen for Real-Time Pump Status (Bi-directional)
        pumpListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean pump = snapshot.getValue(Boolean.class);
                if (pump != null) {
                    lastKnownPump = pump;
                    listener.onSensorUpdated(lastKnownSoil, lastKnownWater, lastKnownPump);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onError(error.getMessage());
            }
        };
        pumpRef.addValueEventListener(pumpListener);
    }

    // ── Berhenti mendengarkan (panggil di onDestroy / onPause) ───────
    public void stopListening() {
        if (activeListener != null) {
            rootRef.removeEventListener(activeListener);
            activeListener = null;
        }
        if (pumpListener != null) {
            pumpRef.removeEventListener(pumpListener);
            pumpListener = null;
        }
    }

    // ── Tulis status pompa ke Firebase (dibaca oleh IoT device) ──────
    public void setPumpStatus(boolean isOn) {
        pumpRef.setValue(isOn);
        lastKnownPump = isOn;
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
        return "USR002"; // Fallback Test ID
    }
}
