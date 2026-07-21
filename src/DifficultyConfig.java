import java.util.ArrayList;
import java.util.List;

// ============================================================================
// FITUR BARU: FILE KHUSUS BUAT HARDCODE KONFIGURASI DIFFICULTY & WAVE HORDE
// ============================================================================
// Semua isi wave (jenis Horde, arah datangnya, & jarak waktu antar wave) buat
// Easy / Medium / Hard SENGAJA di-tulis manual di sini (BUKAN random sama sekali
// -- tidak ada java.util.Random di file ini), supaya gampang diubah-ubah kapan
// aja tanpa perlu bongkar logic yang ada di GamePanel/WaveManager.
//
// --- REVISI: sekarang 10 wave (I..X), cuma 2 SISI spawn (bukan 8 arah lagi biar
// horde-nya kerasa kayak GEROMBOLAN nyerang dari 2 sisi, bukan nyebar tipis-tipis),
// dan jenis Horde yang muncul di-unlock bertahap per rentang wave:
//   Wave 1-2  : AXEMAN + BOWMAN
//   Wave 3-4  : + SHIELDBEARER + TWO_AXE
//   Wave 5-7  : + LOG + BEAR
//   Wave 8-10 : + SORCERER (semua jenis lengkap)
//
// CARA EDIT:
// 1. Mau ganti 2 SISI datangnya horde (default NORTH & SOUTH) -> tinggal ganti
//    SIDE_A / SIDE_B di bawah, otomatis kepakai buat semua wave & semua difficulty.
// 2. Mau ganti kapan suatu jenis Horde di-unlock -> edit TIER_1..TIER_4 dan/atau
//    method getTierForWave() (batas index wave-nya).
// 3. Mau ganti berapa lama wave ke-N nunggu / berapa banyak Horde-nya -> tinggal
//    ubah angka di array *_INTERVALS / *_COUNTS (index 0 = Wave I, index 9 = Wave X).
//    Isi Horde per wave OTOMATIS diracik dari TIER + COUNT itu (dibagi rata per
//    jenis yang lagi unlock, lalu dibagi 2 buat SIDE_A/SIDE_B) -- tetap 100%
//    deterministik/fix, tidak ada elemen acak sama sekali.
public class DifficultyConfig {

    // --- Arah mata angin (tetap disediakan 8 kalau suatu saat mau dipakai lagi) ---
    public enum SpawnDirection { NORTH, SOUTH, EAST, WEST, NORTH_EAST, NORTH_WEST, SOUTH_EAST, SOUTH_WEST }

    // --- REVISI: cuma 2 SISI yang dipakai buat spawn wave sekarang. Ganti di sini
    // aja kalau mau pindah sisi (misal ke EAST & WEST), semua wave ikut berubah. ---
    private static final SpawnDirection SIDE_A = SpawnDirection.NORTH;
    private static final SpawnDirection SIDE_B = SpawnDirection.SOUTH;

    // Satu entri = SATU Horde yang bakal spawn: tipe-nya apa & dari arah mana. Nilainya final/fix.
    public static class HordeSpawnEntry {
        public final Horde.HordeType type;
        public final SpawnDirection direction;
        public HordeSpawnEntry(Horde.HordeType type, SpawnDirection direction) {
            this.type = type;
            this.direction = direction;
        }
    }

    // Satu wave = waktu tunggu (tick) + daftar Horde yang muncul (urutan & arah sudah pasti/fix)
    public static class WaveData {
        public final int intervalTicks;
        public final List<HordeSpawnEntry> hordes;
        public WaveData(int intervalTicks, List<HordeSpawnEntry> hordes) {
            this.intervalTicks = intervalTicks;
            this.hordes = hordes;
        }
    }

    // --- REVISI: jenis Horde yang di-unlock bertahap sesuai rentang wave yang lo minta ---
    private static final Horde.HordeType[] TIER_1 = { Horde.HordeType.AXEMAN, Horde.HordeType.BOWMAN };
    private static final Horde.HordeType[] TIER_2 = { Horde.HordeType.AXEMAN, Horde.HordeType.BOWMAN,
            Horde.HordeType.SHIELDBEARER, Horde.HordeType.TWO_AXE };
    private static final Horde.HordeType[] TIER_3 = { Horde.HordeType.AXEMAN, Horde.HordeType.BOWMAN,
            Horde.HordeType.SHIELDBEARER, Horde.HordeType.TWO_AXE, Horde.HordeType.LOG, Horde.HordeType.BEAR };
    private static final Horde.HordeType[] TIER_4 = { Horde.HordeType.AXEMAN, Horde.HordeType.BOWMAN,
            Horde.HordeType.SHIELDBEARER, Horde.HordeType.TWO_AXE, Horde.HordeType.LOG, Horde.HordeType.BEAR,
            Horde.HordeType.SORCERER };

