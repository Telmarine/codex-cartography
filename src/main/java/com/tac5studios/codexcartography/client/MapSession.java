package com.tac5studios.codexcartography.client;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tac5studios.codexcartography.CodexCartography;
import com.tac5studios.codexcartography.config.ClientConfig;
import com.tac5studios.codexcartography.config.Features;
import com.tac5studios.codexcartography.mark.MarkStore;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * The map for the server or world the player is in: picks the folder, switches dimensions, feeds chunks to the
 * scanner, runs Depth View when underground and saves on a timer and when leaving.
 */
public final class MapSession {

    private static MapSession current;

    private final WorldKey world;
    private final Path worldDir;
    private final MarkStore marks;
    private final ScanQueue queue = new ScanQueue();
    private final ScanQueue depthQueue = new ScanQueue();
    private final LongOpenHashSet seen = new LongOpenHashSet();
    /** Stores of other dimensions opened for viewing on the world map (closed when the map closes). */
    private final Map<ResourceLocation, DimensionStore> viewing = new HashMap<>();
    private ResourceKey<Level> dimension;
    private DimensionStore store;
    private DimensionStore depthStore;
    private int depthBand = Integer.MIN_VALUE;
    private boolean depthForced;
    private boolean autoUnderground;
    private int autoHold;
    private long ticks;
    private long lastSave;

    private MapSession(WorldKey world, Path worldDir) {
        this.world = world;
        this.worldDir = worldDir;
        this.marks = new MarkStore(worldDir);
        this.lastSave = System.currentTimeMillis();
    }

    public static MapSession get() { return current; }
    public WorldKey world() { return world; }
    public Path worldDir() { return worldDir; }
    public MarkStore marks() { return marks; }
    public ResourceKey<Level> dimension() { return dimension; }
    public DimensionStore surface() { return store; }

    /** The layer the maps should draw now: the Depth View band when it's on, else the surface. */
    public DimensionStore active() {
        return depthStore != null ? depthStore : store;
    }

    public boolean depthOn() { return depthStore != null; }

    /** Turns Depth View on or off by hand (the quick menu); auto mode switches it while underground. */
    public void toggleDepth() {
        depthForced = !depthForced;
    }

