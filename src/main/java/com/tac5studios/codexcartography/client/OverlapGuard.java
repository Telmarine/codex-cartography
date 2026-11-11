package com.tac5studios.codexcartography.client;

import com.tac5studios.codexcartography.config.Features;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;

import java.util.ArrayList;
import java.util.List;

/**
 * When another minimap or world map mod is installed, our minimap and the Codex key start off
 * (unless force_on keeps them on: the server's features.toml on a server with the mod, else the player's own),
 * and the player is told once.
 */
public final class OverlapGuard {

    private static final List<String> OTHER_MAPS = List.of(
            "xaerominimap", "xaerominimapfair", "xaeroworldmap", "journeymap", "ftbchunks", "voxelmap");

    private static List<String> found;

    private OverlapGuard() {}

    /** Display names of the other map mods installed. */
    public static List<String> found() {
        if (found == null) {
            List<String> names = new ArrayList<>();
            for (String id : OTHER_MAPS) {
                ModList.get().getModContainerById(id).ifPresent(c -> names.add(c.getModInfo().getDisplayName()));
            }
            found = List.copyOf(names);
        }
        return found;
    }

    public static boolean minimapOn() {
        return Features.MINIMAP.get() && (found().isEmpty() || forced("minimap"));
    }

    public static boolean keyOn() {
        return found().isEmpty() || forced("key");
    }

    /** On a server with the mod, the server's force_on decides; elsewhere the player's own features.toml. */
    private static boolean forced(String name) {
        return ClientRules.fromServer ? ClientRules.forceOn.contains(name) : Features.FORCE_ON.get().contains(name);
    }

    /** Tells the player once (ever) which map mod made ours step back. */
    static void warnOnce(Minecraft mc) {
        if (found().isEmpty() || (minimapOn() && keyOn()) || mc.player == null) return;
        String names = String.join(", ", found());
        if (!ClientState.markOnce("overlap:" + names)) return;
        mc.player.displayClientMessage(Component.translatable("codex_cartography.overlap", names), false);
    }
}
