/**
 * Build the launcher-facing token artifacts into `dist/`:
 *   - tokens.css      Tailwind v4 `@theme` stylesheet (utilities + :root vars)
 *   - tokens.json     full token snapshot
 *
 * The Kotlin codegen for the mod lives separately in
 * tooling/scripts/gen-tokens-kotlin.ts (turbo task `tokens:gen`).
 */

import { mkdirSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { emitCss, emitJson } from "../src/emit.js";

const here = dirname(fileURLToPath(import.meta.url));
const distDir = join(here, "..", "dist");

mkdirSync(distDir, { recursive: true });

const artifacts: Array<[string, string]> = [
  ["tokens.css", emitCss()],
  ["tokens.json", emitJson()],
];

for (const [name, contents] of artifacts) {
  const path = join(distDir, name);
  writeFileSync(path, contents, "utf8");
  console.log(`  ✓ ${name} (${contents.length} bytes)`);
}

console.log(`design-tokens: emitted ${artifacts.length} artifacts to dist/`);
