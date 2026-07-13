import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.List;

public class Horde implements Serializable {

    // Enum (Sistem Status & Tipe)
    // AXEMAN = 1Horde, SHIELDBEARER = 2Horde, BOWMAN = 3Horde
    public enum HordeType { AXEMAN, SHIELDBEARER, BOWMAN }
    public enum HordeState { IDLE, MOVING, ATTACKING }

    // Atribut Identitas
    public HordeType type;
    public HordeState state;
    public double speed;

    // Atribut Posisi & Geometri
    public double x, y;
    public int size = 20; // Samakan dengan Guard agar proporsinya imbang

    // --- FITUR BARU: ARAH HADAP (Kiri/Kanan) ---
    // Default false = menghadap kiri (sesuai gambar asli 1Horde.png, 2Horde.png, 3Horde.png)
    public boolean facingRight = false;

    // Atribut Status Pertarungan (Samsak)
    public double maxHp;
    public double currentHp;
    public double attackDamage;

    // Atribut Pertarungan Lanjutan
    public long lastAttackTime = 0;
    public long attackCooldown = 1200; // Musuh memukul sedikit lebih lambat (1.2 detik)
    public double attackRange = size + 5;
    // --- FITUR BARU: PATHFINDING BUAT JALAN NGEDEKATIN TARGET + FALLBACK HANCURIN TEMBOK ---
    public transient java.util.List<Point> path = null;
    public int pathIndex = 0;
    private long lastChasePathTime = 0;
    private static final long CHASE_REPATH_COOLDOWN = 600; // ms, jangan itung ulang A* tiap frame
    private Building forcedWallTarget = null; // Diisi kalau lagi kejebak & harus mecahin tembok dulu

    public Horde(HordeType type, double startX, double startY) {
        this.type = type;

        // Geser anchor point agar pas di tengah kursor saat di-spawn
        this.x = startX - (size / 2.0);
        this.y = startY - (size / 2.0);
        this.state = HordeState.IDLE;

        // Penentuan Status berdasarkan Tipe Horde
        if (this.type == HordeType.AXEMAN) {
            this.maxHp = 80;
            this.attackDamage = 10;
            this.speed = 0.8;
        } else if (this.type == HordeType.SHIELDBEARER) {
            this.maxHp = 150; // Darah paling tebal (Tank)
            this.attackDamage = 5;
            this.speed = 0.7; // Paling lambat
        } else if (this.type == HordeType.BOWMAN) {
            this.maxHp = 50;  // Darah tipis
            this.attackDamage = 15;
            this.speed = 1;
        }

        // Darah penuh saat pertama kali spawn
        this.currentHp = this.maxHp;
    }

