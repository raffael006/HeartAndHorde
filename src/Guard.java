import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.List;

public class Guard implements Serializable {

    public enum GuardType { ARCHER, SPEARMAN }
    public enum GuardState { IDLE, MOVING, DEFENDING, CHASING }

    // --- VARIABEL GRAPH ---
    public transient java.util.List<Point> path = null;
    public int pathIndex = 0;

    // --- INI METHOD YANG KEMARIN HILANG ---
    public void setPath(java.util.List<Point> newPath) {
        this.path = newPath;
        this.pathIndex = 0;
    }

    // --- FITUR BARU: kalau lagi hold position dan gak ada musuh valid, balik ke titik jaga ---
    private void goHomeToPost(java.util.List<Building> allBuildings) {
        if (state == GuardState.MOVING) return; // udah lagi jalan, jangan diinterupsi
        double dxHome = holdX - x, dyHome = holdY - y;
        double distHome = Math.sqrt(dxHome * dxHome + dyHome * dyHome);
        if (distHome > 12.0) {
            java.util.List<Point> pathHome = PathFinder.findPath(
                    x + size / 2.0, y + size / 2.0,
                    holdX + size / 2.0, holdY + size / 2.0,
                    allBuildings
            );
            setPath(pathHome);
            targetX = holdX;
            targetY = holdY;
            state = GuardState.MOVING;
        }
    }

