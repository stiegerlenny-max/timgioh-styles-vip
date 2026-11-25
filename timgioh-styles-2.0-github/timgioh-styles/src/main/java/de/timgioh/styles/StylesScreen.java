package de.timgioh.styles;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

/** Hauptmenue: pro Eintrag (Obsidian, Crystals, Schwerter ...) ein eigener Look. */
public class StylesScreen extends Screen {
    private static final int HEADER_H = 20;
    private static final int ITEM_H = 24;

    private static final class Row {
        String header;
        int count;
        Catalog.Item item;
        ItemStack icon;
        List<Catalog.Option> options;
    }

    private final Screen parent;
    private final List<Row> rows = new ArrayList<>();
    private int contentH;
    private int scroll;

    private int panelW;
    private int panelX;
    private int panelY;
    private int panelH;
    private int listTop;
    private int listBottom;
    private int footY;

    public StylesScreen(Screen parent) {
        super(Text.literal("timgioh styles"));
        this.parent = parent;
        for (Catalog.Category cat : Catalog.categories()) {
            Row h = new Row();
            h.header = cat.name();
            h.count = cat.items().size();
            rows.add(h);
            contentH += HEADER_H;
            for (Catalog.Item it : cat.items()) {
                Row r = new Row();
                r.item = it;
                r.icon = Ui.icon(it.icon());
                r.options = Catalog.options(it);
                rows.add(r);
                contentH += ITEM_H;
            }
        }
    }

    private void layout() {
        panelW = Math.min(310, this.width - 16);
        panelX = (this.width - panelW) / 2;
        panelY = 8;
        panelH = this.height - 16;
        listTop = panelY + 34;
        footY = panelY + panelH - 52;
        listBottom = footY - 4;
    }

    private int maxScroll() {
        return Math.max(0, contentH - (listBottom - listTop));
    }

    private Catalog.Option current(Row r) {
        String id = Settings.get(r.item.id());
        for (Catalog.Option o : r.options) {
            if (o.id().equals(id)) {
                return o;
            }
        }
        return r.options.get(0);
    }

    // ------------------------------------------------------------------ Aufbau

    @Override
    protected void init() {
        layout();
        scroll = Math.max(0, Math.min(scroll, maxScroll()));

        int y = listTop - scroll;
        for (Row r : rows) {
            int h = r.item == null ? HEADER_H : ITEM_H;
            if (r.item != null) {
                int top = Math.max(y, listTop);
                int bottom = Math.min(y + h, listBottom);
                if (bottom - top >= 4) {
                    final Row row = r;
                    this.addDrawableChild(Ui.hit(panelX + 6, top, panelW - 12, bottom - top, () -> openPicker(row)));
                }
            }
            y += h;
        }

        int bw = (panelW - 30) / 2;
        int x1 = panelX + 10, x2 = panelX + 20 + bw;
        this.addDrawableChild(Ui.hit(x1, footY, bw, 20, this::openColors));
        this.addDrawableChild(Ui.hit(x2, footY, bw, 20, () -> {
            Settings.selection.clear();
            Settings.save();
        }));
        this.addDrawableChild(Ui.hit(x1, footY + 26, bw, 20, () -> {
            if (StyleManager.pending()) {
                StyleManager.apply();
            }
        }));
        this.addDrawableChild(Ui.hit(x2, footY + 26, bw, 20, this::close));
    }

    private void openPicker(Row r) {
        List<PickScreen.Entry> entries = new ArrayList<>();
        int sel = 0;
        String cur = Settings.get(r.item.id());
        for (int i = 0; i < r.options.size(); i++) {
            Catalog.Option o = r.options.get(i);
            int swatch = o.swatch();
            if (o.id().equals(Catalog.FLACH)) {
                swatch = Colors.byId(Settings.colorId).rgb();
            }
            entries.add(new PickScreen.Entry(label(o), swatch));
            if (o.id().equals(cur)) {
                sel = i;
            }
        }
        final Catalog.Item item = r.item;
        final List<Catalog.Option> options = r.options;
        this.client.setScreen(new PickScreen(this, item.name(), r.icon, entries, sel, idx -> {
            Settings.set(item.id(), options.get(idx).id());
            Settings.save();
        }));
    }

    private static String label(Catalog.Option o) {
        if (o.id().equals(Catalog.TIMGIOH)) {
            return "timgioh (Packfarbe)";
        }
        if (o.id().equals(Catalog.WARDEN)) {
            return "White Warden (Packfarbe)";
        }
        if (o.id().equals(Catalog.FLACH)) {
            return "Flach schwarz (Packfarbe)";
        }
        return o.name();
    }

    private void openColors() {
        List<PickScreen.Entry> entries = new ArrayList<>();
        int sel = 0;
        for (int i = 0; i < Colors.ALL.size(); i++) {
            Colors.Color c = Colors.ALL.get(i);
            entries.add(new PickScreen.Entry(c.name(), c.rgb()));
            if (c.id().equals(Settings.colorId)) {
                sel = i;
            }
        }
        this.client.setScreen(new PickScreen(this, "Packfarbe", Ui.icon("white_dye"), entries, sel, idx -> {
            Settings.colorId = Colors.ALL.get(idx).id();
            Settings.save();
        }));
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int old = scroll;
        scroll = (int) Math.max(0, Math.min(maxScroll(), scroll - verticalAmount * 20));
        if (scroll != old) {
            this.clearAndInit();
        }
        return true;
    }

