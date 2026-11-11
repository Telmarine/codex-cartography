package com.tac5studios.codexcartography.screen;

import com.tac5studios.codexcartography.claims.ClaimPanel;
import com.tac5studios.codexcartography.claims.ClaimsData;
import com.tac5studios.codexcartography.client.ChunkColumns;
import com.tac5studios.codexcartography.client.ClientRules;
import com.tac5studios.codexcartography.client.Dims;
import com.tac5studios.codexcartography.client.DimensionStore;
import com.tac5studios.codexcartography.client.MapRegion;
import com.tac5studios.codexcartography.client.MapSession;
import com.tac5studios.codexcartography.client.ServerMarkers;
import com.tac5studios.codexcartography.client.SlimeChunks;
import com.tac5studios.codexcartography.client.WorldBorders;
import com.tac5studios.codexcartography.config.ClientConfig;
import com.tac5studios.codexcartography.config.Features;
import com.tac5studios.codexcartography.hud.Sweep;
import com.tac5studios.codexcartography.map.RegionImages;
import com.tac5studios.codexcartography.mark.Mark;
import com.tac5studios.codexcartography.net.ClientNet;
import com.tac5studios.codexcartography.ui.Codex;
import com.tac5studios.codexcartography.ui.Tex;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.border.WorldBorder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** The world map: a Codex page with the map in the middle, the index rail, the layer rail and the readout strip. */
public final class WorldMapScreen extends CodexScreen {

    private static final int RIGHT = 32;
    private static final int STRIP = 16;
    private static final double MIN_ZOOM = 1 / 32.0;
    private static final double MAX_ZOOM = 16;
    private static final Map<ResourceLocation, double[]> VIEWS = new HashMap<>();

    private final ResourceLocation dim;
    private double centerX;
    private double centerZ;
    private double zoom = 1;
    private boolean panning;
    private double pressX;
    private double pressY;
    private int pressButton = -1;
    private double selX0;
    private double selZ0;
    private double selX1;
    private double selZ1;
    private boolean selecting;
    private ContextMenu menu;
    private Drawer drawer;
    private EditBox search;
    private final List<SearchHit> hits = new ArrayList<>();

    private record SearchHit(Component label, Component kind, double x, double z) {}

    public WorldMapScreen(ResourceLocation dimension) {
        super(Component.translatable("codex_cartography.section.map"));
        Minecraft mc = Minecraft.getInstance();
        this.dim = dimension != null ? dimension : mc.level != null ? mc.level.dimension().location() : ResourceLocation.withDefaultNamespace("overworld");
        double[] v = VIEWS.get(dim);
        LocalPlayer p = mc.player;
        boolean here = mc.level != null && mc.level.dimension().location().equals(dim);
        if (v != null) {
            centerX = v[0];
            centerZ = v[1];
            zoom = v[2];
        }
        if (here && p != null) {
            centerX = p.getX();
            centerZ = p.getZ();
        }
    }

    @Override
    protected Section section() {
        return Section.MAP;
    }

    @Override
    protected ResourceLocation railDimension() {
        return dim;
    }

