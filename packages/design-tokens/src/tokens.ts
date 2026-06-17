/**
 * Orrery design tokens — the single source of truth (DESIGN_SPEC §5).
 *
 * Authored once here. Consumed directly by the Svelte launcher and
 * code-generated into `Tokens.kt` for Lumen (the in-game UI toolkit).
 * Change a token, both surfaces change — they cannot drift.
 *
 * Concept: "brass instrument in the void" — deep cosmic void surfaces,
 * warm brass/gold primary accent, cold starlight cyan counterpoint.
 */

export const color = {
  // Surfaces — the void
  void: { base: "#0A0B12", s1: "#11131F", s2: "#181B2A", s3: "#212539" },
  hairline: { base: "#2E3350", bright: "#3D4468" },
  // Text
  text: { hi: "#ECEDF5", mid: "#A6A9BE", low: "#6A6E86" },
  // Accents
  brass: { ember: "#B5731C", base: "#E3A23C", hi: "#F7C765" },
  cyan: { base: "#4FC9DB", hi: "#7FE0EE" },
  // Semantic
  semantic: {
    success: "#5FD08A",
    warning: "#F2C14E",
    danger: "#F2585B",
    info: "#4FC9DB",
  },
  // SkyBlock rarity — game-data colors, kept separate from brand accents
  rarity: {
    common: "#C7CAD8",
    uncommon: "#5FD08A",
    rare: "#4F8FF0",
    epic: "#B061F0",
    legendary: "#F0A93E",
    mythic: "#F06FD0",
    divine: "#5BD6E0",
    special: "#F2585B",
  },
} as const;

/** Radius — small and precise; instruments aren't bubbly. */
export const radius = { sm: 4, md: 6, lg: 10, pill: 999 } as const;

/** Motion — mechanical but smooth, like orrery gears. Durations in ms. */
export const motion = {
  fast: 120,
  base: 180,
  slow: 240,
  easing: "cubic-bezier(0.2, 0.8, 0.2, 1)",
} as const;

/** Typography — the custom typeface is the keystone of the identity. */
export const typography = {
  family: {
    display: "Space Grotesk",
    body: "Inter",
    mono: "JetBrains Mono",
  },
  weight: {
    display: [500, 700],
    body: [400, 500],
    mono: [400, 500],
  },
  /** Type scale in px. */
  scale: {
    caption: 11,
    small: 12,
    bodySm: 13,
    body: 14,
    bodyLg: 16,
    h3: 20,
    h2: 26,
    h1: 34,
    display: 42,
  },
} as const;

export const tokens = { color, radius, motion, typography } as const;
export type Tokens = typeof tokens;
