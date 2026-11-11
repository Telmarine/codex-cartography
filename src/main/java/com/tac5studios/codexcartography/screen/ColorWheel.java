package com.tac5studios.codexcartography.screen;

import com.tac5studios.codexcartography.config.ClientConfig;
import com.tac5studios.codexcartography.ui.Codex;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * The Codex color picker: a hue ring around a shade square (saturation left to right, brightness top to bottom),
 * a hex field, 16 quick colors and the player's last 8 picks. Used everywhere a color is picked.
 */
public final class ColorWheel {

    public static final int[] QUICK = {0x39FF8C, 0x28C85A, 0x78DC6E, 0x40E0D0, 0x46AAFF, 0x3C5AE6, 0x9664FF, 0xC878FF,
            0xFF6EC8, 0xFF5050, 0xFF9A2E, 0xFFD23C, 0xF0F0F0, 0xA0A6AC, 0x8C643C, 0x5A3C28};
    private static final int R = 30;
    private static final int RING = 6;

    private float hue;
    private float sat;
    private float val;
    private int x;
    private int y;
    private int dragging;
    private final EditBox hex;
    private boolean typing;

    public ColorWheel(Font font, int rgb) {
        hex = new EditBox(font, 0, 0, 52, 12, Component.literal("hex"));
        hex.setMaxLength(7);
        hex.setBordered(false);
        hex.setTextColor(Codex.TEXT);
        hex.setResponder(this::fromHex);
        set(rgb);
    }

    public void set(int rgb) {
        float[] hsv = rgbToHsv(rgb);
        hue = hsv[0];
        sat = hsv[1];
        val = hsv[2];
        syncHex();
    }

    public int rgb() {
        return Mth.hsvToRgb(hue, sat, val) & 0xFFFFFF;
    }

    public float brightness() {
        return val;
    }

    private void syncHex() {
        typing = true;
        hex.setValue(Codex.hex(rgb()));
        typing = false;
    }

    private void fromHex(String s) {
        if (typing) return;
        String t = s.startsWith("#") ? s.substring(1) : s;
        if (t.length() != 6) return;
        try {
            float[] hsv = rgbToHsv(Integer.parseInt(t, 16));
            hue = hsv[0];
            sat = hsv[1];
            val = hsv[2];
        } catch (NumberFormatException ignored) {
            // not a color yet; keep the old one
        }
    }

    /** Total height of the picker. */
    public static int height() {
        return R * 2 + 4;
    }

    public void render(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        this.x = x;
        this.y = y;
        int cx = x + R;
        int cy = y + R;
        for (int a = 0; a < 360; a += 3) {
            int col = 0xFF000000 | Mth.hsvToRgb(a / 360f, 1f, 1f);
            for (int r = R - RING; r < R; r += 2) {
                int px = cx + (int) Math.round(Math.cos(Math.toRadians(a - 90)) * r);
                int py = cy + (int) Math.round(Math.sin(Math.toRadians(a - 90)) * r);
                g.fill(px - 1, py - 1, px + 2, py + 2, col);
            }
        }
        int half = squareHalf();
        for (int i = 0; i < half * 2; i++) {
            int top = 0xFF000000 | Mth.hsvToRgb(hue, i / (half * 2f - 1), 1f);
            g.fillGradient(cx - half + i, cy - half, cx - half + i + 1, cy + half, top, 0xFF000000);
        }
        int kx = cx - half + Math.round(sat * (half * 2 - 1));
        int ky = cy - half + Math.round((1 - val) * (half * 2 - 1));
        Codex.outline(g, kx - 3, ky - 3, kx + 4, ky + 4, 0xFFFFFFFF);
        double ha = Math.toRadians(hue * 360 - 90);
        int hx = cx + (int) Math.round(Math.cos(ha) * (R - RING / 2.0));
        int hy = cy + (int) Math.round(Math.sin(ha) * (R - RING / 2.0));
        Codex.outline(g, hx - 4, hy - 4, hx + 5, hy + 5, 0xFFFFFFFF);

        Font font = Codex.font();
        int rx = x + R * 2 + 8;
        g.drawString(font, Component.translatable("codex_cartography.color.hex"), rx, y, Codex.MUTE, false);
        g.fill(rx, y + 9, rx + 56, y + 21, Codex.CARD);
        Codex.outline(g, rx, y + 9, rx + 56, y + 21, Codex.LINE);
        hex.setX(rx + 3);
        hex.setY(y + 11);
        hex.render(g, mouseX, mouseY, 0);
        g.fill(rx + 60, y + 9, rx + 72, y + 21, 0xFF000000 | rgb());
        Codex.outline(g, rx + 60, y + 9, rx + 72, y + 21, Codex.TEXT);
        g.drawString(font, Component.translatable("codex_cartography.color.quick"), rx, y + 24, Codex.MUTE, false);
        for (int i = 0; i < QUICK.length; i++) {
            int qx = rx + (i % 8) * 10;
            int qy = y + 33 + (i / 8) * 10;
            g.fill(qx, qy, qx + 8, qy + 8, 0xFF000000 | QUICK[i]);
            if (QUICK[i] == rgb()) Codex.outline(g, qx - 1, qy - 1, qx + 9, qy + 9, Codex.TEXT);
        }
        List<Integer> recent = recent();
        if (!recent.isEmpty()) {
            g.drawString(font, Component.translatable("codex_cartography.color.recent"), rx, y + 54, Codex.MUTE, false);
            for (int i = 0; i < recent.size(); i++) {
                int qx = rx + i * 10;
                g.fill(qx, y + 63, qx + 8, y + 71, 0xFF000000 | recent.get(i));
            }
        }
    }

