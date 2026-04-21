package com.example.itprojek;

public class Notifikasi implements Comparable<Notifikasi> {
    public String id_notif;
    public String id_pengguna;
    public String id_perangkat;
    public String judul;
    public String pesan;
    public String status_baca;
    public String timestamp;
    public String tindakan;
    public String tipe_alert;

    public Notifikasi() {}

    @Override
    public int compareTo(Notifikasi o) {
        if (this.timestamp == null || o.timestamp == null) return 0;
        // Reverse order so newest notifications are top
        return o.timestamp.compareTo(this.timestamp);
    }
}
