import java.util.Arrays;
import java.util.List;

// ============================================================================
// FITUR BARU: FILE KHUSUS BUAT HARDCODE KONFIGURASI DIFFICULTY & WAVE HORDE
// ============================================================================
// Semua isi wave (jenis Horde, arah datangnya, & jarak waktu antar wave) buat
// Easy / Medium / Hard SENGAJA di-tulis manual satu-satu di sini (BUKAN random
// sama sekali -- tidak ada java.util.Random di file ini), supaya gampang diubah-ubah
// kapan aja tanpa perlu bongkar logic yang ada di GamePanel.
//
// CARA EDIT:
// 1. Tiap difficulty (EASY_WAVES / MEDIUM_WAVES / HARD_WAVES) isinya 5 WaveData,
//    urut dari Wave I sampai Wave V.
// 2. Tiap WaveData = new WaveData(intervalTicks, daftarHorde)
//      - intervalTicks -> lama waktu HITUNG MUNDUR (dalam tick) sebelum wave ini
//        muncul, dihitung sejak wave sebelumnya selesai di-spawn (atau sejak
//        Campaign dimulai untuk Wave I). 60 tick = 1 detik (sama kayak siklus siang-malam).
//      - daftarHorde -> urutan PERSIS Horde yang bakal muncul di wave itu, ditulis
//        pakai shortcut axe(...) / shield(...) / bow(...) + arah datangnya (8 mata angin).
//        Urutan & jumlahnya FIX sesuai yang ditulis di sini, tidak diacak sama sekali.
// 3. Tinggal tambah/hapus/copy-paste baris axe(...)/shield(...)/bow(...) buat ngatur
//    jenis & arah tiap Horde satu-satu, atau ganti angka intervalTicks buat ngatur
//    cepat/lambatnya wave itu datang.
public class DifficultyConfig {

    // --- 8 Arah mata angin, buat nentuin DARI SISI MANA Horde itu muncul di tepi map ---
    // Tiap HordeSpawnEntry milih salah satu arah ini secara eksplisit/manual (bukan random).
    public enum SpawnDirection { NORTH, SOUTH, EAST, WEST, NORTH_EAST, NORTH_WEST, SOUTH_EAST, SOUTH_WEST }

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

    // --- Shortcut penulisan biar list di bawah gak kepanjangan ---
    private static HordeSpawnEntry axe(SpawnDirection dir)    { return new HordeSpawnEntry(Horde.HordeType.AXEMAN, dir); }
    private static HordeSpawnEntry shield(SpawnDirection dir) { return new HordeSpawnEntry(Horde.HordeType.SHIELDBEARER, dir); }
    private static HordeSpawnEntry bow(SpawnDirection dir)    { return new HordeSpawnEntry(Horde.HordeType.BOWMAN, dir); }

    // ==========================================================================
    // EASY -> wave lebih jarang muncul, Horde lebih sedikit & didominasi Axeman (lemah)
    // ==========================================================================
    private static final List<WaveData> EASY_WAVES = Arrays.asList(
            // Wave I - nunggu 90 detik (5400 tick), 4 Horde dari Utara & Selatan
            new WaveData(5400, Arrays.asList(
                    axe(SpawnDirection.NORTH), axe(SpawnDirection.NORTH),
                    axe(SpawnDirection.SOUTH), axe(SpawnDirection.SOUTH)
            )),
            // Wave II - nunggu 80 detik (4800 tick), 6 Horde, mulai ada Shieldbearer dari Timur
            new WaveData(4800, Arrays.asList(
                    axe(SpawnDirection.NORTH), axe(SpawnDirection.NORTH), axe(SpawnDirection.NORTH),
                    axe(SpawnDirection.SOUTH), axe(SpawnDirection.SOUTH),
                    shield(SpawnDirection.EAST)
            )),
            // Wave III - nunggu 70 detik (4200 tick), 8 Horde dari 4 arah
            new WaveData(4200, Arrays.asList(
                    axe(SpawnDirection.NORTH), axe(SpawnDirection.NORTH), axe(SpawnDirection.NORTH),
                    axe(SpawnDirection.SOUTH), axe(SpawnDirection.SOUTH), axe(SpawnDirection.SOUTH),
                    shield(SpawnDirection.EAST), shield(SpawnDirection.WEST)
            )),
            // Wave IV - nunggu 60 detik (3600 tick), 10 Horde, Bowman pertama muncul
            new WaveData(3600, Arrays.asList(
                    axe(SpawnDirection.NORTH), axe(SpawnDirection.NORTH), axe(SpawnDirection.NORTH),
                    axe(SpawnDirection.SOUTH), axe(SpawnDirection.SOUTH), axe(SpawnDirection.SOUTH),
                    shield(SpawnDirection.EAST), shield(SpawnDirection.EAST), shield(SpawnDirection.WEST),
                    bow(SpawnDirection.EAST)
            )),
            // Wave V (TERAKHIR) - nunggu 50 detik (3000 tick), 12 Horde dari segala arah
            new WaveData(3000, Arrays.asList(
                    axe(SpawnDirection.NORTH), axe(SpawnDirection.NORTH), axe(SpawnDirection.NORTH),
                    axe(SpawnDirection.SOUTH), axe(SpawnDirection.SOUTH), axe(SpawnDirection.SOUTH),
                    shield(SpawnDirection.EAST), shield(SpawnDirection.EAST), shield(SpawnDirection.WEST), shield(SpawnDirection.WEST),
                    bow(SpawnDirection.NORTH_EAST), bow(SpawnDirection.SOUTH_WEST)
            ))
    );

