# Orrery — Design Specification

> A fully custom Hypixel SkyBlock client. Not "vanilla SkyBlock with better mods" — a complete reskin and re-render of the SkyBlock experience, with custom UIs replacing nearly every server-side menu, a custom HUD layer, a Tauri launcher, and a cloud backend for accounts, config sync, and cosmetics.
For the tauri app, you are to use the Impeccable skill for any designs.

**Working name:** Orrery — *a mechanical model of the heavens; a custom rendering of the whole sky-world.*
**Alternates:** Zenith · Vesper · Nimbus · Halo

**Status:** Design spec, v0.1
**Author:** Ethan
**Document scope:** End-to-end architecture, monorepo layout, the visual identity / design system, the menu-override engine, launcher, backend services, build/release, and a phased roadmap.

---

## 1. Vision

Orrery rebuilds the SkyBlock interface from the ground up. Where existing mods (SkyHanni, Skyblocker, Firmament) overlay information *on top of* vanilla chest menus, Orrery **intercepts those menus and renders its own UI in their place** — custom layouts, custom widgets, custom fonts and textures, animation, a coherent design language shared with the launcher.

The thesis: the SkyBlock menu system is a data layer (server-side chest inventories) wearing a vanilla skin. Orrery treats the vanilla skin as disposable. We read the data the server already sends, render whatever we want, and route the user's actions back through the game's own interaction path so the server can't tell the difference.

The non-negotiable design principle that makes this possible *and* keeps users un-bannable is in §2.

---

## 2. The Compliance Model (this is architecture, not a footnote)

Hypixel permits client-side cosmetic/UI/information mods and **bans anything that automates gameplay or alters client↔server communication**. Orrery is "safe by construction" — the architecture is shaped so that a compliant client is the *only* thing it's capable of being.

**The interaction contract — every feature must satisfy all four:**

1. **Render-only freedom.** The UI may re-render anything. Custom menus, custom HUD, custom everything. Pixels are unrestricted.
2. **User-triggered actions only.** Every action Orrery sends to the server must correspond to a physical input the user just made. No synthesized, queued, batched, reordered, or auto-fired actions. No macros, no automation, ever.
3. **Vanilla interaction path only.** Actions are never hand-crafted packets. They go through `interactionManager.clickSlot(...)`, which emits the *identical* packet the vanilla client would. Orrery never invents, rewrites, reorders, or pre-sends network traffic.
4. **Display only what the server sent.** UI may only show data already delivered to the client (container contents, item NBT/lore) or data already public via the Hypixel API. No fabricating hidden information, no ESP / see-through-walls, no other-player health/range, no minimaps.

If a proposed feature can't satisfy all four, it does not get built. This contract is enforced in code review and encoded as lint rules / architectural fitness functions where feasible (see §11).

**Forbidden categories, explicitly:** auto-clickers/macros/scripts of any kind; packet manipulation of any kind; pre-fetching by sending extra packets ahead of the user; ESP / x-ray / through-wall rendering; minimaps; other-player health or distance indicators; anything that "plays the game for you" in part or whole.

---

## 3. High-Level Architecture

Three pillars plus shared packages, all in one Turborepo.

```
┌─────────────────────────────────────────────────────────────┐
│                         ORRERY                                │
│                                                               │
│  ┌────────────────┐   ┌──────────────┐   ┌────────────────┐  │
│  │  THE MOD       │   │  THE LAUNCHER │   │  THE BACKEND   │  │
│  │  (Kotlin/      │   │  (Tauri +     │   │  (Rust/Axum)   │  │
│  │   Fabric/      │◄─►│   Svelte)     │◄─►│                │  │
│  │   Mixin)       │   │  "Observatory"│   │  Horizon ▸     │  │
│  │                │   │               │   │  Meridian      │  │
│  │  Eclipse       │   │  auth · jars  │   │  Ephemeris     │  │
│  │  Lumen         │   │  launch · IPC │   │  Sextant       │  │
│  │  Atlas         │   │  config UI    │   │  Aurora        │  │
│  │  Halo          │   │  cosmetics UI │   │  Comet         │  │
│  │  Codex         │   │  updates      │   │  Transit       │  │
│  │  Relay  ───────┼───┼── loopback WS │   │                │  │
│  └────────────────┘   └──────────────┘   └────────────────┘  │
│           ▲                   ▲                   ▲           │
│           └───────── shared design tokens ────────┘          │
│                      shared protocol types                   │
└─────────────────────────────────────────────────────────────┘
```

- **The Mod** runs inside the game JVM. Owns interception, rendering, HUD, config, and a loopback bridge to the launcher.
- **The Launcher (Observatory)** is the home app: authentication, game-file management, mod injection, the launch pipeline, the config/cosmetics UI, and auto-updates. Tauri (Rust core) + Svelte (UI).
- **The Backend** is a set of small Rust/Axum services behind a gateway: accounts, cloud config sync, Hypixel API proxy/cache, cosmetics, releases, telemetry.
- **Shared packages** give the launcher and the in-game UI one design language (tokens) and give the launcher and backend one set of wire types.

---

## 4. Monorepo Layout (Turborepo)

Turborepo orchestrates the workspace and caches the JS/TS pipeline natively. The Kotlin (Gradle) and Rust (Cargo) builds are wrapped as `package.json` tasks with declared inputs/outputs so `turbo` can sequence and cache them too. Be realistic: Turbo's deep value is on the TS side and as the task graph/runner; Gradle and Cargo keep their own internal caches, and Turbo wraps them rather than replacing them.

**Package manager:** pnpm (workspaces) · **Runner:** turbo

