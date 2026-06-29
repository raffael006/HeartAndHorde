import javax.swing.*;
import java.awt.*;
import java.util.Random;

public class SplashPanel extends JPanel {
    private GameWindow window;
    private int progress = 0;

    // Variabel Animasi
    private float alpha = 0.0f; // Untuk efek fade-in text utama
    private float pulseAlpha = 0.0f; // Untuk efek denyut teks loading
    private boolean pulseUp = true;

    private Timer timer;
    private String currentTip;
    private Random random = new Random();

    // Kumpulan tips game / Lore cerita khas Medieval Fantasy
    private final String[] GAME_TIPS = {
            "TIP: Perkuat tembok benteng sebelum malam tiba!",
            "LORE: Kerajaan ini dulunya makmur, sebelum Horde datang dari tanah gelap.",
            "TIP: Tempatkan Archer (Pemanah) di menara tinggi untuk jangkauan maksimal.",
            "TIP: Bangun barikade kayu untuk memperlambat laju pasukan musuh.",
            "LORE: 'Heart' adalah relik suci kuno yang diincar oleh para monster."
    };

    public SplashPanel(GameWindow window) {
        this.window = window;
        setBackground(new Color(15, 10, 10)); // Warna hitam kemerahan gelap (Dark Crimson)
        setLayout(null);

        currentTip = GAME_TIPS[random.nextInt(GAME_TIPS.length)];

        // Loop Animasi (Dijalankan setiap ~16ms)
        timer = new Timer(16, e -> {
            // 1. Progress loading
            if (progress < 100) {
                if (random.nextInt(10) > 7) progress += 1;
            }

            // 2. Efek Fade-in text utama
            if (alpha < 1.0f) {
                alpha += 0.01f;
                if (alpha > 1.0f) alpha = 1.0f;
            }

            // 3. Efek Pulsating (Redup-Nyala) untuk teks Loading
            if (pulseUp) {
                pulseAlpha += 0.02f;
                if (pulseAlpha >= 1.0f) {
                    pulseAlpha = 1.0f;
                    pulseUp = false;
                }
            } else {
                pulseAlpha -= 0.02f;
                if (pulseAlpha <= 0.2f) { // Jangan sampai hilang total
                    pulseAlpha = 0.2f;
                    pulseUp = true;
                }
            }

            // 4. Ganti tips saat setengah jalan
            if (progress == 50 && currentTip.equals(GAME_TIPS[0])) {
                currentTip = GAME_TIPS[random.nextInt(GAME_TIPS.length)];
            }

            repaint();

            // 5. Pindah layar
            if (progress >= 100) {
                timer.stop();
                window.showScreen("MENU_SCREEN");
            }
        });
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        // ==========================================
        // 1. BACKGROUND GRADIENT (Ruang Bawah Tanah / Kastil Kelam)
        // ==========================================
        // Gradasi dari merah darah tua di atas ke hitam pekat di bawah
        GradientPaint gp = new GradientPaint(0, 0, new Color(40, 15, 15), 0, height, new Color(5, 5, 5));
        g2d.setPaint(gp);
        g2d.fillRect(0, 0, width, height);

        // ==========================================
        // 2. GAMBAR JUDUL (Fade-In dengan warna Emas Kuno)
        // ==========================================
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

        // Bayangan Teks (Drop Shadow)
        g2d.setFont(new Font("Georgia", Font.BOLD, 64)); // Font klasik elegan
        g2d.setColor(new Color(0, 0, 0, 180));
        String titleText = "HEART & HORDE";
        int titleWidth = g2d.getFontMetrics().stringWidth(titleText);
        g2d.drawString(titleText, (width - titleWidth) / 2 + 5, height / 2 - 80 + 5);

        // Teks Utama (Warna Emas/Kuning Kuno)
        g2d.setColor(new Color(218, 165, 32)); // Goldenrod color
        g2d.drawString(titleText, (width - titleWidth) / 2, height / 2 - 80);

        // Sub-judul (Merah Pudar)
        g2d.setFont(new Font("Serif", Font.ITALIC, 26));
        g2d.setColor(new Color(180, 80, 80));
        String subText = "— Bloodshed in Cryonia —";
        int subWidth = g2d.getFontMetrics().stringWidth(subText);
        g2d.drawString(subText, (width - subWidth) / 2, height / 2 - 30);

        // ==========================================
        // 3. GAMBAR PROGRESS BAR (Batu & Api)
        // ==========================================
        int barWidth = 500;
        int barHeight = 8;
        int barX = (width - barWidth) / 2;
        int barY = height - 150;

        // Bingkai luar (Warna Batu Gelap)
        g2d.setColor(new Color(30, 25, 25));
        g2d.fillRoundRect(barX, barY, barWidth, barHeight, 4, 4);
        g2d.setColor(new Color(10, 10, 10)); // Garis tepi ukiran batu
        g2d.drawRoundRect(barX, barY, barWidth, barHeight, 4, 4);

        // Isian Progress Bar (Gradasi Api / Darah)
        int currentBarWidth = (int) ((progress / 100.0) * barWidth);
        GradientPaint barGrad = new GradientPaint(barX, 0, new Color(139, 0, 0), barX + barWidth, 0, new Color(255, 140, 0)); // Dark Red to Dark Orange
        g2d.setPaint(barGrad);
        g2d.fillRoundRect(barX, barY, currentBarWidth, barHeight, 4, 4);

        // Teks Persentase
        g2d.setFont(new Font("Serif", Font.BOLD, 16));
        g2d.setColor(new Color(218, 165, 32));
        g2d.drawString(progress + "%", barX + barWidth + 15, barY + 9);

        // ==========================================
        // 4. TEKS LOADING BERDENYUT & LORE
        // ==========================================
        // Teks Loading menggunakan Alpha berdenyut
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, pulseAlpha));
        g2d.setFont(new Font("Serif", Font.BOLD, 14));
        g2d.setColor(new Color(200, 180, 150)); // Warna perkamen kusam
        g2d.drawString("PREPARING THE BATTLEFIELD...", barX, barY - 15);

        // Reset composite ke normal untuk Teks Lore
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        g2d.setFont(new Font("Serif", Font.ITALIC, 16));
        g2d.setColor(new Color(150, 140, 130));
        int tipWidth = g2d.getFontMetrics().stringWidth(currentTip);
        g2d.drawString(currentTip, (width - tipWidth) / 2, barY + 50);
    }
}