    // ==========================================================================
    // MEDIUM -> keseimbangan standar antara ancaman & waktu bersiap
    // ==========================================================================
    private static final List<WaveData> MEDIUM_WAVES = Arrays.asList(
            // Wave I - nunggu 60 detik (3600 tick), 6 Horde
            new WaveData(3600, Arrays.asList(
                    axe(SpawnDirection.NORTH), axe(SpawnDirection.NORTH),
                    axe(SpawnDirection.SOUTH), axe(SpawnDirection.SOUTH),
                    shield(SpawnDirection.EAST), bow(SpawnDirection.WEST)
            )),
            // Wave II - nunggu 53 detik (3180 tick), 9 Horde
            new WaveData(3180, Arrays.asList(
                    axe(SpawnDirection.NORTH), axe(SpawnDirection.NORTH), axe(SpawnDirection.NORTH),
                    axe(SpawnDirection.SOUTH), axe(SpawnDirection.SOUTH),
                    shield(SpawnDirection.EAST), shield(SpawnDirection.WEST),
                    bow(SpawnDirection.EAST), bow(SpawnDirection.WEST)
            )),
            // Wave III - nunggu 46 detik (2760 tick), 12 Horde
            new WaveData(2760, Arrays.asList(
                    axe(SpawnDirection.NORTH), axe(SpawnDirection.NORTH), axe(SpawnDirection.NORTH),
                    axe(SpawnDirection.SOUTH), axe(SpawnDirection.SOUTH), axe(SpawnDirection.SOUTH),
                    shield(SpawnDirection.EAST), shield(SpawnDirection.EAST), shield(SpawnDirection.WEST),
                    bow(SpawnDirection.NORTH_EAST), bow(SpawnDirection.NORTH_WEST), bow(SpawnDirection.SOUTH_EAST)
            )),
            // Wave IV - nunggu 39 detik (2340 tick), 15 Horde
            new WaveData(2340, Arrays.asList(
                    axe(SpawnDirection.NORTH), axe(SpawnDirection.NORTH), axe(SpawnDirection.NORTH), axe(SpawnDirection.NORTH),
                    axe(SpawnDirection.SOUTH), axe(SpawnDirection.SOUTH), axe(SpawnDirection.SOUTH),
                    shield(SpawnDirection.EAST), shield(SpawnDirection.EAST), shield(SpawnDirection.WEST), shield(SpawnDirection.WEST),
                    bow(SpawnDirection.NORTH_EAST), bow(SpawnDirection.NORTH_WEST),
                    bow(SpawnDirection.SOUTH_EAST), bow(SpawnDirection.SOUTH_WEST)
            )),
            // Wave V (TERAKHIR) - nunggu 32 detik (1920 tick), 18 Horde dari segala arah
            new WaveData(1920, Arrays.asList(
                    axe(SpawnDirection.NORTH), axe(SpawnDirection.NORTH), axe(SpawnDirection.NORTH), axe(SpawnDirection.NORTH),
                    axe(SpawnDirection.SOUTH), axe(SpawnDirection.SOUTH), axe(SpawnDirection.SOUTH), axe(SpawnDirection.SOUTH),
                    shield(SpawnDirection.EAST), shield(SpawnDirection.EAST), shield(SpawnDirection.WEST), shield(SpawnDirection.WEST),
                    bow(SpawnDirection.NORTH_EAST), bow(SpawnDirection.NORTH_EAST), bow(SpawnDirection.NORTH_WEST),
                    bow(SpawnDirection.SOUTH_EAST), bow(SpawnDirection.SOUTH_WEST), bow(SpawnDirection.SOUTH_WEST)
            ))
    );

