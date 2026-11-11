package com.tac5studios.codexcartography.server;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.tac5studios.codexcartography.CodexCartography;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * A hand-written TOML file in config/codex_cartography/. Written from the mod's default (with its comments)
 * when missing, read again on /cartography reload. Missing keys fall back to the default.
 */
public final class TomlFile {

    private final String name;
    private CommentedConfig data;
    private CommentedConfig defaults;

    public TomlFile(String name) {
        this.name = name;
    }

    public Path path() {
        return FMLPaths.CONFIGDIR.get().resolve(CodexCartography.MOD_ID).resolve(name);
    }

    /** Reads the file (writing the default first if it's missing). Throws if the file can't be read. */
    public void load() throws IOException {
        Path file = path();
        if (defaults == null) defaults = readDefault();
        if (!Files.exists(file)) {
            Files.createDirectories(file.getParent());
            try (InputStream in = TomlFile.class.getResourceAsStream("/defaults/" + name)) {
                if (in != null) Files.copy(in, file);
            }
        }
        try (CommentedFileConfig cfg = CommentedFileConfig.builder(file).sync().build()) {
            cfg.load();
            data = CommentedConfig.copy(cfg);
        } catch (RuntimeException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    private CommentedConfig readDefault() {
        try (InputStream in = TomlFile.class.getResourceAsStream("/defaults/" + name)) {
            if (in != null) {
                return new com.electronwill.nightconfig.toml.TomlParser().parse(new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            CodexCartography.LOGGER.warn("[Cartography] Default {} is broken: {}", name, e.toString());
        }
        return CommentedConfig.inMemory();
    }

    private Object raw(String path) {
        Object v = data != null ? data.get(path) : null;
        return v != null ? v : defaults != null ? defaults.get(path) : null;
    }

    public boolean bool(String path, boolean fallback) {
        return raw(path) instanceof Boolean b ? b : fallback;
    }

    public int integer(String path, int fallback) {
        return raw(path) instanceof Number n ? n.intValue() : fallback;
    }

    public String string(String path, String fallback) {
        return raw(path) instanceof String s ? s : fallback;
    }

    public List<String> strings(String path) {
        List<String> out = new ArrayList<>();
        if (raw(path) instanceof List<?> list) for (Object o : list) if (o != null) out.add(o.toString());
        return out;
    }
}
