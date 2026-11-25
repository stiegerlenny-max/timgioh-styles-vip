package de.timgioh.styles;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/** Gespeicherte Auswahl: pro Eintrag eine Option + die timgioh-Farbe. */
public final class Settings {
    /** Eintrags-ID -> Options-ID (fehlt = Vanilla). */
    public static final Map<String, String> selection = new LinkedHashMap<>();
    public static String colorId = "weiss";
    /** Schluessel der Auswahl, die gerade als Pack aktiv ist. */
    public static String appliedKey = "";

    private static Path file;

    public static String get(String itemId) {
        return selection.getOrDefault(itemId, Catalog.VANILLA);
    }

    public static void set(String itemId, String optionId) {
        if (optionId == null || optionId.equals(Catalog.VANILLA)) {
            selection.remove(itemId);
        } else {
            selection.put(itemId, optionId);
        }
    }

    public static void load(Path path) {
        file = path;
        selection.clear();
        colorId = "weiss";
        appliedKey = "";
        if (!Files.isRegularFile(path)) {
            return;
        }
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(path)) {
            p.load(in);
        } catch (IOException e) {
            return;
        }
        for (String k : p.stringPropertyNames()) {
            if (k.startsWith("item.")) {
                String id = k.substring(5);
                if (Catalog.item(id) != null) {
                    set(id, p.getProperty(k));
                }
            }
        }
        colorId = Colors.byId(p.getProperty("color", "weiss")).id();
        appliedKey = p.getProperty("applied", "");
    }

    public static void save() {
        if (file == null) {
            return;
        }
        Properties p = new Properties();
        for (Map.Entry<String, String> e : selection.entrySet()) {
            p.setProperty("item." + e.getKey(), e.getValue());
        }
        p.setProperty("color", colorId);
        p.setProperty("applied", appliedKey);
        try {
            Files.createDirectories(file.getParent());
            try (OutputStream out = Files.newOutputStream(file)) {
                p.store(out, "timgioh styles");
            }
        } catch (IOException ignored) {
            // Speichern ist nur eine Bequemlichkeit
        }
    }

    private Settings() {
    }
}
