package com.tac5studios.codexcartography.screen;

import com.tac5studios.codexcartography.client.ClientRules;
import com.tac5studios.codexcartography.client.Dims;
import com.tac5studios.codexcartography.client.MapSession;
import com.tac5studios.codexcartography.client.ServerMarkers;
import com.tac5studios.codexcartography.mark.Folio;
import com.tac5studios.codexcartography.mark.Mark;
import com.tac5studios.codexcartography.mark.MarkShare;
import com.tac5studios.codexcartography.net.ClientNet;
import com.tac5studios.codexcartography.ui.Codex;
import com.tac5studios.codexcartography.ui.Tex;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** The Ledger: every mark as a card, grouped by folio, with filters, sorting and a detail drawer. */
public final class LedgerScreen extends CodexScreen {

    private static final int DRAWER = 176;
    private static final int CARD_W = 150;
    private static final int CARD_H = 40;

    private enum Sort { NEAREST, NAME, NEWEST }

    private Mark selected;
    private ResourceLocation dimFilter;
    private String folioFilter;
    private Sort sort = Sort.NEAREST;
    private EditBox search;
    private double scroll;
    private boolean confirmRemove;
    private final List<Chip> chips = new ArrayList<>();
    private final List<Placed> placed = new ArrayList<>();

    private record Chip(int x0, int y0, int x1, int y1, Runnable action) {}

    private record Placed(int x0, int y0, int x1, int y1, Mark mark) {}

    public LedgerScreen() {
        this(null);
    }

    public LedgerScreen(Mark select) {
        super(Component.translatable("codex_cartography.section.ledger"));
        this.selected = select;
    }

    @Override
    protected Section section() {
        return Section.LEDGER;
    }

    @Override
    protected void init() {
        int right = width - DRAWER - 8;
        search = new EditBox(font, right - 104, 11, 96, 12, Component.translatable("codex_cartography.ledger.search"));
        search.setBordered(false);
        search.setTextColor(Codex.TEXT);
        search.setHint(Component.translatable("codex_cartography.ledger.search").withColor(Codex.GREY));
        addRenderableWidget(search);
    }

    private List<Mark> marks() {
        MapSession s = MapSession.get();
        if (s == null) return List.of();
        String q = search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
        List<Mark> out = new ArrayList<>();
        for (Mark m : s.marks().ledger()) {
            if (dimFilter != null && !m.dim.equals(dimFilter)) continue;
            if (folioFilter != null && !m.folio.equals(folioFilter)) continue;
            if (!q.isEmpty() && !m.name.toLowerCase(Locale.ROOT).contains(q)) continue;
            out.add(m);
        }
        // Elements: Nexus homes and warps, read-only, in their own group at the end
        if (folioFilter == null) {
            for (ServerMarkers.Marker sm : ServerMarkers.homesAndWarps()) {
                if (dimFilter != null && !sm.dim().equals(dimFilter)) continue;
                if (!q.isEmpty() && !sm.name().toLowerCase(Locale.ROOT).contains(q)) continue;
                Mark m = new Mark("server:" + sm.kind() + ":" + sm.dim() + ":" + sm.name(), sm.name(), sm.kind(), sm.color(),
                        sm.dim(), sm.x(), sm.y(), sm.z());
                m.kind = Mark.Kind.SERVER;
                m.folio = SERVER_GROUP;
                out.add(m);
            }
        }
        Comparator<Mark> c = switch (sort) {
            case NAME -> Comparator.comparing(m -> m.name.toLowerCase(Locale.ROOT));
            case NEWEST -> Comparator.comparingLong((Mark m) -> m.created).reversed();
            case NEAREST -> Comparator.comparingDouble(LedgerScreen::distanceSq);
        };
        out.sort(Comparator.comparing((Mark m) -> folioName(m.folio)).thenComparing(c));
        return out;
    }

    /** The group id homes and warps are listed under (never a real folio). */
    private static final String SERVER_GROUP = "\u0001server";

    private static String folioName(String id) {
        if (id.equals(SERVER_GROUP)) return "\uffff\uffff";
        MapSession s = MapSession.get();
        Folio f = s == null || id.isEmpty() ? null : s.marks().folio(id);
        return f == null ? "￿" : f.name.toLowerCase(Locale.ROOT);
    }

