import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.List;
import java.util.Random;

public class Civil implements Serializable {
    private static final long serialVersionUID = 1L;

    // ADT Status Sipil
    // FLEEING = lagi kabur ketakutan karena ada Horde deket
    public enum CivilState { IDLE, WANDERING, FLEEING }

    // Atribut Posisi & Geometri
    public double x, y;
    public double homeX, homeY; // Titik pusat rumah/bangunan mereka
    public double targetX, targetY;

    // Ukuran 20 (Sengaja dibuat sedikit lebih kecil dari Guard yang ukurannya 24)
    public int size = 20;
    public double speed = 0.6; // Kecepatan jalan santai, lebih lambat dari Guard

    // Atribut Behavior (Tingkah Laku)
    public CivilState state;
    private long lastStateChange = 0;
    private int stateDuration = 2000;
    private Random random = new Random();
    private int wanderRadius = 120; // Jarak maksimal mereka boleh main jauh dari rumah

    // --- FITUR BARU: DARAH CIVIL ---
    public double maxHp = 30;
    public double currentHp = 30;

    // --- FITUR BARU: RUMAH & MODE KABUR ---
    public Building homeBuilding; // Referensi ke bangunan asal Civil ini (bisa null kalau spawn cheat)
    public boolean hidden = false; // True kalau lagi ngumpet aman di dalam rumah (gak digambar, gak bisa diserang)
    private double dangerRadius = 160; // Radius "ngeh" civil kalau ada Horde masuk area ini -> panik & kabur
    private double safeDistance = 60; // Kalau lari bebas (rumah hancur), sejauh apa larinya menjauh dari ancaman

    // --- FITUR BARU: NAVIGASI GRAPH (PathFinder) BIAR GAK NABRAK RUMAH ---
    private List<Point> currentPath = null;
    private int pathIndex = 0;
    private long lastPathCalcTime = 0;
    private long pathCalcCooldown = 800; // Jangan hitung ulang rute tiap frame, berat di CPU

    public Civil(double spawnX, double spawnY) {
        this(spawnX, spawnY, null);
    }

    // --- FITUR BARU: Constructor dengan referensi rumah asal (dipakai saat rumah selesai dibangun) ---
    public Civil(double spawnX, double spawnY, Building home) {
        // Geser ke tengah kursor/rumah
        this.x = spawnX - (size / 2.0);
        this.y = spawnY - (size / 2.0);

        // Catat titik asal sebagai "Rumah"
        this.homeX = this.x;
        this.homeY = this.y;

        this.targetX = this.x;
        this.targetY = this.y;
        this.state = CivilState.IDLE;

        this.homeBuilding = home;
    }

    // Overload lama biar kalau ada pemanggilan tanpa parameter (misal kode lama) tetap jalan tanpa graph & tanpa deteksi ancaman
    public void update() {
        update(java.util.Collections.emptyList(), java.util.Collections.emptyList());
    }

