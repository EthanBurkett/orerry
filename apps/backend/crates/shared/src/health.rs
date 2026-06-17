use serde::{Deserialize, Serialize};

/// Standard health-check response body returned by every service's `/health` endpoint.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct HealthResponse {
    /// The name of the service answering the request.
    pub service: String,
    /// A short human-readable status string (e.g. `"ok"`).
    pub status: String,
}

impl HealthResponse {
    /// Construct a healthy response for `service`.
    pub fn ok(service: impl Into<String>) -> Self {
        Self {
            service: service.into(),
            status: "ok".to_string(),
        }
    }
}
