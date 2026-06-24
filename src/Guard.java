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
    public double speed = 1.0;

    // Atribut Posisi & Geometri
    public double x, y;
    public int size = 24; // Ukuran hitbox kita buat 64x64 agar gambarnya terlihat jelas
    public boolean isSelected = false;

    // Atribut Status Pertarungan
    public double maxHp;
    public double currentHp;
    public double attackDamage; // Kita siapkan wadahnya sekarang

    // Atribut Pertarungan Lanjutan
    public long lastAttackTime = 0;
    public long attackCooldown = 1000; // 1000 ms = 1 detik jeda antar serangan
    public double attackRange = size + 5; // Jarak serang (nempel)
    public double sightRange = 200; // Jarak pandang mata untuk nyamperin musuh

    public Guard(GuardType type, double startX, double startY) {
        this.type = type;

        // --- PERBAIKAN ANCHOR POINT ---
        // Geser titik x dan y ke kiri dan ke atas sebanyak setengah ukuran (size / 2)
        // agar kursor mouse pas berada di tengah-tengah badan Guard.
        this.x = startX - (size / 2.0);
        this.y = startY - (size / 2.0);

        this.targetX = this.x; // Target juga harus disamakan dengan posisi x yang baru
        this.targetY = this.y; // Target juga harus disamakan dengan posisi y yang baru
        this.state = GuardState.IDLE;

        // Penentuan Status berdasarkan Tipe
        if (this.type == GuardType.SPEARMAN) {
            this.maxHp = 100;
            this.attackDamage = 15;
        } else if (this.type == GuardType.ARCHER) {
            this.maxHp = 60;
            this.attackDamage = 20;
        }

        // Darah penuh saat pertama kali spawn
        this.currentHp = this.maxHp;
    }

    // Fungsi update menerima tambahan daftar Proyektil (GameWindow.activeProjectiles)
    public void update(java.util.List<Guard> allGuards, java.util.List<Horde> allHordes, java.util.List<Projectile> allProjectiles) {

        // --- 1. Logika Berjalan Manual (Order Klik) (Dipertahankan) ---
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

        // --- 2. Logika PERTEMPURAN BERDASARKAN TIPE ---
        // Atribut Pertarungan Lanjutan
        long currentTime = System.currentTimeMillis();

        if (type == GuardType.ARCHER) {
            // --- LOGIKA ARCHER (PANAH) ---
            double attackRangePanah = 250.0; // Jarak tembak jauh
            Horde targetEnemy = null;
            double minDistance = sightRange;

            // Cari musuh terdekat dalam jarak pandang
            for (Horde enemy : allHordes) {
                double dx = this.x - enemy.x;
                double dy = this.y - enemy.y;
                double distance = Math.sqrt(dx * dx + dy * dy);

                if (distance < minDistance) {
                    minDistance = distance;
                    targetEnemy = enemy;
                }
            }

            if (targetEnemy != null) {
                // Jika musuh terlihat
                if (minDistance <= attackRangePanah) {
                    // 1. Masuk jarak tembak: Berhenti dan Menembak!

                    // JIKA sedang disuruh jalan manual, biarkan jalan dulu, jangan nembak (opsional, tergantung selera)
                    if (state == GuardState.MOVING) return;

                    if (currentTime - lastAttackTime >= attackCooldown) {
                        // BUAT PROYEKTIL BARU (Menembak ke lokasi musuh SAAT INI)
                        // true = dari player, attackDamage = 20
                        allProjectiles.add(new Projectile(x, y, targetEnemy.x, targetEnemy.y, true, attackDamage));
                        lastAttackTime = currentTime;
                    }
                } else if (state != GuardState.MOVING) {
                    // 2. Musuh terlihat tapi di luar jarak tembak: Maju sampai masuk jarak tembak
                    double dx = targetEnemy.x - this.x;
                    double dy = targetEnemy.y - this.y;
                    this.x += (dx / minDistance) * speed;
                    this.y += (dy / minDistance) * speed;
                }
            }

        } else if (type == GuardType.SPEARMAN) {
            // --- LOGIKA SPEARMAN (MELEE - Tetap seperti aslinya) ---
            double attackRangeMelee = size + 5;
            Horde targetEnemyMelee = null;
            double minDistanceMelee = sightRange;

            for (Horde enemy : allHordes) {
                double dx = this.x - enemy.x;
                double dy = this.y - enemy.y;
                double distanceMelee = Math.sqrt(dx * dx + dy * dy);
                if (distanceMelee < minDistanceMelee) {
                    minDistanceMelee = distanceMelee;
                    targetEnemyMelee = enemy;
                }
            }

            if (targetEnemyMelee != null) {
                if (minDistanceMelee <= attackRangeMelee) {
                    if (currentTime - lastAttackTime >= attackCooldown) {
                        targetEnemyMelee.currentHp -= this.attackDamage;
                        lastAttackTime = currentTime;
                    }
                } else if (state != GuardState.MOVING) {
                    double dx = targetEnemyMelee.x - this.x;
                    double dy = targetEnemyMelee.y - this.y;
                    this.x += (dx / minDistanceMelee) * speed;
                    this.y += (dy / minDistanceMelee) * speed;
                }
            }
        }


        // --- 3. Logika Anti-Tumpuk (Dipertahankan) ---
        int personalSpace = size - 5;
        for (Guard other : allGuards) {
            if (other == this) continue;
            double dx = this.x - other.x;
            double dy = this.y - other.y;
            double distanceSq = dx * dx + dy * dy;
            if (distanceSq == 0) {
                this.x += Math.random() * 2 - 1; this.y += Math.random() * 2 - 1; continue;
            }
            if (distanceSq < personalSpace * personalSpace) {
                double distance = Math.sqrt(distanceSq);
                double pushForce = (personalSpace - distance) / personalSpace;
                this.x += (dx / distance) * pushForce * 1.5;
                this.y += (dy / distance) * pushForce * 1.5;
            }
        }
        for (Horde enemy : allHordes) {
            double dx = this.x - enemy.x;
            double dy = this.y - enemy.y;
            double distanceSq = dx * dx + dy * dy;
            if (distanceSq < personalSpace * personalSpace && distanceSq > 0) {
                double distance = Math.sqrt(distanceSq);
                double pushForce = (personalSpace - distance) / personalSpace;
                this.x += (dx / distance) * pushForce * 1.5;
                this.y += (dy / distance) * pushForce * 1.5;
            }
        }
    }

    public void draw(Graphics2D g2d, BufferedImage fullSheet) {

        // --- LAYER 1: LINGKARAN KUNING (Digambar Paling Bawah) ---
        if (isSelected) {
            g2d.setColor(Color.YELLOW);
            g2d.setStroke(new BasicStroke(1.5f));

            int ovalWidth = (int) (size * 0.50);
            int ovalHeight = (int) ((size / 2) * 0.50);

            int ovalX = (int)x + (size - ovalWidth) / 2;

            // POSISI VERTIKAL OVAL:
            // Tambahkan minus (-) untuk menaikkan posisinya ke atas.
            // Silakan ubah angka 8 ini jika masih kurang naik atau terlalu naik.
            int ovalY = (int)y + size - (ovalHeight / 2) - 4;

            g2d.drawOval(ovalX, ovalY, ovalWidth, ovalHeight);
        }

        // --- LAYER 2: GAMBAR KARAKTER (Menutupi Lingkaran) ---
        if (fullSheet != null) {
            // Langsung menggambar seluruh gambar (fullSheet) ke ukuran hitbox (size)
            g2d.drawImage(fullSheet, (int)x, (int)y, size, size, null);

        } else {
            // Kotak warna cadangan jika gambar aset gagal dimuat
            g2d.setColor(type == GuardType.ARCHER ? new Color(50, 150, 50) : new Color(50, 50, 150));
            g2d.fillRect((int)x, (int)y, size, size);
        }

        // --- LAYER 3: BAR DARAH (Paling Atas, Tidak Boleh Tertutup Karakter) ---
        int barWidth = size;
        int barHeight = 4;
        int barX = (int) x;
        int barY = (int) y - 8; // Muncul sedikit di atas kepala guard

        // 1. Gambar background merah (darah kosong/berkurang)
        g2d.setColor(Color.RED);
        g2d.fillRect(barX, barY, barWidth, barHeight);

        // 2. Gambar bar hijau (darah saat ini)
        double hpPercentage = currentHp / maxHp;
        // Pastikan darah tidak minus secara visual
        if (hpPercentage < 0) hpPercentage = 0;
        int currentBarWidth = (int) (barWidth * hpPercentage);

        g2d.setColor(Color.GREEN);
        g2d.fillRect(barX, barY, currentBarWidth, barHeight);

        // 3. Outline hitam agar bar terlihat rapi dan tegas
        g2d.setColor(Color.BLACK);
        g2d.drawRect(barX, barY, barWidth, barHeight);
    }
}