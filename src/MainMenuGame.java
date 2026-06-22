import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import javax.sound.sampled.*;
import javax.swing.Timer;

public class MainMenuGame extends JFrame {

    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private BufferedImage backgroundImage;

    // Deklarasi Global Panel Menu
    private JPanel menuBox;
    private JPanel optionsBox;
    private JPanel loadBox;
    private BackgroundPanel mainPanel;

    private boolean isOverlayOpen = false;
    private Clip backgroundMusic;

    // Deklarasi Komponen Video & Audio
    private JComboBox<String> modeCombo;
    private JComboBox<String> resCombo;
    private JComboBox<String> hzCombo;
    private JSlider masterSlider;
    private JSlider musicSlider;
    private JSlider sfxSlider;

    private JButton slot1Btn, slot2Btn, slot3Btn;

    // === STATE PROGRES GAME ===
    public List<Building> savedBuildings = new ArrayList<>();

    public MainMenuGame() {
        setTitle("Heart & Horde ~ Bloodshed in Cryonia");
        setSize(WIDTH, HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        // Path langsung diarahkan ke folder img
        try {
            backgroundImage = ImageIO.read(new File("assets/img/background.jpg"));
        } catch (Exception e) {
            System.out.println("Gagal memuat background.jpg di folder assets/img/");
        }

        masterSlider = createStyledSlider(75);
        musicSlider = createStyledSlider(75);
        sfxSlider = createStyledSlider(75);

        // Path langsung diarahkan ke folder music
        playBackgroundMusic("assets/music/StarCraft II - Terran Theme 01.wav");

        mainPanel = new BackgroundPanel();
        mainPanel.setLayout(null);
        setContentPane(mainPanel);

        setupKeyBindings();

        menuBox = createMenuBox();
        mainPanel.add(menuBox);

        optionsBox = createOptionsBox();
        optionsBox.setVisible(false);
        mainPanel.add(optionsBox);

        loadBox = createLoadBox();
        loadBox.setVisible(false);
        mainPanel.add(loadBox);

        applyVideoSettings();
    }

    private void setupKeyBindings() {
        mainPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "minimizeGame");
        mainPanel.getActionMap().put("minimizeGame", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setState(JFrame.ICONIFIED);
            }
        });
    }

    public void saveGameData(int slot) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("heart_save_" + slot + ".dat"))) {
            oos.writeObject(savedBuildings);
            JOptionPane.showMessageDialog(this, "Progress kota berhasil disimpan di Slot " + slot + "!", "Save Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            System.out.println("Gagal menyimpan game: " + e.getMessage());
        }
    }

    public boolean loadGameData(int slot) {
        File file = new File("heart_save_" + slot + ".dat");
        if (!file.exists()) return false;

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            savedBuildings = (List<Building>) ois.readObject();
            return true;
        } catch (Exception e) {
            System.out.println("Gagal memuat game: " + e.getMessage());
            return false;
        }
    }

    private void updateLoadBoxUI() {
        slot1Btn.setText(new File("heart_save_1.dat").exists() ? "SAVE SLOT 1  [ DATA FOUND ]" : "SAVE SLOT 1  [ EMPTY ]");
        slot2Btn.setText(new File("heart_save_2.dat").exists() ? "SAVE SLOT 2  [ DATA FOUND ]" : "SAVE SLOT 2  [ EMPTY ]");
        slot3Btn.setText(new File("heart_save_3.dat").exists() ? "SAVE SLOT 3  [ DATA FOUND ]" : "SAVE SLOT 3  [ EMPTY ]");
    }

    private float calculateGain(float sliderValue, float masterValue) {
        if (sliderValue == 0 || masterValue == 0) return -80.0f;
        float effectiveVolume = (sliderValue / 100f) * (masterValue / 100f);
        return 20f * (float) Math.log10(effectiveVolume);
    }

    private void applyAudioSettings() {
        if (backgroundMusic != null && backgroundMusic.isOpen()) {
            try {
                FloatControl gainControl = (FloatControl) backgroundMusic.getControl(FloatControl.Type.MASTER_GAIN);
                gainControl.setValue(calculateGain(musicSlider.getValue(), masterSlider.getValue()));
            } catch (Exception e) {}
        }
    }

    private void playBackgroundMusic(String filePath) {
        try {
            File musicPath = new File(filePath);
            if (musicPath.exists()) {
                AudioInputStream audioInput = AudioSystem.getAudioInputStream(musicPath);
                backgroundMusic = AudioSystem.getClip();
                backgroundMusic.open(audioInput);
                applyAudioSettings();
                backgroundMusic.loop(Clip.LOOP_CONTINUOUSLY);
                backgroundMusic.start();
            }
        } catch (Exception e) {}
    }

    public void playHoverSound(String filePath) {
        try {
            File sfxPath = new File(filePath);
            if (sfxPath.exists()) {
                AudioInputStream audioInput = AudioSystem.getAudioInputStream(sfxPath);
                Clip sfxClip = AudioSystem.getClip();
                sfxClip.open(audioInput);
                if (sfxSlider != null && masterSlider != null) {
                    FloatControl gainControl = (FloatControl) sfxClip.getControl(FloatControl.Type.MASTER_GAIN);
                    gainControl.setValue(calculateGain(sfxSlider.getValue(), masterSlider.getValue()));
                }
                sfxClip.start();
            }
        } catch (Exception e) {}
    }

    private void applyVideoSettings() {
        String selectedMode = (String) modeCombo.getSelectedItem();
        String selectedRes = (String) resCombo.getSelectedItem();
        String selectedHz = (String) hzCombo.getSelectedItem();

        int w = 1280, h = 720;
        if (selectedRes.equals("1920 x 1080")) { w = 1920; h = 1080; }
        else if (selectedRes.equals("1600 x 900")) { w = 1600; h = 900; }
        else if (selectedRes.equals("1366 x 768")) { w = 1366; h = 768; }

        int hz = Integer.parseInt(selectedHz.replace(" Hz", ""));

        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        dispose();

        if (gd.getFullScreenWindow() != null) gd.setFullScreenWindow(null);

        try {
            if (selectedMode.equals("Windowed")) {
                setUndecorated(false); setSize(w, h); setLocationRelativeTo(null);
            } else if (selectedMode.equals("Borderless")) {
                setUndecorated(true); setSize(w, h); setLocationRelativeTo(null);
            } else if (selectedMode.equals("Full Screen")) {
                setUndecorated(true); gd.setFullScreenWindow(this);
                if (gd.isDisplayChangeSupported()) {
                    try {
                        gd.setDisplayMode(new DisplayMode(w, h, DisplayMode.BIT_DEPTH_MULTI, hz));
                    } catch (IllegalArgumentException ex) {}
                }
            }
        } catch (Exception e) {}

        setVisible(true);
        optionsBox.setBounds(0, 0, getWidth(), getHeight());
        loadBox.setBounds(0, 0, getWidth(), getHeight());
        menuBox.setBounds(getWidth() - 350, (getHeight() - 460) / 2, 280, 460);
    }

    private class BackgroundPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            if (backgroundImage != null) {
                g2d.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), null);
            } else {
                g2d.setPaint(new GradientPaint(0, 0, new Color(35, 25, 20), getWidth(), getHeight(), new Color(10, 8, 5)));
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }

            if (!isOverlayOpen) {
                g2d.setFont(new Font("Georgia", Font.BOLD, 68));
                g2d.setColor(new Color(0, 0, 0, 80)); g2d.drawString("Heart&Horde", 85, 105);
                g2d.setColor(new Color(0, 0, 0, 200)); g2d.drawString("Heart&Horde", 82, 102);
                g2d.setPaint(new GradientPaint(80, 40, new Color(255, 252, 235), 80, 105, new Color(200, 160, 90)));
                g2d.drawString("Heart&Horde", 80, 100);

                g2d.setFont(new Font("Georgia", Font.ITALIC, 28));
                g2d.setColor(new Color(0, 0, 0, 200)); g2d.drawString("Bloodshed in Cryonia", 107, 142);
                g2d.setPaint(new GradientPaint(105, 120, new Color(220, 220, 220), 105, 145, new Color(150, 145, 140)));
                g2d.drawString("Bloodshed in Cryonia", 105, 140);
            }
        }
    }

    private JPanel createMenuBox() {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(new Color(20, 15, 12, 190));
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            }
        };

        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(160, 130, 95, 80), 1),
                BorderFactory.createEmptyBorder(25, 20, 25, 20)));

        JLabel menuTitle = new JLabel("MAIN MENU");
        menuTitle.setFont(new Font("Serif", Font.BOLD, 18));
        menuTitle.setForeground(new Color(215, 195, 165));
        menuTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(menuTitle);
        panel.add(Box.createRigidArea(new Dimension(0, 25)));

        String[] menuItems = {"CONTINUE", "CAMPAIGN", "OPTIONS", "EXTRAS", "QUIT"};
        for (String item : menuItems) {
            panel.add(createMenuButton(item));
            panel.add(Box.createRigidArea(new Dimension(0, 14)));
        }

        return panel;
    }

    private JPanel createLoadBox() {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setColor(new Color(15, 10, 8, 245));
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };

        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.add(Box.createRigidArea(new Dimension(0, 120)));

        JLabel title = new JLabel("SELECT SAVE DATA");
        title.setFont(new Font("Georgia", Font.BOLD, 36));
        title.setForeground(new Color(230, 200, 150));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(title);
        panel.add(Box.createRigidArea(new Dimension(0, 60)));

        slot1Btn = createSlotButton(1);
        slot2Btn = createSlotButton(2);
        slot3Btn = createSlotButton(3);

        panel.add(slot1Btn); panel.add(Box.createRigidArea(new Dimension(0, 20)));
        panel.add(slot2Btn); panel.add(Box.createRigidArea(new Dimension(0, 20)));
        panel.add(slot3Btn); panel.add(Box.createRigidArea(new Dimension(0, 60)));

        JButton btnBack = createMenuButton("BACK TO MENU");
        btnBack.setMaximumSize(new Dimension(300, 45));
        panel.add(btnBack);

        return panel;
    }

    private JPanel createOptionsBox() {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setColor(new Color(15, 10, 8, 245));
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };

        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.add(Box.createRigidArea(new Dimension(0, 80)));

        JLabel title = new JLabel("SETTINGS & OPTIONS");
        title.setFont(new Font("Georgia", Font.BOLD, 36)); title.setForeground(new Color(230, 200, 150));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(title); panel.add(Box.createRigidArea(new Dimension(0, 50)));

        JLabel dLabel = new JLabel("✦ GRAPHICS & DISPLAY ✦");
        dLabel.setFont(new Font("Serif", Font.BOLD, 18)); dLabel.setForeground(new Color(170, 150, 120));
        dLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(dLabel); panel.add(Box.createRigidArea(new Dimension(0, 20)));

        modeCombo = createStyledComboBox(new String[]{"Windowed", "Borderless", "Full Screen"});
        modeCombo.setSelectedItem("Full Screen");
        JPanel modeP = createSettingRow("Window Mode:"); modeP.add(modeCombo, BorderLayout.EAST);
        panel.add(modeP); panel.add(Box.createRigidArea(new Dimension(0, 15)));

        resCombo = createStyledComboBox(new String[]{"1920 x 1080", "1600 x 900", "1366 x 768", "1280 x 720"});
        JPanel resP = createSettingRow("Display Resolution:"); resP.add(resCombo, BorderLayout.EAST);
        panel.add(resP); panel.add(Box.createRigidArea(new Dimension(0, 15)));

        hzCombo = createStyledComboBox(new String[]{"60 Hz", "75 Hz", "120 Hz", "144 Hz"});
        hzCombo.setSelectedItem("60 Hz");
        JPanel hzP = createSettingRow("Refresh Rate (Hz):"); hzP.add(hzCombo, BorderLayout.EAST);
        panel.add(hzP); panel.add(Box.createRigidArea(new Dimension(0, 50)));

        JLabel aLabel = new JLabel("✦ AUDIO & SOUND ✦");
        aLabel.setFont(new Font("Serif", Font.BOLD, 18)); aLabel.setForeground(new Color(170, 150, 120));
        aLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(aLabel); panel.add(Box.createRigidArea(new Dimension(0, 20)));

        panel.add(createSliderRow("Master Volume:", masterSlider)); panel.add(Box.createRigidArea(new Dimension(0, 15)));
        panel.add(createSliderRow("Music Volume:", musicSlider)); panel.add(Box.createRigidArea(new Dimension(0, 15)));
        panel.add(createSliderRow("Sound Effects (SFX):", sfxSlider)); panel.add(Box.createRigidArea(new Dimension(0, 60)));

        JButton btnBack = createMenuButton("APPLY & BACK");
        btnBack.setMaximumSize(new Dimension(300, 45));
        panel.add(btnBack);

        return panel;
    }

    private JPanel createSettingRow(String labelText) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false); row.setMaximumSize(new Dimension(550, 35));
        JLabel label = new JLabel(labelText); label.setFont(new Font("Serif", Font.PLAIN, 18)); label.setForeground(Color.WHITE);
        row.add(label, BorderLayout.WEST);
        return row;
    }

    private JSlider createStyledSlider(int def) {
        JSlider s = new JSlider(0, 100, def); s.setOpaque(false); s.setForeground(new Color(200, 160, 90)); return s;
    }

    private JPanel createSliderRow(String label, JSlider s) {
        JPanel r = createSettingRow(label); r.add(s, BorderLayout.CENTER);
        ((JLabel)r.getComponent(0)).setPreferredSize(new Dimension(250, 30)); return r;
    }

    private JComboBox<String> createStyledComboBox(String[] items) {
        JComboBox<String> cb = new JComboBox<>(items);
        cb.setFont(new Font("Serif", Font.PLAIN, 16)); cb.setBackground(new Color(45, 15, 10)); cb.setForeground(Color.WHITE);
        cb.setBorder(BorderFactory.createLineBorder(new Color(130, 85, 45), 1)); cb.setPreferredSize(new Dimension(220, 30));
        return cb;
    }

    private void openGameplay() {
        GamePanel gamePanel = new GamePanel(MainMenuGame.this);
        setContentPane(gamePanel);
        revalidate();
        repaint();
        gamePanel.requestFocusInWindow();
    }

    private JButton createSlotButton(int slotIndex) {
        JButton button = new JButton("SAVE SLOT " + slotIndex) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isRollover()) g2d.setPaint(new GradientPaint(0, 0, new Color(45, 55, 75), 0, getHeight(), new Color(20, 25, 35)));
                else g2d.setPaint(new GradientPaint(0, 0, new Color(30, 35, 45), 0, getHeight(), new Color(15, 18, 25)));

                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
                g2d.setColor(getModel().isRollover() ? new Color(150, 180, 220) : new Color(80, 100, 130));
                g2d.setStroke(new BasicStroke(1.5f));
                g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 4, 4);

                g2d.setColor(getModel().isRollover() ? Color.WHITE : new Color(200, 200, 200));
                g2d.setFont(new Font("Serif", Font.BOLD, 18));
                FontMetrics fm = g2d.getFontMetrics();
                g2d.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2d.dispose();
            }
        };
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setMaximumSize(new Dimension(400, 55));
        button.setFocusPainted(false); button.setBorderPainted(false); button.setContentAreaFilled(false);

        // Path langsung diarahkan ke folder music
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) { playHoverSound("assets/music/342200__christopherderp__videogame-menu-button-click.wav"); }
        });

        button.addActionListener(e -> {
            if (loadGameData(slotIndex)) {
                System.out.println("Memuat Slot " + slotIndex + "...");
                openGameplay();
            } else {
                JOptionPane.showMessageDialog(this, "Slot " + slotIndex + " kosong! Buat Campaign baru terlebih dahulu.", "Data Not Found", JOptionPane.WARNING_MESSAGE);
            }
        });
        return button;
    }

    private JButton createMenuButton(String text) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isRollover()) g2d.setPaint(new GradientPaint(0, 0, new Color(135, 35, 25), 0, getHeight(), new Color(75, 15, 10)));
                else g2d.setPaint(new GradientPaint(0, 0, new Color(85, 25, 15), 0, getHeight(), new Color(45, 10, 5)));
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);

                g2d.setColor(getModel().isRollover() ? new Color(230, 185, 110) : new Color(130, 85, 45));
                g2d.setStroke(new BasicStroke(1.5f));
                g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 4, 4);

                g2d.setColor(getModel().isRollover() ? new Color(255, 245, 220) : Color.WHITE);
                g2d.setFont(new Font("Serif", Font.PLAIN, 15));
                FontMetrics fm = g2d.getFontMetrics();
                g2d.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2d.dispose();
            }
        };
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setMaximumSize(new Dimension(230, 42));
        button.setFocusPainted(false); button.setBorderPainted(false); button.setContentAreaFilled(false);

        // Path langsung diarahkan ke folder music
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) { playHoverSound("assets/music/342200__christopherderp__videogame-menu-button-click.wav"); }
        });

        button.addActionListener(e -> {
            switch (text) {
                case "CONTINUE":
                    updateLoadBoxUI();
                    isOverlayOpen = true;
                    menuBox.setVisible(false);
                    loadBox.setVisible(true);
                    mainPanel.repaint();
                    break;
                case "CAMPAIGN":
                    System.out.println("Memulai Campaign Baru (Reset Data)...");
                    savedBuildings.clear();
                    openGameplay();
                    break;
                case "OPTIONS":
                    isOverlayOpen = true; menuBox.setVisible(false); optionsBox.setVisible(true); mainPanel.repaint();
                    break;
                case "BACK TO MENU":
                case "APPLY & BACK":
                    if (text.equals("APPLY & BACK")) { applyVideoSettings(); applyAudioSettings(); }
                    isOverlayOpen = false;
                    optionsBox.setVisible(false);
                    loadBox.setVisible(false);
                    menuBox.setVisible(true);
                    mainPanel.repaint();
                    break;
                case "QUIT": System.exit(0); break;
            }
        });
        return button;
    }

    private class GamePanel extends JPanel {
        private MainMenuGame frame;
        private BufferedImage gameplayBg;
        private BufferedImage houseImage;

        private Camera camera = new Camera();

        // --- Tambahkan di bawah private final int PAN_SPEED = 20; ---
        private boolean wPressed = false;
        private boolean sPressed = false;
        private boolean aPressed = false;
        private boolean dPressed = false;
        private Timer cameraTimer;

        private enum ToolMode { NONE, BUILD, MOVE, DELETE }
        private ToolMode currentTool = ToolMode.NONE;

        private int mouseX = -100;
        private int mouseY = -100;
        private Building holdingBuilding = null;

        private final int houseWidth = 90;
        private final int houseHeight = 90;

        // Tambahkan di bawah private final int houseHeight = 90;
        private JPanel bottomLeftBar;
        private JPanel topRightBar;
        private JButton menuBtn;





        public GamePanel(MainMenuGame frame) {
            this.frame = frame;
            setLayout(null);

            cameraTimer = new Timer(16, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    // Cukup suruh kamera bergerak, lalu cek apakah dia benar-benar bergeser
                    boolean moved = camera.move(wPressed, sPressed, aPressed, dPressed);
                    if (moved) repaint();
                }
            });
            cameraTimer.start(); // Nyalakan mesinnya




            // --- Logika Zoom (Ctrl + Scroll) dengan Anchor Tengah ---
            addMouseWheelListener(e -> {
                if (e.isControlDown()) {
                    camera.zoomInOut(e.getWheelRotation(), getWidth(), getHeight());
                    repaint();
                }
            });

            // --- Logika Pan/Geser (WASD - Mulus & Anti Hilang Fokus) ---
            // W Key (Ditekan & Dilepas)
            getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_W, 0, false), "wPress");
            getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_W, 0, true), "wRelease");
            getActionMap().put("wPress", new AbstractAction() { public void actionPerformed(ActionEvent e) { wPressed = true; }});
            getActionMap().put("wRelease", new AbstractAction() { public void actionPerformed(ActionEvent e) { wPressed = false; }});

            // S Key (Ditekan & Dilepas)
            getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_S, 0, false), "sPress");
            getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_S, 0, true), "sRelease");
            getActionMap().put("sPress", new AbstractAction() { public void actionPerformed(ActionEvent e) { sPressed = true; }});
            getActionMap().put("sRelease", new AbstractAction() { public void actionPerformed(ActionEvent e) { sPressed = false; }});

            // A Key (Ditekan & Dilepas)
            getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_A, 0, false), "aPress");
            getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_A, 0, true), "aRelease");
            getActionMap().put("aPress", new AbstractAction() { public void actionPerformed(ActionEvent e) { aPressed = true; }});
            getActionMap().put("aRelease", new AbstractAction() { public void actionPerformed(ActionEvent e) { aPressed = false; }});

            // D Key (Ditekan & Dilepas)
            getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0, false), "dPress");
            getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0, true), "dRelease");
            getActionMap().put("dPress", new AbstractAction() { public void actionPerformed(ActionEvent e) { dPressed = true; }});
            getActionMap().put("dRelease", new AbstractAction() { public void actionPerformed(ActionEvent e) { dPressed = false; }});



            try {
                gameplayBg = ImageIO.read(new File("assets/img/bg.png"));
                houseImage = ImageIO.read(new File("assets/img/house_h&h.png"));
            } catch (Exception e) {
                System.out.println("Gagal memuat visual game panel di folder assets/img/");
            }

            getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_B, 0), "toolBuild");
            getActionMap().put("toolBuild", new AbstractAction() { public void actionPerformed(ActionEvent e) { currentTool = ToolMode.BUILD; holdingBuilding = null; repaint(); }});

            getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_M, 0), "toolMove");
            getActionMap().put("toolMove", new AbstractAction() { public void actionPerformed(ActionEvent e) { currentTool = ToolMode.MOVE; repaint(); }});

            getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_X, 0), "toolDelete");
            getActionMap().put("toolDelete", new AbstractAction() { public void actionPerformed(ActionEvent e) { currentTool = ToolMode.DELETE; holdingBuilding = null; repaint(); }});

            addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    // Terjemahkan dulu posisi X dan Y layar ke dunia game
                    Point worldPos = camera.toWorld(e.getX(), e.getY());
                    mouseX = worldPos.x;
                    mouseY = worldPos.y;

                    if (currentTool != ToolMode.NONE) repaint();
                }
            });

            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    // Cegah klik nembus ke tanah kalau lagi klik UI
                    if (bottomLeftBar != null && topRightBar != null) {
                        if (bottomLeftBar.getBounds().contains(e.getPoint()) || topRightBar.getBounds().contains(e.getPoint())) {
                            return;
                        }
                    }

                    if (SwingUtilities.isRightMouseButton(e)) {
                        if (holdingBuilding != null) {
                            frame.savedBuildings.add(holdingBuilding);
                            holdingBuilding = null;
                        }
                        currentTool = ToolMode.NONE;
                        repaint();
                        return;
                    }

                    if (!SwingUtilities.isLeftMouseButton(e)) return;

                    Point worldPos = camera.toWorld(e.getX(), e.getY());

                    int targetX = worldPos.x - (houseWidth / 2);
                    int targetY = worldPos.y - (houseHeight / 2);
                    Rectangle newArea = new Rectangle(targetX, targetY, houseWidth, houseHeight);

                    if (currentTool == ToolMode.BUILD) {
                        if (!isOverlapping(newArea, null)) {
                            frame.savedBuildings.add(new Building(targetX, targetY, houseWidth, houseHeight));
                            frame.playHoverSound("assets/music/342200__christopherderp__videogame-menu-button-click.wav");
                        }
                    }
                    else if (currentTool == ToolMode.MOVE) {
                        if (holdingBuilding == null) {
                            for (int i = frame.savedBuildings.size() - 1; i >= 0; i--) {
                                if (frame.savedBuildings.get(i).contains(worldPos)) {
                                    holdingBuilding = frame.savedBuildings.remove(i);
                                    frame.playHoverSound("assets/music/342200__christopherderp__videogame-menu-button-click.wav");
                                    break;
                                }
                            }
                        } else {
                            if (!isOverlapping(newArea, holdingBuilding)) {
                                frame.savedBuildings.add(new Building(targetX, targetY, houseWidth, houseHeight));
                                holdingBuilding = null;
                                frame.playHoverSound("assets/music/342200__christopherderp__videogame-menu-button-click.wav");
                            }
                        }
                    }
                    else if (currentTool == ToolMode.DELETE) {
                        for (int i = frame.savedBuildings.size() - 1; i >= 0; i--) {
                            if (frame.savedBuildings.get(i).contains(worldPos)) {
                                frame.savedBuildings.remove(i);
                                frame.playHoverSound("assets/music/342200__christopherderp__videogame-menu-button-click.wav");
                                break;
                            }
                        }
                    }
                    repaint();
                }
            });

            setupHUD();

            // Supaya UI tetap nempel di pojok saat ganti resolusi
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







        private boolean isOverlapping(Rectangle newRect, Building ignoreBuilding) {
            for (Building b : frame.savedBuildings) {
                if (b != ignoreBuilding && b.intersects(newRect)) return true;
            }
            return false;
        }
        private JButton createRTSButton(String text, ToolMode modeAction) {
            JButton btn = new JButton(text) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2d = (Graphics2D) g.create();
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    if (currentTool == modeAction && modeAction != ToolMode.NONE) {
                        g2d.setColor(new Color(180, 50, 40));
                    } else if (getModel().isRollover()) {
                        g2d.setColor(new Color(90, 30, 20));
                    } else {
                        g2d.setColor(new Color(40, 25, 20));
                    }

                    g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
                    g2d.setColor(new Color(160, 120, 70));
                    g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 4, 4);

                    g2d.setColor(Color.WHITE); g2d.setFont(new Font("Serif", Font.BOLD, 14));
                    FontMetrics fm = g2d.getFontMetrics();
                    g2d.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                    g2d.dispose();
                }
            };
            btn.setPreferredSize(new Dimension(130, 50));
            btn.setContentAreaFilled(false); btn.setBorderPainted(false); btn.setFocusPainted(false);

            if (modeAction != ToolMode.NONE) {
                btn.addActionListener(e -> {
                    if (holdingBuilding != null) { frame.savedBuildings.add(holdingBuilding); holdingBuilding = null; }
                    currentTool = modeAction; repaint();
                });
            }
            return btn;
        }


        private void setupHUD() {

            // HUD KANAN ATAS
            topRightBar = new JPanel() {
                protected void paintComponent(Graphics g) {
                    g.setColor(new Color(30, 25, 45, 240));
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
            };
            topRightBar.setLayout(null); topRightBar.setOpaque(false);

            // Dummy Indikator Ekonomi
            String[] stats = {"😊 100", "🍖 500", "💧 300", "💰 1250", "📜 5%"};
            int statX = 20;
            for (String s : stats) {
                JLabel l = new JLabel(s); l.setForeground(Color.WHITE);
                l.setFont(new Font("Segoe UI Emoji", Font.BOLD, 16));
                l.setBounds(statX, 10, 100, 30); topRightBar.add(l); statX += 100;
            }

            // Tombol Menu Putih
            menuBtn = new JButton() {
                protected void paintComponent(Graphics g) { g.setColor(Color.WHITE); g.fillRect(0, 0, getWidth(), getHeight()); }
            };
            menuBtn.setContentAreaFilled(false); menuBtn.setBorderPainted(false);
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
            bottomLeftBar.add(atkBtn);


            add(bottomLeftBar);
            repaint();
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

                    g2d.setColor(new Color(255, 255, 255, 80));
                    g2d.drawRoundRect(1, 1, getWidth()-3, getHeight()-3, 6, 6);
                    g2d.setColor(new Color(0, 0, 0, 100));
                    g2d.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 6, 6);

                    if (currentTool == mode && mode != ToolMode.NONE) {
                        g2d.setColor(new Color(255, 215, 0));
                        g2d.setStroke(new BasicStroke(3));
                        g2d.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, 6, 6);
                    }

                    g2d.setColor(Color.WHITE);
                    g2d.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
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

                    g2d.setColor(new Color(255, 255, 255, 100));
                    g2d.drawOval(1, 1, getWidth()-3, getHeight()-3);

                    g2d.setColor(Color.WHITE);
                    g2d.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
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

            saveItem.addActionListener(e -> {
                String[] options = {"Slot 1", "Slot 2", "Slot 3"};
                int choice = JOptionPane.showOptionDialog(frame, "Pilih Slot Penyimpanan:", "Save Progress", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
                if (choice >= 0) frame.saveGameData(choice + 1);
            });
            mainMenuItem.addActionListener(e -> {
                if (holdingBuilding != null) frame.savedBuildings.add(holdingBuilding);
                frame.setContentPane(frame.mainPanel); frame.revalidate(); frame.repaint();
            });

            popup.add(saveItem); popup.addSeparator(); popup.add(mainMenuItem);
            popup.show(this, getWidth() / 2 - 75, getHeight() / 2 - 50);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            // 1. Jalankan kalkulasi pembatas kamera
            camera.clamp(getWidth(), getHeight(), 3000, 2000);

            // 2. Kloning Kuas (WAJIB: agar bar HUD di layar tidak ikut ke-zoom/kegeser)
            Graphics2D g2d = (Graphics2D) g.create();

            // 3. Terapkan posisi dan zoom kamera
            camera.applyTransform(g2d);

            // 4. Render Background Dunia
            if (gameplayBg != null) {
                g2d.drawImage(gameplayBg, 0, 0, 3000, 2000, null);
            } else {
                g2d.setColor(new Color(30, 50, 30));
                g2d.fillRect(0, 0, 3000, 2000);
            }

            // 5. Render Rumah
            for (Building b : frame.savedBuildings) {
                b.draw(g2d, houseImage); // Kita cukup suruh bangunannya menggambar dirinya sendiri!
            }

            // 6. Render Preview Kursor
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
                g2d.setColor(new Color(255, 0, 0, 100));
                g2d.fillOval(mouseX - 25, mouseY - 25, 50, 50);
            }
            else if (currentTool == ToolMode.MOVE && holdingBuilding == null) {
                g2d.setColor(new Color(0, 200, 255, 100));
                g2d.fillOval(mouseX - 25, mouseY - 25, 50, 50);
            }
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));

            // 7. Buang Kuas Kloning (Agar kembalinya bersih untuk HUD)
            g2d.dispose();
        }


    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainMenuGame());
    }
}