    public void close() {
        this.client.setScreen(parent);
    }

    // ---------------------------------------------------------------- Zeichnen

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        layout();

        Ui.pill(ctx, panelX, panelY, panelW, panelH, 0xD0262020);
        ctx.drawText(this.textRenderer, "timgioh styles", panelX + 12, panelY + 10, Ui.WHITE, true);
        String status;
        int statusColor;
        if (StyleManager.pending()) {
            status = "• Änderungen offen";
            statusColor = Ui.ORANGE;
        } else if (Settings.appliedKey.isEmpty()) {
            status = "alles Vanilla";
            statusColor = Ui.GREY;
        } else {
            status = "• aktiv";
            statusColor = Ui.TEAL;
        }
        ctx.drawText(this.textRenderer, status, panelX + panelW - 12 - this.textRenderer.getWidth(status), panelY + 10, statusColor, false);
        ctx.drawText(this.textRenderer, "Klick auf einen Eintrag wählt den Look", panelX + 12, panelY + 22, Ui.GREY, false);

        ctx.enableScissor(panelX, listTop, panelX + panelW, listBottom);
        int y = listTop - scroll;
        for (Row r : rows) {
            int h = r.item == null ? HEADER_H : ITEM_H;
            if (y + h >= listTop && y <= listBottom) {
                if (r.item == null) {
                    ctx.drawText(this.textRenderer, r.header, panelX + 12, y + 9, Ui.TEAL, false);
                    String n = String.valueOf(r.count);
                    ctx.drawText(this.textRenderer, n, panelX + panelW - 16 - this.textRenderer.getWidth(n), y + 9, Ui.GREY, false);
                } else {
                    drawItemRow(ctx, r, y, mouseX, mouseY);
                }
            }
            y += h;
        }
        ctx.disableScissor();

        if (maxScroll() > 0) {
            int track = listBottom - listTop;
            int thumb = Math.max(14, track * track / contentH);
            int ty = listTop + (track - thumb) * scroll / maxScroll();
            ctx.fill(panelX + panelW - 5, listTop, panelX + panelW - 3, listBottom, 0x30FFFFFF);
            ctx.fill(panelX + panelW - 5, ty, panelX + panelW - 3, ty + thumb, 0xA0FFFFFF);
        }

        int bw = (panelW - 30) / 2;
        int x1 = panelX + 10, x2 = panelX + 20 + bw;
        Colors.Color col = Colors.byId(Settings.colorId);
        button(ctx, x1, footY, bw, "Farbe: " + col.name(), 0x30FFFFFF, Ui.WHITE, mouseX, mouseY);
        ctx.fill(x1 + bw - 14, footY + 6, x1 + bw - 6, footY + 14, 0xFF000000 | col.rgb());
        button(ctx, x2, footY, bw, "Alles Vanilla", 0x30FFFFFF, Ui.WHITE, mouseX, mouseY);
        boolean pend = StyleManager.pending();
        button(ctx, x1, footY + 26, bw, "Übernehmen", pend ? 0x8030E0B0 : 0x18FFFFFF, pend ? Ui.WHITE : Ui.GREY, mouseX, mouseY);
        button(ctx, x2, footY + 26, bw, "Zurück", 0x30FFFFFF, Ui.WHITE, mouseX, mouseY);
    }

    private void button(DrawContext ctx, int x, int y, int w, String label, int bg, int fg, int mouseX, int mouseY) {
        boolean hv = Ui.inside(mouseX, mouseY, x, y, w, 20);
        Ui.pill(ctx, x, y, w, 20, hv ? bg + 0x10000000 : bg);
        ctx.drawText(this.textRenderer, label, x + (w - this.textRenderer.getWidth(label)) / 2, y + 6, fg, false);
    }

    private void drawItemRow(DrawContext ctx, Row r, int y, int mouseX, int mouseY) {
        boolean hover = Ui.inside(mouseX, mouseY, panelX + 6, Math.max(y, listTop), panelW - 12, ITEM_H)
            && mouseY >= listTop && mouseY < listBottom;
        if (hover) {
            Ui.pill(ctx, panelX + 6, y + 1, panelW - 12, ITEM_H - 2, 0x1EFFFFFF);
        }
        ctx.drawItem(r.icon, panelX + 12, y + 4);
        ctx.drawText(this.textRenderer, r.item.name(), panelX + 36, y + 8, Ui.WHITE, true);

        Catalog.Option o = current(r);
        boolean custom = !o.id().equals(Catalog.VANILLA);
        String text = o.name();
        int tw = this.textRenderer.getWidth(text);
        int pw = Math.max(52, tw + (custom ? 24 : 16));
        int px = panelX + panelW - 16 - pw;
        int py = y + 6;
        Ui.pill(ctx, px, py, pw, 13, custom ? 0x6030E0B0 : 0x30FFFFFF);
        int tx = px + (pw - tw) / 2;
        if (custom) {
            int sw = o.id().equals(Catalog.FLACH) ? Colors.byId(Settings.colorId).rgb() : o.swatch();
            ctx.fill(px + 5, py + 4, px + 10, py + 9, 0xFF000000 | sw);
            tx = px + 7 + (pw - 7 - tw) / 2;
        }
        ctx.drawText(this.textRenderer, text, tx, py + 3, custom ? 0xFFC8FFEE : 0xFFBDB6B6, false);
    }
}
