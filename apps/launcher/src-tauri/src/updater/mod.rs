//! # updater — Comet client for self-update and mod updates
//!
//! Implements the client side of the Comet release/update distribution
//! service. Responsible for:
//! - Checking the Comet manifest for a new Observatory (launcher) version.
//! - Checking for mod artifact updates on the configured release channel
//!   (stable / beta).
//! - Verifying artifact signatures before applying.
//! - Triggering the Tauri updater for launcher self-update.
//!
//! **Phase 0 stub** — module compiles; no update logic yet.
//! Phase 6 will add `CometClient`, channel selection, and signature checks.
