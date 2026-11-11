package com.tac5studios.codexcartography.claims;

import com.tac5studios.codexcartography.net.ClientNet;
import com.tac5studios.codexcartography.screen.Drawer;
import com.tac5studios.codexcartography.screen.MarkEditor;
import com.tac5studios.codexcartography.screen.WorldMapScreen;
import com.tac5studios.codexcartography.ui.Codex;
import com.tac5studios.codexcartography.ui.Tex;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ChunkPos;

import java.util.Locale;

/**
 * The claims panel: opens when a claim is clicked on the world map. Your land (chunks and properties against your
 * rank's limits), this claim, its sub-areas and claim marks, and buttons for Land Config and the active property.
 * Players who can't manage the claim get a read-only version.
 */
public final class ClaimPanel implements Drawer {

    private static final int W = 196;

    private final WorldMapScreen screen;
    private final int claimId;
    private int x0;
    private int marksY;
    private int buttonsY;

    public ClaimPanel(WorldMapScreen screen, ClaimsData.Claim claim) {
        this.screen = screen;
        this.claimId = claim.id();
    }

    private ClaimsData.Claim claim() {
        for (ClaimsData.Claim c : ClaimsData.all()) if (c.id() == claimId) return c;
        return null;
    }

    @Override
    public int width() {
        return W;
    }

    @Override
    public boolean open() {
        return claim() != null;
    }

    private boolean canManage(ClaimsData.Claim c) {
        return c.standing() == ClaimsData.OWN || c.canManage();
    }

