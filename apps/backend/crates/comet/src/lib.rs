//! # comet
//!
//! **Role (§8):** Release & update distribution. Manages versioned launcher and
//! mod artifacts, release channels (stable/beta), and artifact signatures.
//! The Observatory launcher consumes Comet manifests for self-update and mod
//! update. Horizon mounts this router under `/api/v1/releases`.
//!
//! This is a stub — real artifact storage, channel management, and signed
//! manifest delivery are out of scope for Phase 0.

use axum::{Json, Router, routing::get};
use shared::health::HealthResponse;

/// Returns the comet service router.
///
/// Stub: exposes only a liveness probe. Real routes (latest, channels,
/// artifacts, signatures) will be added in Phase 6.
pub fn router() -> Router {
    Router::new().route("/health", get(health_handler))
}

async fn health_handler() -> Json<HealthResponse> {
    Json(HealthResponse::ok("comet"))
}
