package de.timgioh.styles;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Zugriff auf die mitgelieferten timgioh-/Warden-ZIPs (tgstyles/&lt;linie&gt;/&lt;farbe&gt;.zip + base.zip). */
public final class Bundle {
    private static final String PREFIX = "assets/minecraft/";
    private static final Map<String, Set<String>> NAMES = new HashMap<>();

    /** Alle Pfade (ab assets/minecraft/, ohne ".mcmeta") aller ZIPs dieser Linie. */
    public static synchronized Set<String> names(String line) {
        Set<String> set = NAMES.get(line);
        if (set == null) {
            set = new HashSet<>();
            for (String zip : zipNames(null)) {
                try (InputStream raw = open(line, zip)) {
                    if (raw == null) {
                        continue;
                    }
                    try (ZipInputStream zin = new ZipInputStream(raw)) {
                        ZipEntry e;
                        while ((e = zin.getNextEntry()) != null) {
                            String rel = rel(e);
                            if (rel != null) {
                                set.add(rel);
                            }
                        }
                    }
                } catch (IOException ignored) {
                    // ZIP fehlt oder ist kaputt: ueberspringen
                }
            }
            NAMES.put(line, set);
        }
        return set;
    }

    public static boolean covers(String line, Catalog.Item item) {
        for (String n : names(line)) {
            if (item.claims(n)) {
                return true;
            }
        }
        return false;
    }

    /** Kopiert alle Dateien dieses Eintrags aus Farb-ZIP und base.zip nach files (Farbe hat Vorrang). */
    public static void collect(String line, String colorId, Catalog.Item item, Map<String, byte[]> files) throws IOException {
        List<String> order = new ArrayList<>();
        if (!colorId.equals("weiss")) {
            order.add(colorId);
        }
        order.add("base");
        for (String zip : order) {
            try (InputStream raw = open(line, zip)) {
                if (raw == null) {
                    continue;
                }
                try (ZipInputStream zin = new ZipInputStream(raw)) {
                    ZipEntry e;
                    while ((e = zin.getNextEntry()) != null) {
                        String rel = rel(e);
                        if (rel != null && item.claims(rel)) {
                            files.putIfAbsent(e.getName(), zin.readAllBytes());
                        }
                    }
                }
            }
        }
    }

    private static List<String> zipNames(String unused) {
        List<String> l = new ArrayList<>();
        for (Colors.Color c : Colors.ALL) {
            if (!c.id().equals("weiss")) {
                l.add(c.id());
            }
        }
        l.add("base");
        return l;
    }

    private static InputStream open(String line, String zip) {
        return Bundle.class.getResourceAsStream("/tgstyles/" + line + "/" + zip + ".zip");
    }

    /** Pfad ab assets/minecraft/ ohne ".mcmeta", oder null (Ordner / fremde Datei). */
    private static String rel(ZipEntry e) {
        if (e.isDirectory() || !e.getName().startsWith(PREFIX)) {
            return null;
        }
        String r = e.getName().substring(PREFIX.length());
        return r.endsWith(".mcmeta") ? r.substring(0, r.length() - 7) : r;
    }

    private Bundle() {
    }
}
