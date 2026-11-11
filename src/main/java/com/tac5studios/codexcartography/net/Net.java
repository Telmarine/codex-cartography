package com.tac5studios.codexcartography.net;

import com.tac5studios.codexcartography.server.Discovery;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Registers the mod's payloads. Every payload is optional, so vanilla clients can join a server with the mod
 * and the mod's clients can join servers without it.
 */
public final class Net {

    private Net() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar r = event.registrar("1").optional();
        r.playToClient(SyncPayload.TYPE, SyncPayload.CODEC, (p, ctx) -> ClientNet.onSync(p.tag()));
        r.playToServer(LookPayload.TYPE, LookPayload.CODEC, (p, ctx) -> {
            if (ctx.player() instanceof ServerPlayer sp) Discovery.look(sp, p.pos());
        });
        // Elements: Claims registers this channel itself on the server; the map only needs it on the client.
        if (FMLEnvironment.dist == Dist.CLIENT) {
            PayloadRegistrar claims = event.registrar("1").optional();
            claims.playBidirectional(ClaimsPayload.TYPE, ClaimsPayload.CODEC, (p, ctx) -> ClientNet.onClaims(p.tag()));
        }
    }

    /** Sends to a player who has the map mod (others are skipped). */
    public static void send(ServerPlayer player, CompoundTag tag) {
        if (player.connection.hasChannel(SyncPayload.TYPE)) player.connection.send(new SyncPayload(tag));
    }
}
