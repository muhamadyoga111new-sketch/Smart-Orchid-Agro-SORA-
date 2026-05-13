package com.example.itprojek;

import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import androidx.annotation.NonNull;

/**
 * Membaca data real-time dari sensors/soil, sensors/water, sensors/pump
 * Setiap field dibaca secara TERPISAH agar tidak perlu izin baca root sensors/
 */
public class FirebaseDataManager {

    private static final String TAG    = "SORA-DB";
    private static final String DB_URL =
            "https://sora-app-9f18a-default-rtdb.asia-southeast1.firebasedatabase.app/";

    public interface SensorListener {
        void onSensorUpdated(int soilMoisture, int waterLevel, boolean pumpStatus);
        void onError(String errorMessage);
    }

    private final DatabaseReference soilRef;   // sensors/soil
    private final DatabaseReference waterRef;  // sensors/water
    private final DatabaseReference pumpRef;   // sensors/pump
    private final DatabaseReference historyRef;

    private ValueEventListener soilListener;
    private ValueEventListener waterListener;
    private ValueEventListener pumpListener;

    private int     lastSoil  = 0;
    private int     lastWater = 0;
    private boolean lastPump  = false;

    public FirebaseDataManager(String deviceId) {
        FirebaseDatabase db = FirebaseDatabase.getInstance(DB_URL);
        DatabaseReference sensorsRef = db.getReference("sensors");
        soilRef    = sensorsRef.child("soil");
        waterRef   = sensorsRef.child("water");
        pumpRef    = sensorsRef.child("pump");
        historyRef = db.getReference("history").child(deviceId);
    }

    /** Real-time listener untuk ketiga field sekaligus */
    public void listenSensorData(SensorListener listener) {
        stopListening();

        soilListener = new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) {
                lastSoil = toInt(s.getValue());
                Log.d(TAG, "soil=" + lastSoil);
                listener.onSensorUpdated(lastSoil, lastWater, lastPump);
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {
                listener.onError("soil: " + e.getMessage());
            }
        };

        waterListener = new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) {
                lastWater = toInt(s.getValue());
                Log.d(TAG, "water=" + lastWater);
                listener.onSensorUpdated(lastSoil, lastWater, lastPump);
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {
                listener.onError("water: " + e.getMessage());
            }
        };

        soilRef.addValueEventListener(soilListener);
        waterRef.addValueEventListener(waterListener);
    }

    /** Fetch sekali (polling) */
    public void fetchSensorDataOnce(SensorListener listener) {
        soilRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) {
                lastSoil = toInt(s.getValue());
                waterRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot sw) {
                        lastWater = toInt(sw.getValue());
                        Log.d(TAG, "poll soil=" + lastSoil + " water=" + lastWater);
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
                listener.onSensorUpdated(lastSoil, lastWater, lastPump);
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {
                listener.onError(e.getMessage());
            }
        };
        pumpRef.addValueEventListener(pumpListener);
    }

    public void stopListening() {
        if (soilListener  != null) { soilRef.removeEventListener(soilListener);   soilListener  = null; }
        if (waterListener != null) { waterRef.removeEventListener(waterListener); waterListener = null; }
        if (pumpListener  != null) { pumpRef.removeEventListener(pumpListener);   pumpListener  = null; }
    }

    public void setPumpStatus(boolean isOn) {
        pumpRef.setValue(isOn);
        lastPump = isOn;
    }

    public void updateSensorData(int soil, int water) {
        soilRef.setValue(soil);
        waterRef.setValue(water);
    }

    public DatabaseReference getHistoryRef() { return historyRef; }

    public static String getCurrentUserId() {
        if (FirebaseAuth.getInstance().getCurrentUser() != null)
            return FirebaseAuth.getInstance().getCurrentUser().getUid();
        return "USR002";
    }

    private int toInt(Object val) {
        if (val instanceof Long)    return ((Long) val).intValue();
        if (val instanceof Integer) return (Integer) val;
        if (val instanceof String)  { try { return Integer.parseInt((String) val); } catch (Exception ignored) {} }
        return 0;
    }
}
