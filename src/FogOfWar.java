import java.awt.*;


public class FogOfWar {
    private static final int CELL_SIZE = 500;       // Ukuran 1 petak (dalam world units) — atur di sini
    private static final float FADE_SPEED = 0.02f;  // Kecepatan fade per tick (makin besar = makin cepat kebuka)
    private static final int BASE_DARK_ALPHA = 242;  // Tingkat gelap petak yang belum kebuka (0-255)

    private final int cols, rows;
    private final boolean[][] triggered; // Petak ini sudah pernah didatangi guard?
    private final float[][] alpha;       // 0 = masih gelap total, 1 = sudah kebuka penuh (buat animasi fade)

    public FogOfWar(int mapWidth, int mapHeight) {
        cols = (mapWidth / CELL_SIZE) + 1;
        rows = (mapHeight / CELL_SIZE) + 1;
        triggered = new boolean[cols][rows];
        alpha = new float[cols][rows];
    }

    // Dipanggil pas guard gerak: tandai petak-petak di sekitar posisinya sebagai "kebuka"
    public void reveal(double worldX, double worldY, float radius) {
        int cellX = (int) (worldX / CELL_SIZE);
        int cellY = (int) (worldY / CELL_SIZE);
        int cellRadius = (int) Math.ceil(radius / CELL_SIZE);

        for (int dx = -cellRadius; dx <= cellRadius; dx++) {
            for (int dy = -cellRadius; dy <= cellRadius; dy++) {
                int cx = cellX + dx;
                int cy = cellY + dy;
                if (cx < 0 || cy < 0 || cx >= cols || cy >= rows) continue;

                double cellCenterX = cx * CELL_SIZE + CELL_SIZE / 2.0;
                double cellCenterY = cy * CELL_SIZE + CELL_SIZE / 2.0;
                double dist = Math.hypot(cellCenterX - worldX, cellCenterY - worldY);

                if (dist <= radius) {
                    triggered[cx][cy] = true; // Permanen, gak akan di-set false lagi
                }
            }
        }
    }

    public void revealIfMoved(Guard g, double worldX, double worldY, float radius) {
        reveal(worldX, worldY, radius); // Per-petak sudah murah, aman dipanggil tiap frame tanpa cek jarak dulu
    }

    // WAJIB dipanggil SEKALI tiap tick, buat jalanin animasi fade petak yang baru ke-trigger
    public void update() {
        for (int x = 0; x < cols; x++) {
            for (int y = 0; y < rows; y++) {
                if (triggered[x][y] && alpha[x][y] < 1f) {
                    alpha[x][y] += FADE_SPEED;
                    if (alpha[x][y] > 1f) alpha[x][y] = 1f;
                }
            }
        }
    }

    public void draw(Graphics2D g2d, Rectangle visibleWorldArea) {
        int startX = Math.max(0, visibleWorldArea.x / CELL_SIZE);
        int startY = Math.max(0, visibleWorldArea.y / CELL_SIZE);
        int endX = Math.min(cols - 1, (visibleWorldArea.x + visibleWorldArea.width) / CELL_SIZE);
        int endY = Math.min(rows - 1, (visibleWorldArea.y + visibleWorldArea.height) / CELL_SIZE);

        for (int x = startX; x <= endX; x++) {
            for (int y = startY; y <= endY; y++) {
                float a = alpha[x][y];
                int darkAlpha = (int) (BASE_DARK_ALPHA * (1f - a));
                if (darkAlpha <= 0) continue; // Sudah transparan penuh, skip biar hemat

                g2d.setColor(new Color(0, 0, 0, darkAlpha));
                g2d.fillRect(x * CELL_SIZE, y * CELL_SIZE, CELL_SIZE, CELL_SIZE);
            }
        }
    }
}