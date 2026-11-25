package de.timgioh.styles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourcePackManager;

/** Verbindet die Auswahl mit Minecraft: Pack bauen, aktivieren, Ressourcen neu laden. */
public final class StyleManager {
    public static final String PREFIX = "timgioh_styles_";

    private static Path packDir() {
        return FabricLoader.getInstance().getGameDir().resolve("resourcepacks");
    }

    private static String fileName(String key) {
        return PREFIX + "mix_" + key + ".zip";
    }

    public static void init() {
        Settings.load(FabricLoader.getInstance().getConfigDir().resolve("timgioh_styles.properties"));
    }

    public static String currentKey() {
        return Mixer.key(Settings.selection, Settings.colorId);
    }

    /** Gibt es Aenderungen, die noch nicht uebernommen wurden? */
    public static boolean pending() {
        return !currentKey().equals(Settings.appliedKey);
    }

    /** Loescht alte Pack-Dateien dieser Mod (auch die der alten Einzel-Styles), ausser der aktiven. */
    public static void cleanup() {
        String keep = Settings.appliedKey.isEmpty() ? "" : fileName(Settings.appliedKey);
        Path dir = packDir();
        if (!Files.isDirectory(dir)) {
            return;
        }
        try (Stream<Path> s = Files.list(dir)) {
            s.filter(p -> p.getFileName().toString().startsWith(PREFIX))
                .filter(p -> !p.getFileName().toString().equals(keep))
                .forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException ignored) {
                        // Datei ist gerade in Benutzung: naechster Start
                    }
                });
        } catch (IOException ignored) {
            // ohne Aufraeumen weitermachen
        }
    }

    /** Baut das Pack aus der aktuellen Auswahl, aktiviert es und laedt die Ressourcen neu. */
    public static void apply() {
        MinecraftClient mc = MinecraftClient.getInstance();
        String key = currentKey();
        try {
            if (!key.isEmpty()) {
                build(key);
            }
            ResourcePackManager packs = mc.getResourcePackManager();
            packs.scanPacks();
            List<String> ids = new ArrayList<>(packs.getEnabledIds());
            ids.removeIf(s -> s.startsWith("file/" + PREFIX));
            if (!key.isEmpty()) {
                ids.add("file/" + fileName(key));
            }
            packs.setEnabledProfiles(ids);
            mc.options.refreshResourcePacks(packs);
            Settings.appliedKey = key;
            Settings.save();
            mc.reloadResources();
        } catch (Exception e) {
            TimgiohStylesMod.LOGGER.error("Auswahl konnte nicht aktiviert werden", e);
        }
    }

    private static void build(String key) throws IOException {
        Path dir = packDir();
        Files.createDirectories(dir);
        Path target = dir.resolve(fileName(key));
        if (Files.exists(target) && Files.size(target) > 0) {
            return;
        }
        List<Path> roots = new ArrayList<>();
        for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
            if (mod.getMetadata().getId().equals("minecraft")) {
                roots.addAll(mod.getRootPaths());
            }
        }
        Mixer.Result result = Mixer.compose(Settings.selection, Colors.byId(Settings.colorId), new Mixer.PathSource(roots));
        for (String note : result.notes()) {
            TimgiohStylesMod.LOGGER.info(note);
        }
        Path tmp = dir.resolve(fileName(key) + ".tmp");
        Files.write(tmp, result.zip());
        Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
    }

    private StyleManager() {
    }
}
