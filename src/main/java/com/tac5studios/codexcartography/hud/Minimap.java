package com.tac5studios.codexcartography.hud;

import com.mojang.blaze3d.platform.NativeImage;
import com.tac5studios.codexcartography.CodexCartography;
import com.tac5studios.codexcartography.claims.ClaimsData;
import com.tac5studios.codexcartography.client.ClientRules;
import com.tac5studios.codexcartography.client.DimensionStore;
import com.tac5studios.codexcartography.client.MapRegion;
import com.tac5studios.codexcartography.client.MapSession;
import com.tac5studios.codexcartography.client.OverlapGuard;
import com.tac5studios.codexcartography.config.ClientConfig;
import com.tac5studios.codexcartography.config.Features;
import com.tac5studios.codexcartography.map.Painter;
import com.tac5studios.codexcartography.map.RegionImages;
import com.tac5studios.codexcartography.mark.Mark;
import com.tac5studios.codexcartography.client.SlimeChunks;
import com.tac5studios.codexcartography.client.WorldBorders;
import com.tac5studios.codexcartography.ui.Codex;
import com.tac5studios.codexcartography.ui.Tex;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.border.WorldBorder;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The minimap. The map picture is put together on the CPU from the painted region images (never from chunks),
 * at most once a tick, then drawn with the frame, arrows, marks and Sweep on top.
 */
public final class Minimap {

    private static final ResourceLocation TEXTURE_ID = CodexCartography.id("minimap");
    /** The map hole inside a frame image: 18 px band on a 256 px frame, plate corners cut by 36 of 220. */
    public static final float HOLE_INSET = 18f / 256f;
    public static final float HOLE_SIZE = 220f / 256f;
    static final float PLATE_CUT = 36f / 220f;

    private static NativeImage image;
    private static DynamicTexture texture;
    private static int texSize;
    private static long lastCompose = -1;
    private static long lastSignature;
    private static int[] fog;

    private Minimap() {}

    public static boolean visible(Minecraft mc) {
        if (mc.player == null || mc.level == null || mc.options.hideGui) return false;
        if (!ClientConfig.MINIMAP_ON.get() || !OverlapGuard.minimapOn() || !Features.MINIMAP.get()) return false;
        if (mc.getDebugOverlay().showDebugScreen()) return false;
        if (mc.screen != null && !(mc.screen instanceof ChatScreen) && ClientConfig.HIDE_IN_MENUS.get()) return false;
        return MapSession.get() != null && MapSession.get().surface() != null;
    }

