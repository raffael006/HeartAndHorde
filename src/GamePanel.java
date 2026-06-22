import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.List;


public class GamePanel extends JPanel {
    private GameWindow window; // Referensi ke Bos
    private BufferedImage gameplayBg;
    private BufferedImage houseImage;
    private BufferedImage archerImg;
    private BufferedImage spearmanImg;

    // --- ADT Kamera ---
    private Camera camera = new Camera();
    private Timer cameraTimer;

    // --- Variabel Input ---
    private boolean wPressed = false, sPressed = false, aPressed = false, dPressed = false;

    // --- Variabel Alat & Bangunan ---
    private enum ToolMode { NONE, BUILD, MOVE, DELETE, COMMAND }
    private ToolMode currentTool = ToolMode.NONE;
    private int mouseX = -100, mouseY = -100;
    private Point dragStartScreen = null;
    private Point dragEndScreen = null;
    private boolean isDragging = false;
    private Building holdingBuilding = null;
    private final int houseWidth = 90;
    private final int houseHeight = 90;

    // --- Variabel UI/HUD ---
    private JPanel bottomLeftBar;
    private JPanel topRightBar;
    private JButton menuBtn;

    public GamePanel(GameWindow window) {
        this.window = window;
        setLayout(null);

        // 1. Muat Gambar (Menggunakan path src/ dan dipisah agar aman)
        try {
            gameplayBg = ImageIO.read(new File("assets/img/GAMEBACKGROUND.png"));
        } catch (Exception e) {
            System.out.println("Gagal memuat Background!");
            e.printStackTrace();
        }

        try {
            houseImage = ImageIO.read(new File("assets/img/house_h&h.png"));
        } catch (Exception e) {
            System.out.println("Gagal memuat Rumah!");
        }

        try {
            archerImg = ImageIO.read(new File("assets/img/Hearthguardbow.png"));
            spearmanImg = ImageIO.read(new File("assets/img/hearthguard_spier.png"));
        } catch (Exception e) {
            System.out.println("Gagal memuat Pasukan Guard!");
        }

        // 2. Setup Timer Mesin Kamera (60 FPS)
        cameraTimer = new Timer(16, e -> {
            boolean moved = camera.move(wPressed, sPressed, aPressed, dPressed);

            boolean guardMoved = false;
            for (Guard g : window.activeGuards) {
                g.update(window.activeGuards);
                if (g.state == Guard.GuardState.MOVING) {
                    guardMoved = true;
                }
            }

//            if (moved || guardMoved)
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
                    topRightBar.setBounds(w - 600, 0, 600, 50);
                    menuBtn.setBounds(topRightBar.getWidth() - 40, 10, 30, 30);
                    bottomLeftBar.setBounds(0, h - 100, 240, 100);
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

        // Shortcut Tools
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_B, 0), "toolBuild");
        getActionMap().put("toolBuild", new AbstractAction() { public void actionPerformed(ActionEvent e) { currentTool = ToolMode.BUILD; holdingBuilding = null; repaint(); }});

        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_M, 0), "toolMove");
        getActionMap().put("toolMove", new AbstractAction() { public void actionPerformed(ActionEvent e) { currentTool = ToolMode.MOVE; repaint(); }});

        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_X, 0), "toolDelete");
        getActionMap().put("toolDelete", new AbstractAction() { public void actionPerformed(ActionEvent e) { currentTool = ToolMode.DELETE; holdingBuilding = null; repaint(); }});

        // Cheat Spawn Guard (Tekan K)
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_K, 0), "cheatSpawnGuard");
        getActionMap().put("cheatSpawnGuard", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                // Munculkan Archer atau Spearman secara acak (50:50) di posisi mouse saat ini
                Guard.GuardType type = Math.random() > 0.5 ? Guard.GuardType.SPEARMAN : Guard.GuardType.ARCHER;
                window.activeGuards.add(new Guard(type, mouseX, mouseY));
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
                        for (Guard g : window.activeGuards) {
                            if (g.isSelected) {
                                // Arahkan mereka ke titik kursor (dikurangi setengah size agar pas di tengah)
                                g.targetX = worldPos.x - (g.size / 2);
                                g.targetY = worldPos.y - (g.size / 2);
                                g.state = Guard.GuardState.MOVING;
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
                int targetX = worldPos.x - (houseWidth / 2);
                int targetY = worldPos.y - (houseHeight / 2);
                Rectangle newArea = new Rectangle(targetX, targetY, houseWidth, houseHeight);

                if (currentTool == ToolMode.BUILD) {
                    if (!isOverlapping(newArea, null)) {
                        window.savedBuildings.add(new Building(targetX, targetY, houseWidth, houseHeight));
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
                            window.savedBuildings.add(new Building(targetX, targetY, houseWidth, houseHeight));
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
            }
        });
    }

    private boolean isOverlapping(Rectangle newRect, Building ignoreBuilding) {
        for (Building b : window.savedBuildings) {
            if (b != ignoreBuilding && b.intersects(newRect)) return true;
        }
        return false;
    }

    private void setupHUD() {
        // HUD KANAN ATAS
        topRightBar = new JPanel() {
            protected void paintComponent(Graphics g) { g.setColor(new Color(30, 25, 45, 240)); g.fillRect(0, 0, getWidth(), getHeight()); }
        };
        topRightBar.setLayout(null); topRightBar.setOpaque(false);

        String[] stats = {"😊 100", "🍖 500", "💧 300", "💰 1250", "📜 5%"};
        int statX = 20;
        for (String s : stats) {
            JLabel l = new JLabel(s); l.setForeground(Color.WHITE);
            l.setFont(new Font("Segoe UI Emoji", Font.BOLD, 16));
            l.setBounds(statX, 10, 100, 30); topRightBar.add(l); statX += 100;
        }

        menuBtn = new JButton("M");
        menuBtn.addActionListener(e -> showInGameMenu());
        topRightBar.add(menuBtn); add(topRightBar);

        // HUD KIRI BAWAH
        bottomLeftBar = new JPanel() {
            protected void paintComponent(Graphics g) { g.setColor(new Color(30, 25, 45, 240)); g.fillRect(0, 0, getWidth(), getHeight()); }
        };
        bottomLeftBar.setLayout(null); bottomLeftBar.setOpaque(false);

        JButton buildBtn = createColorButton(new Color(110, 55, 25), ToolMode.BUILD, "🔨");
        buildBtn.setBounds(15, 25, 50, 50);
        buildBtn.addActionListener(e -> { currentTool = ToolMode.BUILD; holdingBuilding = null; repaint(); });
        bottomLeftBar.add(buildBtn);

        JButton moveBtn = createColorButton(new Color(100, 150, 255), ToolMode.MOVE, "✋");
        moveBtn.setBounds(75, 25, 20, 20);
        moveBtn.addActionListener(e -> { currentTool = ToolMode.MOVE; repaint(); });
        bottomLeftBar.add(moveBtn);

        JButton removeBtn = createColorButton(new Color(200, 50, 50), ToolMode.DELETE, "❌");
        removeBtn.setBounds(75, 55, 20, 20);
        removeBtn.addActionListener(e -> { currentTool = ToolMode.DELETE; holdingBuilding = null; repaint(); });
        bottomLeftBar.add(removeBtn);

        JButton defBtn = createCircleButton(new Color(100, 100, 120), "🛡️");
        defBtn.setBounds(110, 30, 40, 40);
        bottomLeftBar.add(defBtn);

        JButton atkBtn = createCircleButton(new Color(150, 50, 50), "⚔️");
        atkBtn.setBounds(170, 30, 40, 40);

        atkBtn.addActionListener(e -> { currentTool = ToolMode.COMMAND; repaint(); });

        bottomLeftBar.add(atkBtn);

        add(bottomLeftBar);
        repaint();
    }

    // --- Fungsi Pembuat Tombol Cantik (Paste di bawah setupHUD) ---
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

    private JButton createCircleButton(Color baseColor, String icon) {
        JButton btn = new JButton(icon) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color topColor = getModel().isRollover() ? baseColor : baseColor.darker();
                g2d.setPaint(new GradientPaint(0, 0, topColor, 0, getHeight(), baseColor.darker().darker()));
                g2d.fillOval(0, 0, getWidth()-1, getHeight()-1);

                g2d.setColor(new Color(255, 255, 255, 100)); g2d.drawOval(1, 1, getWidth()-3, getHeight()-3);
                g2d.setColor(Color.WHITE); g2d.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
                FontMetrics fm = g2d.getFontMetrics();
                g2d.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2d.dispose();
            }
        };
        btn.setContentAreaFilled(false); btn.setBorderPainted(false); btn.setFocusPainted(false);
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

        camera.clamp(getWidth(), getHeight(), 3000, 2000);

        Graphics2D g2d = (Graphics2D) g.create();
        camera.applyTransform(g2d);

        // Render Tanah
        if (gameplayBg != null) g2d.drawImage(gameplayBg, 0, 0, 3000, 2000, null);
        else { g2d.setColor(new Color(30, 50, 30)); g2d.fillRect(0, 0, 3000, 2000); }

        // Render Rumah (ADT)
        for (Building b : window.savedBuildings) {
            b.draw(g2d, houseImage);
        }

        for (Guard hg : window.activeGuards) {
            BufferedImage spriteToUse = (hg.type == Guard.GuardType.ARCHER) ? archerImg : spearmanImg;
            hg.draw(g2d, spriteToUse);
        }

        // Render Preview Kursor
        int pX = mouseX - (houseWidth / 2);
        int pY = mouseY - (houseHeight / 2);
        Rectangle previewRect = new Rectangle(pX, pY, houseWidth, houseHeight);

        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));

        if (currentTool == ToolMode.BUILD || (currentTool == ToolMode.MOVE && holdingBuilding != null)) {
            if (isOverlapping(previewRect, null)) {
                g2d.setColor(new Color(255, 0, 0, 150));
                g2d.fillRect(pX, pY, houseWidth, houseHeight);
            } else {
                if (houseImage != null) g2d.drawImage(houseImage, pX, pY, houseWidth, houseHeight, null);
                else { g2d.setColor(new Color(200, 200, 200, 150)); g2d.fillRect(pX, pY, houseWidth, houseHeight); }
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

            g2dHUD.setColor(new Color(0, 255, 0, 40)); // Warna hijau transparan
            g2dHUD.fillRect(sx, sy, sw, sh);
            g2dHUD.setColor(new Color(0, 255, 0, 150)); // Garis tepi
            g2dHUD.setStroke(new BasicStroke(1.5f));
            g2dHUD.drawRect(sx, sy, sw, sh);
            g2dHUD.dispose();
        }

        g2d.dispose();
    }
}