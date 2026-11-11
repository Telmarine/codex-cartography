package com.tac5studios.codexcartography.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tac5studios.codexcartography.CodexCartography;
import com.tac5studios.codexcartography.config.ClientConfig;
import com.tac5studios.codexcartography.map.RegionImages;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.world.level.ChunkPos;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * One map layer of one dimension (the surface, or a Depth View band), saved in
 * codex_cartography/worlds/&lt;world&gt;/&lt;dimension&gt;/[depth/&lt;band&gt;/].
 * Regions load in the background; the least recently used are saved and dropped when over the memory cap.
 */
public final class DimensionStore {

    private final Path dir;
    private final Map<Long, MapRegion> regions = new LinkedHashMap<>(64, 0.75f, true);
    private final LongOpenHashSet loading = new LongOpenHashSet();
    private final ConcurrentLinkedQueue<MapRegion> loaded = new ConcurrentLinkedQueue<>();
    private final RegionImages images;
    private int chunkCount;

    public DimensionStore(Path dir, boolean depth) {
        this.dir = dir;
        this.images = new RegionImages(dir, depth);
        Path meta = dir.resolve("dimension.json");
        try {
            if (Files.exists(meta)) {
                JsonObject json = JsonParser.parseString(Files.readString(meta, StandardCharsets.UTF_8)).getAsJsonObject();
                if (json.has("chunks")) chunkCount = json.get("chunks").getAsInt();
            }
        } catch (Exception e) {
            CodexCartography.LOGGER.warn("[Cartography] Couldn't read {}: {}", meta, e.toString());
        }
    }

    public Path dir() { return dir; }
    public int chunkCount() { return chunkCount; }
    public RegionImages images() { return images; }
    public int loadedRegions() { return regions.size(); }

    /** The region holding this chunk, or null while it's still loading (asked for here if needed). */
    public MapRegion regionOf(int chunkX, int chunkZ) {
        return region(chunkX >> 5, chunkZ >> 5);
    }

    /** A region by region coordinates, or null while it's loading (asked for here if needed). */
    public MapRegion region(int rx, int rz) {
        long key = ChunkPos.asLong(rx, rz);
        MapRegion region = regions.get(key);
        if (region != null || !loading.add(key)) return region;
        MapIO.run("read map region " + rx + "," + rz, () -> loaded.add(RegionFile.read(dir, rx, rz)));
        return null;
    }

    /** A region only if it's already in memory (never loads). */
    public MapRegion peek(int rx, int rz) {
        return regions.get(ChunkPos.asLong(rx, rz));
    }

    /** True when a region file exists on disk or the region is in memory. */
    public boolean known(int rx, int rz) {
        return regions.containsKey(ChunkPos.asLong(rx, rz)) || Files.exists(RegionFile.path(dir, rx, rz));
    }

    /** Stores a scanned chunk in its (already loaded) region and repaints it. */
    public void put(MapRegion region, int chunkX, int chunkZ, ChunkColumns columns) {
        int slot = MapRegion.slot(chunkX, chunkZ);
        if (region.put(slot, columns)) chunkCount++;
        images.chunkChanged(region, slot);
    }

    /** The stored column at a block position, or null if not mapped / not loaded. */
    public ChunkColumns columns(int blockX, int blockZ) {
        MapRegion r = peek(blockX >> 9, blockZ >> 9);
        return r == null ? null : r.get(MapRegion.slot(blockX >> 4, blockZ >> 4));
    }

    /** Takes in regions the IO thread read, drops old ones over the memory cap and updates the pictures. */
    public void tick() {
        MapRegion r;
        while ((r = loaded.poll()) != null) {
            long key = ChunkPos.asLong(r.rx(), r.rz());
            loading.remove(key);
            regions.putIfAbsent(key, r);
        }
        int max = ClientConfig.MAX_REGIONS.get();
        Iterator<MapRegion> it = regions.values().iterator();
        while (regions.size() > max && it.hasNext()) {
            MapRegion eldest = it.next();
            if (eldest.dirty()) save(eldest);
            images.release(eldest.rx(), eldest.rz());
            it.remove();
        }
        images.tick(regions::get);
    }

    /** Saves every changed region in the background. */
    public void saveDirty() {
        boolean any = false;
        for (MapRegion region : regions.values()) {
            if (region.dirty()) {
                save(region);
                images.saveOverview(region.rx(), region.rz());
                any = true;
            }
        }
        if (any) saveMeta();
    }

    /** Saves and frees everything (leaving the dimension). */
    public void close() {
        saveDirty();
        images.releaseAll();
        regions.clear();
    }

    /** Forgets everything in memory without saving (after the files were deleted). */
    public void forget() {
        images.releaseAll();
        regions.clear();
        chunkCount = 0;
    }

    private void save(MapRegion region) {
        MapRegion.Snapshot snap = region.snapshot();
        MapIO.run("save map region " + snap.rx() + "," + snap.rz(), () -> RegionFile.write(dir, snap));
    }

    private void saveMeta() {
        int count = chunkCount;
        MapIO.run("save " + dir.resolve("dimension.json"), () -> {
            JsonObject json = new JsonObject();
            json.addProperty("chunks", count);
            json.addProperty("saved", System.currentTimeMillis());
            Files.createDirectories(dir);
            Files.writeString(dir.resolve("dimension.json"), json.toString(), StandardCharsets.UTF_8);
        });
    }
}
