package com.tac5studios.codexcartography.screen;

import net.minecraft.client.gui.GuiGraphics;

/** A panel that slides in from the right edge of the world map (mark editor, claims panel). */
public interface Drawer {

    int width();

    void render(GuiGraphics g, int x0, int height, int mouseX, int mouseY, float partial);

    boolean mouseClicked(double mx, double my, int button);

    default boolean mouseDragged(double mx, double my, int button, double dx, double dy) { return false; }

    default boolean mouseReleased(double mx, double my, int button) { return false; }

    default boolean mouseScrolled(double mx, double my, double scroll) { return false; }

    default boolean keyPressed(int keyCode, int scanCode, int modifiers) { return false; }

    default boolean charTyped(char c, int modifiers) { return false; }

    /** True while a text field in the drawer has focus (keys go to it, not to the map). */
    default boolean typing() { return false; }

    /** False once the drawer has closed itself. */
    boolean open();
}
