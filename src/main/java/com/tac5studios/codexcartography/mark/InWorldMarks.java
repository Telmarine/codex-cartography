package com.tac5studios.codexcartography.mark;

import com.tac5studios.codexcartography.CodexCartography;
import com.tac5studios.codexcartography.client.MapSession;
import com.tac5studios.codexcartography.config.ClientConfig;
import com.tac5studios.codexcartography.config.Features;
import com.tac5studios.codexcartography.ui.Codex;
import com.tac5studios.codexcartography.ui.Tex;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Marks in the world: a floating emblem with a soft glow and a short drip of light under it,
 * the name above and the distance below. No beam into the sky.
 * Drawn on the HUD at each mark's place on screen, so shader packs never touch them and they show through blocks.
 */
@EventBusSubscriber(modid = CodexCartography.MOD_ID, value = Dist.CLIENT)
@SuppressWarnings("unused") // handlers are called by the event bus
public final class InWorldMarks {

    /** Marks farther than this are drawn at this distance (same direction), so they stay visible. */
    private static final double DRAW_DIST = 48;

    /** The game's projection for the current frame, kept from the world pass. */
    private static final Matrix4f PROJECTION = new Matrix4f();
    private static boolean haveProjection;

    private record Item(Mark mark, double dist, float x, float y, float px) {}

    private InWorldMarks() {}

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        // every stage carries the same projection; taking it from each means no single stage has to fire
        PROJECTION.set(event.getProjectionMatrix());
        haveProjection = true;
    }

    /** HUD layer. */
    public static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        MapSession s = MapSession.get();
        if (!haveProjection || s == null || mc.level == null || mc.player == null || mc.options.hideGui) return;
        if (!Features.IN_WORLD_MARKS.get() || !Features.MARKS.get() || !ClientConfig.MARKS_IN_WORLD.get()) return;
        Camera cam = mc.gameRenderer.getMainCamera();
        Vec3 eye = cam.getPosition();
        Matrix4f view = new Matrix4f().rotation(cam.rotation().conjugate(new Quaternionf()));
        Matrix4f toClip = new Matrix4f(PROJECTION).mul(view);
        int w = g.guiWidth();
        int h = g.guiHeight();
        double max = ClientConfig.MARK_MAX_DISTANCE.get();
        float scale = ClientConfig.MARK_SIZE.get().floatValue();
        float time = mc.level.getGameTime() + delta.getGameTimeDeltaPartialTick(false);

        List<Item> items = new ArrayList<>();
        for (Mark m : s.marks().inWorld(mc.level.dimension().location())) {
            Vec3 d = new Vec3(m.x + 0.5, m.y + 1.8, m.z + 0.5).subtract(eye);
            double dist = d.length();
            if (dist > max || dist < 1.5) continue;
            double shown = Math.min(dist, DRAW_DIST);
            Vec3 at = d.scale(shown / dist);
            float size = (float) (0.55 * scale * (shown / 8.0) * (dist > DRAW_DIST ? Math.max(0.6, 1 - (dist - DRAW_DIST) / 2000) : 1));
            size = Math.max(size, 0.25f * scale);
            float bob = (float) Math.sin((time + m.x) * 0.08) * size * 0.05f;
            Vector4f clip = toClip.transform(new Vector4f((float) at.x, (float) (at.y + bob), (float) at.z, 1));
            if (clip.w <= 0.05f) continue; // behind the camera
            float sx = (clip.x / clip.w + 1) / 2f * w;
            float sy = (1 - clip.y / clip.w) / 2f * h;
            float px = size / clip.w * PROJECTION.m11() * h / 2f;
            if (sx < -px * 2 || sx > w + px * 2 || sy < -px * 3 || sy > h + px * 3) continue;
            items.add(new Item(m, dist, sx, sy, px));
        }
        // far ones first, so nearer marks sit on top
        items.sort(Comparator.comparingDouble(Item::dist).reversed());

        Font font = mc.font;
        for (Item it : items) {
            Mark m = it.mark();
            float x = it.x();
            float y = it.y();
            float px = it.px();
            int color = m.fixedLook() ? (m.kind == Mark.Kind.DEATH ? 0xFF3C3C : 0xFFD63C) : m.drawColor();
            Codex.texAt(g, Tex.GLOW, x, y, px * 1.8f, 0, 64, 0x90000000 | (color & 0xFFFFFF));
            if (m.kind != Mark.Kind.DEATH) drip(g, x, y, px, color);
            if (m.kind == Mark.Kind.DEATH) {
                Codex.texAt(g, Tex.DEATH, x, y, px, 0, 64, 0xFFFFFFFF);
            } else if (m.emblem.equals("danger")) {
                Codex.texAt(g, Tex.DANGER, x, y, px, 0, 64, 0xFFFFFFFF);
            } else {
                Codex.badge(g, Tex.BADGE, Tex.emblem(m.emblem), x, y, px, color & 0xFFFFFF);
            }
            text(g, font, Component.literal(m.name), x, y - px * 0.62f, px * 0.022f, 0xFFFFFFFF);
            if (ClientConfig.MARK_DISTANCE.get() && m.showDistance) {
                double dist = it.dist();
                String t = dist >= 1000 ? String.format(Locale.ROOT, "%.1fk", dist / 1000) : (int) dist + "m";
                text(g, font, Component.literal(t), x, y + px * 0.82f, px * 0.018f, 0xFFC8D2CD);
            }
        }
    }

    /** The short fade of light under the emblem. */
    private static void drip(GuiGraphics g, float x, float y, float px, int rgb) {
        g.pose().pushPose();
        g.pose().translate(x, y + px * 0.4f, 0);
        g.pose().scale(px * 0.32f / 16f, px * 1.1f / 48f, 1);
        Codex.tinted(g, Tex.DRIP, -8, 0, 16, 48, 16, 48, 0xC8000000 | (rgb & 0xFFFFFF));
        g.pose().popPose();
    }

    /** A line of text centered on x with its top at y, on a dark plate. */
    private static void text(GuiGraphics g, Font font, Component text, float x, float y, float scale, int color) {
        int tw = font.width(text);
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1);
        g.pose().translate(-tw / 2f, 0, 0);
        g.fill(-1, -1, tw + 1, font.lineHeight, 0x55000000);
        g.drawString(font, text, 0, 0, color, false);
        g.pose().popPose();
    }
}
