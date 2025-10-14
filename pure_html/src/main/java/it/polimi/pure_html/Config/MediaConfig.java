package it.polimi.pure_html.Config;

import jakarta.servlet.ServletContext;
import java.nio.file.*;

/**
 * Centralizza la configurazione della cartella base in cui salvare/leggere
 * i file multimediali dell’app.
*/
public final class MediaConfig {
    private MediaConfig() {}

    //metodo statico che ritorna il path della cartella base
    public static Path baseDir(ServletContext ctx) {
        String v = System.getProperty("media.base.dir");
        if (v == null || v.isBlank()) v = System.getenv("MEDIA_BASE_DIR");
        if (v == null || v.isBlank()) v = ctx.getInitParameter("media.base.dir");
        if (v == null || v.isBlank()) {
            v = Paths.get(System.getProperty("user.home"), "miawebapp-media").toString();
        }
        Path base = Paths.get(v).toAbsolutePath().normalize();
        try {
            Files.createDirectories(base.resolve("audio"));
            Files.createDirectories(base.resolve("img"));
        } catch (Exception ignored) {}
        return base;
    }
}
