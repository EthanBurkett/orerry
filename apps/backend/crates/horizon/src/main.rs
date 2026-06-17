//! # horizon
//!
//! **Role (§8):** API gateway / edge. TLS termination, routing, rate limiting,
//! and authn of Orrery accounts. All client traffic enters through Horizon, which
//! fans out to the downstream service crates.
//!
//! ## Running
//!
//! ```sh
//! PORT=8080 cargo run -p horizon
//! ```

use axum::{Json, Router, routing::get};
use shared::health::HealthResponse;
use std::net::SocketAddr;
use tower_http::trace::TraceLayer;
use tracing::info;

/// Build the top-level Axum router.
///
/// Mounts:
/// - `GET /health` — gateway liveness probe
/// - `/api/v1/accounts`  → meridian router
/// - `/api/v1/config`    → ephemeris router
/// - `/api/v1/hypixel`   → sextant router
/// - `/api/v1/cosmetics` → aurora router
/// - `/api/v1/releases`  → comet router
/// - `/api/v1/telemetry` → transit router
pub fn build_router() -> Router {
    Router::new()
        .route("/health", get(health_handler))
        .nest("/api/v1/accounts",  meridian::router())
        .nest("/api/v1/config",    ephemeris::router())
        .nest("/api/v1/hypixel",   sextant::router())
        .nest("/api/v1/cosmetics", aurora::router())
        .nest("/api/v1/releases",  comet::router())
        .nest("/api/v1/telemetry", transit::router())
        .layer(TraceLayer::new_for_http())
}

/// `GET /health` — gateway liveness probe.
///
/// Returns `200 OK` with `{"service":"horizon","status":"ok"}`.
async fn health_handler() -> Json<HealthResponse> {
    Json(HealthResponse::ok("horizon"))
}

#[tokio::main]
async fn main() {
    // Initialise tracing — respects RUST_LOG env var.
    tracing_subscriber::fmt()
        .with_env_filter(
            tracing_subscriber::EnvFilter::try_from_default_env()
                .unwrap_or_else(|_| "horizon=info,tower_http=debug".into()),
        )
        .init();

    let port: u16 = std::env::var("PORT")
        .ok()
        .and_then(|v| v.parse().ok())
        .unwrap_or(8080);

    let addr = SocketAddr::from(([0, 0, 0, 0], port));
    info!("horizon listening on {addr}");

    let app = build_router();
    let listener = tokio::net::TcpListener::bind(addr).await.unwrap();
    axum::serve(listener, app).await.unwrap();
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

#[cfg(test)]
mod tests {
    use super::*;
    use axum::body::Body;
    use axum::http::{Request, StatusCode};
    use http_body_util::BodyExt;
    use tower::ServiceExt; // for `.oneshot()`

    /// Calls `GET /health` through the full router using `tower::ServiceExt::oneshot`
    /// (no port binding, no network I/O) and asserts:
    ///   - status 200
    ///   - body parses as `{"service":"horizon","status":"ok"}`
    #[tokio::test]
    async fn health_returns_200_ok() {
        let app = build_router();

        let response = app
            .oneshot(
                Request::builder()
                    .uri("/health")
                    .body(Body::empty())
                    .unwrap(),
            )
            .await
            .expect("request should not fail");

        assert_eq!(response.status(), StatusCode::OK, "expected 200 OK");

        let bytes = response
            .into_body()
            .collect()
            .await
            .expect("body collect failed")
            .to_bytes();

        let json: serde_json::Value =
            serde_json::from_slice(&bytes).expect("body is not valid JSON");

        assert_eq!(json["service"], "horizon", "service field mismatch");
        assert_eq!(json["status"], "ok", "status field mismatch");
    }
}
