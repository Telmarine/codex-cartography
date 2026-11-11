package com.tac5studios.codexcartography.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.Arrays;
import java.util.List;

/**
 * config/codex_cartography/features.toml — one switch per feature. Changes need a restart.
 * The server and each client read their own copy.
 */
public final class Features {

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue MINIMAP;
    public static final ModConfigSpec.BooleanValue WORLD_MAP;
    public static final ModConfigSpec.BooleanValue MARKS;
    public static final ModConfigSpec.BooleanValue DEATH_POINTS;
    public static final ModConfigSpec.BooleanValue IN_WORLD_MARKS;
    public static final ModConfigSpec.BooleanValue SWEEP;
    public static final ModConfigSpec.BooleanValue DEPTH_VIEW;
    public static final ModConfigSpec.BooleanValue LENSES;
    public static final ModConfigSpec.BooleanValue PRINT;
    public static final ModConfigSpec.BooleanValue CLAIMS_LAYER;
    public static final ModConfigSpec.BooleanValue CLAIM_MARKS;
    public static final ModConfigSpec.BooleanValue WAYSTONES_MARKERS;
    public static final ModConfigSpec.BooleanValue NEXUS_MARKERS;
    public static final ModConfigSpec.BooleanValue WAYPOINT_IMPORT;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> FORCE_ON;

    /** Names allowed in force_on. */
    public static final List<String> FORCE_ON_NAMES = Arrays.asList("minimap", "key");

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.comment("Codex: Cartography master switches. Turn a feature off here and it's gone completely.",
                "On a server, switching one off here also switches it off for every player who joins.",
                "Changes need a restart. Every other setting reloads.").push("features");
        MINIMAP = b.comment("The minimap.").define("minimap", true);
        WORLD_MAP = b.comment("The full-screen world map.").define("world_map", true);
        MARKS = b.comment("Marks (waypoints), the Ledger and folios.").define("marks", true);
        DEATH_POINTS = b.comment("A Death point where the player died.").define("death_points", true);
        IN_WORLD_MARKS = b.comment("Marks drawn in the world as floating emblems.").define("in_world_marks", true);
        SWEEP = b.comment("The Sweep (players, mobs, pets, vehicles and items on the map).").define("sweep", true);
        DEPTH_VIEW = b.comment("Depth View (the layer around the player when underground).").define("depth_view", true);
        LENSES = b.comment("Lenses (Relief, Biome and Night views).").define("lenses", true);
        PRINT = b.comment("Print (export the map as a PNG).").define("print", true);
        CLAIMS_LAYER = b.comment("Claims from Elements: Claims drawn on the map.").define("claims_layer", true);
        CLAIM_MARKS = b.comment("Claim marks shared with a claim's trusted players.").define("claim_marks", true);
        WAYSTONES_MARKERS = b.comment("Waystones shown as map markers.").define("waystones_markers", true);
        NEXUS_MARKERS = b.comment("Elements: Nexus homes and warps shown as map markers.").define("nexus_markers", true);
        WAYPOINT_IMPORT = b.comment("One-time import of waypoints from other map mods.").define("waypoint_import", true);
        b.pop();

        b.comment("When another minimap or world map mod is installed, the minimap and the Codex key start off.",
                "Add \"minimap\" and/or \"key\" here to keep them on anyway.",
                "On a server this is sent to every player who joins and decides it for them.").push("overlap_guard");
        FORCE_ON = b.defineListAllowEmpty("force_on", List.of(), () -> "", o -> o instanceof String s && FORCE_ON_NAMES.contains(s));
        b.pop();
        SPEC = b.build();
    }

    private Features() {}

    /** True when the map has any use (minimap or world map on), so scanning is worth doing. */
    public static boolean mapping() {
        return MINIMAP.get() || WORLD_MAP.get();
    }
}
