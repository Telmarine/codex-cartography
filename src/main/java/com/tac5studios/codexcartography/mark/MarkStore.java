package com.tac5studios.codexcartography.mark;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tac5studios.codexcartography.CodexCartography;
import com.tac5studios.codexcartography.client.ClientRules;
import com.tac5studios.codexcartography.client.MapIO;
import com.tac5studios.codexcartography.config.ClientConfig;
import com.tac5studios.codexcartography.config.Features;
import com.tac5studios.codexcartography.net.ClientNet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Marks and folios of one server or world, saved in codex_cartography/worlds/&lt;world&gt;/marks.json.
 * Also adds Death points and discovered portals and structures as they happen.
 */
public final class MarkStore {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final Path file;
    private final List<Mark> marks = new ArrayList<>();
    private final List<Folio> folios = new ArrayList<>();
    /** Claim marks this player hid for themselves (they can't edit them). */
    private final java.util.Set<String> hiddenClaimMarks = new java.util.HashSet<>();
    /** The folio claim marks show under in the Ledger (not saved; built from the claims data). */
    public static final String CLAIMS_FOLIO = "claims";
    private boolean dirty;
    private boolean wasDead;
    private BlockPos lastLook;
    private long lastLookTick;

    public MarkStore(Path worldDir) {
        this.file = worldDir.resolve("marks.json");
        load();
    }

    public List<Mark> all() { return marks; }
    public List<Folio> folios() { return folios; }

    public Folio folio(String id) {
        for (Folio f : folios) if (f.id.equals(id)) return f;
        if (id.equals(CLAIMS_FOLIO)) return claimsFolio();
        return null;
    }

    private static Folio claimsFolio() {
        Folio f = new Folio(CLAIMS_FOLIO, net.minecraft.network.chat.Component.translatable("codex_cartography.folios.claims").getString(), 0x46AAFF);
        f.shown = true;
        return f;
    }

    /** Claim marks of the claims the player can see (read-only marks in the "Claims" folio). */
    public List<Mark> claimMarks() {
        List<Mark> out = new ArrayList<>();
        if (!Features.CLAIM_MARKS.get() || !ClientRules.claimMarks) return out;
        for (com.tac5studios.codexcartography.claims.ClaimsData.Claim c : com.tac5studios.codexcartography.claims.ClaimsData.all()) {
            for (com.tac5studios.codexcartography.claims.ClaimsData.ClaimMark cm : c.marks()) {
                Mark m = new Mark("claim:" + cm.id(), cm.name(), cm.emblem().isEmpty() ? "flag" : cm.emblem(), cm.color(), c.dim(), cm.x(), cm.y(), cm.z());
                m.kind = Mark.Kind.CLAIM;
                m.folio = CLAIMS_FOLIO;
                m.claimId = c.id();
                m.hidden = hiddenClaimMarks.contains(m.id);
                out.add(m);
            }
        }
        return out;
    }

    /** Hides or shows a claim mark for this player only. */
    public void setClaimMarkHidden(Mark m, boolean hidden) {
        if (hidden) hiddenClaimMarks.add(m.id);
        else hiddenClaimMarks.remove(m.id);
        dirty = true;
    }

    /** Own marks plus claim marks, for the Ledger. */
    public List<Mark> ledger() {
        List<Mark> out = new ArrayList<>(marks);
        out.addAll(claimMarks());
        return out;
    }

    private boolean folioShown(Mark m) {
        if (m.folio.isEmpty()) return true;
        Folio f = folio(m.folio);
        return f == null || f.shown;
    }

    /** Marks drawn on the minimap in a dimension. */
    public List<Mark> visibleIn(ResourceLocation dim) {
        List<Mark> out = new ArrayList<>();
        for (Mark m : ledger()) if (m.dim.equals(dim) && !m.hidden && m.onMinimap && folioShown(m) && allowed(m)) out.add(m);
        return out;
    }

