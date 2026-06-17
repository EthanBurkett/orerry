# `@orrery/launcher` — Observatory

The Orrery launcher. A Tauri 2 (Rust core) + Svelte 5 (runes) + Vite desktop app.

## Framework choice

**Plain Svelte 5 SPA + Vite** (not SvelteKit). Rationale: the launcher is a single Tauri webview window — there is no server-side rendering, no file-based routing, no adapter concern, and no need for the SSR/hydration machinery SvelteKit adds. A plain Svelte 5 + Vite SPA is the minimal, correct choice: smaller bundle, simpler config, and no risk of SvelteKit's node/server assumptions conflicting with the Tauri webview environment.

## Stack

| Layer | Choice |
|---|---|
| Runtime shell | Tauri 2 (Rust) |
| UI framework | Svelte 5 (runes mode) |
| Bundler | Vite 6 |
| Language | TypeScript |
| Styling | Tailwind CSS v4 (CSS-first, `@tailwindcss/vite` plugin) |
| Design tokens | `@orrery/design-tokens` (workspace package) |
| Data layer | `@tanstack/svelte-query` v5 |

## Design tokens

`src/app.css` imports tokens via:

```css
@import "tailwindcss";
@import "@orrery/design-tokens/css";
```

The `@theme` block in `tokens.css` registers all design tokens as Tailwind v4 utilities (e.g. `bg-void-base`, `text-brass-hi`, `border-hairline-base`). No `tailwind.config.ts` or JS preset is used — Tailwind v4 is configured entirely in CSS.

No hardcoded hex colors appear in any component. All colors derive from `var(--color-*)` custom properties.

## Development

```bash
# From repo root — one command brings up everything concurrently (turbo):
#   design-tokens build → then launcher (Tauri window + Vite) + backend horizon
pnpm dev

# From apps/launcher:
pnpm dev              # full launcher: Tauri window (its beforeDevCommand starts Vite)
pnpm dev:web          # Vite dev server only, http://localhost:1420 (browser preview, no Tauri)
pnpm build            # Vite production build
pnpm typecheck        # svelte-check
```

Note: `tauri dev` starts its own Vite via `beforeDevCommand` (`pnpm dev:web`),
so don't run `pnpm dev:web` separately at the same time — port 1420 is strict.

## Rust core (`src-tauri`)

Modules are Phase 0 stubs — they compile but contain no real logic yet:

| Module | Phase | Description |
|---|---|---|
| `auth/` | 1 | MSA → XBL → XSTS → Minecraft auth chain |
| `game/` | 1 | Version resolve, asset download, JVM launch |
| `ipc/` | 1 | Loopback WS server (Relay peer) |
| `profiles/` | 3 | Local profile + Codex config store |
| `updater/` | 6 | Comet client (self-update + mod updates) |

One command is registered for Phase 0: `app_version` — proves the frontend↔Rust IPC path works.
