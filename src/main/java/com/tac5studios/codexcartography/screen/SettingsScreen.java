package com.tac5studios.codexcartography.screen;

import com.tac5studios.codexcartography.client.OneKey;
import com.tac5studios.codexcartography.config.ClientConfig;
import com.tac5studios.codexcartography.hud.Minimap;
import com.tac5studios.codexcartography.ui.Codex;
import com.tac5studios.codexcartography.ui.Tex;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Settings: sections on a list beside the rail, a row per setting (toggle, slider, choice, color or button),
 * a search field that finds any setting, and a live preview. Changes save as you go.
 */
public final class SettingsScreen extends CodexScreen {

    private static final int LIST = 96;
    private static final int PREVIEW = 150;

    private enum Kind { TOGGLE, SLIDER, CHOICE, COLOR, BUTTON }

    private record Row(String section, String key, Kind kind, Supplier<Object> get, Consumer<Object> set,
                       double min, double max, Supplier<Component> shown, Runnable button, ModConfigSpec.ConfigValue<?> value) {}

    private static final String[] SECTIONS = {"minimap", "world_map", "marks", "sweep", "lenses", "claims", "world_border",
            "colors", "motion", "performance", "keys"};

    private String section = "minimap";
    private EditBox search;
    private final List<Row> rows = new ArrayList<>();
    private final List<int[]> rowBoxes = new ArrayList<>();
    private Row colorRow;
    private ColorWheel wheel;
    private Row dragging;
    private int dragX0;
    private int dragW;

    public SettingsScreen() {
        super(Component.translatable("codex_cartography.section.settings"));
        build();
    }

    @Override
    protected Section section() {
        return Section.SETTINGS;
    }

    @Override
    protected void init() {
        search = new EditBox(font, RAIL + 22, 30, LIST - 26, 12, Component.translatable("codex_cartography.settings.search"));
        search.setBordered(false);
        search.setTextColor(Codex.TEXT);
        search.setHint(Component.translatable("codex_cartography.settings.search").withColor(Codex.GREY));
        addRenderableWidget(search);
        wheel = new ColorWheel(font, 0x39FF8C);
    }

    private void toggle(String sec, String key, ModConfigSpec.BooleanValue v) {
        rows.add(new Row(sec, key, Kind.TOGGLE, v::get, o -> ClientConfig.set(v, (Boolean) o), 0, 0, null, null, v));
    }

    private void slider(String sec, String key, ModConfigSpec.DoubleValue v, double min, double max, Supplier<Component> shown) {
        rows.add(new Row(sec, key, Kind.SLIDER, v::get, o -> ClientConfig.set(v, (Double) o), min, max, shown, null, v));
    }

    private void islider(String sec, String key, ModConfigSpec.IntValue v, int min, int max, Supplier<Component> shown) {
        rows.add(new Row(sec, key, Kind.SLIDER, () -> v.get().doubleValue(), o -> ClientConfig.set(v, (int) Math.round((Double) o)), min, max, shown, null, v));
    }

    private <E extends Enum<E>> void choice(String sec, String key, ModConfigSpec.EnumValue<E> v) {
        rows.add(new Row(sec, key, Kind.CHOICE, v::get, o -> {
            E cur = v.get();
            E[] all = cur.getDeclaringClass().getEnumConstants();
            ClientConfig.set(v, all[(cur.ordinal() + 1) % all.length]);
        }, 0, 0, () -> Component.translatable("codex_cartography.settings.value." + v.get().name().toLowerCase(Locale.ROOT)), null, v));
    }

    private void color(String sec, String key, ModConfigSpec.IntValue v) {
        rows.add(new Row(sec, key, Kind.COLOR, v::get, o -> ClientConfig.set(v, (Integer) o), 0, 0, null, null, v));
    }

