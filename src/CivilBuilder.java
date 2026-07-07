import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.List;

public class CivilBuilder implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum BuilderState { IDLE_HOME, MOVING_TO_SITE, BUILDING, MOVING_TO_TREE, CHOPPING, RETURNING_HOME }

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

    // --- FITUR BARU: ANTREAN BANGUNAN ---
    // Daftar bangunan lain yang menunggu digarap oleh builder ini setelah yang sekarang selesai.
    // FIFO (First In First Out) -> yang dipesan duluan, dikerjakan duluan.
    public List<Building> buildQueue = new java.util.LinkedList<>();
    public Tree assignedTree = null;
    public List<Tree> chopQueue = new java.util.LinkedList<>();

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

    // --- FIX UMUM PATHFINDING (menggantikan fix sementara sebelumnya) ---
    // Builder tidak boleh menganggap (1) bangunan yang sedang dia TUJU, atau
    // (2) bangunan yang hitbox-nya kebetulan menutupi POSISI builder SAAT INI
    // (misalnya baru selesai bangun FARM dan masih berdiri persis di atasnya)
    // sebagai tembok penghalang jalan. Kalau tidak difilter, builder bisa "terkurung"
    // oleh hitbox-nya sendiri -> PathFinder gagal kasih jalan (path kosong) -> builder
    // dianggap "sudah sampai" padahal belum gerak sama sekali, atau malah hilang dari
    // layar karena statusnya keburu balik IDLE_HOME padahal fisiknya masih nyangkut.
    private List<Building> buildSafeObstacleList(List<Building> allBuildings, Building extraExclude) {
        List<Building> result = new java.util.ArrayList<>();
        if (allBuildings == null) return result;
        Point myPos = new Point((int) x, (int) y);
        for (Building b : allBuildings) {
            if (b == extraExclude) continue;
            if (b.getSolidHitbox().contains(myPos)) continue; // Lagi nginjek/di dalam hitbox ini -> jangan dianggap tembok
            result.add(b);
        }
        return result;
    }

    // Dipanggil dari GamePanel waktu ada bangunan butuh tukang
    public void assignToBuild(Building target, List<Building> allBuildings) {
        this.assignedBuilding = target;
        target.assignedBuilder = this;
        this.buildingsRef = allBuildings;

        double targetX = target.getBounds().getCenterX();
        double targetY = target.getBounds().getCenterY();

        List<Building> obstacles = buildSafeObstacleList(allBuildings, target);
        setPath(PathFinder.findPath(x, y, targetX, targetY, obstacles));
        state = BuilderState.MOVING_TO_SITE;
    }

    public void assignToChop(Tree target, List<Building> allBuildings) {
        this.assignedTree = target;
        target.assignedBuilder = this;
        this.buildingsRef = allBuildings;

        double targetX = target.getBounds().getCenterX();
        double targetY = target.getBounds().getCenterY();

        List<Building> obstacles = buildSafeObstacleList(allBuildings, null);
        setPath(PathFinder.findPath(x, y, targetX, targetY, obstacles));
        state = BuilderState.MOVING_TO_TREE;
    }

    public void queueChop(Tree target, List<Building> allBuildings) {
        target.assignedBuilder = this;
        this.buildingsRef = allBuildings;

        if (state == BuilderState.IDLE_HOME && assignedBuilding == null && assignedTree == null) {
            assignToChop(target, allBuildings);
        } else {
            chopQueue.add(target);
        }
    }

    // Dipanggil tiap kali 1 tugas kelar -> cari kerjaan berikutnya (build dulu, baru chop, baru pulang)
    private void goToNextJobOrHome() {
        if (!buildQueue.isEmpty()) {
            assignToBuild(buildQueue.remove(0), buildingsRef);
        } else if (!chopQueue.isEmpty()) {
            assignToChop(chopQueue.remove(0), buildingsRef);
        } else {
            startReturningHome();
        }
    }

    // --- FITUR BARU: DIPANGGIL DARI GamePanel UNTUK MENGANTRIKAN BANGUNAN ---
    // Kalau builder ini lagi nganggur di rumah -> langsung berangkat sekarang juga.
    // Kalau builder ini lagi sibuk (jalan/bangun/pulang) -> taruh di antrean dulu,
    // nanti otomatis disamperin abis kerjaan yang sekarang kelar (TANPA mampir pulang dulu).
    public void queueBuilding(Building target, List<Building> allBuildings) {
        target.assignedBuilder = this; // Supaya tidak direbut/ditawarkan ke builder lain selagi masih diantre
        this.buildingsRef = allBuildings;

        if (state == BuilderState.IDLE_HOME && assignedBuilding == null) {
            assignToBuild(target, allBuildings);
        } else {
            buildQueue.add(target);
        }
    }

    public void update() {
        switch (state) {
            case MOVING_TO_SITE:
                moveAlongPath(() -> state = BuilderState.BUILDING);
                break;

            case BUILDING:
                if (assignedBuilding == null || assignedBuilding.isBuilt || assignedBuilding.isDemolishing) {
                    goToNextJobOrHome();
                    return;
                }
                assignedBuilding.buildProgress += 1.0f;
                if (assignedBuilding.buildProgress >= assignedBuilding.maxBuild) {
                    assignedBuilding.isBuilt = true;
                    assignedBuilding.assignedBuilder = null;
                    assignedBuilding = null;
                    goToNextJobOrHome();
                }
                break;

            case MOVING_TO_TREE:
                moveAlongPath(() -> state = BuilderState.CHOPPING);
                break;

            case CHOPPING:
                if (assignedTree == null) {
                    goToNextJobOrHome();
                    return;
                }
                assignedTree.harvestProgress += 1.0f;
                if (assignedTree.harvestProgress >= assignedTree.maxHarvest) {
                    assignedTree = null; // Progress udah penuh; GamePanel yang hapus pohonnya dari mapPohon
                    goToNextJobOrHome();
                }
                break;

            case RETURNING_HOME:
                moveAlongPath(() -> {
                    if (!buildQueue.isEmpty()) {
                        assignToBuild(buildQueue.remove(0), buildingsRef);
                    } else if (!chopQueue.isEmpty()) {
                        assignToChop(chopQueue.remove(0), buildingsRef);
                    } else {
                        state = BuilderState.IDLE_HOME;
                    }
                });
                break;

            case IDLE_HOME:
            default:
                break;
        }
    }

    private void startReturningHome() {
        // --- FIX: Sama seperti assignToBuild, jangan anggap bangunan yang lagi ditempati
        // builder ini (misal baru selesai bangun FARM & masih berdiri di atasnya) sebagai tembok ---
        List<Building> obstacles = buildSafeObstacleList(buildingsRef, null);
        setPath(PathFinder.findPath(x, y, homeX, homeY, obstacles));
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