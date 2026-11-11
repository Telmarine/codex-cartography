package com.tac5studios.codexcartography.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tac5studios.codexcartography.client.Actions;
import com.tac5studios.codexcartography.client.ClientRules;
import com.tac5studios.codexcartography.client.MapSession;
import com.tac5studios.codexcartography.client.OneKey;
import com.tac5studios.codexcartography.config.ClientConfig;
import com.tac5studios.codexcartography.config.Features;
import com.tac5studios.codexcartography.ui.Codex;
import com.tac5studios.codexcartography.ui.Tex;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The in-game quick menu: hold the Codex key, point at a slice, let go to pick it.
 * Drawn by us over the game (not Minecraft's buttons), so it scales with the GUI scale.
 */
public final class RadialScreen extends Screen {

    private record Slice(String key, ResourceLocation glyph, Runnable action) {}

    private final List<Slice> slices = new ArrayList<>();
    private int picked = -1;

    public RadialScreen() {
        super(Component.translatable("codex_cartography.radial"));
        if (Features.MARKS.get()) slices.add(new Slice("new_mark", Tex.action("new_mark"), Actions::newMarkHere));
        if (Features.MARKS.get()) slices.add(new Slice("ledger", Tex.action("ledger"), () -> CodexScreen.open(CodexScreen.Section.LEDGER)));
        if (Features.MINIMAP.get()) {
            slices.add(new Slice("zoom_in", Tex.action("search"), () -> Actions.zoom(true)));
            slices.add(new Slice("zoom_out", Tex.action("search"), () -> Actions.zoom(false)));
        }
        if (Features.LENSES.get()) slices.add(new Slice("lens", Tex.action("lens"), Actions::nextLens));
        if (Features.DEPTH_VIEW.get() && ClientRules.depthView) slices.add(new Slice("depth_view", Tex.layer("grid", true), Actions::toggleDepth));
        if (Features.MINIMAP.get()) slices.add(new Slice("minimap", Tex.layer("players", ClientConfig.MINIMAP_ON.get()), Actions::toggleMinimap));
        slices.add(new Slice("settings", Tex.action("settings"), () -> CodexScreen.open(CodexScreen.Section.SETTINGS)));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics g, int mouseX, int mouseY, float partial) {
        g.fill(0, 0, width, height, 0x60060A0E);
    }

    private Component name(Slice s) {
        return switch (s.key()) {
            case "minimap" -> Component.translatable("codex_cartography.radial.minimap", Component.translatable(ClientConfig.MINIMAP_ON.get()
                    ? "codex_cartography.layer.on" : "codex_cartography.layer.off"));
            case "lens" -> Component.translatable("codex_cartography.radial.lens", Component.translatable("codex_cartography.lens."
                    + ClientConfig.LENS.get().name().toLowerCase(Locale.ROOT)));
            case "depth_view" -> {
                MapSession m = MapSession.get();
                yield Component.translatable("codex_cartography.radial.depth_view", Component.translatable(m != null && m.depthOn()
                        ? "codex_cartography.layer.on" : "codex_cartography.layer.off"));
            }
            default -> Component.translatable("codex_cartography.radial." + s.key());
        };
    }

    @Override
    public void render(@NotNull GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
        int n = slices.size();
        float cx = width / 2f;
        float cy = height / 2f;
        float r0 = 34;
        float r1 = 92;
        double dx = mouseX - cx;
        double dy = mouseY - cy;
        double dist = Math.sqrt(dx * dx + dy * dy);
        picked = -1;
        if (dist > r0 * 0.6) {
            double a = Math.toDegrees(Math.atan2(dy, dx)) + 90 + 180.0 / n;
            picked = (int) Math.floor(((a % 360) + 360) % 360 / (360.0 / n)) % n;
        }
        float gap = 1.6f;
        for (int i = 0; i < n; i++) {
            float a0 = -90 - 180f / n + i * 360f / n + gap;
            float a1 = a0 + 360f / n - gap * 2;
            boolean on = i == picked;
            wedge(g, cx, cy, r0, r1, a0, a1, on ? 0xF0183428 : 0xE80E1216);
            wedge(g, cx, cy, r1 - 1.2f, r1, a0, a1, on ? Codex.accent() : Codex.accent(70));
            if (on) wedge(g, cx, cy, r1, r1 + 3, a0, a1, Codex.accent(90));
        }
        g.flush();
        for (int i = 0; i < n; i++) {
            Slice s = slices.get(i);
            double am = Math.toRadians(-90 + i * 360.0 / n);
            float rm = (r0 + r1) / 2;
            int gx = Math.round(cx + (float) Math.cos(am) * rm);
            int gy = Math.round(cy + (float) Math.sin(am) * rm);
            boolean on = i == picked;
            Codex.tinted(g, s.glyph(), gx - 9, gy - 13, 18, 18, 24, 24, on ? 0xFFFFFFFF : 0xFFC8D0CC);
            if (s.key().startsWith("zoom")) {
                g.drawString(font, s.key().equals("zoom_in") ? "+" : "−", gx + 6, gy - 12, Codex.TEXT, false);
            }
            Component label = name(s);
            // shrink long names to fit across the slice at the label's height
            float room = (float) (2 * (rm + 9) * Math.sin(Math.PI / n)) - 8;
            float scale = Math.min(0.75f, room / Math.max(1, font.width(label)));
            g.pose().pushPose();
            g.pose().translate(gx, gy + 7, 0);
            g.pose().scale(scale, scale, 1);
            g.drawString(font, label, -font.width(label) / 2, 0, on ? Codex.accent() : Codex.TEXT, false);
            g.pose().popPose();
        }
        // center
        wedge(g, cx, cy, 0, r0 - 4, 0, 360, 0xF50A0E12);
        wedge(g, cx, cy, r0 - 5, r0 - 4, 0, 360, Codex.accent(90));
        g.flush();
        Component center = picked >= 0 ? name(slices.get(picked)) : Component.translatable("codex_cartography.radial");
        // both lines shrink to fit inside the center plate
        int room = (int) ((r0 - 9) * 2);
        int left = Math.round(cx) - room / 2;
        Codex.fit(g, center, left, left + room, Math.round(cy) - 9, Codex.TEXT);
        Codex.fit(g, Component.translatable("codex_cartography.radial.release"), left, left + room, Math.round(cy) + 2, Codex.MUTE);
        Component help = Component.translatable("codex_cartography.radial.help");
        g.drawString(font, help, (int) cx - font.width(help) / 2, (int) (cy + r1 + 12), 0xFFC8D2CD, false);
    }

    private static void wedge(GuiGraphics g, float cx, float cy, float r0, float r1, float a0, float a1, int argb) {
        VertexConsumer vc = g.bufferSource().getBuffer(RenderType.gui());
        Matrix4f m = g.pose().last().pose();
        int steps = Math.max(2, (int) ((a1 - a0) / 4));
        for (int i = 0; i < steps; i++) {
            double t0 = Math.toRadians(a0 + (a1 - a0) * i / steps);
            double t1 = Math.toRadians(a0 + (a1 - a0) * (i + 1) / steps);
            float c0 = (float) Math.cos(t0);
            float s0 = (float) Math.sin(t0);
            float c1 = (float) Math.cos(t1);
            float s1 = (float) Math.sin(t1);
            // same turning order as GuiGraphics.fill, or the GUI's back-face culling hides the quad
            vc.addVertex(m, cx + c0 * r0, cy + s0 * r0, 0).setColor(argb);
            vc.addVertex(m, cx + c1 * r0, cy + s1 * r0, 0).setColor(argb);
            vc.addVertex(m, cx + c1 * r1, cy + s1 * r1, 0).setColor(argb);
            vc.addVertex(m, cx + c0 * r1, cy + s0 * r1, 0).setColor(argb);
        }
    }

    /** True while the Codex key is still held. */
    private static boolean keyHeld() {
        Minecraft mc = Minecraft.getInstance();
        InputConstants.Key k = OneKey.KEY.getKey();
        long window = mc.getWindow().getWindow();
        if (k.getType() == InputConstants.Type.MOUSE) return GLFW.glfwGetMouseButton(window, k.getValue()) == GLFW.GLFW_PRESS;
        return k.getValue() != InputConstants.UNKNOWN.getValue() && InputConstants.isKeyDown(window, k.getValue());
    }

    @Override
    public void tick() {
        if (!keyHeld()) {
            Minecraft mc = Minecraft.getInstance();
            int p = picked;
            mc.setScreen(null);
            if (p >= 0 && p < slices.size()) slices.get(p).action().run();
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int p = picked;
        Minecraft.getInstance().setScreen(null);
        if (p >= 0 && p < slices.size()) slices.get(p).action().run();
        return true;
    }
}
