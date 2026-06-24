import javax.swing.*;
import java.awt.*;
import java.util.LinkedList;
import java.util.List;

public class GameWindow extends JFrame {

    // Alat pemindah layar
    private CardLayout cardLayout;
    private JPanel mainContainer;

    // Data global game disimpan di "Bos" agar Menu dan GamePanel bisa mengaksesnya
    public List<Building> savedBuildings = new LinkedList<>();
    public List<Guard> activeGuards = new LinkedList<>();
    public List<Horde> activeHordes = new LinkedList<>();
    public List<Projectile> activeProjectiles = new LinkedList<>();
    public List<Civil> activeCivils = new LinkedList<>();

    public GameWindow() {
        setTitle("Heart & Horde ~ Bloodshed in Cryonia");
        setSize(1920, 1080);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        // --- KODE IKON APLIKASI ---
        try {
            // Mengambil gambar langsung dari path relatif assets/img/
            Image iconImage = new ImageIcon("assets/img/app_icon.png").getImage();
            this.setIconImage(iconImage);
        } catch (Exception e) {
            System.out.println("Gagal load icon: " + e.getMessage());
        }
        // ---------------------------

        // Inisialisasi CardLayout (Sistem tumpukan layar)
        cardLayout = new CardLayout();
        mainContainer = new JPanel(cardLayout);

        // --- NANTI KITA AKAN MASUKKAN MENU DAN GAME DI SINI ---
        mainContainer.add(new MenuPanel(this), "MENU_SCREEN");
        mainContainer.add(new GamePanel(this), "GAME_SCREEN");

        setContentPane(mainContainer);
    }

    // Fungsi sakti untuk berpindah layar dengan 1 baris kode
    public void showScreen(String screenName) {
        cardLayout.show(mainContainer, screenName);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GameWindow window = new GameWindow();
            window.setVisible(true);
        });
    }
}