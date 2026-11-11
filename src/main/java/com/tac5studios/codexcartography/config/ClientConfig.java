package com.tac5studios.codexcartography.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

/** config/codex_cartography/client.toml — every look-and-feel option. Edited in game on the Settings page. */
public final class ClientConfig {

    public enum Shape { PLATE, SQUARE, ROUND }
    public enum Lens { SURFACE, RELIEF, BIOME, NIGHT }
    public enum Palette { CODEX, VANILLA }
    public enum MobIcons { DOTS, HEADS, HOLD }

    public static final ModConfigSpec SPEC;

    // key
    public static final ModConfigSpec.IntValue HOLD_MS;
    // minimap
    public static final ModConfigSpec.BooleanValue MINIMAP_ON;
    public static final ModConfigSpec.EnumValue<Shape> SHAPE;
    public static final ModConfigSpec.IntValue MINIMAP_SIZE;
    public static final ModConfigSpec.IntValue MINIMAP_X;
    public static final ModConfigSpec.IntValue MINIMAP_Y;
    public static final ModConfigSpec.BooleanValue ROTATE;
    public static final ModConfigSpec.DoubleValue MINIMAP_ZOOM;
    public static final ModConfigSpec.DoubleValue OPACITY;
    public static final ModConfigSpec.BooleanValue INFO_COORDS;
    public static final ModConfigSpec.BooleanValue INFO_BIOME;
    public static final ModConfigSpec.BooleanValue INFO_TIME;
    public static final ModConfigSpec.BooleanValue INFO_LIGHT;
    public static final ModConfigSpec.BooleanValue INFO_BORDER;
    public static final ModConfigSpec.BooleanValue HIDE_IN_MENUS;
    // world map
    public static final ModConfigSpec.BooleanValue KEY_HINTS;
    public static final ModConfigSpec.BooleanValue LAYER_CLAIMS;
    public static final ModConfigSpec.BooleanValue LAYER_MARKS;
    public static final ModConfigSpec.BooleanValue LAYER_PLAYERS;
    public static final ModConfigSpec.BooleanValue LAYER_GRID;
    public static final ModConfigSpec.BooleanValue LAYER_SLIME;
    public static final ModConfigSpec.BooleanValue LAYER_BORDER;
    public static final ModConfigSpec.BooleanValue LAYER_WAYSTONES;
    public static final ModConfigSpec.BooleanValue LAYER_HOMES;
    // marks
    public static final ModConfigSpec.BooleanValue MARKS_IN_WORLD;
    public static final ModConfigSpec.IntValue MARK_MAX_DISTANCE;
    public static final ModConfigSpec.BooleanValue MARK_DISTANCE;
    public static final ModConfigSpec.DoubleValue MARK_SIZE;
    public static final ModConfigSpec.IntValue DEATH_POINTS_KEPT;
    public static final ModConfigSpec.BooleanValue DEATH_POINT_CLEAR;
    // sweep
    public static final ModConfigSpec.BooleanValue SWEEP_PLAYERS;
    public static final ModConfigSpec.BooleanValue SWEEP_HOSTILE;
    public static final ModConfigSpec.BooleanValue SWEEP_PASSIVE;
    public static final ModConfigSpec.BooleanValue SWEEP_PETS;
    public static final ModConfigSpec.BooleanValue SWEEP_VEHICLES;
    public static final ModConfigSpec.BooleanValue SWEEP_ITEMS;
    public static final ModConfigSpec.BooleanValue SWEEP_NEAR_HEIGHT;
    public static final ModConfigSpec.IntValue SWEEP_HEIGHT_RANGE;
    public static final ModConfigSpec.EnumValue<MobIcons> MOB_ICONS;
    // lenses
    public static final ModConfigSpec.EnumValue<Lens> LENS;
    public static final ModConfigSpec.BooleanValue DEPTH_AUTO;
    // claims
    public static final ModConfigSpec.IntValue CLAIM_OWN;
    public static final ModConfigSpec.IntValue CLAIM_TRUSTED;
    public static final ModConfigSpec.IntValue CLAIM_OTHERS;
    public static final ModConfigSpec.IntValue CLAIM_SERVER;
    public static final ModConfigSpec.BooleanValue CLAIM_OWN_COLOR;
    public static final ModConfigSpec.BooleanValue COLORBLIND;
    // world border
    public static final ModConfigSpec.BooleanValue BORDER_MINIMAP;
    public static final ModConfigSpec.BooleanValue BORDER_WORLD_MAP;
    public static final ModConfigSpec.IntValue BORDER_WARN;
    public static final ModConfigSpec.IntValue BORDER_COLOR;
    // colors
    public static final ModConfigSpec.IntValue ACCENT;
    public static final ModConfigSpec.EnumValue<Palette> PALETTE;
    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> RECENT_COLORS;
    public static final ModConfigSpec.IntValue WAYSTONE_COLOR;
    public static final ModConfigSpec.IntValue HOME_COLOR;
    public static final ModConfigSpec.IntValue WARP_COLOR;
    // motion
    public static final ModConfigSpec.BooleanValue ANIMATIONS;
    public static final ModConfigSpec.BooleanValue SCAN_LINE;
    public static final ModConfigSpec.BooleanValue SWEEP_PULSE;
    // performance
    public static final ModConfigSpec.DoubleValue SCAN_BUDGET_MS;
    public static final ModConfigSpec.IntValue RESCAN_DELAY_TICKS;
    public static final ModConfigSpec.IntValue MAX_REGIONS;
    public static final ModConfigSpec.IntValue SAVE_INTERVAL_SECONDS;
    public static final ModConfigSpec.BooleanValue OVERVIEW_TILES;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("key");
        HOLD_MS = b.comment("How long (milliseconds) the Codex key must be held to open the quick menu. A shorter press is a tap (world map).")
                .defineInRange("hold_ms", 250, 100, 1000);
        b.pop();

