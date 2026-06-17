//! # ipc — loopback WebSocket server (Observatory side of the Relay bridge)
//!
//! Hosts a loopback-only WebSocket server that the in-game Relay module
//! connects to at runtime. Carries:
//! - Account / session status
//! - Cosmetics manifest for the logged-in account (Aurora)
//! - Live config pushes (Ephemeris → Codex)
//! - Orrery client telemetry events (opt-in; Transit)
//!
//! The WS server is bound to `127.0.0.1` only. It is never network-exposed.
//! Message types are defined in `packages/protocol` (shared TS → mirrored to
//! this crate via codegen in a later phase).
//!
//! **Phase 0 stub** — module compiles; no server started yet.
//! Phase 1 will add `RelayServer`, `RelaySession`, and message dispatch.