    // Fungsi update menerima tambahan daftar Proyektil (GameWindow.activeProjectiles)
    // --- FITUR BARU: Tambahan parameter allCivils & allBuildings, biar Horde bisa niatin warga sipil & bangunan juga ---
    public void update(List<Horde> allHordes, List<Guard> allGuards, java.util.List<Projectile> allProjectiles, List<Civil> allCivils, List<Building> allBuildings) {

        long currentTime = System.currentTimeMillis();

        // Cari Guard terdekat untuk disamperin
        Guard targetGuard = null;
        double minDistance = 9999;
        for (Guard guard : allGuards) {
            double dx = this.x - guard.x;
            double dy = this.y - guard.y;
            double distance = Math.sqrt(dx * dx + dy * dy);
            if (distance < minDistance) {
                minDistance = distance;
                targetGuard = guard;
            }
        }

        // --- FITUR BARU: Cari Civil terdekat juga (yang gak lagi ngumpet aman di rumah) ---
        Civil targetCivil = null;
        double minCivilDistance = 9999;
        if (allCivils != null) {
            for (Civil c : allCivils) {
                if (c.hidden || c.currentHp <= 0) continue; // Civil yang lagi ngumpet gak bisa disamperin
                double dx = this.x - c.x;
                double dy = this.y - c.y;
                double distance = Math.sqrt(dx * dx + dy * dy);
                if (distance < minCivilDistance) {
                    minCivilDistance = distance;
                    targetCivil = c;
                }
            }
        }

        // --- FITUR BARU: Cari Building terdekat juga (yang udah jadi & masih ada darahnya) ---
        Building targetBuildingCandidate = null;
        double minBuildingDistance = 9999;
        if (allBuildings != null) {
            for (Building b : allBuildings) {
                if (!b.isBuilt || b.currentHp <= 0) continue;
                Rectangle hb = b.getSolidHitbox();
                double distance = distanceToRect(hb, this.x, this.y);
                if (distance < minBuildingDistance) {
                    minBuildingDistance = distance;
                    targetBuildingCandidate = b;
                }
            }
        }

        // --- FITUR BARU: PRIORITAS BERTINGKAT (bukan lagi 'yang paling deket menang') ---
        // 1. Guard yang ada DI SEKITAR horde ini -> prioritas utama
        // 2. Kalau gak ada Guard di sekitar -> serang Building terdekat
        // 3. Kalau Building di sekitar udah abis (gak ada yang deket) & ada Civil -> kejar Civil
        // 4. Tapi kalau ngejar Civil udah kejauhan dari Building manapun -> nyerah, balik hancurin Building lain (termasuk Heart)
        final double AGGRO_GUARD_RADIUS = 250.0;     // Guard dianggap "di sekitar" kalau sedeket ini
        final double AGGRO_BUILDING_RADIUS = 250.0;  // Building dianggap "di sekitar" kalau sedeket ini
        final double CIVIL_CHASE_LEASH = 350.0;      // Batas jauh ngejar Civil sebelum nyerah balik ke Building

        boolean hasNearGuard = targetGuard != null && minDistance <= AGGRO_GUARD_RADIUS;
        boolean hasNearBuilding = targetBuildingCandidate != null && minBuildingDistance <= AGGRO_BUILDING_RADIUS;

        boolean attackingCivil = false;
        boolean attackingBuilding = false;
        Building targetBuilding = null;
        double targetX = 0, targetY = 0, minTargetDistance = -1;

        if (hasNearGuard) {
            // 1. Guard di sekitar -> prioritas utama
            targetX = targetGuard.x;
            targetY = targetGuard.y;
            minTargetDistance = minDistance;
        } else if (hasNearBuilding) {
            // 2. Gak ada Guard di sekitar -> serang Building terdekat
            attackingBuilding = true;
            targetBuilding = targetBuildingCandidate;
            Point2D.Double nearestPoint = nearestPointOnRect(targetBuildingCandidate.getSolidHitbox(), this.x, this.y);
            targetX = nearestPoint.x;
            targetY = nearestPoint.y;
            minTargetDistance = minBuildingDistance;
        } else if (targetCivil != null) {
            // 3. Building di sekitar udah abis, ada Civil -> kejar, KECUALI udah kejauhan dari Building manapun (leash)
            boolean tooFarFromAnyBuilding = targetBuildingCandidate != null && minBuildingDistance > CIVIL_CHASE_LEASH;
            if (tooFarFromAnyBuilding) {
                // 4. Nyerah ngejar Civil -> balik hancurin Building lain (walau jauh, termasuk Heart)
                attackingBuilding = true;
                targetBuilding = targetBuildingCandidate;
                Point2D.Double nearestPoint = nearestPointOnRect(targetBuildingCandidate.getSolidHitbox(), this.x, this.y);
                targetX = nearestPoint.x;
                targetY = nearestPoint.y;
                minTargetDistance = minBuildingDistance;
            } else {
                attackingCivil = true;
                targetX = targetCivil.x;
                targetY = targetCivil.y;
                minTargetDistance = minCivilDistance;
            }
        } else if (targetBuildingCandidate != null) {
            // Fallback: gak ada Guard/Civil sama sekali, tapi masih ada Building -> tetep samperin walau jauh
            attackingBuilding = true;
            targetBuilding = targetBuildingCandidate;
            Point2D.Double nearestPoint = nearestPointOnRect(targetBuildingCandidate.getSolidHitbox(), this.x, this.y);
            targetX = nearestPoint.x;
            targetY = nearestPoint.y;
            minTargetDistance = minBuildingDistance;
        } else if (targetGuard != null) {
            // Fallback terakhir: Guard jauh tapi gak ada target lain sama sekali
            targetX = targetGuard.x;
            targetY = targetGuard.y;
            minTargetDistance = minDistance;
        }

        // --- Kalau lagi kejebak & harus mecahin tembok, override target ke tembok itu ---
        if (forcedWallTarget != null) {
            if (forcedWallTarget.currentHp <= 0 || !forcedWallTarget.isBuilt) {
                forcedWallTarget = null; // Tembok udah hancur, lepas override
            } else {
                attackingBuilding = true;
                attackingCivil = false;
                targetBuilding = forcedWallTarget;
                Point2D.Double np = nearestPointOnRect(forcedWallTarget.getSolidHitbox(), this.x, this.y);
                targetX = np.x;
                targetY = np.y;
                minTargetDistance = distanceToRect(forcedWallTarget.getSolidHitbox(), this.x, this.y);
            }
        }


        if (minTargetDistance >= 0) {
            if (type == HordeType.BOWMAN) {
                // --- LOGIKA BOWMAN (PANAH) ---
                double attackRangePanahMusuh = 250.0; // Sedikit lebih pendek dari player biar imbang

                if (minTargetDistance <= attackRangePanahMusuh) {
                    // Masuk jarak tembak: Berhenti dan Tembak (Cek cooldown)
                    // --- FITUR BARU: Saat menembak, tetap hadap ke arah target ---
                    if (targetX > this.x + 0.1) facingRight = true;
                    else if (targetX < this.x - 0.1) facingRight = false;

                    if (currentTime - lastAttackTime >= attackCooldown) {
                        if (attackingBuilding) {
                            // --- FITUR BARU: Panah ke Building damage langsung ---
                            // (sistem collision Projectile yang ada cuma ngecek nabrak Horde/Guard, belum Building,
                            // jadi biar Bowman tetap bisa "menembak" bangunan, damage-nya langsung dikenakan)
                            targetBuilding.currentHp -= this.attackDamage;
                        } else {
                            // BUAT PROYEKTIL BARU (false = dari horde, damage 15)
                            allProjectiles.add(new Projectile(x, y, targetX, targetY, false, attackDamage));
                        }
                        lastAttackTime = currentTime;
                    }
                } else {
                    // Di luar jarak tembak: Maju! (sekarang pakai PathFinder, bukan garis lurus)
                    moveTowardTarget(targetX, targetY, allBuildings);
                }

            } else {
                // --- LOGIKA MELEE (AXEMAN & SHIELD - Tetap aslinya) ---
                double attackRangeMeleeMusuh = size + 5;
                if (minTargetDistance <= attackRangeMeleeMusuh) {
                    // --- FITUR BARU: Saat memukul, tetap hadap ke arah target ---
                    if (targetX > this.x + 0.1) facingRight = true;
                    else if (targetX < this.x - 0.1) facingRight = false;

                    if (currentTime - lastAttackTime >= attackCooldown) {
                        if (attackingCivil) {
                            targetCivil.currentHp -= this.attackDamage;
                        } else if (attackingBuilding) {
                            targetBuilding.currentHp -= this.attackDamage;
                        } else {
                            targetGuard.currentHp -= this.attackDamage;
                        }
                        lastAttackTime = currentTime;
                    }
                } else {
                    // Di luar jarak serang: Maju! (sekarang pakai PathFinder, bukan garis lurus)
                    moveTowardTarget(targetX, targetY, allBuildings);
                }
            }
        }

        // --- Logika Anti-Tumpuk (Dipertahankan) ---
        int personalSpace = size - 5;
        for (Horde other : allHordes) {
            if (other == this) continue;
            double dx = this.x - other.x;
            double dy = this.y - other.y;
            double distanceSq = dx * dx + dy * dy;
            if (distanceSq == 0) {
                this.x += Math.random() * 2 - 1; this.y += Math.random() * 2 - 1; continue;
            }
            if (distanceSq < personalSpace * personalSpace) {
                double distance = Math.sqrt(distanceSq);
                double pushForce = (personalSpace - distance) / personalSpace;
                this.x += (dx / distance) * pushForce * 1.5;
                this.y += (dy / distance) * pushForce * 1.5;
            }
        }
        for (Guard guard : allGuards) {
            double dx = this.x - guard.x;
            double dy = this.y - guard.y;
            double distanceSq = dx * dx + dy * dy;
            if (distanceSq < personalSpace * personalSpace && distanceSq > 0) {
                double distance = Math.sqrt(distanceSq);
                double pushForce = (personalSpace - distance) / personalSpace;
                this.x += (dx / distance) * pushForce * 1.5;
                this.y += (dy / distance) * pushForce * 1.5;
            }
        }
    }

