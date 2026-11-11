package com.tac5studios.codexcartography.screen;

import com.tac5studios.codexcartography.client.Dims;
import com.tac5studios.codexcartography.client.MapSession;
import com.tac5studios.codexcartography.client.OneKey;
import com.tac5studios.codexcartography.config.Features;
import com.tac5studios.codexcartography.ui.Codex;
import com.tac5studios.codexcartography.ui.Tex;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A Codex page: the index rail on the left (worlds and Codex sections), a page header, and hover labels.
 * Every Cartography screen extends this.
 */
public abstract class CodexScreen extends Screen {

    public enum Section { MAP, LEDGER, FOLIOS, PRINT, MAP_DATA, SETTINGS }

    public static final int RAIL = 32;
    private static final int TAB = 22;

    private final List<Hover> hovers = new ArrayList<>();
    private final List<RailButton> railButtons = new ArrayList<>();

    private record Hover(int x0, int y0, int x1, int y1, List<Component> lines, int color) {}

    private record RailButton(int x0, int y0, int x1, int y1, Runnable action) {}

    protected CodexScreen(Component title) {
        super(title);
    }

    protected abstract Section section();

    /** The dimension the rail shows as selected. */
    protected ResourceLocation railDimension() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null ? Level.OVERWORLD.location() : mc.level.dimension().location();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics g, int mouseX, int mouseY, float partial) {
        g.fill(0, 0, width, height, Codex.PAGE);
        for (int x = 0; x < width; x += 16) g.fill(x, 0, x + 1, height, Codex.accent(6));
        for (int y = 0; y < height; y += 16) g.fill(0, y, width, y + 1, Codex.accent(6));
        renderPage(g, mouseX, mouseY, partial);
    }

    @Override
    public void render(@NotNull GuiGraphics g, int mouseX, int mouseY, float partial) {
        hovers.clear();
        railButtons.clear();
        super.render(g, mouseX, mouseY, partial); // background + page, then widgets
        drawRail(g, mouseX, mouseY);
        renderOverlays(g, mouseX, mouseY, partial);
        for (int i = hovers.size() - 1; i >= 0; i--) {
            Hover h = hovers.get(i);
            if (Codex.inside(mouseX, mouseY, h.x0, h.y0, h.x1, h.y1)) {
                Codex.tag(g, mouseX + 10, mouseY + 8, h.color, h.lines);
                break;
            }
        }
    }

    /** The page itself (between the rail and the overlays). */
    protected abstract void renderPage(GuiGraphics g, int mouseX, int mouseY, float partial);

    /** Drawn over the rail: menus and drawers. */
    protected void renderOverlays(GuiGraphics g, int mouseX, int mouseY, float partial) {}

    /** Adds a hover label for an area (drawn on top at the end of the frame). */
    public void hover(int x0, int y0, int x1, int y1, Component... lines) {
        hovers.add(new Hover(x0, y0, x1, y1, List.of(lines), Codex.accent()));
    }

    public void hover(int x0, int y0, int x1, int y1, int color, List<Component> lines) {
        hovers.add(new Hover(x0, y0, x1, y1, lines, color));
    }

    /** All dimensions of this world: from the server's list and the map folders. */
    public static List<ResourceLocation> dimensions() {
        List<ResourceLocation> out = new ArrayList<>();
        Minecraft mc = Minecraft.getInstance();
        ClientPacketListener conn = mc.getConnection();
        if (conn != null) for (ResourceKey<Level> k : conn.levels()) out.add(k.location());
        if (out.isEmpty() && mc.level != null) out.add(mc.level.dimension().location());
        out.sort(Comparator.comparingInt((ResourceLocation d) -> switch (Dims.kind(d)) {
            case "overworld" -> d.equals(Level.OVERWORLD.location()) ? 0 : 3;
            case "nether" -> d.equals(Level.NETHER.location()) ? 1 : 3;
            case "end" -> d.equals(Level.END.location()) ? 2 : 3;
            default -> 3;
        }).thenComparing(ResourceLocation::toString));
        return out;
    }

    private void drawRail(GuiGraphics g, int mouseX, int mouseY) {
        g.fill(0, 0, RAIL, height, Codex.SLATE);
        g.fill(RAIL, 0, RAIL + 1, height, Codex.accent(90));
        int c = RAIL / 2;
        int accent = Codex.accent();
        // Codex seal
        for (int i = 0; i < 6; i++) {
            g.fill(c - i, 7 + i, c + i + 1, 8 + i, accent);
            g.fill(c - i, 18 - i, c + i + 1, 19 - i, accent);
        }
        g.fill(c - 2, 11, c + 3, 15, Codex.SLATE);
        g.fill(c - 1, 12, c + 2, 14, accent);
        int y = 28;
        Codex.fit(g, Component.translatable("codex_cartography.rail.worlds"), 2, RAIL - 2, y, Codex.MUTE);
        y += 10;
        ResourceLocation selected = section() == Section.MAP ? railDimension() : null;
        for (ResourceLocation dim : dimensions()) {
            if (y > height - 140) break;
            int x0 = c - TAB / 2;
            Codex.tex(g, Tex.dim(Dims.kind(dim)), x0, y, TAB, TAB, 32, 32);
            if (dim.equals(selected)) g.fill(0, y + 2, 2, y + TAB - 2, accent);
            int yy = y;
            railButtons.add(new RailButton(x0, yy, x0 + TAB, yy + TAB, () -> openMap(dim)));
            hover(x0, yy, x0 + TAB, yy + TAB, Dims.name(dim));
            y += TAB + 4;
        }
        y += 4;
        g.fill(6, y, RAIL - 6, y + 1, Codex.accent(60));
        y += 6;
        Codex.fit(g, Component.translatable("codex_cartography.rail.codex"), 2, RAIL - 2, y, Codex.MUTE);
        y += 10;
        for (Section s : new Section[] {Section.LEDGER, Section.FOLIOS, Section.PRINT, Section.MAP_DATA, Section.SETTINGS}) {
            if (!sectionOn(s)) continue;
            int x0 = c - TAB / 2;
            Codex.tex(g, Tex.action(glyph(s)), x0, y, TAB, TAB, 24, 24);
            if (s == section()) g.fill(0, y + 2, 2, y + TAB - 2, accent);
            int yy = y;
            railButtons.add(new RailButton(x0, yy, x0 + TAB, yy + TAB, () -> open(s)));
            hover(x0, yy, x0 + TAB, yy + TAB, Component.translatable("codex_cartography.section." + s.name().toLowerCase(java.util.Locale.ROOT)));
            y += TAB + 4;
        }
    }

    private static boolean sectionOn(Section s) {
        return switch (s) {
            case LEDGER, FOLIOS -> Features.MARKS.get();
            case PRINT -> Features.PRINT.get();
            default -> true;
        };
    }

    private static String glyph(Section s) {
        return switch (s) {
            case LEDGER -> "ledger";
            case FOLIOS -> "folio";
            case PRINT -> "print";
            case MAP_DATA -> "mapdata";
            case SETTINGS -> "settings";
            case MAP -> "lens";
        };
    }

    protected void openMap(ResourceLocation dim) {
        Minecraft.getInstance().setScreen(new WorldMapScreen(dim));
    }

    public static void open(Section s) {
        Minecraft mc = Minecraft.getInstance();
        if (MapSession.get() == null) return;
        switch (s) {
            case MAP -> mc.setScreen(new WorldMapScreen(null));
            case LEDGER -> mc.setScreen(new LedgerScreen());
            case FOLIOS -> mc.setScreen(new FoliosScreen());
            case PRINT -> mc.setScreen(new PrintScreen(null, null));
            case MAP_DATA -> mc.setScreen(new MapDataScreen());
            case SETTINGS -> mc.setScreen(new SettingsScreen());
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && mx < RAIL) {
            for (RailButton b : railButtons) {
                if (Codex.inside(mx, my, b.x0, b.y0, b.x1, b.y1)) {
                    b.action.run();
                    return true;
                }
            }
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (OneKey.KEY.matches(keyCode, scanCode) && !typingInField()) {
            OneKey.swallowUntilReleased();
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /** True while a text field on this page has the cursor, so the Codex key types its letter instead of closing. */
    protected boolean typingInField() {
        for (net.minecraft.client.gui.components.events.GuiEventListener c : children()) {
            if (c instanceof net.minecraft.client.gui.components.EditBox box && box.isFocused()) return true;
        }
        return false;
    }

    /** The page header: a dark plate with brackets, a glyph, the page name and a subtitle. */
    protected void header(GuiGraphics g, int x, int y, int w, net.minecraft.resources.ResourceLocation glyph, int glyphTex,
                          Component title, Component subtitle) {
        w = Math.max(w, 26 + Math.max(font.width(title), font.width(subtitle)) + 8);
        Codex.panel(g, x, y, x + w, y + 24);
        Codex.tex(g, glyph, x + 5, y + 4, 16, 16, glyphTex, glyphTex);
        g.drawString(font, title, x + 26, y + 4, Codex.accent(), false);
        g.drawString(font, subtitle, x + 26, y + 14, Codex.MUTE, false);
    }

    /** A small square glyph button with brackets; returns true when the mouse is over it. */
    protected boolean glyphButton(GuiGraphics g, int x, int y, int size, net.minecraft.resources.ResourceLocation tex, int texSize,
                                  int mouseX, int mouseY, Component label) {
        boolean over = Codex.inside(mouseX, mouseY, x, y, x + size, y + size);
        Codex.tex(g, tex, x, y, size, size, texSize, texSize);
        if (over) Codex.outline(g, x - 1, y - 1, x + size + 1, y + size + 1, Codex.accent(160));
        hover(x, y, x + size, y + size, label);
        return over;
    }
}