    // update() versi baru: butuh daftar Horde (buat deteksi ancaman) & daftar Building (buat graph pathfinding)
    public void update(List<Horde> allHordes, List<Building> buildings) {

        long currentTime = System.currentTimeMillis();

        // ==========================================================
        // 1. DETEKSI ANCAMAN: Cari Horde terdekat dari Civil ini
        // ==========================================================
        Horde nearestThreat = null;
        double nearestThreatDist = Double.MAX_VALUE;
        for (Horde h : allHordes) {
            double dx = h.x - x;
            double dy = h.y - y;
            double dist = Math.sqrt(dx * dx + dy * dy);
            if (dist < nearestThreatDist) {
                nearestThreatDist = dist;
                nearestThreat = h;
            }
        }
        boolean isThreatened = (nearestThreat != null && nearestThreatDist <= dangerRadius);

        // Cek apakah rumah asalnya masih berdiri (belum dihancurkan/dibongkar)
        boolean homeAlive = (homeBuilding != null && buildings.contains(homeBuilding));

        // ==========================================================
        // 2. TRANSISI KE MODE KABUR (Kalau ada Horde masuk radius bahaya)
        // ==========================================================
        if (isThreatened && !hidden && state != CivilState.FLEEING) {
            state = CivilState.FLEEING;
            currentPath = null; // Paksa hitung ulang rute kabur dari titik sekarang
        }

        // Kalau lagi ngumpet tapi tiba-tiba rumahnya hancur (dibongkar/hilang) -> paksa keluar lari bebas
        if (hidden && !homeAlive) {
            hidden = false;
            state = CivilState.FLEEING;
            currentPath = null;
        }

        // ==========================================================
        // 3. EKSEKUSI MODE KABUR
        // ==========================================================
        if (state == CivilState.FLEEING) {
            hidden = false; // Lagi lari, otomatis gak lagi ngumpet

            double fleeTargetX, fleeTargetY;

            if (homeAlive) {
                // Rumah masih ada -> kabur pulang ke rumah masing-masing
                Rectangle homeBounds = homeBuilding.getSolidHitbox();
                fleeTargetX = homeBounds.x + homeBounds.width / 2.0;
                fleeTargetY = homeBounds.y + homeBounds.height / 2.0;
            } else {
                // Gak punya rumah / rumah udah hancur -> lari bebas menjauhi ancaman
                if (nearestThreat != null) {
                    double awayAngle = Math.atan2(y - nearestThreat.y, x - nearestThreat.x);
                    fleeTargetX = x + Math.cos(awayAngle) * 300;
                    fleeTargetY = y + Math.sin(awayAngle) * 300;
                } else {
                    fleeTargetX = x;
                    fleeTargetY = y;
                }
            }

            // Hitung rute lewat graph (PathFinder) biar civil gak nembus/nabrak rumah pas kabur
            if (currentPath == null || currentPath.isEmpty() || currentTime - lastPathCalcTime > pathCalcCooldown) {
                currentPath = PathFinder.findPath(x + size / 2.0, y + size / 2.0, fleeTargetX, fleeTargetY, buildings);
                pathIndex = 0;
                lastPathCalcTime = currentTime;
            }

            // Kabur sedikit lebih ngebut dibanding jalan santai biasa
            boolean arrived = followPath(speed * 1.6);

            if (arrived) {
                if (homeAlive) {
                    // Nyampe rumah -> sembunyi, aman!
                    hidden = true;
                    state = CivilState.IDLE;
                    currentPath = null;
                } else if (!isThreatened) {
                    // Udah cukup jauh dari ancaman & gak punya rumah -> tenang lagi
                    state = CivilState.IDLE;
                    currentPath = null;
                    lastStateChange = currentTime;
                } else {
                    // Masih diancam & lari bebas -> cari arah kabur baru
                    currentPath = null;
                }
            }

            return; // Selama kabur, skip logika wandering biasa di bawah
        }

        // Lagi ngumpet aman di rumah -> diem aja, gak usah gerak/wander
        if (hidden) {
            return;
        }

        // ==========================================================
        // 4. LOGIKA WANDERING NORMAL (pakai graph biar gak nabrak rumah)
        // ==========================================================
        if (currentTime - lastStateChange > stateDuration) {

            // 50% Peluang untuk Jalan, 50% Peluang untuk Diam
            if (random.nextBoolean()) {
                state = CivilState.WANDERING;

                // Rumus Trignometri: Mengacak titik di dalam lingkaran (Radius rumah)
                double angle = random.nextDouble() * Math.PI * 2;
                double radius = random.nextDouble() * wanderRadius;

                targetX = homeX + Math.cos(angle) * radius;
                targetY = homeY + Math.sin(angle) * radius;

                stateDuration = 2000 + random.nextInt(4000); // Berjalan selama 2-6 detik
                currentPath = null; // Paksa hitung rute baru ke titik wander yang baru
            } else {
                state = CivilState.IDLE;
                stateDuration = 1000 + random.nextInt(3000); // Terdiam merenung selama 1-4 detik
            }

            lastStateChange = currentTime;
        }

        // 2. Eksekusi Berjalan (lewat graph/node PathFinder, bukan garis lurus lagi)
        if (state == CivilState.WANDERING) {
            if (currentPath == null || currentPath.isEmpty()) {
                currentPath = PathFinder.findPath(x + size / 2.0, y + size / 2.0, targetX, targetY, buildings);
                pathIndex = 0;
            }

            boolean arrived = followPath(speed);
            if (arrived) {
                // Jika sudah sampai tujuan sebelum waktunya habis, langsung diam
                state = CivilState.IDLE;
                currentPath = null;
            }
        }
    }

    // --- FITUR BARU: Helper buat jalan ngikutin node-node hasil graph PathFinder satu-satu ---
    // Return true kalau sudah sampai node terakhir (tujuan akhir)
    private boolean followPath(double moveSpeed) {
        if (currentPath == null || currentPath.isEmpty() || pathIndex >= currentPath.size()) {
            return true;
        }

        Point waypoint = currentPath.get(pathIndex);
        double dx = waypoint.x - (x + size / 4.0);
        double dy = waypoint.y - (y + size / 4.0);
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance <= moveSpeed) {
            pathIndex++;
            if (pathIndex >= currentPath.size()) {
                return true;
            }
        } else {
            x += (dx / distance) * moveSpeed;
            y += (dy / distance) * moveSpeed;
        }
        return false;
    }


    public void draw(Graphics2D g2d, BufferedImage img) {
        // --- FITUR BARU: Lagi ngumpet aman di rumah -> gak usah digambar sama sekali ---
        if (hidden) return;

        if (img != null) {
            // Menggambar gambar civil_h&h.png yang kamu upload
            g2d.drawImage(img, (int)x, (int)y, size, size, null);
        } else {
            // Warna kulit pucat/baju kusam sebagai kotak cadangan
            g2d.setColor(new Color(210, 170, 140));
            g2d.fillRect((int)x, (int)y, size, size);

            // Garis tepi
            g2d.setColor(new Color(100, 60, 40));
            g2d.drawRect((int)x, (int)y, size, size);
        }

        // --- FITUR BARU: BAR DARAH CIVIL (pakai warna hijau biar beda sama Horde yang merah) ---
        int barWidth = size;
        int barHeight = 4;
        int barX = (int) x;
        int barY = (int) y - 8;

        // Background hijau gelap
        g2d.setColor(new Color(20, 70, 20));
        g2d.fillRect(barX, barY, barWidth, barHeight);

        double hpPercentage = currentHp / maxHp;
        if (hpPercentage < 0) hpPercentage = 0;
        int currentBarWidth = (int) (barWidth * hpPercentage);

        g2d.setColor(new Color(90, 220, 90));
        g2d.fillRect(barX, barY, currentBarWidth, barHeight);

        // Outline hitam
        g2d.setColor(Color.BLACK);
        g2d.drawRect(barX, barY, barWidth, barHeight);

        // --- FITUR BARU: Tanda seru kuning kalau lagi panik/kabur ---
        if (state == CivilState.FLEEING) {
            g2d.setColor(new Color(255, 220, 0));
            g2d.setFont(g2d.getFont().deriveFont(Font.BOLD, 12f));
            g2d.drawString("!", (int) x + size / 2 - 2, barY - 3);
        }
    }
}