    /** Marks drawn on the world map in a dimension. */
    public List<Mark> mapIn(ResourceLocation dim) {
        List<Mark> out = new ArrayList<>();
        for (Mark m : ledger()) if (m.dim.equals(dim) && !m.hidden && folioShown(m) && allowed(m)) out.add(m);
        return out;
    }

    /** Marks drawn in the world (floating emblems). */
    public List<Mark> inWorld(ResourceLocation dim) {
        List<Mark> out = new ArrayList<>();
        for (Mark m : ledger()) if (m.dim.equals(dim) && !m.hidden && m.inWorld && folioShown(m) && allowed(m)) out.add(m);
        return out;
    }

    private static boolean allowed(Mark m) {
        if (m.kind == Mark.Kind.DEATH) return Features.DEATH_POINTS.get() && ClientRules.deathPoints;
        return true;
    }

    public static String newId() {
        return UUID.randomUUID().toString();
    }

    public void add(Mark m) {
        marks.add(m);
        dirty = true;
    }

    public void remove(Mark m) {
        marks.remove(m);
        dirty = true;
    }

    public void changed() {
        dirty = true;
    }

    public Folio addFolio(String name, int color) {
        Folio f = new Folio(newId(), name, color);
        folios.add(f);
        dirty = true;
        return f;
    }

    /** Removes a folio; its marks stay, with no folio. */
    public void removeFolio(Folio f) {
        folios.remove(f);
        for (Mark m : marks) if (m.folio.equals(f.id)) m.folio = "";
        dirty = true;
    }

    public int countIn(Folio f) {
        int n = 0;
        for (Mark m : marks) if (m.folio.equals(f.id)) n++;
        return n;
    }

    public void clearAll() {
        marks.clear();
        folios.clear();
        dirty = true;
        save();
    }

    /** Death points, discovered portals and structures. */
    public void tick(Minecraft mc, ClientLevel level, LocalPlayer player) {
        boolean dead = player.isDeadOrDying();
        if (dead && !wasDead && Features.DEATH_POINTS.get() && ClientRules.deathPoints) addDeathPoint(level, player);
        wasDead = dead;
        if (!dead && ClientConfig.DEATH_POINT_CLEAR.get() && level.getGameTime() % 10 == 0) clearReachedDeathPoints(level, player);
        if (level.getGameTime() - lastLookTick >= 5) {
            lastLookTick = level.getGameTime();
            look(mc, level);
        }
    }

    /** A Death point goes away once the player is back on its spot (within 2 blocks, 3 up or down). */
    private void clearReachedDeathPoints(ClientLevel level, LocalPlayer player) {
        ResourceLocation dim = level.dimension().location();
        List<Mark> reached = new ArrayList<>();
        for (Mark d : marks) {
            if (d.kind != Mark.Kind.DEATH || !d.dim.equals(dim)) continue;
            double dx = player.getX() - (d.x + 0.5);
            double dz = player.getZ() - (d.z + 0.5);
            if (dx * dx + dz * dz <= 4 && Math.abs(player.getY() - d.y) <= 3) reached.add(d);
        }
        for (Mark d : reached) remove(d);
    }

    private void addDeathPoint(ClientLevel level, LocalPlayer player) {
        ResourceLocation dim = level.dimension().location();
        Mark m = new Mark(newId(), Component.translatable("codex_cartography.mark.death_point").getString(), "death", 0,
                dim, player.getBlockX(), player.getBlockY(), player.getBlockZ());
        m.kind = Mark.Kind.DEATH;
        add(m);
        List<Mark> deaths = new ArrayList<>();
        for (Mark d : marks) if (d.kind == Mark.Kind.DEATH && d.dim.equals(dim)) deaths.add(d);
        deaths.sort(Comparator.comparingLong(d -> d.created));
        int keep = ClientConfig.DEATH_POINTS_KEPT.get();
        for (int i = 0; i < deaths.size() - keep; i++) marks.remove(deaths.get(i));
    }

