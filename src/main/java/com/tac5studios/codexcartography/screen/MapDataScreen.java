package com.tac5studios.codexcartography.screen;

import com.google.gson.JsonObject;
import com.tac5studios.codexcartography.client.Dims;
import com.tac5studios.codexcartography.client.MapFiles;
import com.tac5studios.codexcartography.client.MapIO;
import com.tac5studios.codexcartography.client.MapSession;
import com.tac5studios.codexcartography.ui.Codex;
import com.tac5studios.codexcartography.ui.Tex;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.nio.file.Path;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Map data: the map saved on this computer for the server (or world) the player is on, and only that one.
 * Rename it, see its size, delete one dimension or the whole map. Every delete asks to confirm first.
 */
public final class MapDataScreen extends CodexScreen {

    private final Map<ResourceLocation, Long> sizes = new ConcurrentHashMap<>();
    private volatile long total = -1;
    private EditBox name;
    private ResourceLocation confirmDim;
    private boolean confirmAll;
    private final List<ResourceLocation> dims = new ArrayList<>();

    public MapDataScreen() {
        super(Component.translatable("codex_cartography.section.map_data"));
    }

    @Override
    protected Section section() {
        return Section.MAP_DATA;
    }

    @Override
    protected void init() {
        MapSession s = MapSession.get();
        dims.clear();
        dims.addAll(dimensions());
        if (s != null) {
            JsonObject info = s.worldInfo();
            name = new EditBox(font, RAIL + 18, 0, 160, 12, Component.translatable("codex_cartography.mapdata.name"));
            name.setBordered(false);
            name.setTextColor(Codex.TEXT);
            name.setMaxLength(40);
            name.setValue(info.has("name") ? info.get("name").getAsString() : s.world().name());
            addRenderableWidget(name);
            measure(s);
        }
    }

    private void measure(MapSession s) {
        Path dir = s.worldDir();
        List<ResourceLocation> list = List.copyOf(dims);
        MapIO.run("measure map data", () -> {
            for (ResourceLocation d : list) sizes.put(d, MapFiles.size(dir.resolve(Dims.folder(d))));
            total = MapFiles.size(dir);
        });
    }

    @Override
    protected void renderPage(GuiGraphics g, int mouseX, int mouseY, float partial) {
        MapSession s = MapSession.get();
        if (s == null) return;
        int x0 = RAIL + 10;
        int split = Math.min(width / 2, x0 + 240);
        header(g, x0, 6, 200, Tex.action("mapdata"), 24, Component.translatable("codex_cartography.section.map_data"),
                Component.translatable("codex_cartography.mapdata.sub", total < 0 ? "…" : MapFiles.human(total)));
        g.drawString(font, Component.translatable("codex_cartography.mapdata.about"), x0, 36, Codex.MUTE, false);

        JsonObject info = s.worldInfo();
        int y = 52;
        g.fill(x0, y, split - 10, y + 70, Codex.CARD);
        Codex.brackets(g, x0, y, split - 10, y + 70, 8, Codex.accent());
        var surface = s.surface();
        if (surface != null) {
            g.drawString(font, Component.translatable("codex_cartography.mapdata.chunks_here", String.format("%,d", surface.chunkCount())),
                    x0 + 8, y + 8, Codex.TEXT, false);
        }
        g.drawString(font, s.world().address().isEmpty() ? Component.translatable("codex_cartography.mapdata.singleplayer")
                : Component.literal(s.world().address()), x0 + 8, y + 20, Codex.MUTE, false);
        DateFormat df = DateFormat.getDateInstance(DateFormat.MEDIUM);
        if (info.has("first_mapped")) {
            g.drawString(font, Component.translatable("codex_cartography.mapdata.first", df.format(new Date(info.get("first_mapped").getAsLong()))),
                    x0 + 8, y + 32, Codex.MUTE, false);
        }
        y += 80;
        g.drawString(font, Component.translatable("codex_cartography.mapdata.name"), x0, y, Codex.MUTE, false);
        g.fill(x0, y + 10, split - 30, y + 24, Codex.CARD);
        Codex.outline(g, x0, y + 10, split - 30, y + 24, name.isFocused() ? Codex.accent() : Codex.LINE);
        name.setX(x0 + 4);
        name.setY(y + 13);
        name.setWidth(split - 40 - x0);
        glyphButton(g, split - 26, y + 10, 14, Tex.action("confirm"), 24, mouseX, mouseY, Component.translatable("codex_cartography.mapdata.rename"));

        int rx = split + 10;
        int rr = width - 12;
        g.drawString(font, Component.translatable("codex_cartography.mapdata.dimensions"), rx, 36, Codex.MUTE, false);
        int dy = 48;
        long max = 1;
        for (long v : sizes.values()) max = Math.max(max, v);
        for (ResourceLocation d : dims) {
            g.fill(rx, dy, rr, dy + 30, Codex.CARD);
            Codex.outline(g, rx, dy, rr, dy + 30, Codex.LINE);
            Codex.tex(g, Tex.dim(Dims.kind(d)), rx + 5, dy + 6, 18, 18, 32, 32);
            g.drawString(font, Dims.name(d), rx + 28, dy + 5, Codex.TEXT, false);
            Long sz = sizes.get(d);
            String size = sz == null ? "…" : MapFiles.human(sz);
            g.drawString(font, size, rr - 30 - font.width(size), dy + 5, Codex.MUTE, false);
            g.fill(rx + 28, dy + 19, rr - 30, dy + 22, 0xFF1E262C);
            if (sz != null) g.fill(rx + 28, dy + 19, rx + 28 + (int) ((rr - 58 - rx) * (sz / (double) max)), dy + 22, 0xFF000000 | Dims.color(d));
            boolean confirm = d.equals(confirmDim);
            Codex.tex(g, Tex.action(confirm ? "remove_confirm" : "remove"), rr - 24, dy + 6, 18, 18, 24, 24);
            hover(rr - 24, dy + 6, rr - 6, dy + 24, Component.translatable(confirm ? "codex_cartography.mapdata.delete_confirm" : "codex_cartography.mapdata.delete_dim", Dims.name(d)));
            dy += 34;
        }
        if (confirmDim != null) warning(g, rx, dy + 4, rr, Component.translatable("codex_cartography.mapdata.warn_dim_title", Dims.name(confirmDim)),
                Component.translatable("codex_cartography.mapdata.warn_dim", Dims.name(confirmDim)));
        int ay = height - 44;
        g.fill(rx, ay, rr, ay + 30, Codex.RED_DARK);
        Codex.outline(g, rx, ay, rr, ay + 30, Codex.RED);
        Codex.tex(g, Tex.action(confirmAll ? "remove_confirm" : "remove"), rx + 6, ay + 6, 18, 18, 24, 24);
        g.drawString(font, Component.translatable("codex_cartography.mapdata.delete_all"), rx + 30, ay + 6, 0xFFFFAAAA, false);
        g.drawString(font, Component.translatable("codex_cartography.mapdata.delete_all_sub"), rx + 30, ay + 17, 0xFFC88C8C, false);
        if (confirmAll) warning(g, rx, ay - 64, rr, Component.translatable("codex_cartography.mapdata.warn_all_title"),
                Component.translatable("codex_cartography.mapdata.warn_all"));
    }

