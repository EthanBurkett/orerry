//! # meridian
//!
//! **Role (§8):** Accounts & identity. Links a Microsoft/Minecraft identity to an
//! Orrery account; issues Orrery session tokens. Horizon mounts this router under
//! `/api/v1/accounts`.
//!
//! This is a stub — real account logic (MSA token exchange, session issuance)
//! is out of scope for Phase 0.

use axum::{Json, Router, routing::get};
use shared::health::HealthResponse;

/// Returns the meridian service router.
///
/// Stub: exposes only a liveness probe. Real routes (register, login, token
/// refresh, link-microsoft) will be added in Phase 4.
pub fn router() -> Router {
    Router::new().route("/health", get(health_handler))
}

async fn health_handler() -> Json<HealthResponse> {
    Json(HealthResponse::ok("meridian"))
}