    private void build() {
        toggle("minimap", "minimap.show", ClientConfig.MINIMAP_ON);
        choice("minimap", "minimap.shape", ClientConfig.SHAPE);
        islider("minimap", "minimap.size", ClientConfig.MINIMAP_SIZE, 48, 256, () -> Component.literal(ClientConfig.MINIMAP_SIZE.get() + " px"));
        rows.add(new Row("minimap", "minimap.layout", Kind.BUTTON, () -> null, o -> { }, 0, 0, null,
                () -> Minecraft.getInstance().setScreen(new LayoutEditorScreen(this)), null));
        toggle("minimap", "minimap.rotate", ClientConfig.ROTATE);
        choice("minimap", "minimap.mob_icons", ClientConfig.MOB_ICONS);
        slider("minimap", "minimap.zoom", ClientConfig.MINIMAP_ZOOM, 0.25, 8, () -> Component.literal(String.format(Locale.ROOT, "%.2fx", ClientConfig.MINIMAP_ZOOM.get())));
        slider("minimap", "minimap.opacity", ClientConfig.OPACITY, 0.2, 1, () -> Component.literal(Math.round(ClientConfig.OPACITY.get() * 100) + "%"));
        toggle("minimap", "minimap.info_coords", ClientConfig.INFO_COORDS);
        toggle("minimap", "minimap.info_biome", ClientConfig.INFO_BIOME);
        toggle("minimap", "minimap.info_time", ClientConfig.INFO_TIME);
        toggle("minimap", "minimap.info_light", ClientConfig.INFO_LIGHT);
        toggle("minimap", "minimap.info_border", ClientConfig.INFO_BORDER);
        toggle("minimap", "minimap.hide_in_menus", ClientConfig.HIDE_IN_MENUS);

        toggle("world_map", "world_map.key_hints", ClientConfig.KEY_HINTS);
        toggle("world_map", "world_map.grid", ClientConfig.LAYER_GRID);
        toggle("world_map", "world_map.claims", ClientConfig.LAYER_CLAIMS);
        toggle("world_map", "world_map.players", ClientConfig.LAYER_PLAYERS);

        toggle("marks", "marks.in_world", ClientConfig.MARKS_IN_WORLD);
        islider("marks", "marks.max_distance", ClientConfig.MARK_MAX_DISTANCE, 16, 10000, () -> Component.literal(ClientConfig.MARK_MAX_DISTANCE.get() + " m"));
        toggle("marks", "marks.show_distance", ClientConfig.MARK_DISTANCE);
        slider("marks", "marks.size", ClientConfig.MARK_SIZE, 0.5, 2.5, () -> Component.literal(String.format(Locale.ROOT, "%.1fx", ClientConfig.MARK_SIZE.get())));
        islider("marks", "marks.death_points", ClientConfig.DEATH_POINTS_KEPT, 1, 20, () -> Component.literal(String.valueOf(ClientConfig.DEATH_POINTS_KEPT.get())));
        toggle("marks", "marks.death_point_clear", ClientConfig.DEATH_POINT_CLEAR);

        toggle("sweep", "sweep.players", ClientConfig.SWEEP_PLAYERS);
        toggle("sweep", "sweep.hostile", ClientConfig.SWEEP_HOSTILE);
        toggle("sweep", "sweep.passive", ClientConfig.SWEEP_PASSIVE);
        toggle("sweep", "sweep.pets", ClientConfig.SWEEP_PETS);
        toggle("sweep", "sweep.vehicles", ClientConfig.SWEEP_VEHICLES);
        toggle("sweep", "sweep.items", ClientConfig.SWEEP_ITEMS);
        toggle("sweep", "sweep.near_height", ClientConfig.SWEEP_NEAR_HEIGHT);
        islider("sweep", "sweep.height_range", ClientConfig.SWEEP_HEIGHT_RANGE, 1, 64,
                () -> Component.translatable("codex_cartography.settings.sweep.height_value", ClientConfig.SWEEP_HEIGHT_RANGE.get()));

        choice("lenses", "lenses.lens", ClientConfig.LENS);
        toggle("lenses", "lenses.depth_auto", ClientConfig.DEPTH_AUTO);

        color("claims", "claims.yours", ClientConfig.CLAIM_OWN);
        color("claims", "claims.trusted", ClientConfig.CLAIM_TRUSTED);
        color("claims", "claims.others", ClientConfig.CLAIM_OTHERS);
        color("claims", "claims.server", ClientConfig.CLAIM_SERVER);
        toggle("claims", "claims.own_color", ClientConfig.CLAIM_OWN_COLOR);
        toggle("claims", "claims.colorblind", ClientConfig.COLORBLIND);

        toggle("world_border", "world_border.minimap", ClientConfig.BORDER_MINIMAP);
        toggle("world_border", "world_border.world_map", ClientConfig.BORDER_WORLD_MAP);
        islider("world_border", "world_border.warn", ClientConfig.BORDER_WARN, 8, 2000, () -> Component.literal(ClientConfig.BORDER_WARN.get() + " blocks"));
        color("world_border", "world_border.color", ClientConfig.BORDER_COLOR);

        color("colors", "colors.accent", ClientConfig.ACCENT);
        choice("colors", "colors.palette", ClientConfig.PALETTE);
        color("colors", "colors.waystone", ClientConfig.WAYSTONE_COLOR);
        color("colors", "colors.home", ClientConfig.HOME_COLOR);
        color("colors", "colors.warp", ClientConfig.WARP_COLOR);

        toggle("motion", "motion.animations", ClientConfig.ANIMATIONS);
        toggle("motion", "motion.scan_line", ClientConfig.SCAN_LINE);
        toggle("motion", "motion.sweep_pulse", ClientConfig.SWEEP_PULSE);

        slider("performance", "performance.budget", ClientConfig.SCAN_BUDGET_MS, 0.25, 20, () -> Component.literal(String.format(Locale.ROOT, "%.2f ms", ClientConfig.SCAN_BUDGET_MS.get())));
        islider("performance", "performance.rescan", ClientConfig.RESCAN_DELAY_TICKS, 1, 200, () -> Component.literal(ClientConfig.RESCAN_DELAY_TICKS.get() + " ticks"));
        islider("performance", "performance.regions", ClientConfig.MAX_REGIONS, 8, 512, () -> Component.literal(String.valueOf(ClientConfig.MAX_REGIONS.get())));
        islider("performance", "performance.save", ClientConfig.SAVE_INTERVAL_SECONDS, 5, 600, () -> Component.literal(ClientConfig.SAVE_INTERVAL_SECONDS.get() + " s"));
        toggle("performance", "performance.overview", ClientConfig.OVERVIEW_TILES);

        islider("keys", "keys.hold", ClientConfig.HOLD_MS, 100, 1000, () -> Component.literal(ClientConfig.HOLD_MS.get() + " ms"));
    }

