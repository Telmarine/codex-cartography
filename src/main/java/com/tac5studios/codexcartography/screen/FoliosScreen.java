package com.tac5studios.codexcartography.screen;

import com.tac5studios.codexcartography.client.Dims;
import com.tac5studios.codexcartography.client.MapSession;
import com.tac5studios.codexcartography.config.Features;
import com.tac5studios.codexcartography.mark.Folio;
import com.tac5studios.codexcartography.mark.Mark;
import com.tac5studios.codexcartography.mark.MarkStore;
import com.tac5studios.codexcartography.mark.WaypointImport;
import com.tac5studios.codexcartography.ui.Codex;
import com.tac5studios.codexcartography.ui.Tex;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Folios: folders of marks. Cards with an on/off toggle, and a drawer to rename, recolor or remove a folio. */
public final class FoliosScreen extends CodexScreen {

    private static final int DRAWER = 180;

    private Folio selected;
    private ColorWheel wheel;
    private EditBox name;
    private boolean confirmRemove;
    private boolean editingColor;
    private Component importResult;
    private ContextMenu importMenu;
    private final List<Placed> placed = new ArrayList<>();

    private record Placed(int x0, int y0, int x1, int y1, Folio folio, boolean toggle) {}

    public FoliosScreen() {
        super(Component.translatable("codex_cartography.section.folios"));
    }

    @Override
    protected Section section() {
        return Section.FOLIOS;
    }

    @Override
    protected void init() {
        name = new EditBox(font, width - DRAWER + 16, 20, DRAWER - 40, 12, Component.translatable("codex_cartography.editor.name"));
        name.setBordered(false);
        name.setTextColor(Codex.TEXT);
        name.setMaxLength(32);
        name.setResponder(v -> {
            if (selected != null && !v.isBlank()) {
                selected.name = v.trim();
                store().changed();
            }
        });
        name.visible = selected != null;
        addRenderableWidget(name);
        wheel = new ColorWheel(font, selected != null ? selected.color : 0x39FF8C);
    }

    private static MarkStore store() {
        return MapSession.get().marks();
    }

    @Override
    protected void renderPage(GuiGraphics g, int mouseX, int mouseY, float partial) {
        if (MapSession.get() == null) return;
        placed.clear();
        MarkStore s = store();
        int x0 = RAIL + 10;
        int right = width - DRAWER - 8;
        int total = s.all().size();
        header(g, x0, 6, 160, Tex.action("folio"), 24, Component.translatable("codex_cartography.section.folios"),
                Component.translatable("codex_cartography.folios.counts", s.folios().size(), total));
        if (Features.WAYPOINT_IMPORT.get()) {
            int bx = right - 140;
            Codex.panel(g, bx, 6, bx + 110, 30);
            Codex.tex(g, Tex.action("import"), bx + 4, 10, 16, 16, 24, 24);
            g.drawString(font, Component.translatable("codex_cartography.folios.import"), bx + 24, 14, Codex.TEXT, false);
            hover(bx, 6, bx + 110, 30, Component.translatable("codex_cartography.folios.import"),
                    importResult != null ? importResult : Component.translatable("codex_cartography.folios.import_hover"));
        }
        glyphButton(g, right - 24, 8, 20, Tex.action("new_mark"), 24, mouseX, mouseY, Component.translatable("codex_cartography.folios.new"));

        int y = 36;
        g.fill(x0, y, right, y + 26, 0xFF121E1A);
        Codex.outline(g, x0, y, right, y + 26, 0xFF1E5A3C);
        g.drawString(font, Component.translatable("codex_cartography.folios.what"), x0 + 6, y + 4, Codex.TEXT, false);
        g.drawString(font, Component.translatable("codex_cartography.folios.what2"), x0 + 6, y + 14, Codex.MUTE, false);

        int top = y + 34;
        int cols = Math.max(1, (right - x0 + 8) / 158);
        int cw = (right - x0 - (cols - 1) * 8) / cols;
        int i = 0;
        for (Folio f : s.folios()) {
            int cx = x0 + (i % cols) * (cw + 8);
            int cy = top + (i / cols) * 70;
            folioCard(g, s, f, cx, cy, cx + cw, cy + 62, mouseX, mouseY);
            i++;
        }
        if (s.folios().isEmpty()) g.drawString(font, Component.translatable("codex_cartography.folios.empty"), x0, top, Codex.MUTE, false);
        drawDrawer(g, s, mouseX, mouseY);
    }

