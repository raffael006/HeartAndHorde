/**
 * ADT untuk state ekonomi/resource pemain (Wood, Food, Stone, Steel) beserta
 * kapasitas maksimalnya. Diekstrak dari GamePanel apa adanya (logic tidak diubah).
 *
 * Field sengaja dibiarkan public (bukan getter/setter penuh), mengikuti gaya yang
 * sudah dipakai di Building.java (mis. buildProgress, currentHp) -- supaya titik
 * pakai di GamePanel (cek cost bangunan, tombol cheat dev panel, HUD) tetap bisa
 * baca/tulis langsung tanpa harus bongkar banyak baris sekaligus.
 */
public class ResourceManager {

    public int wood = 0;
    public int food = 0;
    public int stone = 0;
    public int steel = 0;

    // --- FITUR BARU: KAPASITAS MAKSIMAL RESOURCE (buat bar 0/100 di HUD) ---
    public int maxWood = 20;
    public int maxStone = 15;
    public int maxSteel = 15;
    public int maxFood = 15;
    public int maxCivil = 100;
    public int maxGuard = 100;

    private static final int BASE_CAPACITY = 20;
    private static final int CAPACITY_PER_STORAGE = 25;
    private static final int MORNING_YIELD_PER_BUILDING = 5;

    /** Dipanggil dari GamePanel.resetCampaign() -- kosongkan semua total (kapasitas max tidak direset). */
    public void reset() {
        wood = 0;
        stone = 0;
        steel = 0;
        food = 0;
    }

    /**
     * Dipanggil tiap tick dengan jumlah Storage yang sudah jadi -> kapasitas max naik +25
     * per Storage. Kalau kapasitas mengecil (Storage dihancurkan), total yang sudah
     * kepegang ikut dipotong juga supaya tidak ada angka "kelebihan" (mis. 40/15).
     */
    public void updateStorageCapacity(int builtStorageCount) {
        maxWood = BASE_CAPACITY + builtStorageCount * CAPACITY_PER_STORAGE;
        maxStone = BASE_CAPACITY + builtStorageCount * CAPACITY_PER_STORAGE;
        maxSteel = BASE_CAPACITY + builtStorageCount * CAPACITY_PER_STORAGE;
        maxFood = BASE_CAPACITY + builtStorageCount * CAPACITY_PER_STORAGE;

        wood = Math.min(wood, maxWood);
        stone = Math.min(stone, maxStone);
        steel = Math.min(steel, maxSteel);
        food = Math.min(food, maxFood);
    }

    /** Dipanggil tiap pagi -> Farm nambah food, Mine nambah stone & steel, masing-masing +5/bangunan. */
    public void applyMorningIncome(int farmCount, int builtMineCount) {
        food = Math.min(maxFood, food + farmCount * MORNING_YIELD_PER_BUILDING);
        stone = Math.min(maxStone, stone + builtMineCount * MORNING_YIELD_PER_BUILDING);
        steel = Math.min(maxSteel, steel + builtMineCount * MORNING_YIELD_PER_BUILDING);
    }

    /** Dipakai saat pohon selesai ditebang -> +5 wood (dibatasi maxWood). */
    public void addWood(int amount) {
        wood = Math.min(maxWood, wood + amount);
    }

    public boolean canAffordWood(int cost) {
        return wood >= cost;
    }

    public void spendWood(int cost) {
        wood -= cost;
    }

    // --- FITUR BARU: versi Stone, dibutuhkan buat cost bangunan yang pakai stone (mis. Archer Tower) ---
    public boolean canAffordStone(int cost) {
        return stone >= cost;
    }

    public void spendStone(int cost) {
        stone -= cost;
    }
}