    @Override
    public void render(GuiGraphics g, int x0, int height, int mouseX, int mouseY, float partial) {
        this.x0 = x0;
        ClaimsData.Claim c = claim();
        if (c == null) return;
        Font font = Codex.font();
        g.pose().pushPose();
        g.pose().translate(0, 0, 200);
        g.fill(x0, 0, x0 + W, height, 0xFF0C1014);
        g.fill(x0, 0, x0 + 1, height, Codex.accent());
        int px = x0 + 10;
        int pr = x0 + W - 8;
        int color = 0xFF000000 | c.drawColor();
        Codex.tinted(g, Tex.layer("claims", true), px, 8, 20, 20, 24, 24, color);
        g.drawString(font, c.name(), px + 26, 8, Codex.TEXT, false);
        Component standing = switch (c.standing()) {
            case ClaimsData.OWN -> Component.translatable(c.active() ? "codex_cartography.claims.yours_active" : "codex_cartography.claims.yours");
            case ClaimsData.TRUSTED -> Component.translatable("codex_cartography.claims.trusted", c.rank());
            case ClaimsData.ADMIN -> Component.translatable("codex_cartography.claims.server");
            default -> Component.translatable("codex_cartography.claims.owner", c.owner());
        };
        g.drawString(font, standing, px + 26, 18, color, false);
        g.fill(px, 34, pr, 35, Codex.LINE);

        int y = 40;
        label(g, px, y, "codex_cartography.claims.your_land");
        int cw = (pr - px - 6) / 2;
        meter(g, px, y + 10, cw, "codex_cartography.claims.chunks", ClaimsData.chunksUsed, ClaimsData.chunksAllowed);
        meter(g, px + cw + 6, y + 10, cw, "codex_cartography.claims.properties", ClaimsData.propertiesUsed, ClaimsData.propertiesMax);

        y += 42;
        label(g, px, y, "codex_cartography.claims.this_claim");
        ChunkPos center = center(c);
        info(g, px, y + 11, cw, "codex_cartography.claims.chunks", String.valueOf(c.chunks().length));
        info(g, px + cw + 6, y + 11, cw, "codex_cartography.claims.center", center.getMiddleBlockX() + ", " + center.getMiddleBlockZ());
        info(g, px, y + 23, cw, "codex_cartography.claims.sub_areas", String.valueOf(c.subAreas().size()));
        int rows = 3;
        // the owner gets a full row: player names can be long
        if (!c.owner().isEmpty()) info(g, px, y + 35, pr - px, "codex_cartography.claims.owner_label", c.owner());
        else rows = 2;

        y += 16 + rows * 12;
        if (!c.subAreas().isEmpty()) {
            label(g, px, y, "codex_cartography.claims.sub_areas_list");
            y += 10;
            for (ClaimsData.SubArea a : c.subAreas()) {
                if (y > height - 110) break;
                g.fill(px, y, pr, y + 14, Codex.CARD);
                Codex.outline(g, px + 3, y + 3, px + 11, y + 11, 0xFF78C8FF);
                g.drawString(font, a.name(), px + 15, y + 3, Codex.TEXT, false);
                String size = (a.x2() - a.x1() + 1) + " × " + (a.z2() - a.z1() + 1);
                g.drawString(font, size, pr - 4 - font.width(size), y + 3, Codex.MUTE, false);
                y += 16;
            }
            y += 4;
        }

        marksY = y;
        label(g, px, y, "codex_cartography.claims.marks");
        Component seen = Component.translatable("codex_cartography.claims.marks_seen");
        g.pose().pushPose();
        g.pose().translate(pr - font.width(seen) * 0.7f, y + 1, 0);
        g.pose().scale(0.7f, 0.7f, 1);
        g.drawString(font, seen, 0, 0, Codex.MUTE, false);
        g.pose().popPose();
        y += 10;
        for (ClaimsData.ClaimMark m : c.marks()) {
            if (y > height - 60) break;
            g.fill(px, y, pr, y + 16, Codex.CARD);
            com.tac5studios.codexcartography.mark.Mark mk = new com.tac5studios.codexcartography.mark.Mark("", m.name(),
                    m.emblem().isEmpty() ? "flag" : m.emblem(), m.color(), c.dim(), m.x(), m.y(), m.z());
            mk.drawBadge(g, px + 8, y + 8, 14);
            g.drawString(font, m.name(), px + 18, y + 4, Codex.TEXT, false);
            if (canManage(c)) {
                Codex.tex(g, Tex.action("remove"), pr - 14, y + 2, 12, 12, 24, 24);
                screen.hover(pr - 14, y + 2, pr - 2, y + 14, Component.translatable("codex_cartography.claims.mark_remove"));
            }
            y += 18;
        }
        if (c.marks().isEmpty()) {
            g.drawString(font, Component.translatable("codex_cartography.claims.no_marks"), px, y, Codex.MUTE, false);
            y += 12;
        }
        if (canManage(c)) {
            g.fill(px, y, pr, y + 14, 0xFF121E1A);
            Codex.outline(g, px, y, pr, y + 14, 0xFF1E5A3C);
            Codex.tex(g, Tex.action("new_mark"), px + 2, y + 1, 12, 12, 24, 24);
            g.drawString(font, Component.translatable("codex_cartography.claims.mark_add"), px + 18, y + 3, Codex.accent(), false);
        }

        buttonsY = height - 30;
        g.fill(px, buttonsY - 6, pr, buttonsY - 5, Codex.LINE);
        button(g, px, buttonsY, 92, Tex.action("settings"), Component.translatable("codex_cartography.claims.land_config"), true);
        if (c.standing() == ClaimsData.OWN && !c.active()) {
            button(g, px + 96, buttonsY, pr - px - 96, Tex.action("confirm"), Component.translatable("codex_cartography.claims.make_active"), false);
        }
        g.pose().popPose();
    }

    private static ChunkPos center(ClaimsData.Claim c) {
        long sx = 0;
        long sz = 0;
        for (long ch : c.chunks()) {
            sx += ChunkPos.getX(ch);
            sz += ChunkPos.getZ(ch);
        }
        int n = Math.max(1, c.chunks().length);
        return new ChunkPos((int) (sx / n), (int) (sz / n));
    }

    private static void label(GuiGraphics g, int x, int y, String key) {
        g.drawString(Codex.font(), Component.translatable(key).getString().toUpperCase(Locale.ROOT), x, y, Codex.MUTE, false);
    }

