package com.tac5studios.codexcartography.client;

import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * What the server allows. Filled from the server side when it has the mod
 * (its features.toml switches and server.toml rules); otherwise everything is allowed except slime chunks and travel. A player's own settings can only turn things off.
 */
public final class ClientRules {

    public static boolean fromServer;
    public static boolean sweepPlayers = true;
    public static boolean sweepMobs = true;
    public static boolean sweepPets = true;
    public static boolean sweepVehicles = true;
    public static boolean sweepItems = true;
    public static boolean depthView = true;
    public static boolean slimeChunks;
    public static boolean travel;
    public static boolean deathPoints = true;
    public static boolean discoveredStructures = true;
    public static boolean markSharing = true;
    public static boolean claims = true;
    public static boolean claimMarks = true;
    public static boolean waystones = true;
    public static boolean nexusMarkers = true;
    public static boolean worldBorder = true;
    public static boolean canTravel;
    /** Dimensions where the Sweep and Depth View are off (per-dimension overrides). */
    public static final Set<ResourceLocation> noSweep = new HashSet<>();
    public static final Set<ResourceLocation> noDepth = new HashSet<>();
    public static final Map<ResourceLocation, String> dimensionNames = new HashMap<>();
    /** Overlap guard force_on from the server's features.toml (used instead of the player's own while on that server). */
    public static final Set<String> forceOn = new HashSet<>();

    private ClientRules() {}

    /** Back to the defaults (leaving a server). */
    public static void reset() {
        fromServer = false;
        sweepPlayers = sweepMobs = sweepPets = sweepVehicles = sweepItems = true;
        depthView = deathPoints = discoveredStructures = markSharing = claims = claimMarks = true;
        waystones = nexusMarkers = worldBorder = true;
        slimeChunks = travel = canTravel = false;
        noSweep.clear();
        noDepth.clear();
        dimensionNames.clear();
        forceOn.clear();
    }
}
