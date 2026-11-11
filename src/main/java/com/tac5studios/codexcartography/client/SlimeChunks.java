package com.tac5studios.codexcartography.client;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.world.level.ChunkPos;

/** Slime chunks around the player, sent by the server when its rules allow them. */
public final class SlimeChunks {

    private static final LongOpenHashSet SLIME = new LongOpenHashSet();

    private SlimeChunks() {}

    public static void set(long[] chunks) {
        SLIME.clear();
        for (long c : chunks) SLIME.add(c);
    }

    public static void clear() {
        SLIME.clear();
    }

    public static boolean any() {
        return !SLIME.isEmpty();
    }

    public static boolean isSlime(int chunkX, int chunkZ) {
        return SLIME.contains(ChunkPos.asLong(chunkX, chunkZ));
    }
}
