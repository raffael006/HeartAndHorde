import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.List;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import javax.sound.sampled.*;

public class MenuPanel extends JPanel {
    private GameWindow window; // Referensi ke Bos
    private BufferedImage backgroundImage;

    // 1. DIUBAH MENJADI STATIC AGAR MUSIK PERSISTEN DI MEMORI
    private static Clip backgroundMusic;

    // Kotak-kotak Menu
    private JPanel menuBox;
    private JPanel optionsBox;
    private JPanel loadBox;
    private boolean isOverlayOpen = false;

    // Komponen Video & Audio
    private JComboBox<String> modeCombo;
    private JComboBox<String> resCombo;
    private JComboBox<String> hzCombo;
    private JSlider masterSlider;
    private JSlider musicSlider;
    private JSlider sfxSlider;

    // Tombol Save Slot
    private JButton slot1Btn, slot2Btn, slot3Btn;

    public MenuPanel(GameWindow window) {
        this.window = window;
        setLayout(null);

        try {
            backgroundImage = ImageIO.read(new File("assets/img/background.png"));
        } catch (Exception e) {
            System.out.println("Gagal memuat background.png");
        }

        masterSlider = createStyledSlider(75);
        musicSlider = createStyledSlider(75);
        sfxSlider = createStyledSlider(75);

        playBackgroundMusic("assets/music/Medieval Ambient Music (Crossing the Withered Vale).wav");

        // Buat 3 kotak menu
        menuBox = createMenuBox();
        add(menuBox);

        optionsBox = createOptionsBox();
        optionsBox.setVisible(false);
        add(optionsBox);

        loadBox = createLoadBox();
        loadBox.setVisible(false);
        add(loadBox);

        // Resize & State Listener agar menu selalu di tengah dan musik terpantau
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                menuBox.setBounds(getWidth() - 350, (getHeight() - 460) / 2, 280, 460);
                optionsBox.setBounds(0, 0, getWidth(), getHeight());
                loadBox.setBounds(0, 0, getWidth(), getHeight());
            }

