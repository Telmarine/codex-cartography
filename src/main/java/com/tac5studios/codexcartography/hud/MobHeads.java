package com.tac5studios.codexcartography.hud;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

/**
 * Mob heads for the Sweep: the head of each mob's own model, drawn face-on with the mob's own texture.
 * Mobs without a model we can find (or without a head part) fall back to the dot.
 */
public final class MobHeads {

    /** Head part names to look for, in order, at the top of the model and one level down. */
    private static final String[] HEAD = {"head", "head_parts"};
    private static final String[] BODY = {"body", "bone", "root", "upper_body"};

    private static final Map<EntityType<?>, Head> CACHE = new HashMap<>();
    private static final Head NONE = new Head(null, 0, 0, 0, 0);

    /** A baked head with its size and center (model units). */
    private record Head(ModelPart part, float cx, float cy, float cz, float span) {}

    private MobHeads() {}

    /** Forgets every baked head (resource reloads can change models). */
    public static void clear() {
        CACHE.clear();
    }

    /** Draws the mob's head centered on (x, y), size pixels across. Returns false when it has no head to draw. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static boolean draw(GuiGraphics g, Entity e, float x, float y, float size) {
        Head head = CACHE.computeIfAbsent(e.getType(), MobHeads::bake);
        if (head.part() == null) return false;
        ResourceLocation tex;
        try {
            EntityRenderer renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(e);
            tex = renderer.getTextureLocation(e);
        } catch (Exception ex) {
            return false;
        }
        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(x, y, 120);
        float s = size / head.span();
        pose.scale(s, s, -s);
        pose.translate(-head.cx(), -head.cy(), -head.cz());
        Lighting.setupForEntityInInventory();
        head.part().render(pose, g.bufferSource().getBuffer(RenderType.entityCutoutNoCull(tex)),
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        g.flush();
        Lighting.setupFor3DItems();
        pose.popPose();
        return true;
    }

    private static Head bake(EntityType<?> type) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        ModelPart root;
        try {
            root = Minecraft.getInstance().getEntityModels().bakeLayer(new ModelLayerLocation(id, "main"));
        } catch (Exception ex) {
            return NONE;
        }
        ModelPart part = find(root);
        if (part == null) part = root;
        part.setPos(0, 0, 0);
        part.setRotation(0, 0, 0);
        float[] box = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        part.visit(new PoseStack(), (p, path, index, cube) -> {
            Matrix4f m = p.pose();
            float[] xs = {cube.minX, cube.maxX};
            float[] ys = {cube.minY, cube.maxY};
            float[] zs = {cube.minZ, cube.maxZ};
            for (float cx : xs) for (float cy : ys) for (float cz : zs) {
                Vector3f v = m.transformPosition(new Vector3f(cx / 16f, cy / 16f, cz / 16f));
                box[0] = Math.min(box[0], v.x); box[1] = Math.min(box[1], v.y); box[2] = Math.min(box[2], v.z);
                box[3] = Math.max(box[3], v.x); box[4] = Math.max(box[4], v.y); box[5] = Math.max(box[5], v.z);
            }
        });
        if (box[0] > box[3]) return NONE;
        float span = Math.max(box[3] - box[0], box[4] - box[1]);
        if (span <= 0) return NONE;
        return new Head(part, (box[0] + box[3]) / 2f, (box[1] + box[4]) / 2f, (box[2] + box[5]) / 2f, span);
    }

    /** The head part: at the top of the model, or one level down inside the body. */
    private static ModelPart find(ModelPart root) {
        ModelPart p = child(root, HEAD);
        if (p != null) return p;
        for (String b : BODY) {
            ModelPart body = child(root, b);
            if (body == null) continue;
            p = child(body, HEAD);
            if (p != null) return p;
        }
        return null;
    }

    private static ModelPart child(ModelPart parent, String... names) {
        for (String n : names) {
            try {
                return parent.getChild(n);
            } catch (Exception ignored) {
                // not this one
            }
        }
        return null;
    }
}
