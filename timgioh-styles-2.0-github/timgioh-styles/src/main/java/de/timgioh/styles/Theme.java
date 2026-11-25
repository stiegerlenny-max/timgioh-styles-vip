package de.timgioh.styles;

/** Die 5 eigenen Looks. Jeder faerbt eine Vanilla-Textur ueber einen Farbverlauf (dunkel -> hell) um. */
public enum Theme {
    BLUT("blut", "Blutrot", 0xE01B1B, 0x160000, 0x6E0808, 0xE01B1B, 0xFF9A88),
    EIS("eis", "Eisblau", 0x63C6FF, 0x03101C, 0x1B5A98, 0x63C6FF, 0xEAFBFF),
    TOXIC("toxic", "Toxic", 0x39FF14, 0x021606, 0x0C6E1A, 0x39FF14, 0xDCFFAA),
    GOLD("gold", "Gold", 0xFFC21F, 0x241500, 0x84560A, 0xFFC21F, 0xFFF4B8),
    SCHATTEN("schatten", "Schatten", 0x8F6FE0, 0x040406, 0x17171F, 0x4B3C78, 0xB49CF0);

    public final String id;
    public final String label;
    /** Farbe fuer das Muster-Quadrat in der Auswahl. */
    public final int swatch;
    private final int[] ramp;

    Theme(String id, String label, int swatch, int... ramp) {
        this.id = id;
        this.label = label;
        this.swatch = swatch;
        this.ramp = ramp;
    }

    public static Theme byId(String id) {
        for (Theme t : values()) {
            if (t.id.equals(id)) {
                return t;
            }
        }
        return null;
    }

    /** t in 0..1 -> RGB (ohne Alpha). */
    public int map(double t) {
        if (t <= 0) {
            return ramp[0];
        }
        if (t >= 1) {
            return ramp[ramp.length - 1];
        }
        double pos = t * (ramp.length - 1);
        int i = (int) pos;
        double f = pos - i;
        int a = ramp[i], b = ramp[i + 1];
        int r = (int) Math.round(((a >> 16) & 255) * (1 - f) + ((b >> 16) & 255) * f);
        int g = (int) Math.round(((a >> 8) & 255) * (1 - f) + ((b >> 8) & 255) * f);
        int bl = (int) Math.round((a & 255) * (1 - f) + (b & 255) * f);
        return r << 16 | g << 8 | bl;
    }

    /**
     * Faerbt eine Textur um (Ergebnis landet in dst). Nur der Bereich (x0,y0)-(x1,y1) wird veraendert (Bruchteile 0..1 der Bildgroesse),
     * die Helligkeitsspanne kommt aus dem ganzen Bild.
     */
    public void recolor(Png src, Png dst, double fx0, double fy0, double fx1, double fy1) {
        int w = src.width, h = src.height;
        int[] hist = new int[256];
        int n = 0;
        for (int p : src.argb) {
            if ((p >>> 24) == 0) {
                continue;
            }
            hist[luma(p)]++;
            n++;
        }
        double lo = 0, hi = 1;
        if (n > 0) {
            int cut = Math.max(0, (int) (n * 0.02));
            int acc = 0, l = 0, hh = 255;
            for (int i = 0; i < 256; i++) {
                acc += hist[i];
                if (acc > cut) {
                    l = i;
                    break;
                }
            }
            acc = 0;
            for (int i = 255; i >= 0; i--) {
                acc += hist[i];
                if (acc > cut) {
                    hh = i;
                    break;
                }
            }
            lo = l / 255.0;
            hi = hh / 255.0;
        }
        boolean flat = hi - lo < 0.06;
        int x0 = (int) Math.round(fx0 * w), x1 = (int) Math.round(fx1 * w);
        int y0 = (int) Math.round(fy0 * h), y1 = (int) Math.round(fy1 * h);
        for (int y = Math.max(0, y0); y < Math.min(h, y1); y++) {
            for (int x = Math.max(0, x0); x < Math.min(w, x1); x++) {
                int p = src.argb[y * w + x];
                int a = p >>> 24;
                if (a == 0) {
                    continue;
                }
                double yy = luma(p) / 255.0;
                double t = flat ? yy : (yy - lo) / (hi - lo);
                dst.argb[y * w + x] = a << 24 | map(t);
            }
        }
    }

    private static int luma(int p) {
        int r = (p >> 16) & 255, g = (p >> 8) & 255, b = p & 255;
        return (int) Math.round(0.299 * r + 0.587 * g + 0.114 * b);
    }
}
