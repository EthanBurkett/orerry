# DESIGN

Single source of truth for all design values is **`packages/design-tokens`**
(DESIGN_SPEC §5), emitted to a Tailwind v4 `@theme` stylesheet
(`@orrery/design-tokens/css`) and to `Tokens.kt` for the in-game UI. The launcher
consumes the CSS custom properties. **No raw hex / font-family / radius literals
in components** — every value resolves to a `var(--token)`. If a value is missing,
add it to the token source first.

## Color (CSS vars)

- Surfaces (the void): `--color-void-base` (canvas) · `--color-void-s1` (panels,
  sidebar, chrome) · `--color-void-s2` (insets, hover, cards) · `--color-void-s3`
  (raised/hover).
- Hairlines: `--color-hairline-base` (borders, dividers) · `--color-hairline-bright`
  (emphasized borders, focus tracks).
- Text: `--color-text-hi` · `--color-text-mid` · `--color-text-low`.
- Accents: `--color-brass-ember` (deep/pressed) · `--color-brass-base` (PRIMARY:
  buttons, active state, focus, key numerals) · `--color-brass-hi` (highlights,
  glow, the wordmark "O"). `--color-cyan-base` (SECONDARY: links, ghost outlines,
  info) · `--color-cyan-hi`.
- Ink on accent: `--color-ink-on-brass` (dark text on brass fills — never pure
  black).
- Semantic: `--color-semantic-success | warning | danger | info` (status dots).
- Rarity: `--color-rarity-*` exists for in-game SkyBlock items only. **Never use
  rarity colors as launcher UI decoration / swatches.**

## Type

- `--font-display` Space Grotesk: wordmark, headings, key UI, button labels.
- `--font-body` Inter: prose, labels, most UI text.
- `--font-mono` JetBrains Mono: versions, counts, stats, prices (tabular numerals).
- Scale: `--text-caption|small|bodySm|body|bodyLg|h3|h2|h1|display`.

## Form & motion

- Radius: `--radius-sm|md|lg|pill` (small and precise). Circles use `50%`.
- Motion: `--duration-fast` 120ms · `--duration-base` 180 · `--duration-slow` 240;
  easing `--ease-instrument` (cubic-bezier(0.2,0.8,0.2,1), snappy ease-out). No
  bounce/elastic. Animate transform/opacity, not layout. Respect
  `prefers-reduced-motion`.
- Spacing: 4 / 8 / 12 / 16 / 20 / 24 / 32 / 40 px rhythm (vary for hierarchy).

## Signature motif

Concentric orbital rings rotating at different rates (`OrbitalMotif`): ambient
background art (large, faint) and the loading spinner (`Spinner`). Hairline tick
marks and instrument-readout corners are the supporting texture.

## Component conventions

- Buttons: `primary` (brass fill, ink-on-brass text), `ghost` (cyan outline,
  transparent), `icon` (bare). Every interactive element has hover / focus-visible
  (brass ring) / active / disabled states.
- Active nav = brass LEFT-ACCENT BAR + brass icon/label + subtle `void-s1/s2` fill.
  Never a boxed outline; never a `border-left` stripe on cards (motif only).
- Empty states teach, not "nothing here." Tints via `color-mix()` on tokens.
