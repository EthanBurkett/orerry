# ADR 0001 — Frontend stack & token pipeline

- **Status:** Accepted
- **Date:** 2026-06-17
- **Phase:** 0 (Foundations)
- **Decider:** Orchestrator

## Context

Phase 0 needs the monorepo skeleton and `packages/design-tokens` — the upstream
`^build` dependency of both the mod and the launcher, and the single source of
truth for the §5 visual identity. Several stack details were unspecified in
DESIGN_SPEC.md and had to be decided before scaffolding.

## Decisions

1. **Launcher styling: Tailwind CSS v4 (CSS-first).** Tailwind v4 has no
   `tailwind.config.{ts,js}` / JS preset; the theme is declared in CSS via
   `@theme { … }`. The design system is therefore emitted as a single
   `tokens.css` containing a `@theme` block. Those variables both generate
   utilities (`bg-void-base`, `text-h1`, `ease-instrument`) and are exposed as
   `:root` custom properties — so §5's "render only from tokens, no hardcoded
   colors" holds in markup. Rejected: a JS preset object (does not exist in v4).

2. **Launcher data fetching: `@tanstack/svelte-query`.** For server-state from
   the backend / Tauri commands (caching, retries, invalidation).

3. **Token codegen via tsx, not a compile step.** Tokens are authored in TS;
   `tsx` runs the emit/codegen scripts directly (no intermediate `tsc` emit).
   - `pnpm build` → `dist/tokens.css` (Tailwind v4 `@theme`) + `dist/tokens.json`.
   - `pnpm tokens:gen` → `apps/mod/src/main/kotlin/gg/orrery/generated/Tokens.kt`.
   The launcher imports the TS source directly (Vite compiles); the mod consumes
   the generated Kotlin. One source, two surfaces, cannot drift.

4. **Generated code is not committed.** `**/generated/` and `dist/` are
   gitignored; `tokens:gen` is reproducible and runs in CI / before the mod build.

5. **pnpm `allowBuilds: esbuild: true`.** tsx depends on esbuild's postinstall
   to fetch its platform binary; approved in `pnpm-workspace.yaml`.

## Consequences

- Changing a token value updates both the launcher (Tailwind utilities + CSS
  vars) and the mod (`Tokens.kt`) from one edit.
- CI must run `tokens:gen` before building the mod, and a drift check (build
  tokens, assert no uncommitted change in tracked token inputs) per §10.
- Token scale keys keep their source casing in CSS (`--text-bodySm`), so the
  Tailwind utility is `text-bodySm`. Acceptable; revisit if kebab is preferred.
