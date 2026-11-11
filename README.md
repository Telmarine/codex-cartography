# Codex: Cartography

A minimap and world map for NeoForge 1.21.1, in the Codex style. Claims from Elements: Claims are drawn right on the map, and you can claim land from it.

Client mod with an optional server side. It works on its own in single player and on any server, including servers that don't have it. Vanilla clients can join a server that has it.

## The one key

Everything runs from one key (default **G**, rebindable):

- **Tap** opens the world map. Every other page (Ledger, Folios, Print, Map Data, Settings) is on its left rail.
- **Hold** opens the quick menu in game. Point at a slice and let go: new mark here, Ledger, zoom in, zoom out, lens, Depth View, minimap on/off, settings.

Each quick-menu action can also get its own key in Controls (unbound by default).

## Features

### Minimap
- Three frames: Codex plate (default), square and round. Size and position are set in the layout editor (drag it anywhere).
- North up, or turning with the player. Zoom, opacity and info lines (coordinates, biome, time, light level, world border distance).
- Starts top-left, so it stays clear of side panels, boss bars and effect icons.
- Hides with F1, the debug screen and full menus.

### World map
- Drag to pan, scroll to zoom toward the cursor.
- Layers on the right rail: claims, marks, players, chunk grid, slime chunks, world border, Waystones, homes and warps.
- Right-click opens a menu for the spot: new mark here, copy coordinates, travel here (when the server allows it), claim or unclaim the chunk.
- Right-click and drag selects an area of chunks: claim them, unclaim yours, print the area, copy the corners.
- Every glyph shows its name on hover.

### Marks, the Ledger and Folios
- Marks (waypoints) with 14 emblems and a color wheel (hue ring, shade square, quick colors, your last picks, hex field).
- Shown in the world as a floating emblem with a soft glow, the name above and the distance below.
- The Ledger lists every mark as a card, with filters, sorting and a detail drawer: travel, share in chat, edit, hide, remove.
- Folios are folders for marks. Turn a whole folio on or off. A folio's color can stay on the folio or color every mark in it.
- Death points (cleared when you walk back to them), and discovered places: portals once your crosshair hits one, and structures (villages, ruined portals, temples, modded ones) once your crosshair hits one of their blocks.
- Import of waypoints Xaero's Minimap or JourneyMap saved on your computer (Folios page, Import: pick which world's waypoints).

### Map views
- Lenses: Surface, Relief (height bands with contour lines), Biome and Night (block light as a warm glow).
- Depth View: the layer around you when underground, at any height (automatic, or from the quick menu).
- The Sweep: players, hostile and passive mobs, pets, boats and minecarts, and items on the ground. Mobs show as dots, as their own heads, or as heads only while a key is held (Tab by default).
- World borders of every dimension, including a border that's moving.
- Print: save the explored map (or an area) as a PNG.

### Map Data
- Shows the map saved on your computer for the server you're on, and only that one. Rename it, see its size, delete one dimension or the whole map. Every delete asks to confirm.

## Server side (optional)

Install the same jar on the server to control what players can use and to send them more:

- Server rules: Sweep groups, Depth View, slime chunks, travel, Death points, discovered structures, mark sharing, world border, claims, markers. A player's own settings can only turn things off.
- Readable dimension names, and per-dimension overrides (for example no Sweep in an arena world).
- World borders of every dimension, Waystones the player has activated, and Elements: Nexus homes and warps (also listed in the Ledger).
- Discovered Ancient Cities and Ocean Monuments are confirmed by the server.

## Elements: Claims

With Elements: Claims on the server, claims show on the map as outlines in your colors (yours, trusted, others, server), with the name, owner and your rank on hover. Click a claim for its panel: your chunks and properties against your rank's limits, sub-areas, claim marks, Land Config.

Claiming from the map goes through Claims' own checks, so the chunk limits per rank always apply. Claim marks are shared marks for a claim's trusted players.

## Install

1. Put the jar in your `mods` folder (client). Optional: put the same jar on the server.
2. Start the game. The map starts recording as you explore.

## Commands

| Command | Permission | Default |
| --- | --- | --- |
| `/cartography reload` | `cartography.reload` | OP 3 |
| `/cartography travel <x> <z>` (used by the map's travel action) | `cartography.travel` | OP 2 |

`cartography.bypass` (OP 2) lets staff ignore the Sweep and Depth View limits.

## Config files

All in `config/codex_cartography/`:

- `features.toml`: one switch per feature, plus the overlap guard. Needs a restart.
- `client.toml`: every look and feel option (also edited in game on the Settings page).
- `server.toml`: server rules and dimension names (server). Reloads with `/cartography reload`.
- `messages.toml`: server messages, with `&` colors (server). Reloads with `/cartography reload`.

Maps are saved in `codex_cartography/worlds/` in the game folder, one folder per server or world. Prints go to `codex_cartography/prints/`.

When another minimap or world map mod is installed, the minimap and the Codex key start off and you get a one-time message. Add `"minimap"` and `"key"` to `force_on` in `features.toml` to keep them on. On a server with the mod, the server's `force_on` decides this for every player; your own only counts elsewhere.

## License

Free to use and include in modpacks, credit Telmarine, no resale. See `LICENSE`.
