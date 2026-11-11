# Codex: Cartography - Spec

Status: built (all phases), waiting for the first test on Tenko Test. Mod id `codex_cartography`, package `com.tac5studios.codexcartography`, folder `codex-cartography`.

A minimap and world map for NeoForge, with claims from Elements: Claims drawn right on the map. Client mod, with an optional server side.

Remnants (Apache 2.0) was looked at as a reference for what a modern map mod covers and how the work splits up (chunk scanning, region files, overview tiles, markers). None of its code, art, file formats or names are used. Everything here is written from scratch.

## Identity: not like the others

The mod must never look or feel like Xaero's Minimap / World Map, JourneyMap or FTB Chunks. Every screen, name, icon and default key is our own, in the Codex style.

- **Our own words:** waypoints are **Marks**, groups of marks are **Folios**, the mark list is the **Ledger**, the radar is the **Sweep**, cave mode is **Depth View**, map view modes are **Lenses**, the PNG export is a **Print**, the death mark is a **Death point**.
- **No borrowed layouts:** no grey button bars along the bottom of a screen, no plain scrolling list with Edit / Teleport / Delete buttons, no centered pop-up forms (the Xaero's waypoint screens). No table-style manager (JourneyMap).
- **Own art:** frames, emblems, player arrow, in-world mark glow, fonts for headers and the scan-line effect are all made for this mod.
- **Own default keys:** different from Xaero's and JourneyMap's defaults where they clash (all rebindable).

## One key

The whole mod runs from **one key** out of the box (rebindable; any action can also get its own key in settings, unbound by default).
- **Tap:** opens the world map. Every other page (Ledger, Folios, Print, Map data, Settings), the lenses and the layers are reached from its rails.
- **Hold:** opens the **in-game radial menu** without leaving play. Point at a slice and release to pick it: new mark here, minimap on/off, zoom in, zoom out, lens, Depth View, Ledger, settings. Each slice shows its glyph and name; the center shows the picked slice's name.
- The radial menu is our own screen drawn over the game (not Minecraft's buttons), so it follows the Codex style and scales with the GUI scale.
- Inside the world map, right-click opens a simple drop-down menu instead (see World map). The radial menu is only for the in-game key hold.

## Codex UI

Every screen (Ledger, mark editor, settings, map data, lenses) follows one design language:

- **Codex pages:** each screen is a "page" of the codex: dark slate background, thin Codex-green hairline borders, corner brackets instead of boxes, and a page header with the page name and a small glyph.
- **Index rail:** a narrow rail on the left edge with glyph tabs for each section. Hover shows the section name. No top tab bar.
- **Cards, not lists:** entries (marks, folios, map data per world) are cards in a grid: emblem, name, distance, dimension glyph. A selected card opens a **detail drawer** that slides in from the right edge with its fields and actions as glyph buttons (no bottom button bar).
- **Glyph buttons:** actions are small square glyphs with a label on hover (travel, share, edit, hide, remove). Destructive actions need a second click (the glyph turns red and reads "Confirm").
- **Hover labels:** every glyph shows its name on hover, with no exceptions: index rail tabs, dimension tabs, layer glyphs, action glyphs, the lens dial, search, keycap hints, emblems in the emblem grid, and marks, players and server markers on the map. Layer glyphs also show their state (e.g. "World border: on"), and a glyph that's turned off by the server says so (e.g. "Slime chunks: off on this server"). The label is a small dark tag with corner brackets, in the Language file so it can be translated.
- **Key hints:** shown as small "keycap" chips along the right side of the world map (on/off in settings).
- **Motion:** drawers slide, cards fade in, the scan line sweeps across a page once when it opens. All motion can be turned off in settings.
- **Accent color:** Codex green by default, changeable (presets plus hex).

## Versions

- First build: NeoForge 1.21.1, Java 21.
- Then NeoForge 26.1, 26.2 and 26.3 (releases only, no snapshots), alongside the rest of the 26.x port. 26.4 is watched.
- Same mod id, config and save format on every version, so maps and marks carry over when a server moves up a version.

## Sides

- **Client:** everything a player sees. Works on its own in single player and on any server, including servers without the mod.
- **Server (optional):** server rules, claims, claim marks, and Waystones / Nexus markers.
- Vanilla clients can join a server that has it. Nothing on the server requires the client mod.

## Map data

### Scanning
- Chunks are read on the client as they load, never on the main render path. A scan queue spreads the work over frames with a time budget (default 2 ms per frame), so low-end PCs don't stutter.
- Each block column stores: surface height, surface block color, water depth (for shading water), biome, and a depth layer when Depth View is on.
- Block colors come from the game's own map colors first, with a small built-in table for blocks that look wrong (leaves, glass, snow layers, flowers). Modded blocks work with no setup.
- A chunk is rescanned when a block in it changes (dirty areas only), not every frame.

### Saving
- Saved per server address (or world name in single player) and per dimension, under `codex_cartography/worlds/<world>/<dimension>/` in the game folder.
- Our own region file format: one file per 512 x 512 block area, compressed, with a version number in the header so later versions can upgrade old files.
- Written off the main thread, in batches. Nothing is lost if the game closes mid-write (write to a temp file, then swap).
- Several worlds on one server address (lobby + survival, resource worlds) are kept apart by dimension id. A manual "this is a different world" switch covers servers that reuse dimension ids.

### Map data page
- Shows only the map of the server (or singleplayer world) the player is on. Other servers and worlds are never listed or shown on this page.
- Has its own glyph on the index rail.
- Shows: a preview of the explored map, the server address or world name, first mapped and last visited dates, a name the player can set for it, and counts (regions, chunks, marks, folios).
- Each dimension with its size on disk and its own delete. Deleting a dimension clears its explored map only; marks and folios stay.
- "Delete this server's whole map" (explored map, marks and folios).
- **Deletion warning:** the first click on any delete never deletes. It opens a red warning box that says what will be removed, that it's only the copy saved on this computer, and that it can't be undone. The player confirms by clicking the red delete again, or cancels with Esc or the Cancel link.

### Look (Codex style)
- A dark slate frame with Codex-green accents and edge glow, and a thin green scan line that sweeps the minimap now and then (toggle).
- Terrain is drawn from the stored block colors, then toned to a cooler, slightly muted palette with soft height shading and darker, crisp water edges, so the map reads cleanly against the dark frame.
- Unexplored areas fade into a dark grid (a faint green "code" texture), not plain black.
- Frame, accent color and palette are set in settings; a plain vanilla map-colors palette stays available.

### Lenses (map views)
Switched from the lens dial on the world map, or the lens slice of the radial menu.
- **Surface:** the normal view.
- **Relief:** topographic: height bands with contour lines in the accent color.
- **Biome:** each biome as a flat color, with its name on hover. Modded biomes get a color picked from their own grass/foliage colors.
- **Night:** the map darkened with block light shown as warm glow (torches, lava, glowstone), handy for spotting lit and unlit areas.
- **Depth View:** the layer around the player when underground, at any height: solid ground above the head (leaves and water don't count) and little daylight. It switches after a second so it doesn't flicker at cave mouths. Automatic, or from the radial menu. The Nether always uses it below the roof.

### Zoomed-out view
- Far zoom levels use pre-built smaller tiles (built in the background as regions fill in), so the whole explored world opens fast.

## Minimap

- Square, round, or a clipped-corner "codex plate" shape; size and position set in the layout editor (drag it on screen).
- **Default position:** top-left, so it never sits under the Elements: Nexus side panel (right side), boss bars (top center) or effect icons (top right). The layout editor shows those areas so players don't drag the minimap over them.
- North up or rotating with the player.
- Zoom from the radial menu; smooth zoom.
- Shows: player arrow, other players, marks (pinned to the edge when off the map), claims, chunk grid (toggle), world border (toggle), coordinates, biome, time, and light level under the player (toggle).
- Hides on F1, when the debug screen is open, and while a full screen is open (settings).

## World map

- Opens with a tap of the key. Drag to pan, scroll to zoom toward the cursor, a glyph on the rail to return to the player.
- Dimension switcher on the index rail, with readable names (see Dimension names).
- Coordinates and biome under the cursor, in a readout strip along the bottom edge (not a box).
- Right click opens a **drop-down menu** at the cursor, in the Codex style (dark panel, corner brackets, glyph + name per row). A header row shows the chunk and coordinates under the cursor. Rows: new mark here, copy coordinates, travel here (only when allowed), then claim actions (when Claims is on the server: claim this chunk with the chunks used / allowed, unclaim). A row that can't be used is greyed out with the reason on hover.
- **Area select:** right-click and drag (left drag still pans) to select a block of chunks; the selection snaps to chunk edges and shows its size (e.g. "3 × 3 · 9 chunks") as you drag. Releasing opens the same drop-down for the whole area: claim these chunks (with the count and chunks left; greyed out with the reason if it's more than you have left or outside claim range, since Claims claims all or nothing), unclaim your chunks in this area, print this area, copy corner coordinates. Esc or a click elsewhere clears the selection.
- Layer glyphs on the right rail: claims, marks, players, chunk grid, slime chunks (when allowed), world border, Waystones, homes and warps.
- Search: marks, folios, claims and waystones by name.

## Marks (waypoints)

### Creating and editing
- Place a mark from the world map's right-click menu, or at the player from the in-game radial menu ("new mark here").
- The mark editor is the **detail drawer** on the right of the world map (the map stays visible and live behind it), not a separate screen. It shows: name, an **emblem grid** with 14 emblems of our own: keep, home, village, farm, mine, treasure, shrine, ruin, dungeon (skull), gate, meeting point (flag), favorite (star), explore later (question mark) and danger. The portal, Ancient City and Ocean Monument emblems aren't in the grid (see Discovered portals and Discovered structures), color (see Color wheel), folio, dimension, and a live preview of how the mark looks in the world.
- Coordinates can be typed, or set by clicking the map while the drawer is open.
- Default badge color for a new mark is Codex green, except the dungeon (skull) emblem, which starts yellow. The player can change any of them.

### The Ledger
- The mark list is the Ledger page: marks as cards in a grid, grouped by folio, filtered by dimension glyphs, sorted by distance, name or date.
- A card's drawer has the actions as glyphs: travel (when allowed), share, edit, hide, remove.

### In the world
- Marks show in the world as a **floating emblem**: the mark's badge, always facing the player, with a soft glow in the mark's color around it and a short fade of light under it. The name sits above and the distance below. No beam into the sky.
- On/off and max distance in settings; the emblem shrinks a little with distance but never below a readable size.

### Nether and Overworld
- Overworld marks show in the Nether at their Nether position (÷8), and Nether marks in the Overworld (×8), as hollow emblems so it's clear they're converted. Toggle in settings.

### Discovered portals
- Portals get the portal emblem automatically, but only once the player has discovered them: the portal shows on the map after the player's crosshair has hit a portal block (Nether portal, End portal, End gateway, and portal blocks from other mods by block tag). Being near it, or the chunk being scanned, is never enough.
- Until then the portal is drawn as plain terrain, so the map never gives away a portal the player hasn't seen.
- One marker per portal (the whole portal frame counts as one), placed at its center. If the portal is broken, its marker is removed the next time the player is near it and the chunk is rescanned.
- Discovered portals are saved per player, per world, with the map data. They can be hidden or recolored like any marker (color wheel), but not placed by hand: the portal emblem is only used for discovered portals.

### Discovered structures
- Every structure gets a discovered mark once the player has discovered it: when the player's crosshair hits a block that belongs to it (villages, ruined portals, outposts, temples, shipwrecks, Ancient Cities, Ocean Monuments, modded structures). Walking past or near it is never enough. The emblem comes from the structure's id (village, portal, flag, keep, shrine, ...; unknown ones get "explore later"). The server can skip structures in `[discovered] skip` (mineshafts by default).
- Until then nothing about them shows on the map beyond plain terrain.
- How a block is confirmed:
  - **With Cartography's server side:** when the crosshair lands on a block that can be part of these structures (deepslate bricks/tiles, sculk blocks, reinforced deepslate for Ancient Cities; prismarine, prismarine bricks, dark prismarine, sea lanterns for Ocean Monuments), the client asks the server whether that block is part of the structure. The server checks the world's structure data and answers yes or no. A player-built prismarine wall never counts.
  - **Without the server side:** only blocks that are never player-placed in survival count: reinforced deepslate for Ancient Cities. Ocean Monuments have no such block, so they only appear on servers with the server side.
- Requests are limited (one check per block, a few per second), so looking around doesn't spam the server.
- Default colors: Ancient City purple, Ocean Monument teal, discovered portals red.
- One marker per structure, at its center. Saved per player, per world. They can be hidden or recolored (color wheel), but not placed by hand.
- Server rule `discovered_structures` (on by default) to turn them off.

### Death point
- A Death point is placed where the player dies (setting); the last few are kept (count setting), older ones removed. Walking back to the spot (within 2 blocks) clears it (setting, on by default).
- Death points always use one fixed look: a black diamond with a red X. They can't be recolored, so they're never mistaken for a normal mark.

### Folios
- Folios are folders for marks: each mark can sit in one folio (e.g. "Base", "Mining", "Travel"). Turn a whole folio on or off to show or hide all its marks at once, on the map and in the world.
- Each folio has a color and one choice, worded in plain terms on the folio's drawer: **"What the folio color changes"**: "Just the folio (marks keep their own colors)" (default) or "Every mark in this folio" (all its marks show in the folio's color on the map and in the world; their own colors come back if switched off or the mark leaves the folio). Death points and danger marks never change color.
- The Folios page opens with a short description of what folios are, and the folio picker in the mark editor has a hover label saying the same, so new players aren't left guessing.
- Each folio has a color (picked with the Color wheel). In the folio's drawer, **"Paint the marks too?"** sets what that color does: **No, keep each mark's color** (the color only shows on the folio's card and tag) or **Yes, paint every mark** in the folio that color, on the map and in the world. Default: No.

### Sharing
- Share a mark in chat as a clickable line other players with the mod can add to their Ledger.

### Import
- Import of waypoints from Xaero's Minimap (xaero/minimap/, or XaeroWaypoints/ in older versions) and JourneyMap (journeymap/data/) files on the player's computer. The Import button on the Folios page lists every waypoint folder found, the current world's first; the player picks one. They come in as marks in a folio named after the source. Nothing else from those mods is read.

## Color wheel

One Codex color picker, used everywhere a player picks a color: every kind of marker on the map except Death points and the danger mark (their own marks, claim marks, Waystones markers, Nexus homes and warps), folios, their own claim colors (yours / trusted / others / server), and the accent color.
- Every emblem works with any color: the badge and glow take the picked color, the emblem itself stays white so it reads on any color.
- The danger mark has no badge: it's a yellow warning triangle with a dark exclamation mark, always yellow.
- Marker kinds the player didn't make (Waystones, homes, warps) get a default color per kind, which the player can change for that whole kind; claim marks use the color the claim owner picked, and players can override it for themselves.
- A **hue ring** around a **shade square** (saturation left to right, brightness top to bottom). Drag on either.
- A row of 16 quick colors underneath, plus the player's last 8 picked colors.
- Hex field for exact colors (`#39FF8C`).
- Live preview: the mark badge (or claim outline and tint) in the picked color, on the map's dark background, so players see how it reads before saving.
- Colors too dark to read on the map (very low brightness) get a warning line under the preview; they're still allowed.



Toggles for each group:
- Players
- Hostile mobs
- Passive mobs
- Pets and tamed animals (own pets highlighted)
- Boats and minecarts
- Items on the ground

Shown as small glyphs, or as each mob's own head with a colored rim (hostile, passive, pet). The Mob icons setting (Minimap section) picks dots, heads, or heads only while the mob heads key is held (Tab by default). The Sweep can also pulse outward from the player arrow now and then (toggle), in the accent color. An option limits the Sweep to mobs near the player's own height (5 blocks up and down by default).

## Print (export)

- Export the explored map of a dimension, or the area currently on screen, as a PNG.
- Choose the lens, scale (1 pixel per block or smaller), and whether claims, marks and the grid are drawn in.
- Saved to `codex_cartography/prints/` with the world, dimension and date in the file name. Big prints are made in the background with a progress bar.

## Dimension names

- Modded dimensions show a readable name made from their id (`tenko:spawn` shows as "Spawn"), not the raw id.
- Players can rename any dimension for themselves; servers can send names for their own dimensions (server rules).

## World border

- Every dimension's world border is shown on the minimap and world map: a solid line in the border color with the area outside it shaded.
- Works for borders set per dimension (each dimension can have its own size and center), not only the Overworld's.
- A moving border (growing or shrinking) is drawn live, with its target size shown as a faint dashed line.
- Distance to the border shows on the readout strip and as a minimap info line (toggle) when the player is close (distance set in settings).
- With the server side, the server sends the border of every dimension, so the world map shows the right border when viewing a dimension the player isn't in. Without it, the map uses the border the game already sends for the player's current dimension and remembers the last border seen in each dimension.
- Own layer glyph (on/off) on the world map and a minimap toggle.

## Claims (Elements: Claims)

How claims reach the map:
- Claims gets a small addition: an **optional** network channel that sends claim outlines to players who have the map mod. Players without the map mod never get it, and Claims stays server-only.
- The server sends claims around each player (configurable range, default 16 chunks), then only changes after that (claimed, unclaimed, renamed, trust changed). Nothing is sent every tick.
- Sent per claim: outline (chunks), property name, owner name, the player's own standing (owner / trusted rank / not trusted), admin claim yes/no, and the claim's outline color from Claims.
- Sub-areas are sent as boxes inside their claim, with their name.
- The player's own chunk count and allowance (from `claims.limit`) and property count, so the map can show what's left. Claim and unclaim requests from the map go back over the same channel and run through the same checks as the Claim Deed.

How they're drawn:
- Filled with a light tint and outlined, on both the minimap and the world map. Borders use a dashed "code" line, our own style.
- Colors by standing: your claims, claims you're trusted in, other players' claims, admin/server claims. Each one is picked with the Color wheel; "use the claim's own color" is an option; a colorblind-safe preset is included.
- Hover on the world map: property name, owner, your rank there.

Claim actions from the world map (first version):
- Only for the player's own land, and only what Claims allows them: select chunks to claim or unclaim, open the claim's Land Config menu.
- Claims does every check, the map only asks: the player's chunk allowance per rank (`claims.limit`) and property limit (`claims.properties`) always apply, the same as with the Claim Deed. A selection bigger than the chunks the player has left is refused with the number they can still claim.
- While claim selection is on, the world map shows "Chunks: used / allowance" and greys out chunks that can't be claimed (someone else's land, admin claims).

### Claim marks
- Shared marks for a claim's trusted players, e.g. a "Base" mark every member sees.
- Made by the owner (or a rank Claims allows to manage the claim); stored by Claims with the claim, sent with the claim data.
- Shown in the Ledger in a "Claims" folio, one card per claim mark, marked with the claim's glyph. Members can hide them for themselves but not edit them.
- When a player loses trust in a claim, its claim marks disappear for them.

Other claim mods aren't drawn. The claim channel is documented, so other server mods could send outlines in the same format later.

## Other server mods

Read on the server side (only when the map's server side and that mod are installed) and sent to players with the map:
- **Waystones:** waystones the player has activated (plus global ones) as map markers with their name. Our own waystone emblem.
- **Elements: Nexus:** the player's homes and the server's warps as markers (only what the player can use). Shown on their own layer glyph, and listed read-only in the Ledger under "Homes and warps (from the server)" (distance, coordinates, travel; changed only with the Nexus commands).

## Server rules

Two ways to set them:

### With the server side (full control)
`config/codex_cartography/server.toml`, reloads with `/cartography reload`. Sent to players when they join and when the file reloads. A player's own settings can only turn things off, never back on.

- `sweep_players`, `sweep_mobs`, `sweep_pets`, `sweep_vehicles`, `sweep_items` (on/off)
- `depth_view` (on/off, and allowed dimensions)
- `slime_chunks` (off by default)
- `travel` (off by default; when on, needs `cartography.travel` permission)
- `death_points` (on/off)
- `discovered_structures` (on/off)
- `mark_sharing` (on/off)
- `claims` (on/off), `claims_range`, `claim_marks` (on/off)
- `waystones`, `nexus_markers` (on/off)
- `world_border` (on/off)
- `dimension_names`: `"namespace:path=Name"` lines
- Per-dimension overrides (e.g. no Sweep in an arena world).

### Server switches
A server with the mod controls what players get from its own files: a feature switched off in the server's `features.toml` (the master control file) is off for every player who joins, and `server.toml` sets the finer rules. There is no MOTD tag; servers without the mod don't restrict anything.

## Settings (client)

- `config/codex_cartography/client.toml` for every look and feel option, edited in-game on the Settings page.
- **Settings page layout:** the index rail holds the sections (Minimap, World Map, Marks, Sweep, Lenses, Claims, Colors, Performance, Keys). Each setting is a row with a toggle chip or slider and, where it helps, a small live preview of what it changes (e.g. the minimap shape preview updates as you pick). A search field at the top finds any setting.
- Minimap: shape, size, position (layout editor), rotation, zoom, opacity, which info lines show.
- World border: show on minimap, show on world map, distance warning (blocks), border color.
- Colors: accent color, claim colors (incl. colorblind preset), in-world mark size, map palette (Codex / vanilla). Every color is picked with the Color wheel.
- Motion: page animations, scan line, Sweep pulse (each on/off).
- Performance: scan budget per frame, rescan delay, max region files kept in memory, overview tile building on/off.
- Keys: the one key (tap/hold), plus optional keys for any single action (unbound by default).

## Language

- All client text in a language file (`en_us.json`), so the mod can be translated. Server messages are in `messages.toml` (below), so server owners can reword them.

## Server files

All in `config/codex_cartography/`:
- `features.toml`: one switch per feature (see Master control file). Restart.
- `server.toml`: server rules, dimension names. Reloads.
- `messages.toml`: every server message (claim action results, travel denied, rules updated), with `&` colors. Reloads.

`/cartography reload` re-reads `server.toml` and `messages.toml` and resends rules, dimension names, claims, claim marks and markers to every player. Only `features.toml` needs a restart.

## Commands

| Command | Permission | Default |
| --- | --- | --- |
| `/cartography reload` (server) | `cartography.reload` | OP 3 |
| `/cartography travel <x> <z>` (used by the map's travel action) | `cartography.travel` | OP 2 |

Permissions (NeoForge permission API, registered at start whatever the switches): `cartography.travel` (OP 2), `cartography.reload` (OP 3), `cartography.bypass` (staff ignore Sweep / Depth View limits, OP 2).

## Master control file

`config/codex_cartography/features.toml` (server and client each read their own). One switch per feature: minimap, world map, marks, Death points, in-world marks, Sweep, Depth View, lenses, print, claims layer, claim marks, Waystones markers, Nexus markers, waypoint import. On a server, a switch turned off also turns the feature off for every player who joins. Needs a restart. Everything else reloads.

Overlap guard (client): when Xaero's Minimap, Xaero's World Map, JourneyMap, FTB Chunks' map or another detected minimap is installed, the minimap and the key start off and the player gets a one-time message saying which map was found. `force_on` in features.toml keeps ours on. On a server with the mod, the server's `force_on` is sent to every player and decides it; the player's own file only counts on servers without the mod and in single player.

## Shaders

- The minimap, world map and pages draw in the GUI pass, so shaders don't touch them.
- Floating marks, Death points and discovered places are drawn on the HUD at their spot on screen, not in the world, so shader packs can't change or hide them.
- Shader support is a given: no feature may depend on the world render pass looking a certain way.

## Performance targets

- Under 2 ms per frame of map work at the default budget.
- Minimap draws from cached textures; it never re-reads chunks to draw.
- Memory capped (setting); old regions dropped from memory first.
- Prints and overview tiles built off the main thread.
- Tested on a low-end PC setting before release (render distance 6-8).

## Build phases

Built and tested one phase at a time on Tenko Test; each phase works on its own before the next starts. Screen layouts follow the mockups in `art/ui/`.

1. **Core:** mod setup (`codex_cartography`, NeoForge 1.21.1), `features.toml`, `client.toml`, the one key (tap / hold), block scanning and region saving per server or world and per dimension.
2. **Minimap:** tile drawing, the three frames (plate, square, round), player arrow and other players, info lines, layout editor, hide rules, scan line.
3. **World map:** pan and zoom, index rail and dimension tabs, layer rail, readout strip, fog for unexplored areas, overview tiles for far zoom, lenses, keycap hints, hover labels on every glyph, right-click drop-down and area select.
4. **Marks:** Ledger, mark editor with emblem grid and color wheel, folios (with the folio color choice), in-world floating emblem, Death points, danger mark, discovered portals, sharing in chat, waypoint import.
5. **Pages:** in-game radial menu, Settings page, Map data page with the deletion warning, Print.
6. **Server side:** `server.toml` rules and `/cartography reload`, server features.toml switches applied to players, world border for every dimension, discovered structures, Sweep, Depth View, slime chunks, travel, Waystones and Elements: Nexus markers, overlap guard.
7. **Claims:** the map channel in Elements: Claims (Claims spec section 14), claims layer and colors, claims panel, claim and unclaim (single chunk and area), claim marks.
8. **Finish:** language file, performance pass against the targets above, full test on Tenko Test (with and without the server side, with and without Claims), README, LICENSE, changelog, CurseForge description.

## Not in the first version

- Web map (BlueMap etc. already cover this).
- Server-side map rendering or sharing explored map between players.
- Paper version (no client mods on Paper).

## Open questions for Tempest

None.

Decided (11 Nov 2026): name Codex: Cartography; styled Codex look; claim and unclaim from the world map are in the first version, with the per-rank chunk limits respected; all gap items added; every screen, name and icon distinctly Codex, nothing resembling Xaero's, JourneyMap or FTB Chunks.
