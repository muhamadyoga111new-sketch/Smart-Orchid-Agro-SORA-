package com.example.itprojek;

public class Riwayat implements Comparable<Riwayat> {
    public String durasi_aktual;
    public String id_jadwal;
    public String id_perangkat;
    public String id_riwayat;
    public String kelembapan_sebelum;
    public String kelembapan_sesudah;
    public String tanggal; // Format: yyyy-MM-dd
    public String trigger_type;
    public String waktu_mulai; // Format: HH:mm:ss
    public String waktu_selesai;

    public Riwayat() {
        // Default constructor required for calls to DataSnapshot.getValue(Riwayat.class)
    }

    public String getDurasi_aktual() { return durasi_aktual; }
    public String getId_jadwal() { return id_jadwal; }
    public String getId_perangkat() { return id_perangkat; }
    public String getId_riwayat() { return id_riwayat; }
    public String getKelembapan_sebelum() { return kelembapan_sebelum; }
    public String getKelembapan_sesudah() { return kelembapan_sesudah; }
    public String getTanggal() { return tanggal; }
    public String getTrigger_type() { return trigger_type; }
    public String getWaktu_mulai() { return waktu_mulai; }
    public String getWaktu_selesai() { return waktu_selesai; }

    // Sort by Date (descending) and then Time (descending)
    @Override
    public int compareTo(Riwayat other) {
        if (this.tanggal == null || other.tanggal == null) return 0;
        int dateCmp = other.tanggal.compareTo(this.tanggal);
        if (dateCmp != 0) {
            return dateCmp;
        }
        if (this.waktu_mulai == null || other.waktu_mulai == null) return 0;
        return other.waktu_mulai.compareTo(this.waktu_mulai);
    }
}
