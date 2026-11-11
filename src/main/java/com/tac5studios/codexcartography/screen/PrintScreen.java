package com.tac5studios.codexcartography.screen;

import com.mojang.blaze3d.platform.NativeImage;
import com.tac5studios.codexcartography.CodexCartography;
import com.tac5studios.codexcartography.claims.ClaimsData;
import com.tac5studios.codexcartography.client.Dims;
import com.tac5studios.codexcartography.client.MapIO;
import com.tac5studios.codexcartography.client.MapRegion;
import com.tac5studios.codexcartography.client.MapSession;
import com.tac5studios.codexcartography.client.RegionFile;
import com.tac5studios.codexcartography.config.ClientConfig;
import com.tac5studios.codexcartography.map.Painter;
import com.tac5studios.codexcartography.mark.Mark;
import com.tac5studios.codexcartography.ui.Codex;
import com.tac5studios.codexcartography.ui.Tex;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Print: saves the explored map of a dimension (or a chosen area) as a PNG in codex_cartography/prints/,
 * with the current lens, a scale, and claims, marks and the grid drawn in if wanted. Built in the background.
 */
public final class PrintScreen extends CodexScreen {

    private static final Pattern REGION = Pattern.compile("r\\.(-?\\d+)\\.(-?\\d+)\\.ccr");
    private static final int MAX_SIDE = 16384;
    private static final int[] SCALES = {1, 2, 4, 8};

    private ResourceLocation dim;
    private final int[] area;
    private int scaleIndex;
    private boolean claims = true;
    private boolean marks = true;
    private boolean grid;
    private final AtomicInteger progress = new AtomicInteger(-1);
    private volatile Component result;

    /** @param area minX, minZ, maxX, maxZ in blocks, or null for everything explored */
    public PrintScreen(ResourceLocation dim, int[] area) {
        super(Component.translatable("codex_cartography.section.print"));
        MapSession s = MapSession.get();
        this.dim = dim != null ? dim : s != null && s.dimension() != null ? s.dimension().location() : ResourceLocation.withDefaultNamespace("overworld");
        this.area = area;
    }

    @Override
    protected Section section() {
        return Section.PRINT;
    }

    @Override
    protected void renderPage(GuiGraphics g, int mouseX, int mouseY, float partial) {
        int x0 = RAIL + 10;
        header(g, x0, 6, 200, Tex.action("print"), 24, Component.translatable("codex_cartography.section.print"),
                Dims.name(dim));
        int y = 40;
        g.drawString(font, Component.translatable("codex_cartography.print.about"), x0, y, Codex.MUTE, false);
        y += 18;
        row(g, x0, y, "codex_cartography.print.area", area == null ? Component.translatable("codex_cartography.print.area_all")
                : Component.translatable("codex_cartography.print.area_box", area[0], area[1], area[2], area[3]));
        y += 18;
        row(g, x0, y, "codex_cartography.print.dimension", Dims.name(dim));
        y += 18;
        row(g, x0, y, "codex_cartography.print.lens", Component.translatable("codex_cartography.lens." + ClientConfig.LENS.get().name().toLowerCase(Locale.ROOT)));
        y += 18;
        row(g, x0, y, "codex_cartography.print.scale", Component.translatable("codex_cartography.print.scale_value", SCALES[scaleIndex]));
        y += 18;
        MarkEditor.toggle(g, x0, y, claims, "codex_cartography.print.claims");
        MarkEditor.toggle(g, x0, y + 14, marks, "codex_cartography.print.marks");
        MarkEditor.toggle(g, x0, y + 28, grid, "codex_cartography.print.grid");
        y += 50;
        int p = progress.get();
        boolean busy = p >= 0 && p < 100;
        g.fill(x0, y, x0 + 120, y + 20, busy ? Codex.CARD : Codex.CARD_ON);
        Codex.brackets(g, x0, y, x0 + 120, y + 20, 5, Codex.accent());
        Codex.tex(g, Tex.action("print"), x0 + 4, y + 2, 16, 16, 24, 24);
        g.drawString(font, Component.translatable(busy ? "codex_cartography.print.working" : "codex_cartography.print.go"), x0 + 24, y + 6, Codex.accent(), false);
        if (busy) {
            g.fill(x0, y + 26, x0 + 200, y + 30, Codex.CARD);
            g.fill(x0, y + 26, x0 + 2 * p, y + 30, Codex.accent());
        }
        if (result != null) g.drawString(font, result, x0, y + 36, Codex.TEXT, false);
    }

