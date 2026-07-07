import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.List;

public class Horde implements Serializable {

    // Enum (Sistem Status & Tipe)
    // AXEMAN = 1Horde, SHIELDBEARER = 2Horde, BOWMAN = 3Horde
    public enum HordeType { AXEMAN, SHIELDBEARER, BOWMAN }
    public enum HordeState { IDLE, MOVING, ATTACKING }

    // Atribut Identitas
    public HordeType type;
    public HordeState state;
    public double speed;

    // Atribut Posisi & Geometri
    public double x, y;
    public int size = 24; // Samakan dengan Guard agar proporsinya imbang

    // --- FITUR BARU: ARAH HADAP (Kiri/Kanan) ---
    // Default false = menghadap kiri (sesuai gambar asli 1Horde.png, 2Horde.png, 3Horde.png)
    public boolean facingRight = false;

    // Atribut Status Pertarungan (Samsak)
    public double maxHp;
    public double currentHp;
    public double attackDamage;

    // Atribut Pertarungan Lanjutan
    public long lastAttackTime = 0;
    public long attackCooldown = 1200; // Musuh memukul sedikit lebih lambat (1.2 detik)
    public double attackRange = size + 5;

    public Horde(HordeType type, double startX, double startY) {
        this.type = type;

        // Geser anchor point agar pas di tengah kursor saat di-spawn
        this.x = startX - (size / 2.0);
        this.y = startY - (size / 2.0);
        this.state = HordeState.IDLE;

        // Penentuan Status berdasarkan Tipe Horde
        if (this.type == HordeType.AXEMAN) {
            this.maxHp = 80;
            this.attackDamage = 10;
            this.speed = 1.0;
        } else if (this.type == HordeType.SHIELDBEARER) {
            this.maxHp = 150; // Darah paling tebal (Tank)
            this.attackDamage = 5;
            this.speed = 0.7; // Paling lambat
        } else if (this.type == HordeType.BOWMAN) {
            this.maxHp = 50;  // Darah tipis
            this.attackDamage = 15;
            this.speed = 1.5;
        }

        // Darah penuh saat pertama kali spawn
        this.currentHp = this.maxHp;
    }

