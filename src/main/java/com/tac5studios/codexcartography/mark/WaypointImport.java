package com.tac5studios.codexcartography.mark;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tac5studios.codexcartography.CodexCartography;
import com.tac5studios.codexcartography.client.MapSession;
import com.tac5studios.codexcartography.client.WorldKey;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * One-time import of waypoints from other map mods' own files on this computer. They come in as marks
 * in a folio named after the source. Nothing else from those mods is read.
 */
public final class WaypointImport {

    private WaypointImport() {}

    /** A folder of waypoints another map mod saved: which mod, the folder, and whether it's for the world you're in. */
    public record Source(String mod, String folder, Path path, boolean here) {}

    /**
     * Every waypoint folder on this computer, the ones for the current server or world first.
     * Xaero's Minimap keeps them in xaero/minimap/ (older versions: XaeroWaypoints/), JourneyMap in journeymap/data/.
     */
    public static List<Source> sources() {
        List<Source> out = new ArrayList<>();
        MapSession s = MapSession.get();
        if (s == null) return out;
        Path game = Minecraft.getInstance().gameDirectory.toPath();
        for (Path root : List.of(game.resolve("xaero").resolve("minimap"), game.resolve("XaeroWaypoints"))) {
            for (Path d : folders(root)) {
                if (hasFiles(d, ".txt", 2)) out.add(new Source("Xaero's Minimap", d.getFileName().toString(), d, matches(d, s.world())));
            }
        }
        for (String side : List.of("mp", "sp")) {
            for (Path d : folders(game.resolve("journeymap").resolve("data").resolve(side))) {
                if (hasFiles(d.resolve("waypoints"), ".json", 1)) out.add(new Source("JourneyMap", d.getFileName().toString(), d, matches(d, s.world())));
            }
        }
        out.sort((a, b) -> Boolean.compare(b.here(), a.here()));
        return out;
    }

    /** Imports one folder into the current world's marks. Returns how many marks were added. */
    public static int run(Source source) {
        MapSession s = MapSession.get();
        if (s == null) return 0;
        int added = source.mod().equals("JourneyMap") ? journeyMap(s, source) : xaero(s, source);
        if (added > 0) s.marks().save();
        return added;
    }

    private static List<Path> folders(Path root) {
        if (!Files.isDirectory(root)) return List.of();
        try (Stream<Path> dirs = Files.list(root)) {
            return dirs.filter(Files::isDirectory).toList();
        } catch (IOException e) {
            CodexCartography.LOGGER.warn("[Cartography] Couldn't list {}: {}", root, e.toString());
            return List.of();
        }
    }

    private static boolean hasFiles(Path dir, String ending, int depth) {
        if (!Files.isDirectory(dir)) return false;
        try (Stream<Path> files = Files.walk(dir, depth)) {
            return files.anyMatch(p -> p.toString().endsWith(ending));
        } catch (IOException e) {
            return false;
        }
    }

    /** Xaero's Minimap: &lt;world&gt;/&lt;dim%..&gt;/*.txt, lines "waypoint:name:initials:x:y:z:color:...". */
    private static int xaero(MapSession s, Source source) {
        int added = 0;
        Folio folio = null;
        try (Stream<Path> files = Files.walk(source.path(), 2)) {
            for (Path f : files.filter(p -> p.toString().endsWith(".txt")).toList()) {
                ResourceLocation dim = xaeroDim(f.getParent().getFileName().toString());
                if (dim == null) continue;
                for (String line : Files.readAllLines(f, StandardCharsets.UTF_8)) {
                    if (!line.startsWith("waypoint:")) continue;
                    String[] p = line.split(":");
                    if (p.length < 7) continue;
                    try {
                        int x = Integer.parseInt(p[3]);
                        int y = p[4].equals("~") ? 64 : Integer.parseInt(p[4]);
                        int z = Integer.parseInt(p[5]);
                        if (folio == null) folio = s.marks().addFolio("Xaero's Minimap", 0xA0A6AC);
                        Mark m = new Mark(MarkStore.newId(), p[1].replace("§§", ":"), "flag", Emblems.GREEN, dim, x, y, z);
                        m.folio = folio.id;
                        s.marks().add(m);
                        added++;
                    } catch (NumberFormatException ignored) {
                        // a line we don't understand; skip it
                    }
                }
            }
        } catch (IOException e) {
            CodexCartography.LOGGER.warn("[Cartography] Couldn't read Xaero's waypoints in {}: {}", source.path(), e.toString());
        }
        return added;
    }

    private static ResourceLocation xaeroDim(String folder) {
        if (!folder.startsWith("dim%")) return null;
        String d = folder.substring(4);
        return switch (d) {
            case "0" -> ResourceLocation.withDefaultNamespace("overworld");
            case "-1" -> ResourceLocation.withDefaultNamespace("the_nether");
            case "1" -> ResourceLocation.withDefaultNamespace("the_end");
            default -> ResourceLocation.tryParse(d.replace('$', ':'));
        };
    }

    /** JourneyMap: &lt;world&gt;/waypoints/*.json, one waypoint per file. */
    private static int journeyMap(MapSession s, Source source) {
        int added = 0;
        Folio folio = null;
        Path wp = source.path().resolve("waypoints");
        try (Stream<Path> files = Files.list(wp)) {
            for (Path f : files.filter(p -> p.toString().endsWith(".json")).toList()) {
                try {
                    JsonElement el = JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8));
                    if (!el.isJsonObject()) continue;
                    JsonObject o = el.getAsJsonObject();
                    if (!o.has("name") || !o.has("x") || !o.has("z")) continue;
                    String dimId = "minecraft:overworld";
                    if (o.has("dimensions") && o.get("dimensions").isJsonArray() && !o.getAsJsonArray("dimensions").isEmpty()) {
                        dimId = o.getAsJsonArray("dimensions").get(0).getAsString();
                    } else if (o.has("dimension")) {
                        dimId = o.get("dimension").getAsString();
                    }
                    ResourceLocation dim = ResourceLocation.tryParse(dimId);
                    if (dim == null) continue;
                    if (folio == null) folio = s.marks().addFolio("JourneyMap", 0xA0A6AC);
                    Mark m = new Mark(MarkStore.newId(), o.get("name").getAsString(), "flag", Emblems.GREEN, dim,
                            o.get("x").getAsInt(), o.has("y") ? o.get("y").getAsInt() : 64, o.get("z").getAsInt());
                    m.folio = folio.id;
                    s.marks().add(m);
                    added++;
                } catch (RuntimeException ignored) {
                    // not a waypoint file we understand
                }
            }
        } catch (IOException e) {
            CodexCartography.LOGGER.warn("[Cartography] Couldn't read JourneyMap waypoints in {}: {}", wp, e.toString());
        }
        return added;
    }

    /** Whether a folder's name matches this server's address or world name. */
    private static boolean matches(Path d, WorldKey key) {
        String want = WorldKey.clean(key.address().isEmpty() ? key.name() : key.address()).replace('.', '_').replace(':', '_');
        String n = WorldKey.clean(d.getFileName().toString()).replace('.', '_').toLowerCase(Locale.ROOT)
                .replace("multiplayer_", "").replace("singleplayer_", "").replace("~", "_");
        return n.contains(want) || want.contains(n);
    }
}
