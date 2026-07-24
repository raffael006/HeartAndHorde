import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

/**
 * Input controller: keybinding (WASD/tool shortcut/cheat key), mouse listener (build/move/
 * delete/command + drag-wall + drag-select), dan state machine Build Menu (updateGridMenu()).
 * Diekstrak dari GamePanel apa adanya (logic tidak diubah) -- TIDAK memakai style
 * "suntik dependency lewat constructor" seperti ekstraksi sebelumnya, karena bagian ini
 * menyentuh 20+ field GamePanel sekaligus (currentTool, holdingBuilding, clickedBuilding,
 * clickedMine, selectedBuilding, currentMenuState, drag state, dll) yang juga saling
 * dibaca-tulis oleh paintComponent() dan setupHUD() yang TIDAK ikut pindah.
 *
 * Makanya class ini pakai pola "friend class": pegang referensi langsung ke GamePanel (gp),
 * dan field-field terkait di GamePanel sengaja diubah dari private jadi package-private
 * (tanpa modifier) supaya bisa diakses dari sini -- masih aman karena tidak ada file lain
 * di luar package (default package) ini yang bisa mengaksesnya.
 */
public class InputController {

    private final GamePanel gp;

    public InputController(GamePanel gamePanel) {
        this.gp = gamePanel;
    }

