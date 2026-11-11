package com.tac5studios.codexcartography.screen;

import com.tac5studios.codexcartography.client.Dims;
import com.tac5studios.codexcartography.client.MapSession;
import com.tac5studios.codexcartography.mark.Emblems;
import com.tac5studios.codexcartography.mark.Folio;
import com.tac5studios.codexcartography.mark.Mark;
import com.tac5studios.codexcartography.mark.MarkStore;
import com.tac5studios.codexcartography.ui.Codex;
import com.tac5studios.codexcartography.ui.Tex;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * The mark editor: the drawer on the right of the world map (the map stays live behind it).
 * Name, emblem grid, color wheel, folio, where it shows, and a preview of how it looks in the world.
 */
public final class MarkEditor implements Drawer {

    private static final int W = 196;
    private static final int CELL = 25;

    private final CodexScreen screen;
    private final Mark mark;
    private final boolean isNew;
    private final EditBox name;
    private final ColorWheel wheel;
    private String emblem;
    private String folio;
    private boolean inWorld;
    private boolean onMinimap;
    private boolean colorTouched;
    private boolean open = true;
    private boolean confirmRemove;
    /** Set when this editor makes a claim mark (sent to Elements: Claims instead of saved here). */
    private com.tac5studios.codexcartography.claims.ClaimsData.Claim claim;
    private int x0;
    private int folioY;
    private int togglesY;
    private int gridY;
    private int buttonsY;

    private MarkEditor(CodexScreen screen, Mark mark, boolean isNew) {
        this.screen = screen;
        this.mark = mark;
        this.isNew = isNew;
        Font font = Minecraft.getInstance().font;
        this.name = new EditBox(font, 0, 0, W - 34, 12, Component.translatable("codex_cartography.editor.name"));
        name.setMaxLength(48);
        name.setBordered(false);
        name.setTextColor(Codex.TEXT);
        name.setValue(mark.name);
        name.setFocused(isNew);
        this.wheel = new ColorWheel(font, mark.color);
        this.emblem = mark.emblem;
        this.folio = mark.folio;
        this.inWorld = mark.inWorld;
        this.onMinimap = mark.onMinimap;
        this.colorTouched = !isNew;
    }

    /** A new mark at a spot (added to the Ledger only when saved). */
    public static MarkEditor create(CodexScreen screen, ResourceLocation dim, int x, int y, int z) {
        MapSession s = MapSession.get();
        int n = s == null ? 1 : s.marks().all().size() + 1;
        Mark m = new Mark(MarkStore.newId(), Component.translatable("codex_cartography.mark.default_name", n).getString(), "flag",
                Emblems.GREEN, dim, x, y, z);
        return new MarkEditor(screen, m, true);
    }

    public static MarkEditor edit(CodexScreen screen, Mark mark) {
        return new MarkEditor(screen, mark, false);
    }

    /** A new claim mark for a claim, shared with its trusted players. */
    public static MarkEditor forClaim(CodexScreen screen, com.tac5studios.codexcartography.claims.ClaimsData.Claim claim, int x, int y, int z) {
        MarkEditor e = create(screen, claim.dim(), x, y, z);
        e.claim = claim;
        e.name.setValue(claim.name());
        return e;
    }

    @Override
    public int width() {
        return W;
    }

    @Override
    public boolean open() {
        return open;
    }

    @Override
    public boolean typing() {
        return name.isFocused() || wheel.typing();
    }

