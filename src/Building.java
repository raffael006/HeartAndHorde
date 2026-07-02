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
        FARM, STORAGE, BARRACK, HEART
    }
    public BuildingType type;

    public int maxCapacity;
    public List<Civil> occupants;
    public boolean isBuilt = false;      // Saat baru ditaruh, default-nya belum jadi
    public float buildProgress = 0f;
    public float maxBuild = 100f;

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

    public void draw(Graphics2D g2d, BufferedImage constructionImg, BufferedImage finishedImg) {
        if (!isBuilt) {
            // --- RENDER STEGER PEMBANGUNAN HANYA DI AREA COLLISION (PONDASI) ---
            Rectangle hitbox = getSolidHitbox();
            if (constructionImg != null) {
                g2d.drawImage(constructionImg, hitbox.x, hitbox.y, hitbox.width, hitbox.height, null);
            }

            // --- RENDER LOADING BAR HIJAU ---
            int barW = hitbox.width;
            int barH = 5;
            int barX = hitbox.x;
            int barY = hitbox.y - 10; // Melayang sedikit di atas pondasi

            g2d.setColor(new Color(0, 0, 0, 150));
            g2d.fillRect(barX, barY, barW, barH);
            g2d.setColor(new Color(50, 205, 50));
            int fillW = (int) ((buildProgress / maxBuild) * barW);
            g2d.fillRect(barX, barY, fillW, barH);

        } else {
            // --- RENDER BANGUNAN ASLI (Menjulang tinggi ke atas 2.5D) ---
            if (finishedImg != null) {
                int drawWidth = getVisualWidth(type);
                int drawHeight = getVisualHeight(type);

                int drawX = bounds.x - ((drawWidth - bounds.width) / 2);
                int drawY = bounds.y - (drawHeight - bounds.height);
                if (type == BuildingType.HEART) {
                    System.out.println("drawX=" + drawX +
                            " drawY=" + drawY +
                            " width=" + drawWidth +
                            " height=" + drawHeight);
                }

                g2d.drawImage(finishedImg, drawX, drawY, drawWidth, drawHeight, null);
            }
        }

        // --- RENDER LOADING BAR MERAH SAAT DIHANCURKAN ---
        if (isDemolishing) {
            Rectangle hitbox = getSolidHitbox(); // Posisi loading mengikuti pondasi bawah
            int barW = 30; int barH = 5;
            int barX = hitbox.x + (hitbox.width - barW) / 2;
            int barY = hitbox.y - 10;

            g2d.setColor(new Color(0, 0, 0, 150));
            g2d.fillRect(barX, barY, barW, barH);
            g2d.setColor(Color.RED);
            int fillW = (int) ((demolishProgress / maxDemolish) * barW);
            g2d.fillRect(barX, barY, fillW, barH);
        }
    }


    // Pusat Data: Lebar Visual Gambar PNG
    public static int getVisualWidth(BuildingType type) {
        if (type == BuildingType.HEART) return 138 +20;
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
        if (type == BuildingType.HEART) return 96 +20;
        if (type == BuildingType.WALL_L || type == BuildingType.WALL_R || type == BuildingType.WALL_UD) return 28;
        if (type == BuildingType.SMALL_HOUSE) return 69;
        if (type == BuildingType.MEDIUM_HOUSE) return 69;
        if (type == BuildingType.BIG_HOUSE) return 80;
        if (type == BuildingType.FARM) return 61; // Sesuaikan tinggi farm.png
        if (type == BuildingType.STORAGE) return 45; // Sesuaikan tinggi storage.png
        if (type == BuildingType.BARRACK) return 71;
        return 70;
    }

    public Rectangle getBounds() { return bounds; }
    public boolean contains(Point p) { return bounds.contains(p); }
    public boolean intersects(Rectangle r) { return bounds.intersects(r); }

    public Rectangle getSolidHitbox() {
        return computeSolidHitbox(bounds.x, bounds.y, bounds.width, bounds.height, type);
    }

    // --- PUSAT LOGIKA HITBOX (dipakai juga oleh GamePanel saat preview build/move) ---
    // Supaya hitbox SELALU sama persis dengan bentuk gambar bangunannya,
    // dan tidak dobel-hitung/berbeda antara Building.java dan GamePanel.java.
    public static Rectangle computeSolidHitbox(int x, int y, int width, int height, BuildingType type) {
        if (type == BuildingType.FARM) {
            // Full gambar jadi solid, disamakan persis posisinya dengan visual di draw().
            int visualWidth = getVisualWidth(type);
            int visualHeight = getVisualHeight(type);
            int farmX = x - ((visualWidth - width) / 2);
            int farmY = y - (visualHeight - height);
            return new Rectangle(farmX, farmY, visualWidth, visualHeight);
        }

        int solidWidth = getSolidWidth(type, width);
        int solidHeight = getSolidHeight(type, height);

        // Ditengahkan (center) mengikuti titik tengah bounds,
        // sama seperti cara gambar visualnya digambar terpusat di draw()
        int solidX = x + (width - solidWidth) / 2;
        int solidY = y + height - solidHeight; // Tetap nempel di BAWAH (depan), atas/belakang dibiarkan kosong

        return new Rectangle(solidX, solidY, solidWidth, solidHeight);
    }

    // Lebar dinding depan yang BENERAN solid (menyesuaikan gambar aslinya).
    // Kalau tidak didaftar di sini, dianggap lebar dindingnya = lebar bounds/pondasi (sudah pas, tidak diubah).
    private static int getSolidWidth(BuildingType type, int fallbackWidth) {
        if (type == BuildingType.SMALL_HOUSE) {
            // smhouse.png dindingnya cuma ~36px, jauh lebih sempit dari pondasi (70px).
            // Kalau dipaksa selebar pondasi, warga/pasukan jadi kehalang rumput kosong di kiri-kanan rumah.
            return 34;
        }
        if (type == BuildingType.HEART) {
            // Heart.png lebar totalnya ~158px, termasuk sayap dinding kiri-kanan yang menyambung.
            // Sayap itu ikut solid (bagian depan bangunan), makanya lebar hitbox harus dilebarkan,
            // bukan cuma selebar pondasi tengah (80px) seperti sebelumnya.
            // (Disisakan ~4px di tiap sisi karena tepi gambarnya sedikit transparan/miring, bukan dinding solid.)
            return 150;
        }
        // Tipe lain (MEDIUM_HOUSE, BIG_HOUSE, STORAGE, BARRACK, WALL) sudah proporsional
        // antara lebar gambar & lebar pondasinya -> tidak diubah.
        // (FARM ditangani terpisah di computeSolidHitbox() - full gambar solid.)
        return fallbackWidth;
    }

    // Tinggi bagian depan (dinding+pintu) yang solid. Bagian atas (atap = "belakang" bangunan)
    // sengaja TIDAK solid supaya warga & militer bisa jalan seolah-olah di belakang bangunan.
    private static int getSolidHeight(BuildingType type, int fallbackHeight) {
        // Sudah pas untuk semua tipe saat ini, dipertahankan seperti semula.
        return (int) (fallbackHeight * 0.40);
    }


}