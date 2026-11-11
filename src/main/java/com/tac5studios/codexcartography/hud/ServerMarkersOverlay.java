package com.tac5studios.codexcartography.hud;

import com.tac5studios.codexcartography.client.ServerMarkers;
import com.tac5studios.codexcartography.ui.Codex;
import com.tac5studios.codexcartography.ui.Tex;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Waystones, homes and warps on the minimap (hexagon badges, so they never look like the player's own marks). */
public final class ServerMarkersOverlay {

    private ServerMarkersOverlay() {}

    static void drawOnMinimap(GuiGraphics g, Projection proj, ResourceLocation dim, float size) {
        for (ServerMarkers.Marker m : ServerMarkers.in(dim)) {
            if (!proj.inside(m.x() + 0.5, m.z() + 0.5)) continue;
            float[] o = proj.offset(m.x() + 0.5, m.z() + 0.5);
            Codex.badge(g, Tex.SERVER_BADGE, Tex.serverEmblem(m.kind()), proj.cx + o[0], proj.cy + o[1], size, m.color());
        }
    }
}
