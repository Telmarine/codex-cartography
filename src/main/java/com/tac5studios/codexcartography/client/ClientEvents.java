package com.tac5studios.codexcartography.client;

import com.tac5studios.codexcartography.CodexCartography;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;

/** Game events on the client: the map session, chunk loads and the Codex key. */
@EventBusSubscriber(modid = CodexCartography.MOD_ID, value = Dist.CLIENT)
@SuppressWarnings("unused") // handlers are called by the event bus
public final class ClientEvents {

    private ClientEvents() {}

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        MapSession.tick(mc);
        OneKey.tick(mc);
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel().isClientSide() && event.getChunk() instanceof LevelChunk chunk) {
            MapSession.chunkLoaded(chunk.getPos().toLong());
        }
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            MapSession.chunkUnloaded(event.getChunk().getPos().toLong());
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        MapSession.close();
    }
}
