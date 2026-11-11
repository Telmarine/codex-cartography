package com.tac5studios.codexcartography.server;

import com.tac5studios.codexcartography.CodexCartography;
import com.tac5studios.codexcartography.net.Net;
import com.tac5studios.codexcartography.net.SyncPayload;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** The server side: loads the rules and sends rules, world borders, markers and slime chunks to players with the map. */
@EventBusSubscriber(modid = CodexCartography.MOD_ID)
@SuppressWarnings("unused") // handlers are called by the event bus
public final class ServerSync {

    private static final Map<UUID, Long> SLIME_CENTER = new HashMap<>();
    private static String lastBorders = "";

    private ServerSync() {}

    @SubscribeEvent
    public static void onStarting(ServerStartingEvent event) {
        ServerRules.load();
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        CartographyCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onPermissions(PermissionGatherEvent.Nodes event) {
        Perms.register(event);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) sendAll(p);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SLIME_CENTER.remove(event.getEntity().getUUID());
        Discovery.forget(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            SLIME_CENTER.remove(p.getUUID());
            slime(p);
        }
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        int t = server.getTickCount();
        if (t % 20 == 0) for (ServerPlayer p : server.getPlayerList().getPlayers()) slime(p);
        if (t % 100 == 0) {
            CompoundTag b = borders(server);
            String s = b.toString();
            if (!s.equals(lastBorders)) {
                lastBorders = s;
                for (ServerPlayer p : server.getPlayerList().getPlayers()) Net.send(p, b);
            }
        }
        if (t % 600 == 0) for (ServerPlayer p : server.getPlayerList().getPlayers()) Net.send(p, Markers.tag(p));
    }

    public static void sendAll(ServerPlayer p) {
        if (!p.connection.hasChannel(SyncPayload.TYPE)) return;
        Net.send(p, ServerRules.tag(p));
        Net.send(p, borders(p.server));
        Net.send(p, Markers.tag(p));
        SLIME_CENTER.remove(p.getUUID());
        slime(p);
    }

    public static void resendAll(MinecraftServer server) {
        lastBorders = "";
        for (ServerPlayer p : server.getPlayerList().getPlayers()) sendAll(p);
    }

    private static CompoundTag borders(MinecraftServer server) {
        ListTag list = new ListTag();
        if (ServerRules.on("map.world_border")) {
            for (ServerLevel level : server.getAllLevels()) {
                WorldBorder w = level.getWorldBorder();
                CompoundTag b = new CompoundTag();
                b.putString("dim", level.dimension().location().toString());
                b.putDouble("cx", w.getCenterX());
                b.putDouble("cz", w.getCenterZ());
                b.putDouble("size", w.getSize());
                b.putDouble("target", w.getLerpTarget());
                b.putLong("ms", w.getLerpRemainingTime());
                list.add(b);
            }
        }
        CompoundTag t = new CompoundTag();
        t.putString("kind", "borders");
        t.put("borders", list);
        return t;
    }

    /** Slime chunks within 8 chunks of the player, resent when they move to another chunk. */
    @SuppressWarnings("resource") // the level belongs to the server
    private static void slime(ServerPlayer p) {
        if (!ServerRules.RULES.bool("map.slime_chunks", false) || !p.connection.hasChannel(SyncPayload.TYPE)) return;
        ChunkPos c = p.chunkPosition();
        Long last = SLIME_CENTER.get(p.getUUID());
        if (last != null && last == c.toLong()) return;
        SLIME_CENTER.put(p.getUUID(), c.toLong());
        long seed = p.serverLevel().getSeed();
        boolean slimeWorld = p.serverLevel().dimensionType().natural() && !p.serverLevel().dimensionType().hasCeiling();
        it.unimi.dsi.fastutil.longs.LongArrayList out = new it.unimi.dsi.fastutil.longs.LongArrayList();
        if (slimeWorld) {
            for (int dz = -8; dz <= 8; dz++) {
                for (int dx = -8; dx <= 8; dx++) {
                    int cx = c.x + dx;
                    int cz = c.z + dz;
                    if (WorldgenRandom.seedSlimeChunk(cx, cz, seed, 987234911L).nextInt(10) == 0) out.add(ChunkPos.asLong(cx, cz));
                }
            }
        }
        CompoundTag t = new CompoundTag();
        t.putString("kind", "slime");
        t.putLongArray("chunks", out.toLongArray());
        Net.send(p, t);
    }
}