    @Override
    public void render(GuiGraphics g, int x0, int height, int mouseX, int mouseY, float partial) {
        this.x0 = x0;
        Font font = Codex.font();
        g.pose().pushPose();
        g.pose().translate(0, 0, 200);
        g.fill(x0, 0, x0 + W, height, 0xFF0C1014);
        g.fill(x0, 0, x0 + 1, height, Codex.accent());
        int px = x0 + 10;
        int pr = x0 + W - 8;
        boolean fixed = emblem.equals("danger") || mark.kind == Mark.Kind.DEATH;
        Codex.tex(g, Tex.action(isNew ? "new_mark" : "edit"), px, 8, 16, 16, 24, 24);
        g.drawString(font, Component.translatable(claim != null ? "codex_cartography.editor.new_claim_mark"
                : isNew ? "codex_cartography.editor.new" : "codex_cartography.editor.edit"), px + 20, 8, Codex.accent(), false);
        g.drawString(font, Dims.name(mark.dim).copy().append(" · " + mark.x + ", " + mark.y + ", " + mark.z), px + 20, 18, Codex.MUTE, false);

        int y = 34;
        label(g, px, y, "codex_cartography.editor.name");
        g.fill(px, y + 9, pr, y + 22, Codex.CARD);
        Codex.outline(g, px, y + 9, pr, y + 22, name.isFocused() ? Codex.accent() : Codex.LINE);
        name.setX(px + 4);
        name.setY(y + 12);
        name.render(g, mouseX, mouseY, partial);

        y += 28;
        gridY = y + 9;
        label(g, px, y, "codex_cartography.editor.emblem");
        if (mark.kind == Mark.Kind.MARK) {
            for (int i = 0; i < Emblems.GRID.size(); i++) {
                String e = Emblems.GRID.get(i);
                int cx = px + (i % 7) * CELL;
                int cy = gridY + (i / 7) * CELL;
                boolean sel = e.equals(emblem);
                g.fill(cx + 1, cy, cx + CELL - 1, cy + CELL - 2, sel ? Codex.CARD_ON : Codex.CARD);
                Codex.outline(g, cx + 1, cy, cx + CELL - 1, cy + CELL - 2, sel ? 0xFF000000 | wheel.rgb() : Codex.LINE);
                Mark preview = new Mark("", "", e, sel ? wheel.rgb() : Emblems.defaultColor(e), mark.dim, 0, 0, 0);
                preview.drawBadge(g, cx + CELL / 2f, cy + CELL / 2f - 1, CELL - 6);
                screen.hover(cx, cy, cx + CELL, cy + CELL, Component.translatable(Emblems.langKey(e)));
            }
            y = gridY + CELL * 2 + 4;
        } else {
            g.drawString(font, Component.translatable("codex_cartography.editor.fixed_emblem"), px, gridY, Codex.MUTE, false);
            y = gridY + 14;
        }

        int wheelY = y + 9;
        label(g, px, y, "codex_cartography.editor.color");
        if (fixed) {
            g.drawString(font, Component.translatable("codex_cartography.editor.fixed_color"), px, wheelY, Codex.MUTE, false);
            y = wheelY + 14;
        } else {
            wheel.render(g, px, wheelY, mouseX, mouseY);
            y = wheelY + ColorWheel.height() + 12;
        }

        folioY = y;
        label(g, px, y, "codex_cartography.editor.folio");
        g.fill(px, y + 9, px + 90, y + 22, Codex.CARD);
        Codex.outline(g, px, y + 9, px + 90, y + 22, Codex.LINE);
        g.drawString(font, folioName(), px + 4, y + 12, Codex.TEXT, false);
        screen.hover(px, y + 9, px + 90, y + 22, Component.translatable("codex_cartography.editor.folio"),
                Component.translatable("codex_cartography.folios.what"), Component.translatable("codex_cartography.editor.folio_click"));
        togglesY = y + 9;
        toggle(g, px + 98, togglesY, inWorld, "codex_cartography.editor.in_world");
        toggle(g, px + 98, togglesY + 12, onMinimap, "codex_cartography.editor.on_minimap");

        y += 34;
        label(g, px, y, "codex_cartography.editor.preview");
        preview(g, px, y + 9, pr, y + 59);
        if (!fixed && wheel.brightness() < 0.25f) {
            g.drawString(font, Component.translatable("codex_cartography.color.too_dark"), px, y + 61, Codex.YELLOW, false);
        }

        buttonsY = height - 30;
        g.fill(px, buttonsY - 6, pr, buttonsY - 5, Codex.LINE);
        Codex.tex(g, Tex.action("confirm"), px, buttonsY, 20, 20, 24, 24);
        g.drawString(font, Component.translatable("codex_cartography.editor.save"), px + 24, buttonsY + 2, Codex.accent(), false);
        g.drawString(font, Component.translatable("codex_cartography.editor.enter"), px + 24, buttonsY + 11, Codex.MUTE, false);
        if (!isNew) {
            Codex.tex(g, Tex.action(confirmRemove ? "remove_confirm" : "remove"), pr - 20, buttonsY, 20, 20, 24, 24);
            screen.hover(pr - 20, buttonsY, pr, buttonsY + 20, Component.translatable(confirmRemove
                    ? "codex_cartography.action.remove_confirm" : "codex_cartography.action.remove"));
        } else {
            Component esc = Component.translatable("codex_cartography.editor.esc");
            g.drawString(font, esc, pr - font.width(esc), buttonsY + 6, Codex.MUTE, false);
        }
        g.pose().popPose();
    }

    private Component folioName() {
        MapSession s = MapSession.get();
        Folio f = s == null || folio.isEmpty() ? null : s.marks().folio(folio);
        return f == null ? Component.translatable("codex_cartography.folios.none") : Component.literal(f.name);
    }

    private static void label(GuiGraphics g, int x, int y, String key) {
        g.drawString(Codex.font(), Component.translatable(key).getString().toUpperCase(java.util.Locale.ROOT), x, y, Codex.MUTE, false);
    }

    static void toggle(GuiGraphics g, int x, int y, boolean on, String key) {
        g.fill(x, y, x + 16, y + 9, on ? 0xFF1E5A3C : 0xFF283036);
        int k = on ? x + 8 : x + 1;
        g.fill(k, y + 1, k + 7, y + 8, on ? Codex.accent() : Codex.MUTE);
        g.drawString(Codex.font(), Component.translatable(key), x + 20, y + 1, Codex.TEXT, false);
    }

