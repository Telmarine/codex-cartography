package com.tac5studios.codexcartography.map;

import com.mojang.blaze3d.platform.NativeImage;
import com.tac5studios.codexcartography.CodexCartography;
import com.tac5studios.codexcartography.client.MapIO;
import com.tac5studios.codexcartography.client.MapRegion;
import com.tac5studios.codexcartography.config.ClientConfig;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * The painted pictures of one dimension's regions: a 512 x 512 texture per loaded region (minimap and world map
 * draw from these, never from chunks), plus small 64 x 64 overview tiles saved to disk for far zoom.
 */
public final class RegionImages {

    public static final int SIZE = 512;
    public static final int OVERVIEW = 64;
    private static int nextId;

    private final int id = nextId++;
    private final Path overviewDir;
    private final boolean depth;
    private final Long2ObjectOpenHashMap<Picture> pictures = new Long2ObjectOpenHashMap<>();
    private final LongLinkedOpenHashSet repaint = new LongLinkedOpenHashSet();
    private final Long2ObjectOpenHashMap<Overview> overviews = new Long2ObjectOpenHashMap<>();
    private final LongOpenHashSet overviewMissing = new LongOpenHashSet();
    private final LongOpenHashSet overviewLoading = new LongOpenHashSet();
    private final ConcurrentLinkedQueue<LoadedOverview> overviewLoaded = new ConcurrentLinkedQueue<>();
    private ClientConfig.Lens paintedLens = ClientConfig.LENS.get();
    private ClientConfig.Palette paintedPalette = ClientConfig.PALETTE.get();

    public RegionImages(Path dimDir, boolean depth) {
        this.overviewDir = dimDir.resolve("overview");
        this.depth = depth;
    }

    /** A painted region: the CPU image (read by the minimap) and its texture (drawn by the world map). */
    public static final class Picture {
        public final NativeImage image;
        public final DynamicTexture texture;
        public final ResourceLocation id;
        boolean dirty;

        Picture(NativeImage image, ResourceLocation id) {
            this.image = image;
            this.texture = new DynamicTexture(image);
            this.id = id;
            Minecraft.getInstance().getTextureManager().register(id, texture);
        }
    }

    private record Overview(DynamicTexture texture, ResourceLocation id) {}

    private record LoadedOverview(long key, NativeImage image) {}

    /** The picture of a loaded region, painting it the first time. */
    public Picture picture(MapRegion region) {
        long key = ChunkPos.asLong(region.rx(), region.rz());
        Picture p = pictures.get(key);
        if (p == null) {
            p = new Picture(new NativeImage(SIZE, SIZE, true),
                    CodexCartography.id("map/" + id + "/" + key(region.rx()) + "_" + key(region.rz())));
            for (int slot = 0; slot < MapRegion.CHUNKS * MapRegion.CHUNKS; slot++) {
                Painter.paintChunk(region, slot, p.image, paintedLens, depth);
            }
            p.texture.upload();
            pictures.put(key, p);
        }
        return p;
    }

    /** The picture if it's already painted, without painting. */
    public Picture existing(int rx, int rz) {
        return pictures.get(ChunkPos.asLong(rx, rz));
    }

    private static String key(int v) {
        return v < 0 ? "m" + (-v) : String.valueOf(v);
    }

    /** A chunk changed: repaint it, and the chunks south and east of it, whose shading and shorelines read this one. */
    public void chunkChanged(MapRegion region, int slot) {
        Picture p = pictures.get(ChunkPos.asLong(region.rx(), region.rz()));
        if (p == null) return;
        Painter.paintChunk(region, slot, p.image, paintedLens, depth);
        if (slot + MapRegion.CHUNKS < MapRegion.CHUNKS * MapRegion.CHUNKS) {
            Painter.paintChunk(region, slot + MapRegion.CHUNKS, p.image, paintedLens, depth);
        }
        if (slot % MapRegion.CHUNKS < MapRegion.CHUNKS - 1) {
            Painter.paintChunk(region, slot + 1, p.image, paintedLens, depth);
        }
        p.dirty = true;
    }