            @Override
            public void componentShown(ComponentEvent e) {
                // 2. KETIKA KEMBALI DARI GAMEPLAY KE MENU (CardLayout berganti), MUSIK AKAN OTOMATIS DI-RESUME
                playBackgroundMusic("assets/music/Medieval Ambient Music (Crossing the Withered Vale).wav");
            }
        });
    }

    // ==========================================
    // SISTEM AUDIO & VIDEO
    // ==========================================
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

    // 3. REVISI LOGIKA PENGECEKAN PINTAR AGAR TIDAK MENUMPUK ATAU MENGULANG LAGU
    private void playBackgroundMusic(String filePath) {
        if (backgroundMusic != null) {
            // Jika musik sudah ada tapi sedang berhenti (misal habis keluar dari gameplay), langsung mainkan lagi
            if (!backgroundMusic.isRunning()) {
                backgroundMusic.loop(Clip.LOOP_CONTINUOUSLY);
                backgroundMusic.start();
            }
            return; // Keluar dari fungsi, jangan muat ulang file agar tidak restart dari detik 0
        }

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

        // Memodifikasi Jendela Bos (GameWindow)
        window.dispose();
        if (gd.getFullScreenWindow() != null) gd.setFullScreenWindow(null);

        try {
            if (selectedMode.equals("Windowed")) {
                window.setUndecorated(false); window.setSize(w, h); window.setLocationRelativeTo(null);
            } else if (selectedMode.equals("Borderless")) {
                window.setUndecorated(true); window.setSize(w, h); window.setLocationRelativeTo(null);
            } else if (selectedMode.equals("Full Screen")) {
                window.setUndecorated(true); gd.setFullScreenWindow(window);
                if (gd.isDisplayChangeSupported()) {
                    try { gd.setDisplayMode(new DisplayMode(w, h, DisplayMode.BIT_DEPTH_MULTI, hz)); }
                    catch (IllegalArgumentException ex) {}
                }
            }
        } catch (Exception e) {}

        window.setVisible(true);
    }

    // ==========================================
    // SISTEM LOAD GAME
    // ==========================================
    private boolean loadGameData(int slot) {
        File file = new File("heart_save_" + slot + ".dat");
        if (!file.exists()) return false;

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            // Masukkan data langsung ke variabel bos
            window.savedBuildings = (List<Building>) ois.readObject();
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


    // ==========================================
    // PEMBUAT UI (Menu, Options, Load)
    // ==========================================
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
        panel.add(menuTitle); panel.add(Box.createRigidArea(new Dimension(0, 25)));

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
                g.setColor(new Color(15, 10, 8, 245));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };

        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS)); panel.setOpaque(false);
        panel.add(Box.createRigidArea(new Dimension(0, 120)));

        JLabel title = new JLabel("SELECT SAVE DATA");
        title.setFont(new Font("Georgia", Font.BOLD, 36)); title.setForeground(new Color(230, 200, 150));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(title); panel.add(Box.createRigidArea(new Dimension(0, 60)));

        slot1Btn = createSlotButton(1); slot2Btn = createSlotButton(2); slot3Btn = createSlotButton(3);
        panel.add(slot1Btn); panel.add(Box.createRigidArea(new Dimension(0, 20)));
        panel.add(slot2Btn); panel.add(Box.createRigidArea(new Dimension(0, 20)));
        panel.add(slot3Btn); panel.add(Box.createRigidArea(new Dimension(0, 60)));

        JButton btnBack = createMenuButton("BACK TO MENU");
        btnBack.setMaximumSize(new Dimension(300, 45)); panel.add(btnBack);
        return panel;
    }

    private JPanel createOptionsBox() {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(new Color(15, 10, 8, 245));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };

        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS)); panel.setOpaque(false);
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

        hzCombo = createStyledComboBox(new String[]{"60 Hz", "75 Hz", "120 Hz", "144 Hz"}); hzCombo.setSelectedItem("60 Hz");
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
        btnBack.setMaximumSize(new Dimension(300, 45)); panel.add(btnBack);
        return panel;
    }

    // Komponen Pembantu UI
    private JPanel createSettingRow(String labelText) {
        JPanel row = new JPanel(new BorderLayout()); row.setOpaque(false); row.setMaximumSize(new Dimension(550, 35));
        JLabel label = new JLabel(labelText); label.setFont(new Font("Serif", Font.PLAIN, 18)); label.setForeground(Color.WHITE);
        row.add(label, BorderLayout.WEST); return row;
    }
    private JSlider createStyledSlider(int def) {
        JSlider s = new JSlider(0, 100, def); s.setOpaque(false); s.setForeground(new Color(200, 160, 90)); return s;
    }
    private JPanel createSliderRow(String label, JSlider s) {
        JPanel r = createSettingRow(label); r.add(s, BorderLayout.CENTER);
        ((JLabel)r.getComponent(0)).setPreferredSize(new Dimension(250, 30)); return r;
    }
    private JComboBox<String> createStyledComboBox(String[] items) {
        JComboBox<String> cb = new JComboBox<>(items); cb.setFont(new Font("Serif", Font.PLAIN, 16)); cb.setBackground(new Color(45, 15, 10)); cb.setForeground(Color.WHITE); cb.setBorder(BorderFactory.createLineBorder(new Color(130, 85, 45), 1)); cb.setPreferredSize(new Dimension(220, 30)); return cb;
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
                g2d.setStroke(new BasicStroke(1.5f)); g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 4, 4);
                g2d.setColor(getModel().isRollover() ? Color.WHITE : new Color(200, 200, 200));
                g2d.setFont(new Font("Serif", Font.BOLD, 18)); FontMetrics fm = g2d.getFontMetrics();
                g2d.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2d.dispose();
            }
        };
        button.setAlignmentX(Component.CENTER_ALIGNMENT); button.setMaximumSize(new Dimension(400, 55));
        button.setFocusPainted(false); button.setBorderPainted(false); button.setContentAreaFilled(false);
        button.addMouseListener(new MouseAdapter() { public void mouseEntered(MouseEvent evt) { playHoverSound("assets/music/342200__christopherderp__videogame-menu-button-click.wav"); }});

        button.addActionListener(e -> {
            if (loadGameData(slotIndex)) {
                if (backgroundMusic != null) backgroundMusic.stop();
                window.showScreen("GAME_SCREEN");
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
                g2d.setStroke(new BasicStroke(1.5f)); g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 4, 4);
                g2d.setColor(getModel().isRollover() ? new Color(255, 245, 220) : Color.WHITE);
                g2d.setFont(new Font("Serif", Font.PLAIN, 15)); FontMetrics fm = g2d.getFontMetrics();
                g2d.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2d.dispose();
            }
        };
        button.setAlignmentX(Component.CENTER_ALIGNMENT); button.setMaximumSize(new Dimension(230, 42));
        button.setFocusPainted(false); button.setBorderPainted(false); button.setContentAreaFilled(false);
        button.addMouseListener(new MouseAdapter() { public void mouseEntered(MouseEvent evt) { playHoverSound("assets/music/342200__christopherderp__videogame-menu-button-click.wav"); }});

        button.addActionListener(e -> {
            switch (text) {
                case "CONTINUE":
                    updateLoadBoxUI(); isOverlayOpen = true; menuBox.setVisible(false); loadBox.setVisible(true); repaint();
                    break;
                case "CAMPAIGN":
                    window.savedBuildings.clear(); // Hapus data lama
                    if (backgroundMusic != null) backgroundMusic.stop();
                    window.showScreen("GAME_SCREEN");
                    break;
                case "OPTIONS":
                    isOverlayOpen = true; menuBox.setVisible(false); optionsBox.setVisible(true); repaint();
                    break;
                case "BACK TO MENU":
                case "APPLY & BACK":
                    if (text.equals("APPLY & BACK")) { applyVideoSettings(); applyAudioSettings(); }
                    isOverlayOpen = false; optionsBox.setVisible(false); loadBox.setVisible(false); menuBox.setVisible(true); repaint();
                    break;
                case "QUIT": System.exit(0); break;
            }
        });
        return button;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (backgroundImage != null) g2d.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), null);
        else { g2d.setPaint(new GradientPaint(0, 0, new Color(35, 25, 20), getWidth(), getHeight(), new Color(10, 8, 5))); g2d.fillRect(0, 0, getWidth(), getHeight()); }

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