    private void folioCard(GuiGraphics g, MarkStore s, Folio f, int x0, int y0, int x1, int y1, int mouseX, int mouseY) {
        Codex.card(g, x0, y0, x1, y1, f == selected);
        g.fill(x0, y0, x0 + 3, y1, f.shown ? 0xFF000000 | f.color : 0xFF464E54);
        g.drawString(font, f.name, x0 + 10, y0 + 6, f.shown ? Codex.TEXT : Codex.MUTE, false);
        int count = s.countIn(f);
        g.drawString(font, Component.translatable("codex_cartography.folios.marks", count), x0 + 10, y0 + 17, Codex.MUTE, false);
        int tx = x1 - 24;
        MarkEditor.toggle(g, tx, y0 + 6, f.shown, "codex_cartography.empty");
        placed.add(new Placed(tx, y0 + 4, x1, y0 + 18, f, true));
        hover(tx, y0 + 4, x1, y0 + 18, Component.translatable("codex_cartography.folios.toggle"));
        int ex = x0 + 10;
        int shown = 0;
        for (Mark m : s.all()) {
            if (!m.folio.equals(f.id)) continue;
            if (shown < 5) {
                m.drawBadge(g, ex + 9, y0 + 41, 18);
                ex += 22;
            }
            shown++;
        }
        if (shown > 5) g.drawString(font, "+" + (shown - 5), ex + 2, y0 + 37, Codex.MUTE, false);
        if (!f.shown) g.drawString(font, Component.translatable("codex_cartography.folios.hidden"), x0 + 10, y1 - 10, 0xFF96786A, false);
        placed.add(new Placed(x0, y0, x1, y1, f, false));
    }

