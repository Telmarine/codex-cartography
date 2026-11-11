package com.tac5studios.codexcartography.server;

import com.tac5studios.codexcartography.CodexCartography;
import com.tac5studios.codexcartography.config.Features;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.permission.PermissionAPI;

/** config/codex_cartography/server.toml and messages.toml. */
public final class ServerRules {

    public static final TomlFile RULES = new TomlFile("server.toml");
    public static final TomlFile MESSAGES = new TomlFile("messages.toml");

    private ServerRules() {}

    /** Loads both files. Returns null when fine, else what went wrong. */
    public static String load() {
        StringBuilder err = new StringBuilder();
        for (TomlFile f : new TomlFile[] {RULES, MESSAGES}) {
            try {
                f.load();
            } catch (Exception e) {
                CodexCartography.LOGGER.error("[Cartography] Couldn't read {}: {}", f.path(), e.getMessage());
                if (!err.isEmpty()) err.append("; ");
                err.append(f.path().getFileName()).append(": ").append(e.getMessage());
            }
        }
        return err.isEmpty() ? null : err.toString();
    }

    public static boolean on(String path) {
        return RULES.bool(path, true);
    }

    /** A server message with & colors and {placeholders} filled in. */
    public static String message(String key, String... pairs) {
        String s = MESSAGES.string(key, key);
        for (int i = 0; i + 1 < pairs.length; i += 2) s = s.replace("{" + pairs[i] + "}", pairs[i + 1]);
        return s.replaceAll("&([0-9a-fk-orA-FK-OR])", "§$1");
    }

    /** The rules as sent to one player. */
    public static CompoundTag tag(ServerPlayer player) {
        CompoundTag t = new CompoundTag();
        t.putString("kind", "rules");
        // the server's own features.toml switches count too: a feature switched off there is off for everyone
        boolean sweep = Features.SWEEP.get();
        t.putBoolean("sweep_players", sweep && on("sweep.players"));
        t.putBoolean("sweep_mobs", sweep && on("sweep.mobs"));
        t.putBoolean("sweep_pets", sweep && on("sweep.pets"));
        t.putBoolean("sweep_vehicles", sweep && on("sweep.vehicles"));
        t.putBoolean("sweep_items", sweep && on("sweep.items"));
        boolean bypass = PermissionAPI.getPermission(player, Perms.BYPASS);
        if (bypass && sweep) {
            for (String k : new String[] {"sweep_players", "sweep_mobs", "sweep_pets", "sweep_vehicles", "sweep_items"}) t.putBoolean(k, true);
        }
        t.putBoolean("depth_view", Features.DEPTH_VIEW.get() && (bypass || on("map.depth_view")));
        t.putBoolean("slime_chunks", RULES.bool("map.slime_chunks", false));
        boolean travel = RULES.bool("map.travel", false);
        t.putBoolean("travel", travel);
        t.putBoolean("can_travel", travel && PermissionAPI.getPermission(player, Perms.TRAVEL));
        t.putBoolean("death_points", Features.DEATH_POINTS.get() && on("map.death_points"));
        t.putBoolean("discovered_structures", on("map.discovered_structures"));
        t.putBoolean("mark_sharing", on("map.mark_sharing"));
        t.putBoolean("world_border", on("map.world_border"));
        t.putBoolean("claims", Features.CLAIMS_LAYER.get() && on("claims.claims"));
        t.putBoolean("claim_marks", Features.CLAIM_MARKS.get() && on("claims.claim_marks"));
        t.putBoolean("waystones", Features.WAYSTONES_MARKERS.get() && on("markers.waystones"));
        t.putBoolean("nexus_markers", Features.NEXUS_MARKERS.get() && on("markers.nexus_markers"));
        ListTag noSweep = new ListTag();
        if (!bypass) for (String d : RULES.strings("dimensions.no_sweep")) noSweep.add(StringTag.valueOf(d.trim()));
        t.put("no_sweep", noSweep);
        ListTag noDepth = new ListTag();
        if (!bypass) for (String d : RULES.strings("dimensions.no_depth_view")) noDepth.add(StringTag.valueOf(d.trim()));
        t.put("no_depth", noDepth);
        // overlap guard: the server's features.toml decides what stays on next to other map mods
        ListTag forceOn = new ListTag();
        for (String f : Features.FORCE_ON.get()) forceOn.add(StringTag.valueOf(f));
        t.put("force_on", forceOn);
        CompoundTag names = new CompoundTag();
        for (String line : RULES.strings("dimensions.names")) {
            int eq = line.indexOf('=');
            if (eq > 0) names.putString(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
        }
        t.put("dimension_names", names);
        return t;
    }
}
