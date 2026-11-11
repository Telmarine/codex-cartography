package com.tac5studios.codexcartography.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.tac5studios.codexcartography.config.ClientConfig;
import com.tac5studios.codexcartography.config.Features;
import com.tac5studios.codexcartography.screen.CodexScreen;
import com.tac5studios.codexcartography.screen.RadialScreen;
import com.tac5studios.codexcartography.screen.WorldMapScreen;
import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * The one Codex key. Tap: world map. Hold: the in-game quick menu (released on a slice to pick it).
 * Every quick-menu action can also get its own key in the Controls menu (unbound by default).
 */
public final class OneKey {

    private static final String CATEGORY = "key.categories.codex_cartography";

    public static final KeyMapping KEY = new KeyMapping("key.codex_cartography.codex", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, CATEGORY);

    /** Hold to show mob heads on the minimap (when Mob icons is set to HOLD). */
    public static final KeyMapping MOB_HEADS = new KeyMapping("key.codex_cartography.mob_heads", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_TAB, CATEGORY);

    public static final KeyMapping WORLD_MAP = unbound("world_map");
    public static final KeyMapping NEW_MARK = unbound("new_mark");
    public static final KeyMapping MINIMAP = unbound("minimap");
    public static final KeyMapping ZOOM_IN = unbound("zoom_in");
    public static final KeyMapping ZOOM_OUT = unbound("zoom_out");
    public static final KeyMapping LENS = unbound("lens");
    public static final KeyMapping DEPTH = unbound("depth_view");
    public static final KeyMapping LEDGER = unbound("ledger");
    public static final KeyMapping SETTINGS = unbound("settings");
    public static final List<KeyMapping> ACTIONS = List.of(WORLD_MAP, NEW_MARK, MINIMAP, ZOOM_IN, ZOOM_OUT, LENS, DEPTH, LEDGER, SETTINGS);

    private static boolean wasDown;
    private static boolean held;
    private static long pressedAt;
    /** Set when a Codex page closes on the key press, so letting go of the key doesn't open the map again. */
    private static boolean swallow;

    private OneKey() {}

    public static void swallowUntilReleased() {
        swallow = true;
    }

    private static KeyMapping unbound(String name) {
        return new KeyMapping("key.codex_cartography." + name, KeyConflictContext.IN_GAME, InputConstants.UNKNOWN, CATEGORY);
    }

    static void tick(Minecraft mc) {
        if (mc.player == null || MapSession.get() == null) {
            wasDown = false;
            return;
        }
        while (WORLD_MAP.consumeClick()) Actions.worldMap();
        while (NEW_MARK.consumeClick()) Actions.newMarkHere();
        while (MINIMAP.consumeClick()) Actions.toggleMinimap();
        while (ZOOM_IN.consumeClick()) Actions.zoom(true);
        while (ZOOM_OUT.consumeClick()) Actions.zoom(false);
        while (LENS.consumeClick()) Actions.nextLens();
        while (DEPTH.consumeClick()) Actions.toggleDepth();
        while (LEDGER.consumeClick()) CodexScreen.open(CodexScreen.Section.LEDGER);
        while (SETTINGS.consumeClick()) CodexScreen.open(CodexScreen.Section.SETTINGS);

        //noinspection StatementWithEmptyBody
        while (KEY.consumeClick()) {
            // presses are read from isDown below; drop the queued clicks
        }
        if (!OverlapGuard.keyOn() || mc.screen != null) {
            wasDown = false;
            return;
        }
        boolean down = KEY.isDown();
        if (swallow) {
            if (!down) swallow = false;
            wasDown = false;
            return;
        }
        long now = Util.getMillis();
        if (down && !wasDown) {
            pressedAt = now;
            held = false;
        } else if (down && !held && now - pressedAt >= ClientConfig.HOLD_MS.get()) {
            held = true;
            mc.setScreen(new RadialScreen());
        } else if (!down && wasDown && !held) {
            if (Features.WORLD_MAP.get()) mc.setScreen(new WorldMapScreen(null));
            else if (mc.player != null) mc.player.displayClientMessage(MapSession.status(), true);
        }
        wasDown = down;
    }
}
