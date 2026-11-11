package com.tac5studios.codexcartography.ui;

import com.tac5studios.codexcartography.CodexCartography;
import net.minecraft.resources.ResourceLocation;

/** Every texture the mod draws, under assets/codex_cartography/textures/gui/. */
public final class Tex {

    private Tex() {}

    private static ResourceLocation gui(String path) {
        return CodexCartography.id("textures/gui/" + path + ".png");
    }

    public static final ResourceLocation BADGE = gui("mark/badge");
    public static final ResourceLocation DEATH = gui("mark/death");
    public static final ResourceLocation DANGER = gui("mark/danger");
    public static final ResourceLocation SERVER_BADGE = gui("server/badge");
    public static final ResourceLocation PLAYER = gui("player/player_32");
    public static final ResourceLocation OTHER = gui("player/other_32");
    public static final ResourceLocation FOG = gui("fx_fog");
    public static final ResourceLocation GLOW = gui("fx_glow");
    public static final ResourceLocation DRIP = gui("fx_drip");

    public static ResourceLocation emblem(String name) { return gui("mark/emblem_" + name); }
    public static ResourceLocation serverEmblem(String name) { return gui("server/emblem_" + name); }
    public static ResourceLocation layer(String name, boolean on) { return gui("layer/" + name + (on ? "_on_24" : "_off_24")); }
    public static ResourceLocation action(String name) { return gui("action/" + name + "_24"); }
    public static ResourceLocation dim(String kind) { return gui("dim/" + kind + "_32"); }
    public static ResourceLocation frame(String shape) { return gui("frame/" + shape + "_256"); }
    public static ResourceLocation sweep(String kind) { return gui("sweep/" + kind + "_16"); }
}