    /** Uploads changed pictures, takes in loaded overview tiles and repaints after a lens or palette change. */
    @SuppressWarnings("resource") // loaded images are kept and closed with the overview
    public void tick(java.util.function.LongFunction<MapRegion> regions) {
        ClientConfig.Lens lens = ClientConfig.LENS.get();
        ClientConfig.Palette palette = ClientConfig.PALETTE.get();
        if (lens != paintedLens || palette != paintedPalette) {
            paintedLens = lens;
            paintedPalette = palette;
            repaint.addAll(pictures.keySet());
        }
        int budget = 2;
        while (!repaint.isEmpty() && budget-- > 0) {
            long key = repaint.removeFirstLong();
            Picture p = pictures.get(key);
            MapRegion r = regions.apply(key);
            if (p == null || r == null) continue;
            for (int slot = 0; slot < MapRegion.CHUNKS * MapRegion.CHUNKS; slot++) {
                Painter.paintChunk(r, slot, p.image, paintedLens, depth);
            }
            p.dirty = true;
        }
        for (Picture p : pictures.values()) {
            if (p.dirty) {
                p.texture.upload();
                p.dirty = false;
            }
        }
        LoadedOverview lo;
        while ((lo = overviewLoaded.poll()) != null) {
            overviewLoading.remove(lo.key());
            if (lo.image() == null) {
                overviewMissing.add(lo.key());
                continue;
            }
            putOverview(lo.key(), lo.image());
        }
    }

    /** Repaints everything (palette changed). */
    public void repaintAll() {
        repaint.addAll(pictures.keySet());
    }

    /** Frees a region's picture when the region leaves memory; its overview tile is saved first. */
    public void release(int rx, int rz) {
        long key = ChunkPos.asLong(rx, rz);
        Picture p = pictures.remove(key);
        if (p == null) return;
        saveOverview(rx, rz, p);
        Minecraft.getInstance().getTextureManager().release(p.id);
    }

    public void releaseAll() {
        for (long key : pictures.keySet().toLongArray()) release(ChunkPos.getX(key), ChunkPos.getZ(key));
        for (Overview o : overviews.values()) Minecraft.getInstance().getTextureManager().release(o.id());
        overviews.clear();
    }

    /** Saves the overview tile of a loaded region (called when the region is saved). */
    public void saveOverview(int rx, int rz) {
        Picture p = pictures.get(ChunkPos.asLong(rx, rz));
        if (p != null) saveOverview(rx, rz, p);
    }

    private void saveOverview(int rx, int rz, Picture p) {
        if (!ClientConfig.OVERVIEW_TILES.get() || depth) return;
        NativeImage small = shrink(p.image);
        long key = ChunkPos.asLong(rx, rz);
        overviewMissing.remove(key);
        Overview old = overviews.remove(key);
        if (old != null) Minecraft.getInstance().getTextureManager().release(old.id());
        NativeImage copy = new NativeImage(OVERVIEW, OVERVIEW, true);
        copy.copyFrom(small);
        putOverview(key, copy);
        Path file = overviewDir.resolve("r." + rx + "." + rz + ".png");
        MapIO.run("save overview tile", () -> {
            try (small) {
                Files.createDirectories(overviewDir);
                small.writeToFile(file);
            }
        });
    }

    private static NativeImage shrink(NativeImage big) {
        NativeImage small = new NativeImage(OVERVIEW, OVERVIEW, true);
        int step = SIZE / OVERVIEW;
        for (int y = 0; y < OVERVIEW; y++) {
            for (int x = 0; x < OVERVIEW; x++) {
                small.setPixelRGBA(x, y, big.getPixelRGBA(x * step + step / 2, y * step + step / 2));
            }
        }
        return small;
    }

    private void putOverview(long key, NativeImage image) {
        ResourceLocation rl = CodexCartography.id("overview/" + id + "/" + key(ChunkPos.getX(key)) + "_" + key(ChunkPos.getZ(key)));
        DynamicTexture tex = new DynamicTexture(image);
        Minecraft.getInstance().getTextureManager().register(rl, tex);
        overviews.put(key, new Overview(tex, rl));
    }

    /** The overview tile texture of a region, or null (asked for from disk if not known yet). */
    public ResourceLocation overview(int rx, int rz) {
        long key = ChunkPos.asLong(rx, rz);
        Overview o = overviews.get(key);
        if (o != null) return o.id();
        if (overviewMissing.contains(key) || !overviewLoading.add(key) || depth) return null;
        Path file = overviewDir.resolve("r." + rx + "." + rz + ".png");
        MapIO.run("read overview tile", () -> {
            NativeImage img = null;
            if (Files.exists(file)) {
                try (var in = Files.newInputStream(file)) {
                    img = NativeImage.read(in);
                }
            }
            overviewLoaded.add(new LoadedOverview(key, img));
        });
        return null;
    }
}
