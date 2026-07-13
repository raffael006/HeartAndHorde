import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.function.IntSupplier;

/**
 * HUD kanan atas: bar resource (Day, Wave, Wood, Stone, Steel, Food, Civil, Guard)
 * + tombol menu titik-3. Diekstrak dari GamePanel.setupHUD() apa adanya (logic &
 * tampilan tidak diubah) -- panel ini murni MEMBACA state dari ResourceManager,
 * WaveManager, GameWindow, dan currentDay (lewat IntSupplier karena nilainya
 * berubah tiap tick, bukan snapshot sekali pas dibuat).
 */
public class HudPanel extends JPanel {

    private final GameWindow window;
    private final ResourceManager resourceManager;
    private final WaveManager waveManager;
    private final IntSupplier dayProvider;

    private final BufferedImage iconWave, iconWood, iconStone, iconSteel, iconFood, iconCivil, iconMilitary;

    private final String[] resourceTooltips = {
            "Hari Bertahan Hidup",
            "Wave Serangan Horde",
            "Total Wood (Kayu)",
            "Total Stone (Batu)",
            "Total Steel (Baja)",
            "Total Food (Makanan)",
            "Total Civil (Penduduk)",
            "Total Pasukan Guard"
    };
    private final Rectangle[] resourceHitboxes = new Rectangle[8];

    /** Tombol menu titik-3 (⋮) -- diekspos public karena posisinya di-set ulang dari GamePanel saat resize. */
    public final JButton menuBtn;