    private void row(GuiGraphics g, int x, int y, String key, Component value) {
        g.drawString(font, Component.translatable(key), x, y, Codex.MUTE, false);
        g.drawString(font, value, x + 90, y, Codex.TEXT, false);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        int x0 = RAIL + 10;
        int y = 58;
        if (Codex.inside(mx, my, x0, y + 18, x0 + 300, y + 30)) {
            List<ResourceLocation> all = dimensions();
            dim = all.get((all.indexOf(dim) + 1) % all.size());
        } else if (Codex.inside(mx, my, x0, y + 36, x0 + 300, y + 48)) {
            ClientConfig.Lens[] l = ClientConfig.Lens.values();
            ClientConfig.set(ClientConfig.LENS, l[(ClientConfig.LENS.get().ordinal() + 1) % l.length]);
        } else if (Codex.inside(mx, my, x0, y + 54, x0 + 300, y + 66)) {
            scaleIndex = (scaleIndex + 1) % SCALES.length;
        } else if (Codex.inside(mx, my, x0, y + 72, x0 + 200, y + 82)) {
            claims = !claims;
        } else if (Codex.inside(mx, my, x0, y + 86, x0 + 200, y + 96)) {
            marks = !marks;
        } else if (Codex.inside(mx, my, x0, y + 100, x0 + 200, y + 110)) {
            grid = !grid;
        } else if (Codex.inside(mx, my, x0, y + 122, x0 + 120, y + 142)) {
            int p = progress.get();
            if (p < 0 || p >= 100) start();
        }
        return true;
    }

    /** Mark icon size on a print, in pixels (the same at every scale, so marks stay readable). */
    private static final int ICON = 28;

