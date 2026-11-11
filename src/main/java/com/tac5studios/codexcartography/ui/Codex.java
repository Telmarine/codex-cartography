package com.tac5studios.codexcartography.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.tac5studios.codexcartography.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/** The Codex look: colors, corner brackets, hover tags and tinted textures, shared by every screen and the minimap. */
public final class Codex {

    public static final int DARK = 0xF00E1216;
    public static final int SLATE = 0xFF14191E;
    public static final int PAGE = 0xFF101419;
    public static final int CARD = 0xFF161C22;
    public static final int CARD_ON = 0xFF18342A;
    public static final int LINE = 0xFF283238;
    public static final int MUTE = 0xFF828E96;
    public static final int TEXT = 0xFFE8F0EC;
    public static final int GREY = 0xFF60686E;
    public static final int RED = 0xFFFF5050;
    public static final int RED_DARK = 0xFF281212;
    public static final int YELLOW = 0xFFFFD63C;

    private Codex() {}

    /** The accent color (Codex green unless the player changed it). */
    public static int accent() {
        return 0xFF000000 | ClientConfig.ACCENT.get();
    }

    /** Accent with a custom alpha (0-255). */
    public static int accent(int alpha) {
        return (alpha << 24) | (ClientConfig.ACCENT.get() & 0xFFFFFF);
    }

    public static Font font() {
        return Minecraft.getInstance().font;
    }

    public static void brackets(GuiGraphics g, int x0, int y0, int x1, int y1, int len, int color) {
        g.fill(x0, y0, x0 + len, y0 + 1, color);
        g.fill(x0, y0, x0 + 1, y0 + len, color);
        g.fill(x1 - len, y0, x1, y0 + 1, color);
        g.fill(x1 - 1, y0, x1, y0 + len, color);
        g.fill(x0, y1 - 1, x0 + len, y1, color);
        g.fill(x0, y1 - len, x0 + 1, y1, color);
        g.fill(x1 - len, y1 - 1, x1, y1, color);
        g.fill(x1 - 1, y1 - len, x1, y1, color);
    }

    public static void outline(GuiGraphics g, int x0, int y0, int x1, int y1, int color) {
        g.fill(x0, y0, x1, y0 + 1, color);
        g.fill(x0, y1 - 1, x1, y1, color);
        g.fill(x0, y0, x0 + 1, y1, color);
        g.fill(x1 - 1, y0, x1, y1, color);
    }

    /** A dark panel with accent corner brackets. */
    public static void panel(GuiGraphics g, int x0, int y0, int x1, int y1) {
        g.fill(x0, y0, x1, y1, DARK);
        brackets(g, x0, y0, x1, y1, 6, accent());
    }

    /** A card: dark fill with a thin border; selected cards get the accent border and brackets. */
    public static void card(GuiGraphics g, int x0, int y0, int x1, int y1, boolean selected) {
        g.fill(x0, y0, x1, y1, selected ? CARD_ON : CARD);
        outline(g, x0, y0, x1, y1, selected ? accent() : LINE);
        if (selected) brackets(g, x0 - 2, y0 - 2, x1 + 2, y1 + 2, 7, accent());
    }

    /** A hover tag (label) at x, y, kept on screen. First line bright, the rest muted. */
    public static void tag(GuiGraphics g, int x, int y, int bracketColor, List<Component> lines) {
        if (lines.isEmpty()) return;
        Font font = font();
        int w = 0;
        for (Component c : lines) w = Math.max(w, font.width(c));
        int h = lines.size() * 10 + 4;
        int sw = g.guiWidth();
        int sh = g.guiHeight();
        x = Math.max(2, Math.min(x, sw - w - 12));
        y = Math.max(2, Math.min(y, sh - h - 4));
        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        g.fill(x, y, x + w + 10, y + h + 2, DARK);
        brackets(g, x, y, x + w + 10, y + h + 2, 4, bracketColor);
        for (int i = 0; i < lines.size(); i++) {
            g.drawString(font, lines.get(i), x + 5, y + 3 + i * 10, i == 0 ? TEXT : MUTE, false);
        }
        g.pose().popPose();
    }

