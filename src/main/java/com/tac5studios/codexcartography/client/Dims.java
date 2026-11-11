package com.tac5studios.codexcartography.client;

import com.tac5studios.codexcartography.config.ClientConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.Locale;

/** Readable dimension names (the server's names first) and which tab glyph and color a dimension gets. */
public final class Dims {

    private Dims() {}

    public static Component name(ResourceLocation dim) {
        String custom = ClientRules.dimensionNames.get(dim);
        if (custom != null) return Component.literal(custom);
        if (dim.equals(Level.OVERWORLD.location())) return Component.translatable("codex_cartography.dim.overworld");
        if (dim.equals(Level.NETHER.location())) return Component.translatable("codex_cartography.dim.nether");
        if (dim.equals(Level.END.location())) return Component.translatable("codex_cartography.dim.end");
        String path = dim.getPath();
        int slash = path.lastIndexOf('/');
        if (slash >= 0) path = path.substring(slash + 1);
        StringBuilder sb = new StringBuilder();
        for (String word : path.split("[_\\-]")) {
            if (word.isEmpty()) continue;
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(word.substring(0, 1).toUpperCase(Locale.ROOT)).append(word.substring(1));
        }
        return Component.literal(sb.toString());
    }

    public static Component name(ResourceKey<Level> dim) {
        return name(dim.location());
    }

    /** overworld, nether, end or modded (for the tab glyph). */
    public static String kind(ResourceLocation dim) {
        if (dim.equals(Level.OVERWORLD.location())) return "overworld";
        if (dim.equals(Level.NETHER.location())) return "nether";
        if (dim.equals(Level.END.location())) return "end";
        String p = dim.getPath();
        if (p.contains("nether")) return "nether";
        if (p.contains("end")) return "end";
        if (p.contains("overworld")) return "overworld";
        return "modded";
    }

    public static int color(ResourceLocation dim) {
        return switch (kind(dim)) {
            case "overworld" -> ClientConfig.ACCENT.get();
            case "nether" -> 0xFF5A46;
            case "end" -> 0xBE78FF;
            default -> 0xFFBE46;
        };
    }

    /** Folder name of a dimension under the world folder. */
    public static String folder(ResourceLocation dim) {
        return WorldKey.clean(dim.getNamespace() + "_" + dim.getPath());
    }
}
