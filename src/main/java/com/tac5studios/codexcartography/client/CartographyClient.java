package com.tac5studios.codexcartography.client;

import com.tac5studios.codexcartography.CodexCartography;
import com.tac5studios.codexcartography.hud.Minimap;
import com.tac5studios.codexcartography.mark.InWorldMarks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/** Client start-up: the Codex key (plus optional single-action keys), the in-world marks layer and the minimap layer. */
@Mod(value = CodexCartography.MOD_ID, dist = Dist.CLIENT)
public final class CartographyClient {

    public CartographyClient(IEventBus modBus) {
        modBus.addListener(CartographyClient::registerKeys);
        modBus.addListener(CartographyClient::registerLayers);
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(OneKey.KEY);
        event.register(OneKey.MOB_HEADS);
        for (var k : OneKey.ACTIONS) event.register(k);
    }

    private static void registerLayers(RegisterGuiLayersEvent event) {
        event.registerBelow(VanillaGuiLayers.CROSSHAIR, CodexCartography.id("marks"), InWorldMarks::render);
        event.registerAboveAll(CodexCartography.id("minimap"), Minimap::render);
    }
}
