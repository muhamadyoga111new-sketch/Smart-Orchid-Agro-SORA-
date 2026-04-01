-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Waktu pembuatan: 01 Apr 2026 pada 02.59
-- Versi server: 10.4.32-MariaDB
-- Versi PHP: 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `sora_db`
--

-- --------------------------------------------------------

--
-- Struktur dari tabel `data_sensor`
--

CREATE TABLE `data_sensor` (
  `id_data` varchar(10) NOT NULL,
  `id_perangkat` varchar(10) DEFAULT NULL,
  `timestamp` datetime DEFAULT NULL,
  `kelembapan_tanah` int(11) DEFAULT NULL,
  `level_air` int(11) DEFAULT NULL,
  `status_kelembapan` varchar(20) DEFAULT NULL,
  `status_tangki` varchar(20) DEFAULT NULL,
  `catatan` text DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data untuk tabel `data_sensor`
--

INSERT INTO `data_sensor` (`id_data`, `id_perangkat`, `timestamp`, `kelembapan_tanah`, `level_air`, `status_kelembapan`, `status_tangki`, `catatan`) VALUES
('SEN001', 'DEV001', '2024-03-01 07:00:00', 45, 80, 'Normal', 'Penuh', 'Kondisi baik'),
('SEN002', 'DEV001', '2024-03-01 07:30:00', 38, 78, 'Normal', 'Penuh', 'Kondisi baik'),
('SEN003', 'DEV001', '2024-03-01 08:00:00', 25, 75, 'Kering', 'Penuh', 'Perlu penyiraman'),
('SEN004', 'DEV002', '2024-03-01 08:30:00', 22, 70, 'Kering', 'Cukup', 'Pompa aktif'),
('SEN005', 'DEV001', '2024-03-01 09:00:00', 55, 68, 'Lembap', 'Cukup', 'Setelah penyiraman'),
('SEN006', 'DEV002', '2024-03-01 09:30:00', 58, 65, 'Lembap', 'Cukup', 'Normal'),
('SEN007', 'DEV001', '2024-03-01 10:00:00', 62, 62, 'Lembap', 'Cukup', 'Normal'),
('SEN008', 'DEV002', '2024-03-01 10:30:00', 48, 50, 'Normal', 'Setengah', 'Normal'),
('SEN009', 'DEV001', '2024-03-01 11:00:00', 40, 45, 'Normal', 'Setengah', 'Normal'),
('SEN010', 'DEV002', '2024-03-01 11:30:00', 20, 30, 'Kering', 'Rendah', 'Perlu isi tangki');

-- --------------------------------------------------------

--
-- Struktur dari tabel `jadwal_penyiraman`
--

CREATE TABLE `jadwal_penyiraman` (
  `id_jadwal` varchar(10) NOT NULL,
  `id_pengguna` varchar(10) DEFAULT NULL,
  `id_perangkat` varchar(10) DEFAULT NULL,
  `waktu` time DEFAULT NULL,
  `durasi` int(11) DEFAULT NULL,
  `senin` varchar(5) DEFAULT NULL,
  `selasa` varchar(5) DEFAULT NULL,
  `rabu` varchar(5) DEFAULT NULL,
  `kamis` varchar(5) DEFAULT NULL,
  `jumat` varchar(5) DEFAULT NULL,
  `sabtu` varchar(5) DEFAULT NULL,
  `auto_watering` varchar(5) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data untuk tabel `jadwal_penyiraman`
--

INSERT INTO `jadwal_penyiraman` (`id_jadwal`, `id_pengguna`, `id_perangkat`, `waktu`, `durasi`, `senin`, `selasa`, `rabu`, `kamis`, `jumat`, `sabtu`, `auto_watering`) VALUES
('JDW001', 'USR002', 'DEV003', '07:00:00', 10, 'Ya', 'Ya', 'Ya', 'Ya', 'Ya', 'Ya', 'Ya'),
('JDW002', 'USR002', 'DEV003', '17:00:00', 15, 'Ya', 'Tidak', 'Ya', 'Tidak', 'Ya', 'Tidak', 'Tidak'),
('JDW003', 'USR003', 'DEV005', '06:30:00', 20, 'Ya', 'Ya', 'Ya', 'Ya', 'Ya', 'Ya', 'Ya'),
('JDW004', 'USR003', 'DEV005', '16:00:00', 10, 'Tidak', 'Tidak', 'Ya', 'Tidak', 'Tidak', 'Ya', 'Ya'),
('JDW005', 'USR001', 'DEV003', '08:00:00', 5, 'Ya', 'Ya', 'Ya', 'Ya', 'Ya', 'Tidak', 'Tidak');

-- --------------------------------------------------------

--
-- Struktur dari tabel `kalibrasi_sensor`
--

CREATE TABLE `kalibrasi_sensor` (
  `id_kalibrasi` varchar(10) NOT NULL,
  `id_perangkat` varchar(10) DEFAULT NULL,
  `id_pengguna` varchar(10) DEFAULT NULL,
  `timestamp` datetime DEFAULT NULL,
  `batas_kering` int(11) DEFAULT NULL,
  `batas_normal` int(11) DEFAULT NULL,
  `batas_lembap` int(11) DEFAULT NULL,
  `catatan` text DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data untuk tabel `kalibrasi_sensor`
--

INSERT INTO `kalibrasi_sensor` (`id_kalibrasi`, `id_perangkat`, `id_pengguna`, `timestamp`, `batas_kering`, `batas_normal`, `batas_lembap`, `catatan`) VALUES
('KAL001', 'DEV001', 'USR002', '2024-02-05 10:00:00', 30, 60, 80, 'Kalibrasi awal pemasangan'),
('KAL002', 'DEV001', 'USR002', '2024-02-20 09:00:00', 28, 58, 82, 'Penyesuaian musim hujan'),
('KAL003', 'DEV004', 'USR003', '2024-02-12 11:00:00', 35, 65, 85, 'Kalibrasi awal pemasangan'),
('KAL004', 'DEV001', 'USR001', '2024-03-01 08:00:00', 30, 60, 80, 'Re-kalibrasi rutin'),
('KAL005', 'DEV004', 'USR003', '2024-03-10 09:30:00', 32, 62, 82, 'Penyesuaian');

-- --------------------------------------------------------

--
-- Struktur dari tabel `notifikasi`
--

CREATE TABLE `notifikasi` (
  `id_notif` varchar(10) NOT NULL,
  `id_pengguna` varchar(10) DEFAULT NULL,
  `timestamp` datetime DEFAULT NULL,
  `tipe_alert` varchar(50) DEFAULT NULL,
  `judul` varchar(100) DEFAULT NULL,
  `pesan` text DEFAULT NULL,
  `status_baca` varchar(20) DEFAULT NULL,
  `tindakan` varchar(100) DEFAULT NULL,
  `id_perangkat` varchar(10) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data untuk tabel `notifikasi`
--

INSERT INTO `notifikasi` (`id_notif`, `id_pengguna`, `timestamp`, `tipe_alert`, `judul`, `pesan`, `status_baca`, `tindakan`, `id_perangkat`) VALUES
('NOT001', 'USR002', '2024-03-01 08:05:00', 'Tanah Kering', 'Tanah Kering!', 'Kelembapan turun ke 22%. Penyiraman segera.', 'Sudah', 'Pompa ON', 'DEV001'),
('NOT002', 'USR002', '2024-03-02 07:00:00', 'Jadwal Penyiraman', 'Penyiraman Dimulai', 'Penyiraman otomatis dimulai pukul 07:00.', 'Sudah', 'Tidak ada', 'DEV003'),
('NOT003', 'USR003', '2024-03-03 11:30:00', 'Tangki Rendah', 'Level Air Rendah', 'Level air tangki 30%. Segera isi tangki.', 'Belum', 'Isi tangki', 'DEV002'),
('NOT004', 'USR002', '2024-03-05 14:00:00', 'Tangki Rendah', 'Level Air Kritis', 'Level air tangki hanya 15%!', 'Sudah', 'Isi tangki', 'DEV002'),
('NOT005', 'USR003', '2024-03-06 08:00:00', 'Kelembapan Tinggi', 'Terlalu Lembap', 'Kelembapan 85%. Penyiraman ditunda.', 'Belum', 'Tunda jadwal', 'DEV004'),
('NOT006', 'USR001', '2024-03-07 09:00:00', 'Perangkat Offline', 'Sensor Offline', 'DEV004 tidak merespons sejak 08:45.', 'Belum', 'Cek perangkat', 'DEV004'),
('NOT007', 'USR002', '2024-03-08 07:00:00', 'Jadwal Penyiraman', 'Penyiraman Selesai', 'Durasi 10 menit. Kelembapan: 55%.', 'Sudah', 'Tidak ada', 'DEV003');

-- --------------------------------------------------------

--
-- Struktur dari tabel `pengaturan_notifikasi`
--

CREATE TABLE `pengaturan_notifikasi` (
  `id_setting` varchar(10) NOT NULL,
  `id_pengguna` varchar(10) DEFAULT NULL,
  `notif_tanah_kering` varchar(5) DEFAULT NULL,
  `notif_tangki_rendah` varchar(5) DEFAULT NULL,
  `notif_terlalu_lembap` varchar(5) DEFAULT NULL,
  `notif_alarm_tangki` varchar(5) DEFAULT NULL,
  `last_updated` date DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data untuk tabel `pengaturan_notifikasi`
--

INSERT INTO `pengaturan_notifikasi` (`id_setting`, `id_pengguna`, `notif_tanah_kering`, `notif_tangki_rendah`, `notif_terlalu_lembap`, `notif_alarm_tangki`, `last_updated`) VALUES
('SET001', 'USR001', 'Ya', 'Ya', 'Tidak', 'Tidak', '2024-03-01'),
('SET002', 'USR002', 'Ya', 'Ya', 'Tidak', 'Tidak', '2024-03-01'),
('SET003', 'USR003', 'Ya', 'Ya', 'Ya', 'Ya', '2024-03-05'),
('SET004', 'USR004', 'Ya', 'Tidak', 'Tidak', 'Tidak', '2024-02-10'),
('SET005', 'USR005', 'Ya', 'Ya', 'Ya', 'Tidak', '2024-03-20');

-- --------------------------------------------------------

--
-- Struktur dari tabel `pengguna`
--

CREATE TABLE `pengguna` (
  `id_pengguna` varchar(10) NOT NULL,
  `nama_lengkap` varchar(100) DEFAULT NULL,
  `email` varchar(100) DEFAULT NULL,
  `no_hp` varchar(20) DEFAULT NULL,
  `peran` varchar(50) DEFAULT NULL,
  `tanggal_daftar` date DEFAULT NULL,
  `status_akun` varchar(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data untuk tabel `pengguna`
--

INSERT INTO `pengguna` (`id_pengguna`, `nama_lengkap`, `email`, `no_hp`, `peran`, `tanggal_daftar`, `status_akun`) VALUES
('USR001', 'Ahmad Fauzi', 'ahmad@sora.id', '081234567890', 'Admin', '2024-01-15', 'Aktif'),
('USR002', 'Siti Rahayu', 'siti@sora.id', '085678901234', 'Petani', '2024-02-01', 'Aktif'),
('USR003', 'Budi Santoso', 'budi@sora.id', '087890123456', 'Petani', '2024-02-10', 'Aktif'),
('USR004', 'Dewi Lestari', 'dewi@sora.id', '089012345678', 'Petani', '2024-03-05', 'Non-Aktif'),
('USR005', 'Rizki Pratama', 'rizki@sora.id', '081122334455', 'Admin', '2024-03-20', 'Aktif');

-- --------------------------------------------------------

--
-- Struktur dari tabel `perangkat_iot`
--

CREATE TABLE `perangkat_iot` (
  `id_perangkat` varchar(10) NOT NULL,
  `nama_perangkat` varchar(100) DEFAULT NULL,
  `tipe` varchar(100) DEFAULT NULL,
  `lokasi` varchar(100) DEFAULT NULL,
  `id_pengguna` varchar(10) DEFAULT NULL,
  `tanggal_pasang` date DEFAULT NULL,
  `status` varchar(20) DEFAULT NULL,
  `firmware` varchar(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data untuk tabel `perangkat_iot`
--

INSERT INTO `perangkat_iot` (`id_perangkat`, `nama_perangkat`, `tipe`, `lokasi`, `id_pengguna`, `tanggal_pasang`, `status`, `firmware`) VALUES
('DEV001', 'Sensor Kelembapan 1', 'Capacitive Soil Sensor', 'Kebun A - Baris 1', 'USR002', '2024-02-05', 'Online', 'v1.2.0'),
('DEV002', 'Sensor Level Air 1', 'Ultrasonic HC-SR04', 'Tangki Utama', 'USR002', '2024-02-05', 'Online', 'v1.1.3'),
('DEV003', 'Pompa Air 1', 'Water Pump 12V', 'Sistem Irigasi A', 'USR002', '2024-02-06', 'Online', 'v1.2.0'),
('DEV004', 'Sensor Kelembapan 2', 'Capacitive Soil Sensor', 'Kebun B - Baris 1', 'USR003', '2024-02-12', 'Offline', 'v1.1.0'),
('DEV005', 'Pompa Air 2', 'Water Pump 12V', 'Sistem Irigasi B', 'USR003', '2024-02-12', 'Online', 'v1.2.0'),
('DEV006', 'ESP32 Controller 1', 'ESP32 Dev Board', 'Server Room', 'USR001', '2024-01-20', 'Online', 'v2.0.1');

-- --------------------------------------------------------

--
-- Struktur dari tabel `riwayat_penyiraman`
--

CREATE TABLE `riwayat_penyiraman` (
  `id_riwayat` varchar(10) NOT NULL,
  `id_jadwal` varchar(10) DEFAULT NULL,
  `id_perangkat` varchar(10) DEFAULT NULL,
  `tanggal` date DEFAULT NULL,
  `waktu_mulai` time DEFAULT NULL,
  `waktu_selesai` time DEFAULT NULL,
  `durasi_aktual` int(11) DEFAULT NULL,
  `trigger_type` varchar(20) DEFAULT NULL,
  `kelembapan_sebelum` int(11) DEFAULT NULL,
  `kelembapan_sesudah` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data untuk tabel `riwayat_penyiraman`
--

INSERT INTO `riwayat_penyiraman` (`id_riwayat`, `id_jadwal`, `id_perangkat`, `tanggal`, `waktu_mulai`, `waktu_selesai`, `durasi_aktual`, `trigger_type`, `kelembapan_sebelum`, `kelembapan_sesudah`) VALUES
('RWY001', 'JDW001', 'DEV003', '2024-03-01', '07:00:00', '07:10:00', 10, 'Otomatis', 25, 55),
('RWY002', 'JDW001', 'DEV003', '2024-03-01', '17:00:00', '17:15:00', 15, 'Otomatis', 30, 60),
('RWY003', 'JDW003', 'DEV005', '2024-03-01', '06:30:00', '06:50:00', 20, 'Otomatis', 20, 58),
('RWY004', 'JDW002', 'DEV003', '2024-03-03', '07:00:00', '07:10:00', 10, 'Otomatis', 28, 52),
('RWY005', NULL, 'DEV003', '2024-03-05', '10:15:00', '10:20:00', 5, 'Manual', 33, 55),
('RWY006', 'JDW001', 'DEV003', '2024-03-05', '17:00:00', '17:15:00', 15, 'Otomatis', 29, 61),
('RWY007', NULL, 'DEV005', '2024-03-06', '14:30:00', '14:35:00', 5, 'Manual', 40, 62),
('RWY008', 'JDW003', 'DEV005', '2024-03-07', '06:30:00', '06:50:00', 20, 'Otomatis', 22, 57);

--
-- Indexes for dumped tables
--

--
-- Indeks untuk tabel `data_sensor`
--
ALTER TABLE `data_sensor`
  ADD PRIMARY KEY (`id_data`),
  ADD KEY `id_perangkat` (`id_perangkat`);

--
-- Indeks untuk tabel `jadwal_penyiraman`
--
ALTER TABLE `jadwal_penyiraman`
  ADD PRIMARY KEY (`id_jadwal`),
  ADD KEY `id_pengguna` (`id_pengguna`),
  ADD KEY `id_perangkat` (`id_perangkat`);

--
-- Indeks untuk tabel `kalibrasi_sensor`
--
ALTER TABLE `kalibrasi_sensor`
  ADD PRIMARY KEY (`id_kalibrasi`),
  ADD KEY `id_perangkat` (`id_perangkat`),
  ADD KEY `id_pengguna` (`id_pengguna`);

--
-- Indeks untuk tabel `notifikasi`
--
ALTER TABLE `notifikasi`
  ADD PRIMARY KEY (`id_notif`),
  ADD KEY `id_pengguna` (`id_pengguna`),
  ADD KEY `id_perangkat` (`id_perangkat`);

--
-- Indeks untuk tabel `pengaturan_notifikasi`
--
ALTER TABLE `pengaturan_notifikasi`
  ADD PRIMARY KEY (`id_setting`),
  ADD KEY `id_pengguna` (`id_pengguna`);

--
-- Indeks untuk tabel `pengguna`
--
ALTER TABLE `pengguna`
  ADD PRIMARY KEY (`id_pengguna`);

--
-- Indeks untuk tabel `perangkat_iot`
--
ALTER TABLE `perangkat_iot`
  ADD PRIMARY KEY (`id_perangkat`),
  ADD KEY `id_pengguna` (`id_pengguna`);

--
-- Indeks untuk tabel `riwayat_penyiraman`
--
ALTER TABLE `riwayat_penyiraman`
  ADD PRIMARY KEY (`id_riwayat`),
  ADD KEY `id_jadwal` (`id_jadwal`),
  ADD KEY `id_perangkat` (`id_perangkat`);

--
-- Ketidakleluasaan untuk tabel pelimpahan (Dumped Tables)
--

--
-- Ketidakleluasaan untuk tabel `data_sensor`
--
ALTER TABLE `data_sensor`
  ADD CONSTRAINT `data_sensor_ibfk_1` FOREIGN KEY (`id_perangkat`) REFERENCES `perangkat_iot` (`id_perangkat`);

--
-- Ketidakleluasaan untuk tabel `jadwal_penyiraman`
--
ALTER TABLE `jadwal_penyiraman`
  ADD CONSTRAINT `jadwal_penyiraman_ibfk_1` FOREIGN KEY (`id_pengguna`) REFERENCES `pengguna` (`id_pengguna`),
  ADD CONSTRAINT `jadwal_penyiraman_ibfk_2` FOREIGN KEY (`id_perangkat`) REFERENCES `perangkat_iot` (`id_perangkat`);

--
-- Ketidakleluasaan untuk tabel `kalibrasi_sensor`
--
ALTER TABLE `kalibrasi_sensor`
  ADD CONSTRAINT `kalibrasi_sensor_ibfk_1` FOREIGN KEY (`id_perangkat`) REFERENCES `perangkat_iot` (`id_perangkat`),
  ADD CONSTRAINT `kalibrasi_sensor_ibfk_2` FOREIGN KEY (`id_pengguna`) REFERENCES `pengguna` (`id_pengguna`);

--
-- Ketidakleluasaan untuk tabel `notifikasi`
--
ALTER TABLE `notifikasi`
  ADD CONSTRAINT `notifikasi_ibfk_1` FOREIGN KEY (`id_pengguna`) REFERENCES `pengguna` (`id_pengguna`),
  ADD CONSTRAINT `notifikasi_ibfk_2` FOREIGN KEY (`id_perangkat`) REFERENCES `perangkat_iot` (`id_perangkat`);

--
-- Ketidakleluasaan untuk tabel `pengaturan_notifikasi`
--
ALTER TABLE `pengaturan_notifikasi`
  ADD CONSTRAINT `pengaturan_notifikasi_ibfk_1` FOREIGN KEY (`id_pengguna`) REFERENCES `pengguna` (`id_pengguna`);

--
-- Ketidakleluasaan untuk tabel `perangkat_iot`
--
ALTER TABLE `perangkat_iot`
  ADD CONSTRAINT `perangkat_iot_ibfk_1` FOREIGN KEY (`id_pengguna`) REFERENCES `pengguna` (`id_pengguna`);

--
-- Ketidakleluasaan untuk tabel `riwayat_penyiraman`
--
ALTER TABLE `riwayat_penyiraman`
  ADD CONSTRAINT `riwayat_penyiraman_ibfk_1` FOREIGN KEY (`id_jadwal`) REFERENCES `jadwal_penyiraman` (`id_jadwal`),
  ADD CONSTRAINT `riwayat_penyiraman_ibfk_2` FOREIGN KEY (`id_perangkat`) REFERENCES `perangkat_iot` (`id_perangkat`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
