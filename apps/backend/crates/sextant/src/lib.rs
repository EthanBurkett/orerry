//! # sextant
//!
//! **Role (§8):** Hypixel API proxy + cache. Holds the Hypixel API key server-side,
//! caches Bazaar/AH/profile data, and serves it to Orrery clients. Keeps API keys
//! out of the client and smooths rate limits. Horizon mounts this router under
//! `/api/v1/hypixel`.
//!
//! This is a stub — real Hypixel proxy logic, key management, and hot-cache wiring
//! (Redis or similar) are out of scope for Phase 0.

use axum::{Json, Router, routing::get};
use shared::health::HealthResponse;

/// Returns the sextant service router.
///
/// Stub: exposes only a liveness probe. Real routes (bazaar, auction-house,
/// player-profile, skyblock-news) will be added in Phase 4.
pub fn router() -> Router {
    Router::new().route("/health", get(health_handler))
}

async fn health_handler() -> Json<HealthResponse> {
    Json(HealthResponse::ok("sextant"))
}
