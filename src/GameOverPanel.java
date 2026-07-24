import javax.swing.*;
import java.awt.*;

/**
 * Overlay fullscreen yang muncul saat Campaign berakhir -- MENANG (semua wave tuntas
 * & Horde terakhir habis) atau KALAH (Heart hancur). Menampilkan judul, ringkasan
 * statistik (hari bertahan, wave tercapai, resource terkumpul, sisa Civil/Guard/Bangunan),
 * dan 2 tombol: "Main Lagi" (restart campaign) & "Kembali ke Menu" (balik ke Main Menu).
 *
 * Panel ini murni tampilan + 2 callback -- semua keputusan KAPAN menang/kalah tetap
 * ditentukan oleh GamePanel (lewat triggerGameOver()), bukan di sini.
 */
public class GameOverPanel extends JPanel {

    private boolean won = false;
    private String[] statLines = new String[0];

    private final JButton restartBtn;
    private final JButton backBtn;

    public GameOverPanel(Runnable onRestart, Runnable onBackToMenu) {
        super(null);
        setOpaque(false);
        setVisible(false);

        restartBtn = createStyledButton("Main Lagi", new Color(60, 110, 60));
        backBtn = createStyledButton("Kembali ke Menu", new Color(90, 70, 45));
        restartBtn.addActionListener(e -> onRestart.run());
        backBtn.addActionListener(e -> onBackToMenu.run());
        add(restartBtn);
        add(backBtn);
    }

    /** Tampilkan hasil akhir campaign. Dipanggil sekali oleh GamePanel begitu menang/kalah terdeteksi. */
    public void showResult(boolean won, String[] statLines) {
        this.won = won;
        this.statLines = statLines;
        setVisible(true);
        doLayout();
        repaint();
    }

    public void hideResult() {
        setVisible(false);
    }

    // Reposisi tombol tiap kali ukuran panel berubah (dipanggil otomatis oleh Swing saat
    // setBounds/resize, dan manual sekali di showResult supaya langsung pas pas pertama muncul).
    @Override
    public void doLayout() {
        int w = getWidth(), h = getHeight();
        int bw = 190, bh = 46, gap = 24;
        int totalW = bw * 2 + gap;
        int startX = (w - totalW) / 2;
        int by = h / 2 + 140;
        restartBtn.setBounds(startX, by, bw, bh);
        backBtn.setBounds(startX + bw + gap, by, bw, bh);
    }

    private JButton createStyledButton(String label, Color baseColor) {
        JButton btn = new JButton(label) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color top = getModel().isRollover() ? baseColor.brighter() : baseColor;
                Color bot = getModel().isRollover() ? baseColor : baseColor.darker().darker();
                g2d.setPaint(new GradientPaint(0, 0, top, 0, getHeight(), bot));
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2d.setColor(new Color(220, 195, 130));
                g2d.setStroke(new BasicStroke(1.5f));
                g2d.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 8, 8);
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Georgia", Font.BOLD, 15));
                FontMetrics fm = g2d.getFontMetrics();
                g2d.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2d.dispose();
            }
        };
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth(), h = getHeight();

        // Latar gelap transparan menutup seluruh layar
        g2d.setColor(new Color(8, 6, 5, 205));
        g2d.fillRect(0, 0, w, h);

        // --- JUDUL ---
        String title = won ? "KEMENANGAN!" : "KEKALAHAN...";
        Color titleColor = won ? new Color(255, 210, 70) : new Color(215, 60, 55);
        g2d.setFont(new Font("Georgia", Font.BOLD, 54));
        FontMetrics fmTitle = g2d.getFontMetrics();
        int titleY = h / 2 - 150;
        g2d.setColor(new Color(0, 0, 0, 160));
        g2d.drawString(title, (w - fmTitle.stringWidth(title)) / 2 + 2, titleY + 2);
        g2d.setColor(titleColor);
        g2d.drawString(title, (w - fmTitle.stringWidth(title)) / 2, titleY);

        // --- SUBJUDUL ---
        String subtitle = won
                ? "Cryonia berhasil bertahan dari seluruh gelombang serangan!"
                : "Heart telah hancur -- Cryonia runtuh...";
        g2d.setFont(new Font("Serif", Font.ITALIC, 16));
        g2d.setColor(new Color(215, 200, 175));
        FontMetrics fmSub = g2d.getFontMetrics();
        g2d.drawString(subtitle, (w - fmSub.stringWidth(subtitle)) / 2, titleY + 34);

        // --- KOTAK STATISTIK ---
        int lineGap = 25;
        int boxPaddingTop = 34;
        int boxPaddingBottom = 22;
        int boxW = 380;
        int boxH = boxPaddingTop + boxPaddingBottom + Math.max(1, statLines.length) * lineGap;
        int boxX = (w - boxW) / 2;
        int boxY = titleY + 60;

        g2d.setColor(new Color(24, 19, 14, 235));
        g2d.fillRoundRect(boxX, boxY, boxW, boxH, 14, 14);
        g2d.setColor(new Color(120, 90, 40));
        g2d.setStroke(new BasicStroke(2f));
        g2d.drawRoundRect(boxX, boxY, boxW, boxH, 14, 14);

        g2d.setFont(new Font("Serif", Font.PLAIN, 15));
        g2d.setColor(new Color(220, 205, 175));
        int lineX = boxX + 26;
        int lineY = boxY + boxPaddingTop;
        for (String line : statLines) {
            g2d.drawString(line, lineX, lineY);
            lineY += lineGap;
        }

        g2d.dispose();
    }
}