    // ==========================================================================
    // HARD -> wave datang cepat, Horde banyak & didominasi Shieldbearer/Bowman (kuat & mematikan)
    // ==========================================================================
    private static final List<WaveData> HARD_WAVES = Arrays.asList(
            // Wave I - nunggu 40 detik (2400 tick), 8 Horde dari 4 arah sekaligus
            new WaveData(2400, Arrays.asList(
                    axe(SpawnDirection.NORTH), axe(SpawnDirection.SOUTH),
                    shield(SpawnDirection.EAST), shield(SpawnDirection.WEST),
                    bow(SpawnDirection.NORTH_EAST), bow(SpawnDirection.NORTH_WEST),
                    bow(SpawnDirection.SOUTH_EAST), bow(SpawnDirection.SOUTH_WEST)
            )),
            // Wave II - nunggu 34 detik (2040 tick), 12 Horde
            new WaveData(2040, Arrays.asList(
                    axe(SpawnDirection.NORTH), axe(SpawnDirection.NORTH),
                    axe(SpawnDirection.SOUTH), axe(SpawnDirection.SOUTH),
                    shield(SpawnDirection.EAST), shield(SpawnDirection.EAST), shield(SpawnDirection.WEST),
                    bow(SpawnDirection.NORTH_EAST), bow(SpawnDirection.NORTH_WEST),
                    bow(SpawnDirection.SOUTH_EAST), bow(SpawnDirection.SOUTH_WEST), bow(SpawnDirection.EAST)
            )),
            // Wave III - nunggu 28 detik (1680 tick), 16 Horde
            new WaveData(1680, Arrays.asList(
                    axe(SpawnDirection.NORTH), axe(SpawnDirection.NORTH), axe(SpawnDirection.SOUTH), axe(SpawnDirection.SOUTH),
                    shield(SpawnDirection.EAST), shield(SpawnDirection.EAST), shield(SpawnDirection.WEST), shield(SpawnDirection.WEST),
                    bow(SpawnDirection.NORTH_EAST), bow(SpawnDirection.NORTH_EAST), bow(SpawnDirection.NORTH_WEST),
                    bow(SpawnDirection.SOUTH_EAST), bow(SpawnDirection.SOUTH_WEST), bow(SpawnDirection.SOUTH_WEST),
                    bow(SpawnDirection.EAST), bow(SpawnDirection.WEST)
            )),
            // Wave IV - nunggu 22 detik (1320 tick), 20 Horde
            new WaveData(1320, Arrays.asList(
                    axe(SpawnDirection.NORTH), axe(SpawnDirection.NORTH), axe(SpawnDirection.SOUTH), axe(SpawnDirection.SOUTH),
                    shield(SpawnDirection.EAST), shield(SpawnDirection.EAST), shield(SpawnDirection.EAST),
                    shield(SpawnDirection.WEST), shield(SpawnDirection.WEST), shield(SpawnDirection.WEST),
                    bow(SpawnDirection.NORTH_EAST), bow(SpawnDirection.NORTH_EAST), bow(SpawnDirection.NORTH_WEST), bow(SpawnDirection.NORTH_WEST),
                    bow(SpawnDirection.SOUTH_EAST), bow(SpawnDirection.SOUTH_EAST), bow(SpawnDirection.SOUTH_WEST), bow(SpawnDirection.SOUTH_WEST),
                    bow(SpawnDirection.EAST), bow(SpawnDirection.WEST)
            )),
            // Wave V (TERAKHIR) - nunggu 15 detik (900 tick), 24 Horde dari SEMUA arah sekaligus
            new WaveData(900, Arrays.asList(
                    axe(SpawnDirection.NORTH), axe(SpawnDirection.NORTH), axe(SpawnDirection.SOUTH), axe(SpawnDirection.SOUTH),
                    shield(SpawnDirection.EAST), shield(SpawnDirection.EAST), shield(SpawnDirection.EAST),
                    shield(SpawnDirection.WEST), shield(SpawnDirection.WEST), shield(SpawnDirection.WEST),
                    bow(SpawnDirection.NORTH_EAST), bow(SpawnDirection.NORTH_EAST), bow(SpawnDirection.NORTH_EAST),
                    bow(SpawnDirection.NORTH_WEST), bow(SpawnDirection.NORTH_WEST), bow(SpawnDirection.NORTH_WEST),
                    bow(SpawnDirection.SOUTH_EAST), bow(SpawnDirection.SOUTH_EAST), bow(SpawnDirection.SOUTH_EAST),
                    bow(SpawnDirection.SOUTH_WEST), bow(SpawnDirection.SOUTH_WEST), bow(SpawnDirection.SOUTH_WEST),
                    bow(SpawnDirection.EAST), bow(SpawnDirection.WEST)
            ))
    );

    // --- Ambil semua wave (I..V) sesuai difficulty yang dipilih pemain ---
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