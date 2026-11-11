package com.tac5studios.codexcartography.client;

/**
 * One scanned chunk: 16 x 16 block columns, index (z << 4) | x.
 * Never changed after it's made; a rescan makes a new one, so saving can read it from another thread.
 *
 * @param color     surface color, 0xAARRGGBB; alpha 0 means nothing there (void)
 * @param height    surface height
 * @param water     water depth above the surface block's floor (0 = no water)
 * @param light     block light just above the surface (0-15)
 * @param biome     index into the region's biome list
 * @param scannedAt when it was read (epoch milliseconds)
 */
public record ChunkColumns(int[] color, short[] height, byte[] water, byte[] light, short[] biome, long scannedAt) {

    public static final int COLUMNS = 256;

    public static ChunkColumns blank(long scannedAt) {
        return new ChunkColumns(new int[COLUMNS], new short[COLUMNS], new byte[COLUMNS], new byte[COLUMNS], new short[COLUMNS], scannedAt);
    }
}
