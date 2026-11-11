package com.tac5studios.codexcartography.server;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.server.permission.PermissionAPI;

/**
 * /cartography reload - re-reads server.toml and messages.toml and resends everything.
 * /cartography travel &lt;x&gt; &lt;z&gt; - used by the map's travel action (rule + permission checked).
 */
public final class CartographyCommand {

    private CartographyCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("cartography")
                .then(Commands.literal("reload")
                        .requires(s -> s.getPlayer() == null ? s.hasPermission(3) : PermissionAPI.getPermission(s.getPlayer(), Perms.RELOAD))
                        .executes(CartographyCommand::reload))
                .then(Commands.literal("travel")
                        .then(Commands.argument("x", IntegerArgumentType.integer())
                                .then(Commands.argument("z", IntegerArgumentType.integer())
                                        .executes(CartographyCommand::travel)))));
    }

    private static int reload(CommandContext<CommandSourceStack> ctx) {
        String err = ServerRules.load();
        if (err != null) {
            ctx.getSource().sendFailure(Component.literal(ServerRules.message("reload_failed", "file", "", "error", err)));
            return 0;
        }
        ServerSync.resendAll(ctx.getSource().getServer());
        ctx.getSource().sendSuccess(() -> Component.literal(ServerRules.message("reloaded")), true);
        return 1;
    }

    private static int travel(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer p = ctx.getSource().getPlayer();
        if (p == null) {
            ctx.getSource().sendFailure(Component.literal(ServerRules.message("travel_players_only")));
            return 0;
        }
        if (!ServerRules.RULES.bool("map.travel", false)) {
            p.sendSystemMessage(Component.literal(ServerRules.message("travel_off")));
            return 0;
        }
        if (!PermissionAPI.getPermission(p, Perms.TRAVEL)) {
            p.sendSystemMessage(Component.literal(ServerRules.message("travel_no_permission")));
            return 0;
        }
        int x = IntegerArgumentType.getInteger(ctx, "x");
        int z = IntegerArgumentType.getInteger(ctx, "z");
        ServerLevel level = p.serverLevel();
        if (!level.getWorldBorder().isWithinBounds(new BlockPos(x, 0, z))) {
            p.sendSystemMessage(Component.literal(ServerRules.message("travel_unsafe")));
            return 0;
        }
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        if (level.dimensionType().hasCeiling() || y <= level.getMinBuildHeight()) {
            p.sendSystemMessage(Component.literal(ServerRules.message("travel_unsafe")));
            return 0;
        }
        p.teleportTo(level, x + 0.5, y, z + 0.5, p.getYRot(), p.getXRot());
        p.sendSystemMessage(Component.literal(ServerRules.message("travel_done", "x", String.valueOf(x), "z", String.valueOf(z))));
        return 1;
    }
}
