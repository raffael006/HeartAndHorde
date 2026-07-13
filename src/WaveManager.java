import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * ADT untuk sistem wave Horde per difficulty (Easy/Medium/Hard).
 * Diekstrak dari GamePanel apa adanya (logic tidak diubah) -- cuma memindahkan
 * state (currentWaveIndex, waveCountdownTicks, allWavesSpawned, currentDifficulty)
 * dan logic (tick per frame, spawnWave) ke satu tempat, supaya GamePanel tidak perlu
 * tahu detail cara wave dihitung & di-spawn.
 *
 * Semua data (jarak waktu, jenis Horde, jumlah, & arah datangnya) tetap 100% dibaca
 * dari DifficultyConfig.java (TIDAK ADA random) -- edit DifficultyConfig kalau mau
 * ubah isi wave.
 */
public class WaveManager {

    public static final int MAX_WAVE = 5;
    public static final String[] WAVE_ROMAN = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};

    private GameWindow.Difficulty currentDifficulty = GameWindow.Difficulty.MEDIUM;
    private int currentWaveIndex = 0;
    private int waveCountdownTicks = 0;
    private boolean allWavesSpawned = false;

    /**
     * Dipanggil dari GamePanel.resetCampaign(difficulty) -- menyalakan ulang sistem wave
     * dari Wave I sesuai difficulty yang dipilih.
     */
    public void reset(GameWindow.Difficulty difficulty) {
        this.currentDifficulty = difficulty;
        this.currentWaveIndex = 0;
        this.allWavesSpawned = false;
        this.waveCountdownTicks = DifficultyConfig.getWaves(currentDifficulty).get(0).intervalTicks;
    }

    /**
     * Dipanggil tiap tick dari game loop GamePanel. Menghitung mundur, lalu men-spawn
     * wave berikutnya ke window.activeHordes begitu countdown habis.
     */
    public void tick(GameWindow window) {
        if (allWavesSpawned) return;

        if (waveCountdownTicks > 0) {
            waveCountdownTicks--;
        } else {
            spawnWave(currentWaveIndex, window);
            currentWaveIndex++;
            if (currentWaveIndex >= MAX_WAVE) {
                allWavesSpawned = true;
            } else {
                waveCountdownTicks = DifficultyConfig.getWaves(currentDifficulty).get(currentWaveIndex).intervalTicks;
            }
        }
    }

    // Men-spawn seluruh Horde untuk wave ke-(waveIdx+1) PERSIS sesuai urutan, jenis, & arah
    // yang ditulis di DifficultyConfig, lalu Horde-nya maju menyerbu Heart/pemain seperti biasa.
    private void spawnWave(int waveIdx, GameWindow window) {
        List<DifficultyConfig.WaveData> waves = DifficultyConfig.getWaves(currentDifficulty);
        if (waveIdx >= waves.size()) return; // jaga-jaga kalau DifficultyConfig diisi kurang dari MAX_WAVE
        DifficultyConfig.WaveData waveData = waves.get(waveIdx);

        double centerX = 1500, centerY = 1500;
        double spawnRadius = 1350;
        double spreadSpacing = 55; // jarak antar Horde yang datang dari arah sama, biar gak numpuk 1 titik

        // Hitung dulu ada berapa Horde per arah di wave ini, biar bisa disebar simetris
        // (posisi ke-berapa dari arah itu -> BUKAN random, murni berdasar urutan di list).
        Map<DifficultyConfig.SpawnDirection, Integer> dirTotal = new HashMap<>();
        for (DifficultyConfig.HordeSpawnEntry entry : waveData.hordes) {
            dirTotal.merge(entry.direction, 1, Integer::sum);
        }
        Map<DifficultyConfig.SpawnDirection, Integer> dirSeen = new HashMap<>();

        for (DifficultyConfig.HordeSpawnEntry entry : waveData.hordes) {
            double[] dirVec = DifficultyConfig.directionVector(entry.direction);
            int total = dirTotal.get(entry.direction);
            int seen = dirSeen.getOrDefault(entry.direction, 0);
            dirSeen.put(entry.direction, seen + 1);

            // Titik dasar di tepi map sesuai arah yang di-hardcode di DifficultyConfig
            double baseX = centerX + dirVec[0] * spawnRadius;
            double baseY = centerY + dirVec[1] * spawnRadius;

            // Sebar tegak lurus arah datang biar Horde yang searah gak numpuk persis di 1 titik
            // (offset ini dihitung dari urutan list, jadi tetap deterministik/fix, bukan acak)
            double perpX = -dirVec[1];
            double perpY = dirVec[0];
            double offsetIndex = seen - (total - 1) / 2.0;
            double sx = baseX + perpX * offsetIndex * spreadSpacing;
            double sy = baseY + perpY * offsetIndex * spreadSpacing;

            window.activeHordes.add(new Horde(entry.type, sx, sy));
        }

        System.out.println("Wave " + WAVE_ROMAN[waveIdx] + " (" + currentDifficulty + ") spawn: "
                + waveData.hordes.size() + " Horde (hardcoded dari DifficultyConfig, bukan random)");
    }

    // --- Getter untuk dibaca HUD/UI (GamePanel), state tetap hanya diubah lewat reset()/tick() ---
    public int getCurrentWaveIndex() { return currentWaveIndex; }
    public int getWaveCountdownTicks() { return waveCountdownTicks; }
    public boolean isAllWavesSpawned() { return allWavesSpawned; }
    public GameWindow.Difficulty getCurrentDifficulty() { return currentDifficulty; }
}