    /** A texture's pixels (64 by 64 for mark art, 128 by 128 for the font), or null when it can't be read. */
    private static int[] pixels(ResourceLocation id) {
        try {
            var res = Minecraft.getInstance().getResourceManager().getResource(id);
            if (res.isEmpty()) return null;
            try (InputStream in = res.get().open(); NativeImage img = NativeImage.read(in)) {
                int w = img.getWidth();
                int h = img.getHeight();
                int[] out = new int[w * h + 1];
                out[w * h] = w; // width kept at the end
                for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) out[y * w + x] = img.getPixelRGBA(x, y);
                return out;
            }
        } catch (IOException e) {
            return null;
        }
    }

    private static int width(int[] px) { return px[px.length - 1]; }

    /** Blends one ABGR pixel over the print. */
    private static void blend(NativeImage img, int x, int y, int abgr) {
        if (x < 0 || y < 0 || x >= img.getWidth() || y >= img.getHeight()) return;
        int a = abgr >>> 24;
        if (a == 0) return;
        if (a == 255) {
            img.setPixelRGBA(x, y, abgr);
            return;
        }
        int under = img.getPixelRGBA(x, y);
        float t = a / 255f;
        int r = (int) ((abgr & 0xFF) * t + (under & 0xFF) * (1 - t));
        int g = (int) (((abgr >> 8) & 0xFF) * t + ((under >> 8) & 0xFF) * (1 - t));
        int b = (int) (((abgr >> 16) & 0xFF) * t + ((under >> 16) & 0xFF) * (1 - t));
        img.setPixelRGBA(x, y, 0xFF000000 | (b << 16) | (g << 8) | r);
    }

    /** Draws mark art centered on (cx, cy), ICON pixels across, tinted with an RGB color (white = as is). */
    private static void stamp(NativeImage img, int[] px, int cx, int cy, int rgb) {
        if (px == null) return;
        int size = ICON;
        int sw = width(px);
        int sh = (px.length - 1) / sw;
        float tr = ((rgb >> 16) & 0xFF) / 255f;
        float tg = ((rgb >> 8) & 0xFF) / 255f;
        float tb = (rgb & 0xFF) / 255f;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int p = px[(y * sh / size) * sw + (x * sw / size)];
                int a = p >>> 24;
                if (a == 0) continue;
                int r = (int) ((p & 0xFF) * tr);
                int g = (int) (((p >> 8) & 0xFF) * tg);
                int b = (int) (((p >> 16) & 0xFF) * tb);
                blend(img, cx - size / 2 + x, cy - size / 2 + y, (a << 24) | (b << 16) | (g << 8) | r);
            }
        }
    }

    /** Writes a mark's name centered under it with the game's font (doubled, with a dark outline). Other letters show as '?'. */
    private static void label(NativeImage img, int[] font, String text, int cx, int top) {
        int fw = width(font);
        int cell = fw / 16;
        int[] widths = new int[text.length()];
        int total = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c > 255) c = '?';
            widths[i] = c == ' ' ? 4 : glyphWidth(font, fw, cell, c);
            total += (widths[i] + 1) * 2;
        }
        int x = cx - total / 2;
        for (int pass = 0; pass < 2; pass++) {
            int pen = x;
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c > 255) c = '?';
                int gx = (c % 16) * cell;
                int gy = (c / 16) * cell;
                for (int y = 0; y < cell; y++) {
                    for (int xx = 0; xx < widths[i]; xx++) {
                        if ((font[(gy + y) * fw + gx + xx] >>> 24) == 0) continue;
                        for (int dy = 0; dy < 2; dy++) {
                            for (int dx = 0; dx < 2; dx++) {
                                int px = pen + xx * 2 + dx;
                                int py = top + y * 2 + dy;
                                if (pass == 0) {
                                    // outline: the dark edge around every lit pixel
                                    for (int oy = -1; oy <= 1; oy++) for (int ox = -1; ox <= 1; ox++) blend(img, px + ox, py + oy, 0xE0161210);
                                } else {
                                    blend(img, px, py, 0xFFFFFFFF);
                                }
                            }
                        }
                    }
                }
                pen += (widths[i] + 1) * 2;
            }
        }
    }

    /** How wide a glyph is: its rightmost lit column plus one. */
    private static int glyphWidth(int[] font, int fw, int cell, char c) {
        int gx = (c % 16) * cell;
        int gy = (c / 16) * cell;
        for (int x = cell - 1; x >= 0; x--) {
            for (int y = 0; y < cell; y++) if ((font[(gy + y) * fw + gx + x] >>> 24) != 0) return x + 1;
        }
        return 3;
    }

    private void start() {
        MapSession s = MapSession.get();
        if (s == null) return;
        if (s.surface() != null) s.surface().saveDirty();
        Path dimDir = s.worldDir().resolve(Dims.folder(dim));
        Path out = MapSession.root().resolve("prints");
        String file = s.world().folder() + "_" + Dims.folder(dim) + "_" + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date()) + ".png";
        int scale = SCALES[scaleIndex];
        ClientConfig.Lens lens = ClientConfig.LENS.get();
        List<Mark> markList = marks ? new ArrayList<>(s.marks().mapIn(dim)) : List.of();
        List<ClaimsData.Claim> claimList = claims ? new ArrayList<>(ClaimsData.in(dim)) : List.of();
        boolean drawGrid = grid;
        int[] box = area;
        // mark art and the game font, read now on the main thread; drawn into the print in the background
        Map<ResourceLocation, int[]> art = new HashMap<>();
        for (Mark m : markList) {
            if (m.kind == Mark.Kind.DEATH) art.computeIfAbsent(Tex.DEATH, PrintScreen::pixels);
            else if (m.emblem.equals("danger")) art.computeIfAbsent(Tex.DANGER, PrintScreen::pixels);
            else {
                art.computeIfAbsent(Tex.BADGE, PrintScreen::pixels);
                art.computeIfAbsent(Tex.emblem(m.emblem), PrintScreen::pixels);
            }
        }
        int[] fontPixels = markList.isEmpty() ? null : pixels(ResourceLocation.withDefaultNamespace("textures/font/ascii.png"));
        progress.set(0);
        result = null;
        MapIO.run("print map", () -> {
            List<int[]> regions = new ArrayList<>();
            if (Files.isDirectory(dimDir)) {
                try (Stream<Path> files = Files.list(dimDir)) {
                    for (Path f : files.toList()) {
                        Matcher m = REGION.matcher(f.getFileName().toString());
                        if (m.matches()) regions.add(new int[] {Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2))});
                    }
                }
            }
            if (regions.isEmpty()) {
                result = Component.translatable("codex_cartography.print.nothing");
                progress.set(100);
                return;
            }
            int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
            for (int[] r : regions) {
                minX = Math.min(minX, r[0] * 512);
                minZ = Math.min(minZ, r[1] * 512);
                maxX = Math.max(maxX, r[0] * 512 + 511);
                maxZ = Math.max(maxZ, r[1] * 512 + 511);
            }
            if (box != null) {
                minX = box[0];
                minZ = box[1];
                maxX = box[2];
                maxZ = box[3];
            }
            int w = (maxX - minX + 1) / scale;
            int h = (maxZ - minZ + 1) / scale;
            if (w > MAX_SIDE || h > MAX_SIDE || w <= 0 || h <= 0) {
                result = Component.translatable("codex_cartography.print.too_big", MAX_SIDE);
                progress.set(100);
                return;
            }
            try (NativeImage img = new NativeImage(w, h, true); NativeImage tile = new NativeImage(512, 512, true)) {
                img.fillRect(0, 0, w, h, Painter.abgr(0x0C1014));
                int done = 0;
                for (int[] r : regions) {
                    done++;
                    progress.set(Math.min(99, done * 100 / regions.size()));
                    int rx0 = r[0] * 512;
                    int rz0 = r[1] * 512;
                    if (rx0 > maxX || rz0 > maxZ || rx0 + 511 < minX || rz0 + 511 < minZ) continue;
                    MapRegion region = RegionFile.read(dimDir, r[0], r[1]);
                    for (int slot = 0; slot < MapRegion.CHUNKS * MapRegion.CHUNKS; slot++) Painter.paintChunk(region, slot, tile, lens, false);
                    for (int z = 0; z < 512; z += scale) {
                        int iz = (rz0 + z - minZ) / scale;
                        if (iz < 0 || iz >= h) continue;
                        for (int x = 0; x < 512; x += scale) {
                            int ix = (rx0 + x - minX) / scale;
                            if (ix < 0 || ix >= w) continue;
                            int c = tile.getPixelRGBA(x, z);
                            if ((c >>> 24) == 0) continue;
                            int rgb = Painter.rgbOf(c);
                            int bx = rx0 + x;
                            int bz = rz0 + z;
                            if (drawGrid && ((bx & 15) == 0 || (bz & 15) == 0)) rgb = Codex.mix(rgb, 0xFFFFFF, 0.15f);
                            img.setPixelRGBA(ix, iz, Painter.abgr(rgb));
                        }
                    }
                }
                for (ClaimsData.Claim c : claimList) {
                    int color = c.drawColor();
                    for (long ch : c.chunks()) {
                        int cx0 = (ChunkPos.getX(ch) * 16 - minX) / scale;
                        int cz0 = (ChunkPos.getZ(ch) * 16 - minZ) / scale;
                        int size = Math.max(1, 16 / scale);
                        for (int z = 0; z < size; z++) {
                            for (int x = 0; x < size; x++) {
                                int ix = cx0 + x;
                                int iz = cz0 + z;
                                if (ix < 0 || iz < 0 || ix >= w || iz >= h) continue;
                                boolean edge = x == 0 || z == 0 || x == size - 1 || z == size - 1;
                                int base = Painter.rgbOf(img.getPixelRGBA(ix, iz));
                                img.setPixelRGBA(ix, iz, Painter.abgr(edge ? color : Codex.mix(base, color, 0.25f)));
                            }
                        }
                    }
                }
                for (Mark m : markList) {
                    int ix = (m.x - minX) / scale;
                    int iz = (m.z - minZ) / scale;
                    if (m.kind == Mark.Kind.DEATH) stamp(img, art.get(Tex.DEATH), ix, iz, 0xFFFFFF);
                    else if (m.emblem.equals("danger")) stamp(img, art.get(Tex.DANGER), ix, iz, 0xFFFFFF);
                    else {
                        stamp(img, art.get(Tex.BADGE), ix, iz, m.drawColor());
                        stamp(img, art.get(Tex.emblem(m.emblem)), ix, iz, 0xFFFFFF);
                    }
                    if (fontPixels != null) label(img, fontPixels, m.name, ix, iz + ICON / 2 + 3);
                }
                Files.createDirectories(out);
                img.writeToFile(out.resolve(file));
                result = Component.translatable("codex_cartography.print.saved", "codex_cartography/prints/" + file);
            } catch (IOException e) {
                CodexCartography.LOGGER.warn("[Cartography] Print failed: {}", e.toString());
                result = Component.translatable("codex_cartography.print.failed");
            }
            progress.set(100);
        });
    }
}
