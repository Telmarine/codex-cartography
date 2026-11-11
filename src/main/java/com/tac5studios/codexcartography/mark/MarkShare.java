package com.tac5studios.codexcartography.mark;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.tac5studios.codexcartography.CodexCartography;
import com.tac5studios.codexcartography.client.ClientRules;
import com.tac5studios.codexcartography.client.MapSession;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

import java.util.Base64;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sharing marks in chat. The shared line reads normally for everyone; players with the map mod get an
 * "[Add to Ledger]" link after it (a client-only command, so the server never sees it).
 */
@EventBusSubscriber(modid = CodexCartography.MOD_ID, value = Dist.CLIENT)
@SuppressWarnings("unused") // handlers are called by the event bus
public final class MarkShare {

    private static final Pattern LINE = Pattern.compile("\\[Mark] (.{1,48}) @ (-?\\d+) (-?\\d+) (-?\\d+) \\(([a-z0-9_.\\-]+:[a-z0-9_./\\-]+)\\)");

    private MarkShare() {}

    /** Sends a mark to chat. */
    public static void share(Mark m) {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null || !ClientRules.markSharing) return;
        String name = m.name.replace("@", "").replace("(", "").replace(")", "");
        p.connection.sendChat("[Mark] " + name + " @ " + m.x + " " + m.y + " " + m.z + " (" + m.dim + ")");
    }

    @SubscribeEvent
    public static void onChat(ClientChatReceivedEvent event) {
        String text = event.getMessage().getString();
        Matcher mt = LINE.matcher(text);
        if (!mt.find()) return;
        String data = mt.group(1) + "\n" + mt.group(2) + "\n" + mt.group(3) + "\n" + mt.group(4) + "\n" + mt.group(5);
        String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(data.getBytes(StandardCharsets.UTF_8));
        Component link = Component.translatable("codex_cartography.share.add").withStyle(Style.EMPTY
                .withColor(ChatFormatting.GREEN)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/codexmark " + encoded))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("codex_cartography.share.add_hover"))));
        event.setMessage(event.getMessage().copy().append(" ").append(link));
    }

    @SubscribeEvent
    public static void onCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("codexmark")
                .then(Commands.argument("data", StringArgumentType.word()).executes(ctx -> {
                    add(StringArgumentType.getString(ctx, "data"));
                    return 1;
                })));
    }

    private static void add(String encoded) {
        MapSession s = MapSession.get();
        LocalPlayer p = Minecraft.getInstance().player;
        if (s == null || p == null) return;
        try {
            String[] parts = new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8).split("\n");
            ResourceLocation dim = ResourceLocation.tryParse(parts[4]);
            if (dim == null) return;
            Mark m = new Mark(MarkStore.newId(), parts[0], "flag", Emblems.GREEN, dim,
                    Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
            s.marks().add(m);
            s.marks().save();
            p.displayClientMessage(Component.translatable("codex_cartography.share.added", parts[0]), true);
        } catch (RuntimeException e) {
            p.displayClientMessage(Component.translatable("codex_cartography.share.bad"), true);
        }
    }
}
