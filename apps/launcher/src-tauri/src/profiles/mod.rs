//! # profiles — local profile and config store
//!
//! Manages per-profile and per-account on-disk configuration (Codex schema).
//! The canonical config that the Orrery mod reads on startup is written here
//! before launch. Ephemeris (cloud config sync) reconciles with this store
//! via the backend.
//!
//! Schema is versioned with forward migrations — see DESIGN_SPEC §6.7.
//!
//! **Phase 0 stub** — module compiles; no persistence logic yet.
//! Phase 3 will add `Profile`, `ConfigStore`, and migration infrastructure.