    private void warning(GuiGraphics g, int x0, int y0, int x1, Component title, Component body) {
        int y1 = y0 + 58;
        g.pose().pushPose();
        g.pose().translate(0, 0, 100);
        g.fill(x0, y0, x1, y1, Codex.RED_DARK);
        Codex.outline(g, x0, y0, x1, y1, Codex.RED);
        Codex.brackets(g, x0, y0, x1, y1, 8, Codex.RED);
        Codex.tex(g, Tex.DANGER, x0 + 6, y0 + 6, 16, 16, 64, 64);
        g.drawString(font, title, x0 + 28, y0 + 6, 0xFFFFBEBE, false);
        int ly = y0 + 18;
        for (var line : Codex.wrap(body, x1 - x0 - 34)) {
            g.drawString(font, line, x0 + 28, ly, Codex.TEXT, false);
            ly += 10;
        }
        g.drawString(font, Component.translatable("codex_cartography.mapdata.click_again"), x0 + 28, y1 - 11, 0xFFFFAAAA, false);
        Component cancel = Component.translatable("codex_cartography.mapdata.cancel");
        g.drawString(font, cancel, x1 - 6 - font.width(cancel), y1 - 11, Codex.accent(), false);
        g.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        MapSession s = MapSession.get();
        if (s == null || button != 0) return false;
        int x0 = RAIL + 10;
        int split = Math.min(width / 2, x0 + 240);
        if (Codex.inside(mx, my, split - 26, 142, split - 12, 156) && !name.getValue().isBlank()) {
            s.rename(name.getValue().trim());
            return true;
        }
        int rx = split + 10;
        int rr = width - 12;
        int dy = 48;
        for (ResourceLocation d : dims) {
            if (Codex.inside(mx, my, rr - 24, dy + 6, rr - 6, dy + 24)) {
                if (d.equals(confirmDim)) {
                    s.deleteDimension(d);
                    confirmDim = null;
                    sizes.put(d, 0L);
                    measure(s);
                } else {
                    confirmDim = d;
                    confirmAll = false;
                }
                return true;
            }
            dy += 34;
        }
        int ay = height - 44;
        if (Codex.inside(mx, my, rx, ay, rr, ay + 30)) {
            if (confirmAll) {
                s.deleteAll();
                confirmAll = false;
                sizes.clear();
                measure(s);
            } else {
                confirmAll = true;
                confirmDim = null;
            }
            return true;
        }
        confirmDim = null;
        confirmAll = false;
        return false;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == 256 && (confirmDim != null || confirmAll)) {
            confirmDim = null;
            confirmAll = false;
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }
}
