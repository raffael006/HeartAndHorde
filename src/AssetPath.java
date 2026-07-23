import java.io.File;
import java.net.URISyntaxException;

/**
 * Utility untuk menghitung path folder "assets" berdasarkan LOKASI FILE JAR/EXE ITU SENDIRI,
 * bukan berdasarkan "working directory" proses (yang nilainya beda-beda tergantung cara
 * game dijalankan: dari CMD, dari shortcut Desktop, atau dari installer jpackage).
 *
 * Kenapa ini perlu:
 * - new File("assets/img/x.png") mencari folder "assets" relatif ke working directory saat itu.
 * - Saat dites lewat "java -jar" dari folder yang sama dengan folder assets, itu kebetulan cocok.
 * - Tapi setelah di-package jadi .exe lewat jpackage dan dijalankan lewat shortcut Desktop,
 *   working directory-nya BEDA -> path "assets/..." jadi tidak ketemu -> font/gambar/musik
 *   gagal load & program diam-diam jatuh ke tampilan fallback (karena ada try/catch).
 *
 * Dengan AssetPath, path assets SELALU dihitung dari folder tempat file .jar berada,
 * jadi konsisten di semua cara menjalankan game.
 */
public class AssetPath {

    private static final File BASE_DIR = resolveBaseDir();

    private static File resolveBaseDir() {
        try {
            File location = new File(
                    AssetPath.class.getProtectionDomain().getCodeSource().getLocation().toURI()
            );
            // Kalau "location" adalah file .jar -> ambil folder induknya (tempat assets/ berada).
            // Kalau dijalankan dari .class biasa (belum di-jar, misal saat development) -> location
            // itu sendiri sudah berupa folder (misal folder "out"), jadi dipakai langsung.
            return location.isFile() ? location.getParentFile() : location;
        } catch (URISyntaxException | NullPointerException e) {
            // Fallback super aman kalau karena sesuatu hal gagal dideteksi
            return new File(System.getProperty("user.dir"));
        }
    }

    /** Contoh: AssetPath.get("assets/img/Heart.png") -> File absolut ke gambar itu */
    public static File get(String relativePath) {
        return new File(BASE_DIR, relativePath);
    }

    /** Sama seperti get(), tapi langsung berupa String path absolut (dipakai ImageIcon, dll) */
    public static String getPath(String relativePath) {
        return get(relativePath).getAbsolutePath();
    }
}
