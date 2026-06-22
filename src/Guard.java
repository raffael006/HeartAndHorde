// Tambahkan baris package ini JIKA file java Anda yang lain juga memakainya.
// Jika file lain tidak pakai, hapus saja baris ini.
// package game;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.Serializable;

public class Guard implements Serializable {

    // Enum (Sistem Status & Tipe untuk memenuhi kaidah ADT)
    public enum GuardType { ARCHER, SPEARMAN }
    public enum GuardState { IDLE, MOVING, DEFENDING, CHASING }

    // Atribut Identitas
    public GuardType type;
    public GuardState state;
    public double targetX, targetY;
    public double speed = 4.0;

    // Atribut Posisi & Geometri
    public double x, y;
    public int size = 64; // Ukuran hitbox kita buat 64x64 agar gambarnya terlihat jelas
    public boolean isSelected = false;

    public Guard(GuardType type, double startX, double startY) {
        this.type = type;
        this.x = startX;
        this.y = startY;
        this.targetX = startX; // Di awal, target adalah posisinya sendiri
        this.targetY = startY;
        this.state = GuardState.IDLE;
    }

    // --- TAMBAHKAN FUNGSI UPDATE INI DI BAWAH CONSTRUCTOR ---
    // Fungsi update sekarang menerima daftar semua Guard aktif
    public void update(java.util.List<Guard> allGuards) {
        // --- 1. Logika Berjalan ke Target ---
        if (state == GuardState.MOVING) {
            double dx = targetX - x;
            double dy = targetY - y;
            double distance = Math.sqrt(dx * dx + dy * dy);

            if (distance > speed) {
                x += (dx / distance) * speed;
                y += (dy / distance) * speed;
            } else {
                x = targetX;
                y = targetY;
                state = GuardState.IDLE;
            }
        }

        // --- 2. Logika Anti-Tumpuk (Separation) ---
        // Berlaku setiap saat agar mereka otomatis menyebar saat sampai di tujuan
        int personalSpace = size - 15; // Jarak minimal antar pasukan (bisa disesuaikan)

        for (Guard other : allGuards) {
            if (other == this) continue; // Jangan cek dengan diri sendiri

            double dx = this.x - other.x;
            double dy = this.y - other.y;
            double distance = Math.sqrt(dx * dx + dy * dy);

            // Jika posisinya sama persis (tumpuk 100%), beri dorongan acak agar terpisah
            if (distance == 0) {
                this.x += Math.random() * 2 - 1;
                this.y += Math.random() * 2 - 1;
                continue;
            }

            // Jika jarak teman terlalu dekat, saling dorong menjauh!
            if (distance < personalSpace) {
                double pushForce = (personalSpace - distance) / personalSpace;
                double pushX = (dx / distance) * pushForce * 1.5; // 1.5 adalah kekuatan dorong
                double pushY = (dy / distance) * pushForce * 1.5;

                this.x += pushX;
                this.y += pushY;
            }
        }
    }

    // Fungsi untuk menggambar dirinya sendiri ke layar
    public void draw(Graphics2D g2d, BufferedImage fullSheet) {
        if (fullSheet != null) {
            // --- PERBAIKAN DI SINI ---
            // Kita tidak mengambil lebar keseluruhan gambar.
            // Asumsi 1 frame (1 pose karakter) berukuran 128x128 atau 64x64 piksel di gambar aslinya.
            int frameWidth = 170;  // Sesuaikan angka ini dengan ukuran asli 1 kotak di spritesheet-mu
            int frameHeight = 170; // Biasanya ukurannya persegi

            // Menggambar potongan gambar (Hanya frame ke-1 di pojok kiri atas)
            g2d.drawImage(fullSheet,
                    (int)x, (int)y, (int)x + size, (int)y + size,
                    0, 0, frameWidth, frameHeight, null);
        } else {
            // Kotak warna-warni jika gambar gagal dimuat
            g2d.setColor(type == GuardType.ARCHER ? new Color(50, 150, 50) : new Color(50, 50, 150));
            g2d.fillRect((int)x, (int)y, size, size);
        }

        // Lingkaran kuning jika pasukan sedang di-select
        if (isSelected) {
            g2d.setColor(Color.YELLOW);
            g2d.setStroke(new BasicStroke(2));
            g2d.drawOval((int)x - 5, (int)y - 5, size + 10, size + 10);
        }
    }
}