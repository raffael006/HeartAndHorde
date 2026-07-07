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
    private BufferedImage underConstructionImg;
    private BufferedImage heartImg;
    private BufferedImage builderImg;

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
    public List<Mine> activeMines = new java.util.ArrayList<>();
    private Mine clickedMine = null;
    private boolean cameraInitialized = false;

    // --- TAMBAHAN BARU: Status Cheat Mode ---
    private boolean isCheatModeActive = false;
    // --- FITUR BARU: DEVELOPER PANEL ---
    private boolean devPanelVisible = false;
    private JPanel devPanel;

    // --- Variabel UI/HUD ---
    private JPanel bottomLeftBar;
    private JPanel topRightBar;
    private JButton menuBtn;

    private boolean isBuildMenuExpanded = false;
    private JButton house1Btn, house2Btn, house3Btn;

    // --- VARIABEL GRID MENU & ALAT ---
    private enum MenuState { CLOSED, MAIN_MENU, CIVIL_MENU, MILITARY_MENU, BUILDING_SELECTED, MINE_SELECTED, BARRACK_MENU }    private MenuState currentMenuState = MenuState.MAIN_MENU;
    private Building.BuildingType selectedBuilding = Building.BuildingType.MEDIUM_HOUSE;
    private JPanel gridMenuPanel;
    private Building clickedBuilding = null;
    private BufferedImage civilBuilderImg;

    // --- FITUR BARU: SISTEM PRODUKSI GUARD DARI BARRACK ---
    // Setiap barrack punya antrian Guard-nya sendiri (HashMap supaya bisa beda-beda tiap bangunan)
    private java.util.HashMap<Building, java.util.Queue<Guard.GuardType>> barrackQueues = new java.util.HashMap<>();
    private java.util.HashMap<Building, Float> barrackProgress = new java.util.HashMap<>();
    private final float GUARD_PRODUCTION_MAX = 300f; // 300 tick ~= 5 detik @ 60fps
    private JPanel barrackQueuePanel; // Panel antrean di sebelah kanan grid menu

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

    private FogOfWar fogOfWar;
    private static final float HEART_REVEAL_RADIUS = 500f;
    private static final float GUARD_REVEAL_RADIUS = 300f;

    //===============================TREE====================================================
    // Hashtable (Spatial Hash) untuk menyimpan pohon berdasarkan petak Kavling
    public java.util.HashMap<String, List<Tree>> mapPohon = new java.util.HashMap<>();
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

        // Posisi tengah map (map 3000 x 3000)
        int heartSize = 80;
        int mapCenter = 3000 / 2;
        Building heart = new Building(
                mapCenter - heartSize / 2,
                mapCenter - heartSize / 2,
                heartSize,
                heartSize,
                Building.BuildingType.HEART,
                5
        );
        fogOfWar = new FogOfWar(3000, 3000);
        fogOfWar.reveal(heart.getBounds().getCenterX(), heart.getBounds().getCenterY(), HEART_REVEAL_RADIUS);

        heart.isBuilt = true;
        window.savedBuildings.add(heart);
        System.out.println(heart.getBounds());
        System.out.println("Jumlah building: " + window.savedBuildings.size());


        for (int i = 0; i < 5; i++) {
            window.activeCivils.add(new Civil(1500, 1500));
        }

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
            underConstructionImg = ImageIO.read(new File("assets/img/building.png"));
        } catch (Exception e) {
            System.out.println("Gagal memuat Gambar Rumah!");
        }

        try {
            heartImg = ImageIO.read(new File("assets/img/Heart.png"));
            System.out.println(heartImg);
        } catch (Exception e) {
            System.out.println("Gagal memuat Heart!");
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
            civilBuilderImg = ImageIO.read(new File("assets/img/Civil_Builder.png"));
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

        try{
            builderImg = ImageIO.read(new File("assets/img/builder.png"));
        } catch (Exception e) {
            e.printStackTrace();
        }


        cameraTimer = new Timer(16, e -> {
            boolean moved = camera.move(wPressed, sPressed, aPressed, dPressed);

            for (CivilBuilder cb : window.activeCivilBuilders) {
                cb.update();
            }

            // --- FITUR BARU: LOGIKA PRODUKSI GUARD DARI BARRACK ---
            for (Building b : window.savedBuildings) {
                if (b.type == Building.BuildingType.BARRACK && b.isBuilt) {
                    java.util.Queue<Guard.GuardType> q = barrackQueues.get(b);
                    if (q != null && !q.isEmpty()) {
                        float prog = barrackProgress.getOrDefault(b, 0f) + 1f;
                        if (prog >= GUARD_PRODUCTION_MAX) {
                            Guard.GuardType type = q.poll();
                            double spawnX = b.getBounds().getCenterX();
                            double spawnY = b.getBounds().getMaxY() + 15;
                            window.activeGuards.add(new Guard(type, spawnX, spawnY));
                            barrackProgress.put(b, 0f);
                        } else {
                            barrackProgress.put(b, prog);
                        }
                    }
                }
            }
            // Refresh panel antrean kalau lagi terbuka
            if (barrackQueuePanel != null && barrackQueuePanel.isVisible()) barrackQueuePanel.repaint();

            // 1. Update Guards (Parameter Nambah)
            for (Guard g : window.activeGuards) {
                g.update(window.activeGuards, window.activeHordes, window.activeProjectiles);
                fogOfWar.revealIfMoved(g, g.x, g.y, GUARD_REVEAL_RADIUS);
            }
            fogOfWar.update();

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
            for (List<Tree> daftarPohon : mapPohon.values()) {
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

            for (Building b : window.savedBuildings) {
                if (!b.isBuilt && b.assignedBuilder == null) {
                    // --- FITUR BARU: SISTEM ANTREAN CIVIL BUILDER ---
                    // 1) Prioritaskan builder yang lagi nganggur di rumah (langsung berangkat sekarang).
                    CivilBuilder chosenBuilder = null;
                    for (CivilBuilder cb : window.activeCivilBuilders) {
                        if (cb.state == CivilBuilder.BuilderState.IDLE_HOME) {
                            chosenBuilder = cb;
                            break;
                        }
                    }
                    // 2) Kalau semua builder lagi sibuk, taruh ke builder dengan antrean paling sedikit
                    // (biar beban kerja merata kalau builder-nya lebih dari satu).
                    if (chosenBuilder == null) {
                        for (CivilBuilder cb : window.activeCivilBuilders) {
                            if (chosenBuilder == null || cb.buildQueue.size() < chosenBuilder.buildQueue.size()) {
                                chosenBuilder = cb;
                            }
                        }
                    }
                    if (chosenBuilder != null) {
                        chosenBuilder.queueBuilding(b, window.savedBuildings);
                    }
                }
            }

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
                    topRightBar.setBounds(w - 830, 0, 830, 40);
                    menuBtn.setBounds(topRightBar.getWidth() - 40, 4, 32, 32);
                    bottomLeftBar.setBounds(15, h - 310, 180, 80);
                    if (gridMenuPanel != null) gridMenuPanel.setBounds(230, h - 200, 240, 180);
                    if (barrackQueuePanel != null) barrackQueuePanel.setBounds(475, h - 200, 180, 180);
                    if (devPanel != null) devPanel.setBounds(w / 2 - 270, h / 2 - 230, 540, 460);
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

        // --- UPDATE PENGGUNAAN CHEAT: HANYA BISA JIKA CHEAT MODE AKTIF ---
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_K, 0), "cheatSpawnSpearman");
        getActionMap().put("cheatSpawnSpearman", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (isCheatModeActive) { window.activeGuards.add(new Guard(Guard.GuardType.SPEARMAN, mouseX, mouseY)); repaint(); }
            }
        });

        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_L, 0), "cheatSpawnArcher");
        getActionMap().put("cheatSpawnArcher", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (isCheatModeActive) { window.activeGuards.add(new Guard(Guard.GuardType.ARCHER, mouseX, mouseY)); repaint(); }
            }
        });

        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_7, 0), "spawnAxeman");
        getActionMap().put("spawnAxeman", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (isCheatModeActive) { window.activeHordes.add(new Horde(Horde.HordeType.AXEMAN, mouseX, mouseY)); repaint(); }
            }
        });

        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_8, 0), "spawnShield");
        getActionMap().put("spawnShield", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (isCheatModeActive) { window.activeHordes.add(new Horde(Horde.HordeType.SHIELDBEARER, mouseX, mouseY)); repaint(); }
            }
        });

        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_9, 0), "spawnBowmanHorde");
        getActionMap().put("spawnBowmanHorde", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (isCheatModeActive) { window.activeHordes.add(new Horde(Horde.HordeType.BOWMAN, mouseX, mouseY)); repaint(); }
            }
        });

        // --- CHEAT SPAWN CIVIL (Tekan P) ---
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_P, 0), "spawnCivil");
        getActionMap().put("spawnCivil", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (isCheatModeActive) { window.activeCivils.add(new Civil(mouseX, mouseY)); repaint(); }
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

                                    List<Point> calculatedPath = PathFinder.findPath(
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
                    for (List<Tree> daftarPohon : mapPohon.values()) {
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

                    } else {
                        // --- TAMBAHAN KODE: CEK DAN KURANGI COST KAYU ---
                        int cost = Building.getWoodCost(selectedBuilding);
                        // --- DEV MODE: gratis, skip cek kayu ---
                        if (isCheatModeActive || totalWood >= cost) {
                            if (!isOverlapping(newArea, null) && !isTreeBlocking(newArea)) {
                                if (!isCheatModeActive) totalWood -= cost; // Bayar kayunya (hanya kalau bukan dev mode)

                                int cap = getBuildCapacity(selectedBuilding);
                                Building newBuilding = new Building(targetX, targetY, bw, bw, selectedBuilding, cap);
                                window.savedBuildings.add(newBuilding);

                                double spawnPointX = targetX + (bw / 2.0);
                                double spawnPointY = targetY + (bw / 2.0);

                                if (selectedBuilding == Building.BuildingType.BUILDER) {
                                    // Builder House -> isinya CivilBuilder, bukan Civil biasa
                                    for (int i = 0; i < cap; i++) {
                                        window.activeCivilBuilders.add(new CivilBuilder(spawnPointX, spawnPointY, newBuilding));
                                    }
                                } else {
                                    for (int i = 0; i < cap; i++) {
                                        window.activeCivils.add(new Civil(spawnPointX, spawnPointY));
                                    }
                                }
                            }
                        } else {
                            System.out.println("Kayu tidak cukup untuk membangun!");
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
                            Building.BuildingType type = (x == startX) ? Building.BuildingType.WALL_L : Building.BuildingType.WALL_R;
                            int cost = Building.getWoodCost(type);
                            if ((isCheatModeActive || totalWood >= cost) && !isOverlapping(new Rectangle(x, y, 10, 10), null) && !isTreeBlocking(new Rectangle(x, y, 10, 10))) {
                                if (!isCheatModeActive) totalWood -= cost;
                                window.savedBuildings.add(new Building(x, y, 10, 10, type, 0));
                            }
                        }
                    } else {
                        // VERTIKAL
                        int startY = Math.min(dragStartPoint.y, dragCurrentPoint.y);
                        int endY = Math.max(dragStartPoint.y, dragCurrentPoint.y);
                        int x = dragStartPoint.x;
                        for (int y = startY; y <= endY; y += 10) {
                            int cost = Building.getWoodCost(Building.BuildingType.WALL_UD);
                            if ((isCheatModeActive || totalWood >= cost) && !isOverlapping(new Rectangle(x, y, 10, 10), null) && !isTreeBlocking(new Rectangle(x, y, 10, 10))) {
                                if (!isCheatModeActive) totalWood -= cost;
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

    // --- CEK APAKAH MASIH ADA POHON DI AREA YANG MAU DIBANGUN ---
    // Selama pohonnya belum habis ditebang (masih ada di mapPohon), area itu tidak boleh dibangun.
    private boolean isTreeBlocking(Rectangle newRect) {
        for (List<Tree> daftarPohon : mapPohon.values()) {
            for (Tree pohon : daftarPohon) {
                if (pohon.getBounds().intersects(newRect)) {
                    return true; // Masih ada pohon -> harus ditebang dulu
                }
            }
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
        if (type == Building.BuildingType.FARM) return 0;    // Farm tempat kerja, bukan tempat tinggal -> tidak spawn Civil
        if (type == Building.BuildingType.STORAGE) return 0; // Storage cuma gudang -> tidak spawn Civil
        if (type == Building.BuildingType.BARRACK) return 0; // Barrack tidak spawn Civil -> Guard diproduksi manual via menu ➕
        if (type == Building.BuildingType.SMALL_HOUSE) return 2;
        if (type == Building.BuildingType.BIG_HOUSE) return 8;
        if (type == Building.BuildingType.BUILDER) return 2;
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
        if (type == Building.BuildingType.HEART) return heartImg;
        if (type == Building.BuildingType.BUILDER) return builderImg;
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
            boolean isBuilderSel = (selectedBuilding == Building.BuildingType.BUILDER);


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
            gridMenuPanel.add(createGridBtn("👷", () -> {
                selectedBuilding = Building.BuildingType.BUILDER;
                currentTool = ToolMode.BUILD;
                updateGridMenu();
                repaint();
            }, isBuilderSel));

            for(int i=0; i<6; i++) gridMenuPanel.add(createGridBtn("", null, false));
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
            // --- FITUR BARU: Tombol "+" khusus muncul kalau bangunan yang diklik adalah BARRACK ---
            if (clickedBuilding != null && clickedBuilding.type == Building.BuildingType.BARRACK && clickedBuilding.isBuilt) {
                gridMenuPanel.add(createGridBtn("➕", () -> {
                    currentMenuState = MenuState.BARRACK_MENU;
                    updateGridMenu();
                    if (barrackQueuePanel != null) {
                        barrackQueuePanel.setVisible(true);
                        barrackQueuePanel.repaint();
                    }
                }, false));
            } else {
                gridMenuPanel.add(createGridBtn("", null, false));
            }
            for(int i=0; i<7; i++) gridMenuPanel.add(createGridBtn("", null, false));
            gridMenuPanel.add(createGridBtn("⬅️", () -> {
                clickedBuilding = null; currentMenuState = MenuState.MAIN_MENU; updateGridMenu(); repaint();
                if (barrackQueuePanel != null) barrackQueuePanel.setVisible(false);
            }, false));
        }

        // --- FITUR BARU: MENU PILIH GUARD DI BARRACK ---
        else if (currentMenuState == MenuState.BARRACK_MENU) {
            // Slot 1: Spearman (pakai gambar sprite)
            gridMenuPanel.add(createGridBtnWithImage(spearmanImg, "Spearman", () -> {
                if (clickedBuilding != null) {
                    // --- FITUR BARU: Butuh 1 Civil untuk direkrut jadi Guard ---
                    if (!window.activeCivils.isEmpty()) {
                        window.activeCivils.remove(window.activeCivils.size() - 1); // Kurangi 1 civil
                        barrackQueues.computeIfAbsent(clickedBuilding, k -> new java.util.LinkedList<>())
                                .add(Guard.GuardType.SPEARMAN);
                        if (barrackQueuePanel != null) barrackQueuePanel.repaint();
                    } else {
                        System.out.println("Tidak ada Civil yang bisa direkrut menjadi Spearman!");
                    }
                }
            }));
            // Slot 2: Archer (pakai gambar sprite)
            gridMenuPanel.add(createGridBtnWithImage(archerImg, "Archer", () -> {
                if (clickedBuilding != null) {
                    // --- FITUR BARU: Butuh 1 Civil untuk direkrut jadi Guard ---
                    if (!window.activeCivils.isEmpty()) {
                        window.activeCivils.remove(window.activeCivils.size() - 1); // Kurangi 1 civil
                        barrackQueues.computeIfAbsent(clickedBuilding, k -> new java.util.LinkedList<>())
                                .add(Guard.GuardType.ARCHER);
                        if (barrackQueuePanel != null) barrackQueuePanel.repaint();
                    } else {
                        System.out.println("Tidak ada Civil yang bisa direkrut menjadi Archer!");
                    }
                }
            }));
            // Slot 3: Tampilan jumlah Civil yang tersedia (teks, merah kalau 0)
            gridMenuPanel.add(createCivilCountDisplay());
            // Sisa kotak kosong + tombol kembali
            for(int i=0; i<8; i++) gridMenuPanel.add(createGridBtn("", null, false));
            gridMenuPanel.add(createGridBtn("⬅️", () -> {
                currentMenuState = MenuState.BUILDING_SELECTED;
                updateGridMenu();
                if (barrackQueuePanel != null) barrackQueuePanel.setVisible(false);
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

    // --- FITUR BARU: Varian createGridBtn yang menampilkan gambar sprite (untuk menu Barrack) ---
    private JButton createGridBtnWithImage(BufferedImage img, String label, Runnable action) {
        JButton btn = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Background + hover
                g2d.setColor(getModel().isRollover() ? new Color(15, 12, 10) : new Color(40, 35, 30));
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.setColor(new Color(80, 60, 35));
                g2d.drawRect(0, 0, getWidth(), getHeight());

                int w = getWidth(), h = getHeight();

                // Gambar sprite di tengah atas
                if (img != null) {
                    int imgSize = Math.min(w - 10, h - 20);
                    int imgX = (w - imgSize) / 2;
                    int imgY = 4;
                    g2d.drawImage(img, imgX, imgY, imgSize, imgSize, null);
                }

                // Label nama di bawah gambar
                g2d.setColor(new Color(210, 190, 160));
                g2d.setFont(new Font("Serif", Font.BOLD, 10));
                FontMetrics fm = g2d.getFontMetrics();
                g2d.drawString(label, (w - fm.stringWidth(label)) / 2, h - 5);

                g2d.dispose();
            }
        };
        btn.setContentAreaFilled(false); btn.setBorderPainted(false); btn.setFocusPainted(false);
        if (action != null) btn.addActionListener(e -> action.run());
        return btn;
    }

    // --- FITUR BARU: Slot info jumlah Civil tersedia di menu Barrack ---
    // Teks putih kalau cukup, merah kalau 0 (tidak bisa rekrut)
    private JPanel createCivilCountDisplay() {
        return new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Background sama seperti slot grid lain
                g2d.setColor(new Color(40, 35, 30));
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.setColor(new Color(80, 60, 35));
                g2d.drawRect(0, 0, getWidth(), getHeight());

                int civilCount = window.activeCivils.size();
                int w = getWidth(), h = getHeight();

                // Label atas: "Civil"
                g2d.setFont(new Font("Serif", Font.BOLD, 11));
                g2d.setColor(new Color(180, 160, 120));
                String label = "Civil";
                FontMetrics fm = g2d.getFontMetrics();
                g2d.drawString(label, (w - fm.stringWidth(label)) / 2, h / 2 - 8);

                // Angka: putih kalau ada, merah kalau 0
                g2d.setFont(new Font("Serif", Font.BOLD, 20));
                g2d.setColor(civilCount > 0 ? new Color(220, 210, 180) : new Color(220, 50, 50));
                String countStr = String.valueOf(civilCount);
                FontMetrics fm2 = g2d.getFontMetrics();
                g2d.drawString(countStr, (w - fm2.stringWidth(countStr)) / 2, h / 2 + 14);

                // Label bawah: "tersedia" atau "kurang!" kalau 0
                g2d.setFont(new Font("Serif", Font.ITALIC, 9));
                g2d.setColor(civilCount > 0 ? new Color(150, 135, 105) : new Color(200, 60, 60));
                String sub = civilCount > 0 ? "tersedia" : "kurang!";
                FontMetrics fm3 = g2d.getFontMetrics();
                g2d.drawString(sub, (w - fm3.stringWidth(sub)) / 2, h / 2 + 26);

                g2d.dispose();
            }
        };
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

        // --- FITUR BARU: PANEL ANTREAN PRODUKSI BARRACK (tanpa grid, di sebelah kanan gridMenuPanel) ---
        barrackQueuePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth(), h = getHeight();

                // Background gelap sama seperti gridMenuPanel
                g2d.setColor(new Color(25, 20, 18, 240));
                g2d.fillRect(0, 0, w, h);
                g2d.setColor(new Color(100, 75, 45));
                g2d.setStroke(new BasicStroke(2f));
                g2d.drawRect(1, 1, w - 3, h - 3);

                // Judul
                g2d.setFont(new Font("Segoe UI Emoji", Font.BOLD, 11));
                g2d.setColor(new Color(210, 190, 150));
                g2d.drawString("PRODUCTION QUEUE", 10, 18);

                // Ambil data antrian dari barrack yang sedang dipilih
                java.util.Queue<Guard.GuardType> q = (clickedBuilding != null) ? barrackQueues.get(clickedBuilding) : null;
                float prog = (clickedBuilding != null) ? barrackProgress.getOrDefault(clickedBuilding, 0f) : 0f;
                Guard.GuardType[] arr = (q != null) ? q.toArray(new Guard.GuardType[0]) : new Guard.GuardType[0];

                if (arr.length == 0) {
                    // Antrean kosong
                    g2d.setFont(new Font("Serif", Font.ITALIC, 11));
                    g2d.setColor(new Color(130, 115, 90));
                    g2d.drawString("(queue kosong)", 18, h / 2 + 4);
                } else {
                    // --- Bar Produksi: Guard yang sedang dibuat (arr[0]) ---
                    int barY = 30;
                    int barW = w - 20;

                    // Gambar sprite guard yang sedang diproduksi
                    BufferedImage producingImg = (arr[0] == Guard.GuardType.SPEARMAN) ? spearmanImg : archerImg;
                    if (producingImg != null) g2d.drawImage(producingImg, 8, barY, 28, 28, null);

                    // Label nama
                    g2d.setFont(new Font("Segoe UI Emoji", Font.BOLD, 11));
                    g2d.setColor(new Color(210, 190, 150));
                    g2d.drawString(arr[0] == Guard.GuardType.SPEARMAN ? "Spearman" : "Archer", 42, barY + 12);

                    // Loading bar produksi
                    int pbY = barY + 18;
                    int pbW = barW - 34;
                    int pbH = 8;
                    int pbX = 42;
                    g2d.setColor(new Color(60, 50, 35));
                    g2d.fillRoundRect(pbX, pbY, pbW, pbH, 4, 4);
                    g2d.setColor(new Color(180, 140, 50));
                    int fill = (int) ((prog / GUARD_PRODUCTION_MAX) * pbW);
                    g2d.fillRoundRect(pbX, pbY, fill, pbH, 4, 4);
                    g2d.setColor(new Color(100, 75, 45));
                    g2d.drawRoundRect(pbX, pbY, pbW, pbH, 4, 4);

                    // --- Sisa antrian (arr[1] dst.) sebagai ikon kecil ---
                    if (arr.length > 1) {
                        g2d.setColor(new Color(150, 130, 100));
                        g2d.setFont(new Font("Serif", Font.ITALIC, 10));
                        g2d.drawString("Waiting:", 8, 72);

                        int iconX = 8;
                        int iconY = 78;
                        int iconSize = 24;
                        for (int i = 1; i < arr.length && i <= 6; i++) {
                            BufferedImage qIcon = (arr[i] == Guard.GuardType.SPEARMAN) ? spearmanImg : archerImg;
                            if (qIcon != null) {
                                g2d.drawImage(qIcon, iconX, iconY, iconSize, iconSize, null);
                                // Garis kotak kecil di sekeliling ikon
                                g2d.setColor(new Color(100, 75, 45));
                                g2d.drawRect(iconX, iconY, iconSize, iconSize);
                            }
                            iconX += iconSize + 3;
                            if (iconX + iconSize > w - 5) { iconX = 8; iconY += iconSize + 3; }
                        }
                        // Kalau masih ada lebih dari yang ditampilkan
                        if (arr.length > 7) {
                            g2d.setFont(new Font("Serif", Font.BOLD, 10));
                            g2d.setColor(new Color(200, 180, 140));
                            g2d.drawString("+" + (arr.length - 7) + " more", iconX, iconY + 16);
                        }
                    }
                }
                g2d.dispose();
            }
        };
        barrackQueuePanel.setBounds(475, getHeight() - 200, 180, 180);
        barrackQueuePanel.setVisible(false);
        barrackQueuePanel.setOpaque(false);

        add(topRightBar); add(gridMenuPanel); add(barrackQueuePanel); add(bottomLeftBar);
        setComponentZOrder(topRightBar, 0); setComponentZOrder(gridMenuPanel, 1); setComponentZOrder(barrackQueuePanel, 2); setComponentZOrder(bottomLeftBar, 3);

        // --- FITUR BARU: INISIALISASI DEV PANEL ---
        devPanel = buildDevPanel();
        add(devPanel);
        setComponentZOrder(devPanel, 0); // Di atas semua panel lain
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

    // =====================================================================
    // --- DEVELOPER PANEL (custom designed, all features accessible) ---
    // =====================================================================
    private JPanel buildDevPanel() {
        JPanel panel = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();

                // --- Background gelap dengan gradient ---
                GradientPaint bg = new GradientPaint(0, 0, new Color(18, 14, 12), 0, h, new Color(28, 22, 18));
                g2d.setPaint(bg);
                g2d.fillRoundRect(0, 0, w, h, 16, 16);

                // --- Border emas ---
                g2d.setColor(new Color(120, 90, 40));
                g2d.setStroke(new BasicStroke(2f));
                g2d.drawRoundRect(1, 1, w - 3, h - 3, 16, 16);

                // --- Garis dekorasi bawah title ---
                g2d.setColor(new Color(100, 75, 30));
                g2d.setStroke(new BasicStroke(1f));
                g2d.drawLine(16, 44, w - 16, 44);

                // --- Title ---
                g2d.setFont(new Font("Georgia", Font.BOLD, 16));
                g2d.setColor(new Color(218, 165, 32));
                g2d.drawString("⚙  DEVELOPER MODE", 18, 30);

                // --- Section headers ---
                g2d.setFont(new Font("Georgia", Font.BOLD, 11));
                g2d.setColor(new Color(180, 140, 70));
                g2d.drawString("RESOURCES", 18, 68);
                g2d.drawString("GUARDS", 18, 168);
                g2d.drawString("HORDES", 18, 268);
                g2d.drawString("WORLD", 18, 368);

                // Garis tipis tiap section
                g2d.setColor(new Color(60, 48, 30));
                g2d.drawLine(90, 62, w - 16, 62);
                g2d.drawLine(72, 162, w - 16, 162);
                g2d.drawLine(70, 262, w - 16, 262);
                g2d.drawLine(62, 362, w - 16, 362);

                // --- Live stats ---
                g2d.setFont(new Font("Serif", Font.PLAIN, 11));
                g2d.setColor(new Color(160, 145, 115));
                g2d.drawString("Wood: " + totalWood
                        + "   Civil: " + window.activeCivils.size()
                        + "   Guard: " + window.activeGuards.size()
                        + "   Horde: " + window.activeHordes.size(), 18, h - 14);

                g2d.dispose();
            }
        };
        panel.setOpaque(false);

        // Helper: buat tombol dev bergaya game
        java.util.function.BiFunction<String, Runnable, JButton> mkBtn = (label, action) -> {
            JButton b = new JButton(label) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    boolean hover = getModel().isRollover();
                    boolean press = getModel().isPressed();
                    g2.setColor(press ? new Color(90, 65, 20) : hover ? new Color(55, 44, 28) : new Color(30, 24, 18));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    g2.setColor(hover ? new Color(200, 155, 50) : new Color(100, 75, 35));
                    g2.setStroke(new BasicStroke(1.2f));
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                    g2.setFont(new Font("Serif", Font.BOLD, 11));
                    g2.setColor(hover ? new Color(230, 195, 100) : new Color(185, 160, 100));
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2, (getHeight() + fm.getAscent() - fm.getDescent()) / 2 - 1);
                    g2.dispose();
                }
            };
            b.setContentAreaFilled(false); b.setBorderPainted(false); b.setFocusPainted(false);
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            b.addActionListener(e -> { action.run(); panel.repaint(); });
            return b;
        };

        // Helper: posisi spawn (tengah layar visible)
        java.util.function.Supplier<double[]> spawnPos = () -> {
            double wx = (getWidth() / 2.0 - camera.getX()) / camera.getZoom();
            double wy = (getHeight() / 2.0 - camera.getY()) / camera.getZoom();
            return new double[]{wx, wy};
        };

        int bw = 114, bh = 30, col1 = 18, col2 = 140, col3 = 262, col4 = 390;

        // ===== SECTION: RESOURCES =====
        JButton addWood100  = mkBtn.apply("+100 Wood",  () -> totalWood += 100);
        JButton addWood500  = mkBtn.apply("+500 Wood",  () -> totalWood += 500);
        JButton addWood2000 = mkBtn.apply("+2000 Wood", () -> totalWood += 2000);
        JButton setWood0    = mkBtn.apply("Reset Wood", () -> totalWood = 0);
        addWood100 .setBounds(col1, 76, bw, bh);
        addWood500 .setBounds(col2, 76, bw, bh);
        addWood2000.setBounds(col3, 76, bw, bh);
        setWood0   .setBounds(col4, 76, bw, bh);

        JButton spawnCivil1  = mkBtn.apply("+1 Civil",  () -> { double[] p = spawnPos.get(); window.activeCivils.add(new Civil(p[0], p[1])); });
        JButton spawnCivil5  = mkBtn.apply("+5 Civil",  () -> { double[] p = spawnPos.get(); for (int i = 0; i < 5; i++) window.activeCivils.add(new Civil(p[0] + i * 25, p[1])); });
        JButton spawnCivil20 = mkBtn.apply("+20 Civil", () -> { double[] p = spawnPos.get(); for (int i = 0; i < 20; i++) window.activeCivils.add(new Civil(p[0] + (i % 5) * 28, p[1] + (i / 5) * 28)); });
        JButton killAllCivil = mkBtn.apply("Kill Civils", () -> window.activeCivils.clear());
        spawnCivil1 .setBounds(col1, 113, bw, bh);
        spawnCivil5 .setBounds(col2, 113, bw, bh);
        spawnCivil20.setBounds(col3, 113, bw, bh);
        killAllCivil.setBounds(col4, 113, bw, bh);

        // ===== SECTION: GUARDS =====
        JButton spawnSpear1 = mkBtn.apply("+1 Spearman", () -> { double[] p = spawnPos.get(); window.activeGuards.add(new Guard(Guard.GuardType.SPEARMAN, p[0], p[1])); });
        JButton spawnSpear5 = mkBtn.apply("+5 Spearman", () -> { double[] p = spawnPos.get(); for (int i = 0; i < 5; i++) window.activeGuards.add(new Guard(Guard.GuardType.SPEARMAN, p[0] + i * 28, p[1])); });
        JButton spawnArch1  = mkBtn.apply("+1 Archer",   () -> { double[] p = spawnPos.get(); window.activeGuards.add(new Guard(Guard.GuardType.ARCHER, p[0], p[1])); });
        JButton spawnArch5  = mkBtn.apply("+5 Archer",   () -> { double[] p = spawnPos.get(); for (int i = 0; i < 5; i++) window.activeGuards.add(new Guard(Guard.GuardType.ARCHER, p[0] + i * 28, p[1])); });
        spawnSpear1.setBounds(col1, 176, bw, bh);
        spawnSpear5.setBounds(col2, 176, bw, bh);
        spawnArch1 .setBounds(col3, 176, bw, bh);
        spawnArch5 .setBounds(col4, 176, bw, bh);

        JButton killAllGuard = mkBtn.apply("Kill All Guards", () -> window.activeGuards.clear());
        JButton heal100Guard  = mkBtn.apply("Full HP Guards",  () -> window.activeGuards.forEach(g -> g.currentHp = g.maxHp));
        killAllGuard.setBounds(col1, 213, bw, bh);
        heal100Guard.setBounds(col2, 213, bw, bh);

        // ===== SECTION: HORDES =====
        JButton spawnAxe1    = mkBtn.apply("+1 Axeman",     () -> { double[] p = spawnPos.get(); window.activeHordes.add(new Horde(Horde.HordeType.AXEMAN, p[0], p[1])); });
        JButton spawnShield1 = mkBtn.apply("+1 Shieldbearer",() -> { double[] p = spawnPos.get(); window.activeHordes.add(new Horde(Horde.HordeType.SHIELDBEARER, p[0], p[1])); });
        JButton spawnBow1    = mkBtn.apply("+1 Bowman",     () -> { double[] p = spawnPos.get(); window.activeHordes.add(new Horde(Horde.HordeType.BOWMAN, p[0], p[1])); });
        JButton spawnAll3    = mkBtn.apply("+5 Mixed Horde",() -> { double[] p = spawnPos.get(); for (int i = 0; i < 5; i++) { int t = i % 3; window.activeHordes.add(new Horde(t == 0 ? Horde.HordeType.AXEMAN : t == 1 ? Horde.HordeType.SHIELDBEARER : Horde.HordeType.BOWMAN, p[0] + i * 30, p[1])); } });
        spawnAxe1   .setBounds(col1, 276, bw, bh);
        spawnShield1.setBounds(col2, 276, bw, bh);
        spawnBow1   .setBounds(col3, 276, bw, bh);
        spawnAll3   .setBounds(col4, 276, bw, bh);

        JButton killAllHorde = mkBtn.apply("Kill All Hordes", () -> window.activeHordes.clear());
        JButton spawnHorde20 = mkBtn.apply("+20 Axemen",     () -> { double[] p = spawnPos.get(); for (int i = 0; i < 20; i++) window.activeHordes.add(new Horde(Horde.HordeType.AXEMAN, p[0] + (i % 5) * 30, p[1] + (i / 5) * 30)); });
        killAllHorde.setBounds(col1, 313, bw, bh);
        spawnHorde20.setBounds(col2, 313, bw, bh);

        // ===== SECTION: WORLD =====
        JButton clearBuildings = mkBtn.apply("Clear Buildings", () -> { window.savedBuildings.clear(); });
        JButton clearProj      = mkBtn.apply("Clear Projectiles", () -> window.activeProjectiles.clear());
        JButton clearAll       = mkBtn.apply("Clear Everything", () -> { window.activeGuards.clear(); window.activeHordes.clear(); window.activeProjectiles.clear(); });
        JButton centerCam      = mkBtn.apply("Center Camera", () -> { camera.centerOn(1500, 1500, getWidth(), getHeight()); repaint(); });
        clearBuildings.setBounds(col1, 376, bw, bh);
        clearProj     .setBounds(col2, 376, bw, bh);
        clearAll      .setBounds(col3, 376, bw, bh);
        centerCam     .setBounds(col4, 376, bw, bh);

        // ===== CLOSE BUTTON =====
        JButton closeBtn = new JButton("✕") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean hover = getModel().isRollover();
                g2.setColor(hover ? new Color(160, 40, 30) : new Color(80, 30, 24));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(hover ? new Color(255, 100, 80) : new Color(180, 80, 60));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.setFont(new Font("Serif", Font.BOLD, 13));
                g2.setColor(new Color(240, 200, 190));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString("✕", (getWidth() - fm.stringWidth("✕")) / 2, (getHeight() + fm.getAscent() - fm.getDescent()) / 2 - 1);
                g2.dispose();
            }
        };
        closeBtn.setContentAreaFilled(false); closeBtn.setBorderPainted(false); closeBtn.setFocusPainted(false);
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.setBounds(540 - 38, 8, 28, 28);
        closeBtn.addActionListener(e -> { devPanelVisible = false; panel.setVisible(false); });

        // Add all buttons
        for (JButton b : new JButton[]{
                addWood100, addWood500, addWood2000, setWood0,
                spawnCivil1, spawnCivil5, spawnCivil20, killAllCivil,
                spawnSpear1, spawnSpear5, spawnArch1, spawnArch5,
                killAllGuard, heal100Guard,
                spawnAxe1, spawnShield1, spawnBow1, spawnAll3,
                killAllHorde, spawnHorde20,
                clearBuildings, clearProj, clearAll, centerCam,
                closeBtn
        }) panel.add(b);

        panel.setVisible(false);
        return panel;
    }

    private void showInGameMenu() {
        // ================================================================
        // CUSTOM IN-GAME MENU — desain dark fantasy, bukan default Java UI
        // ================================================================
        JWindow menuWin = new JWindow(SwingUtilities.getWindowAncestor(this));
        menuWin.setBackground(new Color(0, 0, 0, 0));

        // Posisikan di dekat tombol menu (pojok kanan atas)
        java.awt.Point btnLoc = menuBtn.getLocationOnScreen();
        int menuW = 220, menuH = 148;
        menuWin.setBounds(btnLoc.x - menuW + menuBtn.getWidth(), btnLoc.y + menuBtn.getHeight() + 4, menuW, menuH);

        // --- Panel utama custom-painted ---
        JPanel menuPanel = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                // Background
                g2d.setColor(new Color(18, 14, 12, 250));
                g2d.fillRoundRect(0, 0, w, h, 12, 12);
                // Border emas
                g2d.setColor(new Color(100, 75, 35));
                g2d.setStroke(new BasicStroke(1.5f));
                g2d.drawRoundRect(1, 1, w - 3, h - 3, 12, 12);
                g2d.dispose();
            }
        };
        menuPanel.setOpaque(false);
        menuPanel.setBounds(0, 0, menuW, menuH);

        // Helper buat item menu
        java.util.function.BiFunction<String, Runnable, JPanel> mkItem = (label, action) -> {
            JPanel item = new JPanel(null) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    if (getClientProperty("hover") == Boolean.TRUE) {
                        g2.setColor(new Color(50, 40, 25));
                        g2.fillRoundRect(4, 1, getWidth() - 8, getHeight() - 2, 6, 6);
                    }

                    int bx = 12, by = getHeight() / 2 - 7, bs = 14;
                    Color gold = new Color(185, 145, 60);
                    Color goldDark = new Color(100, 75, 25);

                    if (label.equals("Save Progress")) {
                        // --- IKON FLOPPY DISK ---
                        // Body luar
                        g2.setColor(goldDark);
                        g2.fillRect(bx, by, bs, bs);
                        g2.setColor(gold);
                        g2.setStroke(new BasicStroke(1.4f));
                        g2.drawRect(bx, by, bs, bs);
                        // Notch sudut kanan atas (ciri khas floppy)
                        g2.setColor(new Color(18, 14, 12));
                        g2.fillPolygon(new int[]{bx+9, bx+bs, bx+bs}, new int[]{by, by, by+5}, 3);
                        g2.setColor(gold);
                        g2.drawLine(bx+9, by, bx+bs, by+5);
                        // Shutter (kotak kecil tengah atas)
                        g2.setColor(new Color(130, 100, 35));
                        g2.fillRect(bx+3, by, 5, 4);
                        // Label area (kotak bawah)
                        g2.setColor(new Color(130, 100, 35));
                        g2.fillRect(bx+2, by+8, bs-4, bs-9);
                        g2.setColor(gold);
                        g2.setStroke(new BasicStroke(0.8f));
                        g2.drawRect(bx+2, by+8, bs-4, bs-9);

                    } else if (label.equals("Back to Main Menu")) {
                        // --- IKON PANAH KIRI ---
                        // Kepala panah (segitiga kiri)
                        g2.setColor(gold);
                        int[] px = {bx+5, bx+bs, bx+bs};
                        int[] py = {by+7, by+1, by+13};
                        g2.fillPolygon(px, py, 3);
                        // Batang panah
                        g2.fillRect(bx+bs, by+4, 1, 6);
                        // Garis outline kepala
                        g2.setColor(goldDark);
                        g2.setStroke(new BasicStroke(0.8f));
                        g2.drawPolygon(px, py, 3);
                    }

                    // Label teks
                    g2.setFont(new Font("Georgia", Font.PLAIN, 13));
                    g2.setColor(new Color(210, 185, 130));
                    g2.drawString(label, 34, getHeight() / 2 + 5);
                    g2.dispose();
                }
            };
            item.setOpaque(false);
            item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            item.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseEntered(java.awt.event.MouseEvent e) { item.putClientProperty("hover", true); item.repaint(); }
                @Override public void mouseExited(java.awt.event.MouseEvent e)  { item.putClientProperty("hover", false); item.repaint(); }
                @Override public void mouseClicked(java.awt.event.MouseEvent e) { menuWin.dispose(); if (action != null) action.run(); }
            });
            return item;
        };

        // --- Separator line helper ---
        JPanel sep = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(new Color(70, 55, 30)); g.fillRect(10, 0, getWidth() - 20, 1);
            }
        };
        sep.setOpaque(false);

        // ---- ITEM 1: Save Progress ----
        JPanel saveItem = mkItem.apply("Save Progress", () -> {
            String[] options = {"Slot 1", "Slot 2", "Slot 3"};
            int choice = JOptionPane.showOptionDialog(window, "Pilih Slot Penyimpanan:", "Save Progress",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
            if (choice >= 0) saveGameData(choice + 1);
        });
        saveItem.setBounds(0, 8, menuW, 36);

        // ---- Separator 1 ----
        JPanel sep1 = new JPanel() {
            @Override protected void paintComponent(Graphics g) { g.setColor(new Color(60, 48, 28)); g.fillRect(10, 0, getWidth()-20, 1); }
        };
        sep1.setOpaque(false);
        sep1.setBounds(0, 44, menuW, 2);

        // ---- ITEM 2: Developer Mode (checkbox style) ----
        JPanel devItem = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                if (getClientProperty("hover") == Boolean.TRUE) {
                    g2.setColor(new Color(50, 40, 25));
                    g2.fillRoundRect(4, 1, getWidth() - 8, getHeight() - 2, 6, 6);
                }
                // --- Checkbox: digambar langsung Java2D ---
                int bx = 14, by = getHeight() / 2 - 6, bs = 12;
                // Isi kotak: emas kalau aktif, gelap kalau tidak
                g2.setColor(isCheatModeActive ? new Color(160, 120, 35) : new Color(35, 28, 18));
                g2.fillRect(bx, by, bs, bs);
                // Border kotak
                g2.setColor(new Color(185, 145, 60));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRect(bx, by, bs, bs);
                // Centang kalau aktif
                if (isCheatModeActive) {
                    g2.setColor(new Color(20, 14, 8));
                    g2.setStroke(new BasicStroke(2.2f));
                    g2.drawLine(bx + 2, by + 6, bx + 5, by + 9);
                    g2.drawLine(bx + 5, by + 9, bx + 10, by + 3);
                }
                // Label teks
                g2.setFont(new Font("Georgia", Font.PLAIN, 13));
                g2.setColor(new Color(210, 185, 130));
                g2.drawString("Developer Mode", 34, getHeight() / 2 + 5);
                g2.dispose();
            }
        };
        devItem.setOpaque(false);
        devItem.setBounds(0, 46, menuW, 36);
        devItem.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        devItem.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { devItem.putClientProperty("hover", true); devItem.repaint(); }
            @Override public void mouseExited(java.awt.event.MouseEvent e)  { devItem.putClientProperty("hover", false); devItem.repaint(); }
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                isCheatModeActive = !isCheatModeActive;
                devItem.repaint();
            }
        });

        // ---- Separator 2 ----
        JPanel sep2 = new JPanel() {
            @Override protected void paintComponent(Graphics g) { g.setColor(new Color(60, 48, 28)); g.fillRect(10, 0, getWidth()-20, 1); }
        };
        sep2.setOpaque(false);
        sep2.setBounds(0, 82, menuW, 2);

        // ---- ITEM 3: Back to Main Menu ----
        JPanel backItem = mkItem.apply("Back to Main Menu", () -> {
            if (holdingBuilding != null) window.savedBuildings.add(holdingBuilding);
            window.showScreen("MENU_SCREEN");
        });
        backItem.setBounds(0, 84, menuW, 36);

        // ---- Tambah ke panel ----
        menuPanel.add(saveItem);
        menuPanel.add(sep1);
        menuPanel.add(devItem);
        menuPanel.add(sep2);
        menuPanel.add(backItem);
        menuWin.add(menuPanel);

        // --- Tutup kalau klik di luar menu (pakai AWTEventListener, lebih reliable dari windowLostFocus) ---
        java.awt.event.AWTEventListener[] closeListenerHolder = new java.awt.event.AWTEventListener[1];
        closeListenerHolder[0] = event -> {
            if (event instanceof MouseEvent) {
                MouseEvent me = (MouseEvent) event;
                if (me.getID() == MouseEvent.MOUSE_PRESSED) {
                    java.awt.Point clickScreen = me.getLocationOnScreen();
                    if (!menuWin.getBounds().contains(clickScreen)) {
                        menuWin.dispose();
                        java.awt.Toolkit.getDefaultToolkit().removeAWTEventListener(closeListenerHolder[0]);
                    }
                }
            }
        };
        java.awt.Toolkit.getDefaultToolkit().addAWTEventListener(
                closeListenerHolder[0], AWTEvent.MOUSE_EVENT_MASK);

        // Pastikan listener juga dihapus saat window di-dispose (anti-memory leak)
        menuWin.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                java.awt.Toolkit.getDefaultToolkit().removeAWTEventListener(closeListenerHolder[0]);
            }
        });

        menuWin.setVisible(true);
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

        // --- Center kamera ke Heart begitu panel pertama kali digambar dengan ukuran final ---
        if (!cameraInitialized && getWidth() > 0 && getHeight() > 0) {
            camera.centerOn(1500, 1500, getWidth(), getHeight());
            cameraInitialized = true;
        }

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

        List<RenderItem> renderList = new java.util.ArrayList<>();

        for (Building b : window.savedBuildings) {
            BufferedImage finishedImg = getBuildImage(b.type);
            renderList.add(new RenderItem(b.getBounds().getY() + b.getBounds().getHeight(), () -> {
                if (b.type == Building.BuildingType.HEART) {
                    System.out.println("Heart ditemukan, img = " + finishedImg);
                }
                // MENGIRIM 2 GAMBAR SEKALIGUS KE BUILDING.JAVA
                b.draw(g2d, underConstructionImg, finishedImg);

                // --- EFEK HIGHLIGHT KUNING JIKA DIKLIK ---
                if (b == clickedBuilding) {
                    g2d.setColor(new Color(255, 255, 0, 70)); // Kuning transparan
                    Rectangle hitbox = b.getSolidHitbox(); // Ambil ukuran pondasinya saja
                    g2d.fillRect(hitbox.x, hitbox.y, hitbox.width, hitbox.height);
                }
            }));
        }
        // 2. Masukkan Civil
        for (Civil c : window.activeCivils) {
            renderList.add(new RenderItem(c.y + c.size, () -> c.draw(g2d, civilImg)));
        }

        for (CivilBuilder cb : window.activeCivilBuilders) {
            if (cb.state != CivilBuilder.BuilderState.IDLE_HOME) { // Cuma kelihatan kalau lagi keluar rumah
                renderList.add(new RenderItem(cb.y + cb.size, () -> cb.draw(g2d, civilBuilderImg)));
            }
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
        for (List<Tree> daftarPohon : mapPohon.values()) {
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
        int viewX = (int) (-camera.getX() / camera.getZoom());
        int viewY = (int) (-camera.getY() / camera.getZoom());
        int viewW = (int) (getWidth() / camera.getZoom());
        int viewH = (int) (getHeight() / camera.getZoom());
        fogOfWar.draw(g2d, new Rectangle(viewX, viewY, viewW, viewH));
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
                    if(isOverlapping(new Rectangle(x, dragStartPoint.y, 10, 10), null) || isTreeBlocking(new Rectangle(x, dragStartPoint.y, 10, 10))) {
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
                    if(isOverlapping(new Rectangle(dragStartPoint.x, y, 10, 10), null) || isTreeBlocking(new Rectangle(dragStartPoint.x, y, 10, 10))) {
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
            Building.BuildingType typeToDraw = (currentTool == ToolMode.BUILD) ? selectedBuilding : holdingBuilding.type;

            // --- HITUNG UKURAN PONDASI (PAKAI LOGIKA YANG SAMA DENGAN Building.getSolidHitbox) ---
            // Supaya preview-nya persis sama bentuknya dengan hitbox asli setelah bangunan jadi ditaruh.
            Rectangle solidPreview = Building.computeSolidHitbox(pX, pY, bw, bw, typeToDraw);

            if (isOverlapping(previewRect, ignoreB) || isTreeBlocking(previewRect)) {
                g2d.setColor(new Color(255, 0, 0, 150));
                // HANYA GAMBAR AREA MERAH DI PONDASI BAWAHNYA SAJA!
                g2d.fillRect(solidPreview.x, solidPreview.y, solidPreview.width, solidPreview.height);
            } else {
                if (previewImg != null) {
                    int drawW = Building.getVisualWidth(typeToDraw);
                    int drawH = Building.getVisualHeight(typeToDraw);

                    int drawX = pX - ((drawW - bw) / 2);
                    int drawY = pY - (drawH - bw);

                    g2d.drawImage(previewImg, drawX, drawY, drawW, drawH, null);
                } else {
                    g2d.setColor(new Color(200, 200, 200, 150));
                    g2d.fillRect(solidPreview.x, solidPreview.y, solidPreview.width, solidPreview.height);
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
                List<Tree> daftarPohon = new java.util.ArrayList<>();

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