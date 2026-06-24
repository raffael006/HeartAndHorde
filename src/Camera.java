import java.awt.Graphics2D;
import java.awt.Point;

public class Camera {
    private double x = 0;
    private double y = 0;
    private double zoom = 2.0;
    private final int panSpeed = 20;

    // --- 1. Logika Geser (WASD) ---
    public boolean move(boolean w, boolean s, boolean a, boolean d) {
        boolean moved = false;
        if (w) { y += panSpeed; moved = true; }
        if (s) { y -= panSpeed; moved = true; }
        if (a) { x += panSpeed; moved = true; }
        if (d) { x -= panSpeed; moved = true; }
        return moved;
    }

    // --- 2. Logika Zoom ---
    public void zoomInOut(int scrollDirection, int screenWidth, int screenHeight) {
        double oldZoom = zoom;

        if (scrollDirection < 0) {
            zoom *= 1.1; // Zoom In
        } else {
            zoom /= 1.1; // Zoom Out
        }

        double minZoomLimit = 1.2;
        double maxZoomLimit = 17.0;
        zoom = Math.max(minZoomLimit, Math.min(zoom, maxZoomLimit));

        double screenCenterX = screenWidth / 2.0;
        double screenCenterY = screenHeight / 2.0;

        double worldCenterX = (screenCenterX - x) / oldZoom;
        double worldCenterY = (screenCenterY - y) / oldZoom;

        x = screenCenterX - (worldCenterX * zoom);
        y = screenCenterY - (worldCenterY * zoom);
    }

    // --- 3. Logika Pembatas Peta (Clamp) ---
    public void clamp(int screenWidth, int screenHeight, int mapWidth, int mapHeight) {
        double minCamX = screenWidth - (mapWidth * zoom);
        double minCamY = screenHeight - (mapHeight * zoom);

        x = Math.max(minCamX, Math.min(x, 0));
        y = Math.max(minCamY, Math.min(y, 0));
    }

    // --- 4. Penerjemah Klik Mouse ---
    public Point toWorld(int screenX, int screenY) {
        int worldX = (int) ((screenX - x) / zoom);
        int worldY = (int) ((screenY - y) / zoom);
        return new Point(worldX, worldY);
    }

    // --- 5. Penerapan Efek Visual ke Layar ---
    public void applyTransform(Graphics2D g2d) {
        g2d.translate(x, y);
        g2d.scale(zoom, zoom);
    }

    // --- 6. Akses Posisi untuk Minimap ---
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZoom() { return zoom; }
}