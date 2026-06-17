/**
 * Codegen: design tokens -> apps/mod/.../generated/Tokens.kt
 *
 * Emits the Kotlin `Tokens` object that Lumen renders from, so the in-game UI
 * and the web UI share one source of truth and cannot drift (DESIGN_SPEC §5).
 * Run via the `tokens:gen` turbo task.
 */

import { mkdirSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { emitKotlin } from "../../packages/design-tokens/src/emit.js";

const PKG = "gg.orrery.generated";

const here = dirname(fileURLToPath(import.meta.url));
const repoRoot = join(here, "..", "..");
const outPath = join(
  repoRoot,
  "apps",
  "mod",
  "src",
  "main",
  "kotlin",
  ...PKG.split("."),
  "Tokens.kt",
);

mkdirSync(dirname(outPath), { recursive: true });
writeFileSync(outPath, emitKotlin(PKG), "utf8");

console.log(`tokens:gen → ${outPath}`);
