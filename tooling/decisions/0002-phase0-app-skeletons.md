# ADR 0002 — Phase 0 app skeleton decisions

- **Status:** Accepted
- **Date:** 2026-06-17
- **Phase:** 0 (Foundations)
- **Decider:** Orchestrator (accepting worker-surfaced decisions)

## Context

The three Phase-0 app skeletons (`apps/mod`, `apps/launcher`, `apps/backend`)
were built by parallel workers. A few decisions surfaced during execution that
future tasks must honor; recording them here.

## Decisions

1. **Launcher is a plain Svelte 5 + Vite SPA, not SvelteKit.** The launcher is a
   single Tauri webview — no SSR, file-routing, adapter, or hydration. SvelteKit
   would add dead weight and risk server assumptions conflicting with the Tauri
   webview. Routing is in-app/component-level.

2. **No global Svelte runes mode.** `@tanstack/svelte-query` v5's
   `QueryClientProvider` still uses legacy `export let` and won't compile under
   global runes. Our components opt into runes individually
   (`<svelte:options runes />`). Do not enable global runes in `svelte.config.js`
   until svelte-query ships a runes-native provider.

3. **Tauri commands registered in `lib.rs`, not `main.rs`.** Standard Tauri 2
   pattern: `#[tauri::command]` + `generate_handler!` live in a `run()` in
   `lib.rs`; `main.rs` is just `observatory::run()`. Avoids duplicate macro errors.

4. **Mod toolchain pins (verified against maven.fabricmc.net, 2026-06-17):**
   MC 1.21.11 · Yarn `1.21.11+build.6` · Loader `0.19.3` ·
   Fabric API `0.141.4+1.21.11` · FLK `1.13.12+kotlin.2.4.0` · Loom `1.17.11` ·
   Gradle `9.5.1` · Kotlin `2.4.0` · `--release 21` bytecode (builds on JDK 25).
   NOTE: Fabric `maven-metadata.xml` `<latest>` was misleading (pointed at a
   non-1.21.11 suffix); `0.141.4+1.21.11` is the latest that actually resolves.
   Re-verify on any version bump.

5. **Mod turbo wrapper uses `gradlew.bat`** in `package.json` scripts so Turbo
   runs it reliably on Windows. (Cross-platform note: revisit if/when CI is
   Linux — may need an OS-conditional or a node shim.)

6. **Backend build opts out of Turbo output caching** (`apps/backend/turbo.json`,
   `outputs: []`): Cargo manages `target/` itself; Turbo only sequences the task,
   per §4 ("Turbo wraps Gradle/Cargo caches rather than replacing them").

## Consequences

- Item 5 is a known cross-platform debt for CI (§10). Flagged, not yet resolved.
- Item 2 constrains how UI components declare reactivity until upstream changes.