        b.push("minimap");
        MINIMAP_ON = b.comment("Show the minimap.").define("show", true);
        SHAPE = b.comment("PLATE (clipped corners), SQUARE or ROUND.").defineEnum("shape", Shape.PLATE);
        MINIMAP_SIZE = b.comment("Size on screen (GUI pixels).").defineInRange("size", 100, 48, 256);
        MINIMAP_X = b.comment("Distance from the left edge of the screen (GUI pixels).").defineInRange("x", 6, 0, 4000);
        MINIMAP_Y = b.comment("Distance from the top edge of the screen (GUI pixels).").defineInRange("y", 6, 0, 4000);
        ROTATE = b.comment("Turn the map with the player. Off = north always up.").define("turn_with_player", false);
        MOB_ICONS = b.comment("How mobs show on the minimap: DOTS, HEADS (each mob's own head), or HOLD (heads while the mob heads key is held).")
                .defineEnum("mob_icons", MobIcons.HOLD);
        MINIMAP_ZOOM = b.comment("Screen pixels per block.").defineInRange("zoom", 1.0, 0.25, 8.0);
        OPACITY = b.comment("Map opacity (0.2 - 1.0).").defineInRange("opacity", 1.0, 0.2, 1.0);
        INFO_COORDS = b.define("info_coordinates", true);
        INFO_BIOME = b.define("info_biome", true);
        INFO_TIME = b.define("info_time", false);
        INFO_LIGHT = b.define("info_light_level", false);
        INFO_BORDER = b.comment("Show the distance to the world border when close.").define("info_border_distance", true);
        HIDE_IN_MENUS = b.comment("Hide the minimap while a full menu is open.").define("hide_in_menus", true);
        b.pop();

        b.push("world_map");
        KEY_HINTS = b.comment("Show the key and mouse hints on the right rail.").define("key_hints", true);
        LAYER_CLAIMS = b.define("layer_claims", true);
        LAYER_MARKS = b.define("layer_marks", true);
        LAYER_PLAYERS = b.define("layer_players", true);
        LAYER_GRID = b.define("layer_chunk_grid", false);
        LAYER_SLIME = b.define("layer_slime_chunks", false);
        LAYER_BORDER = b.define("layer_world_border", true);
        LAYER_WAYSTONES = b.define("layer_waystones", true);
        LAYER_HOMES = b.define("layer_homes_and_warps", true);
        b.pop();

        b.push("marks");
        MARKS_IN_WORLD = b.comment("Draw marks in the world as floating emblems.").define("in_world", true);
        MARK_MAX_DISTANCE = b.comment("Farthest distance (blocks) a mark is drawn in the world.").defineInRange("max_distance", 2000, 16, 100000);
        MARK_DISTANCE = b.comment("Show the distance under marks in the world.").define("show_distance", true);
        MARK_SIZE = b.comment("Size of marks in the world.").defineInRange("size", 1.0, 0.5, 2.5);
        DEATH_POINTS_KEPT = b.comment("How many Death points are kept per dimension (oldest go first).").defineInRange("death_points_kept", 3, 1, 20);
        DEATH_POINT_CLEAR = b.comment("Remove a Death point once you walk back to it.").define("death_point_clear_on_reach", true);
        b.pop();