    public void setupKeyBindings() {
        // Logika Pan/Geser (WASD - Anti Hilang Fokus)
        gp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_W, 0, false), "wPress");
        gp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_W, 0, true), "wRelease");
        gp.getActionMap().put("wPress", new AbstractAction() { public void actionPerformed(ActionEvent e) { gp.wPressed = true; }});
        gp.getActionMap().put("wRelease", new AbstractAction() { public void actionPerformed(ActionEvent e) { gp.wPressed = false; }});

        gp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_S, 0, false), "sPress");
        gp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_S, 0, true), "sRelease");
        gp.getActionMap().put("sPress", new AbstractAction() { public void actionPerformed(ActionEvent e) { gp.sPressed = true; }});
        gp.getActionMap().put("sRelease", new AbstractAction() { public void actionPerformed(ActionEvent e) { gp.sPressed = false; }});

        gp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_A, 0, false), "aPress");
        gp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_A, 0, true), "aRelease");
        gp.getActionMap().put("aPress", new AbstractAction() { public void actionPerformed(ActionEvent e) { gp.aPressed = true; }});
        gp.getActionMap().put("aRelease", new AbstractAction() { public void actionPerformed(ActionEvent e) { gp.aPressed = false; }});

        gp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0, false), "dPress");
        gp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0, true), "dRelease");
        gp.getActionMap().put("dPress", new AbstractAction() { public void actionPerformed(ActionEvent e) { gp.dPressed = true; }});
        gp.getActionMap().put("dRelease", new AbstractAction() { public void actionPerformed(ActionEvent e) { gp.dPressed = false; }});

        gp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_B, 0), "toolBuild");
        gp.getActionMap().put("toolBuild", new AbstractAction() { public void actionPerformed(ActionEvent e) { gp.currentTool = GamePanel.ToolMode.BUILD; gp.holdingBuilding = null; gp.repaint(); }});

        gp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_M, 0), "toolMove");
        gp.getActionMap().put("toolMove", new AbstractAction() { public void actionPerformed(ActionEvent e) { gp.currentTool = GamePanel.ToolMode.MOVE; gp.repaint(); }});

        gp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_X, 0), "toolDelete");
        gp.getActionMap().put("toolDelete", new AbstractAction() { public void actionPerformed(ActionEvent e) { gp.currentTool = GamePanel.ToolMode.DELETE; gp.holdingBuilding = null; gp.repaint(); }});

        // --- UPDATE PENGGUNAAN CHEAT: HANYA BISA JIKA CHEAT MODE AKTIF ---
        gp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_K, 0), "cheatSpawnSpearman");
        gp.getActionMap().put("cheatSpawnSpearman", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (gp.isCheatModeActive) { gp.window.activeGuards.add(new Guard(Guard.GuardType.SPEARMAN, gp.mouseX, gp.mouseY)); gp.repaint(); }
            }
        });

        gp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_L, 0), "cheatSpawnArcher");
        gp.getActionMap().put("cheatSpawnArcher", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (gp.isCheatModeActive) { gp.window.activeGuards.add(new Guard(Guard.GuardType.ARCHER, gp.mouseX, gp.mouseY)); gp.repaint(); }
            }
        });

        gp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_7, 0), "spawnAxeman");
        gp.getActionMap().put("spawnAxeman", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (gp.isCheatModeActive) { gp.window.activeHordes.add(new Horde(Horde.HordeType.AXEMAN, gp.mouseX, gp.mouseY)); gp.repaint(); }
            }
        });

        gp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_8, 0), "spawnShield");
        gp.getActionMap().put("spawnShield", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (gp.isCheatModeActive) { gp.window.activeHordes.add(new Horde(Horde.HordeType.SHIELDBEARER, gp.mouseX, gp.mouseY)); gp.repaint(); }
            }
        });

        gp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_9, 0), "spawnBowmanHorde");
        gp.getActionMap().put("spawnBowmanHorde", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (gp.isCheatModeActive) { gp.window.activeHordes.add(new Horde(Horde.HordeType.BOWMAN, gp.mouseX, gp.mouseY)); gp.repaint(); }
            }
        });

        gp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_0, 0), "spawnBear");
        gp.getActionMap().put("spawnBear", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (gp.isCheatModeActive) { gp.window.activeHordes.add(new Horde(Horde.HordeType.BEAR, gp.mouseX, gp.mouseY)); gp.repaint(); }
            }
        });

        gp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, 0), "spawnTwoAxe");
        gp.getActionMap().put("spawnTwoAxe", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (gp.isCheatModeActive) { gp.window.activeHordes.add(new Horde(Horde.HordeType.TWO_AXE, gp.mouseX, gp.mouseY)); gp.repaint(); }
            }
        });

        gp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, 0), "spawnLog");
        gp.getActionMap().put("spawnLog", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (gp.isCheatModeActive) { gp.window.activeHordes.add(new Horde(Horde.HordeType.LOG, gp.mouseX, gp.mouseY)); gp.repaint(); }
            }
        });

        gp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_OPEN_BRACKET, 0), "spawnSorcerer");
        gp.getActionMap().put("spawnSorcerer", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (gp.isCheatModeActive) { gp.window.activeHordes.add(new Horde(Horde.HordeType.SORCERER, gp.mouseX, gp.mouseY)); gp.repaint(); }
            }
        });

        // --- CHEAT SPAWN CIVIL (Tekan P) ---
        gp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_P, 0), "spawnCivil");
        gp.getActionMap().put("spawnCivil", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (gp.isCheatModeActive) { gp.window.activeCivils.add(new Civil(gp.mouseX, gp.mouseY)); gp.repaint(); }
            }
        });
    }

    public void setupMouseListeners() {
        // Zoom Kamera
        gp.addMouseWheelListener(e -> {
            if (e.isControlDown()) {
                gp.camera.zoomInOut(e.getWheelRotation(), gp.getWidth(), gp.getHeight());
                gp.repaint();
            }
        });

        // Pergerakan Kursor (Preview Bangunan & Fitur Drag-Select)
        gp.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                Point worldPos = gp.camera.toWorld(e.getX(), e.getY());
                gp.mouseX = worldPos.x;
                gp.mouseY = worldPos.y;
                if (gp.currentTool != GamePanel.ToolMode.NONE) gp.repaint();
            }

            // --- TAMBAHAN BARU UNTUK NGE-BLOK ---
            @Override
            public void mouseDragged(MouseEvent e) {
                if (gp.currentTool == GamePanel.ToolMode.COMMAND && SwingUtilities.isLeftMouseButton(e)) {
                    gp.isDragging = true;
                    gp.dragEndScreen = e.getPoint();
                    gp.repaint();
                }
                if (gp.isDraggingWall) {
                    Point worldPos = gp.camera.toWorld(e.getX(), e.getY());
                    int bw = 10;
                    int rawTargetX = worldPos.x - (bw / 2);
                    int rawTargetY = worldPos.y - (bw / 2);
                    gp.dragCurrentPoint.x = Math.round((float) rawTargetX / 10f) * 10;
                    gp.dragCurrentPoint.y = Math.round((float) rawTargetY / 10f) * 10;
                    gp.repaint();
                }
            }
        });

        // Logika Klik (Bangun, Pindah, Hapus)
        gp.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // Cegah nembus HUD
                if (gp.bottomLeftBar != null && gp.topRightBar != null) {
                    if (gp.bottomLeftBar.getBounds().contains(e.getPoint()) || gp.topRightBar.getBounds().contains(e.getPoint())) return;
                }

                // Klik Kanan (Batal / Perintah Jalan)
                if (SwingUtilities.isRightMouseButton(e)) {
                    // Jika di mode Command, klik kanan berarti menyuruh Guard jalan
                    if (gp.currentTool == GamePanel.ToolMode.COMMAND || gp.currentTool == GamePanel.ToolMode.DEFENSE) {
                        Point worldPos = gp.camera.toWorld(e.getX(), e.getY());

                        int selectedCount = 0;
                        for (Guard g : gp.window.activeGuards) {
                            if (g.isSelected) selectedCount++;
                        }

                        if (selectedCount > 0) {
                            int cols = (int) Math.ceil(Math.sqrt(selectedCount));
                            int row = 0, col = 0;
                            int spacing = 32;

                            int startOffsetX = -((cols - 1) * spacing) / 2;
                            int startOffsetY = -((cols - 1) * spacing) / 2;

                            for (Guard g : gp.window.activeGuards) {
                                if (g.isSelected) {
                                    double finalTargetX = worldPos.x + startOffsetX + (col * spacing) - (g.size / 2.0);
                                    double finalTargetY = worldPos.y + startOffsetY + (row * spacing) - (g.size / 2.0);

                                    List<Point> calculatedPath = PathFinder.findPath(
                                            g.x + (g.size/2.0), g.y + (g.size/2.0),
                                            finalTargetX + (g.size/2.0), finalTargetY + (g.size/2.0),
                                            gp.window.savedBuildings
                                    );

                                    if (calculatedPath.isEmpty()) {
                                        finalTargetX = worldPos.x - (g.size / 2.0);
                                        finalTargetY = worldPos.y - (g.size / 2.0);
                                        calculatedPath = PathFinder.findPath(
                                                g.x + (g.size/2.0), g.y + (g.size/2.0),
                                                finalTargetX + (g.size/2.0), finalTargetY + (g.size/2.0),
                                                gp.window.savedBuildings
                                        );
                                    }

                                    g.setPath(calculatedPath);
                                    g.targetX = finalTargetX;
                                    g.targetY = finalTargetY;
                                    g.state = Guard.GuardState.MOVING;

                                    if (gp.currentTool == GamePanel.ToolMode.DEFENSE) {
                                        g.holdPosition = true;
                                        g.holdX = finalTargetX;
                                        g.holdY = finalTargetY;
                                    } else {
                                        g.holdPosition = false; // dikasih order jalan biasa -> lepas dari mode jaga
                                    }

                                    col++;
                                    if (col >= cols) {
                                        col = 0; row++;
                                    }
                                }
                            }
                        }
                    } else if (gp.holdingBuilding != null) {
                        gp.window.savedBuildings.add(gp.holdingBuilding);
                        gp.holdingBuilding = null;
                        gp.currentTool = GamePanel.ToolMode.NONE;
                    } else {
                        gp.currentTool = GamePanel.ToolMode.NONE;
                    }
                    gp.repaint();
                    return;
                }

                if (gp.currentTool == GamePanel.ToolMode.COMMAND || gp.currentTool == GamePanel.ToolMode.DEFENSE){
                    gp.dragStartScreen = e.getPoint();
                    gp.dragEndScreen = e.getPoint();
                    gp.isDragging = true;
                    return; // Berhenti di sini, tidak perlu jalankan kode taruh rumah di bawahnya
                }

                if (!SwingUtilities.isLeftMouseButton(e)) return;

                Point worldPos = gp.camera.toWorld(e.getX(), e.getY());

                // --- LOGIKA KLIK BANGUNAN & TAMBANG UNTUK MENU DINAMIS ---
                if (gp.currentTool == GamePanel.ToolMode.NONE) {
                    boolean hitSomething = false;

                    // 1. Cek Bangunan (Rumah) Terlebih Dahulu
                    for (int i = gp.window.savedBuildings.size() - 1; i >= 0; i--) {
                        Building b = gp.window.savedBuildings.get(i);
                        if (b.contains(worldPos) && !b.isDemolishing) {
                            gp.clickedBuilding = b;
                            gp.clickedMine = null; // Reset status tambang
                            gp.currentMenuState = GamePanel.MenuState.BUILDING_SELECTED;
                            updateGridMenu();
                            hitSomething = true;
                            break;
                        }
                    }

                    // 2. Jika tidak kena rumah, cek Tambang
                    if (!hitSomething) {
                        for (Mine m : gp.activeMines) {
                            if (m.getBounds().contains(worldPos)) {
                                gp.clickedMine = m;
                                gp.clickedBuilding = null; // Reset status rumah
                                gp.currentMenuState = GamePanel.MenuState.MINE_SELECTED;
                                updateGridMenu();
                                hitSomething = true;
                                break;
                            }
                        }
                    }

                    // 3. Kalau klik tanah kosong
                    if (!hitSomething) {
                        gp.clickedBuilding = null;
                        gp.clickedMine = null;
                        gp.currentMenuState = GamePanel.MenuState.MAIN_MENU;
                        updateGridMenu();
                    }
                    gp.repaint();
                    return;
                }

                int bw = gp.getBuildWidth(gp.selectedBuilding);

                int rawTargetX = worldPos.x - (bw / 2);
                int rawTargetY = worldPos.y - (bw / 2);

                int targetX = Math.round((float) rawTargetX / 10f) * 10;
                int targetY = Math.round((float) rawTargetY / 10f) * 10;

                Rectangle newArea = new Rectangle(targetX, targetY, bw, bw);

                // --- LOGIKA TEBANG POHON ---
                if (gp.currentTool == GamePanel.ToolMode.CHOP_WOOD) {
                    for (List<Tree> daftarPohon : gp.mapPohon.values()) {
                        for (Tree pohon : daftarPohon) {
                            if (pohon.getBounds().contains(worldPos)) {
                                pohon.isHarvesting = true; // Pohon mulai ditebang!
                            }
                        }
                    }
                }

                if (gp.currentTool == GamePanel.ToolMode.BUILD) {
                    boolean isWall = (gp.selectedBuilding == Building.BuildingType.WALL_L ||
                            gp.selectedBuilding == Building.BuildingType.WALL_R ||
                            gp.selectedBuilding == Building.BuildingType.WALL_UD);

                    if (isWall) {
                        gp.isDraggingWall = true;
                        gp.dragStartPoint.x = targetX;
                        gp.dragStartPoint.y = targetY;
                        gp.dragCurrentPoint.x = targetX;
                        gp.dragCurrentPoint.y = targetY;

                    } else {
                        // --- FITUR BARU: cek & bayar wood + stone + civil sekaligus (dulunya cuma wood) ---
                        // Building lama (Farm, House, dll) stone/civil cost-nya 0 dari Building.java,
                        // jadi behavior-nya sama persis kayak sebelumnya buat mereka -- cuma Archer Tower
                        // yang beneran kepotong stone & civil-nya.
                        int woodCost = Building.getWoodCost(gp.selectedBuilding);
                        int stoneCost = Building.getStoneCost(gp.selectedBuilding);
                        int civilCost = Building.getCivilCost(gp.selectedBuilding);

                        boolean enoughWood = gp.isCheatModeActive || gp.resourceManager.wood >= woodCost;
                        boolean enoughStone = gp.isCheatModeActive || gp.resourceManager.stone >= stoneCost;
                        boolean enoughCivilPool = gp.isCheatModeActive || gp.window.activeCivils.size() >= civilCost;

                        // --- DEV MODE: gratis, skip semua cek ---
                        if (enoughWood && enoughStone && enoughCivilPool) {
                            if (!gp.isOverlapping(newArea, null) && !gp.isTreeBlocking(newArea)) {
                                if (!gp.isCheatModeActive) {
                                    gp.resourceManager.wood -= woodCost;   // Bayar kayunya
                                    gp.resourceManager.stone -= stoneCost; // Bayar batunya
                                    // "Korbankan" civil idle sejumlah civilCost (dipakai buat Archer Tower)
                                    for (int i = 0; i < civilCost && !gp.window.activeCivils.isEmpty(); i++) {
                                        gp.window.activeCivils.remove(gp.window.activeCivils.size() - 1);
                                    }
                                }

                                int cap = gp.getBuildCapacity(gp.selectedBuilding);
                                Building newBuilding = new Building(targetX, targetY, bw, bw, gp.selectedBuilding, cap);
                                gp.window.savedBuildings.add(newBuilding);

                                double spawnPointX = targetX + (bw / 2.0);
                                double spawnPointY = targetY + (bw / 2.0);

                                if (gp.selectedBuilding == Building.BuildingType.BUILDER) {
                                    // Builder House -> isinya CivilBuilder, bukan Civil biasa
                                    for (int i = 0; i < cap; i++) {
                                        gp.window.activeCivilBuilders.add(new CivilBuilder(spawnPointX, spawnPointY, newBuilding));
                                    }
                                } else if (gp.selectedBuilding == Building.BuildingType.ARCHER_TOWER) {
                                    // --- FITUR BARU: Archer Tower nembak sendiri, gak butuh occupant Civil
                                    // kayak House (cap-nya emang 0 di getBuildCapacity, ini cuma dokumentasi) ---
                                } else {
                                    for (int i = 0; i < cap; i++) {
                                        // --- FITUR BARU: Civil dikasih tau rumah asalnya (newBuilding) biar bisa kabur pulang ---
                                        Civil newCivil = new Civil(spawnPointX, spawnPointY, newBuilding);
                                        gp.window.activeCivils.add(newCivil);
                                        // --- FITUR BARU: daftarkan Civil ini sebagai penghuni resmi rumahnya
                                        // (dipakai buat cek slot kosong pas regenerasi tiap pagi & panel info bangunan) ---
                                        newBuilding.enterBuilding(newCivil);
                                    }
                                }
                            }
                        } else {
                            System.out.println("Resource tidak cukup untuk membangun!");
                        }
                    }
                }
                else if (gp.currentTool == GamePanel.ToolMode.MOVE) {
                    if (gp.holdingBuilding == null) {
                        for (int i = gp.window.savedBuildings.size() - 1; i >= 0; i--) {
                            if (gp.window.savedBuildings.get(i).contains(worldPos)) {
                                gp.holdingBuilding = gp.window.savedBuildings.remove(i);
                                break;
                            }
                        }
                    } else {
                        if (!gp.isOverlapping(newArea, gp.holdingBuilding)) {
                            // Tinggal geser saja lokasinya, tidak perlu buat objek baru!
                            gp.holdingBuilding.getBounds().setLocation(targetX, targetY);
                            gp.window.savedBuildings.add(gp.holdingBuilding);
                            gp.holdingBuilding = null;
                        }
                    }
                }
                else if (gp.currentTool == GamePanel.ToolMode.DELETE) {
                    for (int i = gp.window.savedBuildings.size() - 1; i >= 0; i--) {
                        if (gp.window.savedBuildings.get(i).contains(worldPos)) {
                            gp.window.savedBuildings.remove(i);
                            break;
                        }
                    }
                }
                gp.repaint();
            }

            // --- FUNGSI BARU: SAAT KLIK KIRI DILEPAS SETELAH NGE-BLOK ---
            @Override
            public void mouseReleased(MouseEvent e) {
                if (gp.currentTool == GamePanel.ToolMode.COMMAND && SwingUtilities.isLeftMouseButton(e) && gp.isDragging) {
                    gp.isDragging = false;

                    if (gp.dragStartScreen != null && gp.dragEndScreen != null) {
                        // Ubah kotak di layar menjadi koordinat dunia (world coordinate)
                        Point worldStart = gp.camera.toWorld(gp.dragStartScreen.x, gp.dragStartScreen.y);
                        Point worldEnd = gp.camera.toWorld(gp.dragEndScreen.x, gp.dragEndScreen.y);

                        int wx = Math.min(worldStart.x, worldEnd.x);
                        int wy = Math.min(worldStart.y, worldEnd.y);
                        int wWidth = Math.abs(worldStart.x - worldEnd.x);
                        int wHeight = Math.abs(worldStart.y - worldEnd.y);

                        Rectangle worldSelectRect = new Rectangle(wx, wy, wWidth, wHeight);

                        // Cek Guard mana yang masuk jaring blok kita
                        boolean anySelected = false;
                        for (Guard g : gp.window.activeGuards) {
                            Rectangle guardRect = new Rectangle((int)g.x, (int)g.y, g.size, g.size);
                            if (worldSelectRect.intersects(guardRect) || worldSelectRect.contains(guardRect)) {
                                g.isSelected = true;
                                anySelected = true;
                            } else {
                                g.isSelected = false; // Batal terpilih jika di luar kotak
                            }
                        }

                        // FITUR BARU: klik/drag kosong (gak kena unit) = cancel, matiin mode attack juga
                        if (!anySelected) {
                            gp.currentTool = GamePanel.ToolMode.NONE;
                        }
                    }
                    gp.repaint();
                }

                if (gp.currentTool == GamePanel.ToolMode.BUILD && gp.isDraggingWall) {
                    gp.isDraggingWall = false;
                    int diffX = Math.abs(gp.dragCurrentPoint.x - gp.dragStartPoint.x);
                    int diffY = Math.abs(gp.dragCurrentPoint.y - gp.dragStartPoint.y);

                    if (diffX > diffY) {
                        // HORIZONTAL
                        int startX = Math.min(gp.dragStartPoint.x, gp.dragCurrentPoint.x);
                        int endX = Math.max(gp.dragStartPoint.x, gp.dragCurrentPoint.x);
                        int y = gp.dragStartPoint.y;
                        for (int x = startX; x <= endX; x += 10) {
                            Building.BuildingType type = (x == startX) ? Building.BuildingType.WALL_L : Building.BuildingType.WALL_R;
                            int cost = Building.getWoodCost(type);
                            if ((gp.isCheatModeActive || gp.resourceManager.wood >= cost) && !gp.isOverlapping(new Rectangle(x, y, 10, 10), null) && !gp.isTreeBlocking(new Rectangle(x, y, 10, 10))) {
                                if (!gp.isCheatModeActive) gp.resourceManager.wood -= cost;
                                gp.window.savedBuildings.add(new Building(x, y, 10, 10, type, 0));
                            }
                        }
                    } else {
                        // VERTIKAL
                        int startY = Math.min(gp.dragStartPoint.y, gp.dragCurrentPoint.y);
                        int endY = Math.max(gp.dragStartPoint.y, gp.dragCurrentPoint.y);
                        int x = gp.dragStartPoint.x;
                        for (int y = startY; y <= endY; y += 10) {
                            int cost = Building.getWoodCost(Building.BuildingType.WALL_UD);
                            if ((gp.isCheatModeActive || gp.resourceManager.wood >= cost) && !gp.isOverlapping(new Rectangle(x, y, 10, 10), null) && !gp.isTreeBlocking(new Rectangle(x, y, 10, 10))) {
                                if (!gp.isCheatModeActive) gp.resourceManager.wood -= cost;
                                gp.window.savedBuildings.add(new Building(x, y, 10, 10, Building.BuildingType.WALL_UD, 0));
                            }
                        }
                    }
                    gp.repaint();
                }
            }
        });
    }

    public void updateGridMenu() {
        gp.gridMenuPanel.removeAll();

        // --- FITUR BARU: PANEL INFO IDENTITAS BANGUNAN ---
        // Tampil setiap kali ada bangunan yang lagi dipilih (BUILDING_SELECTED), gaya & slotnya
        // persis barrackQueuePanel -- makanya keduanya otomatis gak akan pernah tabrakan tampil
        // bareng (beda MenuState: BUILDING_SELECTED vs BARRACK_MENU).
        if (gp.buildingInfoPanel != null) {
            boolean showInfo = gp.currentMenuState == GamePanel.MenuState.BUILDING_SELECTED && gp.clickedBuilding != null;
            gp.buildingInfoPanel.setVisible(showInfo);
            if (showInfo) {
                gp.buildingInfoPanel.repaint();
                if (gp.barrackQueuePanel != null) gp.barrackQueuePanel.setVisible(false);
            }
        }

        if (gp.currentMenuState == GamePanel.MenuState.MAIN_MENU) {
            gp.gridMenuPanel.add(gp.createGridBtn("👨‍🌾", () -> { gp.currentMenuState = GamePanel.MenuState.CIVIL_MENU; updateGridMenu(); }, false));
            gp.gridMenuPanel.add(gp.createGridBtn("⚔️", () -> { gp.currentMenuState = GamePanel.MenuState.MILITARY_MENU; updateGridMenu(); }, false));

            for(int i=0; i<10; i++) gp.gridMenuPanel.add(gp.createGridBtn("", null, false));
        }
        else if (gp.currentMenuState == GamePanel.MenuState.CIVIL_MENU) {
            boolean isSmall = (gp.selectedBuilding == Building.BuildingType.SMALL_HOUSE);
            boolean isMed = (gp.selectedBuilding == Building.BuildingType.MEDIUM_HOUSE);
            boolean isBig = (gp.selectedBuilding == Building.BuildingType.BIG_HOUSE);
            boolean isChop = (gp.currentTool == GamePanel.ToolMode.CHOP_WOOD);
            boolean isFarm = (gp.selectedBuilding == Building.BuildingType.FARM);
            boolean isStorage = (gp.selectedBuilding == Building.BuildingType.STORAGE);
            boolean isBuilderSel = (gp.selectedBuilding == Building.BuildingType.BUILDER);


            // Sekarang pakai gambar sprite bangunan asli, kecuali kapak (biar tetap emoji)
            gp.gridMenuPanel.add(gp.createGridBtnBuilding(Building.BuildingType.SMALL_HOUSE, () -> { gp.selectedBuilding = Building.BuildingType.SMALL_HOUSE; gp.currentTool = GamePanel.ToolMode.BUILD; updateGridMenu(); gp.repaint(); }, isSmall));
            gp.gridMenuPanel.add(gp.createGridBtnBuilding(Building.BuildingType.MEDIUM_HOUSE, () -> { gp.selectedBuilding = Building.BuildingType.MEDIUM_HOUSE; gp.currentTool = GamePanel.ToolMode.BUILD; updateGridMenu(); gp.repaint(); }, isMed));
            gp.gridMenuPanel.add(gp.createGridBtnBuilding(Building.BuildingType.BIG_HOUSE, () -> { gp.selectedBuilding = Building.BuildingType.BIG_HOUSE; gp.currentTool = GamePanel.ToolMode.BUILD; updateGridMenu(); gp.repaint(); }, isBig));
            gp.gridMenuPanel.add(gp.createGridBtn("🪓", () -> {
                gp.currentTool = GamePanel.ToolMode.CHOP_WOOD;
                updateGridMenu();
                gp.repaint();
            }, isChop));
            gp.gridMenuPanel.add(gp.createGridBtnBuilding(Building.BuildingType.FARM, () -> { gp.selectedBuilding = Building.BuildingType.FARM; gp.currentTool = GamePanel.ToolMode.BUILD; updateGridMenu(); gp.repaint(); }, isFarm));
            gp.gridMenuPanel.add(gp.createGridBtnBuilding(Building.BuildingType.STORAGE, () -> { gp.selectedBuilding = Building.BuildingType.STORAGE; gp.currentTool = GamePanel.ToolMode.BUILD; updateGridMenu(); gp.repaint(); }, isStorage));
            gp.gridMenuPanel.add(gp.createGridBtnBuilding(Building.BuildingType.BUILDER, () -> {
                gp.selectedBuilding = Building.BuildingType.BUILDER;
                gp.currentTool = GamePanel.ToolMode.BUILD;
                updateGridMenu();
                gp.repaint();
            }, isBuilderSel));

            for(int i=0; i<7; i++) gp.gridMenuPanel.add(gp.createGridBtn("", null, false));
            gp.gridMenuPanel.add(gp.createGridBtn("⬅️", () -> { gp.currentMenuState = GamePanel.MenuState.MAIN_MENU; updateGridMenu(); }, false));
        }
        else if (gp.currentMenuState == GamePanel.MenuState.MILITARY_MENU) {
            boolean isBarrack = (gp.selectedBuilding == Building.BuildingType.BARRACK);
            boolean isWall = (gp.selectedBuilding == Building.BuildingType.WALL_L);
            boolean isArcherTower = (gp.selectedBuilding == Building.BuildingType.ARCHER_TOWER);

            // Tambahkan tombol Barrack (sekarang pakai gambar sprite asli)
            gp.gridMenuPanel.add(gp.createGridBtnBuilding(Building.BuildingType.BARRACK, () -> {
                gp.selectedBuilding = Building.BuildingType.BARRACK;
                gp.currentTool = GamePanel.ToolMode.BUILD;
                updateGridMenu();
                gp.repaint();
            }, isBarrack));

            // --- FITUR BARU: Wall dipindah kesini dari CIVIL_MENU (satu tempat sama unit/bangunan militer) ---
            gp.gridMenuPanel.add(gp.createGridBtnBuilding(Building.BuildingType.WALL_L, () -> {
                gp.selectedBuilding = Building.BuildingType.WALL_L; // Jadikan L sebagai default/pancingan awal
                gp.currentTool = GamePanel.ToolMode.BUILD;
                updateGridMenu();
                gp.repaint();
            }, isWall));

            // --- FITUR BARU: Tombol Archer Tower (cost wood+stone+civil, nembak sendiri, hancur -> 4 Guard Bow) ---
            gp.gridMenuPanel.add(gp.createGridBtnBuilding(Building.BuildingType.ARCHER_TOWER, () -> {
                gp.selectedBuilding = Building.BuildingType.ARCHER_TOWER;
                gp.currentTool = GamePanel.ToolMode.BUILD;
                updateGridMenu();
                gp.repaint();
            }, isArcherTower));

            // Isi sisa 8 kotak kosong dan 1 tombol kembali
            for(int i=0; i<8; i++) gp.gridMenuPanel.add(gp.createGridBtn("", null, false));
            gp.gridMenuPanel.add(gp.createGridBtn("⬅️", () -> { gp.currentMenuState = GamePanel.MenuState.MAIN_MENU; updateGridMenu(); }, false));
        }

        else if (gp.currentMenuState == GamePanel.MenuState.BUILDING_SELECTED) {
            // --- FITUR BARU: Heart tidak boleh dipindah maupun dihancurkan ---
            boolean isHeart = gp.clickedBuilding != null && gp.clickedBuilding.type == Building.BuildingType.HEART;

            // --- FITUR BARU: Tombol Upgrade dihapus (dulu cuma println, gak fungsi beneran) ---
            gp.gridMenuPanel.add(gp.createGridBtn("", null, false));

            // 2. Tombol MOVE (Angkat bangunan) - disembunyikan kalau Heart
            if (isHeart) {
                gp.gridMenuPanel.add(gp.createGridBtn("", null, false));
            } else {
                gp.gridMenuPanel.add(gp.createGridBtn("✋", () -> {
                    if (gp.clickedBuilding != null) {
                        gp.holdingBuilding = gp.clickedBuilding;
                        gp.window.savedBuildings.remove(gp.clickedBuilding); // Cabut dari tanah
                        gp.clickedBuilding = null; // Lepaskan pilihan
                        gp.currentTool = GamePanel.ToolMode.MOVE;
                        gp.currentMenuState = GamePanel.MenuState.MAIN_MENU; // Kembalikan menu
                        updateGridMenu();
                        gp.repaint();
                    }
                }, false));
            }

            // 3. Tombol DESTROY (Mulai Loading Hancur) - disembunyikan kalau Heart
            if (isHeart) {
                gp.gridMenuPanel.add(gp.createGridBtn("", null, false));
            } else {
                gp.gridMenuPanel.add(gp.createGridBtn("❌", () -> {
                    if (gp.clickedBuilding != null) {
                        gp.clickedBuilding.isDemolishing = true; // Picu loading bar merah
                        gp.clickedBuilding = null;
                        gp.currentMenuState = GamePanel.MenuState.MAIN_MENU;
                        updateGridMenu();
                    }
                }, false));
            }

            // Isi 8 kotak kosong dan 1 tombol kembali
            // --- FITUR BARU: Tombol "+" khusus muncul kalau bangunan yang diklik adalah BARRACK ---
            if (gp.clickedBuilding != null && gp.clickedBuilding.type == Building.BuildingType.BARRACK && gp.clickedBuilding.isBuilt) {
                gp.gridMenuPanel.add(gp.createGridBtn("➕", () -> {
                    gp.currentMenuState = GamePanel.MenuState.BARRACK_MENU;
                    updateGridMenu();
                    if (gp.barrackQueuePanel != null) {
                        gp.barrackQueuePanel.setVisible(true);
                        gp.barrackQueuePanel.repaint();
                    }
                }, false));
            } else {
                gp.gridMenuPanel.add(gp.createGridBtn("", null, false));
            }
            for(int i=0; i<7; i++) gp.gridMenuPanel.add(gp.createGridBtn("", null, false));
            gp.gridMenuPanel.add(gp.createGridBtn("⬅️", () -> {
                gp.clickedBuilding = null; gp.currentMenuState = GamePanel.MenuState.MAIN_MENU; updateGridMenu(); gp.repaint();
                if (gp.barrackQueuePanel != null) gp.barrackQueuePanel.setVisible(false);
            }, false));
        }

        // --- FITUR BARU: MENU PILIH GUARD DI BARRACK ---
        else if (gp.currentMenuState == GamePanel.MenuState.BARRACK_MENU) {
            // Slot 1: Spearman (pakai gambar sprite)
            gp.gridMenuPanel.add(gp.createGridBtnWithImage(gp.spearmanImg, "Spearman", () -> {
                if (gp.clickedBuilding != null) {
                    // --- FITUR BARU: Butuh 1 Civil untuk direkrut jadi Guard ---
                    if (!gp.window.activeCivils.isEmpty()) {
                        Civil recruited = gp.window.activeCivils.remove(gp.window.activeCivils.size() - 1); // Kurangi 1 civil
                        // --- FITUR BARU: lepaskan slot rumah asalnya, biar bisa keisi lagi pas regenerasi pagi ---
                        if (recruited.homeBuilding != null) recruited.homeBuilding.exitBuilding(recruited);
                        gp.barrackQueues.computeIfAbsent(gp.clickedBuilding, k -> new java.util.LinkedList<>())
                                .add(Guard.GuardType.SPEARMAN);
                        if (gp.barrackQueuePanel != null) gp.barrackQueuePanel.repaint();
                    } else {
                        System.out.println("Tidak ada Civil yang bisa direkrut menjadi Spearman!");
                    }
                }
            }));
            // Slot 2: Archer (pakai gambar sprite)
            gp.gridMenuPanel.add(gp.createGridBtnWithImage(gp.archerImg, "Archer", () -> {
                if (gp.clickedBuilding != null) {
                    // --- FITUR BARU: Butuh 1 Civil untuk direkrut jadi Guard ---
                    if (!gp.window.activeCivils.isEmpty()) {
                        Civil recruited = gp.window.activeCivils.remove(gp.window.activeCivils.size() - 1); // Kurangi 1 civil
                        // --- FITUR BARU: lepaskan slot rumah asalnya, biar bisa keisi lagi pas regenerasi pagi ---
                        if (recruited.homeBuilding != null) recruited.homeBuilding.exitBuilding(recruited);
                        gp.barrackQueues.computeIfAbsent(gp.clickedBuilding, k -> new java.util.LinkedList<>())
                                .add(Guard.GuardType.ARCHER);
                        if (gp.barrackQueuePanel != null) gp.barrackQueuePanel.repaint();
                    } else {
                        System.out.println("Tidak ada Civil yang bisa direkrut menjadi Archer!");
                    }
                }
            }));
            // Slot 3: Tampilan jumlah Civil yang tersedia (teks, merah kalau 0)
            gp.gridMenuPanel.add(gp.createCivilCountDisplay());
            // Sisa kotak kosong + tombol kembali
            for(int i=0; i<8; i++) gp.gridMenuPanel.add(gp.createGridBtn("", null, false));
            gp.gridMenuPanel.add(gp.createGridBtn("⬅️", () -> {
                gp.currentMenuState = GamePanel.MenuState.BUILDING_SELECTED;
                updateGridMenu();
                if (gp.barrackQueuePanel != null) gp.barrackQueuePanel.setVisible(false);
            }, false));
        }

        else if (gp.currentMenuState == GamePanel.MenuState.MINE_SELECTED) {
            // Jika tambang belum dibangun dan belum dalam proses loading
            if (gp.clickedMine != null && !gp.clickedMine.isBuilt && !gp.clickedMine.isBuilding) {
                // Tombol Palu untuk Membangun Tambang
                gp.gridMenuPanel.add(gp.createGridBtn("🔨", () -> {
                    if (gp.clickedMine != null) {
                        gp.clickedMine.isBuilding = true; // Mulai loading!
                        gp.clickedMine = null; // Tutup menu agar pemain fokus lihat loading
                        gp.currentMenuState = GamePanel.MenuState.MAIN_MENU;
                        updateGridMenu();
                        gp.repaint();
                    }
                }, false));

                // Isi sisa grid yang kosong (8 kotak + 1 kembali)
                for(int i=0; i<8; i++) gp.gridMenuPanel.add(gp.createGridBtn("", null, false));
                gp.gridMenuPanel.add(gp.createGridBtn("⬅️", () -> {
                    gp.clickedMine = null; gp.currentMenuState = GamePanel.MenuState.MAIN_MENU; updateGridMenu(); gp.repaint();
                }, false));
            }
            else {
                // Jika tambang SUDAH JADI (mine.png), menu sementara kosong atau bisa diisi fitur nambang ke depannya
                for(int i=0; i<9; i++) gp.gridMenuPanel.add(gp.createGridBtn("", null, false));
                gp.gridMenuPanel.add(gp.createGridBtn("⬅️", () -> {
                    gp.clickedMine = null; gp.currentMenuState = GamePanel.MenuState.MAIN_MENU; updateGridMenu(); gp.repaint();
                }, false));
            }
        }
        gp.gridMenuPanel.revalidate();
        gp.gridMenuPanel.repaint();
    }
}