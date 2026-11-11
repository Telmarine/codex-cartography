package com.tac5studios.codexcartography.client;

import com.tac5studios.codexcartography.config.ClientConfig;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;

/**
 * Chunks waiting to be read. Work is spread over ticks with a time budget,
 * and changed chunks wait a short delay so a burst of block changes is read once.
 */
public final class ScanQueue {

    private final LongLinkedOpenHashSet pending = new LongLinkedOpenHashSet();
    private final Long2LongOpenHashMap dirty = new Long2LongOpenHashMap();

    public void add(long chunk) {
        pending.add(chunk);
    }

    public void markDirty(long chunk, long tick) {
        dirty.putIfAbsent(chunk, tick);
    }

    public void clear() {
        pending.clear();
        dirty.clear();
    }

    public void process(ClientLevel level, DimensionStore store, long tick, int depthTop, double share) {
        int delay = ClientConfig.RESCAN_DELAY_TICKS.get();
        ObjectIterator<Long2LongMap.Entry> it = dirty.long2LongEntrySet().iterator();
        while (it.hasNext()) {
            Long2LongMap.Entry e = it.next();
            if (tick - e.getLongValue() >= delay) {
                pending.add(e.getLongKey());
                it.remove();
            }
        }
        if (pending.isEmpty()) return;

        long budget = (long) (ClientConfig.SCAN_BUDGET_MS.get() * 1_000_000L * share);
        long start = System.nanoTime();
        LongArrayList waiting = new LongArrayList();
        while (!pending.isEmpty() && System.nanoTime() - start < budget) {
            long key = pending.removeFirstLong();
            int cx = ChunkPos.getX(key);
            int cz = ChunkPos.getZ(key);
            LevelChunk chunk = level.getChunkSource().getChunk(cx, cz, ChunkStatus.FULL, false);
            if (chunk == null) continue; // unloaded since it was queued
            MapRegion region = store.regionOf(cx, cz);
            if (region == null) {
                waiting.add(key); // region still loading; try again next tick
                continue;
            }
            store.put(region, cx, cz, ChunkScanner.scan(level, chunk, region, depthTop));
        }
        pending.addAll(waiting);
    }

    public int size() {
        return pending.size() + dirty.size();
    }
}
