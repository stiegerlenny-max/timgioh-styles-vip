package de.timgioh.styles;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Baut aus der Auswahl ein Resourcepack-ZIP (nur die gewaehlten Eintraege, Rest bleibt Vanilla). */
public final class Mixer {
    private static final String PREFIX = "assets/minecraft/";

    /** Quelle der Vanilla-Texturen. Pfade immer relativ zu assets/minecraft/, mit "/". */
    public interface Source {
        List<String> list(String dir);

        byte[] read(String path);
    }

    /** Liest direkt aus dem Minecraft-Jar (oder einem Ordner). */
    public static final class PathSource implements Source {
        private final List<Path> roots;

        public PathSource(List<Path> roots) {
            this.roots = roots;
        }

        @Override
        public List<String> list(String dir) {
            List<String> out = new ArrayList<>();
            for (Path root : roots) {
                Path base = root.resolve("assets").resolve("minecraft");
                Path start = base.resolve(dir);
                if (!Files.isDirectory(start)) {
                    continue;
                }
                try (Stream<Path> s = Files.walk(start)) {
                    s.filter(Files::isRegularFile).forEach(p ->
                        out.add(base.relativize(p).toString().replace('\\', '/')));
                } catch (IOException ignored) {
                    // Ordner nicht lesbar: ueberspringen
                }
            }
            return out;
        }

        @Override
        public byte[] read(String path) {
            for (Path root : roots) {
                Path p = root.resolve("assets").resolve("minecraft").resolve(path);
                if (Files.isRegularFile(p)) {
                    try {
                        return Files.readAllBytes(p);
                    } catch (IOException ignored) {
                        // naechste Wurzel probieren
                    }
                }
            }
            return null;
        }
    }

    /** Ergebnis: das ZIP und ein paar Zahlen fuers Log. */
    public record Result(byte[] zip, int files, List<String> notes) {
    }

    private static final class Work {
        final Png pristine;
        final Png cur;

        Work(Png p) {
            pristine = p;
            cur = p.copy();
        }
    }

