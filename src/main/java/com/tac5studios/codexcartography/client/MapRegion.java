package com.tac5studios.codexcartography.client;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** A 512 x 512 block area (32 x 32 chunks) of one dimension's map. Used on the client thread only. */
public final class MapRegion {

    public static final int CHUNKS = 32;

    private final int rx;
    private final int rz;
    private final ChunkColumns[] chunks = new ChunkColumns[CHUNKS * CHUNKS];
    private final List<String> biomes = new ArrayList<>();
    private final Object2IntOpenHashMap<String> biomeIndex = new Object2IntOpenHashMap<>();
    private boolean dirty;

    public MapRegion(int rx, int rz) {
        this.rx = rx;
        this.rz = rz;
        biomeIndex.defaultReturnValue(-1);
    }

    public int rx() { return rx; }
    public int rz() { return rz; }

    public static int slot(int chunkX, int chunkZ) {
        return (chunkZ & (CHUNKS - 1)) * CHUNKS + (chunkX & (CHUNKS - 1));
    }

    /** Index of a biome id in this region's list, adding it if new. */
    public int biome(String id) {
        int i = biomeIndex.getInt(id);
        if (i < 0) {
            i = biomes.size();
            biomes.add(id);
            biomeIndex.put(id, i);
        }
        return i;
    }

    /** Stores a chunk. Returns true when the chunk wasn't mapped before. */
    public boolean put(int slot, ChunkColumns columns) {
        boolean fresh = chunks[slot] == null;
        chunks[slot] = columns;
        dirty = true;
        return fresh;
    }

    public String biomeId(int index) {
        return index >= 0 && index < biomes.size() ? biomes.get(index) : "unknown";
    }

    public ChunkColumns get(int slot) {
        return chunks[slot];
    }

    public boolean dirty() { return dirty; }

    /** A copy that can be written from the IO thread. Clears the dirty flag. */
    public Snapshot snapshot() {
        dirty = false;
        return new Snapshot(rx, rz, Arrays.copyOf(chunks, chunks.length), List.copyOf(biomes));
    }

    /** Used when reading a region file. */
    void load(List<String> biomeIds, int slot, ChunkColumns columns) {
        if (biomes.isEmpty()) for (String id : biomeIds) biome(id);
        chunks[slot] = columns;
    }

    public record Snapshot(int rx, int rz, ChunkColumns[] chunks, List<String> biomes) {}
}
