import { test } from "node:test";
import assert from "node:assert/strict";
import { color } from "./tokens.js";
import { colorEntries, emitCss, emitJson, emitKotlin } from "./emit.js";

test("colorEntries flattens nested groups to dash-joined names", () => {
  const map = new Map(colorEntries());
  assert.equal(map.get("void-base"), "#0A0B12");
  assert.equal(map.get("brass-base"), "#E3A23C");
  assert.equal(map.get("rarity-epic"), "#B061F0");
  assert.equal(map.get("text-hi"), "#ECEDF5");
});

test("emitCss is a Tailwind v4 @theme exposing every color as --color-*", () => {
  const css = emitCss();
  assert.match(css, /@theme \{/);
  for (const [name, hex] of colorEntries()) {
    assert.ok(
      css.includes(`--color-${name}: ${hex};`),
      `missing --color-${name}`,
    );
  }
  assert.match(css, /--radius-sm: 4px;/);
  assert.match(css, /--font-display: "Space Grotesk", sans-serif;/);
  assert.match(css, /--text-h1: 34px;/);
  assert.match(css, /--ease-instrument: cubic-bezier\(0\.2, 0\.8, 0\.2, 1\);/);
  assert.match(css, /--duration-fast: 120ms;/);
});

test("emitKotlin produces valid ARGB int literals for all colors", () => {
  const kt = emitKotlin();
  assert.match(kt, /package gg\.orrery\.generated/);
  assert.match(kt, /object Tokens \{/);
  assert.match(kt, /val voidBase = 0xFF0A0B12\.toInt\(\)/);
  assert.match(kt, /val brassBase = 0xFFE3A23C\.toInt\(\)/);
  assert.match(kt, /val rarityEpic = 0xFFB061F0\.toInt\(\)/);
  assert.match(kt, /const val fast = 120/);
  assert.match(kt, /const val display = "Space Grotesk"/);
  // every color must appear exactly once as a `0xFF...toInt()` literal
  const argbCount = (kt.match(/0xFF[0-9A-F]{6}\.toInt\(\)/g) ?? []).length;
  assert.equal(argbCount, colorEntries().length);
});

test("emitJson round-trips to the source color tokens", () => {
  const parsed = JSON.parse(emitJson());
  assert.deepEqual(parsed.color, color);
});

