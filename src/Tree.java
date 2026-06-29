import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.Serializable;

public class Tree implements Serializable {
    private static final long serialVersionUID = 1L;

    // Posisi dan ukuran pohon
    public int x, y;
    public int width, height;

    // --- FITUR BARU: TEBANG POHON ---
    public boolean isHarvesting = false;
    public float harvestProgress = 0f;
    public float maxHarvest = 100f; // Semakin besar, semakin lama loadingnya

    // Constructor: Dijalankan saat pohon baru "dilahirkan" ke dunia
    public Tree(int startX, int startY, int w, int h) {
        this.x = startX;
        this.y = startY;
        this.width = w;
        this.height = h;
    }

    // --- INI DIA INTI FISIKANYA (collision) ---
    // Cuma area batang/akar bawah yang padat dan bisa ditabrak
    public Rectangle getSolidHitbox() {
        //  set hanya 30% atau 40% bagian bawah yang solid (area batang pohon)
        int solidHeight = (int) (height * 0.30);
        int solidY = y + height - solidHeight;
        return new Rectangle(x, solidY, width, solidHeight);
    }

    // --- FUNGSI BARU: Untuk mendeteksi klik kursor Mouse (Kapak) ---
    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }

    public void draw(Graphics2D g2d, BufferedImage img) {
        if (img != null) {
            // Jika gambar tree.png berhasil dimuat, gambar di layar
            g2d.drawImage(img, x, y, width, height, null);
        } else {
            // Warna cadangan (Kotak hijau) kalau gambar gagal dimuat
            g2d.setColor(new Color(34, 139, 34)); // Warna hijau hutan
            g2d.fillRect(x, y, width, height);
        }

        // --- RENDER LOADING BAR HIJAU JIKA SEDANG DITEBANG ---
        if (isHarvesting) {
            int barW = 30; // Lebar bar
            int barH = 5;  // Tinggi bar
            int barX = x + (width - barW) / 2; // Posisikan di tengah pohon
            int barY = y - 10; // Melayang sedikit di atas pohon

            // Background Bar (Hitam transparan)
            g2d.setColor(new Color(0, 0, 0, 150));
            g2d.fillRect(barX, barY, barW, barH);

            // Isi Bar (Hijau)
            g2d.setColor(new Color(0, 255, 0));
            int fillW = (int) ((harvestProgress / maxHarvest) * barW);
            g2d.fillRect(barX, barY, fillW, barH);
        }
    }
}