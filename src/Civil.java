import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.Random;

public class Civil implements Serializable {
    private static final long serialVersionUID = 1L;

    // ADT Status Sipil
    public enum CivilState { IDLE, WANDERING }

    // Atribut Posisi & Geometri
    public double x, y;
    public double homeX, homeY; // Titik pusat rumah/bangunan mereka
    public double targetX, targetY;

    // Ukuran 20 (Sengaja dibuat sedikit lebih kecil dari Guard yang ukurannya 24)
    public int size = 20;
    public double speed = 0.6; // Kecepatan jalan santai, lebih lambat dari Guard

    // Atribut Behavior (Tingkah Laku)
    public CivilState state;
    private long lastStateChange = 0;
    private int stateDuration = 2000;
    private Random random = new Random();
    private int wanderRadius = 120; // Jarak maksimal mereka boleh main jauh dari rumah

    public Civil(double spawnX, double spawnY) {
        // Geser ke tengah kursor/rumah
        this.x = spawnX - (size / 2.0);
        this.y = spawnY - (size / 2.0);

        // Catat titik asal sebagai "Rumah"
        this.homeX = this.x;
        this.homeY = this.y;

        this.targetX = this.x;
        this.targetY = this.y;
        this.state = CivilState.IDLE;
    }

    public void update() {
        long currentTime = System.currentTimeMillis();

        // 1. Timer Keputusan (Ganti aksi jika waktunya habis)
        if (currentTime - lastStateChange > stateDuration) {

            // 50% Peluang untuk Jalan, 50% Peluang untuk Diam
            if (random.nextBoolean()) {
                state = CivilState.WANDERING;

                // Rumus Trignometri: Mengacak titik di dalam lingkaran (Radius rumah)
                double angle = random.nextDouble() * Math.PI * 2;
                double radius = random.nextDouble() * wanderRadius;

                targetX = homeX + Math.cos(angle) * radius;
                targetY = homeY + Math.sin(angle) * radius;

                stateDuration = 2000 + random.nextInt(4000); // Berjalan selama 2-6 detik
            } else {
                state = CivilState.IDLE;
                stateDuration = 1000 + random.nextInt(3000); // Terdiam merenung selama 1-4 detik
            }

            lastStateChange = currentTime;
        }

        // 2. Eksekusi Berjalan
        if (state == CivilState.WANDERING) {
            double dx = targetX - x;
            double dy = targetY - y;
            double distance = Math.sqrt(dx * dx + dy * dy);

            if (distance > speed) {
                x += (dx / distance) * speed;
                y += (dy / distance) * speed;
            } else {
                // Jika sudah sampai tujuan sebelum waktunya habis, langsung diam
                state = CivilState.IDLE;
            }
        }
    }

    public void draw(Graphics2D g2d, BufferedImage img) {
        if (img != null) {
            // Menggambar gambar civil_h&h.png yang kamu upload
            g2d.drawImage(img, (int)x, (int)y, size, size, null);
        } else {
            // Warna kulit pucat/baju kusam sebagai kotak cadangan
            g2d.setColor(new Color(210, 170, 140));
            g2d.fillRect((int)x, (int)y, size, size);

            // Garis tepi
            g2d.setColor(new Color(100, 60, 40));
            g2d.drawRect((int)x, (int)y, size, size);
        }
    }
}