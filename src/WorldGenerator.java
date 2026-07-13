import java.util.HashMap;
import java.util.List;

/**
 * ADT untuk membangun isi awal dunia (hutan & tambang) saat campaign dimulai/direset.
 * Diekstrak dari GamePanel.generateForest() / generateMines() apa adanya (logic tidak diubah),
 * cuma dipisah supaya GamePanel tidak perlu tahu detail cara pohon & tambang disebar.
 */
public class WorldGenerator {

    private static final int PLOT_SIZE = 1000; // Map 3000x3000 dibagi jadi petakan 1000x1000
    private static final int GRID_COUNT = 3;   // 3x3 petak kavling

    private static final int TREE_WIDTH = 26;
    private static final int TREE_HEIGHT = 47;

    private static final int MINE_COUNT = 8;
    private static final int MINE_MARGIN = 100; // Sisakan batas pinggir map
    private static final int MINE_SPREAD = 2800; // Rentang acak persis seperti logic asli

    private WorldGenerator() {
        // Utility class, tidak perlu diinstansiasi
    }

    /**
     * Menyebar pohon ke seluruh petak kavling map, lalu menyimpannya ke mapPohon
     * (dikelompokkan per petak "gridX,gridY" seperti spatial hash).
     * mapPohon diisi langsung (mutasi), bukan dikembalikan, supaya pemanggil
     * (GamePanel) tidak perlu ganti referensi field yang sudah dipakai di banyak tempat.
     */
    public static void generateForest(HashMap<String, List<Tree>> mapPohon) {
        for (int gridX = 0; gridX < GRID_COUNT; gridX++) {
            for (int gridY = 0; gridY < GRID_COUNT; gridY++) {

                String kunciKavling = gridX + "," + gridY;
                List<Tree> daftarPohon = new java.util.ArrayList<>();

                // Lempar dadu nasib untuk kavling ini (Angka acak 1, 2, atau 3)
                int nasib = (int) (Math.random() * 3) + 1;

                int jumlahPohon = 30;
                if (nasib == 1) jumlahPohon = 100; // Hutan Lebat
                else if (nasib == 3) jumlahPohon = 50; // Pinggiran Hutan
                // Jika nasib == 2, jumlahPohon tetap 30 (nilai default di atas)

                for (int i = 0; i < jumlahPohon; i++) {
                    int randomX = (gridX * PLOT_SIZE) + (int) (Math.random() * 900);
                    int randomY = (gridY * PLOT_SIZE) + (int) (Math.random() * 900);

                    Tree pohonBaru = new Tree(randomX, randomY, TREE_WIDTH, TREE_HEIGHT);
                    daftarPohon.add(pohonBaru);
                }

                mapPohon.put(kunciKavling, daftarPohon);
            }
        }
    }

    /**
     * Menyebar tambang secara acak ke seluruh map (dengan margin pinggir),
     * lalu menambahkannya ke activeMines (mutasi list, bukan return baru).
     */
    public static void generateMines(List<Mine> activeMines) {
        for (int i = 0; i < MINE_COUNT; i++) {
            int randomX = MINE_MARGIN + (int) (Math.random() * MINE_SPREAD);
            int randomY = MINE_MARGIN + (int) (Math.random() * MINE_SPREAD);

            activeMines.add(new Mine(randomX, randomY));
        }
    }
}