    public static Path root() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve(CodexCartography.MOD_ID);
    }

    static void tick(Minecraft mc) {
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) {
            close();
            return;
        }
        if (!Features.mapping()) return;
        if (current == null) open(mc);
        current.update(mc, level, mc.player);
    }

    private static void open(Minecraft mc) {
        WorldKey key = WorldKey.of(mc);
        current = new MapSession(key, root().resolve("worlds").resolve(key.folder()));
        current.touchWorldFile(false);
        OverlapGuard.warnOnce(mc);
        CodexCartography.LOGGER.info("[Cartography] Mapping {} in {}", key.name(), current.worldDir);
    }

    /** Saves everything and ends the session (leaving the world or server). */
    static void close() {
        com.tac5studios.codexcartography.hud.MobHeads.clear();
        if (current == null) return;
        MapSession s = current;
        current = null;
        if (s.store != null) s.store.close();
        if (s.depthStore != null) s.depthStore.close();
        s.closeViewing();
        s.marks.save();
        s.touchWorldFile(true);
        ClientRules.reset();
        ServerMarkers.clear();
        SlimeChunks.clear();
        WorldBorders.clear();
        com.tac5studios.codexcartography.claims.ClaimsData.clear();
        MapIO.await(5000);
    }

    static void chunkLoaded(long chunk) {
        if (current != null && current.store != null && current.seen.add(chunk)) current.queue.add(chunk);
    }

    static void chunkUnloaded(long chunk) {
        if (current != null) current.seen.remove(chunk);
    }

    /** Called when a block changes on the client; the chunk is read again after a short delay. */
    public static void blockChanged(BlockPos pos) {
        if (current != null && current.store != null) {
            long key = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
            current.queue.markDirty(key, current.ticks);
            if (current.depthStore != null) current.depthQueue.markDirty(key, current.ticks);
        }
    }

    /** One line for the action bar: how much of this dimension is mapped. */
    static Component status() {
        if (current == null || current.store == null) return Component.translatable("codex_cartography.status.off");
        return Component.translatable("codex_cartography.status", Dims.name(current.dimension),
                String.format("%,d", current.store.chunkCount()));
    }

    /** The store of any dimension of this world: the live one, or one opened for viewing. */
    public DimensionStore storeFor(ResourceLocation dim) {
        if (dimension != null && dim.equals(dimension.location())) return active();
        return viewing.computeIfAbsent(dim, d -> new DimensionStore(worldDir.resolve(Dims.folder(d)), false));
    }

    /** Ticks stores opened for viewing other dimensions (the world map calls this each frame it shows one). */
    public void tickViewing() {
        for (DimensionStore s : viewing.values()) s.tick();
    }

    /** Frees stores opened for viewing (the world map closed). */
    public void closeViewing() {
        for (DimensionStore s : viewing.values()) s.close();
        viewing.clear();
    }

    private void update(Minecraft mc, ClientLevel level, LocalPlayer player) {
        ticks++;
        if (store == null || !level.dimension().equals(dimension)) switchDimension(level);
        if (ticks % 20 == 0) findLoadedChunks(mc, level);
        updateDepth(level, player);
        store.tick();
        double share = depthStore != null ? 0.5 : 1.0;
        queue.process(level, store, ticks, Integer.MIN_VALUE, share);
        if (depthStore != null) {
            if (ticks % 20 == 0) queueAround(level, player, depthQueue, 6);
            depthStore.tick();
            // read from just above the player's head, so shallow caves and tunnels show at any height
            depthQueue.process(level, depthStore, ticks, player.getBlockY() + 2, 0.5);
        }
        marks.tick(mc, level, player);
        long now = System.currentTimeMillis();
        if (now - lastSave >= ClientConfig.SAVE_INTERVAL_SECONDS.get() * 1000L) {
            lastSave = now;
            store.saveDirty();
            if (depthStore != null) depthStore.saveDirty();
            marks.save();
        }
    }

    private void updateDepth(ClientLevel level, LocalPlayer player) {
        boolean allowed = Features.DEPTH_VIEW.get() && ClientRules.depthView
                && !ClientRules.noDepth.contains(level.dimension().location());
        if (level.dimensionType().hasCeiling()) {
            // under a roof (the Nether) the map always follows the player's height
            autoUnderground = true;
            autoHold = 0;
        } else if (ClientConfig.DEPTH_AUTO.get()) {
            if (ticks % 5 == 0) {
                boolean now = underground(level, player);
                // the new state has to hold for a second before the map switches, so it doesn't flicker at cave mouths
                if (now == autoUnderground) autoHold = 0;
                else if (++autoHold >= 4) {
                    autoUnderground = now;
                    autoHold = 0;
                }
            }
        } else {
            autoUnderground = false;
            autoHold = 0;
        }
        boolean on = allowed && (autoUnderground != depthForced);
        int band = Math.floorDiv(player.getBlockY(), 8);
        if (!on) {
            if (depthStore != null) {
                depthStore.close();
                depthStore = null;
                depthQueue.clear();
            }
            return;
        }
        if (depthStore == null || band != depthBand) {
            if (depthStore != null) depthStore.close();
            depthBand = band;
            depthStore = new DimensionStore(worldDir.resolve(Dims.folder(dimension.location())).resolve("depth").resolve("y" + band), true);
            depthQueue.clear();
            queueAround(level, player, depthQueue, 6);
        }
    }

    /**
     * Underground at any height: solid ground (not leaves, not water) somewhere above the player's head,
     * and little daylight reaching them. Mountain caves, deep caves and covered tunnels all count;
     * forests, open overhangs and swimming don't.
     */
    private static boolean underground(ClientLevel level, LocalPlayer player) {
        BlockPos eye = BlockPos.containing(player.getEyePosition());
        if (level.getBrightness(LightLayer.SKY, eye) >= 8) return false;
        int top = Math.min(level.getMaxBuildHeight() - 1,
                level.getHeight(Heightmap.Types.WORLD_SURFACE, eye.getX(), eye.getZ()));
        BlockPos.MutableBlockPos p = eye.mutable();
        for (int y = eye.getY() + 1; y <= top; y++) {
            p.setY(y);
            BlockState st = level.getBlockState(p);
            if (!st.is(BlockTags.LEAVES) && !st.getCollisionShape(level, p).isEmpty()) return true;
        }
        return false;
    }

    private void switchDimension(ClientLevel level) {
        if (store != null) store.close();
        if (depthStore != null) depthStore.close();
        depthStore = null;
        dimension = level.dimension();
        viewing.remove(dimension.location());
        store = new DimensionStore(worldDir.resolve(Dims.folder(dimension.location())), false);
        queue.clear();
        depthQueue.clear();
        seen.clear();
    }

    private static void queueAround(ClientLevel level, LocalPlayer player, ScanQueue q, int r) {
        ChunkPos center = player.chunkPosition();
        for (int dz = -r; dz <= r; dz++) {
            for (int dx = -r; dx <= r; dx++) {
                if (level.getChunkSource().getChunk(center.x + dx, center.z + dz, ChunkStatus.FULL, false) != null) {
                    q.add(ChunkPos.asLong(center.x + dx, center.z + dz));
                }
            }
        }
    }

    /** Catches chunks that loaded before the session started (and any load event that was missed). */
    private void findLoadedChunks(Minecraft mc, ClientLevel level) {
        if (mc.player == null) return;
        ChunkPos center = mc.player.chunkPosition();
        int r = mc.options.getEffectiveRenderDistance() + 1;
        for (int dz = -r; dz <= r; dz++) {
            for (int dx = -r; dx <= r; dx++) {
                long key = ChunkPos.asLong(center.x + dx, center.z + dz);
                if (!seen.contains(key) && level.getChunkSource().getChunk(center.x + dx, center.z + dz, ChunkStatus.FULL, false) != null) {
                    seen.add(key);
                    queue.add(key);
                }
            }
        }
    }

    /** codex_cartography/worlds/&lt;world&gt;/world.json: name, address, first mapped and last visited. */
    private void touchWorldFile(boolean leaving) {
        Path file = worldDir.resolve("world.json");
        WorldKey key = world;
        MapIO.run("save " + file, () -> {
            JsonObject json = Files.exists(file)
                    ? JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject()
                    : new JsonObject();
            long now = System.currentTimeMillis();
            if (!json.has("name")) json.addProperty("name", key.name());
            json.addProperty("address", key.address());
            if (!json.has("first_mapped")) json.addProperty("first_mapped", now);
            json.addProperty("last_visited", now);
            if (leaving) json.addProperty("last_left", now);
            Files.createDirectories(worldDir);
            Files.writeString(file, new GsonBuilder().setPrettyPrinting().create().toJson(json), StandardCharsets.UTF_8);
        });
    }

    /** Reads world.json (for the Map data page). */
    public JsonObject worldInfo() {
        Path file = worldDir.resolve("world.json");
        try {
            if (Files.exists(file)) return JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception e) {
            CodexCartography.LOGGER.warn("[Cartography] Couldn't read {}: {}", file, e.toString());
        }
        return new JsonObject();
    }

    /** Renames this map (shown on the Map data page and the world map header). */
    public void rename(String name) {
        Path file = worldDir.resolve("world.json");
        MapIO.run("rename map", () -> {
            JsonObject json = Files.exists(file)
                    ? JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject() : new JsonObject();
            json.addProperty("name", name);
            Files.createDirectories(worldDir);
            Files.writeString(file, new GsonBuilder().setPrettyPrinting().create().toJson(json), StandardCharsets.UTF_8);
        });
    }

    /** Deletes the explored map of one dimension (marks and folios stay). */
    public void deleteDimension(ResourceLocation dim) {
        Path dir = worldDir.resolve(Dims.folder(dim));
        if (dimension != null && dim.equals(dimension.location())) {
            store.forget();
            if (depthStore != null) depthStore.forget();
            seen.clear();
            queue.clear();
        }
        DimensionStore v = viewing.remove(dim);
        if (v != null) v.forget();
        MapIO.run("delete map of " + dim, () -> MapFiles.deleteTree(dir));
    }

    /** Deletes this server's whole map: explored map, marks and folios. */
    public void deleteAll() {
        if (store != null) store.forget();
        if (depthStore != null) depthStore.forget();
        closeViewing();
        marks.clearAll();
        seen.clear();
        queue.clear();
        Path dir = worldDir;
        MapIO.run("delete map", () -> {
            MapFiles.deleteTree(dir);
            Files.createDirectories(dir);
        });
        touchWorldFile(false);
    }
}
