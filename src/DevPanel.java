import javax.swing.*;
import java.awt.*;
import java.util.function.BiFunction;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Panel developer/cheat tools (Resources/Guards/Hordes/World + live stats).
 * Diekstrak dari GamePanel.buildDevPanel() apa adanya (logic & tampilan tidak diubah).
 *
 * PENTING: spawnPos & tombol "Center Camera" tadinya pakai getWidth()/getHeight() milik
 * GamePanel (viewport penuh) untuk menghitung posisi dunia dari titik tengah layar --
 * BUKAN ukuran devPanel sendiri (540x460). Supaya perilakunya tetap identik setelah
 * dipisah jadi class sendiri, ukuran viewport itu diteruskan lewat hostWidth/hostHeight
 * (IntSupplier yang menunjuk ke GamePanel::getWidth / GamePanel::getHeight), bukan
 * this::getWidth milik DevPanel.
 */
public class DevPanel extends JPanel {

    private final ResourceManager resourceManager;
    private final GameWindow window;
    private final Camera camera;
    private final IntSupplier hostWidth;
    private final IntSupplier hostHeight;

    public DevPanel(ResourceManager resourceManager, GameWindow window, Camera camera,
                    IntSupplier hostWidth, IntSupplier hostHeight, Runnable onClose) {
        super(null);
        this.resourceManager = resourceManager;
        this.window = window;
        this.camera = camera;
        this.hostWidth = hostWidth;
        this.hostHeight = hostHeight;

        setOpaque(false);
        buildContent(onClose);
        setVisible(false);
    }

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
        g2d.drawString("WORLD", 18, 405);

        // Garis tipis tiap section
        g2d.setColor(new Color(60, 48, 30));
        g2d.drawLine(90, 62, w - 16, 62);
        g2d.drawLine(72, 162, w - 16, 162);
        g2d.drawLine(70, 262, w - 16, 262);
        g2d.drawLine(62, 362, w - 16, 399);

        // --- Live stats ---
        g2d.setFont(new Font("Serif", Font.PLAIN, 11));
        g2d.setColor(new Color(160, 145, 115));
        g2d.drawString("Wood: " + resourceManager.wood
                + "   Civil: " + window.activeCivils.size()
                + "   Guard: " + window.activeGuards.size()
                + "   Horde: " + window.activeHordes.size(), 18, h - 14);

        g2d.dispose();
    }

    private void buildContent(Runnable onClose) {
        JPanel panel = this;

        // Helper: buat tombol dev bergaya game
        BiFunction<String, Runnable, JButton> mkBtn = (label, action) -> {
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

        // Helper: posisi spawn (tengah layar visible) -- pakai ukuran viewport GamePanel (hostWidth/hostHeight),
        // BUKAN ukuran devPanel sendiri.
        Supplier<double[]> spawnPos = () -> {
            double wx = (hostWidth.getAsInt() / 2.0 - camera.getX()) / camera.getZoom();
            double wy = (hostHeight.getAsInt() / 2.0 - camera.getY()) / camera.getZoom();
            return new double[]{wx, wy};
        };

        int bw = 114, bh = 30, col1 = 18, col2 = 140, col3 = 262, col4 = 390;

        // ===== SECTION: RESOURCES =====
        JButton addWood100  = mkBtn.apply("+100 Wood",  () -> resourceManager.wood += 100);
        JButton addWood500  = mkBtn.apply("+500 Wood",  () -> resourceManager.wood += 500);
        JButton addWood2000 = mkBtn.apply("+2000 Wood", () -> resourceManager.wood += 2000);
        JButton setWood0    = mkBtn.apply("Reset Wood", () -> resourceManager.wood = 0);
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

        JButton spawnBear1     = mkBtn.apply("+1 Bear",     () -> { double[] p = spawnPos.get(); window.activeHordes.add(new Horde(Horde.HordeType.BEAR, p[0], p[1])); });
        JButton spawnTwoAxe1   = mkBtn.apply("+1 2Axe",     () -> { double[] p = spawnPos.get(); window.activeHordes.add(new Horde(Horde.HordeType.TWO_AXE, p[0], p[1])); });
        JButton spawnLog1      = mkBtn.apply("+1 Log",      () -> { double[] p = spawnPos.get(); window.activeHordes.add(new Horde(Horde.HordeType.LOG, p[0], p[1])); });
        JButton spawnSorcerer1 = mkBtn.apply("+1 Sorcerer", () -> { double[] p = spawnPos.get(); window.activeHordes.add(new Horde(Horde.HordeType.SORCERER, p[0], p[1])); });
        spawnBear1    .setBounds(col1, 313, bw, bh);
        spawnTwoAxe1  .setBounds(col2, 313, bw, bh);
        spawnLog1     .setBounds(col3, 313, bw, bh);
        spawnSorcerer1.setBounds(col4, 313, bw, bh);

        JButton killAllHorde = mkBtn.apply("Kill All Hordes", () -> window.activeHordes.clear());
        JButton spawnHorde20 = mkBtn.apply("+20 Axemen",     () -> { double[] p = spawnPos.get(); for (int i = 0; i < 20; i++) window.activeHordes.add(new Horde(Horde.HordeType.AXEMAN, p[0] + (i % 5) * 30, p[1] + (i / 5) * 30)); });
        killAllHorde.setBounds(col1, 350, bw, bh);
        spawnHorde20.setBounds(col2, 350, bw, bh);

        // ===== SECTION: WORLD =====
        JButton clearBuildings = mkBtn.apply("Clear Buildings", () -> { window.savedBuildings.clear(); });
        JButton clearProj      = mkBtn.apply("Clear Projectiles", () -> window.activeProjectiles.clear());
        JButton clearAll       = mkBtn.apply("Clear Everything", () -> { window.activeGuards.clear(); window.activeHordes.clear(); window.activeProjectiles.clear(); });
        JButton centerCam      = mkBtn.apply("Center Camera", () -> { camera.centerOn(1500, 1500, hostWidth.getAsInt(), hostHeight.getAsInt()); repaint(); });
        clearBuildings.setBounds(col1, 413, bw, bh);
        clearProj     .setBounds(col2, 413, bw, bh);
        clearAll      .setBounds(col3, 413, bw, bh);
        centerCam     .setBounds(col4, 413, bw, bh);

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
        closeBtn.addActionListener(e -> { onClose.run(); panel.setVisible(false); });

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
    }
}