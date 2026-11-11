package com.tac5studios.codexcartography.screen;

import com.tac5studios.codexcartography.ui.Codex;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/** The world map's right-click drop-down: a header, then rows with a glyph and a name. Greyed rows say why on hover. */
public final class ContextMenu {

    private static final int ROW = 16;
    private static final int W = 150;

    public record Row(Component label, ResourceLocation glyph, boolean enabled, Component reason, String extra, Runnable action) {}

    private final Component title;
    private final Component subtitle;
    private final List<Row> rows = new ArrayList<>();
    private int x;
    private int y;
    private int w = W;

    public ContextMenu(Component title, Component subtitle) {
        this.title = title;
        this.subtitle = subtitle;
    }

    public ContextMenu add(Component label, ResourceLocation glyph, boolean enabled, Component reason, String extra, Runnable action) {
        rows.add(new Row(label, glyph, enabled, reason, extra, action));
        return this;
    }

    /** A divider line. */
    public ContextMenu divider() {
        rows.add(null);
        return this;
    }

    public void place(int mx, int my, int screenW, int screenH) {
        Font font = Codex.font();
        for (Row r : rows) {
            if (r != null) w = Math.max(w, 30 + font.width(r.label()) + (r.extra() != null ? font.width(r.extra()) + 12 : 0));
        }
        int h = height();
        x = mx + 4 + w > screenW ? mx - w - 4 : mx + 4;
        y = Math.max(2, Math.min(my + 4, screenH - h - 2));
    }

    private int height() {
        int h = 26;
        for (Row r : rows) h += r == null ? 5 : ROW;
        return h + 3;
    }

    public void render(GuiGraphics g, int mouseX, int mouseY, ScreenHover hoverSink) {
        Font font = Codex.font();
        int h = height();
        g.pose().pushPose();
        g.pose().translate(0, 0, 300);
        g.fill(x + 3, y + 3, x + w + 3, y + h + 3, 0x70000000);
        g.fill(x, y, x + w, y + h, 0xFF0C1014);
        Codex.outline(g, x, y, x + w, y + h, Codex.accent());
        Codex.brackets(g, x - 2, y - 2, x + w + 2, y + h + 2, 7, Codex.accent());
        g.drawString(font, title, x + 6, y + 5, Codex.accent(), false);
        g.drawString(font, subtitle, x + 6, y + 15, Codex.MUTE, false);
        g.fill(x + 4, y + 25, x + w - 4, y + 26, Codex.LINE);
        int yy = y + 27;
        for (Row r : rows) {
            if (r == null) {
                g.fill(x + 4, yy + 2, x + w - 4, yy + 3, Codex.LINE);
                yy += 5;
                continue;
            }
            boolean over = Codex.inside(mouseX, mouseY, x, yy, x + w, yy + ROW);
            if (over && r.enabled()) {
                g.fill(x + 1, yy, x + w - 1, yy + ROW - 1, Codex.CARD_ON);
                g.fill(x + 1, yy, x + 3, yy + ROW - 1, Codex.accent());
            }
            int col = r.enabled() ? (over ? Codex.accent() : Codex.TEXT) : Codex.GREY;
            Codex.tinted(g, r.glyph(), x + 6, yy + 2, 12, 12, 24, 24, r.enabled() ? 0xFFFFFFFF : 0xFF707880);
            g.drawString(font, r.label(), x + 24, yy + 4, col, false);
            if (r.extra() != null) g.drawString(font, r.extra(), x + w - 6 - font.width(r.extra()), yy + 4, over ? Codex.accent() : Codex.MUTE, false);
            if (!r.enabled() && r.reason() != null) hoverSink.add(x, yy, x + w, yy + ROW, r.label(), r.reason());
            yy += ROW;
        }
        g.pose().popPose();
    }

    public boolean contains(double mx, double my) {
        return Codex.inside(mx, my, x - 2, y - 2, x + w + 2, y + height() + 2);
    }

    /** Runs the row under the mouse. Returns true when a row ran. */
    public boolean click(double mx, double my) {
        int yy = y + 27;
        for (Row r : rows) {
            if (r == null) {
                yy += 5;
                continue;
            }
            if (Codex.inside(mx, my, x, yy, x + w, yy + ROW)) {
                if (r.enabled()) {
                    r.action().run();
                    return true;
                }
                return false;
            }
            yy += ROW;
        }
        return false;
    }

    /** Where greyed rows report their reason (the screen shows it as a hover label). */
    @FunctionalInterface
    public interface ScreenHover {
        void add(int x0, int y0, int x1, int y1, Component title, Component reason);
    }
}
