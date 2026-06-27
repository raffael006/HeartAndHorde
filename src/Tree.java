import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.Serializable;

public class Tree implements Serializable {
    private static final long serialVersionUID = 1L;

    // Posisi dan ukuran pohon
    public int x, y;
    public int width, height;

    // Constructor: Dijalankan saat pohon baru "dilahirkan" ke dunia
    public Tree(int startX, int startY, int w, int h) {
        this.x = startX;
        this.y = startY;
        this.width = w;
        this.height = h;

    }

    // --- INI DIA INTI FISIKANYA (THE ANCHOR) ---
    // Cuma area batang/akar bawah yang padat dan bisa ditabrak
    public Rectangle getSolidHitbox() {
        // Kita set hanya 30% atau 40% bagian bawah yang solid (area batang pohon)
        int solidHeight = (int) (height * 0.30);
        int solidY = y + height - solidHeight;
        return new Rectangle(x, solidY, width, solidHeight);
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
    }
}