import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Building implements Serializable {
    private static final long serialVersionUID = 1L;
    private Rectangle bounds;

    // --- FITUR BARU: TIPE BANGUNAN DINAMIS ---
    public enum BuildingType { SMALL_HOUSE, MEDIUM_HOUSE, BIG_HOUSE, WALL_L, WALL_R, WALL_UD }
    public BuildingType type;

    public int maxCapacity;
    public List<Civil> occupants;

    public Building(int x, int y, int width, int height, BuildingType type, int capacity) {
        this.bounds = new Rectangle(x, y, width, height);
        this.type = type;
        this.maxCapacity = capacity;
        this.occupants = new ArrayList<>();
    }

    public boolean isFull() { return occupants.size() >= maxCapacity; }
    public boolean enterBuilding(Civil civil) {
        if (!isFull()) { occupants.add(civil); return true; }
        return false;
    }
    public void exitBuilding(Civil civil) { occupants.remove(civil); }

    public void draw(Graphics2D g2d, BufferedImage img) {
        if (img != null) {
            // LANGSUNG AMBIL DARI PUSAT DATA!
            int drawWidth = getVisualWidth(type);
            int drawHeight = getVisualHeight(type);

            // Tambahkan "bounds." untuk mengakses x, y, width, dan height
            int drawX = bounds.x - ((drawWidth - bounds.width) / 2);
            int drawY = bounds.y - (drawHeight - bounds.height);

            g2d.drawImage(img, drawX, drawY, drawWidth, drawHeight, null);
        }
    }


    // Pusat Data: Lebar Visual Gambar PNG
    public static int getVisualWidth(BuildingType type) {
        if (type == BuildingType.WALL_L || type == BuildingType.WALL_R || type == BuildingType.WALL_UD) return 10;
        if (type == BuildingType.SMALL_HOUSE) return 36;
        if (type == BuildingType.MEDIUM_HOUSE) return 66;
        if (type == BuildingType.BIG_HOUSE) return 86;
        return 70;
    }

    // Pusat Data: Tinggi Visual Gambar PNG (Termasuk atap)
    public static int getVisualHeight(BuildingType type) {
        if (type == BuildingType.WALL_L || type == BuildingType.WALL_R || type == BuildingType.WALL_UD) return 28;
        if (type == BuildingType.SMALL_HOUSE) return 69;
        if (type == BuildingType.MEDIUM_HOUSE) return 69;
        if (type == BuildingType.BIG_HOUSE) return 80;
        return 70;
    }

    public Rectangle getBounds() { return bounds; }
    public boolean contains(Point p) { return bounds.contains(p); }
    public boolean intersects(Rectangle r) { return bounds.intersects(r); }

    public Rectangle getSolidHitbox() {
        int solidHeight = (int) (bounds.height * 0.40);
        int solidY = bounds.y + bounds.height - solidHeight;
        return new Rectangle(bounds.x, solidY, bounds.width, solidHeight);
    }


}