import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.List;
import java.util.Random;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import javax.sound.sampled.*;

public class MenuPanel extends JPanel {
    private GameWindow window;
    private BufferedImage backgroundImage;
    private static Clip backgroundMusic;

    // --- VARIABEL UNTUK ANIMASI FADE TO BLACK ---
    private boolean isTransitioning = false;
    private float fadeAlpha = 0.0f; // Tingkat kegelapan (0 = transparan, 1 = hitam pekat)
    private Timer fadeTimer;

    // Kotak-kotak Menu
    private JPanel menuBox;
    private JPanel optionsBox;
    private JPanel loadBox;
    private JPanel difficultyBox; // --- FITUR BARU: Pilihan Easy/Medium/Hard sebelum Campaign mulai ---
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

    // --- FITUR BARU: AMBIENT DARK TOWER DEFENSE — senada sama SplashPanel ---
    private Timer ambientTimer;
    private long tickCounter = 0;
    private final Random ambientRandom = new Random();

    private static class Ember {
        float x, y, size, speed, driftPhase, driftSpeed, baseAlpha;
    }
    private static class Spark {
        float x, y, size, speed, driftPhase, driftSpeed, baseAlpha, sway;
        boolean warm;
    }
    private static class Fog {
        float x, y, w, h, speed, baseAlpha;
    }
    private final List<Ember> ambientEmbers = new java.util.ArrayList<>();
    private final List<Spark> ambientSparks = new java.util.ArrayList<>();
    private final List<Fog> ambientFogs = new java.util.ArrayList<>();
    private static final int AMBIENT_EMBER_COUNT = 26;
    private static final int AMBIENT_SPARK_COUNT = 14;
    private static final int AMBIENT_FOG_COUNT = 4;

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

        menuBox = createMenuBox();
        add(menuBox);

        optionsBox = createOptionsBox();
        optionsBox.setVisible(false);
        add(optionsBox);

        loadBox = createLoadBox();
        loadBox.setVisible(false);
        add(loadBox);

        difficultyBox = createDifficultyBox();
        difficultyBox.setVisible(false);
        add(difficultyBox);

        // --- FITUR BARU: siapkan & jalankan ambient (kabut + partikel bara/debu) ---
        for (int i = 0; i < AMBIENT_EMBER_COUNT; i++) ambientEmbers.add(spawnAmbientEmber(true));
        for (int i = 0; i < AMBIENT_SPARK_COUNT; i++) ambientSparks.add(spawnAmbientSpark(true));
        for (int i = 0; i < AMBIENT_FOG_COUNT; i++) ambientFogs.add(spawnAmbientFog(true));

