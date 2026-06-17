# ADR 0005 — Phase 2: semantic menus, custom fonts, restrained color

- **Status:** Accepted
- **Date:** 2026-06-17
- **Phase:** 2 (Mature Lumen + coverage)
- **Decider:** Orchestrator (user delegated)

## Context

Phase 1 proved interception/render/route with a literal slot grid. The user's
direction (mockup) is the §5.1 thesis realized: menus re-rendered as **semantic
UI** (search, header purse, labelled entry cards, footer progress), not reskinned
chest grids. This ADR fixes the Phase 2 decisions + shared interfaces.

## Decisions

### 1. Custom menus are semantic views

A recognized menu gets a per-menu **view** that lays out parsed entries however
the design says; the generic slot grid (`OrreryContainerScreen`) remains the
fallback for owned-but-unmodelled containers. First view: `SkyBlockMenuView`.

### 2. Color: §5 restraint (not per-category rainbow)

Adopt the mockup's layout/IA exactly. Color stays brass/cyan-restrained:
- **Key numerals → `brass.hi`** (§5.2 lists this use); interactive accents → cyan.
- Entry **icons render the actual backing `ItemStack`** (`DrawContext.drawItem`)
  on a subtle `surface.2` tile — data-faithful, zero new icon art, no rainbow.
- Rarity colors remain game-data only (a thin rarity tick on a card is allowed).
A controlled category-accent token set may be added later if desired.

### 3. Fonts: native MC TTF providers (not MSDF)

Ship Space Grotesk / Inter / JetBrains Mono as Minecraft **TTF font providers**
under `assets/orrery/font/`, exposed as font Identifiers. Lumen text renders via
the vanilla `TextRenderer` **with our font Identifier**, so "no Minecraft pixel
font" (§5.1) is satisfied without a custom MSDF GL shader. Supersedes the §5.3
MSDF assumption and ADR 0004 (the temporary vanilla-font use). True MSDF stays a
later option only if arbitrary-scale crispness demands it.

## Shared interfaces (workers build to these)

### Atlas (core, MC-free) — entry parsing

```kotlin
data class MenuEntry(
    val backingSlot: Int,   // index into ScreenHandler.slots (for icon + click routing)
    val title: String,      // primary label, § codes stripped
    val subtitle: String?,  // first descriptive lore line (not the rarity line), or null
    val rarity: Rarity,
)

// Filters filler/empty slots (glass panes, unnamed); keeps real entries.
fun parseEntries(menu: ParsedMenu): List<MenuEntry>
```

The view fetches the icon `ItemStack` from `handler.slots[backingSlot]` (Atlas
core stays MC-free).

### Lumen — font API

```kotlin
object LumenFonts {
    val DISPLAY: Identifier  // orrery:display  (Space Grotesk)
    val BODY: Identifier     // orrery:body     (Inter)
    val MONO: Identifier     // orrery:mono     (JetBrains Mono)
}
// LumenDraw.text(...) renders via TextRenderer using one of these fonts + a Tokens color.
```

## Verification (no Hypixel; user runs PrismLauncher/runClient)

Gate = compiles vs 1.21.11 Yarn + §11 green + Atlas entry tests + `/orrery
preview` renders. `DevPreview` mock is updated to resemble the real SkyBlock
menu (named entries + lore subtitles + purse) so the view shows well offline.
