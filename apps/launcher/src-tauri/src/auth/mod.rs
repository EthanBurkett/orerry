//! # auth — MSA → Xbox Live → XSTS → Minecraft auth chain
//!
//! Implements the full Microsoft Authentication (device-code or auth-code flow)
//! → Xbox Live → XSTS → Minecraft Services token chain.
//!
//! Tokens are stored in the OS keychain via Tauri's secure storage plugin.
//! This module never touches `packages/protocol` wire types directly — the
//! `ipc` module translates between auth state and the frontend payload.
//!
//! **Phase 0 stub** — module compiles; no real auth logic yet.
//! Phase 1 will add `MsaClient`, `XblFlow`, and `MinecraftTokenStore`.
