import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Popup menu in-game (titik-3 di HUD): Save Progress / toggle Developer Mode / Back to Main Menu,
 * plus fungsi penyimpanan ke disk. Diekstrak dari GamePanel.showInGameMenu() & saveGameData()
 * apa adanya (logic & tampilan tidak diubah).
 *
 * isCheatActive/setCheatActive dipakai (bukan boolean biasa) karena field aslinya
 * (isCheatModeActive) tetap tinggal di GamePanel -- dibaca/ditulis banyak tempat lain
 * (cek cost bangunan, cheat spawn via keybinding P/O/dll) di luar menu ini.
 * holdingBuildingSupplier dipakai supaya nilai holdingBuilding yang dibaca tetap
 * live/terkini pas tombol "Back to Main Menu" benar-benar diklik, bukan snapshot
 * pas menu ini pertama kali dibuka.
 */
public class InGameMenu {

    private InGameMenu() {
        // Utility class, tidak perlu diinstansiasi
    }

    public static void show(JComponent anchor, JButton menuBtn, GameWindow window,
                            List<Mine> activeMines,
                            Supplier<Building> holdingBuildingSupplier,
                            BooleanSupplier isCheatActive,
                            Consumer<Boolean> setCheatActive) {
        // ================================================================
        // CUSTOM IN-GAME MENU — desain dark fantasy, bukan default Java UI
        // ================================================================
        JWindow menuWin = new JWindow(SwingUtilities.getWindowAncestor(anchor));
        menuWin.setBackground(new Color(0, 0, 0, 0));

        // Posisikan di dekat tombol menu (pojok kanan atas)
        Point btnLoc = menuBtn.getLocationOnScreen();
        int menuW = 220, menuH = 148;
        menuWin.setBounds(btnLoc.x - menuW + menuBtn.getWidth(), btnLoc.y + menuBtn.getHeight() + 4, menuW, menuH);

        // --- Panel utama custom-painted ---
        JPanel menuPanel = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                // Background
                g2d.setColor(new Color(18, 14, 12, 250));
                g2d.fillRoundRect(0, 0, w, h, 12, 12);
                // Border emas
                g2d.setColor(new Color(100, 75, 35));
                g2d.setStroke(new BasicStroke(1.5f));
                g2d.drawRoundRect(1, 1, w - 3, h - 3, 12, 12);
                g2d.dispose();
            }
        };
        menuPanel.setOpaque(false);
        menuPanel.setBounds(0, 0, menuW, menuH);

        // Helper buat item menu
        BiFunction<String, Runnable, JPanel> mkItem = (label, action) -> {
            JPanel item = new JPanel(null) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    if (getClientProperty("hover") == Boolean.TRUE) {
                        g2.setColor(new Color(50, 40, 25));
                        g2.fillRoundRect(4, 1, getWidth() - 8, getHeight() - 2, 6, 6);
                    }

                    int bx = 12, by = getHeight() / 2 - 7, bs = 14;
                    Color gold = new Color(185, 145, 60);
                    Color goldDark = new Color(100, 75, 25);

                    if (label.equals("Save Progress")) {
                        // --- IKON FLOPPY DISK ---
                        // Body luar
                        g2.setColor(goldDark);
                        g2.fillRect(bx, by, bs, bs);
                        g2.setColor(gold);
                        g2.setStroke(new BasicStroke(1.4f));
                        g2.drawRect(bx, by, bs, bs);
                        // Notch sudut kanan atas (ciri khas floppy)
                        g2.setColor(new Color(18, 14, 12));
                        g2.fillPolygon(new int[]{bx+9, bx+bs, bx+bs}, new int[]{by, by, by+5}, 3);
                        g2.setColor(gold);
                        g2.drawLine(bx+9, by, bx+bs, by+5);
                        // Shutter (kotak kecil tengah atas)
                        g2.setColor(new Color(130, 100, 35));
                        g2.fillRect(bx+3, by, 5, 4);
                        // Label area (kotak bawah)
                        g2.setColor(new Color(130, 100, 35));
                        g2.fillRect(bx+2, by+8, bs-4, bs-9);
                        g2.setColor(gold);
                        g2.setStroke(new BasicStroke(0.8f));
                        g2.drawRect(bx+2, by+8, bs-4, bs-9);

                    } else if (label.equals("Back to Main Menu")) {
                        // --- IKON PANAH KIRI ---
                        // Kepala panah (segitiga kiri)
                        g2.setColor(gold);
                        int[] px = {bx+5, bx+bs, bx+bs};
                        int[] py = {by+7, by+1, by+13};
                        g2.fillPolygon(px, py, 3);
                        // Batang panah
                        g2.fillRect(bx+bs, by+4, 1, 6);
                        // Garis outline kepala
                        g2.setColor(goldDark);
                        g2.setStroke(new BasicStroke(0.8f));
                        g2.drawPolygon(px, py, 3);
                    }

                    // Label teks
                    g2.setFont(new Font("Georgia", Font.PLAIN, 13));
                    g2.setColor(new Color(210, 185, 130));
                    g2.drawString(label, 34, getHeight() / 2 + 5);
                    g2.dispose();
                }
            };
            item.setOpaque(false);
            item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            item.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { item.putClientProperty("hover", true); item.repaint(); }
                @Override public void mouseExited(MouseEvent e)  { item.putClientProperty("hover", false); item.repaint(); }
                @Override public void mouseClicked(MouseEvent e) { menuWin.dispose(); if (action != null) action.run(); }
            });
            return item;
        };

        // ---- ITEM 1: Save Progress ----
        JPanel saveItem = mkItem.apply("Save Progress", () -> {
            String[] options = {"Slot 1", "Slot 2", "Slot 3"};
            int choice = JOptionPane.showOptionDialog(window, "Pilih Slot Penyimpanan:", "Save Progress",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
            if (choice >= 0) saveGameData(window, activeMines, choice + 1);
        });
        saveItem.setBounds(0, 8, menuW, 36);

        // ---- Separator 1 ----
        JPanel sep1 = new JPanel() {
            @Override protected void paintComponent(Graphics g) { g.setColor(new Color(60, 48, 28)); g.fillRect(10, 0, getWidth()-20, 1); }
        };
        sep1.setOpaque(false);
        sep1.setBounds(0, 44, menuW, 2);

        // ---- ITEM 2: Developer Mode (checkbox style) ----
        JPanel devItem = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                if (getClientProperty("hover") == Boolean.TRUE) {
                    g2.setColor(new Color(50, 40, 25));
                    g2.fillRoundRect(4, 1, getWidth() - 8, getHeight() - 2, 6, 6);
                }
                // --- Checkbox: digambar langsung Java2D ---
                int bx = 14, by = getHeight() / 2 - 6, bs = 12;
                // Isi kotak: emas kalau aktif, gelap kalau tidak
                g2.setColor(isCheatActive.getAsBoolean() ? new Color(160, 120, 35) : new Color(35, 28, 18));
                g2.fillRect(bx, by, bs, bs);
                // Border kotak
                g2.setColor(new Color(185, 145, 60));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRect(bx, by, bs, bs);
                // Centang kalau aktif
                if (isCheatActive.getAsBoolean()) {
                    g2.setColor(new Color(20, 14, 8));
                    g2.setStroke(new BasicStroke(2.2f));
                    g2.drawLine(bx + 2, by + 6, bx + 5, by + 9);
                    g2.drawLine(bx + 5, by + 9, bx + 10, by + 3);
                }
                // Label teks
                g2.setFont(new Font("Georgia", Font.PLAIN, 13));
                g2.setColor(new Color(210, 185, 130));
                g2.drawString("Developer Mode", 34, getHeight() / 2 + 5);
                g2.dispose();
            }
        };
        devItem.setOpaque(false);
        devItem.setBounds(0, 46, menuW, 36);
        devItem.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        devItem.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { devItem.putClientProperty("hover", true); devItem.repaint(); }
            @Override public void mouseExited(MouseEvent e)  { devItem.putClientProperty("hover", false); devItem.repaint(); }
            @Override public void mouseClicked(MouseEvent e) {
                setCheatActive.accept(!isCheatActive.getAsBoolean());
                devItem.repaint();
            }
        });

        // ---- Separator 2 ----
        JPanel sep2 = new JPanel() {
            @Override protected void paintComponent(Graphics g) { g.setColor(new Color(60, 48, 28)); g.fillRect(10, 0, getWidth()-20, 1); }
        };
        sep2.setOpaque(false);
        sep2.setBounds(0, 82, menuW, 2);

        // ---- ITEM 3: Back to Main Menu ----
        JPanel backItem = mkItem.apply("Back to Main Menu", () -> {
            Building holdingBuilding = holdingBuildingSupplier.get();
            if (holdingBuilding != null) window.savedBuildings.add(holdingBuilding);
            window.showScreen("MENU_SCREEN");
        });
        backItem.setBounds(0, 84, menuW, 36);

        // ---- Tambah ke panel ----
        menuPanel.add(saveItem);
        menuPanel.add(sep1);
        menuPanel.add(devItem);
        menuPanel.add(sep2);
        menuPanel.add(backItem);
        menuWin.add(menuPanel);

        // --- Tutup kalau klik di luar menu (pakai AWTEventListener, lebih reliable dari windowLostFocus) ---
        java.awt.event.AWTEventListener[] closeListenerHolder = new java.awt.event.AWTEventListener[1];
        closeListenerHolder[0] = event -> {
            if (event instanceof MouseEvent) {
                MouseEvent me = (MouseEvent) event;
                if (me.getID() == MouseEvent.MOUSE_PRESSED) {
                    Point clickScreen = me.getLocationOnScreen();
                    if (!menuWin.getBounds().contains(clickScreen)) {
                        menuWin.dispose();
                        Toolkit.getDefaultToolkit().removeAWTEventListener(closeListenerHolder[0]);
                    }
                }
            }
        };
        Toolkit.getDefaultToolkit().addAWTEventListener(
                closeListenerHolder[0], AWTEvent.MOUSE_EVENT_MASK);

        // Pastikan listener juga dihapus saat window di-dispose (anti-memory leak)
        menuWin.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                Toolkit.getDefaultToolkit().removeAWTEventListener(closeListenerHolder[0]);
            }
        });

        menuWin.setVisible(true);
    }

    // --- TAMBAHKAN FUNGSI PENYIMPANAN INI TEPAT DI BAWAHNYA ---
    private static void saveGameData(GameWindow window, List<Mine> activeMines, int slot) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("heart_save_" + slot + ".dat"))) {
            // --- FITUR BARU: Sebelumnya cuma savedBuildings doang, sekarang Civil/Guard/Horde/
            // CivilBuilder/Mine ikut disimpan juga biar pas di-load penduduk & pasukannya gak hilang ---
            oos.writeObject(window.savedBuildings);
            oos.writeObject(window.activeCivils);
            oos.writeObject(window.activeGuards);
            oos.writeObject(window.activeHordes);
            oos.writeObject(window.activeCivilBuilders);
            oos.writeObject(activeMines);
            JOptionPane.showMessageDialog(window, "Progress kota berhasil disimpan di Slot " + slot + "!", "Save Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            System.out.println("Gagal menyimpan game: " + e.getMessage());
        }
    }
}