    // --- FITUR BARU: Helper hitung jarak dari titik (Horde) ke sisi terdekat rectangle Building ---
    // Dipakai supaya jangkauan serang/deteksi Building tetap akurat walau bangunannya gede,
    // (bukan ngukur ke titik tengah bangunan, yang bisa nyangkut jauh di dalam hitbox solid).
    private double distanceToRect(Rectangle r, double px, double py) {
        double cx = Math.max(r.x, Math.min(px, r.x + r.width));
        double cy = Math.max(r.y, Math.min(py, r.y + r.height));
        double dx = px - cx, dy = py - cy;
        return Math.sqrt(dx * dx + dy * dy);
    }

    private Point2D.Double nearestPointOnRect(Rectangle r, double px, double py) {
        double cx = Math.max(r.x, Math.min(px, r.x + r.width));
        double cy = Math.max(r.y, Math.min(py, r.y + r.height));
        return new Point2D.Double(cx, cy);
    }
    // Kalau target gak reachable sama sekali (terkurung tembok tanpa celah),
    // otomatis alihkan buat ngehancurin Wall terdekat yang beneran reachable dulu.
    private void moveTowardTarget(double tx, double ty, List<Building> allBuildings) {
        if (hasClearLine(x, y, tx, ty, allBuildings)) {
            path = null;
            double dx = tx - x, dy = ty - y;
            double dist = Math.sqrt(dx * dx + dy * dy);
            if (dist > 1) {
                if (dx > 0.1) facingRight = true;
                else if (dx < -0.1) facingRight = false;
                x += (dx / dist) * speed;
                y += (dy / dist) * speed;
            }
            return;
        }

        long now = System.currentTimeMillis();
        boolean needRepath = path == null || path.isEmpty() || pathIndex >= path.size()
                || now - lastChasePathTime > CHASE_REPATH_COOLDOWN;

        if (needRepath) {
            List<Point> newPath = PathFinder.findPath(x, y, tx, ty, allBuildings);

            if (newPath.isEmpty() && allBuildings != null) {
                Building nearestWall = findNearestReachableWall(allBuildings);
                if (nearestWall != null) {
                    forcedWallTarget = nearestWall;
                    Point2D.Double np = nearestPointOnRect(nearestWall.getSolidHitbox(), x, y);
                    newPath = PathFinder.findPath(x, y, np.x, np.y, allBuildings);
                }
            } else {
                forcedWallTarget = null;
            }

            path = newPath;
            pathIndex = 0;
            lastChasePathTime = now;
        }

        if (path != null && pathIndex < path.size()) {
            Point node = path.get(pathIndex);
            double nx = node.x - size / 2.0;
            double ny = node.y - size / 2.0;
            double dx = nx - x, dy = ny - y;
            double dist = Math.sqrt(dx * dx + dy * dy);

            if (dist > 12.0) {
                if (dx > 0.1) facingRight = true;
                else if (dx < -0.1) facingRight = false;
                x += (dx / dist) * speed;
                y += (dy / dist) * speed;
            } else {
                pathIndex++;
            }
        }
    }
    private boolean hasClearLine(double x1, double y1, double x2, double y2, java.util.List<Building> buildings) {
        if (buildings == null) return true;
        double dx = x2 - x1, dy = y2 - y1;
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist < 1) return true;

