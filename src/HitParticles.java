import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * FITUR BARU: sistem particle burst super ringan buat "juice" combat.
 * Gak butuh asset/sprite baru sama sekali -- cuma titik-titik kecil (fillOval)
 * yang muncrat dari titik damage/kematian lalu jatuh & fade out.
 *
 * Dipakai statis (satu kolam partikel buat seluruh game) supaya titik pakainya
 * (Horde.java, Guard.java, GamePanel.java) gak perlu inject instance kemana-mana.
 *
 * Cara pakai:
 *   - burst(x, y, warna, jumlah)  -> panggil tiap kali ada damage / kematian
 *   - update()                    -> panggil SEKALI tiap tick (di cameraTimer GamePanel)
 *   - draw(g2d)                   -> panggil SEKALI tiap paintComponent, PALING ATAS
 *                                     (sesudah render unit & projectile) biar keliatan nempel
 */
public class HitParticles {

    private static class P {
        double x, y, vx, vy;
        int life, maxLife;
        Color color;
        int size;
    }

    private static final List<P> particles = new ArrayList<>();

    /**
     * Ledakan partikel kecil di satu titik.
     * @param x,y   posisi dunia (world coordinate, sama sistem sama x/y unit)
     * @param color warna dasar partikel (mis. merah buat darah, cokelat buat serpihan kayu)
     * @param count jumlah partikel -- pakai ~5-8 buat hit biasa, ~14-18 buat kematian
     */
    public static void burst(double x, double y, Color color, int count) {
        for (int i = 0; i < count; i++) {
            P p = new P();
            p.x = x;
            p.y = y;
            double angle = Math.random() * Math.PI * 2;
            double speed = 0.8 + Math.random() * 2.0;
            p.vx = Math.cos(angle) * speed;
            p.vy = Math.sin(angle) * speed - 1.2; // dorongan ke atas dikit biar kerasa "muncrat"
            p.maxLife = 16 + (int) (Math.random() * 10);
            p.life = p.maxLife;
            p.size = 2 + (int) (Math.random() * 3);
            // Variasi kecerahan warna biar burst-nya gak keliatan flat/rata
            int variance = 30;
            int r = clamp(color.getRed() + (int) (Math.random() * variance) - variance / 2);
            int g = clamp(color.getGreen() + (int) (Math.random() * variance) - variance / 2);
            int b = clamp(color.getBlue() + (int) (Math.random() * variance) - variance / 2);
            p.color = new Color(r, g, b);
            particles.add(p);
        }
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    /** Panggil sekali tiap tick game (bukan tiap frame render). */
    public static void update() {
        Iterator<P> it = particles.iterator();
        while (it.hasNext()) {
            P p = it.next();
            p.x += p.vx;
            p.y += p.vy;
            p.vy += 0.18; // gravitasi ringan, biar partikel jatuh natural
            p.vx *= 0.94; // gesekan udara dikit, biar melambat
            p.life--;
            if (p.life <= 0) it.remove();
        }
    }

    /** Panggil sekali tiap paintComponent, di atas layer unit/projectile. */
    public static void draw(Graphics2D g2d) {
        for (P p : particles) {
            float alpha = Math.max(0f, (float) p.life / p.maxLife);
            g2d.setColor(new Color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(), (int) (alpha * 255)));
            g2d.fillOval((int) (p.x - p.size / 2.0), (int) (p.y - p.size / 2.0), p.size, p.size);
        }
    }
}