    // --- FITUR BARU: Jalan ngejar pakai PathFinder (bukan garis lurus), throttled biar gak berat ---
    private void chaseTarget(double targetX, double targetY, java.util.List<Building> allBuildings) {
        // Kalau garis lurus ke target masih bersih, jalan LANGSUNG - jauh lebih halus,
        // PathFinder cuma dipanggil kalau garis lurusnya beneran keblok bangunan.
        if (hasClearLine(x, y, targetX, targetY, allBuildings)) {
            path = null; // Buang path lama, biar kalau ke-blok lagi nanti, mulai fresh
            double dx = targetX - x;
            double dy = targetY - y;
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
        if (path == null || path.isEmpty() || pathIndex >= path.size()
                || now - lastChasePathTime > CHASE_REPATH_COOLDOWN) {
            path = PathFinder.findPath(x, y, targetX, targetY, allBuildings);
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

    // --- FITUR BARU: Cek apakah garis lurus dari (x1,y1) ke (x2,y2) ketutup bangunan atau enggak ---
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

    // Atribut Identitas
    public GuardType type;
    public GuardState state;
    public double targetX, targetY;
    public double speed = 1.0;

    public double x, y;
    public int size = 20;
    public boolean isSelected = false;

    // --- FITUR BARU: ARAH HADAP (Kiri/Kanan) ---
    // Default true = menghadap kanan (sesuai gambar asli Hearthguardbow.png & hearthguard_spier.png)
    public boolean facingRight = true;

    public double maxHp;
    public double currentHp;
    public double attackDamage;

    public long lastAttackTime = 0;
    public long attackCooldown = 1000;
    public double attackRange = size + 5;
    public double sightRange = 200;
    public boolean holdPosition = false;
    public double holdX, holdY;
    private static final double HOLD_LEASH_RADIUS = 260.0; // dikit lebih gede dari attackRangePanah biar gak ke-cut pas nembak di ujung
    // --- FITUR BARU: Throttle buat PathFinder pas ngejar musuh ---
    private long lastChasePathTime = 0;
    private static final long CHASE_REPATH_COOLDOWN = 600; // ms

    public Guard(GuardType type, double startX, double startY) {
        this.type = type;

        this.x = startX - (size / 2.0);
        this.y = startY - (size / 2.0);

        this.targetX = this.x;
        this.targetY = this.y;
        this.state = GuardState.IDLE;

        if (this.type == GuardType.SPEARMAN) {
            this.maxHp = 100;
            this.attackDamage = 15;
        } else if (this.type == GuardType.ARCHER) {
            this.maxHp = 60;
            this.attackDamage = 20;
        }

        this.currentHp = this.maxHp;
    }

    public void update(java.util.List<Guard> allGuards, java.util.List<Horde> allHordes, java.util.List<Projectile> allProjectiles, java.util.List<Building> allBuildings) {

        // --- 1. Logika Berjalan Penerapan GRAPH ---
        if (state == GuardState.MOVING) {
            if (path != null && pathIndex < path.size()) {
                Point nextNode = path.get(pathIndex);
                double nodeTargetX = nextNode.x - (size / 2.0);
                double nodeTargetY = nextNode.y - (size / 2.0);

                double dx = nodeTargetX - x;
                double dy = nodeTargetY - y;
                double distance = Math.sqrt(dx * dx + dy * dy);

                // FIX: Toleransi diubah jadi 12.0 agar tidak terjadi traffic jam saat gerombolan lewat gang sempit!
                if (distance > 12.0) {
                    // --- FITUR BARU: Update arah hadap sesuai arah jalan ---
                    if (dx > 0.1) facingRight = true;
                    else if (dx < -0.1) facingRight = false;

                    x += (dx / distance) * speed;
                    y += (dy / distance) * speed;
                } else {
                    pathIndex++; // Lanjut ke node selanjutnya
                }
            } else {
                // Rute sudah habis, bergerak perlahan paskan posisi barisan (Formasi Akhir)
                double dx = targetX - x;
                double dy = targetY - y;
                double distance = Math.sqrt(dx * dx + dy * dy);

                if (distance > speed) {
                    // --- FITUR BARU: Update arah hadap sesuai arah jalan (formasi akhir) ---
                    if (dx > 0.1) facingRight = true;
                    else if (dx < -0.1) facingRight = false;

                    x += (dx / distance) * speed;
                    y += (dy / distance) * speed;
                } else {
                    x = targetX;
                    y = targetY;
                    state = GuardState.IDLE;
                    path = null;
                }
            }
        }

        // --- 2. Logika PERTEMPURAN ---
        long currentTime = System.currentTimeMillis();

        if (type == GuardType.ARCHER) {
            double attackRangePanah = 250.0;
            Horde targetEnemy = null;
            double minDistance = sightRange;

            for (Horde enemy : allHordes) {
                double dx = this.x - enemy.x;
                double dy = this.y - enemy.y;
                double distance = Math.sqrt(dx * dx + dy * dy);

                if (distance < minDistance) {
                    minDistance = distance;
                    targetEnemy = enemy;
                }
            }

            // FITUR BARU: kalau lagi hold position, musuh di luar radius jaga jangan dikejar
            if (targetEnemy != null && holdPosition) {
                double dxHold = targetEnemy.x - holdX;
                double dyHold = targetEnemy.y - holdY;
                if (Math.sqrt(dxHold * dxHold + dyHold * dyHold) > HOLD_LEASH_RADIUS) {
                    targetEnemy = null;
                }
            }

            if (targetEnemy != null) {
                if (minDistance <= attackRangePanah) {
                    // --- FIX: begitu masuk jarak tembak, LANGSUNG berhenti di tempat &
                    // mulai nembak dari situ -- gak perlu nunggu sampai titik tujuan klik
                    // dulu. State diubah dari MOVING jadi DEFENDING supaya bagian
                    // "1. Logika Berjalan" di atas otomatis berhenti gerakin guard
                    // di tick berikutnya, dan path lama (tujuan klik) dibuang.
                    state = GuardState.DEFENDING;
                    path = null;

                    // --- FITUR BARU: Saat menembak, tetap hadap ke arah musuh ---
                    if (targetEnemy.x > this.x + 0.1) facingRight = true;
                    else if (targetEnemy.x < this.x - 0.1) facingRight = false;

                    if (currentTime - lastAttackTime >= attackCooldown) {
                        allProjectiles.add(new Projectile(x, y, targetEnemy.x, targetEnemy.y, true, attackDamage));
                        lastAttackTime = currentTime;
                    }
                } else if (state != GuardState.MOVING) {
                    if (targetEnemy.x > this.x + 0.1) facingRight = true;
                    else if (targetEnemy.x < this.x - 0.1) facingRight = false;
                    chaseTarget(targetEnemy.x, targetEnemy.y, allBuildings);
                }
            } else if (holdPosition) {
                goHomeToPost(allBuildings);
            }

        } else if (type == GuardType.SPEARMAN) {
            double attackRangeMelee = size + 5;
            Horde targetEnemyMelee = null;
            double minDistanceMelee = sightRange;

            for (Horde enemy : allHordes) {
                double dx = this.x - enemy.x;
                double dy = this.y - enemy.y;
                double distanceMelee = Math.sqrt(dx * dx + dy * dy);
                if (distanceMelee < minDistanceMelee) {
                    minDistanceMelee = distanceMelee;
                    targetEnemyMelee = enemy;
                }
            }

            // FITUR BARU: kalau lagi hold position, musuh di luar radius jaga jangan dikejar
            if (targetEnemyMelee != null && holdPosition) {
                double dxHold = targetEnemyMelee.x - holdX;
                double dyHold = targetEnemyMelee.y - holdY;
                if (Math.sqrt(dxHold * dxHold + dyHold * dyHold) > HOLD_LEASH_RADIUS) {
                    targetEnemyMelee = null;
                }
            }

            if (targetEnemyMelee != null) {
                if (minDistanceMelee <= attackRangeMelee) {
                    // --- FIX: sama seperti Archer -- begitu masuk jarak pukul, berhenti
                    // di tempat (bukan lagi MOVING) baru menyerang, gak nyerobot nyerang
                    // sambil tetap "dianggap" jalan menuju titik klik.
                    state = GuardState.DEFENDING;
                    path = null;

                    // --- FITUR BARU: Saat memukul, tetap hadap ke arah musuh ---
                    if (targetEnemyMelee.x > this.x + 0.1) facingRight = true;
                    else if (targetEnemyMelee.x < this.x - 0.1) facingRight = false;

                    if (currentTime - lastAttackTime >= attackCooldown) {
                        targetEnemyMelee.currentHp -= this.attackDamage;
                        lastAttackTime = currentTime;
                    }
                } else if (state != GuardState.MOVING) {
                    if (targetEnemyMelee.x > this.x + 0.1) facingRight = true;
                    else if (targetEnemyMelee.x < this.x - 0.1) facingRight = false;
                    chaseTarget(targetEnemyMelee.x, targetEnemyMelee.y, allBuildings);
                }
            } else if (holdPosition) {
                goHomeToPost(allBuildings);
            }
        }

        // --- 3. Logika Anti-Tumpuk ---
        int personalSpace = size - 5;
        for (Guard other : allGuards) {
            if (other == this) continue;
            double dx = this.x - other.x;
            double dy = this.y - other.y;
            double distanceSq = dx * dx + dy * dy;
            if (distanceSq < personalSpace * personalSpace && distanceSq > 0) {
                double distance = Math.sqrt(distanceSq);
                double pushForce = (personalSpace - distance) / personalSpace;
                this.x += (dx / distance) * pushForce * 1.5;
                this.y += (dy / distance) * pushForce * 1.5;
            }
        }
    }

    public void draw(Graphics2D g2d, BufferedImage fullSheet) {
        if (isSelected) {
            g2d.setColor(Color.YELLOW);
            g2d.setStroke(new BasicStroke(1.5f));
            int ovalWidth = (int) (size * 0.50);
            int ovalHeight = (int) ((size / 2) * 0.50);
            int ovalX = (int)x + (size - ovalWidth) / 2;
            int ovalY = (int)y + size - (ovalHeight / 2) - 4;
            g2d.drawOval(ovalX, ovalY, ovalWidth, ovalHeight);
        }

        if (fullSheet != null) {
            // --- FITUR BARU: Flip gambar horizontal kalau lagi menghadap kiri ---
            if (!facingRight) {
                java.awt.geom.AffineTransform oldTransform = g2d.getTransform();
                g2d.translate((int) x + size, (int) y);
                g2d.scale(-1, 1);
                g2d.drawImage(fullSheet, 0, 0, size, size, null);
                g2d.setTransform(oldTransform);
            } else {
                g2d.drawImage(fullSheet, (int)x, (int)y, size, size, null);
            }
        } else {
            g2d.setColor(type == GuardType.ARCHER ? new Color(50, 150, 50) : new Color(50, 50, 150));
            g2d.fillRect((int)x, (int)y, size, size);
        }

        int barWidth = size;
        int barHeight = 4;
        int barX = (int) x;
        int barY = (int) y - 8;

        g2d.setColor(Color.RED);
        g2d.fillRect(barX, barY, barWidth, barHeight);

        double hpPercentage = currentHp / maxHp;
        if (hpPercentage < 0) hpPercentage = 0;
        int currentBarWidth = (int) (barWidth * hpPercentage);

        g2d.setColor(Color.GREEN);
        g2d.fillRect(barX, barY, currentBarWidth, barHeight);

        g2d.setColor(Color.BLACK);
        g2d.drawRect(barX, barY, barWidth, barHeight);
    }
}