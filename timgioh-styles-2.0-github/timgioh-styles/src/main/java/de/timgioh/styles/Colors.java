package de.timgioh.styles;

import java.util.List;

/** Farben der timgioh-Packs (gleiche IDs wie die ZIPs unter tgstyles/<linie>/). */
public final class Colors {
    public record Color(String id, String name, int rgb) {
    }

    /** "weiss" ist die Basis (keine eigene ZIP). */
    public static final List<Color> ALL = List.of(
        new Color("weiss", "Weiß", 0xFFFFFF),
        new Color("rot", "Rot", 0xFF2828),
        new Color("gruen", "Grün", 0x32DC3C),
        new Color("blau", "Blau", 0x285AFF),
        new Color("jade", "Jade", 0x00B46E),
        new Color("hellblau", "Hellblau", 0x7DCDFF),
        new Color("tuerkis", "Türkis", 0x00DECD),
        new Color("gold", "Gold", 0xFFC31E),
        new Color("orange", "Orange", 0xFF7D0F),
        new Color("pink", "Pink", 0xFF46A0),
        new Color("rosa", "Rosa", 0xFFAACD),
        new Color("magenta", "Magenta", 0xE600C8),
        new Color("cyan", "Cyan", 0x00E6FF),
        new Color("mint", "Mint", 0x96FFC8),
        new Color("lime", "Lime", 0xAAFF00),
        new Color("dunkelblau", "Dunkelblau", 0x0A1E96),
        new Color("eisblau", "Eisblau", 0xBEEBFF),
        new Color("braun", "Braun", 0x8C5528),
        new Color("bordeaux", "Bordeaux", 0x820A28),
        new Color("grau", "Grau", 0x969696),
        new Color("silber", "Silber", 0xCDD2DC),
        new Color("gelb", "Gelb", 0xFFF028),
        new Color("lila", "Lila", 0xAA55FF),
        new Color("schwarz", "Schwarz", 0x555555),
        new Color("leichtschwarz", "Leichtes Schwarz", 0x888888),
        new Color("rainbow", "Rainbow", 0xFF55FF));

    public static Color byId(String id) {
        for (Color c : ALL) {
            if (c.id().equals(id)) {
                return c;
            }
        }
        return ALL.get(0);
    }

    private Colors() {
    }
}
