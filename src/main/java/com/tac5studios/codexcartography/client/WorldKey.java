package com.tac5studios.codexcartography.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Path;
import java.util.Locale;

/**
 * Which map folder the player is in: one per server address, or per save folder in single player.
 *
 * @param folder  folder name under codex_cartography/worlds/
 * @param name    name shown to the player
 * @param address server address ("" in single player)
 */
public record WorldKey(String folder, String name, String address) {

    public static WorldKey of(Minecraft mc) {
        IntegratedServer local = mc.getSingleplayerServer();
        if (local != null) {
            Path save = local.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize().getFileName();
            String saveName = save != null ? save.toString() : "world";
            return new WorldKey("sp_" + clean(saveName), local.getWorldData().getLevelName(), "");
        }
        ServerData server = mc.getCurrentServer();
        String address;
        if (server != null) {
            address = server.ip;
        } else {
            ClientPacketListener connection = mc.getConnection();
            address = connection != null ? String.valueOf(connection.getConnection().getRemoteAddress()) : "unknown";
        }
        address = address.trim().toLowerCase(Locale.ROOT);
        if (address.endsWith(":25565")) address = address.substring(0, address.length() - ":25565".length());
        return new WorldKey("mp_" + clean(address), server != null ? server.name : address, address);
    }

    /** Lower case letters, digits, dot, dash and underscore only. */
    public static String clean(String s) {
        String out = s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "_");
        return out.isEmpty() ? "_" : out;
    }
}
