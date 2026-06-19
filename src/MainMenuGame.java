import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import javax.sound.sampled.*;

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
    public List<Rectangle> savedHouses = new ArrayList<>();

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
            oos.writeObject(savedHouses);
            JOptionPane.showMessageDialog(this, "Progress kota berhasil disimpan di Slot " + slot + "!", "Save Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            System.out.println("Gagal menyimpan game: " + e.getMessage());
        }
    }

    public boolean loadGameData(int slot) {
        File file = new File("heart_save_" + slot + ".dat");
        if (!file.exists()) return false;

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            savedHouses = (List<Rectangle>) ois.readObject();
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
                    savedHouses.clear();
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

        private enum ToolMode { NONE, BUILD, MOVE, DELETE }
        private ToolMode currentTool = ToolMode.NONE;

        private int mouseX = -100;
        private int mouseY = -100;
        private Rectangle holdingHouse = null;

        private final int houseWidth = 90;
        private final int houseHeight = 90;

        public GamePanel(MainMenuGame frame) {
            this.frame = frame;
            setLayout(new BorderLayout());

            // Path langsung diarahkan ke folder img
            try {
                gameplayBg = ImageIO.read(new File("assets/img/bg.png"));
                houseImage = ImageIO.read(new File("assets/img/house_h&h.png"));
            } catch (Exception e) {
                System.out.println("Gagal memuat visual game panel di folder assets/img/");
            }

            getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_B, 0), "toolBuild");
            getActionMap().put("toolBuild", new AbstractAction() { public void actionPerformed(ActionEvent e) { currentTool = ToolMode.BUILD; holdingHouse = null; repaint(); }});

            getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_M, 0), "toolMove");
            getActionMap().put("toolMove", new AbstractAction() { public void actionPerformed(ActionEvent e) { currentTool = ToolMode.MOVE; repaint(); }});

            getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_X, 0), "toolDelete");
            getActionMap().put("toolDelete", new AbstractAction() { public void actionPerformed(ActionEvent e) { currentTool = ToolMode.DELETE; holdingHouse = null; repaint(); }});

            addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    mouseX = e.getX(); mouseY = e.getY();
                    if (currentTool != ToolMode.NONE) repaint();
                }
            });

            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (SwingUtilities.isRightMouseButton(e)) {
                        if (holdingHouse != null) {
                            frame.savedHouses.add(holdingHouse);
                            holdingHouse = null;
                        }
                        currentTool = ToolMode.NONE;
                        repaint();
                        return;
                    }

                    if (!SwingUtilities.isLeftMouseButton(e)) return;

                    int targetX = e.getX() - (houseWidth / 2);
                    int targetY = e.getY() - (houseHeight / 2);
                    Rectangle newArea = new Rectangle(targetX, targetY, houseWidth, houseHeight);

                    if (currentTool == ToolMode.BUILD) {
                        if (!isOverlapping(newArea, null)) {
                            frame.savedHouses.add(newArea);
                            frame.playHoverSound("assets/music/342200__christopherderp__videogame-menu-button-click.wav");
                        }
                    }
                    else if (currentTool == ToolMode.MOVE) {
                        if (holdingHouse == null) {
                            for (int i = frame.savedHouses.size() - 1; i >= 0; i--) {
                                if (frame.savedHouses.get(i).contains(e.getPoint())) {
                                    holdingHouse = frame.savedHouses.remove(i);
                                    frame.playHoverSound("assets/music/342200__christopherderp__videogame-menu-button-click.wav");
                                    break;
                                }
                            }
                        } else {
                            if (!isOverlapping(newArea, holdingHouse)) {
                                frame.savedHouses.add(newArea);
                                holdingHouse = null;
                                frame.playHoverSound("assets/music/342200__christopherderp__videogame-menu-button-click.wav");
                            }
                        }
                    }
                    else if (currentTool == ToolMode.DELETE) {
                        for (int i = frame.savedHouses.size() - 1; i >= 0; i--) {
                            if (frame.savedHouses.get(i).contains(e.getPoint())) {
                                frame.savedHouses.remove(i);
                                frame.playHoverSound("assets/music/342200__christopherderp__videogame-menu-button-click.wav");
                                break;
                            }
                        }
                    }
                    repaint();
                }
            });

            JPanel commandCard = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2d = (Graphics2D) g;
                    g2d.setColor(new Color(15, 12, 10, 230));
                    g2d.fillRect(0, 0, getWidth(), getHeight());
                    g2d.setColor(new Color(130, 85, 45));
                    g2d.setStroke(new BasicStroke(3f));
                    g2d.drawLine(0, 0, getWidth(), 0);
                }
            };
            commandCard.setPreferredSize(new Dimension(frame.getWidth(), 110));
            commandCard.setLayout(new BorderLayout());

            JLabel infoLabel = new JLabel("  HEART & HORDE : CRYONIA SETTLEMENT  ");
            infoLabel.setFont(new Font("Georgia", Font.BOLD, 18));
            infoLabel.setForeground(new Color(200, 160, 90));
            commandCard.add(infoLabel, BorderLayout.WEST);

            JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 25));
            actionPanel.setOpaque(false);

            actionPanel.add(createRTSButton("BUILD (B)", ToolMode.BUILD));
            actionPanel.add(createRTSButton("MOVE (M)", ToolMode.MOVE));
            actionPanel.add(createRTSButton("DELETE (X)", ToolMode.DELETE));

            JButton btnSave = createRTSButton("SAVE (S)", ToolMode.NONE);
            btnSave.addActionListener(e -> {
                String[] options = {"Slot 1", "Slot 2", "Slot 3"};
                int choice = JOptionPane.showOptionDialog(frame, "Pilih Slot Penyimpanan:", "Save Progress",
                        JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

                if (choice >= 0) {
                    frame.saveGameData(choice + 1);
                }
            });
            actionPanel.add(btnSave);

            JButton btnMenu = createRTSButton("MAIN MENU", ToolMode.NONE);
            btnMenu.addActionListener(e -> {
                if (holdingHouse != null) frame.savedHouses.add(holdingHouse);
                frame.setContentPane(frame.mainPanel);
                frame.revalidate(); frame.repaint();
            });
            actionPanel.add(btnMenu);

            commandCard.add(actionPanel, BorderLayout.EAST);
            add(commandCard, BorderLayout.SOUTH);
        }

        private boolean isOverlapping(Rectangle newRect, Rectangle ignoreRect) {
            for (Rectangle h : frame.savedHouses) {
                if (h != ignoreRect && newRect.intersects(h)) return true;
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
                    if (holdingHouse != null) { frame.savedHouses.add(holdingHouse); holdingHouse = null; }
                    currentTool = modeAction; repaint();
                });
            }
            return btn;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;

            if (gameplayBg != null) g2d.drawImage(gameplayBg, 0, 0, getWidth(), getHeight(), null);
            else { g2d.setColor(new Color(30, 50, 30)); g2d.fillRect(0, 0, getWidth(), getHeight()); }

            for (Rectangle h : frame.savedHouses) {
                if (houseImage != null) g2d.drawImage(houseImage, h.x, h.y, h.width, h.height, null);
                else { g2d.setColor(new Color(140, 70, 40)); g2d.fillRect(h.x, h.y, h.width, h.height); }
            }

            int pX = mouseX - (houseWidth / 2);
            int pY = mouseY - (houseHeight / 2);
            Rectangle previewRect = new Rectangle(pX, pY, houseWidth, houseHeight);

            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));

            if (currentTool == ToolMode.BUILD || (currentTool == ToolMode.MOVE && holdingHouse != null)) {
                if (isOverlapping(previewRect, null)) {
                    g2d.setColor(new Color(255, 0, 0, 150)); g2d.fillRect(pX, pY, houseWidth, houseHeight);
                } else {
                    if (houseImage != null) g2d.drawImage(houseImage, pX, pY, houseWidth, houseHeight, null);
                    else { g2d.setColor(new Color(200, 200, 200, 150)); g2d.fillRect(pX, pY, houseWidth, houseHeight); }
                }
            }
            else if (currentTool == ToolMode.DELETE) {
                g2d.setColor(new Color(255, 0, 0, 100));
                g2d.fillOval(mouseX - 25, mouseY - 25, 50, 50);
            }
            else if (currentTool == ToolMode.MOVE && holdingHouse == null) {
                g2d.setColor(new Color(0, 200, 255, 100));
                g2d.fillOval(mouseX - 25, mouseY - 25, 50, 50);
            }
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainMenuGame());
    }
}