    /** "Label  value" in a box w wide: the value starts after the label and shrinks if it would run past the box. */
    private static void info(GuiGraphics g, int x, int y, int w, String key, String value) {
        Font font = Codex.font();
        Component label = Component.translatable(key);
        g.drawString(font, label, x, y, Codex.MUTE, false);
        int vx = x + Math.max(44, font.width(label) + 6);
        shrink(g, Component.literal(value), vx, y, x + w - vx, Codex.TEXT);
    }

    /** Left-aligned text, scaled down to fit max pixels when it's wider. */
    private static void shrink(GuiGraphics g, Component text, int x, int y, int max, int color) {
        Font font = Codex.font();
        int tw = font.width(text);
        if (tw <= max || max <= 0) {
            g.drawString(font, text, x, y, color, false);
            return;
        }
        float s = max / (float) tw;
        g.pose().pushPose();
        g.pose().translate(x, y + font.lineHeight * (1 - s) / 2f, 0);
        g.pose().scale(s, s, 1);
        g.drawString(font, text, 0, 0, color, false);
        g.pose().popPose();
    }

    private static void meter(GuiGraphics g, int x, int y, int w, String key, int used, int max) {
        Font font = Codex.font();
        g.fill(x, y, x + w, y + 26, Codex.CARD);
        Codex.outline(g, x, y, x + w, y + 26, Codex.LINE);
        String v = used + " / " + max;
        g.drawString(font, v, x + w - 4 - font.width(v), y + 3, Codex.TEXT, false);
        shrink(g, Component.translatable(key), x + 4, y + 3, w - 14 - font.width(v), Codex.MUTE);
        g.fill(x + 4, y + 17, x + w - 4, y + 20, 0xFF1E262C);
        if (max > 0) g.fill(x + 4, y + 17, x + 4 + (int) ((w - 8) * Math.min(1.0, used / (double) max)), y + 20, Codex.accent());
    }

    private static void button(GuiGraphics g, int x, int y, int w, net.minecraft.resources.ResourceLocation glyph, Component label, boolean primary) {
        g.fill(x, y, x + w, y + 20, primary ? Codex.CARD_ON : Codex.CARD);
        Codex.outline(g, x, y, x + w, y + 20, primary ? Codex.accent() : Codex.LINE);
        Codex.tex(g, glyph, x + 3, y + 3, 14, 14, 24, 24);
        g.drawString(Codex.font(), label, x + 20, y + 6, primary ? Codex.accent() : Codex.TEXT, false);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        ClaimsData.Claim c = claim();
        if (c == null) return true;
        int px = x0 + 10;
        int pr = x0 + W - 8;
        if (Codex.inside(mx, my, px, buttonsY, px + 92, buttonsY + 20)) {
            CompoundTag t = new CompoundTag();
            t.putString("op", "open_config");
            t.putInt("id", c.id());
            ClientNet.sendClaims(t);
            return true;
        }
        if (c.standing() == ClaimsData.OWN && !c.active() && Codex.inside(mx, my, px + 96, buttonsY, pr, buttonsY + 20)) {
            CompoundTag t = new CompoundTag();
            t.putString("op", "set_active");
            t.putInt("id", c.id());
            ClientNet.sendClaims(t);
            return true;
        }
        if (!canManage(c)) return true;
        int y = marksY + 10;
        for (ClaimsData.ClaimMark m : c.marks()) {
            if (Codex.inside(mx, my, pr - 14, y + 2, pr - 2, y + 14)) {
                CompoundTag t = new CompoundTag();
                t.putString("op", "mark_remove");
                t.putInt("claim", c.id());
                t.putString("id", m.id());
                ClientNet.sendClaims(t);
                return true;
            }
            y += 18;
        }
        if (c.marks().isEmpty()) y += 12;
        if (Codex.inside(mx, my, px, y, pr, y + 14)) {
            LocalPlayer p = Minecraft.getInstance().player;
            ChunkPos ce = center(c);
            int mx2 = p != null ? p.getBlockX() : ce.getMiddleBlockX();
            int my2 = p != null ? p.getBlockY() : 64;
            int mz2 = p != null ? p.getBlockZ() : ce.getMiddleBlockZ();
            screen.openDrawer(MarkEditor.forClaim(screen, c, mx2, my2, mz2));
            return true;
        }
        return true;
    }
}