    // waveIdx 0-based: 0-1 = Wave I-II, 2-3 = Wave III-IV, 4-6 = Wave V-VII, 7-9 = Wave VIII-X
    private static Horde.HordeType[] getTierForWave(int waveIdx) {
        if (waveIdx <= 1) return TIER_1;
        if (waveIdx <= 3) return TIER_2;
        if (waveIdx <= 6) return TIER_3;
        return TIER_4;
    }

    // --- REVISI: total Horde per wave (index 0 = Wave I ... index 9 = Wave X) buat tiap difficulty.
    // Gampang diubah -- tinggal ganti angkanya di sini, otomatis kepakai buat racik gerombolannya. ---
    private static final int[] EASY_INTERVALS   = {5400, 5100, 4800, 4500, 4200, 3900, 3600, 3300, 3000, 2700};
    private static final int[] EASY_COUNTS      = {   4,    6,    8,   10,   13,   16,   19,   23,   27,   32};

    private static final int[] MEDIUM_INTERVALS = {3600, 3300, 3000, 2700, 2400, 2100, 1800, 1500, 1200, 1000};
    private static final int[] MEDIUM_COUNTS    = {   6,    9,   13,   17,   22,   27,   33,   39,   46,   54};

    // --- HARD: interval-nya paling singkat & COUNT paling gede -> ini yang bikin "gerombolan banget" ---
    private static final int[] HARD_INTERVALS   = {2400, 2100, 1800, 1500, 1200, 1000,  900,  800,  700,  600};
    private static final int[] HARD_COUNTS      = {  10,   14,   20,   26,   32,   38,   44,   50,   58,   68};

    private static final List<WaveData> EASY_WAVES   = buildDifficultyWaves(EASY_INTERVALS, EASY_COUNTS);
    private static final List<WaveData> MEDIUM_WAVES = buildDifficultyWaves(MEDIUM_INTERVALS, MEDIUM_COUNTS);
    private static final List<WaveData> HARD_WAVES   = buildDifficultyWaves(HARD_INTERVALS, HARD_COUNTS);

    // Rakit 10 WaveData dari array interval & total count di atas. Murni loop atas angka yang
    // udah di-hardcode -- BUKAN random, hasilnya selalu sama tiap kali dijalankan.
    private static List<WaveData> buildDifficultyWaves(int[] intervals, int[] counts) {
        List<WaveData> waves = new ArrayList<>();
        for (int i = 0; i < intervals.length; i++) {
            waves.add(new WaveData(intervals[i], buildWave(counts[i], getTierForWave(i))));
        }
        return waves;
    }

    // Bagi `totalCount` Horde rata ke tiap jenis yang lagi unlock (sisa pembagian ditumpuk ke
    // jenis pertama), lalu tiap jenis dibagi 2 lagi ke SIDE_A & SIDE_B (sisa ganjil ke SIDE_A).
    // Semua pembagian ini deterministik (murni pembagian bilangan bulat), tidak ada Random.
    private static List<HordeSpawnEntry> buildWave(int totalCount, Horde.HordeType[] unlockedTypes) {
        List<HordeSpawnEntry> list = new ArrayList<>();
        int n = unlockedTypes.length;
        int base = totalCount / n;
        int remainder = totalCount % n;

        for (int i = 0; i < n; i++) {
            int countForType = base + (i < remainder ? 1 : 0);
            int sideACount = countForType - (countForType / 2);
            int sideBCount = countForType / 2;

            for (int j = 0; j < sideACount; j++) list.add(new HordeSpawnEntry(unlockedTypes[i], SIDE_A));
            for (int j = 0; j < sideBCount; j++) list.add(new HordeSpawnEntry(unlockedTypes[i], SIDE_B));
        }
        return list;
    }

    // --- Ambil semua wave (I..X) sesuai difficulty yang dipilih pemain ---
    public static List<WaveData> getWaves(GameWindow.Difficulty difficulty) {
        switch (difficulty) {
            case EASY: return EASY_WAVES;
            case HARD: return HARD_WAVES;
            default:   return MEDIUM_WAVES; // MEDIUM
        }
    }

    // --- Konversi arah mata angin -> vektor arah (dx, dy) buat ngitung posisi spawn di tepi map ---
    // Koordinat layar: Y makin ke bawah makin besar, jadi NORTH = dy negatif, SOUTH = dy positif.
    // Nilai-nilai ini FIX/tetap, tidak ada elemen acak sama sekali.
    public static double[] directionVector(SpawnDirection dir) {
        switch (dir) {
            case NORTH:      return new double[]{ 0.0,      -1.0     };
            case SOUTH:      return new double[]{ 0.0,       1.0     };
            case EAST:       return new double[]{ 1.0,       0.0     };
            case WEST:       return new double[]{-1.0,       0.0     };
            case NORTH_EAST: return new double[]{ 0.70711,  -0.70711 };
            case NORTH_WEST: return new double[]{-0.70711,  -0.70711 };
            case SOUTH_EAST: return new double[]{ 0.70711,   0.70711 };
            default:         return new double[]{-0.70711,   0.70711 }; // SOUTH_WEST
        }
    }
}