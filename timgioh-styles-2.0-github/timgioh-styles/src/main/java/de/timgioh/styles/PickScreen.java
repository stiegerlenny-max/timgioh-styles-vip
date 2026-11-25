package de.timgioh.styles;

import java.util.List;
import java.util.function.IntConsumer;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

/** Auswahlliste: eine Option anklicken, dann geht es zurueck zum Menue. */
public class PickScreen extends Screen {
    public record Entry(String name, int swatch) {
    }

    private static final int ROW_H = 22;

    private final Screen parent;
    private final String heading;
    private final ItemStack icon;
    private final List<Entry> entries;
    private final int selected;
    private final IntConsumer onPick;
    private int scroll;

    private int panelW;
    private int panelX;
    private int panelY;
    private int panelH;
    private int listTop;
    private int listBottom;

    public PickScreen(Screen parent, String heading, ItemStack icon, List<Entry> entries, int selected, IntConsumer onPick) {
        super(Text.literal(heading));
        this.parent = parent;
        this.heading = heading;
        this.icon = icon;
        this.entries = entries;
        this.selected = selected;
        this.onPick = onPick;
        this.scroll = Math.max(0, selected * ROW_H - 2 * ROW_H);
    }

    private void layout() {
        panelW = Math.min(230, this.width - 16);
        panelX = (this.width - panelW) / 2;
        int want = 34 + entries.size() * ROW_H + 40;
        panelH = Math.min(want, this.height - 20);
        panelY = (this.height - panelH) / 2;
        listTop = panelY + 34;
        listBottom = panelY + panelH - 36;
    }

    private int maxScroll() {
        return Math.max(0, entries.size() * ROW_H - (listBottom - listTop));
    }

    @Override
    protected void init() {
        layout();
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
        int y = listTop - scroll;
        for (int i = 0; i < entries.size(); i++, y += ROW_H) {
            int top = Math.max(y, listTop);
            int bottom = Math.min(y + ROW_H, listBottom);
            if (bottom - top < 4) {
                continue;
            }
            final int index = i;
            this.addDrawableChild(Ui.hit(panelX + 6, top, panelW - 12, bottom - top, () -> {
                onPick.accept(index);
                this.client.setScreen(parent);
            }));
        }
        this.addDrawableChild(Ui.hit(panelX + panelW / 2 - 50, panelY + panelH - 28, 100, 18, this::close));
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int old = scroll;
        scroll = (int) Math.max(0, Math.min(maxScroll(), scroll - verticalAmount * ROW_H));
        if (scroll != old) {
            this.clearAndInit();
        }
        return true;
    }

    public void close() {
        this.client.setScreen(parent);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        layout();
        Ui.pill(ctx, panelX, panelY, panelW, panelH, 0xE0221D1D);
        ctx.drawItem(icon, panelX + 10, panelY + 9);
        ctx.drawText(this.textRenderer, heading, panelX + 32, panelY + 13, Ui.WHITE, true);
        ctx.drawText(this.textRenderer, entries.size() + " Looks", panelX + panelW - 12 - this.textRenderer.getWidth(entries.size() + " Looks"),
            panelY + 13, Ui.GREY, false);

        ctx.enableScissor(panelX, listTop, panelX + panelW, listBottom);
        int y = listTop - scroll;
        for (int i = 0; i < entries.size(); i++, y += ROW_H) {
            if (y + ROW_H < listTop || y > listBottom) {
                continue;
            }
            boolean hover = Ui.inside(mouseX, mouseY, panelX + 6, Math.max(y, listTop), panelW - 12, ROW_H)
                && mouseY < listBottom;
            boolean sel = i == selected;
            if (sel) {
                Ui.pill(ctx, panelX + 6, y + 1, panelW - 12, ROW_H - 2, 0x4030E0B0);
            } else if (hover) {
                Ui.pill(ctx, panelX + 6, y + 1, panelW - 12, ROW_H - 2, 0x28FFFFFF);
            }
            Entry e = entries.get(i);
            ctx.fill(panelX + 14, y + 5, panelX + 26, y + 17, 0xFF000000 | 0x111111);
            ctx.fill(panelX + 15, y + 6, panelX + 25, y + 16, 0xFF000000 | e.swatch());
            ctx.drawText(this.textRenderer, e.name(), panelX + 34, y + 7, sel ? Ui.TEAL : Ui.WHITE, true);
            if (sel) {
                String mark = "aktiv";
                ctx.drawText(this.textRenderer, mark, panelX + panelW - 16 - this.textRenderer.getWidth(mark), y + 7, Ui.TEAL, false);
            }
        }
        ctx.disableScissor();

        if (maxScroll() > 0) {
            int track = listBottom - listTop;
            int thumb = Math.max(14, track * track / (entries.size() * ROW_H));
            int ty = listTop + (track - thumb) * scroll / maxScroll();
            ctx.fill(panelX + panelW - 5, listTop, panelX + panelW - 3, listBottom, 0x30FFFFFF);
            ctx.fill(panelX + panelW - 5, ty, panelX + panelW - 3, ty + thumb, 0xA0FFFFFF);
        }

        int bx = panelX + panelW / 2 - 50, by = panelY + panelH - 28;
        boolean hv = Ui.inside(mouseX, mouseY, bx, by, 100, 18);
        Ui.pill(ctx, bx, by, 100, 18, hv ? 0x50FFFFFF : 0x30FFFFFF);
        String back = "Zurück";
        ctx.drawText(this.textRenderer, back, bx + 50 - this.textRenderer.getWidth(back) / 2, by + 5, Ui.WHITE, false);
    }
}