    private int squareHalf() {
        return (int) ((R - RING - 2) / Math.sqrt(2));
    }

    private static List<Integer> recent() {
        List<Integer> out = new ArrayList<>();
        for (Object o : ClientConfig.RECENT_COLORS.get()) if (o instanceof Integer i) out.add(i);
        return out;
    }

    /** Remembers a picked color in the "recent" row. */
    public static void remember(int rgb) {
        List<Integer> list = recent();
        list.remove(Integer.valueOf(rgb));
        list.addFirst(rgb);
        while (list.size() > 8) list.removeLast();
        ClientConfig.set(ClientConfig.RECENT_COLORS, list);
    }

    public boolean mouseClicked(double mx, double my) {
        int cx = x + R;
        int cy = y + R;
        double dx = mx - cx;
        double dy = my - cy;
        double d = Math.sqrt(dx * dx + dy * dy);
        int half = squareHalf();
        if (Math.abs(dx) <= half && Math.abs(dy) <= half) {
            dragging = 2;
            pickSquare(mx, my);
            return true;
        }
        if (d >= R - RING - 1 && d <= R + 1) {
            dragging = 1;
            pickRing(mx, my);
            return true;
        }
        int rx = x + R * 2 + 8;
        if (Codex.inside(mx, my, rx, y + 9, rx + 56, y + 21)) {
            hex.setFocused(true);
            return hex.mouseClicked(mx, my, 0);
        }
        hex.setFocused(false);
        for (int i = 0; i < QUICK.length; i++) {
            int qx = rx + (i % 8) * 10;
            int qy = y + 33 + (i / 8) * 10;
            if (Codex.inside(mx, my, qx, qy, qx + 8, qy + 8)) {
                set(QUICK[i]);
                return true;
            }
        }
        List<Integer> recent = recent();
        for (int i = 0; i < recent.size(); i++) {
            int qx = rx + i * 10;
            if (Codex.inside(mx, my, qx, y + 63, qx + 8, y + 71)) {
                set(recent.get(i));
                return true;
            }
        }
        return false;
    }

    public boolean mouseDragged(double mx, double my) {
        if (dragging == 1) pickRing(mx, my);
        else if (dragging == 2) pickSquare(mx, my);
        return dragging != 0;
    }

    public void mouseReleased() {
        dragging = 0;
    }

    private void pickRing(double mx, double my) {
        double a = Math.toDegrees(Math.atan2(my - (y + R), mx - (x + R))) + 90;
        hue = (float) (((a % 360) + 360) % 360 / 360.0);
        syncHex();
    }

    private void pickSquare(double mx, double my) {
        int half = squareHalf();
        sat = (float) Mth.clamp((mx - (x + R - half)) / (half * 2 - 1), 0, 1);
        val = (float) Mth.clamp(1 - (my - (y + R - half)) / (half * 2 - 1), 0, 1);
        syncHex();
    }

    public boolean keyPressed(int key, int scan, int mods) {
        return hex.isFocused() && hex.keyPressed(key, scan, mods);
    }

    public boolean charTyped(char c, int mods) {
        return hex.isFocused() && hex.charTyped(c, mods);
    }

    public boolean typing() {
        return hex.isFocused();
    }

    static float[] rgbToHsv(int rgb) {
        float r = ((rgb >> 16) & 0xFF) / 255f;
        float g = ((rgb >> 8) & 0xFF) / 255f;
        float b = (rgb & 0xFF) / 255f;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float d = max - min;
        float h;
        if (d == 0) h = 0;
        else if (max == r) h = ((g - b) / d) % 6;
        else if (max == g) h = (b - r) / d + 2;
        else h = (r - g) / d + 4;
        h /= 6;
        if (h < 0) h += 1;
        return new float[] {h, max == 0 ? 0 : d / max, max};
    }
}