    private static double distanceSq(Mark m) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return 0;
        double d = mc.player.distanceToSqr(m.x, mc.player.getY(), m.z);
        return m.dim.equals(mc.level.dimension().location()) ? d : 1e18 + d;
    }

    @Override
    protected void renderPage(GuiGraphics g, int mouseX, int mouseY, float partial) {
        MapSession s = MapSession.get();
        if (s == null) return;
        chips.clear();
        placed.clear();
        int x0 = RAIL + 10;
        int right = width - DRAWER - 8;
        header(g, x0, 6, 150, Tex.action("ledger"), 24, Component.translatable("codex_cartography.section.ledger"),
                Component.translatable("codex_cartography.ledger.counts", s.marks().all().size(), s.marks().folios().size()));
        Codex.panel(g, right - 124, 6, right, 30);
        Codex.tex(g, Tex.action("search"), right - 120, 10, 14, 14, 24, 24);

        int y = 36;
        int x = x0;
        x = chip(g, x, y, Component.translatable("codex_cartography.ledger.all"), dimFilter == null && folioFilter == null, mouseX, mouseY, () -> {
            dimFilter = null;
            folioFilter = null;
        });
        Set<ResourceLocation> dims = new LinkedHashSet<>();
        for (Mark m : s.marks().ledger()) dims.add(m.dim);
        for (ResourceLocation d : dims) {
            x = chip(g, x, y, Dims.name(d), d.equals(dimFilter), mouseX, mouseY, () -> dimFilter = d.equals(dimFilter) ? null : d);
        }
        for (Folio f : s.marks().folios()) {
            if (x > right - 80) break;
            x = chip(g, x, y, Component.literal(f.name), f.id.equals(folioFilter), mouseX, mouseY,
                    () -> folioFilter = f.id.equals(folioFilter) ? null : f.id);
        }
        Component sortLabel = Component.translatable("codex_cartography.ledger.sort." + sort.name().toLowerCase(Locale.ROOT));
        int sw = font.width(sortLabel) + 8;
        g.drawString(font, sortLabel, right - sw, y + 3, Codex.MUTE, false);
        chips.add(new Chip(right - sw, y, right, y + 14, () -> sort = Sort.values()[(sort.ordinal() + 1) % Sort.values().length]));

        int top = y + 20;
        g.enableScissor(x0 - 4, top - 2, right + 4, height - 4);
        int cols = Math.max(1, (right - x0 + 8) / (CARD_W + 8));
        int cw = (right - x0 - (cols - 1) * 8) / cols;
        int cy = top - (int) scroll;
        int col = 0;
        String lastFolio = null;
        for (Mark m : marks()) {
            if (!m.folio.equals(lastFolio)) {
                if (col != 0) {
                    cy += CARD_H + 6;
                    col = 0;
                }
                lastFolio = m.folio;
                Folio f = s.marks().folio(m.folio);
                Component group = m.folio.equals(SERVER_GROUP) ? Component.translatable("codex_cartography.ledger.server_group")
                        : f == null ? Component.translatable("codex_cartography.folios.none") : Component.literal(f.name);
                g.drawString(font, group.getString().toUpperCase(Locale.ROOT), x0, cy, f == null ? Codex.MUTE : 0xFF000000 | f.color, false);
                cy += 11;
            }
            int cx = x0 + col * (cw + 8);
            card(g, m, cx, cy, cx + cw, cy + CARD_H, mouseX, mouseY);
            placed.add(new Placed(cx, cy, cx + cw, cy + CARD_H, m));
            if (++col >= cols) {
                col = 0;
                cy += CARD_H + 6;
            }
        }
        g.disableScissor();
        drawDrawer(g, mouseX, mouseY);
    }

    private int chip(GuiGraphics g, int x, int y, Component label, boolean on, int mouseX, int mouseY, Runnable action) {
        int w = font.width(label) + 10;
        g.fill(x, y, x + w, y + 14, on ? Codex.CARD_ON : Codex.CARD);
        Codex.outline(g, x, y, x + w, y + 14, on ? Codex.accent() : Codex.LINE);
        g.drawString(font, label, x + 5, y + 3, on ? Codex.accent() : Codex.TEXT, false);
        chips.add(new Chip(x, y, x + w, y + 14, action));
        return x + w + 4;
    }

    private void card(GuiGraphics g, Mark m, int x0, int y0, int x1, int y1, int mouseX, int mouseY) {
        Codex.card(g, x0, y0, x1, y1, selected != null && m.id.equals(selected.id));
        m.drawBadge(g, x0 + 18, y0 + 20, 26);
        int tx = x0 + 36;
        g.drawString(font, m.name, tx, y0 + 5, m.hidden ? Codex.MUTE : Codex.TEXT, false);
        LocalPlayer p = Minecraft.getInstance().player;
        Minecraft mc = Minecraft.getInstance();
        String dist = p != null && mc.level != null && m.dim.equals(mc.level.dimension().location()) ? WorldMapScreen.distance(p, m.x, m.z) : "—";
        g.drawString(font, dist, tx, y0 + 16, Codex.accent(), false);
        Codex.tex(g, Tex.dim(Dims.kind(m.dim)), tx + font.width(dist) + 4, y0 + 15, 10, 10, 32, 32);
        g.drawString(font, m.x + ", " + m.y + ", " + m.z, tx, y0 + 28, 0xFF64707A, false);
        if (m.kind == Mark.Kind.DISCOVERED) {
            hover(x0, y0, x1, y1, Component.literal(m.name), Component.translatable("codex_cartography.mark.discovered_hover"));
        } else if (m.kind == Mark.Kind.CLAIM) {
            hover(x0, y0, x1, y1, Component.literal(m.name), Component.translatable("codex_cartography.mark.claim_hover"));
        } else if (m.kind == Mark.Kind.DEATH) {
            hover(x0, y0, x1, y1, Component.literal(m.name), Component.translatable("codex_cartography.mark.death_hover"));
        } else if (m.kind == Mark.Kind.SERVER) {
            hover(x0, y0, x1, y1, Component.literal(m.name), Component.translatable("codex_cartography.mark.server_hover." + m.emblem));
        }
    }

    private void drawDrawer(GuiGraphics g, int mouseX, int mouseY) {
        int dx = width - DRAWER;
        g.fill(dx, 0, width, height, 0xFF0C1014);
        g.fill(dx, 0, dx + 1, height, Codex.accent());
        if (selected == null) {
            g.drawString(font, Component.translatable("codex_cartography.ledger.pick"), dx + 12, 14, Codex.MUTE, false);
            return;
        }
        Mark m = selected;
        int px = dx + 12;
        int pr = width - 10;
        m.drawBadge(g, px + 18, 28, 36);
        g.drawString(font, m.name, px + 42, 18, Codex.TEXT, false);
        Folio f = MapSession.get() == null || m.folio.isEmpty() ? null : MapSession.get().marks().folio(m.folio);
        Component group = m.kind == Mark.Kind.SERVER ? Component.translatable("codex_cartography.mark.server_hover." + m.emblem)
                : f == null ? Component.translatable("codex_cartography.folios.none") : Component.literal(f.name);
        g.drawString(font, group, px + 42, 30, Codex.MUTE, false);
        g.fill(px, 52, pr, 53, Codex.LINE);
        int y = 60;
        g.drawString(font, Component.translatable("codex_cartography.ledger.coordinates"), px, y, Codex.MUTE, false);
        g.drawString(font, m.x + ", " + m.y + ", " + m.z, px, y + 10, Codex.TEXT, false);
        glyphButton(g, pr - 16, y + 4, 16, Tex.action("copy"), 24, mouseX, mouseY, Component.translatable("codex_cartography.action.copy"));
        y += 28;
        g.drawString(font, Component.translatable("codex_cartography.ledger.dimension"), px, y, Codex.MUTE, false);
        Codex.tex(g, Tex.dim(Dims.kind(m.dim)), px, y + 10, 12, 12, 32, 32);
        g.drawString(font, Dims.name(m.dim), px + 16, y + 12, Codex.TEXT, false);
        y += 28;
        if (!m.fixedLook() && m.kind != Mark.Kind.SERVER) {
            g.drawString(font, Component.translatable("codex_cartography.editor.color"), px, y, Codex.MUTE, false);
            g.fill(px, y + 10, px + 12, y + 22, 0xFF000000 | m.drawColor());
            g.drawString(font, Codex.hex(m.drawColor()), px + 16, y + 12, Codex.TEXT, false);
            y += 28;
        }
        if (m.kind != Mark.Kind.SERVER) {
            MarkEditor.toggle(g, px, y, m.inWorld, "codex_cartography.editor.in_world");
            MarkEditor.toggle(g, px, y + 13, m.onMinimap, "codex_cartography.editor.on_minimap");
            MarkEditor.toggle(g, px, y + 26, m.showDistance, "codex_cartography.editor.show_distance");
        }

        int ay = height - 32;
        g.fill(px, ay - 8, pr, ay - 7, Codex.LINE);
        int ax = px;
        for (String a : ACTIONS) {
            boolean enabled = enabled(m, a);
            String tex = a.equals("remove") && confirmRemove ? "remove_confirm" : a;
            Codex.tinted(g, Tex.action(tex), ax, ay, 20, 20, 24, 24, enabled ? 0xFFFFFFFF : 0xFF606870);
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("codex_cartography.action." + (a.equals("remove") && confirmRemove ? "remove_confirm" : a)));
            if (!enabled) lines.add(Component.translatable(m.kind == Mark.Kind.SERVER ? "codex_cartography.action.server_off"
                    : "codex_cartography.action." + a + ".off"));
            hover(ax, ay, ax + 20, ay + 20, Codex.accent(), lines);
            ax += 26;
        }
    }

    private static final List<String> ACTIONS = List.of("travel", "share", "edit", "hide", "remove");

    private static boolean enabled(Mark m, String a) {
        Minecraft mc = Minecraft.getInstance();
        return switch (a) {
            case "travel" -> ClientRules.travel && ClientRules.canTravel && mc.level != null && m.dim.equals(mc.level.dimension().location());
            case "share" -> ClientRules.markSharing && m.kind != Mark.Kind.SERVER;
            case "edit", "remove" -> m.kind != Mark.Kind.CLAIM && m.kind != Mark.Kind.SERVER;
            case "hide" -> m.kind != Mark.Kind.SERVER;
            default -> true;
        };
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        if (button != 0) return false;
        for (Chip c : chips) {
            if (Codex.inside(mx, my, c.x0, c.y0, c.x1, c.y1)) {
                c.action.run();
                scroll = 0;
                return true;
            }
        }
        int dx = width - DRAWER;
        if (mx >= dx) {
            clickDrawer(mx, my);
            return true;
        }
        for (Placed p : placed) {
            if (Codex.inside(mx, my, p.x0, p.y0, p.x1, p.y1)) {
                selected = p.mark;
                confirmRemove = false;
                return true;
            }
        }
        return false;
    }

    private void clickDrawer(double mx, double my) {
        if (selected == null) return;
        MapSession s = MapSession.get();
        if (s == null) return;
        Mark m = selected;
        int px = width - DRAWER + 12;
        int pr = width - 10;
        if (Codex.inside(mx, my, pr - 16, 64, pr, 80)) {
            Minecraft.getInstance().keyboardHandler.setClipboard(m.x + " " + m.y + " " + m.z);
            return;
        }
        int y = 60 + 28 + 28 + (m.fixedLook() || m.kind == Mark.Kind.SERVER ? 0 : 28);
        if (m.kind != Mark.Kind.SERVER) { // server homes and warps have no toggles
            if (Codex.inside(mx, my, px, y, pr, y + 10)) {
                m.inWorld = !m.inWorld;
                s.marks().changed();
            } else if (Codex.inside(mx, my, px, y + 13, pr, y + 23)) {
                m.onMinimap = !m.onMinimap;
                s.marks().changed();
            } else if (Codex.inside(mx, my, px, y + 26, pr, y + 36)) {
                m.showDistance = !m.showDistance;
                s.marks().changed();
            }
        }
        int ay = height - 32;
        int ax = px;
        for (String a : ACTIONS) {
            if (Codex.inside(mx, my, ax, ay, ax + 20, ay + 20) && enabled(m, a)) {
                act(s, m, a);
                return;
            }
            ax += 26;
        }
        confirmRemove = false;
    }

    private void act(MapSession s, Mark m, String a) {
        switch (a) {
            case "travel" -> {
                ClientNet.travel(m.x, m.z);
                onClose();
            }
            case "share" -> MarkShare.share(m);
            case "edit" -> {
                WorldMapScreen map = new WorldMapScreen(m.dim);
                Minecraft.getInstance().setScreen(map);
                map.openDrawer(MarkEditor.edit(map, m));
            }
            case "hide" -> {
                if (m.kind == Mark.Kind.CLAIM) {
                    s.marks().setClaimMarkHidden(m, !m.hidden);
                    m.hidden = !m.hidden;
                } else {
                    m.hidden = !m.hidden;
                    s.marks().changed();
                }
            }
            case "remove" -> {
                if (confirmRemove) {
                    s.marks().remove(m);
                    selected = null;
                    confirmRemove = false;
                } else {
                    confirmRemove = true;
                }
            }
            default -> { }
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        scroll = Math.max(0, scroll - sy * 20);
        return true;
    }

    @Override
    public void removed() {
        MapSession s = MapSession.get();
        if (s != null) s.marks().save();
    }
}
