//! # ephemeris
//!
//! **Role (§8):** Cloud config sync. Stores per-account Codex config, feature
//! flags, and remote recognizer toggles. Reconciles with the Observatory launcher.
//! Horizon mounts this router under `/api/v1/config`.
//!
//! This is a stub — real config storage and reconciliation logic is out of scope
//! for Phase 0.

use axum::{Json, Router, routing::get};
use shared::health::HealthResponse;

/// Returns the ephemeris service router.
///
/// Stub: exposes only a liveness probe. Real routes (get-config, put-config,
/// feature-flags, recognizer-toggles) will be added in Phase 4.
pub fn router() -> Router {
    Router::new().route("/health", get(health_handler))
}

async fn health_handler() -> Json<HealthResponse> {
    Json(HealthResponse::ok("ephemeris"))
}
