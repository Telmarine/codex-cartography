package com.tac5studios.codexcartography.mark;

import java.util.List;

/** The emblem grid in the mark editor, and default colors. */
public final class Emblems {

    /** The 14 emblems players can pick, in grid order. */
    public static final List<String> GRID = List.of("keep", "home", "village", "farm", "mine", "treasure", "shrine",
            "ruin", "skull", "gate", "flag", "star", "question", "danger");

    public static final int GREEN = 0x39FF8C;
    public static final int YELLOW = 0xFFD63C;
    public static final int PORTAL = 0xFF5050;
    public static final int ANCIENT_CITY = 0xAA64FF;
    public static final int OCEAN_MONUMENT = 0x2ED6C8;

    private Emblems() {}

    public static int defaultColor(String emblem) {
        return switch (emblem) {
            case "skull" -> YELLOW;
            case "portal" -> PORTAL;
            case "ancient_city" -> ANCIENT_CITY;
            case "ocean_monument" -> OCEAN_MONUMENT;
            default -> GREEN;
        };
    }

    /** Lang key of an emblem's name (shown on hover). */
    public static String langKey(String emblem) {
        return "codex_cartography.emblem." + emblem;
    }
}
