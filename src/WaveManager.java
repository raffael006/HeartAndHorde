import java.util.List;
import java.util.HashMap;
import java.util.Map;
import javax.sound.sampled.*;
import java.io.File;

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

    // --- REVISI: sekarang 10 wave (I..X), ngikutin isi DifficultyConfig yang baru ---
    public static final int MAX_WAVE = 10;
    public static final String[] WAVE_ROMAN = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};

    private GameWindow.Difficulty currentDifficulty = GameWindow.Difficulty.MEDIUM;
    private int currentWaveIndex = 0;
    private int waveCountdownTicks = 0;
    private boolean allWavesSpawned = false;
    // --- FITUR BARU: true selagi Horde wave yang barusan di-spawn masih ada yang hidup.
    // Selama ini true, countdown ke wave berikutnya DIBEKUKAN dulu -- baru jalan lagi
    // begitu window.activeHordes kosong (semua Horde wave ini mati/tumpas). ---
    private boolean waveInProgress = false;

    /**
     * Dipanggil dari GamePanel.resetCampaign(difficulty) -- menyalakan ulang sistem wave
     * dari Wave I sesuai difficulty yang dipilih.
     */
    public void reset(GameWindow.Difficulty difficulty) {
        this.currentDifficulty = difficulty;
        this.currentWaveIndex = 0;
        this.allWavesSpawned = false;
        this.waveInProgress = false;
        this.waveCountdownTicks = DifficultyConfig.getWaves(currentDifficulty).get(0).intervalTicks;
    }

    /**
     * Dipanggil tiap tick dari game loop GamePanel. Menghitung mundur, lalu men-spawn
     * wave berikutnya ke window.activeHordes begitu countdown habis. Countdown ke wave
     * SETELAHNYA baru mulai jalan lagi kalau Horde wave yang barusan spawn udah abis semua.
     */
    public void tick(GameWindow window) {
        if (allWavesSpawned) return;

        // --- FITUR BARU: lagi nunggu Horde wave sekarang tumpas dulu -> countdown dibekukan ---
        if (waveInProgress) {
            if (window.activeHordes.isEmpty()) {
                waveInProgress = false; // Horde wave ini abis -> boleh mulai hitung mundur wave berikutnya
                if (currentWaveIndex >= MAX_WAVE) {
                    allWavesSpawned = true;
                } else {
                    waveCountdownTicks = DifficultyConfig.getWaves(currentDifficulty).get(currentWaveIndex).intervalTicks;
                }
            }
            return; // Selama masih ada Horde hidup, jangan kurangi waveCountdownTicks
        }

        if (waveCountdownTicks > 0) {
            waveCountdownTicks--;
        } else {
            spawnWave(currentWaveIndex, window);
            currentWaveIndex++;
            waveInProgress = true; // Tunggu Horde wave ini abis dulu sebelum lanjut ke countdown berikutnya
        }
    }

    // Men-spawn seluruh Horde untuk wave ke-(waveIdx+1) PERSIS sesuai urutan, jenis, & arah
    // yang ditulis di DifficultyConfig, lalu Horde-nya maju menyerbu Heart/pemain seperti biasa.
    private void spawnWave(int waveIdx, GameWindow window) {
        // --- FITUR BARU: bunyiin tanduk perang persis pas wave baru mulai muncul ---
        playSound("Assets/music/wavehornhorde.wav");

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

    // --- FITUR BARU: pemutar SFX pendek (WAV) buat tanduk perang tiap wave mulai. ---
    // Dijalankan di thread terpisah biar (1) game loop utama gak ke-block nunggu file
    // audio-nya kebuka/kelar, dan (2) kalau kepanggil beruntun suaranya boleh overlap,
    // bukan nunggu antre. Gagal muter (file gak ada/format salah) cuma nge-print log,
    // gak sampai bikin game crash.
    private void playSound(String filePath) {
        new Thread(() -> {
            try {
                File file = new File(filePath);
                AudioInputStream audioStream = AudioSystem.getAudioInputStream(file);
                Clip clip = AudioSystem.getClip();
                clip.open(audioStream);
                clip.start();

                // Tutup Clip & stream-nya sendiri begitu selesai muter, biar resource-nya gak numpuk/leak
                clip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) {
                        clip.close();
                        try { audioStream.close(); } catch (Exception ignored) {}
                    }
                });
            } catch (Exception e) {
                System.out.println("Gagal muter suara '" + filePath + "': " + e.getMessage());
            }
        }).start();
    }

    // --- Getter untuk dibaca HUD/UI (GamePanel), state tetap hanya diubah lewat reset()/tick() ---
    public int getCurrentWaveIndex() { return currentWaveIndex; }
    public int getWaveCountdownTicks() { return waveCountdownTicks; }
    public boolean isAllWavesSpawned() { return allWavesSpawned; }
    public GameWindow.Difficulty getCurrentDifficulty() { return currentDifficulty; }
    // --- FITUR BARU: true kalau lagi fase "nunggu Horde wave sekarang abis", bukan fase hitung mundur ---
    public boolean isWaveInProgress() { return waveInProgress; }
}