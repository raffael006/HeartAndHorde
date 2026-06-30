import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Building implements Serializable {
    private static final long serialVersionUID = 1L;
    private Rectangle bounds;

    // --- FITUR BARU: TIPE BANGUNAN DINAMIS ---
    public enum BuildingType {
        SMALL_HOUSE, MEDIUM_HOUSE, BIG_HOUSE, WALL_L, WALL_R, WALL_UD,
        FARM, STORAGE, BARRACK
    }
    public BuildingType type;

    public int maxCapacity;
    public List<Civil> occupants;

    public boolean isDemolishing = false;
    public float demolishProgress = 0f;
    public float maxDemolish = 100f; // Waktu untuk hancur

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

        if (isDemolishing) {
            int barW = 30; int barH = 5;
            int barX = bounds.x + (bounds.width - barW) / 2;
            int barY = bounds.y - 10;

            g2d.setColor(new Color(0, 0, 0, 150));
            g2d.fillRect(barX, barY, barW, barH);
            g2d.setColor(Color.RED);
            int fillW = (int) ((demolishProgress / maxDemolish) * barW);
            g2d.fillRect(barX, barY, fillW, barH);
        }
    }


    // Pusat Data: Lebar Visual Gambar PNG
    public static int getVisualWidth(BuildingType type) {
        if (type == BuildingType.WALL_L || type == BuildingType.WALL_R || type == BuildingType.WALL_UD) return 10;
        if (type == BuildingType.SMALL_HOUSE) return 36;
        if (type == BuildingType.MEDIUM_HOUSE) return 66;
        if (type == BuildingType.BIG_HOUSE) return 86;
        // --- BANGUNAN BARU ---
        if (type == BuildingType.FARM) return 108;    // Sesuaikan lebar farm.png
        if (type == BuildingType.STORAGE) return 70; // Sesuaikan lebar storage.png
        if (type == BuildingType.BARRACK) return 114; // Sesuaikan lebar barrack.png
        return 70;
    }

    // Pusat Data: Tinggi Visual Gambar PNG (Termasuk atap)
    public static int getVisualHeight(BuildingType type) {
        if (type == BuildingType.WALL_L || type == BuildingType.WALL_R || type == BuildingType.WALL_UD) return 28;
        if (type == BuildingType.SMALL_HOUSE) return 69;
        if (type == BuildingType.MEDIUM_HOUSE) return 69;
        if (type == BuildingType.BIG_HOUSE) return 80;
        if (type == BuildingType.FARM) return 61;    // Sesuaikan tinggi farm.png
        if (type == BuildingType.STORAGE) return 45; // Sesuaikan tinggi storage.png
        if (type == BuildingType.BARRACK) return 71;
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