    /** GUI layer entry point. */
    public static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (!visible(mc)) return;
        int size = ClientConfig.MINIMAP_SIZE.get();
        int x = Math.min(ClientConfig.MINIMAP_X.get(), g.guiWidth() - size);
        int y = Math.min(ClientConfig.MINIMAP_Y.get(), g.guiHeight() - size - 30);
        draw(g, mc, x, y, size, delta.getGameTimeDeltaPartialTick(false));
    }

    /** Draws the minimap at a position (also used by the layout editor and the Settings preview). */
    public static void draw(GuiGraphics g, Minecraft mc, int x, int y, int size, float partial) {
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        MapSession session = MapSession.get();
        if (player == null || level == null || session == null || session.surface() == null) return;
        ClientConfig.Shape shape = ClientConfig.SHAPE.get();
        float holeX = x + size * HOLE_INSET;
        float holeY = y + size * HOLE_INSET;
        float hole = size * HOLE_SIZE;
        double zoom = ClientConfig.MINIMAP_ZOOM.get();
        boolean rotate = ClientConfig.ROTATE.get();
        double px = Mth.lerp(partial, player.xo, player.getX());
        double pz = Mth.lerp(partial, player.zo, player.getZ());
        float yaw = player.getViewYRot(partial);

        compose(mc, level, session.active(), player, Math.max(96, Math.min(512, Math.round(hole * 2))), zoom, rotate, shape);
        Codex.tinted(g, TEXTURE_ID, Math.round(holeX), Math.round(holeY), Math.round(hole), Math.round(hole), texSize, texSize, 0xFFFFFFFF);

        Projection proj = new Projection(px, pz, zoom, rotate ? yaw : 180f, holeX + hole / 2f, holeY + hole / 2f, hole / 2f, shape);
        drawOverlays(g, level, player, session, proj, partial);
        if (ClientConfig.SCAN_LINE.get() && ClientConfig.ANIMATIONS.get()) scanLine(g, proj);
        Codex.tex(g, Tex.frame(shape.name().toLowerCase(java.util.Locale.ROOT)), x, y, size, size, 256, 256);

        float arrowAngle = rotate ? 0f : yaw + 180f;
        Codex.texAt(g, Tex.PLAYER, proj.cx, proj.cy, Math.max(8, size / 9f), arrowAngle, 32, 0xFFFFFFFF);
        infoLines(g, level, player, x, y + size + 2, size);
    }

    /** Builds the map picture for this tick (skipped when nothing moved). */
    private static void compose(Minecraft mc, ClientLevel level, DimensionStore store, LocalPlayer player, int s,
                                double zoom, boolean rotate, ClientConfig.Shape shape) {
        long tick = level.getGameTime();
        long sig = ((long) player.getBlockX() * 31 + player.getBlockZ()) * 31 + (rotate ? (int) (player.getYRot() / 2) : 0);
        sig = sig * 31 + Double.hashCode(zoom) + s * 7L + shape.ordinal() + store.hashCode() + ClientConfig.LENS.get().ordinal() * 13L;
        if (image != null && texSize == s && (tick == lastCompose || (sig == lastSignature && tick - lastCompose < 10))) return;
        lastCompose = tick;
        lastSignature = sig;
        if (image == null || texSize != s) {
            if (texture != null) mc.getTextureManager().release(TEXTURE_ID);
            image = new NativeImage(s, s, true);
            texture = new DynamicTexture(image);
            mc.getTextureManager().register(TEXTURE_ID, texture);
            texSize = s;
        }
        if (fog == null) fog = loadFog(mc);

        double k = 1.0 / (zoom * (s / (ClientConfig.MINIMAP_SIZE.get() * HOLE_SIZE)));
        double cx = player.getX();
        double cz = player.getZ();
        double yawRad = Math.toRadians(rotate ? player.getYRot() : 180f);
        double fx = -Math.sin(yawRad);
        double fz = Math.cos(yawRad);
        double rx = -fz;
        int alpha = (int) (ClientConfig.OPACITY.get() * 255);
        float half = s / 2f;
        float cut = (2 - 2 * PLATE_CUT) * half;
        boolean grid = ClientConfig.LAYER_GRID.get() && zoom >= 1;
        boolean claims = Features.CLAIMS_LAYER.get() && ClientRules.claims && ClientConfig.LAYER_CLAIMS.get() && !ClaimsData.isEmpty();
        boolean slime = ClientConfig.LAYER_SLIME.get() && ClientRules.slimeChunks && SlimeChunks.any();
        WorldBorder border = ClientConfig.BORDER_MINIMAP.get() && ClientRules.worldBorder ? WorldBorders.current(level) : null;
        int borderColor = ClientConfig.BORDER_COLOR.get();
        RegionImages images = store.images();
        int lastRx = Integer.MIN_VALUE;
        int lastRz = Integer.MIN_VALUE;
        RegionImages.Picture pic = null;

        for (int ty = 0; ty < s; ty++) {
            float dy = ty - half + 0.5f;
            for (int tx = 0; tx < s; tx++) {
                float dx = tx - half + 0.5f;
                if (!inShape(shape, dx, dy, half, cut)) {
                    image.setPixelRGBA(tx, ty, 0);
                    continue;
                }
                double wxd = cx + (dx * rx - dy * fx) * k;
                double wzd = cz + (dx * fx - dy * fz) * k;
                int wx = Mth.floor(wxd);
                int wz = Mth.floor(wzd);
                int rgx = wx >> 9;
                int rgz = wz >> 9;
                if (rgx != lastRx || rgz != lastRz) {
                    lastRx = rgx;
                    lastRz = rgz;
                    MapRegion r = store.region(rgx, rgz);
                    pic = r == null ? null : images.picture(r);
                }
                int c = pic == null ? 0 : pic.image.getPixelRGBA(wx & 511, wz & 511);
                int rgb;
                if ((c >>> 24) == 0) {
                    rgb = fog[((ty >> 1) & 63) * 64 + ((tx >> 1) & 63)];
                } else {
                    rgb = Painter.rgbOf(c);
                    if (grid && ((wx & 15) == 0 || (wz & 15) == 0)) rgb = Codex.mix(rgb, 0xFFFFFF, 0.12f);
                    if (slime && SlimeChunks.isSlime(wx >> 4, wz >> 4)) rgb = Codex.mix(rgb, 0x78DC6E, 0.35f);
                    if (claims) rgb = ClaimsData.tint(rgb, wx, wz, wxd, wzd, k);
                }
                if (border != null) {
                    double dist = border.getDistanceToBorder(wxd, wzd);
                    if (dist < 0) rgb = Codex.mix(Codex.scale(rgb, 0.45f), borderColor, 0.25f);
                    else if (dist < k * 1.5) rgb = borderColor;
                }
                image.setPixelRGBA(tx, ty, (alpha << 24) | (Painter.abgr(rgb) & 0xFFFFFF));
            }
        }
        texture.upload();
    }

    static boolean inShape(ClientConfig.Shape shape, float dx, float dy, float half, float cut) {
        return switch (shape) {
            case SQUARE -> Math.abs(dx) <= half && Math.abs(dy) <= half;
            case ROUND -> dx * dx + dy * dy <= half * half;
            case PLATE -> Math.abs(dx) <= half && Math.abs(dy) <= half && Math.abs(dx) + Math.abs(dy) <= cut;
        };
    }

    private static int[] loadFog(Minecraft mc) {
        int[] out = new int[64 * 64];
        try {
            Optional<net.minecraft.server.packs.resources.Resource> res = mc.getResourceManager().getResource(Tex.FOG);
            if (res.isPresent()) {
                try (InputStream in = res.get().open(); NativeImage img = NativeImage.read(in)) {
                    for (int y = 0; y < 64; y++) {
                        for (int x = 0; x < 64; x++) out[y * 64 + x] = Codex.scale(Painter.rgbOf(img.getPixelRGBA(x % img.getWidth(), y % img.getHeight())), 0.6f);
                    }
                    return out;
                }
            }
        } catch (Exception e) {
            CodexCartography.LOGGER.warn("[Cartography] Couldn't read the fog texture: {}", e.toString());
        }
        java.util.Arrays.fill(out, 0x0C1014);
        return out;
    }

    /** Marks, players, the Sweep and server markers on top of the map. */
    private static void drawOverlays(GuiGraphics g, ClientLevel level, LocalPlayer player, MapSession session,
                                     Projection proj, float partial) {
        ResourceLocation dim = level.dimension().location();
        float iconSize = Math.max(7, proj.radius / 5f);
        if (Features.MARKS.get()) {
            for (Mark m : session.marks().visibleIn(dim)) {
                float[] p = proj.toScreenClamped(m.x() + 0.5, m.z() + 0.5);
                m.drawBadge(g, p[0], p[1], iconSize);
            }
        }
        ServerMarkersOverlay.drawOnMinimap(g, proj, dim, iconSize);
        Sweep.draw(g, level, player, proj, partial);
        if (ClientConfig.SWEEP_PULSE.get() && ClientConfig.ANIMATIONS.get() && Sweep.anyOn()) {
            float t = (level.getGameTime() % 80 + partial) / 80f;
            if (t < 0.5f) {
                float r = proj.radius * t * 2;
                int a = (int) (120 * (1 - t * 2));
                ring(g, proj.cx, proj.cy, r, Codex.accent(a));
            }
        }
    }

    private static void ring(GuiGraphics g, float cx, float cy, float r, int color) {
        int steps = Math.max(16, (int) r * 2);
        for (int i = 0; i < steps; i++) {
            double a = i * Math.PI * 2 / steps;
            int x = Math.round(cx + (float) Math.cos(a) * r);
            int y = Math.round(cy + (float) Math.sin(a) * r);
            g.fill(x, y, x + 1, y + 1, color);
        }
    }

    private static void scanLine(GuiGraphics g, Projection proj) {
        long ms = net.minecraft.Util.getMillis() % 9000L;
        if (ms > 1400) return;
        float t = ms / 1400f;
        float dy = -proj.radius + t * proj.radius * 2;
        float halfW = switch (proj.shape) {
            case SQUARE -> proj.radius;
            case ROUND -> (float) Math.sqrt(Math.max(0, proj.radius * proj.radius - dy * dy));
            case PLATE -> Math.min(proj.radius, (2 - 2 * PLATE_CUT) * proj.radius - Math.abs(dy));
        };
        int y = Math.round(proj.cy + dy);
        int fade = (int) (200 * Math.sin(t * Math.PI));
        g.fill(Math.round(proj.cx - halfW), y, Math.round(proj.cx + halfW), y + 1, Codex.accent(fade));
        g.fill(Math.round(proj.cx - halfW), y - 1, Math.round(proj.cx + halfW), y, Codex.accent(fade / 3));
    }

    private static void infoLines(GuiGraphics g, ClientLevel level, LocalPlayer player, int x, int y, int size) {
        List<Component> lines = new ArrayList<>();
        BlockPos pos = player.blockPosition();
        if (ClientConfig.INFO_COORDS.get()) lines.add(Component.literal(pos.getX() + ", " + pos.getY() + ", " + pos.getZ()));
        if (ClientConfig.INFO_BIOME.get()) {
            level.getBiome(pos).unwrapKey().ifPresent(k -> lines.add(Component.translatable("biome." + k.location().getNamespace() + "." + k.location().getPath())));
        }
        if (ClientConfig.INFO_TIME.get()) {
            long t = (level.getDayTime() + 6000) % 24000;
            lines.add(Component.literal(String.format("%02d:%02d", t / 1000, (t % 1000) * 60 / 1000)));
        }
        if (ClientConfig.INFO_LIGHT.get()) {
            lines.add(Component.translatable("codex_cartography.minimap.light", level.getBrightness(LightLayer.BLOCK, pos)));
        }
        if (ClientConfig.INFO_BORDER.get() && ClientRules.worldBorder) {
            double d = level.getWorldBorder().getDistanceToBorder(player);
            if (d < ClientConfig.BORDER_WARN.get()) lines.add(Component.translatable("codex_cartography.minimap.border", (int) d));
        }
        MapSession session = MapSession.get();
        if (session != null && session.depthOn()) lines.addFirst(Component.translatable("codex_cartography.minimap.depth").withColor(Codex.accent() & 0xFFFFFF));
        int cx = x + size / 2;
        for (Component line : lines) {
            Codex.label(g, line, cx, y, Codex.TEXT);
            y += 10;
        }
    }
}
