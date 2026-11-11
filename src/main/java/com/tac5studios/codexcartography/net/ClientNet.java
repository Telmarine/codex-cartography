package com.tac5studios.codexcartography.net;

import com.tac5studios.codexcartography.claims.ClaimsData;
import com.tac5studios.codexcartography.client.ClientRules;
import com.tac5studios.codexcartography.client.MapSession;
import com.tac5studios.codexcartography.client.ServerMarkers;
import com.tac5studios.codexcartography.client.SlimeChunks;
import com.tac5studios.codexcartography.client.WorldBorders;
import it.unimi.dsi.fastutil.longs.LongList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/** Client side of the network: what the server sends, and what the map asks for. */
public final class ClientNet {

    private static long lastLook;

    private ClientNet() {}

    /** True when the server has Cartography's server side. */
    public static boolean serverHasMod() {
        ClientPacketListener c = Minecraft.getInstance().getConnection();
        return c != null && c.hasChannel(LookPayload.TYPE);
    }

    public static boolean serverHasClaims() {
        ClientPacketListener c = Minecraft.getInstance().getConnection();
        return c != null && c.hasChannel(ClaimsPayload.TYPE);
    }

    public static void sendLookAt(BlockPos pos) {
        long now = System.currentTimeMillis();
        if (now - lastLook < 500) return;
        lastLook = now;
        ClientPacketListener c = Minecraft.getInstance().getConnection();
        if (c != null) c.send(new LookPayload(pos));
    }

