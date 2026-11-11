package com.tac5studios.codexcartography.client;

import com.tac5studios.codexcartography.config.ClientConfig;
import com.tac5studios.codexcartography.config.Features;
import com.tac5studios.codexcartography.screen.MarkEditor;
import com.tac5studios.codexcartography.screen.WorldMapScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/** The quick-menu actions (also on their own optional keys). */
public final class Actions {

    private Actions() {}

    public static void worldMap() {
        if (Features.WORLD_MAP.get()) Minecraft.getInstance().setScreen(new WorldMapScreen(null));
    }

    /** Opens the mark editor for a new mark where the player stands. */
    public static void newMarkHere() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        MapSession s = MapSession.get();
        if (p == null || s == null || mc.level == null || !Features.MARKS.get()) return;
        WorldMapScreen map = new WorldMapScreen(mc.level.dimension().location());
        mc.setScreen(map);
        map.openDrawer(MarkEditor.create(map, mc.level.dimension().location(), p.getBlockX(), p.getBlockY(), p.getBlockZ()));
    }

    public static void toggleMinimap() {
        ClientConfig.set(ClientConfig.MINIMAP_ON, !ClientConfig.MINIMAP_ON.get());
    }

    public static void zoom(boolean in) {
        double z = ClientConfig.MINIMAP_ZOOM.get();
        ClientConfig.set(ClientConfig.MINIMAP_ZOOM, Math.max(0.25, Math.min(8.0, in ? z * 2 : z / 2)));
    }

    public static void nextLens() {
        if (!Features.LENSES.get()) return;
        ClientConfig.Lens[] all = ClientConfig.Lens.values();
        ClientConfig.Lens next = all[(ClientConfig.LENS.get().ordinal() + 1) % all.length];
        ClientConfig.set(ClientConfig.LENS, next);
        LocalPlayer p = Minecraft.getInstance().player;
        if (p != null) {
            p.displayClientMessage(Component.translatable("codex_cartography.lens." + next.name().toLowerCase(Locale.ROOT)), true);
        }
    }

    public static void toggleDepth() {
        MapSession s = MapSession.get();
        if (s != null && Features.DEPTH_VIEW.get() && ClientRules.depthView) s.toggleDepth();
    }
}
