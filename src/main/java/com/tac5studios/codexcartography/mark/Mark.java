package com.tac5studios.codexcartography.mark;

import com.tac5studios.codexcartography.client.MapSession;
import com.tac5studios.codexcartography.ui.Codex;
import com.tac5studios.codexcartography.ui.Tex;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** A mark (waypoint). Death points, the danger mark and discovered places are marks too, with a fixed look. */
public final class Mark {

    /** SERVER: a home or warp from Elements: Nexus, shown in the Ledger only (never saved). */
    public enum Kind { MARK, DEATH, DISCOVERED, CLAIM, SERVER }

    public String id;
    public String name;
    public String emblem;
    public int color;
    public ResourceLocation dim;
    public int x;
    public int y;
    public int z;
    public String folio = "";
    public boolean hidden;
    public boolean inWorld = true;
    public boolean onMinimap = true;
    public boolean showDistance = true;
    public Kind kind = Kind.MARK;
    public long created = System.currentTimeMillis();
    /** For claim marks: the claim it belongs to (map id from Elements: Claims). */
    public int claimId;

    public Mark(String id, String name, String emblem, int color, ResourceLocation dim, int x, int y, int z) {
        this.id = id;
        this.name = name;
        this.emblem = emblem;
        this.color = color;
        this.dim = dim;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public int x() { return x; }
    public int z() { return z; }

    public boolean fixedLook() {
        return kind == Kind.DEATH || emblem.equals("danger");
    }

    /** The color it's drawn in (its own, or its folio's when the folio colors every mark). */
    public int drawColor() {
        if (fixedLook()) return 0xFFFFFF;
        MapSession s = MapSession.get();
        if (s != null && !folio.isEmpty()) {
            Folio f = s.marks().folio(folio);
            if (f != null && f.colorAll) return f.color;
        }
        return color;
    }

    /** Draws the badge centered at a screen position. */
    public void drawBadge(GuiGraphics g, float cx, float cy, float size) {
        if (kind == Kind.SERVER) {
            Codex.badge(g, Tex.SERVER_BADGE, Tex.serverEmblem(emblem), cx, cy, size, color);
        } else if (kind == Kind.DEATH) {
            Codex.texAt(g, Tex.DEATH, cx, cy, size, 0, 64, 0xFFFFFFFF);
        } else if (emblem.equals("danger")) {
            Codex.texAt(g, Tex.DANGER, cx, cy, size, 0, 64, 0xFFFFFFFF);
        } else {
            Codex.badge(g, Tex.BADGE, Tex.emblem(emblem), cx, cy, size, drawColor());
        }
    }
}