    /** The map's travel action: the server checks the rule and the cartography.travel permission. */
    public static void travel(int x, int z) {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p != null) p.connection.sendCommand("cartography travel " + x + " " + z);
    }

    /** Claim or unclaim chunks through Elements: Claims (Claims does every check). */
    public static void claim(ResourceLocation dim, LongList chunks, boolean claim) {
        CompoundTag t = new CompoundTag();
        t.putString("op", claim ? "claim" : "unclaim");
        t.putString("dim", dim.toString());
        t.putLongArray("chunks", chunks.toLongArray());
        sendClaims(t);
    }

    public static void sendClaims(CompoundTag tag) {
        ClientPacketListener c = Minecraft.getInstance().getConnection();
        if (c != null && c.hasChannel(ClaimsPayload.TYPE)) c.send(new ClaimsPayload(tag));
    }

    static void onSync(CompoundTag t) {
        switch (t.getString("kind")) {
            case "rules" -> rules(t);
            case "borders" -> {
                for (Tag e : t.getList("borders", Tag.TAG_COMPOUND)) {
                    CompoundTag b = (CompoundTag) e;
                    ResourceLocation dim = ResourceLocation.tryParse(b.getString("dim"));
                    if (dim != null) {
                        WorldBorders.set(dim, b.getDouble("cx"), b.getDouble("cz"), b.getDouble("size"), b.getDouble("target"), b.getLong("ms"));
                    }
                }
            }
            case "markers" -> {
                List<ServerMarkers.Marker> list = new ArrayList<>();
                for (Tag e : t.getList("markers", Tag.TAG_COMPOUND)) {
                    CompoundTag m = (CompoundTag) e;
                    ResourceLocation dim = ResourceLocation.tryParse(m.getString("dim"));
                    if (dim != null) list.add(new ServerMarkers.Marker(m.getString("kind"), m.getString("name"), dim, m.getInt("x"), m.getInt("y"), m.getInt("z")));
                }
                ServerMarkers.set(list);
            }
            case "slime" -> SlimeChunks.set(t.getLongArray("chunks"));
            case "discovered" -> {
                MapSession s = MapSession.get();
                ResourceLocation dim = ResourceLocation.tryParse(t.getString("dim"));
                if (s != null && dim != null) {
                    s.marks().discovered(t.getString("structure"), t.getString("emblem"), t.getString("name"), dim, t.getInt("x"), t.getInt("y"), t.getInt("z"));
                }
            }
            default -> { }
        }
    }

    private static void rules(CompoundTag t) {
        ClientRules.fromServer = true;
        ClientRules.sweepPlayers = t.getBoolean("sweep_players");
        ClientRules.sweepMobs = t.getBoolean("sweep_mobs");
        ClientRules.sweepPets = t.getBoolean("sweep_pets");
        ClientRules.sweepVehicles = t.getBoolean("sweep_vehicles");
        ClientRules.sweepItems = t.getBoolean("sweep_items");
        ClientRules.depthView = t.getBoolean("depth_view");
        ClientRules.slimeChunks = t.getBoolean("slime_chunks");
        ClientRules.travel = t.getBoolean("travel");
        ClientRules.canTravel = t.getBoolean("can_travel");
        ClientRules.deathPoints = t.getBoolean("death_points");
        ClientRules.discoveredStructures = t.getBoolean("discovered_structures");
        ClientRules.markSharing = t.getBoolean("mark_sharing");
        ClientRules.claims = t.getBoolean("claims");
        ClientRules.claimMarks = t.getBoolean("claim_marks");
        ClientRules.waystones = t.getBoolean("waystones");
        ClientRules.nexusMarkers = t.getBoolean("nexus_markers");
        ClientRules.worldBorder = t.getBoolean("world_border");
        ClientRules.noSweep.clear();
        ClientRules.noDepth.clear();
        ClientRules.dimensionNames.clear();
        for (Tag e : t.getList("no_sweep", Tag.TAG_STRING)) addDim(ClientRules.noSweep, e.getAsString());
        for (Tag e : t.getList("no_depth", Tag.TAG_STRING)) addDim(ClientRules.noDepth, e.getAsString());
        ClientRules.forceOn.clear();
        for (Tag e : t.getList("force_on", Tag.TAG_STRING)) ClientRules.forceOn.add(e.getAsString());
        CompoundTag names = t.getCompound("dimension_names");
        for (String k : names.getAllKeys()) {
            ResourceLocation d = ResourceLocation.tryParse(k);
            if (d != null) ClientRules.dimensionNames.put(d, names.getString(k));
        }
        if (!ClientRules.slimeChunks) SlimeChunks.clear();
    }

    private static void addDim(java.util.Set<ResourceLocation> set, String s) {
        ResourceLocation d = ResourceLocation.tryParse(s);
        if (d != null) set.add(d);
    }

    /** Messages from Elements: Claims' map channel. */
    static void onClaims(CompoundTag t) {
        switch (t.getString("op")) {
            case "hello", "counts" -> {
                ClaimsData.present = true;
                if (t.contains("claim_range")) ClaimsData.claimRange = t.getInt("claim_range");
                ClaimsData.chunksUsed = t.getInt("chunks_used");
                ClaimsData.chunksAllowed = t.getInt("chunks_allowed");
                ClaimsData.propertiesUsed = t.getInt("props_used");
                ClaimsData.propertiesMax = t.getInt("props_max");
            }
            case "claims" -> {
                ClaimsData.present = true;
                ResourceLocation dim = ResourceLocation.tryParse(t.getString("dim"));
                if (dim == null) return;
                for (Tag e : t.getList("claims", Tag.TAG_COMPOUND)) ClaimsData.put(claim(dim, (CompoundTag) e));
            }
            case "remove" -> {
                for (int id : t.getIntArray("ids")) ClaimsData.remove(id);
            }
            case "clear" -> ClaimsData.clear();
            case "result" -> {
                LocalPlayer p = Minecraft.getInstance().player;
                if (p != null && !t.getString("message").isEmpty()) p.displayClientMessage(Component.literal(t.getString("message")), true);
            }
            default -> { }
        }
    }

    private static ClaimsData.Claim claim(ResourceLocation dim, CompoundTag c) {
        List<ClaimsData.SubArea> subs = new ArrayList<>();
        for (Tag e : c.getList("subs", Tag.TAG_COMPOUND)) {
            CompoundTag s = (CompoundTag) e;
            subs.add(new ClaimsData.SubArea(s.getString("name"), s.getInt("x1"), s.getInt("y1"), s.getInt("z1"), s.getInt("x2"), s.getInt("y2"), s.getInt("z2")));
        }
        List<ClaimsData.ClaimMark> marks = new ArrayList<>();
        for (Tag e : c.getList("marks", Tag.TAG_COMPOUND)) {
            CompoundTag m = (CompoundTag) e;
            marks.add(new ClaimsData.ClaimMark(m.getString("id"), m.getString("name"), m.getString("emblem"), m.getInt("color"),
                    m.getInt("x"), m.getInt("y"), m.getInt("z")));
        }
        return new ClaimsData.Claim(c.getInt("id"), dim, c.getString("name"), c.getString("owner"), c.getInt("standing"),
                c.getString("rank"), c.getBoolean("private"), c.contains("color") ? c.getInt("color") : -1, c.getLongArray("chunks"),
                subs, marks, c.getBoolean("manage"), c.getBoolean("active"));
    }
}