    /** Eindeutiger Kurzschluessel der Auswahl, "" wenn alles Vanilla ist. */
    public static String key(Map<String, String> sel, String colorId) {
        StringBuilder sb = new StringBuilder();
        boolean usesColor = false;
        for (Catalog.Category c : Catalog.categories()) {
            for (Catalog.Item it : c.items()) {
                String o = sel.getOrDefault(it.id(), Catalog.VANILLA);
                if (o.equals(Catalog.VANILLA)) {
                    continue;
                }
                sb.append(it.id()).append('=').append(o).append(';');
                if (o.equals(Catalog.TIMGIOH) || o.equals(Catalog.WARDEN) || o.equals(Catalog.FLACH)) {
                    usesColor = true;
                }
            }
        }
        if (sb.length() == 0) {
            return "";
        }
        if (usesColor) {
            sb.append("color=").append(colorId);
        }
        try {
            byte[] d = MessageDigest.getInstance("SHA-1").digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                hex.append(String.format("%02x", d[i]));
            }
            return hex.toString();
        } catch (Exception e) {
            return Integer.toHexString(sb.toString().hashCode());
        }
    }

    public static Result compose(Map<String, String> sel, Colors.Color color, Source vanilla) throws IOException {
        Map<String, byte[]> files = new TreeMap<>();
        Map<String, Work> work = new HashMap<>();
        Set<String> failed = new HashSet<>();
        Map<String, List<String>> listings = new HashMap<>();
        List<String> notes = new ArrayList<>();

        for (Catalog.Category cat : Catalog.categories()) {
            for (Catalog.Item item : cat.items()) {
                String opt = sel.getOrDefault(item.id(), Catalog.VANILLA);
                if (opt.equals(Catalog.VANILLA)) {
                    continue;
                }
                if (opt.equals(Catalog.TIMGIOH) || opt.equals(Catalog.WARDEN)) {
                    Bundle.collect(opt, color.id(), item, files);
                } else if (opt.equals(Catalog.FLACH) && item.id().equals("obsidian")) {
                    files.put(PREFIX + "textures/block/obsidian.png", flat(false, color.rgb()).encode());
                    files.put(PREFIX + "textures/block/crying_obsidian.png", flat(true, color.rgb()).encode());
                } else {
                    Theme theme = Theme.byId(opt);
                    if (theme == null) {
                        continue;
                    }
                    int before = work.size();
                    for (Catalog.Spec spec : item.specs()) {
                        List<String> all = listings.computeIfAbsent(spec.dir(), vanilla::list);
                        for (String path : all) {
                            if (!spec.matches(path)) {
                                continue;
                            }
                            Work w = work.get(path);
                            if (w == null && !failed.contains(path)) {
                                byte[] b = vanilla.read(path);
                                Png png = b == null ? null : Png.decode(b);
                                if (png == null) {
                                    failed.add(path);
                                    notes.add("uebersprungen: " + path);
                                } else {
                                    w = new Work(png);
                                    work.put(path, w);
                                }
                            }
                            if (w == null) {
                                continue;
                            }
                            double[] r = spec.region() == null ? new double[] {0, 0, 1, 1} : spec.region();
                            theme.recolor(w.pristine, w.cur, r[0], r[1], r[2], r[3]);
                            byte[] meta = vanilla.read(path + ".mcmeta");
                            if (meta != null) {
                                files.put(PREFIX + path + ".mcmeta", meta);
                            }
                        }
                    }
                    if (work.size() == before) {
                        notes.add("keine Vanilla-Dateien gefunden fuer: " + item.id());
                    }
                }
            }
        }
        for (Map.Entry<String, Work> e : work.entrySet()) {
            files.put(PREFIX + e.getKey(), e.getValue().cur.encode());
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bos)) {
            put(zip, "pack.mcmeta", ("{\"pack\":{\"pack_format\":34,\"min_format\":34,\"max_format\":99,"
                + "\"supported_formats\":{\"min_inclusive\":34,\"max_inclusive\":99},"
                + "\"description\":\"timgioh styles: Mix\"}}").getBytes(StandardCharsets.UTF_8));
            for (Map.Entry<String, byte[]> e : files.entrySet()) {
                put(zip, e.getKey(), e.getValue());
            }
        }
        return new Result(bos.toByteArray(), files.size() + 1, notes);
    }

    private static void put(ZipOutputStream zip, String name, byte[] data) throws IOException {
        ZipEntry ze = new ZipEntry(name);
        ze.setTime(0L);
        zip.putNextEntry(ze);
        zip.write(data);
        zip.closeEntry();
    }

    /** Flacher schwarzer Obsidian mit Rahmen in der gewaehlten Farbe (crying: zusaetzlich "Traenen"). */
    static Png flat(boolean crying, int frame) {
        int n = 16;
        int[] px = new int[n * n];
        int dark = 0xFF08080C, mid = 0xFF16161E, edge = 0xFF000000 | frame;
        int tear = 0xFF000000 | brighten(frame);
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                int d = Math.min(Math.min(x, y), Math.min(n - 1 - x, n - 1 - y));
                px[y * n + x] = d == 0 ? edge : d == 1 ? mid : dark;
            }
        }
        if (crying) {
            int[][] t = {{4, 4}, {4, 5}, {10, 7}, {10, 8}, {10, 9}, {6, 11}, {12, 3}};
            for (int[] p : t) {
                px[p[1] * n + p[0]] = tear;
            }
        }
        return new Png(n, n, px);
    }

    private static int brighten(int rgb) {
        int r = Math.min(255, ((rgb >> 16) & 255) + 40);
        int g = Math.min(255, ((rgb >> 8) & 255) + 40);
        int b = Math.min(255, (rgb & 255) + 40);
        return r << 16 | g << 8 | b;
    }

    private Mixer() {
    }
}
