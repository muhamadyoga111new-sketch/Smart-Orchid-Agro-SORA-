package com.example.itprojek;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/**
 * SORAMessagingService
 * ─────────────────────────────────────────────────────────────────
 * Handler untuk Firebase Cloud Messaging (FCM).
 * Notifikasi dikirim dari server/IoT saat terjadi kondisi kritis:
 *   - Tanah terlalu kering (kelembapan < 20%)
 *   - Tangki air hampir habis (level < 15%)
 *   - Pompa menyala / mati otomatis
 *
 * Payload contoh (dikirim dari server):
 * {
 *   "to": "<fcm-token>",
 *   "notification": {
 *     "title": "⚠️ Tanah Kering",
 *     "body": "Kelembapan tanah sangat rendah! Segera periksa sistem."
 *   },
 *   "data": {
 *     "type": "SOIL_DRY",
 *     "soilMoisture": "12"
 *   }
 * }
 */
public class SORAMessagingService extends FirebaseMessagingService {

    private static final String TAG          = "SORAMessaging";
    private static final String CHANNEL_ID   = "sora_alerts";
    private static final String CHANNEL_NAME = "SORA Peringatan";

    // ─────────────────────────────────────────────────────────────────
    //  Dipanggil saat ada pesan masuk (ketika app foreground / background)
    // ─────────────────────────────────────────────────────────────────
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, "Pesan dari: " + remoteMessage.getFrom());

        String title = "SORA";
        String body  = "Ada notifikasi baru dari sistem.";

        // Ambil dari notification payload
        if (remoteMessage.getNotification() != null) {
            if (remoteMessage.getNotification().getTitle() != null)
                title = remoteMessage.getNotification().getTitle();
            if (remoteMessage.getNotification().getBody() != null)
                body  = remoteMessage.getNotification().getBody();
        }

        // Fallback: ambil dari data payload
        if (remoteMessage.getData().containsKey("title"))
            title = remoteMessage.getData().get("title");
        if (remoteMessage.getData().containsKey("body"))
            body  = remoteMessage.getData().get("body");

        showNotification(title, body);
    }

    // ─────────────────────────────────────────────────────────────────
    //  Dipanggil saat token FCM diperbarui — simpan ke Realtime DB
    // ─────────────────────────────────────────────────────────────────
    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        Log.d(TAG, "Token FCM diperbarui: " + token);
        saveFcmTokenToDatabase(token);
    }

    // ── Tampilkan notifikasi lokal ────────────────────────────────────
    private void showNotification(String title, String body) {
        createNotificationChannel();

        // Tap notifikasi → buka MainActivity
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                        .setAutoCancel(true)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setContentIntent(pendingIntent);

        NotificationManager nm =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    // ── Buat notification channel (wajib di Android 8+) ──────────────
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Peringatan sistem penyiraman SORA");
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    // ── Simpan token FCM ke Firebase Realtime Database ────────────────
    private void saveFcmTokenToDatabase(String token) {
        String uid = FirebaseDataManager.getCurrentUserId();
        if (uid == null) return;

        com.google.firebase.database.FirebaseDatabase
                .getInstance("https://sora-app-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("users")
                .child(uid)
                .child("fcmToken")
                .setValue(token);
    }
}