```
orrery/
├─ turbo.json
├─ pnpm-workspace.yaml
├─ package.json
├─ apps/
│  ├─ mod/                     # Kotlin Fabric mod  (Gradle + Loom)
│  │  ├─ build.gradle.kts
│  │  ├─ src/main/kotlin/gg/orrery/
│  │  │  ├─ eclipse/           # menu override engine
│  │  │  ├─ lumen/             # UI toolkit
│  │  │  ├─ atlas/             # SkyBlock data model + parsers
│  │  │  ├─ halo/              # HUD layer
│  │  │  ├─ codex/             # config
│  │  │  ├─ relay/             # loopback bridge to launcher
│  │  │  └─ OrreryMod.kt       # entrypoint
│  │  ├─ src/main/resources/
│  │  │  ├─ orrery.mixins.json
│  │  │  ├─ assets/orrery/...  # fonts, textures, atlases
│  │  │  └─ fabric.mod.json
│  │  └─ package.json          # wraps ./gradlew tasks for turbo
│  │
│  ├─ launcher/                # Tauri 2 + Svelte 5
│  │  ├─ src-tauri/            # Rust core ("Observatory")
│  │  │  ├─ src/
│  │  │  │  ├─ auth/           # MSA→XBL→XSTS→MC chain
│  │  │  │  ├─ game/           # version/jar/library mgmt, launch
│  │  │  │  ├─ ipc/            # loopback WS server (Relay peer)
│  │  │  │  ├─ profiles/       # local profile + config store
│  │  │  │  └─ updater/        # Comet client
│  │  │  ├─ Cargo.toml
│  │  │  └─ tauri.conf.json
│  │  ├─ src/                  # Svelte UI
│  │  │  ├─ routes/            # home, accounts, mods, settings, cosmetics
│  │  │  ├─ lib/
│  │  │  └─ app.html
│  │  ├─ vite.config.ts
│  │  └─ package.json
│  │
│  └─ backend/                 # Rust/Axum services (Cargo workspace)
│     ├─ Cargo.toml            # [workspace]
│     ├─ crates/
│     │  ├─ horizon/           # API gateway / edge
│     │  ├─ meridian/          # accounts + Orrery-account auth
│     │  ├─ ephemeris/         # cloud config sync
│     │  ├─ sextant/           # Hypixel API proxy + cache
│     │  ├─ aurora/            # cosmetics service
│     │  ├─ comet/             # release/update distribution
│     │  ├─ transit/           # opt-in telemetry
│     │  └─ shared/            # common types, db, errors
│     └─ package.json          # wraps cargo tasks for turbo
│
├─ packages/
│  ├─ design-tokens/           # the design system (§5): color, type, radius,
│  │                           #   motion. Authored in TS/JSON; consumed by
│  │                           #   Svelte and code-genned to Tokens.kt for Lumen.
│  ├─ protocol/                # TS types for launcher↔backend + launcher↔mod
│  │                           #   (mod side mirrored in Kotlin, kept in sync)
│  └─ tsconfig/                # shared tsconfig presets
│
└─ tooling/
   ├─ scripts/                 # codegen (tokens→Kotlin), release helpers
   └─ ci/                      # shared CI fragments
```

**`turbo.json` pipeline (sketch):**

```jsonc
{
  "tasks": {
    "build": { "dependsOn": ["^build"], "outputs": ["dist/**", "build/libs/**", "target/release/**"] },
    "tokens:gen": { "outputs": ["**/generated/Tokens.kt", "dist/tokens.*"] },
    "lint":  {},
    "test":  { "dependsOn": ["^build"] },
    "dev":   { "cache": false, "persistent": true }
  }
}
```

