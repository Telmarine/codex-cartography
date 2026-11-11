package com.tac5studios.codexcartography.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tac5studios.codexcartography.CodexCartography;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/** Small things the mod remembers between games (one-time messages already shown), in codex_cartography/state.json. */
public final class ClientState {

    private static Set<String> shown;

    private ClientState() {}

    private static Path file() {
        return MapSession.root().resolve("state.json");
    }

    /** Returns true the first time a key is seen (and remembers it); false after that. */
    public static boolean markOnce(String key) {
        load();
        if (!shown.add(key)) return false;
        JsonArray arr = new JsonArray();
        shown.forEach(arr::add);
        JsonObject json = new JsonObject();
        json.add("shown", arr);
        Path file = file();
        MapIO.run("save " + file, () -> {
            Files.createDirectories(file.getParent());
            Files.writeString(file, json.toString(), StandardCharsets.UTF_8);
        });
        return true;
    }

    private static void load() {
        if (shown != null) return;
        shown = new HashSet<>();
        Path file = file();
        if (!Files.exists(file)) return;
        try {
            JsonObject json = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            if (json.get("shown") instanceof JsonArray arr) for (JsonElement e : arr) shown.add(e.getAsString());
        } catch (Exception e) {
            CodexCartography.LOGGER.warn("[Cartography] Couldn't read {}: {}", file, e.toString());
        }
    }
}
