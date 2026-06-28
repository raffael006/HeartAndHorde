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

    // Atribut Identitas
    public GuardType type;
    public GuardState state;
    public double targetX, targetY;
    public double speed = 1.0;

    public double x, y;
    public int size = 24;
    public boolean isSelected = false;

    public double maxHp;
    public double currentHp;
    public double attackDamage;

    public long lastAttackTime = 0;
    public long attackCooldown = 1000;
    public double attackRange = size + 5;
    public double sightRange = 200;

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

    public void update(java.util.List<Guard> allGuards, java.util.List<Horde> allHordes, java.util.List<Projectile> allProjectiles) {

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

            if (targetEnemy != null) {
                if (minDistance <= attackRangePanah) {
                    if (state == GuardState.MOVING) return;
                    if (currentTime - lastAttackTime >= attackCooldown) {
                        allProjectiles.add(new Projectile(x, y, targetEnemy.x, targetEnemy.y, true, attackDamage));
                        lastAttackTime = currentTime;
                    }
                } else if (state != GuardState.MOVING) {
                    double dx = targetEnemy.x - this.x;
                    double dy = targetEnemy.y - this.y;
                    this.x += (dx / minDistance) * speed;
                    this.y += (dy / minDistance) * speed;
                }
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

            if (targetEnemyMelee != null) {
                if (minDistanceMelee <= attackRangeMelee) {
                    if (currentTime - lastAttackTime >= attackCooldown) {
                        targetEnemyMelee.currentHp -= this.attackDamage;
                        lastAttackTime = currentTime;
                    }
                } else if (state != GuardState.MOVING) {
                    double dx = targetEnemyMelee.x - this.x;
                    double dy = targetEnemyMelee.y - this.y;
                    this.x += (dx / minDistanceMelee) * speed;
                    this.y += (dy / minDistanceMelee) * speed;
                }
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
            g2d.drawImage(fullSheet, (int)x, (int)y, size, size, null);
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