`design-tokens` builds first (it's an upstream dependency of both `mod` and `launcher` via `^build`), and a codegen step emits a `Tokens.kt` so the in-game UI and the web UI never drift apart.

---

## 5. Visual Identity & Design System

Orrery has its own identity and wears it everywhere — the launcher and the in-game UI are the same brand, not a Minecraft mod with a launcher bolted on. The `packages/design-tokens` package is the single source of truth: authored once in TS/JSON, consumed directly by the Svelte launcher, and code-generated into a `Tokens.kt` object that Lumen renders from. Change a token, both surfaces change; they cannot drift.

### 5.1 Concept — "brass instrument in the void"

An orrery is a precision brass machine that models the heavens, and that is the aesthetic: a deep cosmic **void**, warm **brass/gold** as the primary accent (a nod to the molten-sun lineage without copying Solarfall's red-orange), a cold **starlight cyan** as the counterpoint, and hairline **orbital** motifs throughout. Warm metal against cold void is the whole personality — precise, instrument-like, dark. Never playful-blocky, never Minecraft.

**Identity rules (non-negotiable for Lumen):**

- The custom typeface is the keystone — the moment menus stop using Minecraft's pixel font, they stop reading as a mod.
- Lumen renders **only** from tokens. No hardcoded colors, no Minecraft fonts, no vanilla widget textures, no chrome (no dirt/stone backgrounds, no beveled buttons, no vanilla slot grid) anywhere.
- The SkyBlock chest layout is *data* (Atlas parses it), never a visual constraint. Inventories are laid out however the design system says, rendered from slot data.
- Glow is done with layered low-opacity strokes and concentric rings — not neon, not heavy blur. Restraint reads as "instrument"; excess reads as "hack client."

### 5.2 Color tokens

**Surfaces — the void**

| Token | Hex | Use |
|---|---|---|
| `void.base` | `#0A0B12` | app / canvas background |
| `surface.1` | `#11131F` | panels, menus |
| `surface.2` | `#181B2A` | slots, insets, cards |
| `surface.3` | `#212539` | raised / hover surfaces |
| `hairline` | `#2E3350` | borders, dividers |
| `hairline.bright` | `#3D4468` | emphasized borders, focus tracks |

**Text**

| Token | Hex | Use |
|---|---|---|
| `text.hi` | `#ECEDF5` | primary text, headings |
| `text.mid` | `#A6A9BE` | secondary text, labels |
| `text.low` | `#6A6E86` | hints, metadata, disabled |

**Accents**

| Token | Hex | Use |
|---|---|---|
| `brass.ember` | `#B5731C` | deep / pressed brass |
| `brass` | `#E3A23C` | **primary accent** — buttons, active state, focus |
| `brass.hi` | `#F7C765` | highlights, glow, key numerals |
| `cyan` | `#4FC9DB` | **secondary accent** — links, ghost buttons, info |
| `cyan.hi` | `#7FE0EE` | cyan highlight |

**Semantic**

| Token | Hex |
|---|---|
| `success` | `#5FD08A` |
| `warning` | `#F2C14E` |
| `danger` | `#F2585B` |
| `info` | `#4FC9DB` (= `cyan`) |

**SkyBlock rarity** — semi-canonical to SkyBlock, harmonized into the palette. Its own token group, kept separate from the brand accents so brand and game-data colors never get conflated.

| Token | Hex | Token | Hex |
|---|---|---|---|
| `rarity.common` | `#C7CAD8` | `rarity.legendary` | `#F0A93E` |
| `rarity.uncommon` | `#5FD08A` | `rarity.mythic` | `#F06FD0` |
| `rarity.rare` | `#4F8FF0` | `rarity.divine` | `#5BD6E0` |
| `rarity.epic` | `#B061F0` | `rarity.special` | `#F2585B` |

### 5.3 Typography

| Role | Family | Weights | Notes |
|---|---|---|---|
| display | Space Grotesk | 500, 700 | wordmark, headings, key UI — the technical-instrument character |
| body | Inter | 400, 500 | prose, labels, most UI text |
| mono | JetBrains Mono | 400, 500 | prices, stats, counts — **tabular numerals** (a data-heavy client lives on aligned numbers) |

**Scale (px):** caption `11` · small `12` · body-sm `13` · body `14` · body-lg `16` · h3 `20` · h2 `26` · h1 `34` · display `42`.

In-game these become an MSDF font atlas (crisp at any scale) under `assets/orrery/`; the launcher loads the same families as webfonts. Same families, both surfaces.

### 5.4 Radius, motion, motif

- **Radius:** `sm` 4 · `md` 6 · `lg` 10 · `pill` 999. Small and precise — instruments aren't bubbly.
- **Motion:** `fast` 120ms · `base` 180ms · `slow` 240ms. Standard easing `cubic-bezier(0.2, 0.8, 0.2, 1)` (snappy ease-out). Mechanical but smooth — like orrery gears.
- **Motif:** concentric orbital rings, hairline tick marks, instrument-readout corners. Loading/empty states use the orbital-rings spinner (concentric rings rotating at different rates — literally an orrery).

### 5.5 Token authoring & codegen

Authored once in TS/JSON; the launcher imports it directly, and a codegen step (`tooling/scripts`) emits the Kotlin object Lumen consumes.

```ts
// packages/design-tokens/src/tokens.ts
export const color = {
  void:     { base: "#0A0B12", s1: "#11131F", s2: "#181B2A", s3: "#212539" },
  hairline: { base: "#2E3350", bright: "#3D4468" },
  text:     { hi: "#ECEDF5", mid: "#A6A9BE", low: "#6A6E86" },
  brass:    { ember: "#B5731C", base: "#E3A23C", hi: "#F7C765" },
  cyan:     { base: "#4FC9DB", hi: "#7FE0EE" },
  rarity:   { common: "#C7CAD8", uncommon: "#5FD08A", rare: "#4F8FF0",
              epic: "#B061F0", legendary: "#F0A93E", mythic: "#F06FD0",
              divine: "#5BD6E0", special: "#F2585B" },
} as const;

export const radius = { sm: 4, md: 6, lg: 10, pill: 999 } as const;
export const motion = { fast: 120, base: 180, slow: 240,
  easing: "cubic-bezier(0.2, 0.8, 0.2, 1)" } as const;
```

```kotlin
// generated — packages/design-tokens → apps/mod/.../generated/Tokens.kt
object Tokens {
  object Color {
    val voidBase = 0xFF0A0B12.toInt()
    val surface2 = 0xFF181B2A.toInt()
    val hairline = 0xFF2E3350.toInt()
    val textHi   = 0xFFECEDF5.toInt()
    val brass    = 0xFFE3A23C.toInt()
    val brassHi  = 0xFFF7C765.toInt()
    val cyan     = 0xFF4FC9DB.toInt()
    // … rarity, semantic …
  }
  object Radius { const val sm = 4; const val md = 6; const val lg = 10 }
  object Motion { const val fast = 120; const val base = 180; const val slow = 240 }
}
```

### 5.6 Canonical style tile

The reference for the identity — palette, type, the rarity row, a sample custom menu (a SkyBlock chest re-rendered entirely in Orrery's vocabulary: hairline slots, brass primary button, cyan ghost button, an orbital tick instead of a chest icon — zero vanilla chrome), and the motion note. Source below is the target Lumen + the Svelte UI render against.

```svg
<svg width="100%" viewBox="0 0 680 724" role="img" xmlns="http://www.w3.org/2000/svg">
<title>Orrery visual identity style tile</title>
<desc>A dark brand board showing Orrery's palette of deep void surfaces, brass gold and starlight cyan accents, SkyBlock rarity colors, typography specimen, a sample custom menu panel, and motion notes.</desc>
<style>
@import url('https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@400;500;700&amp;family=Inter:wght@400;500&amp;family=JetBrains+Mono:wght@400;500&amp;display=swap');
.disp{font-family:'Space Grotesk',sans-serif}
.body{font-family:'Inter',sans-serif}
.mono{font-family:'JetBrains Mono',monospace}
text{fill:#ECEDF5}
.mid{fill:#A6A9BE}.low{fill:#6A6E86}.brass{fill:#E3A23C}.brassHi{fill:#F7C765}.cyan{fill:#4FC9DB}
.kick{fill:#6A6E86;letter-spacing:1.5px}
</style>

<rect x="0" y="0" width="680" height="724" rx="16" fill="#0A0B12"/>
<rect x="1" y="1" width="678" height="722" rx="15" fill="none" stroke="#1C2034" stroke-width="1"/>

<circle cx="540" cy="50" r="1.2" fill="#F7C765" opacity="0.5"/>
<circle cx="600" cy="86" r="1" fill="#4FC9DB" opacity="0.45"/>
<circle cx="470" cy="40" r="1" fill="#ECEDF5" opacity="0.3"/>
<circle cx="640" cy="60" r="1.4" fill="#E3A23C" opacity="0.4"/>
<circle cx="430" cy="96" r="1" fill="#ECEDF5" opacity="0.22"/>
<circle cx="575" cy="36" r="1" fill="#4FC9DB" opacity="0.3"/>

<circle cx="70" cy="74" r="34" fill="none" stroke="#4FC9DB" stroke-width="0.75" opacity="0.35"/>
<circle cx="70" cy="74" r="25" fill="none" stroke="#E3A23C" stroke-width="1" opacity="0.85"/>
<circle cx="70" cy="74" r="15" fill="none" stroke="#3D4468" stroke-width="0.75"/>
<circle cx="70" cy="74" r="4" fill="#F7C765"/>
<circle cx="91.6" cy="61.5" r="3" fill="#4FC9DB"/>
<circle cx="44.6" cy="84" r="2.4" fill="#E3A23C"/>

<text class="disp" x="120" y="68" font-size="34" font-weight="700" letter-spacing="0.5"><tspan fill="#F7C765">O</tspan><tspan fill="#ECEDF5">rrery</tspan></text>
<text class="body mid" x="121" y="92" font-size="13">Hypixel SkyBlock client</text>
<text class="mono low" x="640" y="60" font-size="11" text-anchor="end">brass instrument</text>
<text class="mono brass" x="640" y="78" font-size="11" text-anchor="end">in the void</text>

<line x1="40" y1="116" x2="640" y2="116" stroke="#1C2034" stroke-width="1"/>

<text class="body kick" x="40" y="148" font-size="11">PALETTE</text>

<rect x="40" y="160" width="56" height="56" rx="6" fill="#0A0B12" stroke="#3D4468" stroke-width="0.75"/>
<rect x="108" y="160" width="56" height="56" rx="6" fill="#11131F" stroke="#3D4468" stroke-width="0.75"/>
<rect x="176" y="160" width="56" height="56" rx="6" fill="#181B2A" stroke="#3D4468" stroke-width="0.75"/>
<rect x="244" y="160" width="56" height="56" rx="6" fill="#212539" stroke="#3D4468" stroke-width="0.75"/>
<rect x="312" y="160" width="56" height="56" rx="6" fill="#2E3350" stroke="#3D4468" stroke-width="0.75"/>
<rect x="380" y="160" width="56" height="56" rx="6" fill="#B5731C"/>
<rect x="448" y="160" width="56" height="56" rx="6" fill="#E3A23C"/>
<rect x="516" y="160" width="56" height="56" rx="6" fill="#F7C765"/>
<rect x="584" y="160" width="56" height="56" rx="6" fill="#4FC9DB"/>

<text class="mono low" x="68" y="232" font-size="11" text-anchor="middle">0A0B12</text>
<text class="mono low" x="136" y="232" font-size="11" text-anchor="middle">11131F</text>
<text class="mono low" x="204" y="232" font-size="11" text-anchor="middle">181B2A</text>
<text class="mono low" x="272" y="232" font-size="11" text-anchor="middle">212539</text>
<text class="mono low" x="340" y="232" font-size="11" text-anchor="middle">2E3350</text>
<text class="mono low" x="408" y="232" font-size="11" text-anchor="middle">B5731C</text>
<text class="mono brass" x="476" y="232" font-size="11" text-anchor="middle">E3A23C</text>
<text class="mono brassHi" x="544" y="232" font-size="11" text-anchor="middle">F7C765</text>
<text class="mono cyan" x="612" y="232" font-size="11" text-anchor="middle">4FC9DB</text>

<text class="body kick" x="40" y="262" font-size="11">RARITY</text>

<rect x="40" y="272" width="68" height="26" rx="6" fill="#C7CAD8"/>
<text class="body" x="74" y="289" font-size="11" font-weight="500" fill="#1A1C24" text-anchor="middle">common</text>
<rect x="116" y="272" width="68" height="26" rx="6" fill="#5FD08A"/>
<text class="body" x="150" y="289" font-size="11" font-weight="500" fill="#0E2419" text-anchor="middle">uncommon</text>
<rect x="192" y="272" width="68" height="26" rx="6" fill="#4F8FF0"/>
<text class="body" x="226" y="289" font-size="11" font-weight="500" fill="#08213F" text-anchor="middle">rare</text>
<rect x="268" y="272" width="68" height="26" rx="6" fill="#B061F0"/>
<text class="body" x="302" y="289" font-size="11" font-weight="500" fill="#26113B" text-anchor="middle">epic</text>
<rect x="344" y="272" width="68" height="26" rx="6" fill="#F0A93E"/>
<text class="body" x="378" y="289" font-size="11" font-weight="500" fill="#3A2606" text-anchor="middle">legendary</text>
<rect x="420" y="272" width="68" height="26" rx="6" fill="#F06FD0"/>
<text class="body" x="454" y="289" font-size="11" font-weight="500" fill="#3A0E30" text-anchor="middle">mythic</text>
<rect x="496" y="272" width="68" height="26" rx="6" fill="#5BD6E0"/>
<text class="body" x="530" y="289" font-size="11" font-weight="500" fill="#0A3236" text-anchor="middle">divine</text>
<rect x="572" y="272" width="68" height="26" rx="6" fill="#F2585B"/>
<text class="body" x="606" y="289" font-size="11" font-weight="500" fill="#3D0E0F" text-anchor="middle">special</text>

<line x1="40" y1="322" x2="640" y2="322" stroke="#1C2034" stroke-width="1"/>

<text class="body kick" x="40" y="352" font-size="11">TYPE</text>

<text class="disp" x="40" y="398" font-size="42" font-weight="500" fill="#ECEDF5">Orrery</text>
<text class="mono low" x="640" y="388" font-size="11" text-anchor="end">Space Grotesk · display</text>

<text class="body mid" x="40" y="432" font-size="16">Configure your loadout before the next run.</text>
<text class="mono low" x="640" y="432" font-size="11" text-anchor="end">Inter · body</text>

<text class="mono" x="40" y="462" font-size="15" font-weight="500"><tspan fill="#F7C765">1,204,500</tspan><tspan fill="#6A6E86"> coins   </tspan><tspan fill="#5FD08A">+18.4%</tspan></text>
<text class="mono low" x="640" y="462" font-size="11" text-anchor="end">JetBrains Mono · numeric</text>

<line x1="40" y1="484" x2="640" y2="484" stroke="#1C2034" stroke-width="1"/>

<text class="body kick" x="40" y="514" font-size="11">COMPONENTS · CUSTOM MENU</text>

<rect x="40" y="526" width="600" height="150" rx="10" fill="#11131F" stroke="#2E3350" stroke-width="1"/>
<circle cx="64" cy="552" r="7" fill="none" stroke="#E3A23C" stroke-width="1"/>
<circle cx="64" cy="552" r="2" fill="#F7C765"/>
<text class="disp" x="80" y="557" font-size="15" font-weight="500" fill="#ECEDF5">SkyBlock Menu</text>
<text class="body low" x="616" y="558" font-size="17" text-anchor="end">×</text>
<line x1="56" y1="570" x2="624" y2="570" stroke="#2E3350" stroke-width="0.75"/>

<rect x="56" y="586" width="56" height="56" rx="6" fill="#181B2A" stroke="#2E3350" stroke-width="0.75"/>
<circle cx="84" cy="612" r="11" fill="#F0A93E" opacity="0.85"/>
<rect x="124" y="586" width="56" height="56" rx="6" fill="#181B2A" stroke="#2E3350" stroke-width="0.75"/>
<circle cx="152" cy="612" r="11" fill="#B061F0" opacity="0.85"/>
<rect x="192" y="586" width="56" height="56" rx="6" fill="#181B2A" stroke="#2E3350" stroke-width="0.75"/>
<circle cx="220" cy="612" r="11" fill="#4F8FF0" opacity="0.85"/>
<rect x="260" y="586" width="56" height="56" rx="6" fill="#181B2A" stroke="#2E3350" stroke-width="0.75"/>
<circle cx="288" cy="612" r="11" fill="#5BD6E0" opacity="0.85"/>
<rect x="328" y="586" width="56" height="56" rx="6" fill="#181B2A" stroke="#2E3350" stroke-width="0.75"/>
<circle cx="356" cy="612" r="11" fill="#5FD08A" opacity="0.85"/>
<text class="mono brassHi" x="378" y="638" font-size="10" text-anchor="end">64</text>
<rect x="396" y="586" width="56" height="56" rx="6" fill="#181B2A" stroke="#2E3350" stroke-width="0.75"/>
<circle cx="424" cy="612" r="11" fill="#C7CAD8" opacity="0.8"/>

<rect x="472" y="586" width="152" height="36" rx="6" fill="#E3A23C"/>
<text class="disp" x="548" y="609" font-size="14" font-weight="500" fill="#2A1A06" text-anchor="middle">Open</text>
<rect x="472" y="630" width="152" height="32" rx="6" fill="none" stroke="#4FC9DB" stroke-width="1"/>
<text class="disp cyan" x="548" y="651" font-size="13" font-weight="500" text-anchor="middle">Details</text>

<circle cx="60" cy="700" r="13" fill="none" stroke="#3D4468" stroke-width="1"/>
<circle cx="60" cy="700" r="13" fill="none" stroke="#E3A23C" stroke-width="1.5" stroke-dasharray="20 62" stroke-linecap="round"/>
<circle cx="60" cy="700" r="3" fill="#F7C765"/>
<text class="body mid" x="88" y="696" font-size="12">Mechanical, precise — 120 / 180 / 240ms</text>
<text class="mono low" x="88" y="713" font-size="11">ease-out · cubic-bezier(0.2, 0.8, 0.2, 1)</text>
</svg>
```

---

## 6. The Mod (Kotlin · Fabric · Mixin)

### 6.1 Target & toolchain

- **Minecraft version:** **1.21.11 only.** Single-version target — no multi-version band, no backward support. Everything below is built and tested against 1.21.11 exclusively.
- **Loader:** Fabric Loader + Fabric API.
- **Language:** Kotlin via Fabric Language Kotlin. Coroutines for async (API calls, IPC).
- **Mappings:** Yarn (community-standard for Fabric; method names in this doc, e.g. `clickSlot`, are Yarn).
- **Patching:** Mixin (via Loom).
- **Build:** Gradle (Kotlin DSL) + Fabric Loom.
- **Compatibility:** must coexist with Sodium and friends — Orrery renders its own GUI screens and HUD, and must not fight world-rendering optimizers. Detect and gracefully degrade if conflicting UI mods are present.
- **Version pinning:** Mixin targets, mappings, and Fabric/loader/dependency versions are pinned to the 1.21.11 toolchain. No `compat`/multi-version layer — version-sensitive code targets 1.21.11 directly. A future version bump is a deliberate, wholesale migration, not a runtime band.

### 6.2 Subsystems

| Codename | Role |
|---|---|
| **Eclipse** | Menu override engine. Intercepts container opens, reads the `ScreenHandler`, decides whether Orrery owns the menu, and drives the vanilla interaction path on input. The spine. |
| **Lumen** | The custom UI toolkit. Retained-mode component tree, layout, theming from design tokens, custom fonts/textures, animation. Renders both custom menus and the HUD. |
| **Atlas** | SkyBlock data model. Parses container items, NBT (`ExtraAttributes` etc.), and lore into typed structures; fingerprints menus so Eclipse knows what it's looking at. |
| **Halo** | HUD layer. Cosmetic/status overlays built on Lumen. Strictly information-already-available + cosmetics. |
| **Codex** | Config system. On-disk source of truth; schema-versioned; reconciled with Ephemeris by the launcher. |
| **Relay** | Loopback bridge to the launcher (account status, live config push, cosmetics handshake). |

### 6.3 Eclipse — the menu override spine

This is the make-or-break vertical slice and the heart of the client.

**How SkyBlock menus actually work:** every menu (SkyBlock menu, Bazaar, AH, storage, NPCs) is a server-side chest inventory. The server sends a container of items whose display name / lore / NBT encode the information; clicking a slot sends a standard window-click that the server interprets. A "custom menu" is therefore three jobs:

1. **Read** — parse the container the server already sent (Atlas).
2. **Render** — draw an Orrery `Screen` from that data instead of the vanilla chest (Lumen).
3. **Route** — when the user clicks an Orrery widget, fire the click on the *corresponding real slot* via the vanilla interaction manager.

**Mechanism:**

- Mixin into the handled-screen lifecycle. On container open, Atlas fingerprints the menu by title + slot signature + item NBT.
- If Orrery owns this menu, suppress/replace the vanilla `HandledScreen` with an Orrery `Screen` that reads from the live `ScreenHandler`'s slot list.
- On user input against an Orrery widget, resolve it to the backing slot index and call:

```kotlin
// Illustrative — the only sanctioned way to act on a menu.
client.interactionManager?.clickSlot(
    handler.syncId,
    backingSlotIndex,        // the REAL slot this widget maps to
    button,                  // left/right exactly as the user pressed
    SlotActionType.PICKUP,   // or QUICK_MOVE for shift-click, etc.
    client.player            // emits the identical ClickSlotC2SPacket vanilla would
)
```

The server receives byte-identical traffic to a normal player clicking that slot. We never construct a packet. We never click a slot the user didn't physically click. That is the entire compliance posture, enforced at the one chokepoint every menu interaction must pass through.

**Ownership model:** Eclipse maintains a registry of menu *recognizers* (Atlas fingerprints → Lumen view factories). Unrecognized menus fall through to vanilla untouched. This makes coverage incremental — ship one menu, then widen.

### 6.4 Lumen — the UI toolkit

- Retained-mode component tree (think a tiny declarative UI runtime) rendered through `DrawContext`, with an optional custom render path for effects (gradients, layering, animated transitions) within what the GUI pipeline allows.
- Layout engine: a simple flexbox-ish solver (row/column/stack, padding, alignment, sizing). Components are composable; state is explicit.
- **Visual identity:** Lumen renders **only** from the generated `Tokens.kt` (the design system, §5) — zero hardcoded colors, zero Minecraft fonts, zero vanilla widget textures or chrome. The custom typeface is the keystone. See §5.1 for the identity rules.
- Assets: custom font atlas + texture atlas under `assets/orrery/`. MSDF font for crisp custom typography at any scale.
- Input: pointer + keyboard routed through Lumen's hit-testing; every action-producing widget ultimately calls back into Eclipse's `clickSlot` chokepoint (it cannot bypass it).

### 6.5 Atlas — data model

- Typed parsers for the SkyBlock item model: rarity, item id, `ExtraAttributes`, enchants, reforges, lore lines → structured fields.
- Menu fingerprinting: stable recognizers resilient to minor Hypixel changes (match on title pattern + key slot items rather than absolute layout where possible). Fingerprints are versioned; a recognizer can be disabled remotely (via Ephemeris flags) if Hypixel changes a menu and a recognizer goes stale, so a bad parse degrades to vanilla rather than breaking.
- Hypixel API data (prices, etc.) arrives via Sextant (backend), never by Orrery making its own extra in-game requests.

### 6.6 Halo — HUD

- Cosmetic/status overlays composed in Lumen: stats already visible to the player, item/price info from Sextant, custom-styled scoreboard/action-bar replacements.
- Hard rule inheritance from §2: no minimaps, no other-player indicators, no hidden-info surfacing.

### 6.7 Codex — config

- On-disk JSON/TOML, schema-versioned with migrations.
- The launcher writes config (from the settings UI + Ephemeris) before launch; Relay can push live updates while the game runs.
- Per-profile and per-account scoping.

### 6.8 Relay — launcher bridge

- A WebSocket client to a loopback-only server hosted by Observatory (see §7.4).
- Carries: account/session status, cosmetics manifest for the logged-in account, live config pushes, and Orrery client telemetry events (opt-in, see Transit).
- Loopback only. Never exposed off-device.

---

## 7. The Launcher — "Observatory" (Tauri 2 + Svelte 5)

### 7.1 Responsibilities

Authentication, game-file management, mod + dependency injection into the instance, the launch pipeline, the config/cosmetics/settings UI, and auto-updates. The launcher is a separate native process — none of it runs inside the game JVM, so it's all Rust + web, no JVM constraint.

### 7.2 Rust core (`src-tauri`)

- **auth/** — full MSA → Xbox Live → XSTS → Minecraft services token chain. Device-code or auth-code flow; tokens stored in the OS keychain via Tauri's secure storage. (This is well-trodden — the same chain Prism/MultiMC implement.)
- **game/** — resolve the target MC version, download the official client jar + libraries + assets from Mojang, assemble the classpath (LWJGL etc.), and place the Fabric loader + Orrery mod + required Fabric API/Kotlin libs into the instance. **Bring-your-own legitimate game files; never redistribute Mojang code.**
- **ipc/** — hosts the loopback WebSocket server that Relay connects to.
- **profiles/** — local profile + config store; the canonical on-disk config that Codex reads.
- **updater/** — Comet client for self-update and mod updates.

### 7.3 Svelte UI

- Routes: Home/Play, Accounts, Mods/Features, Settings, Cosmetics.
- Svelte 5 (runes), TypeScript, Vite.
- Consumes `packages/design-tokens` directly (same identity as the in-game UI, §5), and `packages/protocol` for typed Tauri command payloads.
- The settings UI is the authoring surface for Codex config; the cosmetics UI talks to Aurora through the backend.

### 7.4 Launcher ↔ Mod communication

- **Source of truth:** config is written to disk by Observatory before launch; Codex reads it on start.
- **Live channel:** Relay (mod) ⇄ Observatory loopback WS for runtime status, cosmetics handshake, and live config pushes. JSON messages typed by `packages/protocol`. Loopback-bound, never network-exposed.

### 7.5 Launch pipeline

```
auth ok? ──▶ resolve version ──▶ ensure jar+libs+assets ──▶ ensure Fabric+Orrery+deps
        ──▶ write Codex config ──▶ start loopback WS ──▶ spawn JVM ──▶ Relay connects
```

---

## 8. Backend Services (Rust · Axum · Postgres)

Small services behind a gateway. Postgres for durable state; Redis (or similar) for Sextant's hot cache. Celestial codenames to match the client.

| Service | Role |
|---|---|
| **Horizon** | API gateway / edge. TLS termination, routing, rate limiting, authn of Orrery accounts. |
| **Meridian** | Accounts & identity. Links a Microsoft/Minecraft identity to an Orrery account; issues Orrery session tokens. (Your Locksmith/OIDC background applies directly.) |
| **Ephemeris** | Cloud config sync. Stores per-account Codex config + feature flags + remote recognizer toggles; reconciles with the launcher. |
| **Sextant** | Hypixel API proxy + cache. Holds the Hypixel API key server-side, caches Bazaar/AH/profile data, serves it to clients. Keeps keys out of the client and smooths rate limits. |
| **Aurora** | Cosmetics. Cosmetic definitions keyed to accounts; served to the client and rendered by Lumen/Halo. Cosmetics are only visible to other Orrery users — the allowed "same-mod cosmetics" category. |
| **Comet** | Release & update distribution. Versioned launcher + mod artifacts, channels (stable/beta), signatures. |
| **Transit** | Opt-in, privacy-respecting telemetry (crashes, feature usage). Off by default; clearly disclosed. |

---

## 9. Auth & Account Model

- **Game login:** standard MSA → XBL → XSTS → `api.minecraftservices.com` chain in Observatory; tokens in OS keychain.
- **Orrery account:** Meridian issues an Orrery session bound to the player's identity, used for Ephemeris (config), Aurora (cosmetics), and Comet (entitlements/channels).
- Principle of least data: store what's needed for sync and cosmetics; telemetry is opt-in and separable.

---

## 10. Build, CI/CD, Release

- **Local:** `turbo run dev` brings up the launcher (Tauri dev) + backend (cargo watch) + token codegen; the mod is built/run via Loom (`runClient`) for in-game iteration.
- **CI (per package, via turbo task graph):**
  - `mod`: Gradle build + Mixin validation + (where feasible) the compliance fitness checks from §11.
  - `launcher`: Svelte typecheck/lint/build + Tauri build per-OS.
  - `backend`: cargo build/clippy/test per crate.
  - `design-tokens`: build + Kotlin codegen, verify no drift.
- **Release:** Comet publishes signed launcher installers (per-OS) and mod artifacts on channels. Auto-update via Tauri updater + Comet manifests.
- **Distribution:** launcher downloads official game files at runtime; **Orrery ships only its own code and assets, never Mojang's.**

---

## 11. Enforcing the Compliance Model in Code

The §2 contract is load-bearing, so we make violating it hard:

- **Single chokepoint:** all menu actions route through one Eclipse function that wraps `clickSlot`. No other code path may emit interaction packets. A lint/architecture test fails the build if `clickSlot` (or any C2S packet send) appears outside that chokepoint.
- **No raw packet sends:** an architectural fitness test forbids direct construction/sending of `*C2SPacket` types anywhere in the codebase.
- **No timers driving actions:** no scheduler/coroutine path may call the chokepoint; only direct user-input handlers may. Reviewed and, where detectable, linted.
- **Render/act separation:** Lumen widgets cannot call the network; they can only request an action that an input handler authorizes. Enforced by module boundaries (Lumen has no dependency on Relay/network or the click chokepoint except through an input-authorized callback).
- **Data provenance:** Atlas only exposes data parsed from received containers or fetched via Sextant; there's no API for "data the player can't see."

---

## 12. Roadmap (Phased)

**Phase 0 — Foundations.** Turborepo scaffolding; pnpm workspaces; `design-tokens` (the §5 system) + codegen to `Tokens.kt`; mod loads on 1.21.11 and prints a marker; Observatory boots, authenticates, downloads files, and launches *vanilla-plus-empty-Orrery* successfully. *Exit: the empty client launches and connects to Hypixel.*

**Phase 1 — The Eclipse spine (critical slice).** Intercept exactly one menu (the SkyBlock main menu). Fingerprint it (Atlas), render a basic custom `Screen` from its `ScreenHandler` (Lumen), and route one click end-to-end through the `clickSlot` chokepoint. *Exit: one menu fully custom-rendered and fully interactive, server none the wiser.*

**Phase 2 — Toolkit + coverage.** Mature Lumen (layout, theming from tokens, fonts, animation); expand Atlas parsers; add recognizers for the high-traffic menus (Bazaar, AH, storage, key NPCs). Remote recognizer toggles via Ephemeris.

**Phase 3 — HUD + config.** Halo overlays; Codex schema + migrations; launcher settings UI authoring Codex; live config push over Relay.

**Phase 4 — Backend + data.** Stand up Horizon/Meridian/Ephemeris/Sextant; Orrery accounts; Hypixel API integration through Sextant; cloud config sync.

**Phase 5 — Cosmetics + polish.** Aurora cosmetics end-to-end (server → client render, same-mod-visible only); custom textures/fonts/animation pass; performance hardening; Sodium/compat matrix.

**Phase 6 — Release.** Comet channels, signing, auto-update, per-OS installers, public beta.

---

## 13. Agentic Development Workflow

Orrery is built by an orchestrator/worker agent system, not hand-coded linearly. **Opus is the orchestrator** — its only job is to make decisions and decompose work into scoped subtasks. **Sonnet agents are the workers** — each executes exactly one scoped task. The orchestrator never writes production code itself; it holds the global picture (this spec, the roadmap, the decision log), decides, dispatches, verifies, and integrates. Expensive reasoning stays on decisions; execution parallelizes across faster workers. (In practice: Opus is the main Claude Code session; Sonnet subagents run the tasks.)

### 13.1 Roles

**Orchestrator (Opus) — decide & delegate.**
- Owns the spec, the roadmap (§12), and the decision log (§13.6).
- Selects the next task(s) respecting phase/dependency order, writes a task contract (§13.3) for each, and dispatches to workers (in parallel when independent).
- Makes *all* architectural and cross-cutting decisions. Workers never do.
- Verifies every returned task against its success boundaries before accepting; rejects and re-dispatches, or escalates to the human, as needed.
- Does not implement. If it feels tempted to write code, that's the signal the task wasn't decomposed finely enough.

**Workers (Sonnet) — execute one task.**
- Receive a self-contained task contract; produce exactly the declared deliverables (code + tests + docs for their slice).
- Make no architectural decisions, expand no scope, touch no files outside their declared deliverables.
- On ambiguity, a needed decision, or a guideline conflict: **stop and escalate** rather than guess.

### 13.2 The orchestrator loop

```
select task (deps satisfied) ──▶ write task contract ──▶ dispatch to Sonnet worker(s)
        ▲                                                          │
        │                                                          ▼
   update roadmap  ◀── accept & integrate ◀── verify vs. success boundaries
        │                      ▲                        │ fail
        │                      └──── re-dispatch w/ corrections ◀─┘
        └── record decision in log (if one was made) · escalate to human if a decision is needed
```

### 13.3 Task contract (the guideline every subtask follows)

Every dispatched subtask is a filled-out contract — no free-form "go build the launcher" tasks, only scoped, bounded ones.

```md
# Task <id> — <short title>
phase: <n>          package: apps/mod | apps/launcher | apps/backend | packages/<name>
subsystem: <Eclipse|Lumen|Atlas|Halo|Codex|Relay|auth|game|sextant|...>
depends_on: [<task ids that must be merged first>]

## Objective
<1–2 sentences: what to build and why it exists in the plan>

## Guidelines (must follow)
- Compliance: §2 contract + §11 fitness functions          (HARD)
- Identity: render only from Tokens.kt (§5)                (if UI)
- Target: MC 1.21.11 only, Yarn mappings (§6.1)            (if mod)
- Standards: <language style, error handling, tests required>
- Read first: <spec sections, ADRs, existing files/interfaces to honor>

## Inputs
- files to read: <paths>
- interfaces/contracts to honor: <types, signatures, schemas>

## Deliverables (the ONLY files this task may touch)
- <path> — <what>
- tests: <coverage required>
- docs: <if any>

## Success boundaries
done_when:
  - <testable criterion>
  - <testable criterion>
  - `turbo run build lint test` (this package) passes
  - §11 compliance fitness functions pass
out_of_scope:
  - <explicit exclusions — what NOT to build or touch>
escalate_if:
  - ambiguity, a needed decision, unachievable criteria, or a guideline must bend
```

### 13.4 Success boundaries

A task is not "done" because the worker says so — it's done when it provably crosses its boundaries. Every contract defines three:

1. **Definition of done — binary & testable.** No "improve" or "polish" tasks; only "X such that `<observable, checkable condition>`." If you can't write the check, the task isn't ready to dispatch.
2. **Scope ceiling.** The contract names the exact files the task may touch. Touching anything else — or making a decision — is a boundary violation: escalate, don't proceed.
3. **Hard gates.** Not accepted until, for its package, `turbo run build lint test` passes **and** the compliance fitness functions (§11) pass. For mod tasks that specifically includes: no `clickSlot`/C2S send outside the Eclipse chokepoint (§6.3), UI renders only from tokens (§5), and 1.21.11-only.

**Escalation triggers (worker stops and returns):** a missing or contradictory guideline; a decision the orchestrator must own; success criteria not achievable as written; or any guideline — especially the §2 compliance contract — that would have to be bent to finish.

### 13.5 Invariants every agent inherits

Injected into every task contract so no worker can drift, regardless of task:

- The §2 compliance contract and the single `clickSlot` chokepoint (§6.3, §11).
- Tokens-only rendering / the §5 visual identity (any UI work).
- MC 1.21.11 only, Yarn mappings (any mod work) (§6.1).
- Never redistribute Mojang code (§7.2).
- Architectural decisions belong to the orchestrator; workers stay inside declared deliverables.
- Tests + fitness functions are part of "done," never optional.

### 13.6 Decision log

The orchestrator's output is decisions, so they get recorded. `tooling/decisions/` holds ADR-style entries (context · options · choice · rationale · date). The §15 "Decisions Locked vs. Open" list is the rolling summary; the ADRs are the detail. Workers read the relevant ADRs as task context; only the orchestrator writes them.

---

## 14. Risks & Open Questions

- **Hypixel dropping 1.21.11.** The whole client is pinned to 1.21.11; if Hypixel stops accepting it for SkyBlock, Orrery is offline until a deliberate, wholesale migration to the new version. Accepted as a known trade-off of single-version targeting — keep the version-sensitive code well-isolated so a migration is a contained effort.
- **Menu fingerprint brittleness.** Hypixel changes menus periodically. Recognizers must degrade to vanilla on mismatch, and be remotely disable-able via Ephemeris flags.
- **Anti-cheat false positives.** Even compliant clients can trip heuristics if rendering does something odd. Keep interaction byte-identical to vanilla (the chokepoint guarantees this) and avoid anything timing-sensitive.
- **The "render only what's sent" ceiling.** Some UI ideas will be impossible because the data isn't client-side. Design within it; lean on Sextant for legitimately-public API data.
- **Maintenance load.** A full-custom UI is more surface to maintain across MC + SkyBlock updates than an overlay mod. Phasing + recognizer modularity contain it.
- **Naming.** Confirm **Orrery** vs. an alternate before assets/branding are produced.
- **Backend hosting.** Reuse existing infra patterns (Postgres, container stack) or stand up fresh.

---

## 15. Decisions Locked vs. Open

**Locked:** **MC 1.21.11 only** · Kotlin/Fabric/Mixin mod · Tauri + Svelte launcher · Rust/Axum backend · Turborepo + pnpm · custom-UI-over-server-menus architecture · the §2 compliance contract · the `clickSlot` chokepoint · the §5 "brass instrument in the void" visual identity (tokens as single source of truth).

**Open:** final name · mappings (Yarn assumed) · whether to build Lumen from scratch vs. atop an existing Fabric UI lib · backend hosting target · telemetry scope · final typeface licensing (Space Grotesk / Inter / JetBrains Mono assumed — all openly licensed).
