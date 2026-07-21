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
    GameWindow window; // Referensi ke Bos
    private BufferedImage gameplayBg;
    private BufferedImage smallHouseImg;
    private BufferedImage mediumHouseImg;
    private BufferedImage bigHouseImg;
    BufferedImage archerImg;
    BufferedImage spearmanImg;
    private BufferedImage axemanImg;
    private BufferedImage shieldImg;
    private BufferedImage bowmanHordeImg;
    private BufferedImage bearImg, twoAxeImg, logHordeImg, sorcererImg;
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

    // --- FITUR BARU: ICON RESOURCE BAR (PNG asli, gantiin emoji) ---
    private BufferedImage iconMilitary, iconWave, iconStone, iconWood, iconSteel, iconFood, iconCivil;

    // --- ADT Kamera ---
    Camera camera = new Camera();
    private Timer cameraTimer;

    // --- Variabel Input ---
    boolean wPressed = false, sPressed = false, aPressed = false, dPressed = false;

    // --- Variabel Alat & Bangunan ---
    enum ToolMode { NONE, BUILD, MOVE, DELETE, COMMAND, CHOP_WOOD }
    ToolMode currentTool = ToolMode.NONE;
    int mouseX = -100, mouseY = -100;
    Point dragStartScreen = null;
    Point dragEndScreen = null;
    boolean isDragging = false;
    Building holdingBuilding = null;
    boolean isDraggingWall = false;
    Point dragStartPoint = new Point();
    Point dragCurrentPoint = new Point();
    public List<Mine> activeMines = new java.util.ArrayList<>();
    Mine clickedMine = null;
    private boolean cameraInitialized = false;

    // --- TAMBAHAN BARU: Status Cheat Mode ---
    boolean isCheatModeActive = false;
    // --- FITUR BARU: DEVELOPER PANEL ---
    private boolean devPanelVisible = false;
    private JPanel devPanel;

    // --- Variabel UI/HUD ---
    JPanel bottomLeftBar;
    JPanel topRightBar;
    private JButton menuBtn;

    private boolean isBuildMenuExpanded = false;
    private JButton house1Btn, house2Btn, house3Btn;

    // --- VARIABEL GRID MENU & ALAT ---
    enum MenuState { CLOSED, MAIN_MENU, CIVIL_MENU, MILITARY_MENU, BUILDING_SELECTED, MINE_SELECTED, BARRACK_MENU }
    MenuState currentMenuState = MenuState.MAIN_MENU;
    Building.BuildingType selectedBuilding = Building.BuildingType.MEDIUM_HOUSE;
    JPanel gridMenuPanel;
    Building clickedBuilding = null;
    private BufferedImage civilBuilderImg;

    // --- FITUR BARU: SISTEM PRODUKSI GUARD DARI BARRACK ---
    // Setiap barrack punya antrian Guard-nya sendiri (HashMap supaya bisa beda-beda tiap bangunan)
    java.util.HashMap<Building, java.util.Queue<Guard.GuardType>> barrackQueues = new java.util.HashMap<>();
    private java.util.HashMap<Building, Float> barrackProgress = new java.util.HashMap<>();
    private final float GUARD_PRODUCTION_MAX = 300f; // 300 tick ~= 5 detik @ 60fps
    JPanel barrackQueuePanel; // Panel antrean di sebelah kanan grid menu

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

    // --- DIEKSTRAK ke ResourceManager.java (ekstraksi #3, tidak ada perubahan logic) ---
    ResourceManager resourceManager = new ResourceManager();

    // --- DIEKSTRAK ke InputController.java (ekstraksi #8, friend class -- lihat catatan di file itu) ---
    private final InputController inputController = new InputController(this);

    private FogOfWar fogOfWar;
    private static final float HEART_REVEAL_RADIUS = 500f;
    private static final float GUARD_REVEAL_RADIUS = 300f;

    // --- DIEKSTRAK ke WaveManager.java (ekstraksi #2, tidak ada perubahan logic) ---
    private WaveManager waveManager = new WaveManager();
    private JPanel waveCountdownBar;

    //===============================TREE====================================================
    // Hashtable (Spatial Hash) untuk menyimpan pohon berdasarkan petak Kavling
    public java.util.HashMap<String, List<Tree>> mapPohon = new java.util.HashMap<>();
    // Variabel untuk menyimpan gambar pohon
    private BufferedImage treeImg;
    //===================================================================================

    // --- FITUR BARU: RESET CAMPAIGN ---
    // Dipanggil pas GamePanel pertama kali dibuat (constructor) MAUPUN pas tombol
    // CAMPAIGN diklik ulang dari menu. Membersihkan SEMUA sisa data sesi sebelumnya
    // (building, civil, guard, horde, civil builder, projectile, mine, pohon) dulu,
    // baru bikin ulang dunia dari nol -> gak ada lagi sisa data yang numpuk/nyangkut.
    public void resetCampaign() {
        resetCampaign(GameWindow.Difficulty.MEDIUM);
    }

    // --- FITUR BARU: overload resetCampaign() yang menerima Difficulty dari pilihan menu CAMPAIGN.
    // Selain membersihkan & membangun ulang dunia seperti biasa, ini juga menyalakan ulang
    // sistem wave (currentWaveIndex, countdown) sesuai difficulty yang dipilih.
    public void resetCampaign(GameWindow.Difficulty difficulty) {
        // 1. Bersihkan semua list state lama
        window.savedBuildings.clear();
        window.activeCivils.clear();
        window.activeGuards.clear();
        window.activeHordes.clear();
        window.activeCivilBuilders.clear();
        window.activeProjectiles.clear();
        activeMines.clear();
        mapPohon.clear();

        // 2. Reset progres dasar
        currentDay = 1;
        isDayTime = true;
        currentDarkness = 0f;
        resourceManager.reset();

        // --- FITUR BARU: Reset sistem wave -> mulai hitung mundur ke Wave I sesuai difficulty
        // (durasi & isi wave sekarang 100% dibaca dari DifficultyConfig.java, bukan formula random) ---
        waveManager.reset(difficulty);

        // 3. Tumbuhkan ulang hutan & tambang
        generateForest();
        generateMines();

        // 4. Posisi tengah map (map 3000 x 3000) -> bikin Heart baru
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

        window.activeCivilBuilders.add(new CivilBuilder(
                heart.getBounds().getCenterX(),
                heart.getBounds().getCenterY(),
                heart
        ));

        for (int i = 0; i < 5; i++) {
            window.activeCivils.add(new Civil(1500, 1500));
        }

        repaint();
    }

    // --- spawnWave() DIEKSTRAK ke WaveManager.java (ekstraksi #2, tidak ada perubahan logic) ---

    public GamePanel(GameWindow window) {
        this.window = window;
        setLayout(null);

        // --- Setup awal campaign (forest, mine, Heart, CivilBuilder, Civil awal) ---
        // Disatukan ke resetCampaign() biar constructor & tombol CAMPAIGN di menu selalu konsisten.
        resetCampaign();

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
            bearImg = ImageIO.read(new File("assets/img/Bear_Horde.png"));
            twoAxeImg = ImageIO.read(new File("assets/img/Horde_2axe.png"));
            logHordeImg = ImageIO.read(new File("assets/img/Log_horde.png"));
            sorcererImg = ImageIO.read(new File("assets/img/sorcerer_Horde.png"));
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
            iconMilitary = ImageIO.read(new File("assets/img/Military-removebg-preview.png"));
            iconWave = ImageIO.read(new File("assets/img/Wafe-removebg-preview.png"));
            iconStone = ImageIO.read(new File("assets/img/Stone-removebg-preview.png"));
            iconWood = ImageIO.read(new File("assets/img/Wood-removebg-preview.png"));
            iconSteel = ImageIO.read(new File("assets/img/Steel-removebg-preview.png"));
            iconFood = ImageIO.read(new File("assets/img/food-removebg-preview.png"));
            iconCivil = ImageIO.read(new File("assets/img/Civil-removebg-preview.png"));
        } catch (Exception e) {
            System.out.println("Gagal memuat icon resource bar!");
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
                g.update(window.activeGuards, window.activeHordes, window.activeProjectiles, window.savedBuildings);
                fogOfWar.revealIfMoved(g, g.x, g.y, GUARD_REVEAL_RADIUS);
            }
            fogOfWar.update();

            // 2. Update Hordes (Parameter Nambah -- sekarang bawa data Civil juga biar bisa niatin warga sipil)
            for (Horde h : window.activeHordes) {
                h.update(window.activeHordes, window.activeGuards, window.activeProjectiles, window.activeCivils, window.savedBuildings);
            }

            // --- UPDATE CIVIL (Jalan-jalan santai, kabur kalau ada Horde deket, pakai graph biar gak nabrak rumah) ---
            for (Civil c : window.activeCivils) {
                c.update(window.activeHordes, window.savedBuildings);
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
                        boolean panahMenancap = false;
                        for (Guard guard : window.activeGuards) {
                            if (new Rectangle2D.Double(guard.x, guard.y, guard.size, guard.size).intersects(pHitbox)) {
                                guard.currentHp -= p.getDamage();
                                p.active = false; // Panah hilang menancap di badan
                                panahMenancap = true;
                                break;
                            }
                        }
                        // --- FITUR BARU: Cek kena Civil juga (kalau panah Bowman niatin warga sipil) ---
                        if (!panahMenancap) {
                            for (Civil c : window.activeCivils) {
                                if (c.hidden) continue; // Civil yang lagi ngumpet di rumah gak bisa kena panah
                                if (new Rectangle2D.Double(c.x, c.y, c.size, c.size).intersects(pHitbox)) {
                                    c.currentHp -= p.getDamage();
                                    p.active = false;
                                    break;
                                }
                            }
                        }
                    }
                }
            }

            // 4. Bersihkan data (Mayat & Panah non-aktif)
            window.activeGuards.removeIf(g -> g.currentHp <= 0);
            window.activeHordes.removeIf(Horde::isDead);
            window.activeCivils.removeIf(c -> c.currentHp <= 0); // --- FITUR BARU: Civil yang HP-nya habis dihapus juga

            // --- LOGIKA PROGRESS TEBANG POHON ---
            for (List<Tree> daftarPohon : mapPohon.values()) {
                java.util.Iterator<Tree> it = daftarPohon.iterator();
                while (it.hasNext()) {
                    Tree pohon = it.next();
                    if (pohon.harvestProgress >= pohon.maxHarvest) {
                        it.remove();
                        resourceManager.addWood(5);
                    }
                }
            }

            // --- LOGIKA PROGRESS MENGHANCURKAN BANGUNAN ---
            for (Building b : window.savedBuildings) {
                if (b.isDemolishing) b.demolishProgress += 1.0f;
            }
            // Bersihkan jika sudah hancur total
            window.savedBuildings.removeIf(b -> b.isDemolishing && b.demolishProgress >= b.maxDemolish);

            // --- FITUR BARU: Bersihkan building yang darahnya habis diserang Horde ---
            window.savedBuildings.removeIf(b -> b.isBuilt && b.currentHp <= 0);

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

            // --- FITUR BARU: SISTEM ANTREAN CIVIL BUILDER UNTUK POHON ---
            // (Dipindah keluar dari loop Mine supaya SELALU jalan tiap tick, bukan cuma
            // pas kebetulan ada Mine yang lagi dibangun.)
            for (List<Tree> daftarPohon : mapPohon.values()) {
                for (Tree pohon : daftarPohon) {
                    if (pohon.isHarvesting && pohon.assignedBuilder == null) {
                        CivilBuilder chosenBuilder = null;
                        for (CivilBuilder cb : window.activeCivilBuilders) {
                            if (cb.state == CivilBuilder.BuilderState.IDLE_HOME) {
                                chosenBuilder = cb;
                                break;
                            }
                        }
                        if (chosenBuilder == null) {
                            for (CivilBuilder cb : window.activeCivilBuilders) {
                                int load = cb.buildQueue.size() + cb.chopQueue.size();
                                int chosenLoad = (chosenBuilder == null) ? Integer.MAX_VALUE
                                        : chosenBuilder.buildQueue.size() + chosenBuilder.chopQueue.size();
                                if (load < chosenLoad) chosenBuilder = cb;
                            }
                        }
                        if (chosenBuilder != null) chosenBuilder.queueChop(pohon, window.savedBuildings);
                    }
                }
            }

            // --- LOGIKA PEMBANGUNAN TAMBANG: SEKARANG BUTUH CIVILBUILDER ---
            // Progress buildProgress-nya sekarang ditangani oleh CivilBuilder.update() (case MINING),
            // di sini cuma tugas nyariin & ngirim builder ke Mine yang lagi butuh dibangun.
            for (Mine m : activeMines) {
                if (m.isBuilding && !m.isBuilt && m.assignedBuilder == null) {
                    CivilBuilder chosenBuilder = null;
                    for (CivilBuilder cb : window.activeCivilBuilders) {
                        if (cb.state == CivilBuilder.BuilderState.IDLE_HOME) {
                            chosenBuilder = cb;
                            break;
                        }
                    }
                    if (chosenBuilder == null) {
                        for (CivilBuilder cb : window.activeCivilBuilders) {
                            int load = cb.buildQueue.size() + cb.chopQueue.size() + cb.mineQueue.size();
                            int chosenLoad = (chosenBuilder == null) ? Integer.MAX_VALUE
                                    : chosenBuilder.buildQueue.size() + chosenBuilder.chopQueue.size() + chosenBuilder.mineQueue.size();
                            if (load < chosenLoad) chosenBuilder = cb;
                        }
                    }
                    if (chosenBuilder != null) chosenBuilder.queueMine(m, window.savedBuildings);
                }
            }

            // --- TAMBAHKAN PEMBERSIH PANAH NON-AKTIF ---
            window.activeProjectiles.removeIf(p -> !p.active);

            // --- FITUR BARU: Setiap Storage yang sudah jadi, nambah kapasitas max resource +25 ---
            int builtStorageCount = 0;
            for (Building b : window.savedBuildings) {
                if (b.isBuilt && b.type == Building.BuildingType.STORAGE) builtStorageCount++;
            }
            resourceManager.updateStorageCapacity(builtStorageCount);

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

                // --- FITUR BARU: Setiap Farm yang sudah jadi, nambah 5 food tiap pagi ---
                int farmCount = 0;
                for (Building b : window.savedBuildings) {
                    if (b.isBuilt && b.type == Building.BuildingType.FARM) farmCount++;
                }
                // farmCount dipakai di resourceManager.applyMorningIncome() di bawah

                // --- FITUR BARU: Setiap Mine yang sudah jadi, nambah 5 stone & 5 steel tiap pagi ---
                int builtMineCount = 0;
                for (Mine m : activeMines) {
                    if (m.isBuilt) builtMineCount++;
                }
                resourceManager.applyMorningIncome(farmCount, builtMineCount);
            }

            // Transisi pergantian langit super halus perlahan-lahan
            float targetDarkness = isDayTime ? 0.0f : MAX_DARKNESS;
            currentDarkness += (targetDarkness - currentDarkness) * 0.005f;

            // --- FITUR BARU: 6. HITUNG MUNDUR & SPAWN WAVE HORDE (Easy/Medium/Hard) ---
            waveManager.tick(window);
            if (waveCountdownBar != null) waveCountdownBar.repaint();

            repaint(); // (Ini bawaan aslinya, pastikan ini tetap di posisi paling bawah)

            if (topRightBar != null) topRightBar.repaint(); // Resource bar (icon+bar) update tiap tick
            if (gridMenuPanel != null && gridMenuPanel.isVisible()) gridMenuPanel.repaint(); // Warna cost real-time
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
                    topRightBar.setBounds(w - 830, 0, 830, 56);
                    menuBtn.setBounds(topRightBar.getWidth() - 40, 4, 32, 32);
                    // --- FITUR BARU: posisikan bar hitung mundur wave tepat di bawah topRightBar, rata kanan ---
                    if (waveCountdownBar != null) waveCountdownBar.setBounds(w - 420, 62, 420, 30);
                    bottomLeftBar.setBounds(15, h - 310, 180, 80);
                    if (gridMenuPanel != null) gridMenuPanel.setBounds(230, h - 200, 240, 180);
                    if (barrackQueuePanel != null) barrackQueuePanel.setBounds(475, h - 200, 180, 180);
                    if (devPanel != null) devPanel.setBounds(w / 2 - 270, h / 2 - 230, 540, 460);
                }
            }
        });
    }

    private void setupKeyBindings() {
        inputController.setupKeyBindings();
    }

    private void setupMouseListeners() {
        inputController.setupMouseListeners();
    }

    boolean isOverlapping(Rectangle newRect, Building ignoreBuilding) {
        for (Building b : window.savedBuildings) {
            if (b != ignoreBuilding && b.intersects(newRect)) return true;
        }
        return false;
    }

    // --- CEK APAKAH MASIH ADA POHON DI AREA YANG MAU DIBANGUN ---
    // Selama pohonnya belum habis ditebang (masih ada di mapPohon), area itu tidak boleh dibangun.
    boolean isTreeBlocking(Rectangle newRect) {
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
    int getBuildWidth(Building.BuildingType type) {
        if (type == Building.BuildingType.WALL_L || type == Building.BuildingType.WALL_R || type == Building.BuildingType.WALL_UD) return 10;
        if (type == Building.BuildingType.SMALL_HOUSE) return 70;
        if (type == Building.BuildingType.BIG_HOUSE) return 90;
        if (type == Building.BuildingType.BUILDER) return 50;
        return 70;
    }

    int getBuildCapacity(Building.BuildingType type) {
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
        inputController.updateGridMenu();
    }

    // --- TOMBOL GRID DENGAN EFEK MENGGELAP (BUKAN HIJAU) ---
    // --- Tampilan tombol DIEKSTRAK ke GridButtonFactory.java (ekstraksi #7, tidak ada perubahan
    // logic/tampilan). Wrapper ini sengaja dipertahankan supaya updateGridMenu() & titik pakai lain
    // tidak perlu diubah sama sekali -- cukup ganti isinya jadi delegasi. ---
    JButton createGridBtn(String text, Runnable action, boolean isSelected) {
        return GridButtonFactory.createGridBtn(text, action, isSelected);
    }

    JButton createGridBtnWithImage(BufferedImage img, String label, Runnable action) {
        return GridButtonFactory.createGridBtnWithImage(img, label, action);
    }

    JButton createGridBtnBuilding(Building.BuildingType type, Runnable action, boolean isSelected) {
        BufferedImage img = getBuildImage(type);
        int cost = Building.getWoodCost(type);
        return GridButtonFactory.createGridBtnBuilding(img, cost, resourceManager, iconWood, action, isSelected);
    }

    JPanel createCivilCountDisplay() {
        return GridButtonFactory.createCivilCountDisplay(window);
    }

    // --- resourceTooltips & resourceHitboxes DIEKSTRAK ke HudPanel.java (ekstraksi #4) ---

    private void setupHUD() {
        // --- HUD KANAN ATAS: DIEKSTRAK ke HudPanel.java (ekstraksi #4, tidak ada perubahan logic/tampilan) ---
        HudPanel hudPanel = new HudPanel(window, resourceManager, waveManager, () -> currentDay,
                iconWave, iconWood, iconStone, iconSteel, iconFood, iconCivil, iconMilitary,
                this::showInGameMenu);
        topRightBar = hudPanel;
        menuBtn = hudPanel.menuBtn;
        add(topRightBar);

        // (Resource bar sekarang ngitung & gambar semuanya langsung di paintComponent tiap repaint,
        // jadi gak perlu lagi manggil updateResourceBar() di sini.)

        // --- FITUR BARU: BAR HITUNG MUNDUR WAVE, di bawah topRightBar, teks rata kanan ---
        waveCountdownBar = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();
                int margin = 10;

                // Background tipis senada topRightBar
                g2d.setColor(new Color(18, 14, 12, 210));
                g2d.fillRoundRect(0, 0, w, h, 6, 6);
                g2d.setColor(new Color(100, 75, 35));
                g2d.setStroke(new BasicStroke(1.2f));
                g2d.drawRoundRect(0, 0, w - 1, h - 1, 6, 6);

                String text;
                Color textColor;
                if (waveManager.isAllWavesSpawned() && window.activeHordes.isEmpty()) {
                    text = "SEMUA WAVE TUNTAS — Cryonia Selamat";
                    textColor = new Color(140, 220, 150);
                } else if (waveManager.isAllWavesSpawned()) {
                    text = "WAVE " + WaveManager.WAVE_ROMAN[WaveManager.MAX_WAVE - 1] + " — WAVE TERAKHIR!";
                    textColor = new Color(230, 90, 80);
                } else if (waveManager.isWaveInProgress()) {
                    // --- FITUR BARU: selama Horde wave sekarang masih hidup, countdown lagi dibekukan
                    // di WaveManager -> jangan nampilin "00:00" beku, tampilin status "lagi diserbu" ---
                    int attackingWaveIdx = waveManager.getCurrentWaveIndex() - 1;
                    text = "WAVE " + WaveManager.WAVE_ROMAN[attackingWaveIdx] + " sedang menyerbu — habisi semua Horde!";
                    float pulse = 0.55f + 0.45f * (float) Math.sin(System.currentTimeMillis() * 0.01);
                    textColor = new Color(230, (int) (90 * pulse) + 40, (int) (80 * pulse) + 30);
                } else {
                    int totalSeconds = waveManager.getWaveCountdownTicks() / 60;
                    int minutes = totalSeconds / 60;
                    int seconds = totalSeconds % 60;
                    String timeStr = String.format("%02d:%02d", minutes, seconds);
                    text = "WAVE " + WaveManager.WAVE_ROMAN[waveManager.getCurrentWaveIndex()] + " datang dalam " + timeStr;
                    // Merah berkedip pelan begitu hitung mundur mepet (< 10 detik)
                    if (totalSeconds < 10) {
                        float pulse = 0.55f + 0.45f * (float) Math.sin(System.currentTimeMillis() * 0.01);
                        textColor = new Color(230, (int) (90 * pulse) + 40, (int) (80 * pulse) + 30);
                    } else {
                        textColor = new Color(230, 200, 150);
                    }
                }

                g2d.setFont(new Font("Serif", Font.BOLD, 15));
                FontMetrics fm = g2d.getFontMetrics();
                int textX = w - margin - fm.stringWidth(text); // --- rata kanan (align right) ---
                int textY = (h + fm.getAscent() - fm.getDescent()) / 2;

                g2d.setColor(new Color(0, 0, 0, 160));
                g2d.drawString(text, textX + 1, textY + 1);
                g2d.setColor(textColor);
                g2d.drawString(text, textX, textY);

                g2d.dispose();
            }
        };
        waveCountdownBar.setOpaque(false);
        add(waveCountdownBar);

        bottomLeftBar = new JPanel(); bottomLeftBar.setLayout(null); bottomLeftBar.setOpaque(false);

        int mainSize = 76; int subSize = 36; int gap = 4; int startX2 = mainSize + 8;

        JButton buildBtn = createColorButton(new Color(110, 55, 25), ToolMode.NONE, "🔨");
        buildBtn.setBounds(0, 0, mainSize, mainSize);
        buildBtn.addActionListener(e -> {
            currentMenuState = MenuState.MAIN_MENU;
            gridMenuPanel.setVisible(true);
            updateGridMenu();
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
        gridMenuPanel.setBounds(230, getHeight() - 200, 240, 180); gridMenuPanel.setVisible(true);
        updateGridMenu(); // --- FIX: isi grid dari awal, jangan tunggu klik palu dulu ---

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
        devPanel = newDevPanel();
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
    // --- DIEKSTRAK ke DevPanel.java (ekstraksi #5, tidak ada perubahan logic/tampilan) ---
    private DevPanel newDevPanel() {
        return new DevPanel(resourceManager, window, camera, this::getWidth, this::getHeight,
                () -> devPanelVisible = false);
    }

    // --- DIEKSTRAK ke InGameMenu.java (ekstraksi #6, tidak ada perubahan logic/tampilan) ---
    private void showInGameMenu() {
        InGameMenu.show(this, menuBtn, window, activeMines,
                () -> holdingBuilding,
                () -> isCheatModeActive,
                v -> isCheatModeActive = v);
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
            else if (h.type == Horde.HordeType.BEAR) imgToUse = bearImg;
            else if (h.type == Horde.HordeType.TWO_AXE) imgToUse = twoAxeImg;
            else if (h.type == Horde.HordeType.LOG) imgToUse = logHordeImg;
            else if (h.type == Horde.HordeType.SORCERER) imgToUse = sorcererImg;

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

    // --- DIEKSTRAK ke WorldGenerator.java (ekstraksi #1, tidak ada perubahan logic) ---
    private void generateForest() {
        WorldGenerator.generateForest(mapPohon);
    }

    private void generateMines() {
        WorldGenerator.generateMines(activeMines);
    }
}