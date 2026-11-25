package de.timgioh.styles;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Alle Kategorien und Eintraege (wie im Menue) mit den Texturen, die zu jedem Eintrag gehoeren. */
public final class Catalog {
    /** Eine Gruppe von Dateien: Ordner (zum Auflisten) + Regex fuer den ganzen Pfad (ab assets/minecraft/). */
    public record Spec(String dir, Pattern rx, double[] region) {
        boolean matches(String path) {
            return rx.matcher(path).matches();
        }
    }

    public record Item(String id, String name, String icon, List<Spec> specs, List<Pattern> tgExtra) {
        /** Gibt es Vanilla-Texturen, die sich umfaerben lassen? */
        public boolean hasThemes() {
            return !specs.isEmpty();
        }

        /** Gehoert diese Datei (ohne ".mcmeta") zu dem Eintrag, wenn ein timgioh-/Warden-Pack gewaehlt ist? */
        public boolean claims(String path) {
            for (Spec s : specs) {
                if (s.matches(path)) {
                    return true;
                }
            }
            for (Pattern p : tgExtra) {
                if (p.matcher(path).matches()) {
                    return true;
                }
            }
            return false;
        }
    }

    public record Category(String name, List<Item> items) {
    }

    /** Eine Wahlmoeglichkeit in der Pill. */
    public record Option(String id, String name, int swatch) {
    }

    public static final String VANILLA = "vanilla";
    public static final String FLACH = "flach";
    public static final String TIMGIOH = "timgioh";
    public static final String WARDEN = "warden";

    private static final double[] HEAD = {0, 0, 0.5, 0.5};
    private static final double[] CHEST = {0.25, 0.5, 0.875, 1};
    private static final double[] BOOTS = {0, 0.5, 0.25, 1};

    private static final String MAT = "(wooden|stone|copper|iron|golden|diamond|netherite)";
    private static final String ARM = "(leather|chainmail|copper|iron|golden|diamond|netherite|turtle)";

    private static final List<Category> CATEGORIES = build();

    public static List<Category> categories() {
        return CATEGORIES;
    }

    public static Item item(String id) {
        for (Category c : CATEGORIES) {
            for (Item i : c.items()) {
                if (i.id().equals(id)) {
                    return i;
                }
            }
        }
        return null;
    }

    /** Alle Optionen, die fuer diesen Eintrag moeglich sind (timgioh/Warden nur, wenn dort Dateien dafuer liegen). */
    public static List<Option> options(Item item) {
        List<Option> list = new ArrayList<>();
        list.add(new Option(VANILLA, "Vanilla", 0x9A9A9A));
        if (item.id().equals("obsidian")) {
            list.add(new Option(FLACH, "Flach", 0x202028));
        }
        if (Bundle.covers("timgioh", item)) {
            list.add(new Option(TIMGIOH, "timgioh", 0xFFFFFF));
        }
        if (Bundle.covers("warden", item)) {
            list.add(new Option(WARDEN, "White Warden", 0xD8E4EA));
        }
        if (item.hasThemes()) {
            for (Theme t : Theme.values()) {
                list.add(new Option(t.id, t.label, t.swatch));
            }
        }
        return list;
    }

    // ---------------------------------------------------------------- Aufbau

    private static Spec spec(String dir, String regex) {
        return new Spec(dir, Pattern.compile(regex), null);
    }

    private static Spec spec(String dir, String regex, double[] region) {
        return new Spec(dir, Pattern.compile(regex), region);
    }

    private static List<Pattern> tg(String... rx) {
        List<Pattern> l = new ArrayList<>();
        for (String r : rx) {
            l.add(Pattern.compile(r));
        }
        return l;
    }

    private static Item item(String id, String name, String icon, List<Spec> specs, List<Pattern> tg) {
        return new Item(id, name, icon, specs, tg);
    }

    private static Item item(String id, String name, String icon, List<Spec> specs) {
        return new Item(id, name, icon, specs, List.of());
    }

