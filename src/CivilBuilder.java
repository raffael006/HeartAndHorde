import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.List;

public class CivilBuilder implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum BuilderState { IDLE_HOME, MOVING_TO_SITE, BUILDING, RETURNING_HOME }

    public double x, y;
    public double homeX, homeY;
    public Building homeBuilding;          // Rumah Builder asalnya
    public Building assignedBuilding = null;

    public int size = 20;
    public double speed = 0.8;

    public BuilderState state = BuilderState.IDLE_HOME;

    public transient List<Point> path = null;
    public int pathIndex = 0;
    private transient List<Building> buildingsRef; // Buat referensi pathfinding pulang

    public CivilBuilder(double homeX, double homeY, Building homeBuilding) {
        this.homeX = homeX;
        this.homeY = homeY;
        this.homeBuilding = homeBuilding;
        this.x = homeX - size / 2.0;
        this.y = homeY - size / 2.0;
    }

    public void setPath(List<Point> newPath) {
        this.path = newPath;
        this.pathIndex = 0;
    }

    // Dipanggil dari GamePanel waktu ada bangunan butuh tukang
    public void assignToBuild(Building target, List<Building> allBuildings) {
        this.assignedBuilding = target;
        target.assignedBuilder = this;
        this.buildingsRef = allBuildings;

        double targetX = target.getBounds().getCenterX();
        double targetY = target.getBounds().getCenterY();
        setPath(PathFinder.findPath(x, y, targetX, targetY, allBuildings));
        state = BuilderState.MOVING_TO_SITE;
    }

    public void update() {
        switch (state) {
            case MOVING_TO_SITE:
                moveAlongPath(() -> state = BuilderState.BUILDING);
                break;

            case BUILDING:
                if (assignedBuilding == null || assignedBuilding.isBuilt || assignedBuilding.isDemolishing) {
                    startReturningHome();
                    return;
                }
                assignedBuilding.buildProgress += 1.0f;
                if (assignedBuilding.buildProgress >= assignedBuilding.maxBuild) {
                    assignedBuilding.isBuilt = true;
                    assignedBuilding.assignedBuilder = null;
                    assignedBuilding = null;
                    startReturningHome();
                }
                break;

            case RETURNING_HOME:
                moveAlongPath(() -> state = BuilderState.IDLE_HOME);
                break;

            case IDLE_HOME:
            default:
                // Diam di dalam rumah, nunggu ditugasin GamePanel
                break;
        }
    }

    private void startReturningHome() {
        setPath(PathFinder.findPath(x, y, homeX, homeY, buildingsRef));
        state = BuilderState.RETURNING_HOME;
    }

    private void moveAlongPath(Runnable onArrive) {
        if (path != null && pathIndex < path.size()) {
            Point node = path.get(pathIndex);
            double nx = node.x - size / 2.0;
            double ny = node.y - size / 2.0;
            double dx = nx - x, dy = ny - y;
            double dist = Math.sqrt(dx * dx + dy * dy);

            if (dist > 6) {
                x += (dx / dist) * speed;
                y += (dy / dist) * speed;
            } else {
                pathIndex++;
            }
        } else {
            onArrive.run();
        }
    }

    public void draw(Graphics2D g2d, BufferedImage img) {
        if (img != null) {
            g2d.drawImage(img, (int) x, (int) y, size, size, null);
        } else {
            g2d.setColor(new Color(180, 140, 100));
            g2d.fillRect((int) x, (int) y, size, size);
        }
    }
}