        b.push("sweep");
        SWEEP_PLAYERS = b.define("players", true);
        SWEEP_HOSTILE = b.define("hostile_mobs", true);
        SWEEP_PASSIVE = b.define("passive_mobs", false);
        SWEEP_PETS = b.define("pets", true);
        SWEEP_VEHICLES = b.define("boats_and_minecarts", false);
        SWEEP_ITEMS = b.define("items", false);
        SWEEP_NEAR_HEIGHT = b.comment("Only show mobs, pets, vehicles and items near your own height (players always show).")
                .define("only_near_my_height", true);
        SWEEP_HEIGHT_RANGE = b.comment("How many blocks above and below you count as near your height.")
                .defineInRange("height_range", 5, 1, 64);
        b.pop();

        b.push("lenses");
        LENS = b.comment("SURFACE, RELIEF, BIOME or NIGHT.").defineEnum("lens", Lens.SURFACE);
        DEPTH_AUTO = b.comment("Switch to Depth View by itself when underground, at any height.").define("depth_view_auto", true);
        b.pop();

        b.push("claims");
        CLAIM_OWN = b.comment("Colors as 0xRRGGBB numbers.").defineInRange("color_yours", 0x39FF8C, 0, 0xFFFFFF);
        CLAIM_TRUSTED = b.defineInRange("color_trusted", 0x46AAFF, 0, 0xFFFFFF);
        CLAIM_OTHERS = b.defineInRange("color_others", 0xFF9A2E, 0, 0xFFFFFF);
        CLAIM_SERVER = b.defineInRange("color_server", 0xC878FF, 0, 0xFFFFFF);
        CLAIM_OWN_COLOR = b.comment("Use each claim's own color from the server instead.").define("use_claim_color", false);
        COLORBLIND = b.comment("Colorblind-safe claim colors.").define("colorblind", false);
        b.pop();

        b.push("world_border");
        BORDER_MINIMAP = b.define("show_on_minimap", true);
        BORDER_WORLD_MAP = b.define("show_on_world_map", true);
        BORDER_WARN = b.comment("Show the distance to the border when this close (blocks).").defineInRange("warn_distance", 200, 8, 100000);
        BORDER_COLOR = b.defineInRange("color", 0xFF5050, 0, 0xFFFFFF);
        b.pop();

        b.push("colors");
        ACCENT = b.comment("Accent color for every screen (0xRRGGBB).").defineInRange("accent", 0x39FF8C, 0, 0xFFFFFF);
        PALETTE = b.comment("CODEX (cooler, muted) or VANILLA map colors.").defineEnum("palette", Palette.CODEX);
        RECENT_COLORS = b.comment("Last colors picked in the color wheel.")
                .defineListAllowEmpty("recent", List.of(), () -> 0, o -> o instanceof Integer);
        WAYSTONE_COLOR = b.defineInRange("waystone_color", 0x78AAFF, 0, 0xFFFFFF);
        HOME_COLOR = b.defineInRange("home_color", 0xFFBE50, 0, 0xFFFFFF);
        WARP_COLOR = b.defineInRange("warp_color", 0xC878FF, 0, 0xFFFFFF);
        b.pop();

        b.push("motion");
        ANIMATIONS = b.comment("Page animations (drawers slide, cards fade in).").define("page_animations", true);
        SCAN_LINE = b.comment("The scan line that sweeps the minimap now and then.").define("scan_line", true);
        SWEEP_PULSE = b.comment("The Sweep pulse around the player arrow.").define("sweep_pulse", true);
        b.pop();

        b.push("performance");
        SCAN_BUDGET_MS = b.comment("Time (milliseconds) the map may spend reading chunks each game tick.",
                "Lower it on slow PCs; mapping just takes a little longer.").defineInRange("scan_budget_ms", 2.0, 0.25, 20.0);
        RESCAN_DELAY_TICKS = b.comment("Ticks to wait after a block changes before its chunk is read again.")
                .defineInRange("rescan_delay_ticks", 20, 1, 200);
        MAX_REGIONS = b.comment("Most map regions (512 x 512 blocks each) kept in memory. Older ones are saved and dropped first.")
                .defineInRange("max_regions_in_memory", 48, 8, 512);
        SAVE_INTERVAL_SECONDS = b.comment("How often changed map regions are saved to disk.")
                .defineInRange("save_interval_seconds", 30, 5, 600);
        OVERVIEW_TILES = b.comment("Build small overview tiles so far zoom on the world map opens fast.").define("overview_tiles", true);
        b.pop();
        SPEC = b.build();
    }

    private ClientConfig() {}

    /** Sets a value and saves client.toml. */
    public static <T> void set(ModConfigSpec.ConfigValue<T> value, T v) {
        value.set(v);
        value.save();
    }
}
