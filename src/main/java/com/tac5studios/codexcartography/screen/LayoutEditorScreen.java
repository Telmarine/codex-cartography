package com.tac5studios.codexcartography.screen;

import com.tac5studios.codexcartography.config.ClientConfig;
import com.tac5studios.codexcartography.hud.Minimap;
import com.tac5studios.codexcartography.ui.Codex;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/**
 * Drag the minimap anywhere on screen; scroll to resize. The areas other HUD parts use (side panels on the right,
 * boss bars top center, effect icons top right) are shown so the minimap isn't dropped over them.
 */
public final class LayoutEditorScreen extends Screen {

    private final Screen back;
    private int x;
    private int y;
    private int size;
    private boolean dragging;
    private double grabX;
    private double grabY;

    public LayoutEditorScreen(Screen back) {
        super(Component.translatable("codex_cartography.layout"));
        this.back = back;
        this.x = ClientConfig.MINIMAP_X.get();
        this.y = ClientConfig.MINIMAP_Y.get();
        this.size = ClientConfig.MINIMAP_SIZE.get();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics g, int mouseX, int mouseY, float partial) {
        g.fill(0, 0, width, height, 0x50000000);
    }

    @Override
    public void render(@NotNull GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
        zone(g, width - 110, 40, width, height - 60, "codex_cartography.layout.side_panel");
        zone(g, width / 2 - 92, 0, width / 2 + 92, 22, "codex_cartography.layout.boss_bars");
        zone(g, width - 120, 0, width, 34, "codex_cartography.layout.effects");
        x = Math.max(0, Math.min(x, width - size));
        y = Math.max(0, Math.min(y, height - size));
        Minimap.draw(g, Minecraft.getInstance(), x, y, size, partial);
        Codex.brackets(g, x - 3, y - 3, x + size + 3, y + size + 3, 10, Codex.accent());
        Component help = Component.translatable("codex_cartography.layout.help");
        int hw = font.width(help) + 16;
        Codex.panel(g, width / 2 - hw / 2, height - 40, width / 2 + hw / 2, height - 22);
        g.drawString(font, help, width / 2 - font.width(help) / 2, height - 35, Codex.TEXT, false);
    }

    private void zone(GuiGraphics g, int x0, int y0, int x1, int y1, String key) {
        for (int i = x0 - (y1 - y0); i < x1; i += 6) {
            for (int j = 0; j < y1 - y0; j += 2) {
                int px = i + j;
                if (px >= x0 && px < x1) g.fill(px, y0 + j, px + 1, y0 + j + 1, 0x40FFD63C);
            }
        }
        Codex.outline(g, x0, y0, x1, y1, 0x80FFD63C);
        g.drawString(font, Component.translatable(key), x0 + 3, y0 + 3, 0xFFFFD63C, false);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (Codex.inside(mx, my, x, y, x + size, y + size)) {
            dragging = true;
            grabX = mx - x;
            grabY = my - y;
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging) {
            x = (int) (mx - grabX);
            y = (int) (my - grabY);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        dragging = false;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        size = Math.max(48, Math.min(256, size + (sy > 0 ? 8 : -8)));
        return true;
    }

    @Override
    public void onClose() {
        ClientConfig.set(ClientConfig.MINIMAP_X, x);
        ClientConfig.set(ClientConfig.MINIMAP_Y, y);
        ClientConfig.set(ClientConfig.MINIMAP_SIZE, size);
        Minecraft.getInstance().setScreen(back);
    }
}
