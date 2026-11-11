package com.tac5studios.codexcartography.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.border.WorldBorder;

import java.util.HashMap;
import java.util.Map;

/**
 * World borders of every dimension. The player's own dimension uses the border the game already sends;
 * other dimensions come from the server side, or the last border seen there.
 */
public final class WorldBorders {

    private static final Map<ResourceLocation, WorldBorder> BORDERS = new HashMap<>();

    private WorldBorders() {}

    public static WorldBorder current(ClientLevel level) {
        return level.getWorldBorder();
    }

    /** The border of any dimension, or null if not known. */
    public static WorldBorder of(ClientLevel level, ResourceLocation dim) {
        if (level.dimension().location().equals(dim)) return level.getWorldBorder();
        return BORDERS.get(dim);
    }

    /** Server data: center, size now, size it's moving to and milliseconds left. */
    public static void set(ResourceLocation dim, double cx, double cz, double size, double target, long millis) {
        WorldBorder b = BORDERS.computeIfAbsent(dim, d -> new WorldBorder());
        b.setCenter(cx, cz);
        if (millis > 0 && target != size) b.lerpSizeBetween(size, target, millis);
        else b.setSize(size);
    }

    /** Remembers the border of the dimension the player is leaving. */
    public static void remember(ClientLevel level) {
        WorldBorder live = level.getWorldBorder();
        set(level.dimension().location(), live.getCenterX(), live.getCenterZ(), live.getSize(), live.getLerpTarget(), live.getLerpRemainingTime());
    }

    public static void clear() {
        BORDERS.clear();
    }
}
