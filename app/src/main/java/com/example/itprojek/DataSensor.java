package com.example.itprojek;

public class DataSensor implements Comparable<DataSensor> {
    public String catatan;
    public String id_data;
    public String id_perangkat;
    public String kelembapan_tanah;
    public String level_air;
    public String status_kelembapan;
    public String status_tangki;
    public String timestamp;

    public DataSensor() {}

    @Override
    public int compareTo(DataSensor o) {
        if (this.timestamp == null || o.timestamp == null) return 0;
        return this.timestamp.compareTo(o.timestamp);
    }
}
