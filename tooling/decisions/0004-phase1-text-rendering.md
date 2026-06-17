# ADR 0004 — Phase 1 text rendering: vanilla textRenderer (temporary)

- **Status:** Accepted
- **Date:** 2026-06-17
- **Phase:** 1
- **Decider:** Orchestrator (user-confirmed)

## Context

§5.1 / §6.4 require Lumen to render with the custom typeface and **no Minecraft
fonts** — "the custom typeface is the keystone." But the MSDF font atlas is
explicitly a later deliverable (§12 Phase 2: "Mature Lumen … fonts"). Phase 1's
first Lumen `Screen` needs to draw text (titles, item counts) now.

## Decision

For Phase 1 only, Lumen draws **text glyphs** with Minecraft's vanilla
`textRenderer`, isolated behind a single Lumen text helper and marked
`TODO(phase2-msdf)`. Everything else stays 100% token-driven: colors, layout,
spacing, chrome, radii all resolve to `Tokens` (§5). No vanilla widget textures,
no chest chrome, no beveled buttons — only the glyph raster is temporary.

Confirmed by the user (2026-06-17): proceed in spec order; swap to the MSDF
atlas in Phase 2.

## Consequences

- The no-Minecraft-fonts identity rule is deliberately and temporarily bent for
  glyphs only, contained in one helper so Phase 2 swaps it in one place.
- Item icons rendered via `DrawContext.drawItem` are data (the server's item
  models), not "vanilla chrome" — allowed.
- Revisit and remove this when the MSDF atlas lands (Phase 2).
