package com.tac5studios.codexcartography.hud;

import com.tac5studios.codexcartography.client.ClientRules;
import com.tac5studios.codexcartography.config.ClientConfig;
import com.tac5studios.codexcartography.config.Features;
import com.tac5studios.codexcartography.ui.Codex;
import com.tac5studios.codexcartography.ui.Tex;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.VehicleEntity;

/** The Sweep: players, mobs, pets, vehicles and items near the player, drawn on the minimap. */
public final class Sweep {

    private static final int MAX = 256;
    /** Heads past this many are drawn as dots. */
    private static final int MAX_HEADS = 64;

    private Sweep() {}

    private static boolean dimOk() {
        Minecraft mc = Minecraft.getInstance();
        return Features.SWEEP.get() && mc.level != null && !ClientRules.noSweep.contains(mc.level.dimension().location());
    }

    public static boolean players() { return dimOk() && ClientRules.sweepPlayers && ClientConfig.SWEEP_PLAYERS.get(); }
    public static boolean hostile() { return dimOk() && ClientRules.sweepMobs && ClientConfig.SWEEP_HOSTILE.get(); }
    public static boolean passive() { return dimOk() && ClientRules.sweepMobs && ClientConfig.SWEEP_PASSIVE.get(); }
    public static boolean pets() { return dimOk() && ClientRules.sweepPets && ClientConfig.SWEEP_PETS.get(); }
    public static boolean vehicles() { return dimOk() && ClientRules.sweepVehicles && ClientConfig.SWEEP_VEHICLES.get(); }
    public static boolean items() { return dimOk() && ClientRules.sweepItems && ClientConfig.SWEEP_ITEMS.get(); }

    public static boolean anyOn() {
        return players() || hostile() || passive() || pets() || vehicles() || items();
    }

    /** The Sweep kind of an entity (sweep texture name), or null when it isn't shown. */
    public static String kind(Entity e, LocalPlayer self) {
        if (e == self) return null;
        if (e instanceof Player p) return players() && !p.isSpectator() ? "player" : null;
        if (e instanceof OwnableEntity o && o.getOwnerUUID() != null) return pets() ? "pet" : null;
        if (e instanceof Enemy) return hostile() ? "hostile" : null;
        if (e instanceof Animal || e instanceof WaterAnimal || e instanceof AmbientCreature || e instanceof AbstractVillager) {
            return passive() ? "passive" : null;
        }
        if (e instanceof VehicleEntity) return vehicles() ? "vehicle" : null;
        if (e instanceof ItemEntity) return items() ? "item" : null;
        return null;
    }

    static void draw(GuiGraphics g, ClientLevel level, LocalPlayer self, Projection proj, float partial) {
        if (!anyOn()) return;
        float size = Math.max(5, proj.radius / 8f);
        int count = 0;
        int heads = 0;
        ClientConfig.MobIcons icons = ClientConfig.MOB_ICONS.get();
        boolean useHeads = icons == ClientConfig.MobIcons.HEADS
                || (icons == ClientConfig.MobIcons.HOLD && com.tac5studios.codexcartography.client.OneKey.MOB_HEADS.isDown());
        boolean rotate = ClientConfig.ROTATE.get();
        float myYaw = self.getViewYRot(partial);
        boolean nearHeight = ClientConfig.SWEEP_NEAR_HEIGHT.get();
        int range = ClientConfig.SWEEP_HEIGHT_RANGE.get();
        double myY = self.getY();
        for (Entity e : level.entitiesForRendering()) {
            String kind = kind(e, self);
            if (kind == null) continue;
            double x = e.xo + (e.getX() - e.xo) * partial;
            double z = e.zo + (e.getZ() - e.zo) * partial;
            if (!proj.inside(x, z)) continue;
            if (nearHeight && !kind.equals("player") && Math.abs(e.getY() - myY) > range) continue;
            if (++count > MAX) break;
            float[] o = proj.offset(x, z);
            float sx = proj.cx + o[0];
            float sy = proj.cy + o[1];
            if (kind.equals("player")) {
                float yaw = e.getViewYRot(partial);
                float angle = rotate ? yaw - myYaw : yaw + 180f;
                Codex.texAt(g, Tex.OTHER, sx, sy, size * 1.4f, angle, 32, 0xFFFFFFFF);
                continue;
            }
            boolean mine = e instanceof OwnableEntity o2 && self.getUUID().equals(o2.getOwnerUUID());
            if (mine) Codex.texAt(g, Tex.GLOW, sx, sy, size * 2.2f, 0, 64, Codex.accent(200));
            boolean mob = kind.equals("hostile") || kind.equals("passive") || kind.equals("pet");
            if (useHeads && mob && heads < MAX_HEADS) {
                // the dot underneath shows as a colored rim: hostile, passive or pet
                Codex.texAt(g, Tex.sweep(kind), sx, sy, size * 1.7f, 0, 16, 0xFFFFFFFF);
                if (MobHeads.draw(g, e, sx, sy, size * 1.25f)) {
                    heads++;
                    continue;
                }
            }
            Codex.texAt(g, Tex.sweep(kind), sx, sy, size, 0, 16, 0xFFFFFFFF);
        }
    }
}
