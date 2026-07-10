import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
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

    // --- Fade-out pas transisi ke Menu (biar mulus, gak "loncat") ---
    private boolean transitioning = false;
    private float fadeOutAlpha = 0f;

    // --- Fade-in dari hitam pas splash pertama kali tampil ---
    private float introAlpha = 1f;

    private Timer timer;
    private Random random = new Random();

    // --- PARTIKEL AMBIENT: jadi debu/abu halus yang tenang (gaya "quiet melancholy" ala NieR) ---
    private static class Ember {
        float x, y, size, speed, driftPhase, driftSpeed, baseAlpha;
    }
    private final List<Ember> embers = new ArrayList<>();
    private static final int EMBER_COUNT = 40;
    private long tickCounter = 0;

    // --- FITUR BARU: PARTIKEL FOREGROUND — bara api yang jelas terbang naik, dikasih glow & jejak ---
    // (skala kecil & warna hangat-pudar biar tetap nyatu sama mood tenang, gak norak)
    private static class Spark {
        float x, y, size, speed, driftPhase, driftSpeed, baseAlpha, sway;
        boolean warm; // sebagian hangat (ember api), sebagian dingin-keputihan (kesan "Cryonia")
    }
    private final List<Spark> sparks = new ArrayList<>();
    private static final int SPARK_COUNT = 26;

    // --- FITUR BARU: KABUT/FOG AMBIENT — melayang pelan, kesan medan perang berkabut ala tower defense ---
    private static class Fog {
        float x, y, w, h, speed, baseAlpha;
    }
    private final List<Fog> fogs = new ArrayList<>();
    private static final int FOG_COUNT = 6;

    // --- BACKGROUND ART + KEN BURNS (zoom & pan pelan) ---
    private BufferedImage backgroundImage;
    private static final String[] BACKGROUND_CANDIDATES = {
            "/background.png", "/background.jpg",
            "/assets/background.png", "/assets/background.jpg"
    };

    public SplashPanel(GameWindow window) {
        this.window = window;
        setBackground(new Color(10, 12, 16));
        setLayout(null);
        setFocusable(true);

        loadBackgroundImage();

        for (int i = 0; i < EMBER_COUNT; i++) {
            embers.add(spawnEmber(true));
        }
        for (int i = 0; i < SPARK_COUNT; i++) {
            sparks.add(spawnSpark(true));
        }
        for (int i = 0; i < FOG_COUNT; i++) {
            fogs.add(spawnFog(true));
        }

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
                requestFocusInWindow();
            }
        });

        timer = new Timer(16, e -> {
            tickCounter++;

            if (introAlpha > 0f) {
                introAlpha -= 0.02f;
                if (introAlpha < 0f) introAlpha = 0f;
            }

            if (alpha < 1.0f) {
                alpha += 0.01f; // sedikit lebih pelan, kesan tenang bukan meledak-ledak
                if (alpha >= 1.0f) {
                    alpha = 1.0f;
                    showPrompt = true;
                }
            }

            if (showPrompt) {
                if (pulseUp) {
                    pulseAlpha += 0.014f;
                    if (pulseAlpha >= 1.0f) { pulseAlpha = 1.0f; pulseUp = false; }
                } else {
                    pulseAlpha -= 0.014f;
                    if (pulseAlpha <= 0.3f) { pulseAlpha = 0.3f; pulseUp = true; }
                }
            }

            for (int i = 0; i < embers.size(); i++) {
                Ember em = embers.get(i);
                em.y -= em.speed;
                em.x += Math.sin((tickCounter * 0.015) + em.driftPhase) * em.driftSpeed;
                if (em.y < -10) {
                    embers.set(i, spawnEmber(false));
                }
            }

            for (int i = 0; i < sparks.size(); i++) {
                Spark sp = sparks.get(i);
                sp.y -= sp.speed;
                sp.x += Math.sin((tickCounter * 0.025) + sp.driftPhase) * sp.driftSpeed;
                if (sp.y < -20) {
                    sparks.set(i, spawnSpark(false));
                }
            }

            int fw = Math.max(getWidth(), 1080);
            for (Fog fog : fogs) {
                fog.x += fog.speed;
                if (fog.x - fog.w / 2f > fw) {
                    fog.x = -fog.w / 2f;
                }
            }

            if (transitioning) {
                fadeOutAlpha += 0.04f;
                if (fadeOutAlpha >= 1.0f) {
                    fadeOutAlpha = 1.0f;
                    timer.stop();
                    window.showScreen("MENU_SCREEN");
                    transitioning = false;
                    fadeOutAlpha = 0f;
                    alpha = 0f;
                    pulseAlpha = 0f;
                    pulseUp = true;
                    showPrompt = false;
                    introAlpha = 1f;
                }
            }

            repaint();
        });
        timer.start();
    }

    private void loadBackgroundImage() {
        for (String path : BACKGROUND_CANDIDATES) {
            try (InputStream is = getClass().getResourceAsStream(path)) {
                if (is != null) {
                    backgroundImage = ImageIO.read(is);
                    if (backgroundImage != null) return;
                }
            } catch (IOException ignored) {
            }
        }
    }

    private void tryContinue() {
        if (!showPrompt || transitioning) return;
        transitioning = true;
    }

    private Ember spawnEmber(boolean randomHeight) {
        Ember em = new Ember();
        int w = Math.max(getWidth(), 1080);
        int h = Math.max(getHeight(), 720);
        em.x = random.nextFloat() * w;
        em.y = randomHeight ? random.nextFloat() * h : h + random.nextFloat() * 40;
        em.size = 1.0f + random.nextFloat() * 2.0f;
        em.speed = 0.12f + random.nextFloat() * 0.35f; // lebih pelan, kesan debu melayang bukan bara meletup
        em.driftPhase = random.nextFloat() * (float) (Math.PI * 2);
        em.driftSpeed = 0.1f + random.nextFloat() * 0.25f;
        em.baseAlpha = 0.15f + random.nextFloat() * 0.35f;
        return em;
    }

    private Spark spawnSpark(boolean randomHeight) {
        Spark sp = new Spark();
        int w = Math.max(getWidth(), 1080);
        int h = Math.max(getHeight(), 720);
        sp.x = random.nextFloat() * w;
        sp.y = randomHeight ? random.nextFloat() * h : h + random.nextFloat() * 60;
        sp.size = 1.6f + random.nextFloat() * 2.4f;
        sp.speed = 0.35f + random.nextFloat() * 0.9f; // lebih cepat & jelas ketimbang debu di belakang
        sp.driftPhase = random.nextFloat() * (float) (Math.PI * 2);
        sp.driftSpeed = 0.2f + random.nextFloat() * 0.4f;
        sp.sway = 0.5f + random.nextFloat() * 1.5f;
        sp.baseAlpha = 0.35f + random.nextFloat() * 0.45f;
        sp.warm = random.nextFloat() < 0.82f; // dominan hangat (bara pertempuran), sedikit dingin-pucat buat aksen "Cryonia"
        return sp;
    }

    private Fog spawnFog(boolean randomX) {
        Fog fog = new Fog();
        int w = Math.max(getWidth(), 1080);
        int h = Math.max(getHeight(), 720);
        fog.w = w * (0.5f + random.nextFloat() * 0.4f);
        fog.h = fog.w * (0.16f + random.nextFloat() * 0.08f);
        fog.x = randomX ? random.nextFloat() * (w + fog.w) - fog.w / 2f : -fog.w / 2f;
        fog.y = h * (0.42f + random.nextFloat() * 0.5f); // menggantung di area tengah-bawah, kayak kabut medan perang
        fog.speed = 0.08f + random.nextFloat() * 0.14f; // pelan banget, cuma nuansa ambient
        fog.baseAlpha = 0.05f + random.nextFloat() * 0.09f;
        return fog;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        int width = getWidth();
        int height = getHeight();

        // ==========================================
        // 1. BACKGROUND: gambar art (Ken Burns halus) atau gradient fallback
        // ==========================================
        if (backgroundImage != null) {
            drawKenBurnsBackground(g2d, width, height);
        } else {
            GradientPaint gp = new GradientPaint(0, 0, new Color(18, 20, 28), 0, height, new Color(4, 5, 8));
            g2d.setPaint(gp);
            g2d.fillRect(0, 0, width, height);
        }

        // --- Color grading: lebih gelap & berat, kesan medan perang senyap yang terkepung (dark tower defense) ---
        GradientPaint colorGrade = new GradientPaint(
                0, 0, new Color(6, 8, 12, 185),
                0, height, new Color(2, 3, 5, 225)
        );
        g2d.setPaint(colorGrade);
        g2d.fillRect(0, 0, width, height);

        // Sentuhan dingin-kehijauan tipis di seluruh layar — nuansa "Cryonia" yang beku & muram
        g2d.setColor(new Color(10, 22, 18, 40));
        g2d.fillRect(0, 0, width, height);

        // Sentuhan merah pudar di tengah, jejak "bloodshed" tanpa norak
        RadialGradientPaint bloodTint = new RadialGradientPaint(
                new Point(width / 2, height / 2),
                Math.max(width, height) * 0.9f,
                new float[]{0f, 1f},
                new Color[]{new Color(50, 0, 0, 0), new Color(20, 4, 4, 65)}
        );
        g2d.setPaint(bloodTint);
        g2d.fillRect(0, 0, width, height);

        // --- FITUR BARU: cahaya api jauh di garis horizon — kesan api pembakaran/perkemahan horde yang mengepung ---
        float horizonFlicker = 0.75f + 0.25f * (float) Math.sin(tickCounter * 0.03);
        RadialGradientPaint horizonGlow = new RadialGradientPaint(
                new Point(width / 2, height + 40),
                Math.max(width, height) * 0.65f,
                new float[]{0f, 1f},
                new Color[]{new Color(255, 90, 30, (int) (70 * horizonFlicker)), new Color(255, 90, 30, 0)}
        );
        g2d.setPaint(horizonGlow);
        g2d.fillRect(0, 0, width, height);

        // Vignette lebih berat & pekat di pinggir — kesan terkepung/tertekan
        RadialGradientPaint vignette = new RadialGradientPaint(
                new Point(width / 2, height / 2),
                Math.max(width, height) * 0.72f,
                new float[]{0f, 1f},
                new Color[]{new Color(0, 0, 0, 0), new Color(0, 0, 0, 190)}
        );
        g2d.setPaint(vignette);
        g2d.fillRect(0, 0, width, height);

        // ==========================================
        // 1.5 KABUT AMBIENT — melayang pelan-pelan menyamping, kesan medan perang berkabut
        // ==========================================
        for (Fog fog : fogs) {
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

        // ==========================================
        // 2. PARTIKEL DEBU/ABU (halus, di belakang judul)
        // ==========================================
        for (Ember em : embers) {
            float flicker = 0.6f + 0.4f * (float) Math.sin((tickCounter * 0.04) + em.driftPhase * 3);
            float a = Math.max(0f, Math.min(1f, em.baseAlpha * flicker));
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a));
            g2d.setColor(new Color(230, 225, 220));
            g2d.fillOval((int) em.x, (int) em.y, (int) em.size, (int) em.size);
        }
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));

        int centerX = width / 2;
        int dividerY = height / 2 - 12; // garis horizontal utama, kayak di title NieR

        // ==========================================
        // 3. GARIS DIVIDER FULL-WIDTH (ciri khas layout NieR: satu garis tipis, potong layar)
        // ==========================================
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2d.setColor(new Color(225, 220, 210, 200));
        g2d.setStroke(new BasicStroke(1.2f));
        g2d.drawLine(0, dividerY, width, dividerY);
        // ornamen kecil di tengah garis
        g2d.setColor(new Color(225, 220, 210, 230));
        g2d.drawLine(centerX, dividerY - 5, centerX, dividerY + 5);

        // ==========================================
        // 4. JUDUL — tipografi tipis & letter-spaced, straddling garis divider
        // ==========================================
        Font titleFont = new Font("Serif", Font.PLAIN, 58);
        g2d.setFont(titleFont);
        float tracking = 10f; // jarak antar huruf, kesan elegan/art-deco

        String left = "HEART";
        String right = "HORDE";
        FontMetrics tfm = g2d.getFontMetrics();
        float leftW = trackedWidth(tfm, left, tracking);
        float rightW = trackedWidth(tfm, right, tracking);

        // Simbol "&" — italic, sedikit lebih besar dari huruf kapital di kanan-kirinya biar berkesan flourish/dekoratif
        Font ampFont = new Font("Serif", Font.ITALIC, 66);
        FontMetrics afm = g2d.getFontMetrics(ampFont);
        float ampWidth = afm.stringWidth("&");

        int glyphGap = 22;
        float totalW = leftW + glyphGap + ampWidth + glyphGap + rightW;

        float titleBaselineY = dividerY + tfm.getAscent() * 0.32f; // teks nempel & sedikit menembus garis
        float cursorX = centerX - totalW / 2f;

        // Bayangan lembut
        g2d.setColor(new Color(0, 0, 0, 150));
        drawTrackedString(g2d, left, cursorX + 2, titleBaselineY + 2, tracking);
        g2d.setColor(new Color(240, 238, 232));
        cursorX = drawTrackedString(g2d, left, cursorX, titleBaselineY, tracking);

        // Simbol "&" di antara dua kata — gradient emas-putih senada judul
        cursorX += glyphGap;
        float ampBaselineY = titleBaselineY + (afm.getAscent() - tfm.getAscent()) * 0.5f; // biar center-nya sejajar walau fontnya beda ukuran
        g2d.setFont(ampFont);
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.drawString("&", cursorX + 2, ampBaselineY + 2);
        g2d.setPaint(new GradientPaint(cursorX, ampBaselineY - afm.getAscent(), new Color(255, 250, 225), cursorX, ampBaselineY + afm.getDescent(), new Color(200, 150, 70)));
        g2d.drawString("&", cursorX, ampBaselineY);
        g2d.setFont(titleFont);
        cursorX += ampWidth + glyphGap;

        g2d.setColor(new Color(0, 0, 0, 150));
        drawTrackedString(g2d, right, cursorX + 2, titleBaselineY + 2, tracking);
        g2d.setColor(new Color(240, 238, 232));
        drawTrackedString(g2d, right, cursorX, titleBaselineY, tracking);

        // Sub-judul kecil, tenang, di bawah garis
        g2d.setFont(new Font("Serif", Font.ITALIC, 16));
        FontMetrics sfm = g2d.getFontMetrics();
        String subText = "B L O O D S H E D   I N   C R Y O N I A";
        int subWidth = sfm.stringWidth(subText);
        g2d.setColor(new Color(0, 0, 0, 140));
        g2d.drawString(subText, centerX - subWidth / 2 + 1, dividerY + 34 + 1);
        g2d.setColor(new Color(200, 195, 190, 220));
        g2d.drawString(subText, centerX - subWidth / 2, dividerY + 34);

        // ==========================================
        // 4.5 PARTIKEL FOREGROUND — bara/sisik terbang naik, lebih jelas & ada glow+jejak
        // ==========================================
        for (Spark sp : sparks) {
            float flicker = 0.55f + 0.45f * (float) Math.sin((tickCounter * 0.06) + sp.driftPhase * 2);
            float a = Math.max(0f, Math.min(1f, sp.baseAlpha * flicker)) * alpha;

            Color core = sp.warm ? new Color(255, 150, 90) : new Color(210, 225, 240);
            Color glow = sp.warm ? new Color(255, 150, 90, 0) : new Color(210, 225, 240, 0);

            // Jejak tipis di belakang arah geraknya (kesan "terbang", bukan cuma mengambang diam)
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a * 0.5f));
            g2d.setColor(core);
            g2d.setStroke(new BasicStroke(sp.size * 0.6f));
            g2d.drawLine((int) sp.x, (int) sp.y, (int) (sp.x - Math.sin(sp.driftPhase) * sp.sway), (int) (sp.y + sp.size * 5));

            // Glow lembut di sekitar inti
            RadialGradientPaint sparkGlow = new RadialGradientPaint(
                    new Point((int) sp.x, (int) sp.y),
                    Math.max(sp.size * 3f, 1f),
                    new float[]{0f, 1f},
                    new Color[]{new Color(core.getRed(), core.getGreen(), core.getBlue(), (int) (140 * a)), glow}
            );
            g2d.setPaint(sparkGlow);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a));
            g2d.fillOval((int) (sp.x - sp.size * 1.5f), (int) (sp.y - sp.size * 1.5f), (int) (sp.size * 3f), (int) (sp.size * 3f));

            // Inti kecil terang di tengah
            g2d.setColor(new Color(255, 245, 235, (int) (220 * a)));
            g2d.fillOval((int) (sp.x - sp.size * 0.3f), (int) (sp.y - sp.size * 0.3f), (int) (sp.size * 0.6f), (int) (sp.size * 0.6f));
        }
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));

        // ==========================================
        // 5. "PRESS ENTER TO CONTINUE" — tenang, garis titik-titik nempel di teks
        // ==========================================
        if (showPrompt || transitioning) {
            float promptAlpha = transitioning ? Math.max(0f, 1f - fadeOutAlpha) : 1f;
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, pulseAlpha * promptAlpha));

            g2d.setFont(new Font("SansSerif", Font.PLAIN, 14));
            float promptTracking = 4f;
            String promptText = "PRESS ENTER TO CONTINUE";
            FontMetrics pfm = g2d.getFontMetrics();
            float promptWidth = trackedWidth(pfm, promptText, promptTracking);
            float promptX = centerX - promptWidth / 2f;
            int promptY = height - height / 4;

            g2d.setColor(new Color(0, 0, 0, 160));
            drawTrackedString(g2d, promptText, promptX + 1, promptY + 1, promptTracking);
            g2d.setColor(new Color(225, 222, 215));
            drawTrackedString(g2d, promptText, promptX, promptY, promptTracking);

            // Garis titik-titik pendek nempel langsung di kiri-kanan teks
            g2d.setColor(new Color(210, 205, 195, 200));
            drawDottedLine(g2d, (int) promptX - 90, (int) promptX - 14, promptY - 5);
            drawDottedLine(g2d, (int) (promptX + promptWidth) + 14, (int) (promptX + promptWidth) + 90, promptY - 5);
        }

        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, transitioning ? Math.max(0f, 1f - fadeOutAlpha) : 1.0f));

        // ==========================================
        // 6. CREDIT / COPYRIGHT — kecil, senyap, pojok bawah tengah
        // ==========================================
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g2d.setColor(new Color(150, 148, 145, 200));
        String creditLine = "HEART & HORDE ~ BLOODSHED IN CRYONIA  |  A DARK FANTASY TOWER DEFENSE";
        int cw = g2d.getFontMetrics().stringWidth(creditLine);
        g2d.drawString(creditLine, centerX - cw / 2, height - 22);

        // ==========================================
        // 7. FADE OUT (transisi ke Menu) & FADE IN (buka splash)
        // ==========================================
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        if (transitioning) {
            g2d.setColor(new Color(0, 0, 0, (int) (255 * Math.min(1f, fadeOutAlpha))));
            g2d.fillRect(0, 0, width, height);
        }
        if (introAlpha > 0f) {
            g2d.setColor(new Color(0, 0, 0, (int) (255 * introAlpha)));
            g2d.fillRect(0, 0, width, height);
        }

        g2d.dispose();
    }

    /** Gambar teks per-huruf dengan jarak antar huruf (tracking) tambahan; mengembalikan posisi X setelahnya. */
    private float drawTrackedString(Graphics2D g2d, String text, float x, float y, float tracking) {
        FontMetrics fm = g2d.getFontMetrics();
        float cursor = x;
        for (int i = 0; i < text.length(); i++) {
            String ch = String.valueOf(text.charAt(i));
            g2d.drawString(ch, cursor, y);
            cursor += fm.stringWidth(ch) + tracking;
        }
        return cursor - tracking;
    }

    private float trackedWidth(FontMetrics fm, String text, float tracking) {
        float w = 0;
        for (int i = 0; i < text.length(); i++) {
            w += fm.stringWidth(String.valueOf(text.charAt(i))) + tracking;
        }
        return w - tracking;
    }

    /** Garis titik-titik kecil, dipakai untuk aksen di kiri-kanan teks "Press Enter". */
    private void drawDottedLine(Graphics2D g2d, int x1, int x2, int y) {
        int gap = 5;
        for (int x = x1; x < x2; x += gap) {
            g2d.fillRect(x, y, 2, 2);
        }
    }

    private void drawKenBurnsBackground(Graphics2D g2d, int width, int height) {
        int imgW = backgroundImage.getWidth();
        int imgH = backgroundImage.getHeight();

        double cycle = (tickCounter % 4500) / 4500.0; // siklus lebih pelan (~72 detik), kesan tenang
        double wave = Math.sin(cycle * Math.PI * 2);

        float zoom = 1.06f + 0.03f * (float) wave;
        float panX = (float) (Math.cos(cycle * Math.PI * 2) * width * 0.015f);
        float panY = (float) (Math.sin(cycle * Math.PI * 2) * height * 0.01f);

        float coverScale = Math.max((float) width / imgW, (float) height / imgH) * zoom;
        int drawW = (int) (imgW * coverScale);
        int drawH = (int) (imgH * coverScale);
        int drawX = (int) ((width - drawW) / 2f + panX);
        int drawY = (int) ((height - drawH) / 2f + panY);

        g2d.drawImage(backgroundImage, drawX, drawY, drawW, drawH, null);
    }
}