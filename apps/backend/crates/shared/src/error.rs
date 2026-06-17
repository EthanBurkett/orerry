use axum::{
    http::StatusCode,
    response::{IntoResponse, Response},
    Json,
};
use serde_json::json;
use thiserror::Error;

/// Unified error type for all Orrery backend services.
///
/// Each variant maps to an HTTP status code and a JSON error body.
/// Add more variants as real service logic is introduced.
#[derive(Debug, Error)]
pub enum AppError {
    /// A required resource was not found.
    #[error("not found: {0}")]
    NotFound(String),

    /// The caller is not authenticated.
    #[error("unauthorized")]
    Unauthorized,

    /// The request parameters are invalid.
    #[error("bad request: {0}")]
    BadRequest(String),

    /// An unexpected internal failure.
    #[error("internal error: {0}")]
    Internal(String),
}

impl IntoResponse for AppError {
    fn into_response(self) -> Response {
        let (status, message) = match &self {
            AppError::NotFound(msg) => (StatusCode::NOT_FOUND, msg.clone()),
            AppError::Unauthorized => (StatusCode::UNAUTHORIZED, "unauthorized".to_string()),
            AppError::BadRequest(msg) => (StatusCode::BAD_REQUEST, msg.clone()),
            AppError::Internal(msg) => (StatusCode::INTERNAL_SERVER_ERROR, msg.clone()),
        };

        let body = Json(json!({
            "error": message,
            "status": status.as_u16(),
        }));

        (status, body).into_response()
    }
}