    private List<Row> visibleRows() {
        String q = search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
        List<Row> out = new ArrayList<>();
        for (Row r : rows) {
            if (q.isEmpty() ? r.section().equals(section)
                    : Component.translatable("codex_cartography.settings." + r.key()).getString().toLowerCase(Locale.ROOT).contains(q)) {
                out.add(r);
            }
        }
        return out;
    }

    @Override
    protected void renderPage(GuiGraphics g, int mouseX, int mouseY, float partial) {
        int lx0 = RAIL + 1;
        g.fill(lx0, 0, lx0 + LIST, height, 0xFF12161B);
        g.fill(lx0 + LIST, 0, lx0 + LIST + 1, height, Codex.LINE);
        g.drawString(font, Component.translatable("codex_cartography.section.settings").getString().toUpperCase(Locale.ROOT), lx0 + 8, 10, Codex.accent(), false);
        g.fill(lx0 + 6, 27, lx0 + LIST - 6, 43, Codex.CARD);
        Codex.outline(g, lx0 + 6, 27, lx0 + LIST - 6, 43, Codex.LINE);
        Codex.tex(g, Tex.action("search"), lx0 + 8, 29, 12, 12, 24, 24);
        int y = 52;
        for (String s : SECTIONS) {
            boolean on = s.equals(section) && search.getValue().isBlank();
            if (on) {
                g.fill(lx0, y - 3, lx0 + LIST, y + 11, Codex.CARD_ON);
                g.fill(lx0, y - 3, lx0 + 2, y + 11, Codex.accent());
            }
            g.drawString(font, Component.translatable("codex_cartography.settings.section." + s), lx0 + 10, y, on ? Codex.accent() : 0xFFC8D0CC, false);
            y += 17;
        }
        g.drawString(font, Component.translatable("codex_cartography.settings.saves"), lx0 + 8, height - 14, Codex.MUTE, false);

        int cx0 = lx0 + LIST + 14;
        int px0 = width - PREVIEW - 10;
        Component title = search.getValue().isBlank() ? Component.translatable("codex_cartography.settings.section." + section)
                : Component.translatable("codex_cartography.settings.results");
        g.drawString(font, title, cx0, 10, Codex.TEXT, false);
        if (search.getValue().isBlank()) {
            g.drawString(font, Component.translatable("codex_cartography.settings.about." + section), cx0, 22, Codex.MUTE, false);
        }
        rowBoxes.clear();
        int ry = 40;
        int ctrl = cx0 + Math.max(100, (px0 - cx0) / 2);
        for (Row r : visibleRows()) {
            g.fill(cx0, ry - 4, px0 - 10, ry - 3, 0xFF1E262C);
            g.drawString(font, Component.translatable("codex_cartography.settings." + r.key()), cx0, ry + 1, Codex.TEXT, false);
            int w = px0 - 20 - ctrl;
            switch (r.kind()) {
                case TOGGLE -> MarkEditor.toggle(g, ctrl, ry + 1, (Boolean) r.get().get(), "codex_cartography.empty");
                case SLIDER -> {
                    double v = (Double) r.get().get();
                    double t = (v - r.min()) / (r.max() - r.min());
                    int sw = w - 50;
                    g.fill(ctrl, ry + 4, ctrl + sw, ry + 6, 0xFF323C42);
                    g.fill(ctrl, ry + 4, ctrl + (int) (sw * t), ry + 6, Codex.accent());
                    int k = ctrl + (int) (sw * t);
                    g.fill(k - 3, ry, k + 3, ry + 10, Codex.DARK);
                    Codex.outline(g, k - 3, ry, k + 3, ry + 10, Codex.accent());
                    g.drawString(font, r.shown().get(), ctrl + sw + 6, ry + 1, Codex.TEXT, false);
                }
                case CHOICE -> {
                    Component c = r.shown().get();
                    g.fill(ctrl, ry - 1, ctrl + font.width(c) + 18, ry + 11, Codex.CARD);
                    Codex.outline(g, ctrl, ry - 1, ctrl + font.width(c) + 18, ry + 11, Codex.LINE);
                    g.drawString(font, c, ctrl + 4, ry + 1, Codex.TEXT, false);
                    g.drawString(font, "›", ctrl + font.width(c) + 10, ry + 1, Codex.accent(), false);
                }
                case COLOR -> {
                    int c = (Integer) r.get().get();
                    g.fill(ctrl, ry - 1, ctrl + 12, ry + 11, 0xFF000000 | c);
                    Codex.outline(g, ctrl, ry - 1, ctrl + 12, ry + 11, r == colorRow ? Codex.accent() : Codex.TEXT);
                    g.drawString(font, Codex.hex(c), ctrl + 16, ry + 1, Codex.TEXT, false);
                }
                case BUTTON -> {
                    Component b = Component.translatable("codex_cartography.settings." + r.key() + ".button");
                    g.fill(ctrl, ry - 2, ctrl + font.width(b) + 10, ry + 11, Codex.CARD_ON);
                    Codex.brackets(g, ctrl, ry - 2, ctrl + font.width(b) + 10, ry + 11, 4, Codex.accent());
                    g.drawString(font, b, ctrl + 5, ry + 1, Codex.accent(), false);
                }
            }
            rowBoxes.add(new int[] {ctrl, ry - 3, px0 - 20, ry + 12, visibleRows().indexOf(r)});
            ry += 20;
        }
        if (section.equals("keys") && search.getValue().isBlank()) {
            ry += 4;
            g.drawString(font, Component.translatable("codex_cartography.settings.keys.list"), cx0, ry, Codex.MUTE, false);
            ry += 12;
            List<KeyMapping> all = new ArrayList<>();
            all.add(OneKey.KEY);
            all.addAll(OneKey.ACTIONS);
            for (KeyMapping k : all) {
                g.drawString(font, Component.translatable(k.getName()), cx0, ry, Codex.TEXT, false);
                g.drawString(font, k.getTranslatedKeyMessage(), ctrl, ry, Codex.accent(), false);
                ry += 11;
            }
            g.drawString(font, Component.translatable("codex_cartography.settings.keys.controls"), cx0, ry + 4, Codex.MUTE, false);
        }
        drawPreview(g, px0, mouseX, mouseY);
    }

