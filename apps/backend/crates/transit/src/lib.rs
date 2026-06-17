//! # transit
//!
//! **Role (§8):** Opt-in, privacy-respecting telemetry. Collects crash reports and
//! feature-usage events — off by default, clearly disclosed to users.
//! Horizon mounts this router under `/api/v1/telemetry`.
//!
//! This is a stub — real event ingestion, anonymisation, and storage pipelines
//! are out of scope for Phase 0.

use axum::{Json, Router, routing::get};
use shared::health::HealthResponse;

/// Returns the transit service router.
///
/// Stub: exposes only a liveness probe. Real routes (ingest-event, crash-report)
/// will be added in a later phase, gated behind user opt-in.
pub fn router() -> Router {
    Router::new().route("/health", get(health_handler))
}

async fn health_handler() -> Json<HealthResponse> {
    Json(HealthResponse::ok("transit"))
}