    public static void tag(GuiGraphics g, int x, int y, Component... lines) {
        tag(g, x, y, accent(), List.of(lines));
    }

    /** Wraps a long text to lines of the given width. */
    public static List<FormattedCharSequence> wrap(Component text, int width) {
        return new ArrayList<>(font().split(text, width));
    }

    /** Draws a whole texture into a rectangle. */
    public static void tex(GuiGraphics g, ResourceLocation id, int x, int y, int w, int h, int texW, int texH) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.blit(id, x, y, w, h, 0, 0, texW, texH, texW, texH);
    }

    /** Draws a whole texture tinted with an ARGB color. */
    public static void tinted(GuiGraphics g, ResourceLocation id, int x, int y, int w, int h, int texW, int texH, int argb) {
        g.setColor(((argb >> 16) & 0xFF) / 255f, ((argb >> 8) & 0xFF) / 255f, (argb & 0xFF) / 255f, ((argb >>> 24) & 0xFF) / 255f);
        tex(g, id, x, y, w, h, texW, texH);
        g.setColor(1f, 1f, 1f, 1f);
    }

    /** Draws a texture centered on a float position, scaled to size and turned by an angle (degrees). */
    public static void texAt(GuiGraphics g, ResourceLocation id, float cx, float cy, float size, float angle, int texSize, int argb) {
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0);
        if (angle != 0) g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(angle));
        float s = size / texSize;
        g.pose().scale(s, s, 1);
        tinted(g, id, -texSize / 2, -texSize / 2, texSize, texSize, texSize, texSize, argb);
        g.pose().popPose();
    }

    /** A mark badge (tinted with the mark's color) with its white emblem on top. */
    public static void badge(GuiGraphics g, ResourceLocation badge, ResourceLocation emblem, float cx, float cy, float size, int rgb) {
        texAt(g, badge, cx, cy, size, 0, 64, 0xFF000000 | rgb);
        if (emblem != null) texAt(g, emblem, cx, cy, size, 0, 64, 0xFFFFFFFF);
    }

    /** Centered text with a dark outline, for names on the map. */
    public static void label(GuiGraphics g, Component text, int cx, int y, int color) {
        Font font = font();
        int x = cx - font.width(text) / 2;
        int dark = 0xE00A0E12;
        g.drawString(font, text, x - 1, y, dark, false);
        g.drawString(font, text, x + 1, y, dark, false);
        g.drawString(font, text, x, y - 1, dark, false);
        g.drawString(font, text, x, y + 1, dark, false);
        g.drawString(font, text, x, y, color, false);
    }

    /** Text centered between x0 and x1, shrunk to fit when it's wider than that space. */
    public static void fit(GuiGraphics g, Component text, int x0, int x1, int y, int color) {
        Font font = font();
        int w = font.width(text);
        int room = x1 - x0;
        if (w <= room) {
            g.drawString(font, text, x0 + (room - w) / 2, y, color, false);
            return;
        }
        float s = room / (float) w;
        g.pose().pushPose();
        g.pose().translate(x0, y + (font.lineHeight * (1 - s)) / 2f, 0);
        g.pose().scale(s, s, 1);
        g.drawString(font, text, 0, 0, color, false);
        g.pose().popPose();
    }

    public static boolean inside(double mx, double my, int x0, int y0, int x1, int y1) {
        return mx >= x0 && mx < x1 && my >= y0 && my < y1;
    }

    /** Mixes two RGB colors. t = 0 gives a, 1 gives b. */
    public static int mix(int a, int b, float t) {
        int r = (int) (((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
        int gr = (int) (((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
        int bl = (int) ((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
        return (r << 16) | (gr << 8) | bl;
    }

    /** Multiplies an RGB color's brightness. */
    public static int scale(int rgb, float f) {
        int r = Math.min(255, (int) (((rgb >> 16) & 0xFF) * f));
        int gr = Math.min(255, (int) (((rgb >> 8) & 0xFF) * f));
        int b = Math.min(255, (int) ((rgb & 0xFF) * f));
        return (r << 16) | (gr << 8) | b;
    }

    public static String hex(int rgb) {
        return String.format("#%06X", rgb & 0xFFFFFF);
    }
}
