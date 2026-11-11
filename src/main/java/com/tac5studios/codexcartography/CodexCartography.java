package com.tac5studios.codexcartography;

import com.tac5studios.codexcartography.config.ClientConfig;
import com.tac5studios.codexcartography.config.Features;
import com.tac5studios.codexcartography.net.Net;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Codex: Cartography — a minimap and world map in the Codex style.
 * Client mod with an optional server side.
 *
 * <p>Configs: config/codex_cartography/features.toml (server and client each read their own),
 * client.toml (look and feel), and on the server side server.toml and messages.toml (server.ServerRules).
 * Everything the player sees starts in client.CartographyClient; the server side is server.ServerSync.
 */
@Mod(CodexCartography.MOD_ID)
public final class CodexCartography {

    public static final String MOD_ID = "codex_cartography";
    public static final Logger LOGGER = LoggerFactory.getLogger("CodexCartography");

    public CodexCartography(IEventBus modBus, ModContainer container) {
        modBus.addListener(Net::register);
        container.registerConfig(ModConfig.Type.COMMON, Features.SPEC, MOD_ID + "/features.toml");
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC, MOD_ID + "/client.toml");
        LOGGER.info("[Cartography] Codex: Cartography loading.");
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