    private boolean here() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && mc.level.dimension().location().equals(dim);
    }

    @Override
    protected void init() {
        int sw = searchWidth();
        search = new EditBox(font, width - RIGHT - sw - 20, 9, sw - 24, 12, Component.translatable("codex_cartography.map.search"));
        search.setBordered(false);
        search.setTextColor(Codex.TEXT);
        search.setHint(Component.translatable("codex_cartography.map.search").withColor(Codex.GREY));
        search.setResponder(this::updateSearch);
        addRenderableWidget(search);
    }

    @Override
    public void onClose() {
        VIEWS.put(dim, new double[] {centerX, centerZ, zoom});
        MapSession s = MapSession.get();
        if (s != null) s.closeViewing();
        super.onClose();
    }

    @Override
    public void tick() {
        MapSession s = MapSession.get();
        if (s != null && !here()) s.tickViewing();
        if (drawer != null && !drawer.open()) drawer = null;
    }

    private int mapX0() { return RAIL + 1; }
    private int mapX1() { return width - RIGHT - (drawer != null ? drawer.width() : 0); }
    private int mapY1() { return height - STRIP; }
    private double midX() { return (mapX0() + width - RIGHT) / 2.0; }
    private double midY() { return mapY1() / 2.0; }

    public double toScreenX(double wx) { return midX() + (wx - centerX) * zoom; }
    public double toScreenY(double wz) { return midY() + (wz - centerZ) * zoom; }
    public double toWorldX(double sx) { return centerX + (sx - midX()) / zoom; }
    public double toWorldZ(double sy) { return centerZ + (sy - midY()) / zoom; }

    private DimensionStore store() {
        MapSession s = MapSession.get();
        return s == null ? null : s.storeFor(dim);
    }

    @Override
    protected void renderPage(GuiGraphics g, int mouseX, int mouseY, float partial) {
        DimensionStore store = store();
        if (store == null) return;
        int x0 = mapX0();
        int x1 = width - RIGHT;
        int y1 = mapY1();
        g.enableScissor(x0, 0, x1, y1);
        drawFog(g, x0, x1, y1);
        drawRegions(g, store, x0, x1, y1);
        if (ClientConfig.LAYER_GRID.get() && zoom >= 0.5) drawGrid(g, x0, x1, y1);
        if (ClientConfig.LAYER_SLIME.get() && ClientRules.slimeChunks && here()) drawSlime(g, x0, x1, y1);
        if (claimsOn()) drawClaims(g, mouseX, mouseY);
        if (ClientConfig.LAYER_BORDER.get() && ClientConfig.BORDER_WORLD_MAP.get() && ClientRules.worldBorder) drawBorder(g, x0, x1, y1);
        if (selecting) drawSelection(g);
        if (ClientConfig.LAYER_MARKS.get() && Features.MARKS.get()) drawMarks(g, mouseX, mouseY);
        drawServerMarkers(g, mouseX, mouseY);
        if (ClientConfig.LAYER_PLAYERS.get()) drawPlayers(g, mouseX, mouseY, partial);
        g.disableScissor();

        header(g, x0 + 8, 6, 150, Tex.dim(Dims.kind(dim)), 32, Component.translatable("codex_cartography.map.title"),
                Dims.name(dim).copy().append(" · ").append(worldName()));
        drawSearchPlate(g, mouseX, mouseY);
        drawRightRail(g, mouseX, mouseY);
        drawStrip(g, mouseX, mouseY);
        if (Features.MINIMAP.get() || Features.WORLD_MAP.get()) drawNorth(g);
    }

    private Component worldName() {
        MapSession s = MapSession.get();
        if (s == null) return Component.empty();
        var info = s.worldInfo();
        return Component.literal(info.has("name") ? info.get("name").getAsString() : s.world().name());
    }

    private void drawFog(GuiGraphics g, int x0, int x1, int y1) {
        int t = 32;
        int ox = Math.floorMod((int) Math.round(-centerX * zoom), t);
        int oy = Math.floorMod((int) Math.round(-centerZ * zoom), t);
        for (int y = oy - t; y < y1; y += t) {
            for (int x = x0 + ox - t; x < x1; x += t) {
                Codex.tinted(g, Tex.FOG, x, y, t, t, 64, 64, 0xFF8A9A92);
            }
        }
    }

    private void drawRegions(GuiGraphics g, DimensionStore store, int x0, int x1, int y1) {
        RegionImages images = store.images();
        int rx0 = Mth.floor(toWorldX(x0)) >> 9;
        int rx1 = Mth.floor(toWorldX(x1)) >> 9;
        int rz0 = Mth.floor(toWorldZ(0)) >> 9;
        int rz1 = Mth.floor(toWorldZ(y1)) >> 9;
        boolean far = zoom < 0.2 || (rx1 - rx0 + 1) * (rz1 - rz0 + 1) > ClientConfig.MAX_REGIONS.get() - 4;
        int newPictures = 0;
        for (int rz = rz0; rz <= rz1; rz++) {
            for (int rx = rx0; rx <= rx1; rx++) {
                float sx = (float) toScreenX(rx * 512.0);
                float sy = (float) toScreenY(rz * 512.0);
                float size = (float) (512 * zoom);
                ResourceLocation tex = null;
                int texSize = 512;
                if (!far) {
                    RegionImages.Picture p = images.existing(rx, rz);
                    if (p == null) {
                        MapRegion r = store.region(rx, rz);
                        if (r != null && newPictures < 2) {
                            newPictures++;
                            p = images.picture(r);
                        }
                    }
                    if (p != null) tex = p.id;
                }
                if (tex == null) {
                    tex = images.overview(rx, rz);
                    texSize = RegionImages.OVERVIEW;
                }
                if (tex == null) continue;
                g.pose().pushPose();
                g.pose().translate(sx, sy, 0);
                g.pose().scale(size / texSize, size / texSize, 1);
                Codex.tex(g, tex, 0, 0, texSize, texSize, texSize, texSize);
                g.pose().popPose();
            }
        }
    }

    private void drawGrid(GuiGraphics g, int x0, int x1, int y1) {
        int c = 0x18FFFFFF;
        int cx0 = Mth.floor(toWorldX(x0)) >> 4;
        int cx1 = Mth.floor(toWorldX(x1)) >> 4;
        for (int cx = cx0; cx <= cx1 + 1; cx++) {
            int sx = (int) Math.round(toScreenX(cx * 16.0));
            g.fill(sx, 0, sx + 1, y1, c);
        }
        int cz0 = Mth.floor(toWorldZ(0)) >> 4;
        int cz1 = Mth.floor(toWorldZ(y1)) >> 4;
        for (int cz = cz0; cz <= cz1 + 1; cz++) {
            int sy = (int) Math.round(toScreenY(cz * 16.0));
            g.fill(x0, sy, x1, sy + 1, c);
        }
    }

    private void drawSlime(GuiGraphics g, int x0, int x1, int y1) {
        int cx0 = Mth.floor(toWorldX(x0)) >> 4;
        int cx1 = Mth.floor(toWorldX(x1)) >> 4;
        int cz0 = Mth.floor(toWorldZ(0)) >> 4;
        int cz1 = Mth.floor(toWorldZ(y1)) >> 4;
        if ((cx1 - cx0) * (cz1 - cz0) > 40000) return;
        for (int cz = cz0; cz <= cz1; cz++) {
            for (int cx = cx0; cx <= cx1; cx++) {
                if (!SlimeChunks.isSlime(cx, cz)) continue;
                g.fill((int) toScreenX(cx * 16.0), (int) toScreenY(cz * 16.0), (int) toScreenX(cx * 16.0 + 16), (int) toScreenY(cz * 16.0 + 16), 0x5578DC6E);
            }
        }
    }

    private boolean claimsOn() {
        return Features.CLAIMS_LAYER.get() && ClientRules.claims && ClientConfig.LAYER_CLAIMS.get();
    }

    private void drawClaims(GuiGraphics g, int mouseX, int mouseY) {
        for (ClaimsData.Claim c : ClaimsData.in(dim)) {
            int color = c.drawColor();
            int minX = Integer.MAX_VALUE;
            int minZ = Integer.MAX_VALUE;
            for (long ch : c.chunks()) {
                int cx = ChunkPos.getX(ch);
                int cz = ChunkPos.getZ(ch);
                int sx0 = (int) toScreenX(cx * 16.0);
                int sy0 = (int) toScreenY(cz * 16.0);
                int sx1 = (int) toScreenX(cx * 16.0 + 16);
                int sy1 = (int) toScreenY(cz * 16.0 + 16);
                g.fill(sx0, sy0, sx1, sy1, (0x38 << 24) | color);
                int edge = 0xFF000000 | color;
                if (ClaimsData.at(dim, cx, cz - 1) != c) dashed(g, sx0, sy0, sx1, sy0 + 1, edge, true);
                if (ClaimsData.at(dim, cx, cz + 1) != c) dashed(g, sx0, sy1 - 1, sx1, sy1, edge, true);
                if (ClaimsData.at(dim, cx - 1, cz) != c) dashed(g, sx0, sy0, sx0 + 1, sy1, edge, false);
                if (ClaimsData.at(dim, cx + 1, cz) != c) dashed(g, sx1 - 1, sy0, sx1, sy1, edge, false);
                if (cz < minZ || (cz == minZ && cx < minX)) {
                    minX = cx;
                    minZ = cz;
                }
                if (Codex.inside(mouseX, mouseY, sx0, sy0, sx1, sy1) && menu == null) {
                    List<Component> lines = new ArrayList<>();
                    lines.add(Component.literal(c.privateLand() ? Component.translatable("codex_cartography.claims.private").getString() : c.name()));
                    if (!c.privateLand()) lines.add(Component.translatable("codex_cartography.claims.owner", c.owner()));
                    lines.add(standing(c));
                    hover(sx0, sy0, sx1, sy1, color | 0xFF000000, lines);
                }
            }
            if (minX != Integer.MAX_VALUE && zoom >= 0.5) {
                Component name = Component.literal(c.privateLand() ? Component.translatable("codex_cartography.claims.private").getString() : c.name());
                g.drawString(font, name, (int) toScreenX(minX * 16.0) + 3, (int) toScreenY(minZ * 16.0) + 3, 0xFF000000 | color, true);
            }
        }
    }

    static Component standing(ClaimsData.Claim c) {
        return switch (c.standing()) {
            case ClaimsData.OWN -> Component.translatable("codex_cartography.claims.yours");
            case ClaimsData.TRUSTED -> Component.translatable("codex_cartography.claims.trusted", c.rank());
            case ClaimsData.ADMIN -> Component.translatable("codex_cartography.claims.server");
            default -> Component.translatable("codex_cartography.claims.other");
        };
    }

    private static void dashed(GuiGraphics g, int x0, int y0, int x1, int y1, int color, boolean horizontal) {
        if (horizontal) {
            for (int x = x0; x < x1; x += 4) g.fill(x, y0, Math.min(x + 2, x1), y1, color);
        } else {
            for (int y = y0; y < y1; y += 4) g.fill(x0, y, x1, Math.min(y + 2, y1), color);
        }
    }

    private void drawBorder(GuiGraphics g, int x0, int x1, int y1) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        WorldBorder b = WorldBorders.of(mc.level, dim);
        if (b == null) return;
        int color = 0xFF000000 | ClientConfig.BORDER_COLOR.get();
        int bx0 = (int) toScreenX(b.getMinX());
        int bx1 = (int) toScreenX(b.getMaxX());
        int bz0 = (int) toScreenY(b.getMinZ());
        int bz1 = (int) toScreenY(b.getMaxZ());
        int shade = 0x70080404;
        g.fill(x0, 0, x1, Math.max(0, bz0), shade);
        g.fill(x0, Math.min(y1, bz1), x1, y1, shade);
        g.fill(x0, Math.max(0, bz0), Math.max(x0, bx0), Math.min(y1, bz1), shade);
        g.fill(Math.min(x1, bx1), Math.max(0, bz0), x1, Math.min(y1, bz1), shade);
        Codex.outline(g, bx0, bz0, bx1, bz1, color);
        if (b.getLerpRemainingTime() > 0) {
            double half = b.getLerpTarget() / 2;
            int tx0 = (int) toScreenX(b.getCenterX() - half);
            int tx1 = (int) toScreenX(b.getCenterX() + half);
            int tz0 = (int) toScreenY(b.getCenterZ() - half);
            int tz1 = (int) toScreenY(b.getCenterZ() + half);
            int faint = (0x90 << 24) | ClientConfig.BORDER_COLOR.get();
            dashed(g, tx0, tz0, tx1, tz0 + 1, faint, true);
            dashed(g, tx0, tz1, tx1, tz1 + 1, faint, true);
            dashed(g, tx0, tz0, tx0 + 1, tz1, faint, false);
            dashed(g, tx1, tz0, tx1 + 1, tz1, faint, false);
        }
    }

    private void drawSelection(GuiGraphics g) {
        int[] r = selectionChunks();
        int sx0 = (int) toScreenX(r[0] * 16.0);
        int sz0 = (int) toScreenY(r[1] * 16.0);
        int sx1 = (int) toScreenX((r[2] + 1) * 16.0);
        int sz1 = (int) toScreenY((r[3] + 1) * 16.0);
        g.fill(sx0, sz0, sx1, sz1, Codex.accent(40));
        int a = Codex.accent();
        dashed(g, sx0, sz0, sx1, sz0 + 1, a, true);
        dashed(g, sx0, sz1 - 1, sx1, sz1, a, true);
        dashed(g, sx0, sz0, sx0 + 1, sz1, a, false);
        dashed(g, sx1 - 1, sz0, sx1, sz1, a, false);
        int w = r[2] - r[0] + 1;
        int h = r[3] - r[1] + 1;
        Component label = Component.translatable("codex_cartography.map.area_size", w, h, w * h);
        int lw = font.width(label) + 8;
        g.fill(sx0, sz0 - 13, sx0 + lw, sz0 - 1, Codex.DARK);
        Codex.outline(g, sx0, sz0 - 13, sx0 + lw, sz0 - 1, a);
        g.drawString(font, label, sx0 + 4, sz0 - 11, a, false);
    }

    /** The selected chunks as minX, minZ, maxX, maxZ. */
    private int[] selectionChunks() {
        int ax = Mth.floor(selX0) >> 4;
        int az = Mth.floor(selZ0) >> 4;
        int bx = Mth.floor(selX1) >> 4;
        int bz = Mth.floor(selZ1) >> 4;
        return new int[] {Math.min(ax, bx), Math.min(az, bz), Math.max(ax, bx), Math.max(az, bz)};
    }

    private void drawMarks(GuiGraphics g, int mouseX, int mouseY) {
        MapSession s = MapSession.get();
        if (s == null) return;
        Minecraft mc = Minecraft.getInstance();
        float size = (float) Mth.clamp(10 + zoom * 2, 10, 18);
        for (Mark m : s.marks().mapIn(dim)) {
            float sx = (float) toScreenX(m.x + 0.5);
            float sy = (float) toScreenY(m.z + 0.5);
            if (sx < mapX0() - 20 || sx > width + 20 || sy < -20 || sy > height + 20) continue;
            m.drawBadge(g, sx, sy, size);
            boolean over = Math.abs(mouseX - sx) < size / 2 && Math.abs(mouseY - sy) < size / 2;
            if (zoom >= 0.25 || over) Codex.label(g, Component.literal(m.name), (int) sx, (int) (sy - size / 2 - 10), Codex.TEXT);
            if (over && menu == null) {
                List<Component> lines = new ArrayList<>();
                lines.add(Component.literal(m.name));
                lines.add(Component.literal(m.x + ", " + m.y + ", " + m.z));
                if (mc.player != null && here()) lines.add(Component.translatable("codex_cartography.distance", distance(mc.player, m.x, m.z)));
                hover((int) (sx - size / 2), (int) (sy - size / 2), (int) (sx + size / 2), (int) (sy + size / 2), 0xFF000000 | m.drawColor(), lines);
            }
        }
    }

    static String distance(Player p, double x, double z) {
        double d = Math.sqrt(p.distanceToSqr(x + 0.5, p.getY(), z + 0.5));
        return d >= 1000 ? String.format(Locale.ROOT, "%.1fk", d / 1000) : (int) d + "m";
    }

    private void drawServerMarkers(GuiGraphics g, int mouseX, int mouseY) {
        float size = (float) Mth.clamp(10 + zoom * 2, 10, 16);
        for (ServerMarkers.Marker m : ServerMarkers.in(dim)) {
            float sx = (float) toScreenX(m.x() + 0.5);
            float sy = (float) toScreenY(m.z() + 0.5);
            Codex.badge(g, Tex.SERVER_BADGE, Tex.serverEmblem(m.kind()), sx, sy, size, m.color());
            if (zoom >= 0.25) Codex.label(g, Component.literal(m.name()), (int) sx, (int) (sy - size / 2 - 10), Codex.TEXT);
            hover((int) (sx - size / 2), (int) (sy - size / 2), (int) (sx + size / 2), (int) (sy + size / 2), 0xFF000000 | m.color(),
                    List.of(Component.literal(m.name()), Component.translatable("codex_cartography.marker." + m.kind())));
        }
    }

    private void drawPlayers(GuiGraphics g, int mouseX, int mouseY, float partial) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || !here()) return;
        if (Sweep.players()) {
            for (Player p : mc.level.players()) {
                if (p == mc.player || p.isSpectator()) continue;
                float sx = (float) toScreenX(Mth.lerp(partial, p.xo, p.getX()));
                float sy = (float) toScreenY(Mth.lerp(partial, p.zo, p.getZ()));
                Codex.texAt(g, Tex.OTHER, sx, sy, 10, p.getViewYRot(partial) + 180, 32, 0xFFFFFFFF);
                Codex.label(g, p.getName(), (int) sx, (int) sy - 14, Codex.TEXT);
            }
        }
        LocalPlayer me = mc.player;
        float sx = (float) toScreenX(Mth.lerp(partial, me.xo, me.getX()));
        float sy = (float) toScreenY(Mth.lerp(partial, me.zo, me.getZ()));
        Codex.texAt(g, Tex.PLAYER, sx, sy, 13, me.getViewYRot(partial) + 180, 32, 0xFFFFFFFF);
        hover((int) sx - 6, (int) sy - 6, (int) sx + 6, (int) sy + 6, me.getName(), Component.translatable("codex_cartography.map.you"));
    }

    private void drawNorth(GuiGraphics g) {
        int cx = (int) midX();
        g.fill(cx - 6, 34, cx + 7, 44, Codex.DARK);
        Codex.outline(g, cx - 6, 34, cx + 7, 44, Codex.accent());
        g.drawString(font, "N", cx - 2, 35, Codex.accent(), false);
    }

    /** Wide enough for the search hint plus the glyph. */
    private int searchWidth() {
        return Math.max(120, font.width(Component.translatable("codex_cartography.map.search")) + 30);
    }

    private void drawSearchPlate(GuiGraphics g, int mouseX, int mouseY) {
        int sw = searchWidth();
        int x = width - RIGHT - sw - 40;
        Codex.panel(g, x, 4, x + sw, 22);
        Codex.tex(g, Tex.action("search"), x + 3, 6, 14, 14, 24, 24);
        int lx = x + sw + 4;
        Codex.panel(g, lx, 4, lx + 20, 22);
        boolean lensOn = Features.LENSES.get();
        if (glyphButton(g, lx + 3, 6, 14, Tex.action("lens"), 24, mouseX, mouseY,
                Component.translatable("codex_cartography.lens." + ClientConfig.LENS.get().name().toLowerCase(Locale.ROOT)))) {
            if (!lensOn) hover(lx, 4, lx + 20, 22, Component.translatable("codex_cartography.lens.off"));
        }
        if (!hits.isEmpty() && search.isFocused()) {
            int y = 24;
            for (SearchHit h : hits) {
                boolean over = Codex.inside(mouseX, mouseY, x, y, x + sw + 24, y + 12);
                g.fill(x, y, x + sw + 24, y + 12, over ? Codex.CARD_ON : Codex.DARK);
                g.drawString(font, h.label(), x + 4, y + 2, over ? Codex.accent() : Codex.TEXT, false);
                g.drawString(font, h.kind(), x + sw + 20 - font.width(h.kind()), y + 2, Codex.MUTE, false);
                y += 12;
            }
        }
    }

    private void updateSearch(String text) {
        hits.clear();
        String q = text.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) return;
        MapSession s = MapSession.get();
        if (s != null) {
            for (Mark m : s.marks().mapIn(dim)) {
                if (m.name.toLowerCase(Locale.ROOT).contains(q)) hits.add(new SearchHit(Component.literal(m.name), Component.translatable("codex_cartography.search.mark"), m.x, m.z));
            }
        }
        for (ClaimsData.Claim c : ClaimsData.in(dim)) {
            if (!c.privateLand() && c.name().toLowerCase(Locale.ROOT).contains(q) && c.chunks().length > 0) {
                hits.add(new SearchHit(Component.literal(c.name()), Component.translatable("codex_cartography.search.claim"),
                        ChunkPos.getX(c.chunks()[0]) * 16 + 8, ChunkPos.getZ(c.chunks()[0]) * 16 + 8));
            }
        }
        for (ServerMarkers.Marker m : ServerMarkers.in(dim)) {
            if (m.name().toLowerCase(Locale.ROOT).contains(q)) hits.add(new SearchHit(Component.literal(m.name()), Component.translatable("codex_cartography.marker." + m.kind()), m.x(), m.z()));
        }
        if (hits.size() > 8) hits.subList(8, hits.size()).clear();
    }

    private record Layer(String name, String tex, net.neoforged.neoforge.common.ModConfigSpec.BooleanValue value, boolean allowed) {}

    private List<Layer> layers() {
        List<Layer> out = new ArrayList<>();
        out.add(new Layer("claims", "claims", ClientConfig.LAYER_CLAIMS, Features.CLAIMS_LAYER.get() && ClientRules.claims));
        out.add(new Layer("marks", "marks", ClientConfig.LAYER_MARKS, Features.MARKS.get()));
        out.add(new Layer("players", "players", ClientConfig.LAYER_PLAYERS, true));
        out.add(new Layer("grid", "grid", ClientConfig.LAYER_GRID, true));
        out.add(new Layer("slime", "slime", ClientConfig.LAYER_SLIME, ClientRules.slimeChunks));
        out.add(new Layer("border", "border", ClientConfig.LAYER_BORDER, ClientRules.worldBorder));
        out.add(new Layer("waystones", "waystones", ClientConfig.LAYER_WAYSTONES, Features.WAYSTONES_MARKERS.get() && ClientRules.waystones));
        out.add(new Layer("homes", "homes", ClientConfig.LAYER_HOMES, Features.NEXUS_MARKERS.get() && ClientRules.nexusMarkers));
        return out;
    }

    private void drawRightRail(GuiGraphics g, int mouseX, int mouseY) {
        int x0 = width - RIGHT;
        g.fill(x0, 0, width, height, Codex.SLATE);
        g.fill(x0 - 1, 0, x0, height, Codex.accent(90));
        int y = 4;
        Codex.fit(g, Component.translatable("codex_cartography.rail.layers"), x0 + 2, width - 2, y, Codex.MUTE);
        y += 10;
        int bx = x0 + 5;
        for (Layer l : layers()) {
            boolean on = l.allowed() && l.value().get();
            Codex.tex(g, Tex.layer(l.tex(), on), bx, y, 22, 22, 24, 24);
            Component name = Component.translatable("codex_cartography.layer." + l.name());
            Component state = !l.allowed() ? Component.translatable("codex_cartography.layer.server_off")
                    : Component.translatable(on ? "codex_cartography.layer.on" : "codex_cartography.layer.off");
            hover(bx, y, bx + 22, y + 22, Component.translatable("codex_cartography.layer.state", name, state));
            y += 25;
        }
        y += 2;
        glyphButton(g, bx, y, 22, Tex.action("center"), 24, mouseX, mouseY, Component.translatable("codex_cartography.map.center"));
        y += 28;
        if (ClientConfig.KEY_HINTS.get() && y < height - 90) {
            g.fill(x0 + 4, y, width - 4, y + 1, Codex.accent(60));
            y += 4;
            Codex.fit(g, Component.translatable("codex_cartography.rail.keys"), x0 + 2, width - 2, y, Codex.MUTE);
            y += 10;
            for (String k : new String[] {"rmb", "drag", "wheel", "esc"}) {
                Component cap = Component.translatable("codex_cartography.keys." + k);
                Component what = Component.translatable("codex_cartography.keys." + k + ".what");
                int cw = Math.max(18, font.width(cap) + 6);
                int cx = x0 + (RIGHT - cw) / 2;
                g.fill(cx, y, cx + cw, y + 11, 0xFF1E262C);
                Codex.outline(g, cx, y, cx + cw, y + 11, 0xFF505C64);
                g.drawString(font, cap, cx + (cw - font.width(cap)) / 2, y + 2, Codex.TEXT, false);
                g.pose().pushPose();
                g.pose().translate(x0 + RIGHT / 2f, y + 13, 0);
                g.pose().scale(0.6f, 0.6f, 1);
                g.drawString(font, what, -font.width(what) / 2, 0, Codex.MUTE, false);
                g.pose().popPose();
                y += 22;
            }
        }
    }

    private boolean clickRightRail(double mx, double my) {
        int x0 = width - RIGHT;
        if (mx < x0) return false;
        int y = 14;
        int bx = x0 + 5;
        for (Layer l : layers()) {
            if (Codex.inside(mx, my, bx, y, bx + 22, y + 22)) {
                if (l.allowed()) ClientConfig.set(l.value(), !l.value().get());
                return true;
            }
            y += 25;
        }
        y += 2;
        if (Codex.inside(mx, my, bx, y, bx + 22, y + 22)) {
            centerOnPlayer();
            return true;
        }
        return true;
    }

    private void centerOnPlayer() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && here()) {
            centerX = mc.player.getX();
            centerZ = mc.player.getZ();
        }
    }

    private void drawStrip(GuiGraphics g, int mouseX, int mouseY) {
        int y0 = height - STRIP;
        int x1 = width - RIGHT;
        g.fill(RAIL + 1, y0, x1, height, 0xE00A0E12);
        g.fill(RAIL + 1, y0, x1, y0 + 1, Codex.accent(110));
        boolean overMap = mouseX > RAIL && mouseX < mapX1() && mouseY < y0;
        int wx = Mth.floor(overMap ? toWorldX(mouseX) : centerX);
        int wz = Mth.floor(overMap ? toWorldZ(mouseY) : centerZ);
        DimensionStore store = store();
        ChunkColumns col = store == null ? null : store.columns(wx, wz);
        int i = ((wz & 15) << 4) | (wx & 15);
        int x = RAIL + 8;
        int ty = y0 + 5;
        x = coord(g, x, ty, "X", String.valueOf(wx));
        if (col != null && (col.color()[i] >>> 24) != 0) x = coord(g, x, ty, "Y", String.valueOf(col.height()[i]));
        x = coord(g, x, ty, "Z", String.valueOf(wz));
        if (col != null) {
            MapRegion r = store.peek(wx >> 9, wz >> 9);
            if (r != null) {
                ResourceLocation b = ResourceLocation.tryParse(r.biomeId(col.biome()[i]));
                if (b != null) {
                    Component bn = Component.translatable("biome." + b.getNamespace() + "." + b.getPath());
                    g.drawString(font, bn, x, ty, Codex.TEXT, false);
                    x += font.width(bn) + 10;
                }
            }
        }
        Component chunk = Component.translatable("codex_cartography.map.chunk", wx >> 4, wz >> 4);
        g.drawString(font, chunk, x, ty, Codex.TEXT, false);
        x += font.width(chunk) + 10;
        if (claimsOn()) {
            ClaimsData.Claim c = ClaimsData.at(dim, wx >> 4, wz >> 4);
            if (c != null) {
                Component cl = Component.translatable("codex_cartography.map.claimed", c.privateLand()
                        ? Component.translatable("codex_cartography.claims.private").getString() : c.name());
                g.drawString(font, cl, x, ty, 0xFF000000 | c.drawColor(), false);
            }
        }
        // scale bar
        double[] nice = {1, 2, 5, 10, 16, 32, 50, 100, 200, 256, 500, 1000, 2000, 5000, 10000};
        double blocks = nice[nice.length - 1];
        for (double n : nice) {
            if (n * zoom >= 30) {
                blocks = n;
                break;
            }
        }
        int len = (int) (blocks * zoom);
        int bx1 = x1 - 8;
        int bx0 = bx1 - len;
        g.fill(bx0, y0 + 9, bx1, y0 + 10, Codex.TEXT);
        g.fill(bx0, y0 + 6, bx0 + 1, y0 + 12, Codex.TEXT);
        g.fill(bx1 - 1, y0 + 6, bx1, y0 + 12, Codex.TEXT);
        Component bl = Component.literal((int) blocks + "");
        g.drawString(font, bl, bx0 - font.width(bl) - 4, ty, Codex.MUTE, false);
    }

    private int coord(GuiGraphics g, int x, int y, String axis, String value) {
        g.drawString(font, axis, x, y, Codex.accent(), false);
        g.drawString(font, value, x + 8, y, Codex.TEXT, false);
        return x + 8 + font.width(value) + 10;
    }

    @Override
    protected void renderOverlays(GuiGraphics g, int mouseX, int mouseY, float partial) {
        if (drawer != null) drawer.render(g, width - RIGHT - drawer.width(), height - STRIP, mouseX, mouseY, partial);
        if (menu != null) menu.render(g, mouseX, mouseY, (a, b, c, d, t, r) -> hover(a, b, c, d, 0xFF78808A, List.of(t, r)));
    }

    // ---- input ----

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (menu != null) {
            if (menu.contains(mx, my)) {
                if (menu.click(mx, my)) menu = null;
                return true;
            }
            menu = null;
            selecting = false;
        }
        if (search.isFocused() && !hits.isEmpty()) {
            int sw = searchWidth();
            int x = width - RIGHT - sw - 40;
            int y = 24;
            for (SearchHit h : hits) {
                if (Codex.inside(mx, my, x, y, x + sw + 24, y + 12)) {
                    centerX = h.x();
                    centerZ = h.z();
                    zoom = Math.max(zoom, 1);
                    search.setFocused(false);
                    return true;
                }
                y += 12;
            }
        }
        if (drawer != null && mx >= width - RIGHT - drawer.width() && mx < width - RIGHT && my < height - STRIP) {
            drawer.mouseClicked(mx, my, button);
            return true;
        }
        if (button == 0 && clickLens(mx, my)) return true;
        if (button == 0 && clickRightRail(mx, my)) return true;
        if (mx >= width - RIGHT) return true;
        if (super.mouseClicked(mx, my, button)) return true;
        if (search.isFocused() && !Codex.inside(mx, my, search.getX(), search.getY(), search.getX() + search.getWidth(), search.getY() + search.getHeight())) {
            search.setFocused(false);
            setFocused(null);
        }
        if (mx > RAIL && mx < width - RIGHT && my < height - STRIP) {
            pressX = mx;
            pressY = my;
            pressButton = button;
            if (button == 0) panning = true;
            if (button == 1) {
                selX0 = selX1 = toWorldX(mx);
                selZ0 = selZ1 = toWorldZ(my);
            }
            return true;
        }
        return false;
    }

    private boolean clickLens(double mx, double my) {
        int sw = 120;
        int lx = width - RIGHT - sw - 40 + sw + 4;
        if (!Codex.inside(mx, my, lx, 4, lx + 20, 22)) return false;
        if (Features.LENSES.get()) {
            ClientConfig.Lens[] all = ClientConfig.Lens.values();
            ClientConfig.set(ClientConfig.LENS, all[(ClientConfig.LENS.get().ordinal() + 1) % all.length]);
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (drawer != null && drawer.mouseDragged(mx, my, button, dx, dy)) return true;
        if (button == 0 && panning) {
            centerX -= dx / zoom;
            centerZ -= dy / zoom;
            return true;
        }
        if (button == 1 && pressButton == 1) {
            selX1 = toWorldX(mx);
            selZ1 = toWorldZ(my);
            if (Math.abs(mx - pressX) + Math.abs(my - pressY) > 4) selecting = true;
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (drawer != null) drawer.mouseReleased(mx, my, button);
        boolean click = Math.abs(mx - pressX) + Math.abs(my - pressY) <= 4;
        if (button == 0 && panning) {
            panning = false;
            if (click && pressButton == 0) leftClickMap(mx, my);
        } else if (button == 1 && pressButton == 1) {
            if (selecting) openAreaMenu(mx, my);
            else openPointMenu(mx, my);
        }
        pressButton = -1;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        if (drawer != null && mx >= width - RIGHT - drawer.width() && drawer.mouseScrolled(mx, my, scrollY)) return true;
        if (mx < RAIL || mx > width - RIGHT) return false;
        double wx = toWorldX(mx);
        double wz = toWorldZ(my);
        zoom = Mth.clamp(zoom * (scrollY > 0 ? 1.25 : 0.8), MIN_ZOOM, MAX_ZOOM);
        centerX = wx - (mx - midX()) / zoom;
        centerZ = wz - (my - midY()) / zoom;
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 && menu != null) {
            menu = null;
            selecting = false;
            return true;
        }
        if (keyCode == 256 && drawer != null) {
            drawer = null;
            return true;
        }
        if (drawer != null && drawer.keyPressed(keyCode, scanCode, modifiers)) return true;
        if (drawer != null && drawer.typing()) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        if (drawer != null && drawer.charTyped(c, modifiers)) return true;
        return super.charTyped(c, modifiers);
    }

    /** Left click (no drag) on the map: open a claim's panel. */
    private void leftClickMap(double mx, double my) {
        if (!claimsOn()) return;
        int cx = Mth.floor(toWorldX(mx)) >> 4;
        int cz = Mth.floor(toWorldZ(my)) >> 4;
        ClaimsData.Claim c = ClaimsData.at(dim, cx, cz);
        if (c != null && !c.privateLand()) drawer = new ClaimPanel(this, c);
    }

    public void openDrawer(Drawer d) {
        this.drawer = d;
    }

    public ResourceLocation dimension() {
        return dim;
    }

    private int heightAt(int wx, int wz) {
        DimensionStore store = store();
        ChunkColumns col = store == null ? null : store.columns(wx, wz);
        if (col != null) return col.height()[((wz & 15) << 4) | (wx & 15)] + 1;
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null ? mc.player.getBlockY() : 64;
    }

    private void openPointMenu(double mx, double my) {
        int wx = Mth.floor(toWorldX(mx));
        int wz = Mth.floor(toWorldZ(my));
        int wy = heightAt(wx, wz);
        int cx = wx >> 4;
        int cz = wz >> 4;
        menu = new ContextMenu(Component.translatable("codex_cartography.map.chunk", cx, cz),
                Component.literal("X " + wx + "   Z " + wz));
        if (Features.MARKS.get()) {
            menu.add(Component.translatable("codex_cartography.menu.new_mark"), Tex.action("new_mark"), true, null, null,
                    () -> drawer = MarkEditor.create(this, dim, wx, wy, wz));
        }
        menu.add(Component.translatable("codex_cartography.menu.copy"), Tex.action("copy"), true, null, null,
                () -> Minecraft.getInstance().keyboardHandler.setClipboard(wx + " " + wy + " " + wz));
        if (ClientRules.travel) {
            boolean ok = ClientRules.canTravel && here();
            menu.add(Component.translatable("codex_cartography.menu.travel"), Tex.action("travel"), ok,
                    Component.translatable(here() ? "codex_cartography.menu.travel.no_permission" : "codex_cartography.menu.travel.other_dim"),
                    null, () -> ClientNet.travel(wx, wz));
        }
        if (claimsOn() && ClaimsData.present) {
            ClaimsData.Claim c = ClaimsData.at(dim, cx, cz);
            menu.divider();
            Component why = claimReason(LongArrayList.of(ChunkPos.asLong(cx, cz)), true);
            menu.add(Component.translatable("codex_cartography.menu.claim"), Tex.layer("claims", true), why == null, why,
                    ClaimsData.chunksUsed + " / " + ClaimsData.chunksAllowed,
                    () -> ClientNet.claim(dim, LongArrayList.of(ChunkPos.asLong(cx, cz)), true));
            boolean mine = c != null && (c.standing() == ClaimsData.OWN || c.canManage());
            menu.add(Component.translatable("codex_cartography.menu.unclaim"), Tex.layer("claims", false), mine,
                    Component.translatable(c == null ? "codex_cartography.menu.unclaim.none" : "codex_cartography.menu.unclaim.not_yours"),
                    null, () -> ClientNet.claim(dim, LongArrayList.of(ChunkPos.asLong(cx, cz)), false));
            if (c != null && !c.privateLand()) {
                menu.add(Component.translatable("codex_cartography.menu.open_claim"), Tex.layer("claims", true), true, null, null,
                        () -> drawer = new ClaimPanel(this, c));
            }
        }
        menu.place((int) mx, (int) my, width, height);
    }

    private void openAreaMenu(double mx, double my) {
        int[] r = selectionChunks();
        int count = (r[2] - r[0] + 1) * (r[3] - r[1] + 1);
        menu = new ContextMenu(Component.translatable("codex_cartography.menu.area", count),
                Component.literal("X " + r[0] * 16 + " → " + (r[2] * 16 + 15) + "   Z " + r[1] * 16 + " → " + (r[3] * 16 + 15)));
        if (claimsOn() && ClaimsData.present) {
            LongArrayList free = new LongArrayList();
            LongArrayList mine = new LongArrayList();
            for (int cz = r[1]; cz <= r[3]; cz++) {
                for (int cx = r[0]; cx <= r[2]; cx++) {
                    ClaimsData.Claim c = ClaimsData.at(dim, cx, cz);
                    if (c == null) free.add(ChunkPos.asLong(cx, cz));
                    else if (c.standing() == ClaimsData.OWN || c.canManage()) mine.add(ChunkPos.asLong(cx, cz));
                }
            }
            Component why = free.isEmpty() ? Component.translatable("codex_cartography.menu.area.none_free") : claimReason(free, false);
            int left = ClaimsData.chunksAllowed - ClaimsData.chunksUsed;
            menu.add(Component.translatable("codex_cartography.menu.claim_area"), Tex.layer("claims", true), why == null, why,
                    Component.translatable("codex_cartography.menu.uses", free.size(), Math.max(0, left)).getString(),
                    () -> ClientNet.claim(dim, free, true));
            menu.add(Component.translatable("codex_cartography.menu.unclaim_area"), Tex.layer("claims", false), !mine.isEmpty(),
                    Component.translatable("codex_cartography.menu.area.none_yours"), mine.isEmpty() ? null : String.valueOf(mine.size()),
                    () -> ClientNet.claim(dim, mine, false));
            menu.divider();
        }
        if (Features.PRINT.get()) {
            menu.add(Component.translatable("codex_cartography.menu.print_area"), Tex.action("print"), true, null, null,
                    () -> Minecraft.getInstance().setScreen(new PrintScreen(dim, new int[] {r[0] * 16, r[1] * 16, r[2] * 16 + 15, r[3] * 16 + 15})));
        }
        menu.add(Component.translatable("codex_cartography.menu.copy_corners"), Tex.action("copy"), true, null, null,
                () -> Minecraft.getInstance().keyboardHandler.setClipboard(r[0] * 16 + " " + r[1] * 16 + " " + (r[2] * 16 + 15) + " " + (r[3] * 16 + 15)));
        menu.place((int) mx, (int) my, width, height);
    }

    /** Why these chunks can't be claimed (shown greyed out), or null when they can. Claims makes the final call. */
    private Component claimReason(LongArrayList chunks, boolean single) {
        Minecraft mc = Minecraft.getInstance();
        if (!here()) return Component.translatable("codex_cartography.menu.claim.other_dim");
        int left = ClaimsData.chunksAllowed - ClaimsData.chunksUsed;
        if (chunks.size() > left) return Component.translatable("codex_cartography.menu.claim.limit", Math.max(0, left));
        if (mc.player != null) {
            ChunkPos p = mc.player.chunkPosition();
            for (long c : chunks) {
                if (Math.max(Math.abs(ChunkPos.getX(c) - p.x), Math.abs(ChunkPos.getZ(c) - p.z)) > ClaimsData.claimRange) {
                    return Component.translatable("codex_cartography.menu.claim.range", ClaimsData.claimRange);
                }
            }
        }
        if (single && ClaimsData.at(dim, ChunkPos.getX(chunks.getLong(0)), ChunkPos.getZ(chunks.getLong(0))) != null) {
            return Component.translatable("codex_cartography.menu.claim.taken");
        }
        return null;
    }

}
