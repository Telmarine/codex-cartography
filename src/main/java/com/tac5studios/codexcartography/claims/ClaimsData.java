package com.tac5studios.codexcartography.claims;

import com.tac5studios.codexcartography.config.ClientConfig;
import com.tac5studios.codexcartography.ui.Codex;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Claims near the player, as sent by Elements: Claims over its map channel. Client side only. */
public final class ClaimsData {

    public static final int OWN = 0;
    public static final int TRUSTED = 1;
    public static final int OTHER = 2;
    public static final int ADMIN = 3;

    /** A sub-area box inside a claim. */
    public record SubArea(String name, int x1, int y1, int z1, int x2, int y2, int z2) {}

    /** A claim mark shared with the claim's trusted players. */
    public record ClaimMark(String id, String name, String emblem, int color, int x, int y, int z) {}

    /**
     * One claim (property) as the player is allowed to see it.
     * standing: OWN, TRUSTED, OTHER or ADMIN. rank: the player's rank name there ("" if none).
     */
    public record Claim(int id, ResourceLocation dim, String name, String owner, int standing, String rank, boolean privateLand,
                        int color, long[] chunks, List<SubArea> subAreas, List<ClaimMark> marks, boolean canManage, boolean active) {
        public int drawColor() {
            if (ClientConfig.CLAIM_OWN_COLOR.get() && color >= 0) return color;
            if (ClientConfig.COLORBLIND.get()) {
                return switch (standing) {
                    case OWN -> 0x0072B2;
                    case TRUSTED -> 0x56B4E9;
                    case ADMIN -> 0xCC79A7;
                    default -> 0xE69F00;
                };
            }
            return switch (standing) {
                case OWN -> ClientConfig.CLAIM_OWN.get();
                case TRUSTED -> ClientConfig.CLAIM_TRUSTED.get();
                case ADMIN -> ClientConfig.CLAIM_SERVER.get();
                default -> ClientConfig.CLAIM_OTHERS.get();
            };
        }
    }

    private static final Int2ObjectOpenHashMap<Claim> CLAIMS = new Int2ObjectOpenHashMap<>();
    private static final Long2IntOpenHashMap BY_CHUNK = new Long2IntOpenHashMap();
    private static ResourceLocation byChunkDim;
    public static int chunksUsed;
    public static int chunksAllowed;
    public static int propertiesUsed;
    public static int propertiesMax;
    public static int claimRange = 32;
    public static boolean present;

    static {
        BY_CHUNK.defaultReturnValue(-1);
    }

    private ClaimsData() {}

    public static boolean isEmpty() {
        return CLAIMS.isEmpty();
    }

    public static Collection<Claim> all() {
        return CLAIMS.values();
    }

    public static void put(Claim c) {
        CLAIMS.put(c.id(), c);
        byChunkDim = null;
    }

    public static void remove(int id) {
        CLAIMS.remove(id);
        byChunkDim = null;
    }

    public static void clear() {
        CLAIMS.clear();
        BY_CHUNK.clear();
        byChunkDim = null;
        present = false;
    }

    private static ResourceLocation currentDim() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null ? null : mc.level.dimension().location();
    }

    private static void index(ResourceLocation dim) {
        if (dim == null || dim.equals(byChunkDim)) return;
        BY_CHUNK.clear();
        for (Claim c : CLAIMS.values()) {
            if (!c.dim().equals(dim)) continue;
            for (long chunk : c.chunks()) BY_CHUNK.put(chunk, c.id());
        }
        byChunkDim = dim;
    }

    /** The claim on a chunk of a dimension, or null. */
    public static Claim at(ResourceLocation dim, int chunkX, int chunkZ) {
        index(dim);
        long key = ChunkPos.asLong(chunkX, chunkZ);
        // claim ids are hash codes and can be negative, so check the key instead of a -1 default
        return BY_CHUNK.containsKey(key) ? CLAIMS.get(BY_CHUNK.get(key)) : null;
    }

    public static List<Claim> in(ResourceLocation dim) {
        List<Claim> out = new ArrayList<>();
        for (Claim c : CLAIMS.values()) if (c.dim().equals(dim)) out.add(c);
        return out;
    }

    /** Tints a minimap pixel by the claim under it, with a dashed outline along claim edges. */
    public static int tint(int rgb, int wx, int wz, double wxd, double wzd, double blocksPerPixel) {
        ResourceLocation dim = currentDim();
        Claim c = at(dim, wx >> 4, wz >> 4);
        if (c == null) return rgb;
        int color = c.drawColor();
        double lx = wxd - (wx & ~15);
        double lz = wzd - (wz & ~15);
        double edge = Math.max(1.2, blocksPerPixel * 1.5);
        boolean onEdge = (lx < edge && at(dim, (wx >> 4) - 1, wz >> 4) != c)
                || (lx > 16 - edge && at(dim, (wx >> 4) + 1, wz >> 4) != c)
                || (lz < edge && at(dim, wx >> 4, (wz >> 4) - 1) != c)
                || (lz > 16 - edge && at(dim, wx >> 4, (wz >> 4) + 1) != c);
        if (onEdge) {
            double along = (lx < edge || lx > 16 - edge) ? wzd : wxd;
            return ((int) Math.floor(along / 2.0) & 1) == 0 ? color : Codex.mix(rgb, color, 0.5f);
        }
        return Codex.mix(rgb, color, 0.22f);
    }
}
