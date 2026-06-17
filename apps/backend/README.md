# apps/backend — Orrery Backend Services

Rust/Axum Cargo workspace. Eight celestial service crates live under `crates/`.
All traffic enters through **horizon** (the API gateway); the other crates expose
`pub fn router() -> axum::Router` stubs that horizon mounts at phase 0.

## Crate map

| Crate | Kind | Mount path | Role (§8) |
|---|---|---|---|
| **horizon** | binary | — | API gateway / edge. TLS termination, routing, rate limiting, authn. |
| **meridian** | library | `/api/v1/accounts` | Accounts & identity. Links MSA/Minecraft identity → Orrery account; issues session tokens. |
| **ephemeris** | library | `/api/v1/config` | Cloud config sync. Per-account Codex config, feature flags, remote recognizer toggles. |
| **sextant** | library | `/api/v1/hypixel` | Hypixel API proxy + cache. Holds API key server-side; caches Bazaar/AH/profile data. |
| **aurora** | library | `/api/v1/cosmetics` | Cosmetics service. Definitions keyed to accounts; rendered by Lumen/Halo (same-mod visible only). |
| **comet** | library | `/api/v1/releases` | Release & update distribution. Versioned artifacts, channels, signatures. |
| **transit** | library | `/api/v1/telemetry` | Opt-in telemetry. Crash reports and feature-usage events — off by default. |
| **shared** | library | — | Common types (`AppError`, `HealthResponse`), errors, and `prelude`. |

All service crates are stubs at phase 0. Real logic lands in phases 4–6 per the roadmap.

## How to run horizon

```sh
# From this directory (apps/backend):
cargo run -p horizon

# Custom port:
PORT=9000 cargo run -p horizon

# Check liveness:
curl http://localhost:8080/health
# {"service":"horizon","status":"ok"}
```

## Common commands

```sh
cargo build          # build the whole workspace
cargo test           # run all tests (includes horizon /health oneshot test)
cargo clippy         # lint (no warnings expected)
cargo check          # fast type-check without codegen
```

Via pnpm/turbo (from the repo root):

```sh
turbo run build --filter=@orrery/backend
turbo run test  --filter=@orrery/backend
```
