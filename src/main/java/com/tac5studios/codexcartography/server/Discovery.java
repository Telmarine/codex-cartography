package com.tac5studios.codexcartography.server;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server check for discovered structures: is the block under a player's crosshair part of a structure?
 * Any structure counts (villages, ruined portals, temples, modded ones), except those in [discovered] skip.
 */
public final class Discovery {

    private static final Map<UUID, Long> LAST = new ConcurrentHashMap<>();

    private Discovery() {}

    @SuppressWarnings("resource") // the level belongs to the server
    public static void look(ServerPlayer player, BlockPos pos) {
        if (!ServerRules.on("map.discovered_structures")) return;
        long now = System.currentTimeMillis();
        Long last = LAST.get(player.getUUID());
        if (last != null && now - last < 400) return;
        LAST.put(player.getUUID(), now);
        if (player.blockPosition().distSqr(pos) > 160 * 160) return;
        ServerLevel level = player.serverLevel();
        if (!level.isLoaded(pos)) return;
        Set<String> skip = new HashSet<>();
        for (String s : ServerRules.RULES.strings("discovered.skip")) skip.add(s.trim().toLowerCase(Locale.ROOT));
        var registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        for (Structure structure : level.structureManager().getAllStructuresAt(pos).keySet()) {
            ResourceLocation id = registry.getKey(structure);
            if (id == null || skip.contains(id.toString())) continue;
            StructureStart start = level.structureManager().getStructureWithPieceAt(pos, structure);
            if (!start.isValid()) continue;
            BlockPos c = start.getBoundingBox().getCenter();
            CompoundTag t = new CompoundTag();
            t.putString("kind", "discovered");
            t.putString("structure", id.toString());
            t.putString("emblem", emblem(id.getPath()));
            t.putString("name", readable(id.getPath()));
            t.putString("dim", level.dimension().location().toString());
            t.putInt("x", c.getX());
            t.putInt("y", c.getY());
            t.putInt("z", c.getZ());
            com.tac5studios.codexcartography.net.Net.send(player, t);
            return;
        }
    }

    /** The mark emblem for a structure, by its id (works for most modded names too). */
    static String emblem(String path) {
        if (path.contains("ancient_city")) return "ancient_city";
        if (path.contains("monument")) return "ocean_monument";
        if (path.contains("ruined_portal")) return "portal";
        if (path.contains("village")) return "village";
        if (path.contains("outpost")) return "flag";
        if (path.contains("mansion") || path.contains("bastion") || path.contains("fortress") || path.contains("castle")) return "keep";
        if (path.contains("pyramid") || path.contains("temple") || path.contains("shrine")) return "shrine";
        if (path.contains("igloo")) return "home";
        if (path.contains("swamp_hut") || path.contains("witch")) return "skull";
        if (path.contains("shipwreck") || path.contains("treasure")) return "treasure";
        if (path.contains("ruin")) return "ruin";
        if (path.contains("stronghold") || path.contains("trial_chambers")) return "gate";
        if (path.contains("mineshaft")) return "mine";
        if (path.contains("end_city")) return "star";
        return "question";
    }

    /** "ruined_portal_mountain" -> "Ruined Portal Mountain" (a fallback; the client uses its own names when it has one). */
    static String readable(String path) {
        String last = path.substring(path.lastIndexOf('/') + 1);
        StringBuilder b = new StringBuilder();
        for (String part : last.split("_")) {
            if (part.isEmpty()) continue;
            if (!b.isEmpty()) b.append(' ');
            b.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return b.toString();
    }

    public static void forget(UUID id) {
        LAST.remove(id);
    }
}