    private void drawPreview(GuiGraphics g, int x0, int mouseX, int mouseY) {
        int x1 = width - 8;
        g.fill(x0, 8, x1, height - 8, 0xFF0C1014);
        Codex.outline(g, x0, 8, x1, height - 8, Codex.LINE);
        Codex.brackets(g, x0, 8, x1, height - 8, 8, Codex.accent());
        g.drawString(font, Component.translatable("codex_cartography.settings.preview"), x0 + 8, 14, Codex.MUTE, false);
        if (colorRow != null) {
            g.drawString(font, Component.translatable("codex_cartography.settings." + colorRow.key()), x0 + 8, 28, Codex.TEXT, false);
            wheel.render(g, x0 + 8, 40, mouseX, mouseY);
            g.drawString(font, Component.translatable("codex_cartography.editor.done"), x0 + 8, 40 + ColorWheel.height() + 16, Codex.accent(), false);
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (section.equals("minimap") || section.equals("lenses") || section.equals("sweep") || section.equals("colors")) {
            int size = Math.min(x1 - x0 - 30, ClientConfig.MINIMAP_SIZE.get());
            Minimap.draw(g, mc, (x0 + x1) / 2 - size / 2, 30, size, 1f);
            g.drawString(font, Component.translatable("codex_cartography.settings.value." + ClientConfig.SHAPE.get().name().toLowerCase(Locale.ROOT)),
                    x0 + 8, height - 40, Codex.MUTE, false);
        } else {
            int ly = 30;
            for (var line : Codex.wrap(Component.translatable("codex_cartography.settings.preview." + section), x1 - x0 - 16)) {
                g.drawString(font, line, x0 + 8, ly, Codex.MUTE, false);
                ly += 10;
            }
        }
        g.drawString(font, Component.translatable("codex_cartography.settings.reset"), x0 + 8, height - 24, Codex.accent(), false);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        if (button != 0) return false;
        int lx0 = RAIL + 1;
        if (mx < lx0 + LIST) {
            int y = 52;
            for (String s : SECTIONS) {
                if (Codex.inside(mx, my, lx0, y - 3, lx0 + LIST, y + 11)) {
                    section = s;
                    search.setValue("");
                    colorRow = null;
                    return true;
                }
                y += 17;
            }
            return false;
        }
        int px0 = width - PREVIEW - 10;
        if (mx >= px0) {
            if (colorRow != null) {
                if (wheel.mouseClicked(mx, my)) {
                    colorRow.set().accept(wheel.rgb());
                    return true;
                }
                if (Codex.inside(mx, my, px0 + 8, 40 + ColorWheel.height() + 12, px0 + 80, 40 + ColorWheel.height() + 26)) {
                    ColorWheel.remember(wheel.rgb());
                    colorRow = null;
                }
            } else if (Codex.inside(mx, my, px0 + 8, height - 26, px0 + 120, height - 14)) {
                resetSection();
            }
            return true;
        }
        List<Row> vis = visibleRows();
        for (int[] b : rowBoxes) {
            if (!Codex.inside(mx, my, b[0], b[1], b[2], b[3]) || b[4] < 0 || b[4] >= vis.size()) continue;
            Row r = vis.get(b[4]);
            switch (r.kind()) {
                case TOGGLE -> r.set().accept(!(Boolean) r.get().get());
                case CHOICE -> r.set().accept(null);
                case COLOR -> {
                    colorRow = r;
                    wheel.set((Integer) r.get().get());
                }
                case BUTTON -> r.button().run();
                case SLIDER -> {
                    dragging = r;
                    dragX0 = b[0];
                    dragW = b[2] - b[0] - 50;
                    slide(mx);
                }
            }
            return true;
        }
        return false;
    }

    private void slide(double mx) {
        if (dragging == null) return;
        double t = Mth.clamp((mx - dragX0) / dragW, 0, 1);
        dragging.set().accept(dragging.min() + t * (dragging.max() - dragging.min()));
    }

    private void resetSection() {
        for (Row r : rows) {
            if (r.section().equals(section) && r.value() != null) reset(r.value());
        }
    }

    private static <T> void reset(ModConfigSpec.ConfigValue<T> v) {
        ClientConfig.set(v, v.getDefault());
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging != null) {
            slide(mx);
            return true;
        }
        if (colorRow != null && wheel.mouseDragged(mx, my)) {
            colorRow.set().accept(wheel.rgb());
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        dragging = null;
        if (wheel != null) wheel.mouseReleased();
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (colorRow != null && wheel.keyPressed(key, scan, mods)) {
            colorRow.set().accept(wheel.rgb());
            return true;
        }
        if (wheel != null && wheel.typing()) return true;
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean charTyped(char c, int mods) {
        if (colorRow != null && wheel.charTyped(c, mods)) {
            colorRow.set().accept(wheel.rgb());
            return true;
        }
        return super.charTyped(c, mods);
    }
}
