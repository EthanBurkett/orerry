//! # aurora
//!
//! **Role (§8):** Cosmetics service. Cosmetic definitions keyed to Orrery accounts;
//! served to the client and rendered by Lumen/Halo. Cosmetics are only visible to
//! other Orrery users — the allowed "same-mod cosmetics" category.
//! Horizon mounts this router under `/api/v1/cosmetics`.
//!
//! This is a stub — real cosmetics storage, entitlement checks, and manifest
//! delivery are out of scope for Phase 0.

use axum::{Json, Router, routing::get};
use shared::health::HealthResponse;

/// Returns the aurora service router.
///
/// Stub: exposes only a liveness probe. Real routes (cosmetics-manifest,
/// equip, unequip) will be added in Phase 5.
pub fn router() -> Router {
    Router::new().route("/health", get(health_handler))
}

async fn health_handler() -> Json<HealthResponse> {
    Json(HealthResponse::ok("aurora"))
}
