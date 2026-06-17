/**
 * @orrery/design-tokens — the single source of truth for Orrery's visual
 * identity (DESIGN_SPEC §5). Import the token objects directly in TS/Svelte;
 * build artifacts (CSS vars, Tailwind preset, Kotlin) are generated from these.
 */

export { color, radius, motion, typography, tokens } from "./tokens.js";
export type { Tokens } from "./tokens.js";

export { colorEntries, emitCss, emitJson, emitKotlin } from "./emit.js";