    private void preview(GuiGraphics g, int x0, int y0, int x1, int y1) {
        g.fillGradient(x0, y0, x1, y1, 0xFF121A28, 0xFF1E2830);
        for (int x = x0; x < x1; x += 8) {
            int h = y1 - 10 + (int) (2 * Math.sin(x * 0.2));
            g.fill(x, h, Math.min(x + 8, x1), y1, 0xFF34503A);
        }
        Codex.outline(g, x0, y0, x1, y1, Codex.LINE);
        float cx = (x0 + x1) / 2f;
        float cy = y0 + 24;
        int color = emblem.equals("danger") ? Codex.YELLOW & 0xFFFFFF : wheel.rgb();
        if (mark.kind != Mark.Kind.DEATH) {
            Codex.texAt(g, Tex.GLOW, cx, cy, 44, 0, 64, 0xC0000000 | color);
            Codex.tinted(g, Tex.DRIP, (int) cx - 4, (int) cy + 8, 8, 18, 16, 48, 0xFF000000 | color);
        }
        Mark p = new Mark("", "", emblem, wheel.rgb(), mark.dim, 0, 0, 0);
        p.kind = mark.kind;
        p.drawBadge(g, cx, cy, 22);
        Codex.label(g, Component.literal(name.getValue()), (int) cx, (int) cy - 22, Codex.TEXT);
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) Codex.label(g, Component.literal(WorldMapScreen.distance(mc.player, mark.x, mark.z)), (int) cx, (int) cy + 13, 0xFFC8D2CD);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int px = x0 + 10;
        int pr = x0 + W - 8;
        name.setFocused(Codex.inside(mx, my, px, 43, pr, 56));
        if (name.isFocused()) return name.mouseClicked(mx, my, button);
        if (mark.kind == Mark.Kind.MARK) {
            for (int i = 0; i < Emblems.GRID.size(); i++) {
                int cx = px + (i % 7) * CELL;
                int cy = gridY + (i / 7) * CELL;
                if (Codex.inside(mx, my, cx, cy, cx + CELL, cy + CELL)) {
                    emblem = Emblems.GRID.get(i);
                    if (!colorTouched) wheel.set(Emblems.defaultColor(emblem));
                    return true;
                }
            }
        }
        if (!emblem.equals("danger") && mark.kind != Mark.Kind.DEATH && wheel.mouseClicked(mx, my)) {
            colorTouched = true;
            return true;
        }
        if (Codex.inside(mx, my, px, folioY + 9, px + 90, folioY + 22)) {
            cycleFolio();
            return true;
        }
        if (Codex.inside(mx, my, px + 98, togglesY, pr, togglesY + 10)) {
            inWorld = !inWorld;
            return true;
        }
        if (Codex.inside(mx, my, px + 98, togglesY + 12, pr, togglesY + 22)) {
            onMinimap = !onMinimap;
            return true;
        }
        if (Codex.inside(mx, my, px, buttonsY, px + 80, buttonsY + 20)) {
            save();
            return true;
        }
        if (!isNew && Codex.inside(mx, my, pr - 20, buttonsY, pr, buttonsY + 20)) {
            if (confirmRemove) {
                MapSession s = MapSession.get();
                if (s != null) s.marks().remove(mark);
                open = false;
            } else {
                confirmRemove = true;
            }
            return true;
        }
        confirmRemove = false;
        return true;
    }

    private void cycleFolio() {
        MapSession s = MapSession.get();
        if (s == null) return;
        List<Folio> folios = s.marks().folios();
        if (folios.isEmpty()) {
            folio = "";
            return;
        }
        int idx = -1;
        for (int i = 0; i < folios.size(); i++) if (folios.get(i).id.equals(folio)) idx = i;
        folio = idx + 1 >= folios.size() ? "" : folios.get(idx + 1).id;
    }

    private void save() {
        MapSession s = MapSession.get();
        if (s == null) return;
        if (claim != null) {
            net.minecraft.nbt.CompoundTag t = new net.minecraft.nbt.CompoundTag();
            t.putString("op", "mark_add");
            t.putInt("claim", claim.id());
            t.putString("name", name.getValue().isBlank() ? mark.name : name.getValue().trim());
            t.putString("emblem", emblem);
            t.putInt("color", emblem.equals("danger") ? 0xFFD63C : wheel.rgb());
            t.putInt("x", mark.x);
            t.putInt("y", mark.y);
            t.putInt("z", mark.z);
            com.tac5studios.codexcartography.net.ClientNet.sendClaims(t);
            ColorWheel.remember(wheel.rgb());
            open = false;
            return;
        }
        mark.name = name.getValue().isBlank() ? mark.name : name.getValue().trim();
        mark.emblem = emblem;
        if (!emblem.equals("danger") && mark.kind != Mark.Kind.DEATH) {
            mark.color = wheel.rgb();
            ColorWheel.remember(mark.color);
        }
        mark.folio = folio;
        mark.inWorld = inWorld;
        mark.onMinimap = onMinimap;
        if (isNew) s.marks().add(mark);
        else s.marks().changed();
        s.marks().save();
        open = false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        return wheel.mouseDragged(mx, my);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        wheel.mouseReleased();
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 || keyCode == 335) {
            save();
            return true;
        }
        if (name.isFocused()) return name.keyPressed(keyCode, scanCode, modifiers);
        return wheel.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        if (name.isFocused()) return name.charTyped(c, modifiers);
        return wheel.charTyped(c, modifiers);
    }
}
