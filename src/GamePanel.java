import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.awt.geom.Rectangle2D;


public class GamePanel extends JPanel {
    private GameWindow window; // Referensi ke Bos
    private BufferedImage gameplayBg;
    private BufferedImage smallHouseImg;
    private BufferedImage mediumHouseImg;
    private BufferedImage bigHouseImg;
    private BufferedImage archerImg;
    private BufferedImage spearmanImg;
    private BufferedImage axemanImg;
    private BufferedImage shieldImg;
    private BufferedImage bowmanHordeImg;
    private BufferedImage civilImg;
    private BufferedImage wallLeftImg;
    private BufferedImage wallRightImg;
    private BufferedImage wallUDImg;
    private BufferedImage abandonedMineImg;
    private BufferedImage builtMineImg;
    private BufferedImage farmImg;
    private BufferedImage storageImg;
    private BufferedImage barrackImg;

    // --- ADT Kamera ---
    private Camera camera = new Camera();
    private Timer cameraTimer;

    // --- Variabel Input ---
    private boolean wPressed = false, sPressed = false, aPressed = false, dPressed = false;

    // --- Variabel Alat & Bangunan ---
    private enum ToolMode { NONE, BUILD, MOVE, DELETE, COMMAND, CHOP_WOOD }
    private ToolMode currentTool = ToolMode.NONE;
    private int mouseX = -100, mouseY = -100;
    private Point dragStartScreen = null;
    private Point dragEndScreen = null;
    private boolean isDragging = false;
    private Building holdingBuilding = null;
    private boolean isDraggingWall = false;
    private Point dragStartPoint = new Point();
    private Point dragCurrentPoint = new Point();
    public java.util.List<Mine> activeMines = new java.util.ArrayList<>();
    private Mine clickedMine = null;


    // --- Variabel UI/HUD ---
    private JPanel bottomLeftBar;
    private JPanel topRightBar;
    private JButton menuBtn;


    private boolean isBuildMenuExpanded = false;
    private JButton house1Btn, house2Btn, house3Btn;

    // --- VARIABEL GRID MENU & ALAT ---
    private enum MenuState { CLOSED, MAIN_MENU, CIVIL_MENU, MILITARY_MENU, BUILDING_SELECTED, MINE_SELECTED }    private MenuState currentMenuState = MenuState.MAIN_MENU;
    private Building.BuildingType selectedBuilding = Building.BuildingType.MEDIUM_HOUSE;
    private JPanel gridMenuPanel;
    private Building clickedBuilding = null;

    // Kita ubah jadi Width agar dia meluncur/mengembang ke kanan
    private double currentSubWidth = 0.0;
    private int targetSubWidth = 0;
    private String activeCategory = ""; // Untuk melacak kategori apa yang sedang dibuka


    // Wadah tombol bangunan (Placeholder siap pakai)
    private JButton houseBtn, farmBtn, fishBtn;
    private JButton wallBtn, barrackBtn, towerBtn;

    private boolean isDayTime = true;
    private int dayNightTick = 0;
    private float currentDarkness = 0f;
    private final float MAX_DARKNESS = 0.65f;
    private int currentDay = 1;
    public int totalWood = 0;

    //===============================TREE====================================================
    // Hashtable (Spatial Hash) untuk menyimpan pohon berdasarkan petak Kavling
    public java.util.HashMap<String, java.util.List<Tree>> mapPohon = new java.util.HashMap<>();
    // Variabel untuk menyimpan gambar pohon
    private BufferedImage treeImg;
    //===================================================================================

