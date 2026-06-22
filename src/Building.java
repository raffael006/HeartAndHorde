import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.Serializable;

public class Building implements Serializable {
    // serialVersionUID penting agar fitur Save/Load game Anda tidak error
    private static final long serialVersionUID = 1L;

    private Rectangle bounds;

    // --- Constructor (Fungsi pembuat objek) ---
    public Building(int x, int y, int width, int height) {
        this.bounds = new Rectangle(x, y, width, height);
    }

    // --- Behavior: Bangunan bisa menggambar dirinya sendiri ---
    public void draw(Graphics2D g2d, BufferedImage image) {
        if (image != null) {
            g2d.drawImage(image, bounds.x, bounds.y, bounds.width, bounds.height, null);
        } else {
            g2d.setColor(new Color(140, 70, 40));
            g2d.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
        }
    }

    // --- Fungsi Bantuan untuk Logika Mouse (Klik & Tabrakan) ---
    public Rectangle getBounds() { return bounds; }
    public boolean contains(Point p) { return bounds.contains(p); }
    public boolean intersects(Rectangle r) { return bounds.intersects(r); }
}