    // Fungsi update menerima tambahan daftar Proyektil (GameWindow.activeProjectiles)
    public void update(List<Horde> allHordes, List<Guard> allGuards, java.util.List<Projectile> allProjectiles) {

        long currentTime = System.currentTimeMillis();

        // Cari Guard terdekat untuk disamperin
        Guard targetGuard = null;
        double minDistance = 9999;
        for (Guard guard : allGuards) {
            double dx = this.x - guard.x;
            double dy = this.y - guard.y;
            double distance = Math.sqrt(dx * dx + dy * dy);
            if (distance < minDistance) {
                minDistance = distance;
                targetGuard = guard;
            }
        }

        if (targetGuard != null) {
            if (type == HordeType.BOWMAN) {
                // --- LOGIKA BOWMAN (PANAH) ---
                double attackRangePanahMusuh = 230.0; // Sedikit lebih pendek dari player biar imbang

                if (minDistance <= attackRangePanahMusuh) {
                    // Masuk jarak tembak: Berhenti dan Tembak (Cek cooldown)
                    // --- FITUR BARU: Saat menembak, tetap hadap ke arah target ---
                    if (targetGuard.x > this.x + 0.1) facingRight = true;
                    else if (targetGuard.x < this.x - 0.1) facingRight = false;

                    if (currentTime - lastAttackTime >= attackCooldown) {
                        // BUAT PROYEKTIL BARU (false = dari horde, damage 15)
                        allProjectiles.add(new Projectile(x, y, targetGuard.x, targetGuard.y, false, attackDamage));
                        lastAttackTime = currentTime;
                    }
                } else {
                    // Di luar jarak tembak: Maju!
                    double dx = targetGuard.x - this.x;
                    double dy = targetGuard.y - this.y;
                    // --- FITUR BARU: Update arah hadap sesuai arah maju ---
                    if (dx > 0.1) facingRight = true;
                    else if (dx < -0.1) facingRight = false;

                    this.x += (dx / minDistance) * speed;
                    this.y += (dy / minDistance) * speed;
                }

            } else {
                // --- LOGIKA MELEE (AXEMAN & SHIELD - Tetap aslinya) ---
                double attackRangeMeleeMusuh = size + 5;
                if (minDistance <= attackRangeMeleeMusuh) {
                    // --- FITUR BARU: Saat memukul, tetap hadap ke arah target ---
                    if (targetGuard.x > this.x + 0.1) facingRight = true;
                    else if (targetGuard.x < this.x - 0.1) facingRight = false;

                    if (currentTime - lastAttackTime >= attackCooldown) {
                        targetGuard.currentHp -= this.attackDamage;
                        lastAttackTime = currentTime;
                    }
                } else {
                    double dx = targetGuard.x - this.x;
                    double dy = targetGuard.y - this.y;
                    // --- FITUR BARU: Update arah hadap sesuai arah maju ---
                    if (dx > 0.1) facingRight = true;
                    else if (dx < -0.1) facingRight = false;

                    this.x += (dx / minDistance) * speed;
                    this.y += (dy / minDistance) * speed;
                }
            }
        }

        // --- Logika Anti-Tumpuk (Dipertahankan) ---
        int personalSpace = size - 5;
        for (Horde other : allHordes) {
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
        for (Guard guard : allGuards) {
            double dx = this.x - guard.x;
            double dy = this.y - guard.y;
            double distanceSq = dx * dx + dy * dy;
            if (distanceSq < personalSpace * personalSpace && distanceSq > 0) {
                double distance = Math.sqrt(distanceSq);
                double pushForce = (personalSpace - distance) / personalSpace;
                this.x += (dx / distance) * pushForce * 1.5;
                this.y += (dy / distance) * pushForce * 1.5;
            }
        }
    }

    public void draw(Graphics2D g2d, BufferedImage img) {

        // --- LAYER 1: AURA MERAH (Pengganti lingkaran seleksi) ---
        g2d.setColor(new Color(200, 0, 0, 100)); // Merah transparan
        int ovalWidth = (int) (size * 0.50);
        int ovalHeight = (int) ((size / 2) * 0.50);
        int ovalX = (int)x + (size - ovalWidth) / 2 +3;
        int ovalY = (int)y + size - (ovalHeight / 2) - 4; // Posisinya sama persis dengan Guard
        g2d.fillOval(ovalX, ovalY, ovalWidth, ovalHeight);

        // --- LAYER 2: GAMBAR HORDE ---
        if (img != null) {
            // --- FITUR BARU: Flip gambar horizontal kalau lagi menghadap kanan ---
            // (Sprite asli Horde defaultnya menghadap KIRI, kebalikan dari Guard)
            if (facingRight) {
                java.awt.geom.AffineTransform oldTransform = g2d.getTransform();
                g2d.translate((int) x + size, (int) y);
                g2d.scale(-1, 1);
                g2d.drawImage(img, 0, 0, size, size, null);
                g2d.setTransform(oldTransform);
            } else {
                // Gambar langsung mengikuti ukuran hitbox (size x size) agar proporsinya sama dengan Guard
                g2d.drawImage(img, (int)x, (int)y, size, size, null);
            }
        } else {
            g2d.setColor(Color.RED);
            g2d.fillRect((int)x, (int)y, size, size);
        }

        // --- LAYER 3: BAR DARAH MUSUH ---
        int barWidth = size;
        int barHeight = 4;
        int barX = (int) x;
        int barY = (int) y - 8; // Posisi bar darah disamakan dengan Guard

        // Background merah gelap
        g2d.setColor(new Color(150, 0, 0));
        g2d.fillRect(barX, barY, barWidth, barHeight);

        // Darah merah terang
        double hpPercentage = currentHp / maxHp;
        if (hpPercentage < 0) hpPercentage = 0;
        int currentBarWidth = (int) (barWidth * hpPercentage);

        g2d.setColor(new Color(255, 50, 50));
        g2d.fillRect(barX, barY, currentBarWidth, barHeight);

        // Outline hitam
        g2d.setColor(Color.BLACK);
        g2d.drawRect(barX, barY, barWidth, barHeight);
    }
}