package com.tac5studios.codexcartography.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.common.Tags;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Reads one loaded chunk into map columns. Colors come from the game's own map colors,
 * with fixes for blocks that look wrong on a map (grass, leaves and water take their biome color;
 * glass, plants, torches and rails are looked through to the block below).
 */
public final class ChunkScanner {

    private static final int MAX_WATER_DEPTH = 64;

    private ChunkScanner() {}

    public static ChunkColumns scan(ClientLevel level, LevelChunk chunk, MapRegion region) {
        return scan(level, chunk, region, Integer.MIN_VALUE);
    }

    /**
     * Reads a chunk. With a depth top (Depth View), the map shows the first floor below that height
     * instead of the surface.
     */
    public static ChunkColumns scan(ClientLevel level, LevelChunk chunk, MapRegion region, int depthTop) {
        ChunkColumns out = ChunkColumns.blank(System.currentTimeMillis());
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight() - 1;
        boolean ceiling = level.dimensionType().hasCeiling();
        int logicalTop = Math.min(maxY, minY + level.dimensionType().logicalHeight() - 1);
        int baseX = chunk.getPos().getMinBlockX();
        int baseZ = chunk.getPos().getMinBlockZ();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        Map<Holder<Biome>, Integer> biomeIds = new IdentityHashMap<>();

        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                int i = (z << 4) | x;
                int wx = baseX + x;
                int wz = baseZ + z;
                int y = depthTop != Integer.MIN_VALUE ? caveFloor(chunk, pos, wx, wz, Math.min(maxY, depthTop), minY)
                        : ceiling ? belowRoof(chunk, pos, wx, wz, logicalTop, minY)
                        : Math.min(maxY, chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z));
                if (y == Integer.MIN_VALUE) {
                    out.height()[i] = (short) Math.min(maxY, depthTop); // solid rock at the player's level: left dark
                    continue;
                }
                BlockState state = chunk.getBlockState(pos.set(wx, y, wz));
                while (y > minY && seeThrough(level, state, pos)) {
                    y--;
                    state = chunk.getBlockState(pos.set(wx, y, wz));
                }
                out.height()[i] = (short) y;
                if (state.isAir()) continue; // void: color stays 0

                if (state.getFluidState().is(FluidTags.WATER)) {
                    int depth = 0;
                    int wy = y;
                    while (wy > minY && depth < MAX_WATER_DEPTH
                            && chunk.getBlockState(pos.set(wx, wy, wz)).getFluidState().is(FluidTags.WATER)) {
                        depth++;
                        wy--;
                    }
                    out.water()[i] = (byte) depth;
                    pos.set(wx, y, wz);
                }

                // the chunk's own biome: level.getBiome blends in neighbor chunks, and a neighbor that isn't loaded yet
                // reads as the default biome (wrong water and grass colors along chunk edges)
                Holder<Biome> biome = chunk.getNoiseBiome(QuartPos.fromBlock(wx), QuartPos.fromBlock(y), QuartPos.fromBlock(wz));
                out.color()[i] = 0xFF000000 | colorOf(level, state, pos, biome.value(), wx, wz);
                out.biome()[i] = (short) (int) biomeIds.computeIfAbsent(biome, b -> region.biome(
                        b.unwrapKey().map(k -> k.location().toString()).orElse("unknown")));
                out.light()[i] = (byte) level.getBrightness(LightLayer.BLOCK, pos.set(wx, y + 1, wz));
            }
        }
        return out;
    }

    /** Rock thicker than this below the player's head level is drawn dark instead of showing caves further down. */
    private static final int MAX_ROCK = 12;

    /**
     * Depth View: the floor under the player's head level. Open space there goes down to its floor;
     * rock goes down at most {@link #MAX_ROCK} blocks to the next opening, or is left dark (Integer.MIN_VALUE).
     */
    private static int caveFloor(LevelChunk chunk, BlockPos.MutableBlockPos pos, int wx, int wz, int top, int minY) {
        int y = top;
        int rock = 0;
        while (y > minY && !chunk.getBlockState(pos.set(wx, y, wz)).isAir()) {
            if (++rock > MAX_ROCK) return Integer.MIN_VALUE;
            y--;
        }
        while (y > minY && chunk.getBlockState(pos.set(wx, y, wz)).isAir()) y--;
        return y;
    }

    private static int belowRoof(LevelChunk chunk, BlockPos.MutableBlockPos pos, int wx, int wz, int top, int minY) {
        int y = top;
        while (y > minY && !chunk.getBlockState(pos.set(wx, y, wz)).isAir()) y--;
        while (y > minY && chunk.getBlockState(pos.set(wx, y, wz)).isAir()) y--;
        return y;
    }

    /** Blocks the map looks through to the block below. */
    private static boolean seeThrough(ClientLevel level, BlockState state, BlockPos pos) {
        if (state.isAir()) return true;
        if (!state.getFluidState().isEmpty()) return false;
        if (state.is(Tags.Blocks.GLASS_BLOCKS) || state.is(Tags.Blocks.GLASS_PANES)) return true;
        if (state.is(Blocks.SNOW) || state.is(BlockTags.LEAVES)) return false;
        if (state.getMapColor(level, pos) == MapColor.NONE) return true;
        return state.getCollisionShape(level, pos).isEmpty(); // flowers, grass, crops, torches, rails
    }

    private static int colorOf(ClientLevel level, BlockState state, BlockPos pos, Biome biome, int x, int z) {
        if (state.getFluidState().is(FluidTags.WATER)) return biome.getWaterColor();
        if (state.is(Blocks.GRASS_BLOCK)) return biome.getGrassColor(x, z);
        if (state.is(Blocks.SPRUCE_LEAVES)) return FoliageColor.getEvergreenColor();
        if (state.is(Blocks.BIRCH_LEAVES)) return FoliageColor.getBirchColor();
        if (state.is(Blocks.MANGROVE_LEAVES)) return FoliageColor.getMangroveColor();
        if (state.is(Blocks.OAK_LEAVES) || state.is(Blocks.JUNGLE_LEAVES) || state.is(Blocks.ACACIA_LEAVES)
                || state.is(Blocks.DARK_OAK_LEAVES)) {
            return biome.getFoliageColor();
        }
        return state.getMapColor(level, pos).col;
    }
}
