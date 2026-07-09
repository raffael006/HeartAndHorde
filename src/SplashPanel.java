import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SplashPanel extends JPanel {
    private GameWindow window;

    // --- Variabel Animasi Judul ---
    private float alpha = 0.0f;       // Fade-in judul utama
    private float pulseAlpha = 0.0f;  // Denyut teks "PRESS ENTER TO CONTINUE"
    private boolean pulseUp = true;
    private boolean showPrompt = false; // Baru muncul setelah judul selesai fade-in

    // --- FITUR BARU: Fade-out pas transisi ke Menu (biar mulus, gak "loncat") ---
    private boolean transitioning = false;
    private float fadeOutAlpha = 0f;

    private Timer timer;
    private Random random = new Random();

    // --- FITUR BARU: PARTIKEL AMBIENT (bara api melayang, sesuai tema "Bloodshed in Cryonia") ---
    private static class Ember {
        float x, y, size, speed, driftPhase, driftSpeed, baseAlpha;
    }
    private final List<Ember> embers = new ArrayList<>();
    private static final int EMBER_COUNT = 55;
    private long tickCounter = 0;

    public SplashPanel(GameWindow window) {
        this.window = window;
        setBackground(new Color(15, 10, 10));
        setLayout(null);
        setFocusable(true);

        // --- Siapkan partikel bara api ---
        for (int i = 0; i < EMBER_COUNT; i++) {
            embers.add(spawnEmber(true));
        }

        // --- FITUR BARU: Tekan ENTER (atau tombol/klik apa saja) buat lanjut ke Menu ---
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                tryContinue();
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                tryContinue();
            }
        });

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                requestFocusInWindow(); // Biar keyPressed langsung nyantol tanpa perlu klik dulu
            }
        });

        // Loop Animasi (Dijalankan setiap ~16ms)
        timer = new Timer(16, e -> {
            tickCounter++;

            // 1. Efek Fade-in judul utama
            if (alpha < 1.0f) {
                alpha += 0.012f;
                if (alpha >= 1.0f) {
                    alpha = 1.0f;
                    showPrompt = true; // Judul kelar fade-in -> baru tampilkan prompt
                }
            }

            // 2. Efek Pulsating (Redup-Nyala) untuk teks "PRESS ENTER TO CONTINUE"
            if (showPrompt) {
                if (pulseUp) {
                    pulseAlpha += 0.018f;
                    if (pulseAlpha >= 1.0f) { pulseAlpha = 1.0f; pulseUp = false; }
                } else {
                    pulseAlpha -= 0.018f;
                    if (pulseAlpha <= 0.25f) { pulseAlpha = 0.25f; pulseUp = true; }
                }
            }

            // 3. Update partikel bara api (melayang naik + goyang halus ke kiri-kanan)
            for (int i = 0; i < embers.size(); i++) {
                Ember em = embers.get(i);
                em.y -= em.speed;
                em.x += Math.sin((tickCounter * 0.02) + em.driftPhase) * em.driftSpeed;
                if (em.y < -10) {
                    embers.set(i, spawnEmber(false));
                }
            }

            // 4. Fade-out pas transisi ke Menu
            if (transitioning) {
                fadeOutAlpha += 0.045f;
                if (fadeOutAlpha >= 1.0f) {
                    fadeOutAlpha = 1.0f;
                    timer.stop();
                    window.showScreen("MENU_SCREEN");
                    // Reset biar kalau suatu saat splash ini ditampilkan lagi, mulai fresh
                    transitioning = false;
                    fadeOutAlpha = 0f;
                    alpha = 0f;
                    pulseAlpha = 0f;
                    pulseUp = true;
                    showPrompt = false;
                }
            }

            repaint();
        });
        timer.start();
    }

    private void tryContinue() {
        if (!showPrompt || transitioning) return; // Belum boleh lanjut / udah lagi transisi
        transitioning = true;
    }

    private Ember spawnEmber(boolean randomHeight) {
        Ember em = new Ember();
        int w = Math.max(getWidth(), 1080);
        int h = Math.max(getHeight(), 720);
        em.x = random.nextFloat() * w;
        em.y = randomHeight ? random.nextFloat() * h : h + random.nextFloat() * 40;
        em.size = 1.2f + random.nextFloat() * 2.6f;
        em.speed = 0.25f + random.nextFloat() * 0.7f;
        em.driftPhase = random.nextFloat() * (float) (Math.PI * 2);
        em.driftSpeed = 0.15f + random.nextFloat() * 0.35f;
        em.baseAlpha = 0.25f + random.nextFloat() * 0.55f;
        return em;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        // ==========================================
        // 1. BACKGROUND GRADIENT (Ruang Bawah Tanah / Kastil Kelam)
        // ==========================================
        GradientPaint gp = new GradientPaint(0, 0, new Color(40, 15, 15), 0, height, new Color(5, 5, 5));
        g2d.setPaint(gp);
        g2d.fillRect(0, 0, width, height);

        // --- FITUR BARU: Vignette halus di pinggir biar lebih sinematik ---
        RadialGradientPaint vignette = new RadialGradientPaint(
                new Point(width / 2, height / 2),
                Math.max(width, height) * 0.75f,
                new float[]{0f, 1f},
                new Color[]{new Color(0, 0, 0, 0), new Color(0, 0, 0, 160)}
        );
        g2d.setPaint(vignette);
        g2d.fillRect(0, 0, width, height);

        // ==========================================
        // 2. PARTIKEL BARA API (di belakang judul, biar berlapis/depth)
        // ==========================================
        for (Ember em : embers) {
            float flicker = 0.5f + 0.5f * (float) Math.sin((tickCounter * 0.05) + em.driftPhase * 3);
            float a = Math.max(0f, Math.min(1f, em.baseAlpha * flicker));
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a));
            g2d.setColor(new Color(255, 140, 60));
            g2d.fillOval((int) em.x, (int) em.y, (int) em.size, (int) em.size);
            // Inti bara sedikit lebih terang di tengah
            g2d.setColor(new Color(255, 210, 140));
            g2d.fillOval((int) (em.x + em.size * 0.25f), (int) (em.y + em.size * 0.25f), (int) (em.size * 0.5f), (int) (em.size * 0.5f));
        }
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));

        // ==========================================
        // 3. GAMBAR JUDUL (Fade-In dengan Glow, warna Emas Kuno)
        // ==========================================
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

        Font titleFont = new Font("Georgia", Font.BOLD, 76);
        g2d.setFont(titleFont);
        FontMetrics titleFm = g2d.getFontMetrics();
        String titleText = "HEART & HORDE";
        int titleWidth = titleFm.stringWidth(titleText);
        int titleX = (width - titleWidth) / 2;
        int titleY = height / 2 - 60;

        // --- FITUR BARU: Glow di belakang judul (radial gradient hangat) ---
        RadialGradientPaint titleGlow = new RadialGradientPaint(
                new Point(width / 2, titleY - 15),
                titleWidth * 0.65f,
                new float[]{0f, 1f},
                new Color[]{new Color(255, 200, 110, 130), new Color(255, 200, 110, 0)}
        );
        g2d.setPaint(titleGlow);
        g2d.fillOval(width / 2 - titleWidth, titleY - 120, titleWidth * 2, 220);

        // Bayangan Teks (Drop Shadow)
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.drawString(titleText, titleX + 5, titleY + 5);

        // Teks Utama (Warna Emas/Kuning Kuno)
        g2d.setPaint(new GradientPaint(titleX, titleY - 45, new Color(255, 250, 225), titleX, titleY + 15, new Color(200, 150, 70)));
        g2d.drawString(titleText, titleX, titleY);

        // Garis dekoratif tipis di bawah judul (kesan "ukiran")
        g2d.setColor(new Color(200, 160, 90, 200));
        g2d.setStroke(new BasicStroke(1.2f));
        g2d.drawLine(titleX + 4, titleY + 14, titleX + titleWidth - 4, titleY + 14);

        // Sub-judul (Merah Pudar)
        g2d.setFont(new Font("Serif", Font.ITALIC, 26));
        g2d.setColor(new Color(0, 0, 0, 200));
        String subText = "— Bloodshed in Cryonia —";
        int subWidth = g2d.getFontMetrics().stringWidth(subText);
        g2d.drawString(subText, (width - subWidth) / 2 + 2, titleY + 52);
        g2d.setPaint(new GradientPaint(0, titleY + 30, new Color(220, 100, 100), 0, titleY + 55, new Color(150, 60, 60)));
        g2d.drawString(subText, (width - subWidth) / 2, titleY + 50);

        // ==========================================
        // 4. "PRESS ENTER TO CONTINUE" (menggantikan progress bar loading lama)
        // ==========================================
        if (showPrompt || transitioning) {
            float promptAlpha = transitioning ? Math.max(0f, 1f - fadeOutAlpha) : 1f;
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, pulseAlpha * promptAlpha));

            String promptText = "PRESS ENTER TO CONTINUE";
            g2d.setFont(new Font("Serif", Font.BOLD, 20));
            FontMetrics pfm = g2d.getFontMetrics();
            int promptWidth = pfm.stringWidth(promptText);
            int promptX = (width - promptWidth) / 2;
            int promptY = height - 150;

            // Garis flourish tipis di kiri-kanan teks (mirip aksen di referensi FF7)
            g2d.setColor(new Color(230, 200, 150));
            g2d.setStroke(new BasicStroke(1.0f));
            int lineGap = 18;
            g2d.drawLine(promptX - 60, promptY - 6, promptX - lineGap, promptY - 6);
            g2d.drawLine(promptX + promptWidth + lineGap, promptY - 6, promptX + promptWidth + 60, promptY - 6);

            g2d.setColor(new Color(0, 0, 0, 200));
            g2d.drawString(promptText, promptX + 2, promptY + 2);
            g2d.setColor(new Color(235, 215, 180));
            g2d.drawString(promptText, promptX, promptY);
        }

        // Reset composite buat credit text
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, transitioning ? Math.max(0f, 1f - fadeOutAlpha) : 1.0f));

        // ==========================================
        // 5. CREDIT KECIL DI BAWAH (sesuaikan sendiri isinya kalau perlu)
        // ==========================================
        g2d.setFont(new Font("Serif", Font.PLAIN, 13));
        g2d.setColor(new Color(160, 150, 140));
        String[] creditLines = {
                "HEART & HORDE ~ BLOODSHED IN CRYONIA",
                "A DARK FANTASY TOWER DEFENSE"
        };
        int creditY = height - 55;
        for (String line : creditLines) {
            int lw = g2d.getFontMetrics().stringWidth(line);
            g2d.drawString(line, (width - lw) / 2, creditY);
            creditY += 18;
        }

        // ==========================================
        // 6. LAYER HITAM (FADE OUT) — digambar paling akhir, menutupi semuanya
        // ==========================================
        if (transitioning) {
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
            g2d.setColor(new Color(0, 0, 0, (int) (255 * Math.min(1f, fadeOutAlpha))));
            g2d.fillRect(0, 0, width, height);
        }

        g2d.dispose();
    }
}