    public HudPanel(GameWindow window, ResourceManager resourceManager, WaveManager waveManager,
                    IntSupplier dayProvider,
                    BufferedImage iconWave, BufferedImage iconWood, BufferedImage iconStone,
                    BufferedImage iconSteel, BufferedImage iconFood, BufferedImage iconCivil,
                    BufferedImage iconMilitary, Runnable onMenuClick) {
        super(null);
        this.window = window;
        this.resourceManager = resourceManager;
        this.waveManager = waveManager;
        this.dayProvider = dayProvider;
        this.iconWave = iconWave;
        this.iconWood = iconWood;
        this.iconStone = iconStone;
        this.iconSteel = iconSteel;
        this.iconFood = iconFood;
        this.iconCivil = iconCivil;
        this.iconMilitary = iconMilitary;

        setLayout(null);
        setOpaque(false);
        ToolTipManager.sharedInstance().setInitialDelay(100);
        setToolTipText("");

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
        menuBtn.addActionListener(e -> onMenuClick.run());
        add(menuBtn);
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        for (int i = 0; i < resourceHitboxes.length; i++) {
            if (resourceHitboxes[i] != null && resourceHitboxes[i].contains(e.getPoint())) return resourceTooltips[i];
        }
        return null;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // Background gelap kotak (bukan parchment/krem lagi)
        g2d.setColor(new Color(18, 14, 12, 245));
        g2d.fillRect(0, 0, w, h);

        // Border emas tipis, 1 garis saja
        g2d.setColor(new Color(100, 75, 35));
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.drawRect(0, 0, w - 1, h - 1);

        int currentDay = dayProvider.getAsInt();

        // --- DATA RESOURCE ---
        // --- FITUR BARU: angka Wave sekarang ngikutin wave yang sedang berjalan/dihitung mundur,
        // dan mentok di MAX_WAVE (V) begitu semua wave sudah tuntas di-spawn ---
        int wave = Math.min(waveManager.getCurrentWaveIndex() + 1, WaveManager.MAX_WAVE);
        int totalCivil = window.activeCivils.size();
        int totalGuard = window.activeGuards.size();

        // index 0 = Day (lingkaran, bukan gambar), 1 = Wave (gambar, tanpa bar), sisanya (gambar + bar)
        BufferedImage[] icons = {null, iconWave, iconWood, iconStone, iconSteel, iconFood, iconCivil, iconMilitary};
        int[] values = {currentDay, wave, resourceManager.wood, resourceManager.stone, resourceManager.steel, resourceManager.food, totalCivil, totalGuard};
        int[] maxValues = {-1, -1, resourceManager.maxWood, resourceManager.maxStone, resourceManager.maxSteel, resourceManager.maxFood, -1, -1}; // -1 = gak ada bar (Day, Wave, Civil & Guard)

        // --- UKURAN SERAGAM TIAP SLOT ---
        int iconSize = 46;
        int slotWidth = 78;
        int slotGap = 20;
        int barWidth = 40;
        int barHeight = 12;

        int startX = 12;
        for (int i = 0; i < icons.length; i++) {
            int slotX = startX;
            boolean hasBar = maxValues[i] >= 0;

            // SEMUA icon sekarang di-tengah-in VERTIKAL penuh, seragam, gak ada bedanya
            // antara yang punya bar atau enggak.
            int iconY = (h - iconSize) / 2;

            if (i == 0) {
                // --- DAY: lingkaran + "1D", bukan gambar ---
                g2d.setColor(new Color(45, 35, 25));
                g2d.fillOval(slotX, iconY, iconSize, iconSize);
                g2d.setColor(new Color(100, 75, 35));
                g2d.setStroke(new BasicStroke(1.5f));
                g2d.drawOval(slotX, iconY, iconSize, iconSize);

                g2d.setFont(new Font("Segoe UI", Font.BOLD, 13));
                g2d.setColor(new Color(225, 205, 165));
                String dayText = currentDay + "D";
                FontMetrics fmDay = g2d.getFontMetrics();
                int dayTextX = slotX + (iconSize - fmDay.stringWidth(dayText)) / 2;
                int dayTextY = iconY + (iconSize + fmDay.getAscent() - fmDay.getDescent()) / 2;
                g2d.drawString(dayText, dayTextX, dayTextY);

                resourceHitboxes[i] = new Rectangle(slotX - 4, 0, iconSize + 8, h);
                startX += iconSize + 8 + slotGap;
                continue;
            }

            if (icons[i] != null) {
                g2d.drawImage(icons[i], slotX, iconY, iconSize, iconSize, null);
            }

            int textX = slotX + iconSize + 6;
            g2d.setFont(new Font("Segoe UI", Font.BOLD, 13));
            g2d.setColor(new Color(225, 205, 165));
            String valueText = hasBar ? values[i] + "/" + maxValues[i] : String.valueOf(values[i]);
            FontMetrics fmVal = g2d.getFontMetrics();

            if (hasBar) {
                // Blok "angka + bar" di-tengah-in vertikal sebagai 1 kesatuan,
                // jadi titik tengahnya SAMA PERSIS sama titik tengah icon (h/2).
                int gapBetween = 3;
                int textHeight = fmVal.getAscent(); // taksiran tinggi baris teks
                int contentBlockHeight = textHeight + gapBetween + barHeight;
                int blockTop = (h - contentBlockHeight) / 2;

                int textBaselineY = blockTop + textHeight;
                g2d.drawString(valueText, textX, textBaselineY);

                int barX = textX;
                int barY = blockTop + textHeight + gapBetween;

                g2d.setColor(new Color(0, 0, 0, 150));
                g2d.fillRect(barX, barY, barWidth, barHeight);

                float ratio = maxValues[i] == 0 ? 0f : Math.min(1f, (float) values[i] / maxValues[i]);
                // --- FITUR BARU: Kalau udah penuh (mentok max), warna fill bar jadi merah gelap ---
                boolean isFull = maxValues[i] > 0 && values[i] >= maxValues[i];
                g2d.setColor(isFull ? new Color(150, 30, 30) : new Color(80, 200, 120));
                g2d.fillRect(barX, barY, (int) (barWidth * ratio), barHeight);

                g2d.setColor(new Color(100, 75, 35));
                g2d.drawRect(barX, barY, barWidth, barHeight);
            } else {
                // Wave: angka di samping icon, di-tengah-in vertikal (sejajar sama Day)
                int textY = iconY + (iconSize + fmVal.getAscent() - fmVal.getDescent()) / 2;
                g2d.drawString(valueText, textX, textY);
            }

            resourceHitboxes[i] = new Rectangle(slotX - 4, 0, slotWidth, h);
            startX += slotWidth + slotGap;
        }

        g2d.dispose();
    }
}