        ambientTimer = new Timer(30, e -> {
            tickCounter++;
            int w = Math.max(getWidth(), 1080);
            int h = Math.max(getHeight(), 720);

            for (int i = 0; i < ambientEmbers.size(); i++) {
                Ember em = ambientEmbers.get(i);
                em.y -= em.speed;
                em.x += Math.sin((tickCounter * 0.02) + em.driftPhase) * em.driftSpeed;
                if (em.y < -10) ambientEmbers.set(i, spawnAmbientEmber(false));
            }
            for (int i = 0; i < ambientSparks.size(); i++) {
                Spark sp = ambientSparks.get(i);
                sp.y -= sp.speed;
                sp.x += Math.sin((tickCounter * 0.03) + sp.driftPhase) * sp.driftSpeed;
                if (sp.y < -20) ambientSparks.set(i, spawnAmbientSpark(false));
            }
            for (Fog fog : ambientFogs) {
                fog.x += fog.speed;
                if (fog.x - fog.w / 2f > w) fog.x = -fog.w / 2f;
            }
            repaint();
        });
        ambientTimer.start();

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int menuW = 460, menuH = 300;
                menuBox.setBounds((getWidth() - menuW) / 2, getHeight() - menuH - 40, menuW, menuH);
                optionsBox.setBounds(0, 0, getWidth(), getHeight());
                loadBox.setBounds(0, 0, getWidth(), getHeight());
                difficultyBox.setBounds(0, 0, getWidth(), getHeight());
            }

            @Override
            public void componentShown(ComponentEvent e) {
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

    private void playBackgroundMusic(String filePath) {
        if (backgroundMusic != null) {
            if (!backgroundMusic.isRunning()) {
                backgroundMusic.loop(Clip.LOOP_CONTINUOUSLY);
                backgroundMusic.start();
            }
            return;
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

    @SuppressWarnings("unchecked")
    private boolean loadGameData(int slot) {
        File file = new File("heart_save_" + slot + ".dat");
        if (!file.exists()) return false;

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            window.savedBuildings = (List<Building>) ois.readObject();
            // --- FITUR BARU: Muat juga Civil/Guard/Horde/CivilBuilder/Mine dari save file baru ---
            // (Save file LAMA cuma punya savedBuildings -> readObject berikutnya bakal gagal/EOF,
            // makanya seluruh proses ini dibungkus try-catch yang sudah ada, biar aman gak crash,
            // cuma nanti dianggap "gagal memuat" kalau formatnya save lama.)
            window.activeCivils = (List<Civil>) ois.readObject();
            window.activeGuards = (List<Guard>) ois.readObject();
            window.activeHordes = (List<Horde>) ois.readObject();
            window.activeCivilBuilders = (List<CivilBuilder>) ois.readObject();
            if (window.gamePanel != null) {
                window.gamePanel.activeMines = (List<Mine>) ois.readObject();
            }
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
    // FUNGSI ANIMASI FADE TO BLACK (TRANSISI)
    // ==========================================
    private void startFadeTransition(Runnable dataSetupAction) {
        if (isTransitioning) return; // Cegah tombol diklik berkali-kali

        isTransitioning = true;
        fadeAlpha = 0.0f;

        // Musik otomatis berhenti saat animasi mulai gelap
        if (backgroundMusic != null) backgroundMusic.stop();

        fadeTimer = new Timer(20, evt -> {
            fadeAlpha += 0.03f; // Menentukan kecepatan transisi gelap

            if (fadeAlpha >= 1.0f) {
                fadeAlpha = 1.0f;
                fadeTimer.stop();

                // Menjalankan perintah (misal clear map untuk campaign)
                dataSetupAction.run();

                // Pindah Layar
                window.showScreen("GAME_SCREEN");

                // Kembalikan ke normal agar jika kembali ke menu tidak gelap
                isTransitioning = false;
                fadeAlpha = 0.0f;
                menuBox.setVisible(true);

                // --- FIX: reset SEMUA overlay, bukan cuma loadBox, biar gak numpuk
                // pas balik lagi ke MENU_SCREEN. Sebelumnya difficultyBox (dan optionsBox)
                // gak pernah di-set false di sini, jadi kalau alurnya lewat CAMPAIGN ->
                // pilih difficulty -> fade ke GAME_SCREEN -> nanti "Back to Main Menu",
                // difficultyBox masih nyala bareng menuBox (menumpuk).
                loadBox.setVisible(false);
                difficultyBox.setVisible(false);
                optionsBox.setVisible(false);
                isOverlayOpen = false;
            }
            repaint();
        });
        fadeTimer.start();
    }

    // ==========================================
    // PEMBUAT UI (Menu, Options, Load)
    // ==========================================
    private JPanel createMenuBox() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);

        String[] menuItems = {"CONTINUE", "CAMPAIGN", "OPTIONS", "EXTRAS", "QUIT"};
        for (String item : menuItems) {
            panel.add(createMenuButton(item));
            panel.add(Box.createRigidArea(new Dimension(0, 6)));
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
        title.setFont(GameWindow.VIKING_FONT.deriveFont(Font.BOLD, 36f)); title.setForeground(new Color(230, 200, 150));
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

    // --- FITUR BARU: OVERLAY PILIHAN DIFFICULTY (Easy / Medium / Hard) ---
    // Muncul begitu tombol CAMPAIGN diklik. Campaign baru benar-benar mulai
    // (fade to black -> resetCampaign(Difficulty)) setelah salah satu dipilih.
    private JPanel createDifficultyBox() {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(new Color(15, 10, 8, 245));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };

        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS)); panel.setOpaque(false);
        panel.add(Box.createRigidArea(new Dimension(0, 100)));

        JLabel title = new JLabel("SELECT DIFFICULTY");
        title.setFont(GameWindow.VIKING_FONT.deriveFont(Font.BOLD, 36f)); title.setForeground(new Color(230, 200, 150));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(title); panel.add(Box.createRigidArea(new Dimension(0, 10)));

        JLabel subtitle = new JLabel("Menentukan jenis, jumlah, dan kecepatan munculnya wave Horde");
        subtitle.setFont(GameWindow.VIKING_FONT.deriveFont(Font.ITALIC, 15f)); subtitle.setForeground(new Color(170, 150, 120));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(subtitle); panel.add(Box.createRigidArea(new Dimension(0, 50)));

        panel.add(createDifficultyButton("EASY", "Wave lebih jarang muncul, Horde lebih sedikit & lemah",
                new Color(90, 190, 110), GameWindow.Difficulty.EASY));
        panel.add(Box.createRigidArea(new Dimension(0, 22)));
        panel.add(createDifficultyButton("MEDIUM", "Keseimbangan standar antara ancaman dan waktu bersiap",
                new Color(220, 180, 70), GameWindow.Difficulty.MEDIUM));
        panel.add(Box.createRigidArea(new Dimension(0, 22)));
        panel.add(createDifficultyButton("HARD", "Wave datang lebih cepat, Horde lebih banyak & mematikan",
                new Color(210, 70, 60), GameWindow.Difficulty.HARD));
        panel.add(Box.createRigidArea(new Dimension(0, 55)));

        JButton btnBack = createMenuButton("BACK TO MENU");
        btnBack.setMaximumSize(new Dimension(300, 45)); panel.add(btnBack);
        return panel;
    }

    private JButton createDifficultyButton(String label, String desc, Color accent, GameWindow.Difficulty diff) {
        JButton button = new JButton(label) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean hover = getModel().isRollover();
                boolean pressed = getModel().isPressed();

                Color top = pressed ? accent.darker().darker() : (hover ? accent.darker() : new Color(30, 28, 25));
                Color bottom = pressed ? new Color(10, 10, 10) : (hover ? accent.darker().darker() : new Color(15, 13, 11));
                g2d.setPaint(new GradientPaint(0, 0, top, 0, getHeight(), bottom));
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);

                g2d.setColor(hover ? accent : accent.darker());
                g2d.setStroke(new BasicStroke(hover ? 2.2f : 1.4f));
                g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);

                g2d.setFont(GameWindow.VIKING_FONT.deriveFont(Font.BOLD, 22f));
                FontMetrics fmT = g2d.getFontMetrics();
                g2d.setColor(hover ? Color.WHITE : new Color(225, 220, 210));
                g2d.drawString(label, (getWidth() - fmT.stringWidth(label)) / 2, 30);

                g2d.setFont(GameWindow.VIKING_FONT.deriveFont(Font.ITALIC, 13f));
                FontMetrics fmD = g2d.getFontMetrics();
                g2d.setColor(new Color(190, 180, 165));
                g2d.drawString(desc, (getWidth() - fmD.stringWidth(desc)) / 2, 52);

                g2d.dispose();
            }
        };
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setMaximumSize(new Dimension(520, 68));
        button.setPreferredSize(new Dimension(520, 68));
        button.setFocusPainted(false); button.setBorderPainted(false); button.setContentAreaFilled(false);
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) { playHoverSound("assets/music/342200__christopherderp__videogame-menu-button-click.wav"); button.repaint(); }
            public void mouseExited(MouseEvent evt) { button.repaint(); }
        });

        button.addActionListener(e -> {
            if (isTransitioning) return; // Kunci biar ga spam klik
            window.selectedDifficulty = diff;
            startFadeTransition(() -> {
                // --- FITUR BARU: resetCampaign(Difficulty) -> bersihkan SEMUA state lama SEKALIGUS
                // atur jenis/jumlah/kecepatan Horde tiap wave sesuai difficulty yang dipilih ---
                window.gamePanel.resetCampaign(diff);
            });
        });
        return button;
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
        title.setFont(GameWindow.VIKING_FONT.deriveFont(Font.BOLD, 36f)); title.setForeground(new Color(230, 200, 150));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(title); panel.add(Box.createRigidArea(new Dimension(0, 50)));

        JLabel dLabel = new JLabel("✦ GRAPHICS & DISPLAY ✦");
        dLabel.setFont(GameWindow.VIKING_FONT.deriveFont(Font.BOLD, 18f)); dLabel.setForeground(new Color(170, 150, 120));
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
        aLabel.setFont(GameWindow.VIKING_FONT.deriveFont(Font.BOLD, 18f)); aLabel.setForeground(new Color(170, 150, 120));
        aLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(aLabel); panel.add(Box.createRigidArea(new Dimension(0, 20)));

        panel.add(createSliderRow("Master Volume:", masterSlider)); panel.add(Box.createRigidArea(new Dimension(0, 15)));
        panel.add(createSliderRow("Music Volume:", musicSlider)); panel.add(Box.createRigidArea(new Dimension(0, 15)));
        panel.add(createSliderRow("Sound Effects (SFX):", sfxSlider)); panel.add(Box.createRigidArea(new Dimension(0, 60)));

        JButton btnBack = createMenuButton("APPLY & BACK");
        btnBack.setMaximumSize(new Dimension(300, 45)); panel.add(btnBack);
        return panel;
    }

    private JPanel createSettingRow(String labelText) {
        JPanel row = new JPanel(new BorderLayout()); row.setOpaque(false); row.setMaximumSize(new Dimension(550, 35));
        JLabel label = new JLabel(labelText); label.setFont(GameWindow.VIKING_FONT.deriveFont(Font.PLAIN, 18f)); label.setForeground(Color.WHITE);
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
        JComboBox<String> cb = new JComboBox<>(items); cb.setFont(GameWindow.VIKING_FONT.deriveFont(Font.PLAIN, 16f)); cb.setBackground(new Color(45, 15, 10)); cb.setForeground(Color.WHITE); cb.setBorder(BorderFactory.createLineBorder(new Color(130, 85, 45), 1)); cb.setPreferredSize(new Dimension(220, 30)); return cb;
    }

    // --- FITUR BARU: Panah kecil pengapit teks menu yang lagi aktif/hover (gaya referensi) ---
    private void drawCaret(Graphics2D g2d, int cx, int cy, boolean pointLeft) {
        Polygon p = new Polygon();
        int size = 7;
        if (pointLeft) {
            p.addPoint(cx + size, cy - size);
            p.addPoint(cx - size, cy);
            p.addPoint(cx + size, cy + size);
        } else {
            p.addPoint(cx - size, cy - size);
            p.addPoint(cx + size, cy);
            p.addPoint(cx - size, cy + size);
        }
        g2d.fillPolygon(p);
    }

    private JButton createSlotButton(int slotIndex) {
        JButton button = new JButton("SAVE SLOT " + slotIndex) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int yOffset = 0;
                if (getModel().isPressed()) {
                    g2d.setPaint(new GradientPaint(0, 0, new Color(15, 20, 30), 0, getHeight(), new Color(5, 10, 15)));
                    g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
                    g2d.setColor(new Color(60, 80, 110));
                    g2d.setStroke(new BasicStroke(1.5f));
                    g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 4, 4);
                    g2d.setColor(new Color(150, 150, 150));
                    yOffset = 2;
                } else if (getModel().isRollover()) {
                    g2d.setPaint(new GradientPaint(0, 0, new Color(45, 55, 75), 0, getHeight(), new Color(20, 25, 35)));
                    g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
                    g2d.setColor(new Color(150, 180, 220));
                    g2d.setStroke(new BasicStroke(1.5f));
                    g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 4, 4);
                    g2d.setColor(Color.WHITE);
                } else {
                    g2d.setPaint(new GradientPaint(0, 0, new Color(30, 35, 45), 0, getHeight(), new Color(15, 18, 25)));
                    g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
                    g2d.setColor(new Color(80, 100, 130));
                    g2d.setStroke(new BasicStroke(1.5f));
                    g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 4, 4);
                    g2d.setColor(new Color(200, 200, 200));
                }
                g2d.setFont(GameWindow.VIKING_FONT.deriveFont(Font.BOLD, 18f));
                FontMetrics fm = g2d.getFontMetrics();
                g2d.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2, (getHeight() + fm.getAscent() - fm.getDescent()) / 2 + yOffset);
                g2d.dispose();
            }
        };
        button.setAlignmentX(Component.CENTER_ALIGNMENT); button.setMaximumSize(new Dimension(400, 55));
        button.setFocusPainted(false); button.setBorderPainted(false); button.setContentAreaFilled(false);
        button.addMouseListener(new MouseAdapter() { public void mouseEntered(MouseEvent evt) { playHoverSound("assets/music/342200__christopherderp__videogame-menu-button-click.wav"); }});

        button.addActionListener(e -> {
            if (isTransitioning) return; // Kunci biar ga spam klik
            if (new File("heart_save_" + slotIndex + ".dat").exists()) {
                startFadeTransition(() -> loadGameData(slotIndex));
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
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                boolean active = getModel().isRollover() || getModel().isPressed();
                int cx = getWidth() / 2;
                int cy = getHeight() / 2;

                if (active) {
                    // --- Item aktif: teks membesar, glow hangat, dikapit panah kiri-kanan ---
                    Font font = GameWindow.VIKING_FONT.deriveFont(Font.BOLD, 30f);
                    g2d.setFont(font);
                    FontMetrics fm = g2d.getFontMetrics();
                    int textWidth = fm.stringWidth(getText());
                    int baselineY = cy + (fm.getAscent() - fm.getDescent()) / 2;

                    // Glow lembut di belakang teks
                    RadialGradientPaint glow = new RadialGradientPaint(
                            new Point(cx, cy),
                            Math.max(textWidth * 0.8f, 40f),
                            new float[]{0f, 1f},
                            new Color[]{new Color(255, 190, 110, 100), new Color(255, 190, 110, 0)}
                    );
                    g2d.setPaint(glow);
                    g2d.fillOval(cx - textWidth, cy - 28, textWidth * 2, 56);

                    // Panah pengapit kiri-kanan
                    g2d.setColor(new Color(255, 205, 140, 230));
                    int arrowGap = textWidth / 2 + 26;
                    drawCaret(g2d, cx - arrowGap, cy, true);
                    drawCaret(g2d, cx + arrowGap, cy, false);

                    // Bayangan halus + teks emas-terang
                    g2d.setColor(new Color(0, 0, 0, 150));
                    g2d.drawString(getText(), cx - textWidth / 2 + 1, baselineY + 1);
                    g2d.setPaint(new GradientPaint(0, cy - 16, new Color(255, 248, 225), 0, cy + 14, new Color(255, 200, 130)));
                    g2d.drawString(getText(), cx - textWidth / 2, baselineY);
                } else {
                    // --- Item tidak aktif: teks polos, kecil, tanpa kotak/background ---
                    Font font = GameWindow.VIKING_FONT.deriveFont(Font.PLAIN, 20f);
                    g2d.setFont(font);
                    FontMetrics fm = g2d.getFontMetrics();
                    int textWidth = fm.stringWidth(getText());
                    int baselineY = cy + (fm.getAscent() - fm.getDescent()) / 2;

                    g2d.setColor(new Color(0, 0, 0, 130));
                    g2d.drawString(getText(), cx - textWidth / 2 + 1, baselineY + 1);
                    g2d.setColor(new Color(225, 220, 210, 220));
                    g2d.drawString(getText(), cx - textWidth / 2, baselineY);
                }

                g2d.dispose();
            }
        };
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setMaximumSize(new Dimension(440, 46));
        button.setPreferredSize(new Dimension(440, 46));
        button.setFocusPainted(false); button.setBorderPainted(false); button.setContentAreaFilled(false);
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) { playHoverSound("assets/music/342200__christopherderp__videogame-menu-button-click.wav"); button.repaint(); }
            public void mouseExited(MouseEvent evt) { button.repaint(); }
        });

        button.addActionListener(e -> {
            if (isTransitioning) return; // Kunci biar ga spam klik

            switch (text) {
                case "CONTINUE":
                    updateLoadBoxUI(); isOverlayOpen = true; menuBox.setVisible(false); loadBox.setVisible(true); repaint();
                    break;
                case "CAMPAIGN":
                    // --- FITUR BARU: Sebelum langsung masuk game, tampilkan dulu pilihan
                    // Difficulty (Easy/Medium/Hard). Campaign baru benar-benar dimulai
                    // setelah salah satu opsi dipilih (lihat createDifficultyBox()).
                    isOverlayOpen = true; menuBox.setVisible(false); difficultyBox.setVisible(true); repaint();
                    break;
                case "OPTIONS":
                    isOverlayOpen = true; menuBox.setVisible(false); optionsBox.setVisible(true); repaint();
                    break;
                case "BACK TO MENU":
                case "APPLY & BACK":
                    if (text.equals("APPLY & BACK")) { applyVideoSettings(); applyAudioSettings(); }
                    isOverlayOpen = false; optionsBox.setVisible(false); loadBox.setVisible(false); difficultyBox.setVisible(false); menuBox.setVisible(true); repaint();
                    break;
                case "QUIT": System.exit(0); break;
            }
        });
        return button;
    }

    // --- FITUR BARU: Helper spawn partikel ambient (debu, bara terbang, kabut) ---
    private Ember spawnAmbientEmber(boolean randomHeight) {
        Ember em = new Ember();
        int w = Math.max(getWidth(), 1080);
        int h = Math.max(getHeight(), 720);
        em.x = ambientRandom.nextFloat() * w;
        em.y = randomHeight ? ambientRandom.nextFloat() * h : h + ambientRandom.nextFloat() * 40;
        em.size = 1.0f + ambientRandom.nextFloat() * 2.0f;
        em.speed = 0.1f + ambientRandom.nextFloat() * 0.3f;
        em.driftPhase = ambientRandom.nextFloat() * (float) (Math.PI * 2);
        em.driftSpeed = 0.1f + ambientRandom.nextFloat() * 0.25f;
        em.baseAlpha = 0.12f + ambientRandom.nextFloat() * 0.3f;
        return em;
    }

    private Spark spawnAmbientSpark(boolean randomHeight) {
        Spark sp = new Spark();
        int w = Math.max(getWidth(), 1080);
        int h = Math.max(getHeight(), 720);
        sp.x = ambientRandom.nextFloat() * w;
        sp.y = randomHeight ? ambientRandom.nextFloat() * h : h + ambientRandom.nextFloat() * 60;
        sp.size = 1.4f + ambientRandom.nextFloat() * 2.0f;
        sp.speed = 0.3f + ambientRandom.nextFloat() * 0.7f;
        sp.driftPhase = ambientRandom.nextFloat() * (float) (Math.PI * 2);
        sp.driftSpeed = 0.15f + ambientRandom.nextFloat() * 0.35f;
        sp.sway = 0.5f + ambientRandom.nextFloat() * 1.3f;
        sp.baseAlpha = 0.3f + ambientRandom.nextFloat() * 0.4f;
        sp.warm = ambientRandom.nextFloat() < 0.82f;
        return sp;
    }

    private Fog spawnAmbientFog(boolean randomX) {
        Fog fog = new Fog();
        int w = Math.max(getWidth(), 1080);
        int h = Math.max(getHeight(), 720);
        fog.w = w * (0.45f + ambientRandom.nextFloat() * 0.35f);
        fog.h = fog.w * (0.16f + ambientRandom.nextFloat() * 0.08f);
        fog.x = randomX ? ambientRandom.nextFloat() * (w + fog.w) - fog.w / 2f : -fog.w / 2f;
        fog.y = h * (0.5f + ambientRandom.nextFloat() * 0.42f);
        fog.speed = 0.06f + ambientRandom.nextFloat() * 0.1f;
        fog.baseAlpha = 0.04f + ambientRandom.nextFloat() * 0.07f;
        return fog;
    }

    // --- FITUR BARU: Render seluruh layer ambient (color grade, horizon glow, vignette, kabut, partikel) ---
    private void drawAmbient(Graphics2D g2d, int width, int height) {
        // Color grade dingin & kelam, senada SplashPanel
        GradientPaint colorGrade = new GradientPaint(
                0, 0, new Color(6, 8, 12, 110),
                0, height, new Color(2, 3, 5, 170)
        );
        g2d.setPaint(colorGrade);
        g2d.fillRect(0, 0, width, height);

        g2d.setColor(new Color(10, 22, 18, 30));
        g2d.fillRect(0, 0, width, height);

        RadialGradientPaint bloodTint = new RadialGradientPaint(
                new Point(width / 2, height / 2),
                Math.max(width, height) * 0.9f,
                new float[]{0f, 1f},
                new Color[]{new Color(50, 0, 0, 0), new Color(20, 4, 4, 45)}
        );
        g2d.setPaint(bloodTint);
        g2d.fillRect(0, 0, width, height);

        float horizonFlicker = 0.75f + 0.25f * (float) Math.sin(tickCounter * 0.05);
        RadialGradientPaint horizonGlow = new RadialGradientPaint(
                new Point(width / 2, height + 40),
                Math.max(width, height) * 0.6f,
                new float[]{0f, 1f},
                new Color[]{new Color(255, 90, 30, (int) (45 * horizonFlicker)), new Color(255, 90, 30, 0)}
        );
        g2d.setPaint(horizonGlow);
        g2d.fillRect(0, 0, width, height);

        RadialGradientPaint vignette = new RadialGradientPaint(
                new Point(width / 2, height / 2),
                Math.max(width, height) * 0.75f,
                new float[]{0f, 1f},
                new Color[]{new Color(0, 0, 0, 0), new Color(0, 0, 0, 130)}
        );
        g2d.setPaint(vignette);
        g2d.fillRect(0, 0, width, height);

        for (Fog fog : ambientFogs) {
            Graphics2D fogG = (Graphics2D) g2d.create();
            fogG.translate(fog.x, fog.y);
            fogG.scale(1.0, fog.h / fog.w);
            RadialGradientPaint fogPaint = new RadialGradientPaint(
                    new Point(0, 0),
                    fog.w / 2f,
                    new float[]{0f, 1f},
                    new Color[]{new Color(70, 75, 85, (int) (255 * fog.baseAlpha)), new Color(70, 75, 85, 0)}
            );
            fogG.setPaint(fogPaint);
            fogG.fillOval((int) (-fog.w / 2f), (int) (-fog.w / 2f), (int) fog.w, (int) fog.w);
            fogG.dispose();
        }

        for (Ember em : ambientEmbers) {
            float flicker = 0.6f + 0.4f * (float) Math.sin((tickCounter * 0.06) + em.driftPhase * 3);
            float a = Math.max(0f, Math.min(1f, em.baseAlpha * flicker));
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a));
            g2d.setColor(new Color(230, 225, 220));
            g2d.fillOval((int) em.x, (int) em.y, (int) em.size, (int) em.size);
        }
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));

        for (Spark sp : ambientSparks) {
            float flicker = 0.55f + 0.45f * (float) Math.sin((tickCounter * 0.08) + sp.driftPhase * 2);
            float a = Math.max(0f, Math.min(1f, sp.baseAlpha * flicker));
            Color core = sp.warm ? new Color(255, 150, 90) : new Color(210, 225, 240);
            Color glow = sp.warm ? new Color(255, 150, 90, 0) : new Color(210, 225, 240, 0);

            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a * 0.5f));
            g2d.setColor(core);
            g2d.setStroke(new BasicStroke(sp.size * 0.6f));
            g2d.drawLine((int) sp.x, (int) sp.y, (int) (sp.x - Math.sin(sp.driftPhase) * sp.sway), (int) (sp.y + sp.size * 5));

            RadialGradientPaint sparkGlow = new RadialGradientPaint(
                    new Point((int) sp.x, (int) sp.y),
                    Math.max(sp.size * 3f, 1f),
                    new float[]{0f, 1f},
                    new Color[]{new Color(core.getRed(), core.getGreen(), core.getBlue(), (int) (140 * a)), glow}
            );
            g2d.setPaint(sparkGlow);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a));
            g2d.fillOval((int) (sp.x - sp.size * 1.5f), (int) (sp.y - sp.size * 1.5f), (int) (sp.size * 3f), (int) (sp.size * 3f));

            g2d.setColor(new Color(255, 245, 235, (int) (220 * a)));
            g2d.fillOval((int) (sp.x - sp.size * 0.3f), (int) (sp.y - sp.size * 0.3f), (int) (sp.size * 0.6f), (int) (sp.size * 0.6f));
        }
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
    }

    // --- FITUR BARU: Ornamen bracket tipis di sudut kiri-atas & kanan-atas, gaya frame referensi ---
    private void drawCornerOrnaments(Graphics2D g2d, int width, int height) {
        Graphics2D o = (Graphics2D) g2d.create();
        o.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        o.setColor(new Color(200, 165, 100, 150));
        o.setStroke(new BasicStroke(1.2f));

        int len = 46;
        int margin = 18;

        // Kiri-atas
        o.drawLine(margin, margin, margin + len, margin);
        o.drawLine(margin, margin, margin, margin + len);
        o.fillOval(margin - 3, margin - 3, 6, 6);

        // Kanan-atas
        o.drawLine(width - margin, margin, width - margin - len, margin);
        o.drawLine(width - margin, margin, width - margin, margin + len);
        o.fillOval(width - margin - 3, margin - 3, 6, 6);

        o.dispose();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 1. Gambar Background Menu
        if (backgroundImage != null) g2d.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), null);
        else { g2d.setPaint(new GradientPaint(0, 0, new Color(35, 25, 20), getWidth(), getHeight(), new Color(10, 8, 5))); g2d.fillRect(0, 0, getWidth(), getHeight()); }

        // --- FITUR BARU: ambient dark tower defense (color grade, kabut, bara, vignette) ---
        if (!isOverlayOpen) {
            drawAmbient(g2d, getWidth(), getHeight());
        }

        // --- gradasi gelap tipis di bawah, biar list menu teks-polos tetap kebaca ---
        // di atas background yang ramai (mirip nuansa referensi: area bawah lebih temaram)
        if (!isOverlayOpen) {
            GradientPaint bottomShade = new GradientPaint(
                    0, getHeight() * 0.55f, new Color(0, 0, 0, 0),
                    0, getHeight(), new Color(0, 0, 0, 150)
            );
            g2d.setPaint(bottomShade);
            g2d.fillRect(0, 0, getWidth(), getHeight());

            drawCornerOrnaments(g2d, getWidth(), getHeight());
        }

        // 2. Render Judul Game jika tidak ada pop-up option/load
        if (!isOverlayOpen) {
            g2d.setFont(GameWindow.VIKING_FONT.deriveFont(Font.BOLD, 68f));
            g2d.setColor(new Color(0, 0, 0, 80)); g2d.drawString("Heart&Horde", 85, 105);
            g2d.setColor(new Color(0, 0, 0, 200)); g2d.drawString("Heart&Horde", 82, 102);
            g2d.setPaint(new GradientPaint(80, 40, new Color(255, 252, 235), 80, 105, new Color(200, 160, 90)));
            g2d.drawString("Heart&Horde", 80, 100);

            g2d.setFont(GameWindow.VIKING_FONT.deriveFont(Font.ITALIC, 28f));
            g2d.setColor(new Color(0, 0, 0, 200)); g2d.drawString("Bloodshed in Cryonia", 107, 142);
            g2d.setPaint(new GradientPaint(105, 120, new Color(220, 220, 220), 105, 145, new Color(150, 145, 140)));
            g2d.drawString("Bloodshed in Cryonia", 105, 140);
        }

        // ==========================================
        // 3. RENDER LAYER HITAM (FADE TO BLACK)
        // ==========================================
        // Harus digambar paling akhir agar menutupi semuanya!
        if (fadeAlpha > 0.0f) {
            float alphaSafe = Math.min(1.0f, Math.max(0.0f, fadeAlpha));
            g2d.setColor(new Color(0, 0, 0, alphaSafe));
            g2d.fillRect(0, 0, getWidth(), getHeight());
        }
    }
}