        int steps = (int) (dist / 15) + 1; // Cek tiap ~15 unit sepanjang garis
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            double px = x1 + dx * t;
            double py = y1 + dy * t;
            for (Building b : buildings) {
                if (b.getSolidHitbox().contains(px, py)) return false;
            }
        }
        return true;
    }

    // --- FITUR BARU: Cari Wall terdekat yang beneran bisa dijangkau (path-nya gak kosong) ---
    private Building findNearestReachableWall(List<Building> allBuildings) {
        Building nearest = null;
        double bestDist = Double.MAX_VALUE;
        for (Building b : allBuildings) {
            if (!b.isBuilt || b.currentHp <= 0) continue;
            if (b.type != Building.BuildingType.WALL_L
                    && b.type != Building.BuildingType.WALL_R
                    && b.type != Building.BuildingType.WALL_UD) continue;

            double d = distanceToRect(b.getSolidHitbox(), x, y);
            if (d >= bestDist) continue;

            List<Point> p = PathFinder.findPath(x, y,
                    b.getSolidHitbox().getCenterX(), b.getSolidHitbox().getCenterY(), allBuildings);
            if (!p.isEmpty()) {
                bestDist = d;
                nearest = b;
            }
        }
        return nearest;
    }

    public void draw(Graphics2D g2d, BufferedImage img) {

        // --- LAYER 1: AURA MERAH (Pengganti lingkaran seleksi) ---
        g2d.setColor(new Color(200, 0, 0, 100)); // Merah transparan
        int ovalWidth = (int) (size * 0.50);
        int ovalHeight = (int) ((size / 2) * 0.50);
        int ovalX = (int)x + (size - ovalWidth) / 2 +3;
        int ovalY = (int)y + size - (ovalHeight / 2) - 4; // Posisinya sama persis dengan Guard
        g2d.fillOval(ovalX, ovalY, ovalWidth, ovalHeight);

        // --- LAYER 2: GAMBAR HORDE ---
        if (img != null) {
            // --- FITUR BARU: Flip gambar horizontal kalau lagi menghadap kanan ---
            // (Sprite asli Horde defaultnya menghadap KIRI, kebalikan dari Guard)
            if (facingRight) {
                java.awt.geom.AffineTransform oldTransform = g2d.getTransform();
                g2d.translate((int) x + size, (int) y);
                g2d.scale(-1, 1);
                g2d.drawImage(img, 0, 0, size, size, null);
                g2d.setTransform(oldTransform);
            } else {
                // Gambar langsung mengikuti ukuran hitbox (size x size) agar proporsinya sama dengan Guard
                g2d.drawImage(img, (int)x, (int)y, size, size, null);
            }
        } else {
            g2d.setColor(Color.RED);
            g2d.fillRect((int)x, (int)y, size, size);
        }

        // --- LAYER 3: BAR DARAH MUSUH ---
        int barWidth = size;
        int barHeight = 4;
        int barX = (int) x;
        int barY = (int) y - 8; // Posisi bar darah disamakan dengan Guard

        // Background merah gelap
        g2d.setColor(new Color(150, 0, 0));
        g2d.fillRect(barX, barY, barWidth, barHeight);

        // Darah merah terang
        double hpPercentage = currentHp / maxHp;
        if (hpPercentage < 0) hpPercentage = 0;
        int currentBarWidth = (int) (barWidth * hpPercentage);

        g2d.setColor(new Color(255, 50, 50));
        g2d.fillRect(barX, barY, currentBarWidth, barHeight);

        // Outline hitam
        g2d.setColor(Color.BLACK);
        g2d.drawRect(barX, barY, barWidth, barHeight);
    }
}