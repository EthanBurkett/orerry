//! # shared
//!
//! Common types, errors, and utilities shared across all Orrery backend crates.
//!
//! ## Contents
//! - [`AppError`] — unified error type with [`axum::response::IntoResponse`] impl
//! - [`HealthResponse`] — the standard health-check JSON shape
//! - [`prelude`] — re-exports for convenient use in service crates

pub mod error;
pub mod health;

/// Re-exports for convenient glob import in service crates.
///
/// ```rust
/// use shared::prelude::*;
/// ```
pub mod prelude {
    pub use crate::error::AppError;
    pub use crate::health::HealthResponse;
    pub use axum::{Json, Router, routing::get};
    pub use serde::{Deserialize, Serialize};
}
