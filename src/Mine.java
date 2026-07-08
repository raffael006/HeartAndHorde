import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.Serializable;

public class Mine implements Serializable {
    private static final long serialVersionUID = 1L;

    public int x, y, width, height;

    // Status Tambang
    public boolean isBuilt = false;      // false = abandoned, true = sudah jadi mine
    public boolean isBuilding = false;   // Sedang dalam proses dipalu/dibangun
    public float buildProgress = 0f;
    public float maxBuild = 100f;        // Lama waktu loading membangun

    // --- FITUR BARU: Mine sekarang butuh CivilBuilder yang mengerjakannya ---
    public CivilBuilder assignedBuilder = null;

    public Mine(int x, int y) {
        this.x = x;
        this.y = y;
        this.width = 58;  // Ukuran sesuai asetmu
        this.height = 37; // Ukuran sesuai asetmu
    }

    // Hitbox solid untuk tabrakan fisik (sepertiga bawah)
    public Rectangle getSolidHitbox() {
        int solidHeight = (int) (height * 0.40);
        int solidY = y + height - solidHeight;
        return new Rectangle(x, solidY, width, solidHeight);
    }

    // Area klik untuk deteksi kursor mouse
    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }

    public void draw(Graphics2D g2d, BufferedImage abandonedImg, BufferedImage builtImg) {
        // Tentukan gambar mana yang dipakai
        BufferedImage currentImg = isBuilt ? builtImg : abandonedImg;

        if (currentImg != null) {
            g2d.drawImage(currentImg, x, y, width, height, null);
        }

        // --- RENDER LOADING BAR SAAT SEDANG DIBANGUN ---
        if (isBuilding && !isBuilt) {
            int barW = 30;
            int barH = 5;
            int barX = x + (width - barW) / 2;
            int barY = y - 10;

            // Background Bar (Hitam)
            g2d.setColor(new Color(0, 0, 0, 150));
            g2d.fillRect(barX, barY, barW, barH);

            // Isi Bar (Hijau/Kuning)
            g2d.setColor(new Color(50, 205, 50));
            int fillW = (int) ((buildProgress / maxBuild) * barW);
            g2d.fillRect(barX, barY, fillW, barH);
        }
    }
}