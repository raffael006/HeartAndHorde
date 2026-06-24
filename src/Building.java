import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Building implements Serializable {
    private static final long serialVersionUID = 1L;
    private Rectangle bounds;

    // --- FITUR BARU: TIPE BANGUNAN DINAMIS ---
    public enum BuildingType { SMALL_HOUSE, MEDIUM_HOUSE, BIG_HOUSE }
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

    public void draw(Graphics2D g2d, BufferedImage image) {
        if (image != null) {
            g2d.drawImage(image, bounds.x, bounds.y, bounds.width, bounds.height, null);
        } else {
            g2d.setColor(new Color(140, 70, 40));
            g2d.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
        }
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