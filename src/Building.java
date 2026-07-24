import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Building implements Serializable {
    private static final long serialVersionUID = 1L;
    private Rectangle bounds;
    public CivilBuilder assignedBuilder = null;

    // --- FITUR BARU: TIPE BANGUNAN DINAMIS ---
    public enum BuildingType {
        SMALL_HOUSE, MEDIUM_HOUSE, BIG_HOUSE, WALL_L, WALL_R, WALL_UD,
        FARM, STORAGE, BARRACK, HEART, BUILDER,
        ARCHER_TOWER   // <-- FITUR BARU: menara panah (cost wood+stone+civil, hancur -> 4 Guard Bow)
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

    // --- FITUR BARU: DARAH BANGUNAN (biar bisa dihancurkan Horde) ---
    public double maxHp;
    public double currentHp;

    // --- FITUR BARU: FLASH PUTIH pas Building kena hit ---
    public long lastHitTime = -999999;
    private static final long HIT_FLASH_DURATION = 110; // ms

    // --- FITUR BARU: cooldown serangan buat building yang bisa nembak sendiri (Archer Tower) ---
    public long lastAttackTime = 0;

    public Building(int x, int y, int width, int height, BuildingType type, int capacity) {
        this.bounds = new Rectangle(x, y, width, height);
        this.type = type;
        this.maxCapacity = capacity;
        this.occupants = new ArrayList<>();
        this.maxHp = getMaxHpForType(type);
        this.currentHp = this.maxHp;
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

                // --- FITUR BARU: FLASH PUTIH pas abis kena hit (fade out cepat) ---
                long sinceHit = System.currentTimeMillis() - lastHitTime;
                if (sinceHit >= 0 && sinceHit < HIT_FLASH_DURATION) {
                    float t = 1f - (float) sinceHit / HIT_FLASH_DURATION;
                    Composite oldComposite = g2d.getComposite();
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, Math.min(1f, Math.max(0f, t))));
                    g2d.setColor(Color.WHITE);
                    g2d.fillRect(drawX, drawY, drawWidth, drawHeight);
                    g2d.setComposite(oldComposite);
                }
            }

            // --- FITUR BARU: BAR DARAH BANGUNAN (cuma tampil kalau udah jadi & belum full darah) ---
            if (currentHp < maxHp) {
                Rectangle hitbox = getSolidHitbox();
                int barW = hitbox.width;
                int barH = 5;
                int barX = hitbox.x;
                int barY = hitbox.y - 10;

                g2d.setColor(new Color(60, 0, 0));
                g2d.fillRect(barX, barY, barW, barH);

                double hpPercentage = Math.max(0, currentHp / maxHp);
                int fillHpW = (int) (barW * hpPercentage);
                g2d.setColor(new Color(90, 220, 90));
                g2d.fillRect(barX, barY, fillHpW, barH);

                g2d.setColor(Color.BLACK);
                g2d.drawRect(barX, barY, barW, barH);
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
        if (type == BuildingType.BUILDER) return 35;   // di getVisualWidth
        if (type == BuildingType.ARCHER_TOWER) return 48; // Diperkecil dari 70 -> lebih proporsional ke building lain

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
        if (type == BuildingType.BUILDER) return 35;   // di getVisualHeight
        if (type == BuildingType.ARCHER_TOWER) return 100; // Diperkecil dari 145 -> rasio tetep ~320:670
        return 70;
    }

    // --- FITUR BARU: BIAYA KAYU SESUAI UKURAN BANGUNAN ---
    public static int getWoodCost(BuildingType type) {
        if (type == BuildingType.SMALL_HOUSE) return 15;  // Cost untuk smhouse.png
        if (type == BuildingType.MEDIUM_HOUSE) return 30; // Cost untuk mhouse.png
        if (type == BuildingType.BIG_HOUSE) return 60;    // Cost untuk bighouse.png

        // Cost buat bangunan lain sekalian diset
        if (type == BuildingType.WALL_L || type == BuildingType.WALL_R || type == BuildingType.WALL_UD) return 2;
        if (type == BuildingType.FARM) return 10;
        if (type == BuildingType.STORAGE) return 5;
        if (type == BuildingType.BARRACK) return 20;
        if (type == BuildingType.BUILDER) return 25;
        if (type == BuildingType.ARCHER_TOWER) return 12;

        return 0; // Default (contoh: Heart) gratis
    }

    // --- FITUR BARU: BIAYA STONE (dulunya cuma ada wood cost). Default 0 buat tipe yang gak butuh stone. ---
    public static int getStoneCost(BuildingType type) {
        if (type == BuildingType.ARCHER_TOWER) return 6;
        if (type == BuildingType.MEDIUM_HOUSE) return 5;  // Butuh sedikit batu buat pondasi lebih kokoh
        if (type == BuildingType.BIG_HOUSE) return 10;
        return 0;
    }

    // --- FITUR BARU: BIAYA CIVIL (jumlah civil yang harus "dikorbankan"/dipakai buat bangun).
    // CATATAN: pengurangan civil-nya sendiri (activeCivils di GameWindow) HARUS diwire di GamePanel
    // pas tombol build ditekan -- di sini cuma data costnya. ---
    public static int getCivilCost(BuildingType type) {
        if (type == BuildingType.ARCHER_TOWER) return 4;
        return 0;
    }

    // --- FITUR BARU: DATA SERANGAN ARCHER TOWER --------------------------------------
    // Tower ini NEMBAK SENDIRI (4 archer yang berdiri di atasnya), independen dari Guard.
    // Jangkauannya sengaja dibikin lebih luas dari Guard Archer biasa (250) supaya berasa
    // "worth it" ditaruh di titik strategis buat cover area luas / jalur masuk horde.
    public static double getTowerRange(BuildingType type) {
        if (type == BuildingType.ARCHER_TOWER) return 350.0; // lebih luas dari Guard Archer (250)
        return 0.0; // 0 = building ini gak nembak sendiri
    }

    public static double getTowerDamage(BuildingType type) {
        if (type == BuildingType.ARCHER_TOWER) return 20.0; // disamain sama base damage Guard Archer / panah
        return 0.0;
    }

    public static long getTowerCooldown(BuildingType type) {
        if (type == BuildingType.ARCHER_TOWER) return 1000; // 1 archer diwakili slot cooldown yg sama
        return 0;
    }

    // Berapa target berbeda yang bisa kena panah sekaligus tiap kali cooldown abis
    // (4 archer di atas tower = bisa nembak 4 Horde beda sekaligus kalau ada 4 dalam jangkauan)
    public static int getTowerArrowCount(BuildingType type) {
        if (type == BuildingType.ARCHER_TOWER) return 4;
        return 0;
    }

    // --- FITUR BARU: DARAH MAKSIMAL SESUAI TIPE BANGUNAN ---
    public static int getMaxHpForType(BuildingType type) {
        if (type == BuildingType.HEART) return 500;       // Jantung basecamp, paling kuat
        if (type == BuildingType.SMALL_HOUSE) return 60;
        if (type == BuildingType.MEDIUM_HOUSE) return 100;
        if (type == BuildingType.BIG_HOUSE) return 150;
        if (type == BuildingType.WALL_L || type == BuildingType.WALL_R || type == BuildingType.WALL_UD) return 40;
        if (type == BuildingType.FARM) return 70;
        if (type == BuildingType.STORAGE) return 70;
        if (type == BuildingType.BARRACK) return 120;
        if (type == BuildingType.BUILDER) return 70;
        if (type == BuildingType.ARCHER_TOWER) return 140;
        return 100;
    }

    public Rectangle getBounds() { return bounds; }

    // --- FITUR BARU: titik asal panah buat building yang nembak sendiri (Archer Tower).
    // Sebelumnya GamePanel pakai bounds.getCenterX/Y() -> itu titik tengah PONDASI (bawah),
    // padahal archer-nya digambar di platform PALING ATAS sprite (posisi sama kayak draw()
    // ngitung drawX/drawY). Makanya panahnya keliatan nongol dari bawah, bukan dari atas tower.
    public Point2D.Double getRangedAttackOrigin() {
        if (type == BuildingType.ARCHER_TOWER) {
            int visualWidth = getVisualWidth(type);
            int visualHeight = getVisualHeight(type);
            int drawX = bounds.x - ((visualWidth - bounds.width) / 2);
            int drawY = bounds.y - (visualHeight - bounds.height);

            double originX = drawX + visualWidth / 2.0;
            double originY = drawY + visualHeight * 0.18; // ~platform atas tempat 4 archer berdiri
            return new Point2D.Double(originX, originY);
        }
        // Default (building lain yang mungkin nembak sendiri di masa depan): tengah pondasi
        return new Point2D.Double(bounds.getCenterX(), bounds.getCenterY());
    }

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

        if (type == BuildingType.BUILDER) {
            return 80; // <-- atur lebar collision Builder di sini, sesuaikan ke rasa yang pas
        }
        // Tipe lain (MEDIUM_HOUSE, BIG_HOUSE, STORAGE, BARRACK, WALL) sudah proporsional
        // antara lebar gambar & lebar pondasinya -> tidak diubah.
        // (FARM ditangani terpisah di computeSolidHitbox() - full gambar solid.)
        return fallbackWidth;
    }

    // Tinggi bagian depan (dinding+pintu) yang solid. Bagian atas (atap = "belakang" bangunan)
    // sengaja TIDAK solid supaya warga & militer bisa jalan seolah-olah di belakang bangunan.
    private static int getSolidHeight(BuildingType type, int fallbackHeight) {
        if (type == BuildingType.BUILDER) {
            return 40; // <-- atur tinggi collision Builder, kalau mau beda dari default 40%
        }
        return (int) (fallbackHeight * 0.40);
    }
}