    private void drawDrawer(GuiGraphics g, MarkStore s, int mouseX, int mouseY) {
        int dx = width - DRAWER;
        g.fill(dx, 0, width, height, 0xFF0C1014);
        g.fill(dx, 0, dx + 1, height, Codex.accent());
        name.visible = selected != null;
        if (selected == null) {
            g.drawString(font, Component.translatable("codex_cartography.folios.pick"), dx + 12, 14, Codex.MUTE, false);
            return;
        }
        Folio f = selected;
        int px = dx + 12;
        int pr = width - 10;
        g.drawString(font, Component.translatable("codex_cartography.editor.name"), px, 8, Codex.MUTE, false);
        g.fill(px, 17, pr, 31, Codex.CARD);
        Codex.outline(g, px, 17, pr, 31, name.isFocused() ? Codex.accent() : Codex.LINE);
        int y = 38;
        g.drawString(font, Component.translatable("codex_cartography.editor.color"), px, y, Codex.MUTE, false);
        if (editingColor) {
            wheel.render(g, px, y + 10, mouseX, mouseY);
            y += 10 + ColorWheel.height() + 12;
            g.drawString(font, Component.translatable("codex_cartography.editor.done"), px, y - 8, Codex.accent(), false);
        } else {
            g.fill(px, y + 10, px + 12, y + 22, 0xFF000000 | f.color);
            g.drawString(font, Codex.hex(f.color), px + 16, y + 12, Codex.TEXT, false);
            g.drawString(font, Component.translatable("codex_cartography.editor.change"), px + 70, y + 12, Codex.accent(), false);
            y += 28;
        }
        g.drawString(font, Component.translatable("codex_cartography.folios.color_changes"), px, y, Codex.MUTE, false);
        radio(g, px, y + 11, !f.colorAll, "codex_cartography.folios.color_folio");
        radio(g, px, y + 23, f.colorAll, "codex_cartography.folios.color_all");
        y += 40;
        MarkEditor.toggle(g, px, y, f.shown, "codex_cartography.folios.shown");
        y += 16;
        g.drawString(font, Component.translatable("codex_cartography.folios.list"), px, y, Codex.MUTE, false);
        y += 10;
        int n = 0;
        for (Mark m : s.all()) {
            if (!m.folio.equals(f.id)) continue;
            if (y > height - 50) {
                n++;
                continue;
            }
            g.fill(px, y, pr, y + 16, Codex.CARD);
            m.drawBadge(g, px + 8, y + 8, 14);
            g.drawString(font, m.name, px + 18, y + 4, Codex.TEXT, false);
            g.drawString(font, Dims.name(m.dim), pr - 4 - font.width(Dims.name(m.dim)), y + 4, Codex.MUTE, false);
            y += 18;
        }
        if (n > 0) g.drawString(font, Component.translatable("codex_cartography.folios.more", n), px, y, Codex.MUTE, false);
        int ay = height - 30;
        g.fill(px, ay - 6, pr, ay - 5, Codex.LINE);
        Codex.tex(g, Tex.action(f.shown ? "hide" : "confirm"), px, ay, 20, 20, 24, 24);
        hover(px, ay, px + 20, ay + 20, Component.translatable(f.shown ? "codex_cartography.folios.hide" : "codex_cartography.folios.show"));
        Codex.tex(g, Tex.action(confirmRemove ? "remove_confirm" : "remove"), px + 26, ay, 20, 20, 24, 24);
        hover(px + 26, ay, px + 46, ay + 20, Component.translatable(confirmRemove ? "codex_cartography.action.remove_confirm" : "codex_cartography.folios.remove"));
        g.drawString(font, Component.translatable("codex_cartography.folios.remove_note"), px + 52, ay + 6, Codex.MUTE, false);
    }

    private void radio(GuiGraphics g, int x, int y, boolean on, String key) {
        Codex.outline(g, x, y, x + 9, y + 9, on ? Codex.accent() : Codex.MUTE);
        if (on) g.fill(x + 2, y + 2, x + 7, y + 7, Codex.accent());
        g.drawString(font, Component.translatable(key), x + 13, y + 1, Codex.TEXT, false);
    }

    private void select(Folio f) {
        selected = f;
        confirmRemove = false;
        editingColor = false;
        name.setValue(f.name);
        wheel.set(f.color);
    }

    /** The list of waypoint folders found on this computer; picking one imports it. */
    private void openImportMenu(int x) {
        List<WaypointImport.Source> found = WaypointImport.sources();
        importMenu = new ContextMenu(Component.translatable("codex_cartography.folios.import"),
                Component.translatable("codex_cartography.folios.import_pick"));
        if (found.isEmpty()) {
            importMenu.add(Component.translatable("codex_cartography.folios.import_none"), Tex.action("import"), false,
                    Component.translatable("codex_cartography.folios.import_none_hover"), null, () -> { });
        }
        for (WaypointImport.Source src : found) {
            String extra = src.here() ? Component.translatable("codex_cartography.folios.import_here").getString() : null;
            importMenu.add(Component.literal(src.mod() + ": " + src.folder()), Tex.action("import"), true, null, extra, () -> {
                int n = WaypointImport.run(src);
                importResult = Component.translatable("codex_cartography.folios.imported", n);
                if (minecraft != null && minecraft.player != null) minecraft.player.displayClientMessage(importResult, true);
            });
        }
        importMenu.place(x, 32, width, height);
    }

