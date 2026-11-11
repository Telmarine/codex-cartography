package com.tac5studios.codexcartography.server;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;

/** Permission nodes (NeoForge permission API), registered at start whatever the switches. */
public final class Perms {

    public static final PermissionNode<Boolean> TRAVEL = node("travel", 2);
    public static final PermissionNode<Boolean> RELOAD = node("reload", 3);
    public static final PermissionNode<Boolean> BYPASS = node("bypass", 2);

    private Perms() {}

    private static PermissionNode<Boolean> node(String name, int op) {
        return new PermissionNode<>(ResourceLocation.fromNamespaceAndPath("cartography", name), PermissionTypes.BOOLEAN,
                (player, uuid, ctx) -> player != null && player.hasPermissions(op));
    }

    public static void register(PermissionGatherEvent.Nodes event) {
        event.addNodes(TRAVEL, RELOAD, BYPASS);
    }
}