    public GamePanel(GameWindow window) {
        this.window = window;
        setLayout(null);

        // Tumbuhkan pohon di seluruh dunia!
        generateForest();

        // Panggil fungsi pembuat tambang
        generateMines();

        try {
            abandonedMineImg = ImageIO.read(new File("assets/img/abandoned_mine.png"));
            builtMineImg = ImageIO.read(new File("assets/img/mine.png"));
        } catch (Exception e) {
            System.out.println("Gagal memuat gambar Tambang!");
        }

        // 1. Muat Gambar (Menggunakan path src/ dan dipisah agar aman)
        try {
            gameplayBg = ImageIO.read(new File("assets/img/GAMEBACKGROUND.png"));
            treeImg = ImageIO.read(new File("assets/img/tree.png"));
        } catch (Exception e) {}

        try {
            // Perbaikan: Tambahkan "assets/" dan sesuaikan nama file sm_house.png
            smallHouseImg = ImageIO.read(new File("assets/img/smhouse.png"));
            mediumHouseImg = ImageIO.read(new File("assets/img/mhouse.png"));
            bigHouseImg = ImageIO.read(new File("assets/img/bighouse.png"));
            farmImg = ImageIO.read(new File("assets/img/farm.png"));
            storageImg = ImageIO.read(new File("assets/img/storage.png"));
            barrackImg = ImageIO.read(new File("assets/img/barrack.png"));
        } catch (Exception e) {
            System.out.println("Gagal memuat Gambar Rumah!");
        }

        try {
            archerImg = ImageIO.read(new File("assets/img/Hearthguardbow.png"));
            spearmanImg = ImageIO.read(new File("assets/img/hearthguard_spier.png"));
        } catch (Exception e) {
            System.out.println("Gagal memuat Pasukan Guard!");
        }

        try {
            axemanImg = ImageIO.read(new File("assets/img/1Horde.png"));
            shieldImg = ImageIO.read(new File("assets/img/2Horde.png"));
            bowmanHordeImg = ImageIO.read(new File("assets/img/3Horde.png"));
        } catch (Exception e) {
            System.out.println("Gagal memuat Pasukan Horde!");
        }

        try {
            civilImg = ImageIO.read(new File("assets/img/civil_h&h.png"));
        } catch (Exception e) {
            System.out.println("Gagal memuat Civil!");
        }

        try {
            wallLeftImg = ImageIO.read(new File("assets/img/woodenWall_L.png"));
            wallRightImg = ImageIO.read(new File("assets/img/woodenWall_R.png"));
            wallUDImg = ImageIO.read(new File("assets/img/WoodenWall_UD.png"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        cameraTimer = new Timer(16, e -> {
            boolean moved = camera.move(wPressed, sPressed, aPressed, dPressed);

            // 1. Update Guards (Parameter Nambah)
            for (Guard g : window.activeGuards) {
                g.update(window.activeGuards, window.activeHordes, window.activeProjectiles);
            }

            // 2. Update Hordes (Parameter Nambah)
            for (Horde h : window.activeHordes) {
                h.update(window.activeHordes, window.activeGuards, window.activeProjectiles);
            }

            // --- UPDATE CIVIL (Jalan-jalan santai) ---
            for (Civil c : window.activeCivils) {
                c.update();
            }

            // =======================================================
            // --- LOGIKA FISIKA: ANTI NEMBUS BANGUNAN (SOLID WALL) ---
            // =======================================================
            for (Building b : window.savedBuildings) {
                Rectangle wall = b.getSolidHitbox();

                // 1. Cek Tabrakan Guard
                for (Guard g : window.activeGuards) {
                    // Kotak kaki Guard (hanya setengah badan ke bawah)
                    Rectangle foot = new Rectangle((int)g.x, (int)g.y + (g.size/2), g.size, g.size/2);
                    if (wall.intersects(foot)) {
                        // Geser paksa keluar (Efek meluncur di tembok)
                        if (g.x + g.size/2 < wall.x + wall.width/2) g.x -= g.speed; else g.x += g.speed;
                        if (g.y + g.size/2 < wall.y + wall.height/2) g.y -= g.speed; else g.y += g.speed;
                    }
                }



                // 2. Cek Tabrakan Horde
                for (Horde h : window.activeHordes) {
                    Rectangle foot = new Rectangle((int)h.x, (int)h.y + (h.size/2), h.size, h.size/2);
                    if (wall.intersects(foot)) {
                        if (h.x + h.size/2 < wall.x + wall.width/2) h.x -= h.speed; else h.x += h.speed;
                        if (h.y + h.size/2 < wall.y + wall.height/2) h.y -= h.speed; else h.y += h.speed;
                    }
                }

                // 3. Cek Tabrakan Civil
                for (Civil c : window.activeCivils) {
                    Rectangle foot = new Rectangle((int)c.x, (int)c.y + (c.size/2), c.size, c.size/2);
                    if (wall.intersects(foot)) {
                        if (c.x + c.size/2 < wall.x + wall.width/2) c.x -= c.speed; else c.x += c.speed;
                        if (c.y + c.size/2 < wall.y + wall.height/2) c.y -= c.speed; else c.y += c.speed;
                    }
                }
            }

            // --- 3. TAMBAHKAN LOOP PROYEKTIL INI ---
            for (Projectile p : window.activeProjectiles) {
                p.update();

                // --- LOGIKA COLLISION / HITBOX SAAT DI UDARA ---
                // (Agar efisien, kita cek di sini satu-satu list objek yang ada)
                if (p.active && !p.hasLanded()) {
                    Rectangle2D.Double pHitbox = p.getHitbox();

                    if (p.isFromPlayer()) {
                        // Cek kena Horde
                        for (Horde enemy : window.activeHordes) {
                            // Hitbox Horde kita buat sedikit besar biar gampang kena
                            if (new Rectangle2D.Double(enemy.x, enemy.y, enemy.size, enemy.size).intersects(pHitbox)) {
                                enemy.currentHp -= p.getDamage();
                                p.active = false; // Panah hilang karena menancap di badan
                                break; // Selesai cek list Horde untuk panah ini
                            }
                        }
                    } else {
                        // Cek kena Guard
                        for (Guard guard : window.activeGuards) {
                            if (new Rectangle2D.Double(guard.x, guard.y, guard.size, guard.size).intersects(pHitbox)) {
                                guard.currentHp -= p.getDamage();
                                p.active = false; // Panah hilang menancap di badan
                                break;
                            }
                        }
                    }
                }
            }

            // 4. Bersihkan data (Mayat & Panah non-aktif)
            window.activeGuards.removeIf(g -> g.currentHp <= 0);
            window.activeHordes.removeIf(h -> h.currentHp <= 0);

            // --- LOGIKA PROGRESS TEBANG POHON ---
            for (java.util.List<Tree> daftarPohon : mapPohon.values()) {
                // Kita pakai Iterator agar aman menghapus pohon dari daftar saat sedang di-loop
                java.util.Iterator<Tree> it = daftarPohon.iterator();
                while (it.hasNext()) {
                    Tree pohon = it.next();
                    if (pohon.isHarvesting) {
                        pohon.harvestProgress += 1.0f; // Kecepatan loading (bisa dikecilin kalau ketuaan)

                        if (pohon.harvestProgress >= pohon.maxHarvest) {
                            it.remove(); // Hapus pohon dari map
                            totalWood += 5; // Yey! Kayu bertambah +5
                        }
                    }
                }
            }

            // --- LOGIKA PROGRESS MENGHANCURKAN BANGUNAN ---
            for (Building b : window.savedBuildings) {
                if (b.isDemolishing) b.demolishProgress += 1.0f;
            }
            // Bersihkan jika sudah hancur total
            window.savedBuildings.removeIf(b -> b.isDemolishing && b.demolishProgress >= b.maxDemolish);

            // --- LOGIKA PROGRESS PEMBANGUNAN TAMBANG ---
            for (Mine m : activeMines) {
                if (m.isBuilding && !m.isBuilt) {
                    m.buildProgress += 1.0f; // Kecepatan bangun

                    if (m.buildProgress >= m.maxBuild) {
                        m.isBuilt = true;      // Ganti wujud ke mine.png!
                        m.isBuilding = false;  // Matikan loading bar
                    }
                }
            }

            // --- TAMBAHKAN PEMBERSIH PANAH NON-AKTIF ---
            window.activeProjectiles.removeIf(p -> !p.active);

            // --- 5. LOGIKA SIKLUS SIANG & MALAM ---
            dayNightTick++;

            // Anggap 1 detik = 60 frame (karena timer 16ms)
            // Siang berlangsung selama 60 detik (3600 tick)
            if (isDayTime && dayNightTick > 3600) {
                isDayTime = false;
                dayNightTick = 0;
                // Malam berlangsung selama 30 detik (1800 tick)
            } else if (!isDayTime && dayNightTick > 1800) {
                isDayTime = true;
                dayNightTick = 0;

                currentDay++;
            }

            // Transisi pergantian langit super halus perlahan-lahan
            float targetDarkness = isDayTime ? 0.0f : MAX_DARKNESS;
            currentDarkness += (targetDarkness - currentDarkness) * 0.005f;








            repaint(); // (Ini bawaan aslinya, pastikan ini tetap di posisi paling bawah)

            repaint();
        });
        cameraTimer.start();

        // 3. Setup Kontrol Keyboard & Mouse
        setupKeyBindings();
        setupMouseListeners();

        // 4. Setup HUD Layar
        setupHUD();

        // 5. Jaga posisi HUD saat layar berubah ukuran
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int w = getWidth();
                int h = getHeight();
                if(topRightBar != null && bottomLeftBar != null) {
                    // --- FIX: LEBAR DIPERSEMPIT JADI 830 AGAR PAS DENGAN LOGO DAN RAPIH ---
                    topRightBar.setBounds(w - 830, 0, 830, 40);
                    menuBtn.setBounds(topRightBar.getWidth() - 40, 4, 32, 32);

                    // Posisi Wadah Tombol dipindah ke ATAS Minimap (tinggi wadah 80px)
                    bottomLeftBar.setBounds(15, h - 310, 180, 80);

                    // Grid Menu tetap di sebelah minimap
                    if (gridMenuPanel != null) gridMenuPanel.setBounds(230, h - 200, 240, 180);
                }
            }
        });
    }