    @Override
    protected void renderOverlays(GuiGraphics g, int mouseX, int mouseY, float partial) {
        if (importMenu != null) importMenu.render(g, mouseX, mouseY, (a, b, c, d, t, r) -> hover(a, b, c, d, 0xFF78808A, List.of(t, r)));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (importMenu != null) {
            if (importMenu.contains(mx, my)) {
                if (importMenu.click(mx, my)) importMenu = null;
                return true;
            }
            importMenu = null;
            return true;
        }
        if (super.mouseClicked(mx, my, button)) return true;
        if (button != 0 || MapSession.get() == null) return false;
        MarkStore s = store();
        int right = width - DRAWER - 8;
        if (Features.WAYPOINT_IMPORT.get() && Codex.inside(mx, my, right - 140, 6, right - 30, 30)) {
            openImportMenu(right - 140);
            return true;
        }
        if (Codex.inside(mx, my, right - 24, 8, right - 4, 28)) {
            select(s.addFolio(Component.translatable("codex_cartography.folios.new_name", s.folios().size() + 1).getString(), 0x39FF8C));
            return true;
        }
        if (mx >= width - DRAWER) {
            clickDrawer(s, mx, my);
            return true;
        }
        for (Placed p : placed) {
            if (!Codex.inside(mx, my, p.x0, p.y0, p.x1, p.y1)) continue;
            if (p.toggle) {
                p.folio.shown = !p.folio.shown;
                s.changed();
            } else {
                select(p.folio);
            }
            return true;
        }
        return false;
    }

    private void clickDrawer(MarkStore s, double mx, double my) {
        if (selected == null) return;
        Folio f = selected;
        int px = width - DRAWER + 12;
        int pr = width - 10;
        int y = 38;
        if (editingColor) {
            if (wheel.mouseClicked(mx, my)) {
                f.color = wheel.rgb();
                s.changed();
                return;
            }
            y += 10 + ColorWheel.height() + 12;
            if (Codex.inside(mx, my, px, y - 10, px + 60, y)) {
                editingColor = false;
                ColorWheel.remember(f.color);
            }
        } else {
            if (Codex.inside(mx, my, px + 70, y + 10, pr, y + 22)) {
                editingColor = true;
                return;
            }
            y += 28;
        }
        if (Codex.inside(mx, my, px, y + 11, pr, y + 21)) {
            f.colorAll = false;
            s.changed();
        } else if (Codex.inside(mx, my, px, y + 23, pr, y + 33)) {
            f.colorAll = true;
            s.changed();
        }
        y += 40;
        if (Codex.inside(mx, my, px, y, pr, y + 10)) {
            f.shown = !f.shown;
            s.changed();
        }
        int ay = height - 30;
        if (Codex.inside(mx, my, px, ay, px + 20, ay + 20)) {
            f.shown = !f.shown;
            s.changed();
        } else if (Codex.inside(mx, my, px + 26, ay, px + 46, ay + 20)) {
            if (confirmRemove) {
                s.removeFolio(f);
                selected = null;
            }
            confirmRemove = !confirmRemove && selected != null;
            return;
        }
        confirmRemove = false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (editingColor && selected != null && wheel.mouseDragged(mx, my)) {
            selected.color = wheel.rgb();
            store().changed();
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (wheel != null) wheel.mouseReleased();
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == 256 && importMenu != null) {
            importMenu = null;
            return true;
        }
        if (editingColor && wheel.keyPressed(key, scan, mods)) {
            selected.color = wheel.rgb();
            return true;
        }
        if (wheel != null && wheel.typing()) return true;
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean charTyped(char c, int mods) {
        if (editingColor && wheel.charTyped(c, mods)) {
            selected.color = wheel.rgb();
            store().changed();
            return true;
        }
        return super.charTyped(c, mods);
    }

    @Override
    public void removed() {
        MapSession s = MapSession.get();
        if (s != null) s.marks().save();
    }
}