    private static List<Category> build() {
        List<Category> c = new ArrayList<>();
        String ti = "textures/item";
        String tb = "textures/block";

        c.add(new Category("CRYSTAL PVP", List.of(
            item("end_crystal", "End crystal", "end_crystal", List.of(
                spec(ti, ti + "/end_crystal\\.png"),
                spec("textures/entity/end_crystal", "textures/entity/end_crystal/end_crystal(_beam)?\\.png")),
                tg(tb + "/[a-z_]*amethyst[a-z_]*\\.png", ti + "/amethyst_shard\\.png")),
            item("obsidian", "Obsidian", "obsidian", List.of(
                spec(tb, tb + "/(crying_)?obsidian\\.png"))),
            item("respawn_anchor", "Respawn anchor", "respawn_anchor", List.of(
                spec(tb, tb + "/respawn_anchor_[a-z0-9_]+\\.png"))),
            item("glowstone", "Glowstone", "glowstone", List.of(
                spec(tb, tb + "/glowstone\\.png"))),
            item("tnt_minecart", "TNT minecart", "tnt_minecart", List.of(
                spec(ti, ti + "/tnt_minecart\\.png")))
        )));

        c.add(new Category("MACE", List.of(
            item("mace", "Mace", "mace", List.of(
                spec(ti, ti + "/mace\\.png")),
                tg("models/item/(handheld_)?mace\\.json")),
            item("wind_charge", "Wind charge", "wind_charge", List.of(
                spec(ti, ti + "/wind_charge\\.png"),
                spec("textures/entity", "textures/entity/wind_charge\\.png"))),
            item("breeze_rod", "Breeze rod", "breeze_rod", List.of(
                spec(ti, ti + "/breeze_rod\\.png")))
        )));

        c.add(new Category("WEAPONS & TOOLS", List.of(
            item("swords", "Swords", "iron_sword", List.of(
                spec(ti, ti + "/" + MAT + "_sword\\.png")),
                tg("models/item/[a-z_]+_sword\\.json")),
            item("axes", "Axes", "iron_axe", List.of(
                spec(ti, ti + "/" + MAT + "_axe\\.png"))),
            item("spears", "Spears", "iron_spear", List.of(
                spec(ti, ti + "/" + MAT + "_spear\\.png"))),
            item("pickaxes", "Pickaxes", "iron_pickaxe", List.of(
                spec(ti, ti + "/" + MAT + "_pickaxe\\.png"))),
            item("shovels", "Shovels", "iron_shovel", List.of(
                spec(ti, ti + "/" + MAT + "_shovel\\.png")))
        )));

        String eq = "textures/entity/equipment/humanoid";
        String eql = "textures/entity/equipment/humanoid_leggings";
        c.add(new Category("ARMOUR", List.of(
            item("helmets", "Helmets", "iron_helmet", List.of(
                spec(ti, ti + "/" + ARM + "_helmet(_overlay)?\\.png"),
                spec(eq, eq + "/[a-z_]+\\.png", HEAD))),
            item("chestplates", "Chestplates", "iron_chestplate", List.of(
                spec(ti, ti + "/" + ARM.replace("|turtle", "") + "_chestplate(_overlay)?\\.png"),
                spec(eq, eq + "/[a-z_]+\\.png", CHEST))),
            item("leggings", "Leggings", "iron_leggings", List.of(
                spec(ti, ti + "/" + ARM.replace("|turtle", "") + "_leggings(_overlay)?\\.png"),
                spec(eql, eql + "/[a-z_]+\\.png"))),
            item("boots", "Boots", "iron_boots", List.of(
                spec(ti, ti + "/" + ARM.replace("|turtle", "") + "_boots(_overlay)?\\.png"),
                spec(eq, eq + "/[a-z_]+\\.png", BOOTS))),
            item("elytra", "Elytra", "elytra", List.of(
                spec(ti, ti + "/(broken_)?elytra\\.png"),
                spec("textures/entity/equipment/wings", "textures/entity/equipment/wings/[a-z_]+\\.png"),
                spec("textures/entity", "textures/entity/elytra\\.png")),
                tg("models/item/(broken_)?elytra\\.json")),
            item("shield", "Shield", "shield", List.of(
                spec("textures/entity", "textures/entity/(shield/)?shield_base(_nopattern)?\\.png"),
                spec("textures/entity/shield", "textures/entity/shield/shield_base(_nopattern)?\\.png")),
                tg("models/item/shield(_blocking)?\\.json"))
        )));

        c.add(new Category("RANGED", List.of(
            item("bows", "Bows", "bow", List.of(
                spec(ti, ti + "/bow(_pulling_[0-2])?\\.png"))),
            item("arrows", "Arrows", "arrow", List.of(
                spec(ti, ti + "/(arrow|spectral_arrow|tipped_arrow_base|tipped_arrow_head)\\.png"),
                spec("textures/entity/projectiles", "textures/entity/projectiles/(arrow|spectral_arrow|tipped_arrow)\\.png"))),
            item("crossbow", "Crossbow", "crossbow", List.of(
                spec(ti, ti + "/crossbow(_standby|_pulling_[0-2]|_arrow|_firework)?\\.png"))),
            item("trident", "Trident", "trident", List.of(
                spec(ti, ti + "/trident(_in_hand)?\\.png"),
                spec("textures/entity", "textures/entity/trident(_riptide)?\\.png"),
                spec("textures/entity/projectiles", "textures/entity/projectiles/trident\\.png")))
        )));

        c.add(new Category("HEALING & ITEMS", List.of(
            item("totem", "Totem", "totem_of_undying", List.of(
                spec(ti, ti + "/totem_of_undying\\.png")),
                tg("models/item/totem_of_undying\\.json")),
            item("golden_apple", "Golden apples", "golden_apple", List.of(
                spec(ti, ti + "/golden_apple\\.png"))),
            item("op_apple", "OP apples", "enchanted_golden_apple", List.of(
                spec(ti, ti + "/enchanted_golden_apple\\.png"))),
            item("pearls", "Pearls", "ender_pearl", List.of(
                spec(ti, ti + "/ender_pearl\\.png"))),
            item("potions", "Potions", "potion", List.of(
                spec(ti, ti + "/(potion|splash_potion|lingering_potion|glass_bottle|potion_overlay)\\.png"))),
            item("xp", "XP", "experience_bottle", List.of(
                spec(ti, ti + "/experience_bottle\\.png"),
                spec("textures/entity", "textures/entity/experience_orb\\.png")))
        )));

        c.add(new Category("WORLD", List.of(
            item("cobwebs", "Cobwebs", "cobweb", List.of(
                spec(tb, tb + "/cobweb\\.png")),
                tg("models/block/cobweb\\.json")),
            item("fire", "Fire", "campfire", List.of(
                spec(tb, tb + "/(soul_)?fire_[01]\\.png"))),
            item("tnt", "TNT", "tnt", List.of(
                spec(tb, tb + "/tnt_(side|top|bottom)\\.png")))
        )));

        c.add(new Category("INTERFACE", List.of(
            item("inventory", "Inventory", "chest", List.of(
                spec("textures/gui/container", "textures/gui/container/[a-z_0-9]+\\.png"),
                spec("textures/gui/sprites/container", "textures/gui/sprites/container/.*\\.png"))),
            item("hud", "Hotbar / HUD", "clock", List.of(
                spec("textures/gui/sprites/hud", "textures/gui/sprites/hud/.*\\.png"))),
            item("particles", "Particles", "firework_rocket", List.of(
                spec("textures/particle", "textures/particle/[a-z_0-9]+\\.png"))),
            item("effects", "Effects", "enchanted_book", List.of(
                spec("textures/mob_effect", "textures/mob_effect/[a-z_]+\\.png"),
                spec("textures/misc", "textures/misc/enchanted_[a-z_]*glint[a-z_]*\\.png")))
        )));

        c.add(new Category("EXTRAS", List.of(
            item("sky_light", "Himmel & Licht", "daylight_detector", List.of(),
                tg("optifine/.*", "textures/colormap/.*",
                    "textures/misc/(vignette|pumpkinblur|underwater|shadow|forcefield|unknown_pack|unknown_server)\\.png",
                    "textures/block/hitbox_thin\\.png")),
            item("sounds", "Sounds", "note_block", List.of(), tg("sounds/.*"))
        )));
        return List.copyOf(c);
    }

    private Catalog() {
    }
}
