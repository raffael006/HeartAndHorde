import java.awt.*;
import java.io.Serializable;
import java.awt.geom.Rectangle2D;

public class Projectile implements Serializable {

    // Identitas
    private boolean fromPlayer; // true jika dari Guard, false jika dari Horde
    private double damage;

    // Posisi Real di Map (Sumbu X dan Y dasar)
    public double x, y;

    // Target yang diincar (Koordinat target SAAT DITEMBAKKAN)
    private double targetX, targetY;

    // Kecepatan
    private double vx, vy;
    private double speed = 6.0; // Seberapa cepat anak panah melesat

    // Logika Lengkungan (Parabola)
    private double currentDistance;
    private double totalDistance;
    private double maxHeight = 50.0; // Seberapa tinggi panah melambung

    // Status
    public boolean active = true;
    private boolean landed = false;
    private long landedTime;

    public Projectile(double startX, double startY, double trgX, double trgY, boolean fromPlayer, double damage) {
        this.x = startX;
        this.y = startY;
        this.targetX = trgX;
        this.targetY = trgY;
        this.fromPlayer = fromPlayer;
        this.damage = damage;

        // Hitung arah kecepatan
        double dx = targetX - x;
        double dy = targetY - y;
        totalDistance = Math.sqrt(dx * dx + dy * dy);

        // Jika jarak terlalu dekat, jangan tembak
        if (totalDistance < 1) {
            active = false;
            return;
        }

        // Normalisasi vektor kecepatan
        this.vx = (dx / totalDistance) * speed;
        this.vy = (dy / totalDistance) * speed;
        currentDistance = 0;
    }

    public void update() {
        if (!active || landed) return;

        // 1. Gerakkan proyektil di sumbu X dan Y dasar
        x += vx;
        y += vy;

        // Hitung jarak yang sudah ditempuh (untuk menghitung tinggi Z nanti)
        double dx = vx;
        double dy = vy;
        currentDistance += Math.sqrt(dx * dx + dy * dy);

        // 2. Cek apakah sudah sampai kordinat target
        if (currentDistance >= totalDistance) {
            // Pasikan posisinya tepat di target
            x = targetX;
            y = targetY;
            landed = true;
            landedTime = System.currentTimeMillis();
            return; // Berhenti mengecek damage, biarkan gambar menempel di tanah
        }

        // 3. Logika Hitbox saat di udara (Hanya berlaku untuk GUARD)
        // Kita tidak memakai check collision di sini, melainkan di GamePanel/GameWindow 
        // untuk efisiensi list objek.
    }

    // Ambil tinggi visual (Parabola terbalik berdasarkan jarak)
    private double getVisualZ() {
        if (landed) return 0; // Menempel di tanah

        // Rumus parabola sederhana: y = -4h(x^2 - x) di mana x adalah persentase jarak (0-1)
        double p = currentDistance / totalDistance; // Persentase perjalanan
        return 4 * maxHeight * p * (1 - p); // Mengembalikan nilai tinggi
    }

    public void draw(Graphics2D g2d) {
        if (!active) return;

        // Hitung posisi gambar di layar (Y real - Visual Z)
        double visualZ = getVisualZ();
        int drawX = (int) x;
        int drawY = (int) (y - visualZ); // Tarik gambar ke atas langit sesuai lengkungan

        // Set Warna putih kecil
        g2d.setColor(Color.WHITE);

        // Jika sedang melesat di udara, gambar garis sedikit tebal
        if (!landed) {
            // Biar makin WorldBox, garisnya kita buat miring sedikit mengikuti arah vx, vy
            g2d.setStroke(new BasicStroke(1.5f));
            // Garis tipis 3 pixel
            g2d.drawLine(drawX, drawY, (int)(drawX + (vx * 0.5)), (int)(drawY + (vy * 0.5)));
            g2d.setStroke(new BasicStroke(1.0f)); // Reset
        } else {
            // Jika sudah landed (menempel di tanah), gambar titik putih diam selama 1 detik
            g2d.fillRect(drawX - 1, drawY - 1, 2, 2);

            // Panah hilang setelah 1 detik menempel di tanah
            if (System.currentTimeMillis() - landedTime > 1000) {
                active = false;
            }
        }
    }

    // Getter untuk logika collision nanti
    public boolean hasLanded() { return landed; }
    public boolean isFromPlayer() { return fromPlayer; }
    public double getDamage() { return damage; }
    public Rectangle2D.Double getHitbox() {
        // Hitbox kecil di ujung panah (posisi X,Y dasar di map, bukan drawY langit)
        return new Rectangle2D.Double(x - 2, y - 2, 4, 4);
    }
}