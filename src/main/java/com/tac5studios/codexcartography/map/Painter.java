package com.tac5studios.codexcartography.map;

import com.tac5studios.codexcartography.client.ChunkColumns;
import com.tac5studios.codexcartography.client.MapRegion;
import com.tac5studios.codexcartography.config.ClientConfig;
import com.tac5studios.codexcartography.ui.Codex;
import com.mojang.blaze3d.platform.NativeImage;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

/**
 * Turns stored map columns into pixels for the current lens:
 * Surface (block colors, height shading, water by depth), Relief (height bands with contour lines),
 * Biome (flat biome colors) and Night (dark, with block light as a warm glow).
 */
public final class Painter {

    private static final Object2IntOpenHashMap<String> BIOME_COLORS = new Object2IntOpenHashMap<>();

    private Painter() {}

    /** Paints one chunk of a region into the region's 512 x 512 image. */
    public static void paintChunk(MapRegion region, int slot, NativeImage img, ClientConfig.Lens lens, boolean depth) {
        ChunkColumns c = region.get(slot);
        int ox = (slot % MapRegion.CHUNKS) * 16;
        int oz = (slot / MapRegion.CHUNKS) * 16;
        if (c == null) {
            img.fillRect(ox, oz, 16, 16, 0);
            return;
        }
        ChunkColumns north = slot >= MapRegion.CHUNKS ? region.get(slot - MapRegion.CHUNKS) : null;
        ChunkColumns west = slot % MapRegion.CHUNKS > 0 ? region.get(slot - 1) : null;
        boolean vanilla = ClientConfig.PALETTE.get() == ClientConfig.Palette.VANILLA;
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                int i = (z << 4) | x;
                int color = c.color()[i];
                if ((color >>> 24) == 0) {
                    img.setPixelRGBA(ox + x, oz + z, 0);
                    continue;
                }
                int h = c.height()[i];
                int hn = z > 0 ? c.height()[i - 16] : north != null ? north.height()[240 + x] : h;
                int hw = x > 0 ? c.height()[i - 1] : west != null ? west.height()[(z << 4) | 15] : h;
                int water = c.water()[i];
                int rgb = switch (lens) {
                    case SURFACE -> surface(color & 0xFFFFFF, h, hn, water, edge(c, north, west, i, x, z), vanilla);
                    case RELIEF -> relief(h, hn, hw, water);
                    case BIOME -> biome(region.biomeId(c.biome()[i]), color & 0xFFFFFF);
                    case NIGHT -> night(surface(color & 0xFFFFFF, h, hn, water, false, vanilla), c.light()[i]);
                };
                if (depth) rgb = Codex.mix(rgb, 0x101820, 0.15f);
                img.setPixelRGBA(ox + x, oz + z, abgr(rgb));
            }
        }
    }

    /** Water next to land gets a dark shoreline. Unmapped columns (nothing scanned there yet) are not land. */
    private static boolean edge(ChunkColumns c, ChunkColumns north, ChunkColumns west, int i, int x, int z) {
        if (c.water()[i] == 0) return false;
        boolean n = z > 0 ? land(c, i - 16) : north != null && land(north, 240 + x);
        boolean w = x > 0 ? land(c, i - 1) : west != null && land(west, (z << 4) | 15);
        boolean s = z < 15 && land(c, i + 16);
        boolean e = x < 15 && land(c, i + 1);
        return n || w || s || e;
    }

    private static boolean land(ChunkColumns c, int i) {
        return c.water()[i] == 0 && (c.color()[i] >>> 24) != 0;
    }

    private static int surface(int rgb, int h, int hn, int water, boolean waterEdge, boolean vanilla) {
        if (water > 0) {
            float deep = Math.min(water, 20) / 20f;
            int base = vanilla ? rgb : Codex.mix(rgb, 0x1E3A5C, 0.35f);
            int out = Codex.scale(base, 1.0f - deep * 0.45f);
            return waterEdge ? Codex.scale(out, 0.55f) : out;
        }
        int base = vanilla ? rgb : codexTone(rgb);
        float shade = h > hn ? 1.10f : h < hn ? 0.84f : 1.0f;
        return Codex.scale(base, shade);
    }

    /** Cooler, slightly muted version of a block color. */
    private static int codexTone(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        int grey = (r * 30 + g * 59 + b * 11) / 100;
        r = (int) (r * 0.82 + grey * 0.18);
        g = (int) (g * 0.82 + grey * 0.18);
        b = (int) (b * 0.82 + grey * 0.18);
        return Codex.mix((r << 16) | (g << 8) | b, 0x2A3C4A, 0.12f);
    }

    private static int relief(int h, int hn, int hw, int water) {
        if (water > 0) return Codex.scale(0x24425E, 1.0f - Math.min(water, 20) / 40f);
        float t = Math.max(0f, Math.min(1f, (h - 40) / 160f));
        int base = t < 0.5f ? Codex.mix(0x2C4A3A, 0x8C8A5A, t * 2) : Codex.mix(0x8C8A5A, 0xE6E2D2, (t - 0.5f) * 2);
        if (Math.floorDiv(h, 8) != Math.floorDiv(hn, 8) || Math.floorDiv(h, 8) != Math.floorDiv(hw, 8)) {
            return Codex.mix(base, ClientConfig.ACCENT.get(), 0.65f);
        }
        return base;
    }

    private static int biome(String id, int fallback) {
        if (BIOME_COLORS.containsKey(id)) return BIOME_COLORS.getInt(id);
        int color = fallback;
        Minecraft mc = Minecraft.getInstance();
        ResourceLocation key = ResourceLocation.tryParse(id);
        if (mc.level != null && key != null) {
            Biome b = mc.level.registryAccess().registryOrThrow(Registries.BIOME).get(key);
            if (b != null) {
                boolean wet = id.contains("ocean") || id.contains("river");
                color = wet ? b.getWaterColor() : b.getGrassColor(0, 0);
                if (id.contains("desert") || id.contains("badlands") || id.contains("beach")) color = 0xD8C88A;
                if (id.contains("snow") || id.contains("frozen") || id.contains("ice")) color = 0xDCE8F0;
            }
        }
        BIOME_COLORS.put(id, color);
        return color;
    }

    private static int night(int rgb, int light) {
        int dark = Codex.scale(rgb, 0.22f);
        if (light <= 0) return dark;
        return Codex.mix(dark, 0xFFAA50, Math.min(1f, light / 15f) * 0.8f);
    }

    public static int abgr(int rgb) {
        return 0xFF000000 | ((rgb & 0xFF) << 16) | (rgb & 0xFF00) | ((rgb >> 16) & 0xFF);
    }

    public static int rgbOf(int abgr) {
        return ((abgr & 0xFF) << 16) | (abgr & 0xFF00) | ((abgr >> 16) & 0xFF);
    }
}
