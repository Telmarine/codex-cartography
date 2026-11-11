package com.tac5studios.codexcartography.server;

import com.tac5studios.codexcartography.CodexCartography;
import com.tac5studios.codexcartography.config.Features;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Markers from other server mods, read through their own code only when they're installed:
 * Waystones the player has activated, and Elements: Nexus homes (the player's own) and warps (if they can use /warp).
 */
public final class Markers {

    private static boolean waystonesBroken;
    private static boolean nexusBroken;

    private Markers() {}

    public static CompoundTag tag(ServerPlayer player) {
        ListTag list = new ListTag();
        if (Features.WAYSTONES_MARKERS.get() && ServerRules.on("markers.waystones") && ModList.get().isLoaded("waystones")) waystones(player, list);
        if (Features.NEXUS_MARKERS.get() && ServerRules.on("markers.nexus_markers") && ModList.get().isLoaded("elements_nexus")) nexus(player, list);
        CompoundTag t = new CompoundTag();
        t.putString("kind", "markers");
        t.put("markers", list);
        return t;
    }

    private static void add(ListTag list, String kind, String name, String dim, int x, int y, int z) {
        CompoundTag m = new CompoundTag();
        m.putString("kind", kind);
        m.putString("name", name);
        m.putString("dim", dim);
        m.putInt("x", x);
        m.putInt("y", y);
        m.putInt("z", z);
        list.add(m);
    }

    private static void waystones(ServerPlayer player, ListTag list) {
        if (waystonesBroken) return;
        try {
            Class<?> mgr = Class.forName("net.blay09.mods.waystones.core.PlayerWaystoneManager");
            Method get = mgr.getMethod("getActivatedWaystones", Player.class);
            Object result = get.invoke(null, player);
            if (!(result instanceof Collection<?> all)) return;
            for (Object w : all) {
                Object name = w.getClass().getMethod("getName").invoke(w);
                Object pos = w.getClass().getMethod("getPos").invoke(w);
                Object dim = w.getClass().getMethod("getDimension").invoke(w);
                if (!(pos instanceof BlockPos bp) || !(dim instanceof ResourceKey<?> key)) continue;
                String n = name instanceof Component c ? c.getString() : String.valueOf(name);
                add(list, "waystone", n, key.location().toString(), bp.getX(), bp.getY(), bp.getZ());
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            waystonesBroken = true;
            CodexCartography.LOGGER.warn("[Cartography] Couldn't read waystones (this Waystones version isn't supported): {}", e.toString());
        }
    }

    private static void nexus(ServerPlayer player, ListTag list) {
        if (nexusBroken) return;
        try {
            Class<?> homes = Class.forName("com.tac5studios.elementsnexus.homes.Homes");
            Object map = homes.getMethod("of", UUID.class).invoke(null, player.getUUID());
            if (map instanceof Map<?, ?> m) {
                for (Map.Entry<?, ?> e : m.entrySet()) spot(list, "home", String.valueOf(e.getKey()), e.getValue());
            }
            var warp = player.server.getCommands().getDispatcher().getRoot().getChild("warp");
            if (warp != null && warp.canUse(player.createCommandSourceStack())) {
                Class<?> storageClass = Class.forName("com.tac5studios.elementsnexus.storage.Storage");
                Object storage = storageClass.getMethod("get").invoke(null);
                Class<?> spotClass = Class.forName("com.tac5studios.elementsnexus.teleport.Teleports$Spot");
                Object keys = storage.getClass().getMethod("keys", String.class).invoke(storage, "warps");
                Method get = storage.getClass().getMethod("get", String.class, String.class, Type.class);
                if (keys instanceof List<?> names) {
                    for (Object n : names) spot(list, "warp", String.valueOf(n), get.invoke(storage, "warps", String.valueOf(n), spotClass));
                }
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            nexusBroken = true;
            CodexCartography.LOGGER.warn("[Cartography] Couldn't read Elements: Nexus homes and warps: {}", e.toString());
        }
    }

    private static void spot(ListTag list, String kind, String name, Object spot) throws ReflectiveOperationException {
        if (spot == null) return;
        Class<?> c = spot.getClass();
        String dim = String.valueOf(c.getMethod("dim").invoke(spot));
        double x = ((Number) c.getMethod("x").invoke(spot)).doubleValue();
        double y = ((Number) c.getMethod("y").invoke(spot)).doubleValue();
        double z = ((Number) c.getMethod("z").invoke(spot)).doubleValue();
        add(list, kind, name, dim, (int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
    }
}