    private void setupKeyBindings() {
        // Logika Pan/Geser (WASD - Anti Hilang Fokus)
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_W, 0, false), "wPress");
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_W, 0, true), "wRelease");
        getActionMap().put("wPress", new AbstractAction() { public void actionPerformed(ActionEvent e) { wPressed = true; }});
        getActionMap().put("wRelease", new AbstractAction() { public void actionPerformed(ActionEvent e) { wPressed = false; }});

        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_S, 0, false), "sPress");
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_S, 0, true), "sRelease");
        getActionMap().put("sPress", new AbstractAction() { public void actionPerformed(ActionEvent e) { sPressed = true; }});
        getActionMap().put("sRelease", new AbstractAction() { public void actionPerformed(ActionEvent e) { sPressed = false; }});

        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_A, 0, false), "aPress");
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_A, 0, true), "aRelease");
        getActionMap().put("aPress", new AbstractAction() { public void actionPerformed(ActionEvent e) { aPressed = true; }});
        getActionMap().put("aRelease", new AbstractAction() { public void actionPerformed(ActionEvent e) { aPressed = false; }});

        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0, false), "dPress");
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0, true), "dRelease");
        getActionMap().put("dPress", new AbstractAction() { public void actionPerformed(ActionEvent e) { dPressed = true; }});
        getActionMap().put("dRelease", new AbstractAction() { public void actionPerformed(ActionEvent e) { dPressed = false; }});

        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_B, 0), "toolBuild");
        getActionMap().put("toolBuild", new AbstractAction() { public void actionPerformed(ActionEvent e) { currentTool = ToolMode.BUILD; holdingBuilding = null; repaint(); }});

        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_M, 0), "toolMove");
        getActionMap().put("toolMove", new AbstractAction() { public void actionPerformed(ActionEvent e) { currentTool = ToolMode.MOVE; repaint(); }});

        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_X, 0), "toolDelete");
        getActionMap().put("toolDelete", new AbstractAction() { public void actionPerformed(ActionEvent e) { currentTool = ToolMode.DELETE; holdingBuilding = null; repaint(); }});

        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_K, 0), "cheatSpawnSpearman");
        getActionMap().put("cheatSpawnSpearman", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                window.activeGuards.add(new Guard(Guard.GuardType.SPEARMAN, mouseX, mouseY)); repaint();
            }
        });

        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_L, 0), "cheatSpawnArcher");
        getActionMap().put("cheatSpawnArcher", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                window.activeGuards.add(new Guard(Guard.GuardType.ARCHER, mouseX, mouseY)); repaint();
            }
        });

        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_7, 0), "spawnAxeman");
        getActionMap().put("spawnAxeman", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                window.activeHordes.add(new Horde(Horde.HordeType.AXEMAN, mouseX, mouseY)); repaint();
            }
        });

        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_8, 0), "spawnShield");
        getActionMap().put("spawnShield", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                window.activeHordes.add(new Horde(Horde.HordeType.SHIELDBEARER, mouseX, mouseY)); repaint();
            }
        });

        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_9, 0), "spawnBowmanHorde");
        getActionMap().put("spawnBowmanHorde", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                window.activeHordes.add(new Horde(Horde.HordeType.BOWMAN, mouseX, mouseY)); repaint();
            }
        });

        // --- CHEAT SPAWN CIVIL (Tekan P) ---
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_P, 0), "spawnCivil");
        getActionMap().put("spawnCivil", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                window.activeCivils.add(new Civil(mouseX, mouseY));
                repaint();
            }
        });

    }

    private void setupMouseListeners() {
        // Zoom Kamera
        addMouseWheelListener(e -> {
            if (e.isControlDown()) {
                camera.zoomInOut(e.getWheelRotation(), getWidth(), getHeight());
                repaint();
            }
        });

        // Pergerakan Kursor (Preview Bangunan & Fitur Drag-Select)
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                Point worldPos = camera.toWorld(e.getX(), e.getY());
                mouseX = worldPos.x;
                mouseY = worldPos.y;
                if (currentTool != ToolMode.NONE) repaint();
            }

            // --- TAMBAHAN BARU UNTUK NGE-BLOK ---
            @Override
            public void mouseDragged(MouseEvent e) {
                if (currentTool == ToolMode.COMMAND && SwingUtilities.isLeftMouseButton(e)) {
                    isDragging = true;
                    dragEndScreen = e.getPoint();
                    repaint();
                }
                if (isDraggingWall) {
                    Point worldPos = camera.toWorld(e.getX(), e.getY());
                    int bw = 10;
                    int rawTargetX = worldPos.x - (bw / 2);
                    int rawTargetY = worldPos.y - (bw / 2);
                    dragCurrentPoint.x = Math.round((float) rawTargetX / 10f) * 10;
                    dragCurrentPoint.y = Math.round((float) rawTargetY / 10f) * 10;
                    repaint();
                }
            }
        });

        // Logika Klik (Bangun, Pindah, Hapus)
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // Cegah nembus HUD
                if (bottomLeftBar != null && topRightBar != null) {
                    if (bottomLeftBar.getBounds().contains(e.getPoint()) || topRightBar.getBounds().contains(e.getPoint())) return;
                }



                // Klik Kanan (Batal / Perintah Jalan)
                if (SwingUtilities.isRightMouseButton(e)) {
                    // Jika di mode Command, klik kanan berarti menyuruh Guard jalan
                    if (currentTool == ToolMode.COMMAND) {
                        Point worldPos = camera.toWorld(e.getX(), e.getY());

                        int selectedCount = 0;
                        for (Guard g : window.activeGuards) {
                            if (g.isSelected) selectedCount++;
                        }

                        if (selectedCount > 0) {
                            int cols = (int) Math.ceil(Math.sqrt(selectedCount));
                            int row = 0, col = 0;
                            int spacing = 32;

                            int startOffsetX = -((cols - 1) * spacing) / 2;
                            int startOffsetY = -((cols - 1) * spacing) / 2;

                            for (Guard g : window.activeGuards) {
                                if (g.isSelected) {
                                    double finalTargetX = worldPos.x + startOffsetX + (col * spacing) - (g.size / 2.0);
                                    double finalTargetY = worldPos.y + startOffsetY + (row * spacing) - (g.size / 2.0);

                                    java.util.List<Point> calculatedPath = PathFinder.findPath(
                                            g.x + (g.size/2.0), g.y + (g.size/2.0),
                                            finalTargetX + (g.size/2.0), finalTargetY + (g.size/2.0),
                                            window.savedBuildings
                                    );

                                    if (calculatedPath.isEmpty()) {
                                        finalTargetX = worldPos.x - (g.size / 2.0);
                                        finalTargetY = worldPos.y - (g.size / 2.0);
                                        calculatedPath = PathFinder.findPath(
                                                g.x + (g.size/2.0), g.y + (g.size/2.0),
                                                finalTargetX + (g.size/2.0), finalTargetY + (g.size/2.0),
                                                window.savedBuildings
                                        );
                                    }

                                    g.setPath(calculatedPath);
                                    g.targetX = finalTargetX;
                                    g.targetY = finalTargetY;
                                    g.state = Guard.GuardState.MOVING;

                                    col++;
                                    if (col >= cols) {
                                        col = 0; row++;
                                    }
                                }
                            }
                        }
                    } else if (holdingBuilding != null) {
                        window.savedBuildings.add(holdingBuilding);
                        holdingBuilding = null;
                        currentTool = ToolMode.NONE;
                    } else {
                        currentTool = ToolMode.NONE;
                    }
                    repaint();
                    return;
                }

                if (currentTool == ToolMode.COMMAND) {
                    dragStartScreen = e.getPoint();
                    dragEndScreen = e.getPoint();
                    isDragging = true;
                    return; // Berhenti di sini, tidak perlu jalankan kode taruh rumah di bawahnya
                }

                if (!SwingUtilities.isLeftMouseButton(e)) return;

                Point worldPos = camera.toWorld(e.getX(), e.getY());


                // --- LOGIKA KLIK BANGUNAN & TAMBANG UNTUK MENU DINAMIS ---
                if (currentTool == ToolMode.NONE) {
                    boolean hitSomething = false;

                    // 1. Cek Bangunan (Rumah) Terlebih Dahulu
                    for (int i = window.savedBuildings.size() - 1; i >= 0; i--) {
                        Building b = window.savedBuildings.get(i);
                        if (b.contains(worldPos) && !b.isDemolishing) {
                            clickedBuilding = b;
                            clickedMine = null; // Reset status tambang
                            currentMenuState = MenuState.BUILDING_SELECTED;
                            updateGridMenu();
                            hitSomething = true;
                            break;
                        }
                    }

                    // 2. Jika tidak kena rumah, cek Tambang
                    if (!hitSomething) {
                        for (Mine m : activeMines) {
                            if (m.getBounds().contains(worldPos)) {
                                clickedMine = m;
                                clickedBuilding = null; // Reset status rumah
                                currentMenuState = MenuState.MINE_SELECTED;
                                updateGridMenu();
                                hitSomething = true;
                                break;
                            }
                        }
                    }

                    // 3. Kalau klik tanah kosong
                    if (!hitSomething) {
                        clickedBuilding = null;
                        clickedMine = null;
                        currentMenuState = MenuState.MAIN_MENU;
                        updateGridMenu();
                    }
                    repaint();
                    return;
                }

                int bw = getBuildWidth(selectedBuilding);



                int rawTargetX = worldPos.x - (bw / 2);
                int rawTargetY = worldPos.y - (bw / 2);

                int targetX = Math.round((float) rawTargetX / 10f) * 10;
                int targetY = Math.round((float) rawTargetY / 10f) * 10;



                Rectangle newArea = new Rectangle(targetX, targetY, bw, bw);

                // --- LOGIKA TEBANG POHON ---
                if (currentTool == ToolMode.CHOP_WOOD) {
                    for (java.util.List<Tree> daftarPohon : mapPohon.values()) {
                        for (Tree pohon : daftarPohon) {
                            if (pohon.getBounds().contains(worldPos)) {
                                pohon.isHarvesting = true; // Pohon mulai ditebang!
                            }
                        }
                    }
                }

                if (currentTool == ToolMode.BUILD) {
                    boolean isWall = (selectedBuilding == Building.BuildingType.WALL_L ||
                            selectedBuilding == Building.BuildingType.WALL_R ||
                            selectedBuilding == Building.BuildingType.WALL_UD);

                    if (isWall) {
                        isDraggingWall = true;
                        dragStartPoint.x = targetX;
                        dragStartPoint.y = targetY;
                        dragCurrentPoint.x = targetX;
                        dragCurrentPoint.y = targetY;

                    } else if (!isOverlapping(newArea, null)) {
                        int cap = getBuildCapacity(selectedBuilding);
                        window.savedBuildings.add(new Building(targetX, targetY, bw, bw, selectedBuilding, cap));

                        // Spawn Warga sesuai kapasitas!
                        double spawnPointX = targetX + (bw / 2.0);
                        double spawnPointY = targetY + (bw / 2.0);
                        for(int i = 0; i < cap; i++) {
                            window.activeCivils.add(new Civil(spawnPointX, spawnPointY));
                        }
                    }
                }
                else if (currentTool == ToolMode.MOVE) {
                    if (holdingBuilding == null) {
                        for (int i = window.savedBuildings.size() - 1; i >= 0; i--) {
                            if (window.savedBuildings.get(i).contains(worldPos)) {
                                holdingBuilding = window.savedBuildings.remove(i);
                                break;
                            }
                        }
                    } else {
                        if (!isOverlapping(newArea, holdingBuilding)) {
                            // Tinggal geser saja lokasinya, tidak perlu buat objek baru!
                            holdingBuilding.getBounds().setLocation(targetX, targetY);
                            window.savedBuildings.add(holdingBuilding);
                            holdingBuilding = null;
                        }
                    }
                }
                else if (currentTool == ToolMode.DELETE) {
                    for (int i = window.savedBuildings.size() - 1; i >= 0; i--) {
                        if (window.savedBuildings.get(i).contains(worldPos)) {
                            window.savedBuildings.remove(i);
                            break;
                        }
                    }
                }
                repaint();
            }

            // --- FUNGSI BARU: SAAT KLIK KIRI DILEPAS SETELAH NGE-BLOK ---
            @Override
            public void mouseReleased(MouseEvent e) {
                if (currentTool == ToolMode.COMMAND && SwingUtilities.isLeftMouseButton(e) && isDragging) {
                    isDragging = false;

                    if (dragStartScreen != null && dragEndScreen != null) {
                        // Ubah kotak di layar menjadi koordinat dunia (world coordinate)
                        Point worldStart = camera.toWorld(dragStartScreen.x, dragStartScreen.y);
                        Point worldEnd = camera.toWorld(dragEndScreen.x, dragEndScreen.y);

                        int wx = Math.min(worldStart.x, worldEnd.x);
                        int wy = Math.min(worldStart.y, worldEnd.y);
                        int wWidth = Math.abs(worldStart.x - worldEnd.x);
                        int wHeight = Math.abs(worldStart.y - worldEnd.y);

                        Rectangle worldSelectRect = new Rectangle(wx, wy, wWidth, wHeight);

                        // Cek Guard mana yang masuk jaring blok kita
                        for (Guard g : window.activeGuards) {
                            Rectangle guardRect = new Rectangle((int)g.x, (int)g.y, g.size, g.size);
                            if (worldSelectRect.intersects(guardRect) || worldSelectRect.contains(guardRect)) {
                                g.isSelected = true;
                            } else {
                                g.isSelected = false; // Batal terpilih jika di luar kotak
                            }
                        }
                    }
                    repaint();
                }

                if (currentTool == ToolMode.BUILD && isDraggingWall) {
                    isDraggingWall = false;
                    int diffX = Math.abs(dragCurrentPoint.x - dragStartPoint.x);
                    int diffY = Math.abs(dragCurrentPoint.y - dragStartPoint.y);

                    if (diffX > diffY) {
                        // HORIZONTAL
                        int startX = Math.min(dragStartPoint.x, dragCurrentPoint.x);
                        int endX = Math.max(dragStartPoint.x, dragCurrentPoint.x);
                        int y = dragStartPoint.y;
                        for (int x = startX; x <= endX; x += 10) {
                            // Ujung kiri = WALL_L, sisanya = WALL_R (Sesuai idemu!)
                            Building.BuildingType type = (x == startX) ? Building.BuildingType.WALL_L : Building.BuildingType.WALL_R;
                            if (!isOverlapping(new Rectangle(x, y, 10, 10), null)) {
                                window.savedBuildings.add(new Building(x, y, 10, 10, type, 0));
                            }
                        }
                    } else {
                        // VERTIKAL
                        int startY = Math.min(dragStartPoint.y, dragCurrentPoint.y);
                        int endY = Math.max(dragStartPoint.y, dragCurrentPoint.y);
                        int x = dragStartPoint.x;
                        for (int y = startY; y <= endY; y += 10) {
                            if (!isOverlapping(new Rectangle(x, y, 10, 10), null)) {
                                window.savedBuildings.add(new Building(x, y, 10, 10, Building.BuildingType.WALL_UD, 0));
                            }
                        }
                    }
                    repaint();
                }
            }
        });
    }

    private boolean isOverlapping(Rectangle newRect, Building ignoreBuilding) {
        for (Building b : window.savedBuildings) {
            if (b != ignoreBuilding && b.intersects(newRect)) return true;
        }
        return false;
    }

    // --- HELPER UNTUK MENGAMBIL DATA DINAMIS BANGUNAN ---
    private int getBuildWidth(Building.BuildingType type) {
        if (type == Building.BuildingType.WALL_L || type == Building.BuildingType.WALL_R || type == Building.BuildingType.WALL_UD) return 10;
        if (type == Building.BuildingType.SMALL_HOUSE) return 70;
        if (type == Building.BuildingType.BIG_HOUSE) return 90;
        return 70;
    }

    private int getBuildCapacity(Building.BuildingType type) {
        if (type == Building.BuildingType.WALL_L || type == Building.BuildingType.WALL_R || type == Building.BuildingType.WALL_UD) return 0; // Tembok tidak menampung Civil
        if (type == Building.BuildingType.SMALL_HOUSE) return 2;
        if (type == Building.BuildingType.BIG_HOUSE) return 8;
        return 4;
    }

    private BufferedImage getBuildImage(Building.BuildingType type) {
        if (type == Building.BuildingType.WALL_L) return wallLeftImg;
        if (type == Building.BuildingType.WALL_R) return wallRightImg;
        if (type == Building.BuildingType.WALL_UD) return wallUDImg;
        if (type == Building.BuildingType.SMALL_HOUSE) return smallHouseImg;
        if (type == Building.BuildingType.BIG_HOUSE) return bigHouseImg;
        if (type == Building.BuildingType.FARM) return farmImg;
        if (type == Building.BuildingType.STORAGE) return storageImg;
        if (type == Building.BuildingType.BARRACK) return barrackImg;
        return mediumHouseImg;
    }

    // --- MESIN STATE UNTUK GRID (MENGGANTI ISI PAPAN SECARA INSTAN) ---
    private void updateGridMenu() {
        gridMenuPanel.removeAll();

        if (currentMenuState == MenuState.MAIN_MENU) {
            gridMenuPanel.add(createGridBtn("👨‍🌾", () -> { currentMenuState = MenuState.CIVIL_MENU; updateGridMenu(); }, false));
            gridMenuPanel.add(createGridBtn("⚔️", () -> { currentMenuState = MenuState.MILITARY_MENU; updateGridMenu(); }, false));

            for(int i=0; i<10; i++) gridMenuPanel.add(createGridBtn("", null, false));
        }
        else if (currentMenuState == MenuState.CIVIL_MENU) {
            boolean isSmall = (selectedBuilding == Building.BuildingType.SMALL_HOUSE);
            boolean isMed = (selectedBuilding == Building.BuildingType.MEDIUM_HOUSE);
            boolean isBig = (selectedBuilding == Building.BuildingType.BIG_HOUSE);
            boolean isWall = (selectedBuilding == Building.BuildingType.WALL_L);
            boolean isChop = (currentTool == ToolMode.CHOP_WOOD);
            boolean isFarm = (selectedBuilding == Building.BuildingType.FARM);
            boolean isStorage = (selectedBuilding == Building.BuildingType.STORAGE);


            // Hanya Emoji Tanpa Teks
            gridMenuPanel.add(createGridBtn("🏠", () -> { selectedBuilding = Building.BuildingType.SMALL_HOUSE; currentTool = ToolMode.BUILD; updateGridMenu(); repaint(); }, isSmall));
            gridMenuPanel.add(createGridBtn("🏘️", () -> { selectedBuilding = Building.BuildingType.MEDIUM_HOUSE; currentTool = ToolMode.BUILD; updateGridMenu(); repaint(); }, isMed));
            gridMenuPanel.add(createGridBtn("🏰", () -> { selectedBuilding = Building.BuildingType.BIG_HOUSE; currentTool = ToolMode.BUILD; updateGridMenu(); repaint(); }, isBig));
            gridMenuPanel.add(createGridBtn("🧱", () -> {
                selectedBuilding = Building.BuildingType.WALL_L; // Jadikan L sebagai default/pancingan awal
                currentTool = ToolMode.BUILD;
                updateGridMenu();
                repaint();
            }, isWall));
            gridMenuPanel.add(createGridBtn("🪓", () -> {
                currentTool = ToolMode.CHOP_WOOD;
                updateGridMenu();
                repaint();
            }, isChop));
            gridMenuPanel.add(createGridBtn("🌾", () -> { selectedBuilding = Building.BuildingType.FARM; currentTool = ToolMode.BUILD; updateGridMenu(); repaint(); }, isFarm));
            gridMenuPanel.add(createGridBtn("📦", () -> { selectedBuilding = Building.BuildingType.STORAGE; currentTool = ToolMode.BUILD; updateGridMenu(); repaint(); }, isStorage));

            for(int i=0; i<7; i++) gridMenuPanel.add(createGridBtn("", null, false));
            gridMenuPanel.add(createGridBtn("⬅️", () -> { currentMenuState = MenuState.MAIN_MENU; updateGridMenu(); }, false));
        }
        else if (currentMenuState == MenuState.MILITARY_MENU) {
            boolean isBarrack = (selectedBuilding == Building.BuildingType.BARRACK);

            // Tambahkan tombol Barrack
            gridMenuPanel.add(createGridBtn("⚔️🏠", () -> {
                selectedBuilding = Building.BuildingType.BARRACK;
                currentTool = ToolMode.BUILD;
                updateGridMenu();
                repaint();
            }, isBarrack));

            // Isi sisa 10 kotak kosong dan 1 tombol kembali
            for(int i=0; i<10; i++) gridMenuPanel.add(createGridBtn("", null, false));
            gridMenuPanel.add(createGridBtn("⬅️", () -> { currentMenuState = MenuState.MAIN_MENU; updateGridMenu(); }, false));
        }


        else if (currentMenuState == MenuState.BUILDING_SELECTED) {
            // 1. Tombol UPGRADE (Sementara cuma print log)
            gridMenuPanel.add(createGridBtn("⬆️", () -> {
                System.out.println("Fitur Upgrade akan segera hadir!");
            }, false));

            // 2. Tombol MOVE (Angkat bangunan)
            gridMenuPanel.add(createGridBtn("✋", () -> {
                if (clickedBuilding != null) {
                    holdingBuilding = clickedBuilding;
                    window.savedBuildings.remove(clickedBuilding); // Cabut dari tanah
                    clickedBuilding = null; // Lepaskan pilihan
                    currentTool = ToolMode.MOVE;
                    currentMenuState = MenuState.MAIN_MENU; // Kembalikan menu
                    updateGridMenu();
                    repaint();
                }
            }, false));

            // 3. Tombol DESTROY (Mulai Loading Hancur)
            gridMenuPanel.add(createGridBtn("❌", () -> {
                if (clickedBuilding != null) {
                    clickedBuilding.isDemolishing = true; // Picu loading bar merah
                    clickedBuilding = null;
                    currentMenuState = MenuState.MAIN_MENU;
                    updateGridMenu();
                }
            }, false));

            // Isi 8 kotak kosong dan 1 tombol kembali
            for(int i=0; i<8; i++) gridMenuPanel.add(createGridBtn("", null, false));
            gridMenuPanel.add(createGridBtn("⬅️", () -> {
                clickedBuilding = null; currentMenuState = MenuState.MAIN_MENU; updateGridMenu(); repaint();
            }, false));
        }

        else if (currentMenuState == MenuState.MINE_SELECTED) {
            // Jika tambang belum dibangun dan belum dalam proses loading
            if (clickedMine != null && !clickedMine.isBuilt && !clickedMine.isBuilding) {
                // Tombol Palu untuk Membangun Tambang
                gridMenuPanel.add(createGridBtn("🔨", () -> {
                    if (clickedMine != null) {
                        clickedMine.isBuilding = true; // Mulai loading!
                        clickedMine = null; // Tutup menu agar pemain fokus lihat loading
                        currentMenuState = MenuState.MAIN_MENU;
                        updateGridMenu();
                        repaint();
                    }
                }, false));

                // Isi sisa grid yang kosong (8 kotak + 1 kembali)
                for(int i=0; i<8; i++) gridMenuPanel.add(createGridBtn("", null, false));
                gridMenuPanel.add(createGridBtn("⬅️", () -> {
                    clickedMine = null; currentMenuState = MenuState.MAIN_MENU; updateGridMenu(); repaint();
                }, false));
            }
            else {
                // Jika tambang SUDAH JADI (mine.png), menu sementara kosong atau bisa diisi fitur nambang ke depannya
                for(int i=0; i<9; i++) gridMenuPanel.add(createGridBtn("", null, false));
                gridMenuPanel.add(createGridBtn("⬅️", () -> {
                    clickedMine = null; currentMenuState = MenuState.MAIN_MENU; updateGridMenu(); repaint();
                }, false));
            }
        }
        gridMenuPanel.revalidate();
        gridMenuPanel.repaint();
    }

    // --- TOMBOL GRID DENGAN EFEK MENGGELAP (BUKAN HIJAU) ---
    private JButton createGridBtn(String text, Runnable action, boolean isSelected) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();

                // MENGGELAP SAAT DIPILIH ATAU DI-HOVER
                if (isSelected || (getModel().isRollover() && action != null)) {
                    g2d.setColor(new Color(15, 12, 10));
                } else {
                    g2d.setColor(new Color(40, 35, 30));
                }
                g2d.fillRect(0, 0, getWidth(), getHeight());

                // Border tiap kotak (Perunggu Gelap)
                g2d.setColor(new Color(80, 60, 35));
                g2d.drawRect(0, 0, getWidth(), getHeight());

                g2d.setColor(new Color(210, 190, 160));
                // Cari dan ganti baris font ini di dalam createGridBtn:
                g2d.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 26));
                FontMetrics fm = g2d.getFontMetrics();
                if (!text.isEmpty()) {
                    g2d.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                }
                g2d.dispose();
            }
        };
        btn.setContentAreaFilled(false); btn.setBorderPainted(false); btn.setFocusPainted(false);
        if (action != null) btn.addActionListener(e -> action.run());
        return btn;
    }

    private void setupHUD() {
        // --- HUD KANAN ATAS (THE PIRATE MAP) ---
        topRightBar = new JPanel() {
            private Rectangle[] hitboxes = new Rectangle[11];
            private String[] tooltips = {
                    "Hari Bertahan Hidup",
                    "Wave Serangan Horde",
                    "Total Silver (Kekayaan)",
                    "Total Wood (Kayu)",
                    "Total Stone (Batu)",
                    "Total Steel (Baja)",
                    "Total Food (Makanan)",
                    "Persentase Konsumsi Makanan",
                    "Total Civil (Penduduk)",
                    "Total Pasukan Guard",
                    "Total Bangunan"
            };

            @Override
            public String getToolTipText(MouseEvent e) {
                for (int i = 0; i < 11; i++) {
                    if (hitboxes[i] != null && hitboxes[i].contains(e.getPoint())) return tooltips[i];
                }
                return null;
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                // 1. Base Papan (Kertas Perkamen)
                g2d.setColor(new Color(230, 215, 175, 240));
                g2d.fillRoundRect(0, -15, w, h + 15, 20, 20);

                // 2. Garis Tepi (Solid Tanpa Jahitan)
                g2d.setColor(new Color(140, 90, 50));
                g2d.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2d.drawRoundRect(4, -15, w - 8, h + 11, 20, 20);
                g2d.setStroke(new BasicStroke(1f));
                g2d.drawRoundRect(8, -15, w - 16, h + 7, 20, 20);

                // --- TEKS INDIKATOR (MEPET & DINAMIS) ---
                g2d.setFont(new Font("Segoe UI Emoji", Font.BOLD, 18));
                g2d.setColor(new Color(70, 40, 20));

                int wave = 1;
                int silver = 0;
                int stone = 0;
                int steel = 0;
                int food = 0;
                int konsumsi = 0;
                int totalCivil = window.activeCivils.size();
                int totalGuard = window.activeGuards.size();
                int totalBuilding = window.savedBuildings.size();

                String[] icons = {"📅", "💀", "🪙", "🪵", "🪨", "⚙️", "🍞", "🍖", "👨‍🌾", "⚔️", "🏠"};
                String[] values = {
                        String.valueOf(currentDay),
                        String.valueOf(wave),
                        String.valueOf(silver),
                        String.valueOf(totalWood),
                        String.valueOf(stone),
                        String.valueOf(steel),
                        String.valueOf(food),
                        konsumsi + "%",
                        String.valueOf(totalCivil),
                        String.valueOf(totalGuard),
                        String.valueOf(totalBuilding)
                };

                FontMetrics fm = g2d.getFontMetrics();
                int textY = (h + fm.getAscent() - fm.getDescent()) / 2;
                int startX = 20;

                for (int i = 0; i < 11; i++) {
                    String textToDraw = icons[i] + " " + values[i];
                    g2d.drawString(textToDraw, startX, textY);

                    int textWidth = fm.stringWidth(textToDraw);
                    hitboxes[i] = new Rectangle(startX - 5, 0, textWidth + 10, h);

                    // JARAK DINAMIS: Titik X selanjutnya adalah posisi sekarang + lebar tulisan + spasi 15 pixel (Mepet!)
                    startX += textWidth + 15;
                }

                g2d.dispose();
            }
        };
        topRightBar.setLayout(null);
        topRightBar.setOpaque(false);
        ToolTipManager.sharedInstance().setInitialDelay(100);
        topRightBar.setToolTipText("");

        // Atur agar tooltip cepat muncul tanpa perlu ditunggu lama
        ToolTipManager.sharedInstance().setInitialDelay(100);
        // Wajib mengosongkan string tooltip awal agar pendeteksi mouse di panel ini aktif
        topRightBar.setToolTipText("");

        // --- TOMBOL MENU (TINTA & KERTAS - TITIK 3) ---
        menuBtn = new JButton("⋮") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (getModel().isRollover()) {
                    g2d.setColor(new Color(200, 180, 140)); // Warna kertas menggelap jika di-hover
                } else {
                    g2d.setColor(new Color(225, 210, 170)); // Sama dengan map
                }

                g2d.fillOval(0, 0, getWidth() - 1, getHeight() - 1);

                g2d.setColor(new Color(140, 90, 50)); // Tinta coklat
                g2d.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{4, 4}, 0));
                g2d.drawOval(1, 1, getWidth() - 3, getHeight() - 3);
                g2d.setColor(new Color(70, 40, 20));
                g2d.setFont(new Font("SansSerif", Font.BOLD, 22));
                FontMetrics fm = g2d.getFontMetrics();
                g2d.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);

                g2d.dispose();
            }
        };
        menuBtn.setContentAreaFilled(false); menuBtn.setBorderPainted(false); menuBtn.setFocusPainted(false);
        menuBtn.addActionListener(e -> showInGameMenu());
        topRightBar.add(menuBtn); add(topRightBar);

        bottomLeftBar = new JPanel(); bottomLeftBar.setLayout(null); bottomLeftBar.setOpaque(false);

        int mainSize = 76; int subSize = 36; int gap = 4; int startX2 = mainSize + 8;

        JButton buildBtn = createColorButton(new Color(110, 55, 25), ToolMode.NONE, "🔨");
        buildBtn.setBounds(0, 0, mainSize, mainSize);
        buildBtn.addActionListener(e -> {
            if (currentMenuState == MenuState.CLOSED) {
                currentMenuState = MenuState.MAIN_MENU; gridMenuPanel.setVisible(true); updateGridMenu();
            } else {
                currentMenuState = MenuState.CLOSED; gridMenuPanel.setVisible(false); currentTool = ToolMode.NONE;
            }
            holdingBuilding = null;
        });
        bottomLeftBar.add(buildBtn);

        JButton moveBtn = createColorButton(new Color(100, 150, 255), ToolMode.MOVE, "✋");
        moveBtn.setBounds(startX2, 0, subSize, subSize);
        moveBtn.addActionListener(e -> { currentTool = ToolMode.MOVE; repaint(); });
        bottomLeftBar.add(moveBtn);

        JButton removeBtn = createColorButton(new Color(200, 50, 50), ToolMode.DELETE, "❌");
        removeBtn.setBounds(startX2 + subSize + gap, 0, subSize, subSize);
        removeBtn.addActionListener(e -> { currentTool = ToolMode.DELETE; holdingBuilding = null; repaint(); });
        bottomLeftBar.add(removeBtn);

        JButton defBtn = createColorButton(new Color(100, 100, 120), ToolMode.NONE, "🛡️");
        defBtn.setBounds(startX2, subSize + gap, subSize, subSize);
        bottomLeftBar.add(defBtn);

        JButton atkBtn = createColorButton(new Color(150, 50, 50), ToolMode.COMMAND, "⚔️");
        atkBtn.setBounds(startX2 + subSize + gap, subSize + gap, subSize, subSize);
        atkBtn.addActionListener(e -> { currentTool = ToolMode.COMMAND; repaint(); });
        bottomLeftBar.add(atkBtn);

        gridMenuPanel = new JPanel(new GridLayout(3, 4, 3, 3)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setColor(new Color(25, 20, 18, 240)); g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.setColor(new Color(100, 75, 45)); g2d.setStroke(new BasicStroke(2f));
                g2d.drawRect(1, 1, getWidth() - 3, getHeight() - 3); g2d.dispose();
            }
        };
        gridMenuPanel.setBounds(230, getHeight() - 200, 240, 180); gridMenuPanel.setVisible(false);

        add(topRightBar); add(gridMenuPanel); add(bottomLeftBar);
        setComponentZOrder(topRightBar, 0); setComponentZOrder(gridMenuPanel, 1); setComponentZOrder(bottomLeftBar, 2);
    }

    private JButton createColorButton(Color baseColor, ToolMode mode, String icon) {
        JButton btn = new JButton(icon) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color topColor = getModel().isRollover() ? baseColor.brighter() : baseColor;
                Color botColor = getModel().isRollover() ? baseColor : baseColor.darker().darker();
                g2d.setPaint(new GradientPaint(0, 0, topColor, 0, getHeight(), botColor));
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);

                g2d.setColor(new Color(255, 255, 255, 80)); g2d.drawRoundRect(1, 1, getWidth()-3, getHeight()-3, 6, 6);
                g2d.setColor(new Color(0, 0, 0, 100)); g2d.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 6, 6);

                if (currentTool == mode && mode != ToolMode.NONE) {
                    g2d.setColor(new Color(255, 215, 0)); g2d.setStroke(new BasicStroke(3));
                    g2d.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, 6, 6);
                }

                g2d.setColor(Color.WHITE); g2d.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
                FontMetrics fm = g2d.getFontMetrics();
                g2d.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2d.dispose();
            }
        };
        btn.setContentAreaFilled(false); btn.setBorderPainted(false); btn.setFocusPainted(false);
        return btn;
    }

    private JButton createScrollItemButton(String title) {
        JButton btn = new JButton(title) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (getModel().isRollover()) {
                    g2d.setColor(new Color(200, 180, 130)); // Highlight jika di-hover
                } else {
                    g2d.setColor(new Color(210, 190, 140)); // Warna perkamen lebih gelap sedikit
                }
                g2d.fillRect(0, 0, getWidth(), getHeight());

                // Garis putus-putus ala cetakan stempel
                g2d.setColor(new Color(100, 50, 20));
                g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{5}, 0));
                g2d.drawRect(2, 2, getWidth() - 5, getHeight() - 5);

                g2d.setColor(new Color(80, 40, 20));
                g2d.setFont(new Font("Serif", Font.BOLD, 18));
                FontMetrics fm = g2d.getFontMetrics();
                g2d.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);

                g2d.dispose();
            }
        };
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        return btn;
    }

    private void showInGameMenu() {
        JPopupMenu popup = new JPopupMenu();
        JMenuItem saveItem = new JMenuItem("Save Progress");
        JMenuItem mainMenuItem = new JMenuItem("Back to Main Menu");

        // Fitur Save yang dihidupkan kembali!
        saveItem.addActionListener(e -> {
            String[] options = {"Slot 1", "Slot 2", "Slot 3"};
            int choice = JOptionPane.showOptionDialog(window, "Pilih Slot Penyimpanan:", "Save Progress", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
            if (choice >= 0) {
                saveGameData(choice + 1);
            }
        });

        mainMenuItem.addActionListener(e -> {
            if (holdingBuilding != null) window.savedBuildings.add(holdingBuilding);
            window.showScreen("MENU_SCREEN"); // Kembali ke Menu Utama
        });

        popup.add(saveItem);
        popup.addSeparator();
        popup.add(mainMenuItem);
        popup.show(this, getWidth() / 2 - 75, getHeight() / 2 - 50);
    }

    // --- TAMBAHKAN FUNGSI PENYIMPANAN INI TEPAT DI BAWAHNYA ---
    private void saveGameData(int slot) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("heart_save_" + slot + ".dat"))) {
            // Kita mengambil data bangunan dari Sang Bos (window) lalu menyimpannya
            oos.writeObject(window.savedBuildings);
            JOptionPane.showMessageDialog(window, "Progress kota berhasil disimpan di Slot " + slot + "!", "Save Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            System.out.println("Gagal menyimpan game: " + e.getMessage());
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        camera.clamp(getWidth(), getHeight(), 3000, 3000);
        Graphics2D g2d = (Graphics2D) g.create();
        camera.applyTransform(g2d);

        if (gameplayBg != null) g2d.drawImage(gameplayBg, 0, 0, 3000, 3000, null);
        else { g2d.setColor(new Color(30, 50, 30)); g2d.fillRect(0, 0, 3000, 3000); }



        if (currentTool == ToolMode.BUILD) {
            g2d.setColor(new Color(255, 255, 255, 30));
            g2d.setStroke(new BasicStroke(1f));
            int gridSize = 10;
            for (int i = 0; i <= 3000; i += gridSize) {
                g2d.drawLine(i, 0, i, 3000);
                g2d.drawLine(0, i, 3000, i);
            }
        }

        class RenderItem {
            double bottomY; Runnable drawAction;
            RenderItem(double bottomY, Runnable drawAction) {
                this.bottomY = bottomY; this.drawAction = drawAction;
            }

        }

        java.util.List<RenderItem> renderList = new java.util.ArrayList<>();

        for (Building b : window.savedBuildings) {
            BufferedImage img = getBuildImage(b.type);
            renderList.add(new RenderItem(b.getBounds().getY() + b.getBounds().getHeight(), () -> {
                b.draw(g2d, img);
                // --- EFEK HIGHLIGHT KUNING JIKA DIKLIK ---
                if (b == clickedBuilding) {
                    g2d.setColor(new Color(255, 255, 0, 70)); // Kuning transparan
                    g2d.fillRect(b.getBounds().x, b.getBounds().y, b.getBounds().width, b.getBounds().height);
                }
            }));
        }
        // 2. Masukkan Civil
        for (Civil c : window.activeCivils) {
            renderList.add(new RenderItem(c.y + c.size, () -> c.draw(g2d, civilImg)));
        }
        // 3. Masukkan Guard
        for (Guard hg : window.activeGuards) {
            BufferedImage spriteToUse = (hg.type == Guard.GuardType.ARCHER) ? archerImg : spearmanImg;
            renderList.add(new RenderItem(hg.y + hg.size, () -> hg.draw(g2d, spriteToUse)));
        }
        // 4. Masukkan Horde
        for (Horde h : window.activeHordes) {
            BufferedImage imgToUse = null;
            if (h.type == Horde.HordeType.AXEMAN) imgToUse = axemanImg;
            else if (h.type == Horde.HordeType.SHIELDBEARER) imgToUse = shieldImg;
            else if (h.type == Horde.HordeType.BOWMAN) imgToUse = bowmanHordeImg;

            final BufferedImage finalImg = imgToUse;

            renderList.add(new RenderItem(h.y + h.size, () -> h.draw(g2d, finalImg)));
        }

        // 5. Masukkan Pohon dari Hashtable
        // Kita ambil semua daftar pohon yang ada di dalam mapPohon
        for (java.util.List<Tree> daftarPohon : mapPohon.values()) {
            for (Tree pohon : daftarPohon) {
                // bottomY pohon adalah posisi Y ditambah tinggi pohon
                renderList.add(new RenderItem(pohon.y + pohon.height, () -> pohon.draw(g2d, treeImg)));
            }
        }

        // Masukkan Tambang ke daftar render
        for (Mine m : activeMines) {
            renderList.add(new RenderItem(m.y + m.height, () -> {
                m.draw(g2d, abandonedMineImg, builtMineImg);

                // --- EFEK HIGHLIGHT KUNING JIKA DIKLIK ---
                if (m == clickedMine) {
                    g2d.setColor(new Color(255, 255, 0, 70));
                    g2d.fillRect(m.x, m.y, m.width, m.height);
                }
            }));
        }

        // Urutkan dan Gambar! (Y Kecil/Atas digambar duluan, ditimpa oleh Y Besar/Bawah)
        renderList.sort((a, b) -> Double.compare(a.bottomY, b.bottomY));


        // =======================================================

        for (RenderItem item : renderList) {
            item.drawAction.run();
        }

        // Digambar di atas pasukan agar terlihat melintas di langit
        for (Projectile p : window.activeProjectiles) {
            p.draw(g2d);
        }



        // Render Preview Kursor (Dinamis Sesuai Tipe Bangunan)
        int bw = 90; // Default fallback
        BufferedImage previewImg = null;

        // Tentukan ukuran dan gambar bayangan sesuai alat yang dipakai
        if (currentTool == ToolMode.BUILD) {
            bw = getBuildWidth(selectedBuilding);
            previewImg = getBuildImage(selectedBuilding);
        } else if (currentTool == ToolMode.MOVE && holdingBuilding != null) {
            bw = holdingBuilding.getBounds().width;
            previewImg = getBuildImage(holdingBuilding.type);
        }

        int rawX = mouseX - (bw / 2);
        int rawY = mouseY - (bw / 2);

        int pX = Math.round((float) rawX / 10f) * 10;
        int pY = Math.round((float) rawY / 10f) * 10;

        Rectangle previewRect = new Rectangle(pX, pY, bw, bw);

        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));

        if (isDraggingWall) {
            // RENDER BAYANGAN TEMBOK PANJANG
            int diffX = Math.abs(dragCurrentPoint.x - dragStartPoint.x);
            int diffY = Math.abs(dragCurrentPoint.y - dragStartPoint.y);

            if (diffX > diffY) {
                int startX = Math.min(dragStartPoint.x, dragCurrentPoint.x);
                int endX = Math.max(dragStartPoint.x, dragCurrentPoint.x);
                for (int x = startX; x <= endX; x += 10) {
                    Building.BuildingType type = (x == startX) ? Building.BuildingType.WALL_L : Building.BuildingType.WALL_R;
                    if(isOverlapping(new Rectangle(x, dragStartPoint.y, 10, 10), null)) {
                        g2d.setColor(new Color(255, 0, 0, 150)); g2d.fillRect(x, dragStartPoint.y, 10, 10);
                    } else {
                        // Tinggi ditarik ke atas sejauh 18 (28 - 10)
                        g2d.drawImage(getBuildImage(type), x, dragStartPoint.y - 18, 10, 28, null);
                    }
                }
            } else {
                int startY = Math.min(dragStartPoint.y, dragCurrentPoint.y);
                int endY = Math.max(dragStartPoint.y, dragCurrentPoint.y);
                for (int y = startY; y <= endY; y += 10) {
                    if(isOverlapping(new Rectangle(dragStartPoint.x, y, 10, 10), null)) {
                        g2d.setColor(new Color(255, 0, 0, 150)); g2d.fillRect(dragStartPoint.x, y, 10, 10);
                    } else {
                        g2d.drawImage(getBuildImage(Building.BuildingType.WALL_UD), dragStartPoint.x, y - 18, 10, 28, null);
                    }
                }
            }
        }
        else if (currentTool == ToolMode.BUILD || (currentTool == ToolMode.MOVE && holdingBuilding != null)) {
            // Biar saat pindah bangunan (Move) dia gak mendeteksi nabrak dirinya sendiri
            Building ignoreB = (currentTool == ToolMode.MOVE) ? holdingBuilding : null;

            if (isOverlapping(previewRect, ignoreB)) {
                g2d.setColor(new Color(255, 0, 0, 150));
                g2d.fillRect(pX, pY, bw, bw);
            } else {
                if (previewImg != null) {
                    Building.BuildingType typeToDraw = (currentTool == ToolMode.BUILD) ? selectedBuilding : holdingBuilding.type;

                    // MINTA DATA UKURAN KE BUILDING.JAVA (DRY Principle)
                    int drawW = Building.getVisualWidth(typeToDraw);
                    int drawH = Building.getVisualHeight(typeToDraw);

                    int drawX = pX - ((drawW - bw) / 2);
                    int drawY = pY - (drawH - bw);

                    g2d.drawImage(previewImg, drawX, drawY, drawW, drawH, null);
                } else {
                    g2d.setColor(new Color(200, 200, 200, 150));
                    g2d.fillRect(pX, pY, bw, bw);
                }
            }
        }
        else if (currentTool == ToolMode.DELETE) {
            g2d.setColor(new Color(255, 0, 0, 100)); g2d.fillOval(mouseX - 25, mouseY - 25, 50, 50);
        }
        else if (currentTool == ToolMode.MOVE && holdingBuilding == null) {
            g2d.setColor(new Color(0, 200, 255, 100)); g2d.fillOval(mouseX - 25, mouseY - 25, 50, 50);
        }

        // --- RENDER KOTAK BLOK COMMAND (Pakai koordinat layar langsung) ---
        if (isDragging && dragStartScreen != null && dragEndScreen != null && currentTool == ToolMode.COMMAND) {
            Graphics2D g2dHUD = (Graphics2D) g.create();
            int sx = Math.min(dragStartScreen.x, dragEndScreen.x);
            int sy = Math.min(dragStartScreen.y, dragEndScreen.y);
            int sw = Math.abs(dragStartScreen.x - dragEndScreen.x);
            int sh = Math.abs(dragStartScreen.y - dragEndScreen.y);

            g2dHUD.setColor(new Color(0, 255, 0, 40));
            g2dHUD.fillRect(sx, sy, sw, sh);
            g2dHUD.setColor(new Color(0, 255, 0, 150));
            g2dHUD.setStroke(new BasicStroke(1.5f));
            g2dHUD.drawRect(sx, sy, sw, sh);
            g2dHUD.dispose();
        }

        g2d.dispose();

        // =========================================================
        // 7.5. RENDER KEGELAPAN MALAM (Tepat di atas dunia, di bawah UI)
        // =========================================================
        if (currentDarkness > 0.01f) {
            // Menggunakan warna Biru Dongker Malam (Navy Blue) yang transparan
            g.setColor(new Color(15, 20, 45, (int)(currentDarkness * 255)));
            g.fillRect(0, 0, getWidth(), getHeight());
        }

        // =========================================================
        // 8. RENDER MINIMAP (DI MAIN GAME PANEL)
        // =========================================================
        Graphics2D gMap = (Graphics2D) g.create();
        gMap.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // UBAH UKURAN: Menjadi 200x200 (Persegi/Kotak) karena map-mu sekarang 3000x3000
        int mapSize = 200;
        int mapX = 15;
        int mapY = getHeight() - mapSize - 20; // Selalu melayang pas di atas scroll menu

        // A. Gambar Background Minimap (Kotak Gelap)
        gMap.setColor(new Color(20, 15, 10, 220));
        gMap.fillRect(mapX, mapY, mapSize, mapSize);
        gMap.setColor(new Color(160, 120, 70));
        gMap.setStroke(new BasicStroke(2f));
        gMap.drawRect(mapX, mapY, mapSize, mapSize);

        // B. Hitung Skala (Ukuran Asli Peta 3000x3000 -> 200x200)
        double scale = (double) mapSize / 3000.0; // Cukup 1 variabel skala karena bentuknya persegi

        // C. Gambar Bangunan (Kotak Coklat)
        gMap.setColor(new Color(150, 80, 40));
        for (Building b : window.savedBuildings) {
            int bx = mapX + (int)(b.getBounds().x * scale);
            int by = mapY + (int)(b.getBounds().y * scale);
            int mapBw = Math.max(3, (int)(b.getBounds().width * scale));
            int mapBh = Math.max(3, (int)(b.getBounds().height * scale));
            gMap.fillRect(bx, by, mapBw, mapBh);
        }

        // D. Gambar Musuh / Horde (Titik Merah)
        gMap.setColor(Color.RED);
        for (Horde horde : window.activeHordes) {
            int hx = mapX + (int)(horde.x * scale);
            int hy = mapY + (int)(horde.y * scale);
            gMap.fillRect(hx, hy, 3, 3);
        }

        // E. Gambar Pasukan Guard (Titik Biru Terang)
        gMap.setColor(new Color(0, 200, 255));
        for (Guard guard : window.activeGuards) {
            int gx = mapX + (int)(guard.x * scale);
            int gy = mapY + (int)(guard.y * scale);
            gMap.fillRect(gx, gy, 3, 3);
        }

        gMap.setColor(new Color(255, 255, 255, 150)); gMap.setStroke(new BasicStroke(1f));
        double camZoom = camera.getZoom();
        double viewWorldX = -camera.getX() / camZoom; double viewWorldY = -camera.getY() / camZoom;
        double viewWorldW = getWidth() / camZoom; double viewWorldH = getHeight() / camZoom;
        int vx = mapX + (int)(viewWorldX * scale); int vy = mapY + (int)(viewWorldY * scale);
        int vw = (int)(viewWorldW * scale); int vh = (int)(viewWorldH * scale);
        gMap.drawRect(vx, vy, vw, vh);

        int ringSize = 42; int ringX = mapX + mapSize - (ringSize / 2); int ringY = mapY - (ringSize / 2);

        if (isDayTime) { gMap.setColor(new Color(110, 190, 240)); } else { gMap.setColor(new Color(20, 25, 60)); }
        gMap.fillOval(ringX, ringY, ringSize, ringSize);

        // 2. GAMBAR BENDA LANGIT SOLID (MATAHARI / BULAN)
        if (isDayTime) {
            gMap.setColor(new Color(255, 215, 0)); gMap.fillOval(ringX + 8, ringY + 8, 26, 26);
            gMap.setColor(new Color(255, 255, 255, 100)); gMap.fillOval(ringX + 12, ringY + 12, 10, 10);
        } else {
            gMap.setColor(new Color(240, 240, 220)); gMap.fillOval(ringX + 10, ringY + 8, 24, 24);
            gMap.setColor(new Color(20, 25, 60)); gMap.fillOval(ringX + 16, ringY + 4, 20, 20);
        }

        gMap.setColor(new Color(40, 45, 50)); gMap.setStroke(new BasicStroke(4f)); gMap.drawOval(ringX, ringY, ringSize, ringSize);
        gMap.setColor(new Color(130, 135, 140)); gMap.setStroke(new BasicStroke(1.5f));
        gMap.drawOval(ringX - 2, ringY - 2, ringSize + 4, ringSize + 4); gMap.drawOval(ringX + 2, ringY + 2, ringSize - 4, ringSize - 4);
        gMap.dispose();

    }



    private void generateForest() {
        int ukuranKavling = 1000; // Map 3000x3000 dibagi jadi petakan 1000x1000

        // Looping untuk 9 petak kavling (Sumbu X: 0, 1, 2 dan Sumbu Y: 0, 1, 2)
        for (int gridX = 0; gridX < 3; gridX++) {
            for (int gridY = 0; gridY < 3; gridY++) {

                // 1. Buat "Key" untuk Hashtable (Contoh: "0,0", "1,2", dll)
                String kunciKavling = gridX + "," + gridY;

                // 2. Buat "LinkedList/ArrayList" kosong untuk petak ini
                java.util.List<Tree> daftarPohon = new java.util.ArrayList<>();

                // 3. Lempar dadu nasib untuk kavling ini (Angka acak 1, 2, atau 3)
                int nasib = (int)(Math.random() * 3) + 1;

                int jumlahPohon = 30;
                if (nasib == 1) jumlahPohon = 100; // Hutan Lebat (25 Pohon)
                else if (nasib == 3) jumlahPohon = 50; // Pinggiran Hutan (5 Pohon)
                // Jika nasib == 2, jumlahPohon tetap 0 (Area Kosong untuk bangun desa)

                // 4. Mulai tanam pohon sebanyak 'jumlahPohon'
                for (int i = 0; i < jumlahPohon; i++) {
                    // Matematika sederhana:
                    // Titik Awal Petak + Angka Acak (0 sampai 900 agar tidak keluar batas petak)
                    int randomX = (gridX * ukuranKavling) + (int)(Math.random() * 900);
                    int randomY = (gridY * ukuranKavling) + (int)(Math.random() * 900);

                    // Asumsi ukuran pohonmu adalah 60x80 pixel (Silakan sesuaikan nanti)
                    Tree pohonBaru = new Tree(randomX, randomY, 26, 47);
                    daftarPohon.add(pohonBaru);
                }

                // 5. Simpan daftar pohon yang sudah jadi ke dalam Hashtable
                mapPohon.put(kunciKavling, daftarPohon);
            }
        }
    }

    private void generateMines() {
        int jumlahTambang = 8; // Ubah jumlah tambang yang mau disebar di map
        for (int i = 0; i < jumlahTambang; i++) {
            // Acak posisi di map 3000x3000, tapi sisakan batas pinggir
            int randomX = 100 + (int)(Math.random() * 2800);
            int randomY = 100 + (int)(Math.random() * 2800);

            activeMines.add(new Mine(randomX, randomY));
        }
    }
}