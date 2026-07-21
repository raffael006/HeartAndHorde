import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.LinkedList;
import java.util.List;

public class GameWindow extends JFrame {

    public static Font VIKING_FONT;
    private static void loadCustomFont() {
        try {
            VIKING_FONT = Font.createFont(Font.TRUETYPE_FONT, new File("assets/fonts/VidirT2-nLWP.ttf"));
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(VIKING_FONT);
        } catch (Exception e) {
            System.out.println("Gagal load font Norse: " + e.getMessage());
            VIKING_FONT = new Font("Serif", Font.PLAIN, 12); // fallback biar gak crash
        }
    }

    // Alat pemindah layar
    private CardLayout cardLayout;
    private JPanel mainContainer;

    // Data global game disimpan di "Bos" agar Menu dan GamePanel bisa mengaksesnya
    public List<Building> savedBuildings = new LinkedList<>();
    public List<Guard> activeGuards = new LinkedList<>();
    public List<Horde> activeHordes = new LinkedList<>();
    public List<Projectile> activeProjectiles = new LinkedList<>();
    public List<Civil> activeCivils = new LinkedList<>();
    public List<CivilBuilder> activeCivilBuilders = new LinkedList<>();

    // --- FITUR BARU: Simpan referensi GamePanel biar MenuPanel bisa panggil resetCampaign() ---
    public GamePanel gamePanel;

    // --- FITUR BARU: SISTEM DIFFICULTY CAMPAIGN (Easy / Medium / Hard) ---
    // Dipilih di MenuPanel sebelum tombol CAMPAIGN benar-benar memulai game,
    // lalu dibaca GamePanel.resetCampaign(Difficulty) buat nentuin jenis, jumlah,
    // dan kecepatan munculnya Horde tiap wave.
    public enum Difficulty { EASY, MEDIUM, HARD }
    public Difficulty selectedDifficulty = Difficulty.MEDIUM;

    public GameWindow() {
        loadCustomFont();
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

        // --- SISTEM TUMPUKAN LAYAR GAME ---
        // 1. Tambahkan SplashPanel di urutan pertama sebagai layar entrance
        mainContainer.add(new SplashPanel(this), "SPLASH_SCREEN");

        // 2. Tambahkan layar menu utama dan gameplay
        mainContainer.add(new MenuPanel(this), "MENU_SCREEN");
        this.gamePanel = new GamePanel(this);
        mainContainer.add(this.gamePanel, "GAME_SCREEN");

        setContentPane(mainContainer);

        // Pastikan saat pertama kali game berjalan, layar SPLASH_SCREEN yang muncul
        showScreen("SPLASH_SCREEN");
    }

    // Fungsi sakti untuk berpindah layar dengan 1 baris kodea
    public void showScreen(String screenName) {
        cardLayout.show(mainContainer, screenName);
    }

    public static void main(String[] args) {
        // Menjalankan game di thread Swing yang aman (Event Dispatch Thread)
        SwingUtilities.invokeLater(() -> {
            GameWindow window = new GameWindow();
            window.setVisible(true);
        });
    }
}