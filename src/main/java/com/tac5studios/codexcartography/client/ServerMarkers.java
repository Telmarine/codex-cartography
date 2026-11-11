package com.tac5studios.codexcartography.client;

import com.tac5studios.codexcartography.config.ClientConfig;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/** Markers the server sends: Waystones the player has activated, and Elements: Nexus homes and warps. */
public final class ServerMarkers {

    public record Marker(String kind, String name, ResourceLocation dim, int x, int y, int z) {
        public int color() {
            return switch (kind) {
                case "home" -> ClientConfig.HOME_COLOR.get();
                case "warp" -> ClientConfig.WARP_COLOR.get();
                default -> ClientConfig.WAYSTONE_COLOR.get();
            };
        }

        public boolean layerOn() {
            return kind.equals("waystone") ? ClientConfig.LAYER_WAYSTONES.get() && ClientRules.waystones
                    : ClientConfig.LAYER_HOMES.get() && ClientRules.nexusMarkers;
        }
    }

    private static final List<Marker> MARKERS = new ArrayList<>();

    private ServerMarkers() {}

    public static void set(List<Marker> markers) {
        MARKERS.clear();
        MARKERS.addAll(markers);
    }

    public static void clear() {
        MARKERS.clear();
    }

    /** Elements: Nexus homes and warps in every dimension (for the Ledger), when the server allows them. */
    public static List<Marker> homesAndWarps() {
        List<Marker> out = new ArrayList<>();
        if (!com.tac5studios.codexcartography.config.Features.NEXUS_MARKERS.get() || !ClientRules.nexusMarkers) return out;
        for (Marker m : MARKERS) if (!m.kind().equals("waystone")) out.add(m);
        return out;
    }

    public static List<Marker> in(ResourceLocation dim) {
        List<Marker> out = new ArrayList<>();
        for (Marker m : MARKERS) if (m.dim().equals(dim) && m.layerOn()) out.add(m);
        return out;
    }
}