    /** Discovered portals and structures: only once the crosshair hits a block of them. */
    private void look(Minecraft mc, ClientLevel level) {
        if (!(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;
        BlockPos pos = hit.getBlockPos();
        if (pos.equals(lastLook)) return;
        lastLook = pos;
        BlockState state = level.getBlockState(pos);
        ResourceLocation dim = level.dimension().location();
        if (Features.MARKS.get() && state.is(BlockTags.PORTALS)) {
            String key = state.is(Blocks.NETHER_PORTAL) ? "nether_portal" : state.is(Blocks.END_GATEWAY) ? "end_gateway" : "end_portal";
            discover(dim, pos, "portal", Component.translatable("codex_cartography.mark." + key).getString(), 6);
            return;
        }
        if (!ClientRules.discoveredStructures || !Features.MARKS.get()) return;
        if (state.isAir()) return;
        if (ClientNet.serverHasMod()) {
            // the server knows every structure; it answers only when this block is part of one
            ClientNet.sendLookAt(pos);
            return;
        }
        boolean cityBlock = state.is(Blocks.REINFORCED_DEEPSLATE) || state.is(Blocks.DEEPSLATE_BRICKS) || state.is(Blocks.DEEPSLATE_TILES)
                || state.is(Blocks.SCULK_SHRIEKER) || state.is(Blocks.CRACKED_DEEPSLATE_BRICKS) || state.is(Blocks.POLISHED_DEEPSLATE);
        boolean monumentBlock = state.is(Blocks.PRISMARINE) || state.is(Blocks.PRISMARINE_BRICKS) || state.is(Blocks.DARK_PRISMARINE)
                || state.is(Blocks.SEA_LANTERN);
        if (!cityBlock && !monumentBlock) return;
        if (state.is(Blocks.REINFORCED_DEEPSLATE)) discovered("minecraft:ancient_city", "ancient_city", "", dim, pos.getX(), pos.getY(), pos.getZ());
    }

    /**
     * A structure the server confirmed (or the client fallback found). The name comes from our lang file when it
     * has one for that structure (all villages are "Village"), else the server's readable name.
     */
    public void discovered(String structure, String emblem, String fallbackName, ResourceLocation dim, int x, int y, int z) {
        ResourceLocation id = ResourceLocation.tryParse(structure);
        String name = fallbackName;
        if (id != null) {
            String exact = "codex_cartography.structure." + id.getNamespace() + "." + id.getPath();
            String group = "codex_cartography.structure." + id.getNamespace() + "." + group(id.getPath());
            if (net.minecraft.locale.Language.getInstance().has(exact)) name = Component.translatable(exact).getString();
            else if (net.minecraft.locale.Language.getInstance().has(group)) name = Component.translatable(group).getString();
        }
        if (name.isEmpty()) name = Component.translatable(Emblems.langKey(emblem)).getString();
        discover(dim, new BlockPos(x, y, z), emblem, name, 96);
    }

    /** village_plains -> village, ruined_portal_mountain -> ruined_portal, shipwreck_beached -> shipwreck. */
    private static String group(String path) {
        for (String g : new String[] {"village", "ruined_portal", "shipwreck", "ocean_ruin", "mineshaft"}) {
            if (path.startsWith(g)) return g;
        }
        return path;
    }

    private void discover(ResourceLocation dim, BlockPos pos, String emblem, String name, int radius) {
        for (Mark m : marks) {
            if (m.kind == Mark.Kind.DISCOVERED && m.emblem.equals(emblem) && m.name.equals(name) && m.dim.equals(dim)
                    && Math.abs(m.x - pos.getX()) <= radius && Math.abs(m.z - pos.getZ()) <= radius) return;
        }
        Mark m = new Mark(newId(), name, emblem, Emblems.defaultColor(emblem), dim, pos.getX(), pos.getY(), pos.getZ());
        m.kind = Mark.Kind.DISCOVERED;
        add(m);
        LocalPlayer p = Minecraft.getInstance().player;
        if (p != null) p.displayClientMessage(Component.translatable("codex_cartography.mark.discovered", name), true);
    }

    private void load() {
        if (!Files.exists(file)) return;
        try {
            JsonObject json = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            if (json.get("folios") instanceof JsonArray arr) {
                for (JsonElement e : arr) {
                    JsonObject o = e.getAsJsonObject();
                    Folio f = new Folio(o.get("id").getAsString(), o.get("name").getAsString(), o.get("color").getAsInt());
                    f.shown = !o.has("shown") || o.get("shown").getAsBoolean();
                    f.colorAll = o.has("color_all") && o.get("color_all").getAsBoolean();
                    folios.add(f);
                }
            }
            if (json.get("hidden_claim_marks") instanceof JsonArray hc) for (JsonElement e : hc) hiddenClaimMarks.add(e.getAsString());
            if (json.get("marks") instanceof JsonArray arr) {
                for (JsonElement e : arr) {
                    JsonObject o = e.getAsJsonObject();
                    ResourceLocation dim = ResourceLocation.tryParse(o.get("dim").getAsString());
                    if (dim == null) continue;
                    Mark m = new Mark(o.get("id").getAsString(), o.get("name").getAsString(), o.get("emblem").getAsString(),
                            o.get("color").getAsInt(), dim, o.get("x").getAsInt(), o.get("y").getAsInt(), o.get("z").getAsInt());
                    if (o.has("folio")) m.folio = o.get("folio").getAsString();
                    if (o.has("hidden")) m.hidden = o.get("hidden").getAsBoolean();
                    if (o.has("in_world")) m.inWorld = o.get("in_world").getAsBoolean();
                    if (o.has("on_minimap")) m.onMinimap = o.get("on_minimap").getAsBoolean();
                    if (o.has("show_distance")) m.showDistance = o.get("show_distance").getAsBoolean();
                    if (o.has("kind")) m.kind = Mark.Kind.valueOf(o.get("kind").getAsString());
                    if (o.has("created")) m.created = o.get("created").getAsLong();
                    marks.add(m);
                }
            }
        } catch (Exception e) {
            CodexCartography.LOGGER.warn("[Cartography] Couldn't read {}: {}", file, e.toString());
        }
    }

    /** Saves in the background if anything changed. */
    public void save() {
        if (!dirty) return;
        dirty = false;
        JsonObject json = new JsonObject();
        JsonArray fa = new JsonArray();
        for (Folio f : folios) {
            JsonObject o = new JsonObject();
            o.addProperty("id", f.id);
            o.addProperty("name", f.name);
            o.addProperty("color", f.color);
            o.addProperty("shown", f.shown);
            o.addProperty("color_all", f.colorAll);
            fa.add(o);
        }
        json.add("folios", fa);
        JsonArray ma = new JsonArray();
        for (Mark m : marks) {
            JsonObject o = new JsonObject();
            o.addProperty("id", m.id);
            o.addProperty("name", m.name);
            o.addProperty("emblem", m.emblem);
            o.addProperty("color", m.color);
            o.addProperty("dim", m.dim.toString());
            o.addProperty("x", m.x);
            o.addProperty("y", m.y);
            o.addProperty("z", m.z);
            o.addProperty("folio", m.folio);
            o.addProperty("hidden", m.hidden);
            o.addProperty("in_world", m.inWorld);
            o.addProperty("on_minimap", m.onMinimap);
            o.addProperty("show_distance", m.showDistance);
            o.addProperty("kind", m.kind.name());
            o.addProperty("created", m.created);
            ma.add(o);
        }
        json.add("marks", ma);
        JsonArray hc = new JsonArray();
        for (String id : hiddenClaimMarks) hc.add(id);
        json.add("hidden_claim_marks", hc);
        String text = GSON.toJson(json);
        MapIO.run("save marks", () -> {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling("marks.json.tmp");
            Files.writeString(tmp, text, StandardCharsets.UTF_8);
            Files.move(tmp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        });
    }
}
