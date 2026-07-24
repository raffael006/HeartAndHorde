import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Factory untuk tombol-tombol grid Build Menu (kotak 3x4) & display jumlah Civil.
 * Diekstrak dari GamePanel apa adanya (tampilan tidak diubah) -- class ini murni
 * "bagaimana tombolnya digambar", TIDAK menyimpan state menu apapun (MenuState,
 * selectedBuilding, dll tetap di GamePanel karena itu logic, bukan tampilan).
 */
public class GridButtonFactory {

    private GridButtonFactory() {
        // Utility class, tidak perlu diinstansiasi
    }

    public static JButton createGridBtn(String text, Runnable action, boolean isSelected) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();

                // MENGGELAP SAAT DIPILIH ATAU DI-HOVER
                if (isSelected || (getModel().isRollover() && action != null)) {
                    g2d.setColor(new Color(15, 12, 10));
                } else {
                    g2d.setColor(new Color(40, 35, 30));
                }
                g2d.fillRect(0, 0, getWidth(), getHeight());

                // Border tiap kotak (Perunggu Gelap)
                g2d.setColor(new Color(80, 60, 35));
                g2d.drawRect(0, 0, getWidth(), getHeight());

                g2d.setColor(new Color(210, 190, 160));
                g2d.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 26));
                FontMetrics fm = g2d.getFontMetrics();
                if (!text.isEmpty()) {
                    g2d.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                }
                g2d.dispose();
            }
        };
        btn.setContentAreaFilled(false); btn.setBorderPainted(false); btn.setFocusPainted(false);
        if (action != null) btn.addActionListener(e -> action.run());
        return btn;
    }

    // --- FITUR BARU: Varian createGridBtn yang menampilkan gambar sprite (untuk menu Barrack) ---
    public static JButton createGridBtnWithImage(BufferedImage img, String label, Runnable action) {
        JButton btn = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Background + hover
                g2d.setColor(getModel().isRollover() ? new Color(15, 12, 10) : new Color(40, 35, 30));
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.setColor(new Color(80, 60, 35));
                g2d.drawRect(0, 0, getWidth(), getHeight());

                int w = getWidth(), h = getHeight();

                // Gambar sprite di tengah atas
                if (img != null) {
                    int imgSize = Math.min(w - 10, h - 20);
                    int imgX = (w - imgSize) / 2;
                    int imgY = 4;
                    g2d.drawImage(img, imgX, imgY, imgSize, imgSize, null);
                }

                // Label nama di bawah gambar
                g2d.setColor(new Color(210, 190, 160));
                g2d.setFont(new Font("Serif", Font.BOLD, 10));
                FontMetrics fm = g2d.getFontMetrics();
                g2d.drawString(label, (w - fm.stringWidth(label)) / 2, h - 5);

                g2d.dispose();
            }
        };
        btn.setContentAreaFilled(false); btn.setBorderPainted(false); btn.setFocusPainted(false);
        if (action != null) btn.addActionListener(e -> action.run());
        return btn;
    }

    // --- FITUR BARU: Tombol grid pakai gambar sprite BANGUNAN asli, gelap + badge cost kalau kayu kurang ---
    // (img & cost sudah diresolve oleh pemanggil lewat Building.getWoodCost()/getBuildImage(), supaya
    // factory ini tidak perlu tahu apa-apa soal Building.BuildingType atau cara GamePanel resolve asetnya.)
    public static JButton createGridBtnBuilding(BufferedImage img, int cost, ResourceManager resourceManager,
                                                BufferedImage iconWood, Runnable action, boolean isSelected) {
        JButton btn = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // --- FIX: dihitung ulang TIAP repaint (bukan cuma sekali pas tombol dibuat) ---
                // supaya warnanya beneran real-time ikut resourceManager.wood yang sekarang, tanpa perlu
                // buka-tutup grid dulu buat lihat perubahannya.
                boolean canAfford = resourceManager.wood >= cost;

                if (isSelected || getModel().isRollover()) {
                    g2d.setColor(new Color(15, 12, 10));
                } else {
                    g2d.setColor(new Color(40, 35, 30));
                }
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.setColor(new Color(80, 60, 35));
                g2d.drawRect(0, 0, getWidth(), getHeight());

                int w = getWidth(), h = getHeight();

                if (img != null) {
                    int imgSize = Math.min(w - 12, h - 12);
                    int imgX = (w - imgSize) / 2;
                    int imgY = (h - imgSize) / 2;

                    if (!canAfford) {
                        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
                    }
                    g2d.drawImage(img, imgX, imgY, imgSize, imgSize, null);
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
                }

                // --- FITUR BARU: Badge cost SELALU ditampilkan (bukan cuma pas gak cukup) ---
                // Hijau kalau resourceManager.wood cukup, merah kalau kurang -> real time tiap repaint.
                g2d.setColor(new Color(0, 0, 0, 180));
                g2d.fillRect(0, h - 16, w, 16);

                if (iconWood != null) g2d.drawImage(iconWood, 3, h - 15, 14, 14, null);
                g2d.setFont(new Font("Segoe UI", Font.BOLD, 11));
                g2d.setColor(canAfford ? new Color(90, 220, 90) : new Color(230, 90, 90));
                g2d.drawString(String.valueOf(cost), 20, h - 4);

                g2d.dispose();
            }
        };
        btn.setContentAreaFilled(false); btn.setBorderPainted(false); btn.setFocusPainted(false);
        if (action != null) btn.addActionListener(e -> action.run());
        return btn;
    }

    // --- FITUR BARU: Varian createGridBtnBuilding buat building yang cost-nya lebih dari sekadar wood
    // (mis. Archer Tower: wood + stone + civil). Badge cost jadi bertumpuk 1 baris per resource yang
    // cost-nya > 0 -- kalau cuma wood, tampilannya identik sama varian lama (createGridBtnBuilding 6-arg). ---
    public static JButton createGridBtnBuilding(BufferedImage img, int woodCost, int stoneCost, int civilCost,
                                                ResourceManager resourceManager, int availableCivilCount,
                                                BufferedImage iconWood, BufferedImage iconStone, BufferedImage iconCivil,
                                                Runnable action, boolean isSelected) {
        JButton btn = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Dihitung ulang tiap repaint (bukan sekali pas tombol dibuat), sama kayak varian aslinya,
                // supaya warnanya real-time ikut resourceManager & jumlah Civil yang sekarang.
                boolean canAffordWood = resourceManager.wood >= woodCost;
                boolean canAffordStone = resourceManager.stone >= stoneCost;
                boolean canAffordCivil = availableCivilCount >= civilCost;
                boolean canAffordAll = canAffordWood && canAffordStone && canAffordCivil;

                if (isSelected || getModel().isRollover()) {
                    g2d.setColor(new Color(15, 12, 10));
                } else {
                    g2d.setColor(new Color(40, 35, 30));
                }
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.setColor(new Color(80, 60, 35));
                g2d.drawRect(0, 0, getWidth(), getHeight());

                int w = getWidth(), h = getHeight();

                // Wood selalu ditampilkan (baris pertama); Stone & Civil cuma nongol kalau cost-nya > 0
                int rowH = 14;
                int rowCount = 1 + (stoneCost > 0 ? 1 : 0) + (civilCost > 0 ? 1 : 0);
                int badgeAreaH = rowCount * rowH;

                if (img != null) {
                    int imgSize = Math.min(w - 12, h - 12 - (badgeAreaH - rowH));
                    int imgX = (w - imgSize) / 2;
                    int imgY = 2;

                    if (!canAffordAll) {
                        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
                    }
                    g2d.drawImage(img, imgX, imgY, imgSize, imgSize, null);
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
                }

                g2d.setFont(new Font("Segoe UI", Font.BOLD, 10));
                int rowY = h - badgeAreaH;

                // --- Baris Wood ---
                g2d.setColor(new Color(0, 0, 0, 180));
                g2d.fillRect(0, rowY, w, rowH);
                if (iconWood != null) g2d.drawImage(iconWood, 3, rowY + 1, 12, 12, null);
                g2d.setColor(canAffordWood ? new Color(90, 220, 90) : new Color(230, 90, 90));
                g2d.drawString(String.valueOf(woodCost), 18, rowY + 11);
                rowY += rowH;

                // --- Baris Stone (cuma kalau dibutuhkan) ---
                if (stoneCost > 0) {
                    g2d.setColor(new Color(0, 0, 0, 180));
                    g2d.fillRect(0, rowY, w, rowH);
                    if (iconStone != null) g2d.drawImage(iconStone, 3, rowY + 1, 12, 12, null);
                    g2d.setColor(canAffordStone ? new Color(90, 220, 90) : new Color(230, 90, 90));
                    g2d.drawString(String.valueOf(stoneCost), 18, rowY + 11);
                    rowY += rowH;
                }

                // --- Baris Civil (cuma kalau dibutuhkan) ---
                if (civilCost > 0) {
                    g2d.setColor(new Color(0, 0, 0, 180));
                    g2d.fillRect(0, rowY, w, rowH);
                    if (iconCivil != null) g2d.drawImage(iconCivil, 3, rowY + 1, 12, 12, null);
                    g2d.setColor(canAffordCivil ? new Color(90, 220, 90) : new Color(230, 90, 90));
                    g2d.drawString(String.valueOf(civilCost), 18, rowY + 11);
                }

                g2d.dispose();
            }
        };
        btn.setContentAreaFilled(false); btn.setBorderPainted(false); btn.setFocusPainted(false);
        if (action != null) btn.addActionListener(e -> action.run());
        return btn;
    }

    // Teks putih kalau cukup, merah kalau 0 (tidak bisa rekrut)
    public static JPanel createCivilCountDisplay(GameWindow window) {
        return new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Background sama seperti slot grid lain
                g2d.setColor(new Color(40, 35, 30));
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.setColor(new Color(80, 60, 35));
                g2d.drawRect(0, 0, getWidth(), getHeight());

                int civilCount = window.activeCivils.size();
                int w = getWidth(), h = getHeight();

                // Label atas: "Civil"
                g2d.setFont(new Font("Serif", Font.BOLD, 11));
                g2d.setColor(new Color(180, 160, 120));
                String label = "Civil";
                FontMetrics fm = g2d.getFontMetrics();
                g2d.drawString(label, (w - fm.stringWidth(label)) / 2, h / 2 - 8);

                // Angka: putih kalau ada, merah kalau 0
                g2d.setFont(new Font("Serif", Font.BOLD, 20));
                g2d.setColor(civilCount > 0 ? new Color(220, 210, 180) : new Color(220, 50, 50));
                String countStr = String.valueOf(civilCount);
                FontMetrics fm2 = g2d.getFontMetrics();
                g2d.drawString(countStr, (w - fm2.stringWidth(countStr)) / 2, h / 2 + 14);

                // Label bawah: "tersedia" atau "kurang!" kalau 0
                g2d.setFont(new Font("Serif", Font.ITALIC, 9));
                g2d.setColor(civilCount > 0 ? new Color(150, 135, 105) : new Color(200, 60, 60));
                String sub = civilCount > 0 ? "tersedia" : "kurang!";
                FontMetrics fm3 = g2d.getFontMetrics();
                g2d.drawString(sub, (w - fm3.stringWidth(sub)) / 2, h / 2 + 26);

                g2d.dispose();
            }
        };
    }
}