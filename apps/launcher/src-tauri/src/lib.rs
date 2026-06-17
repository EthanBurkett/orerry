// Observatory — Tauri 2 Rust core
// Module tree. Each module dir is a Phase 0 stub with doc comments only;
// real logic will be added in the phases that own each subsystem.

pub mod auth;
pub mod game;
pub mod ipc;
pub mod profiles;
pub mod updater;

/// Returns the running Observatory (launcher) version string.
///
/// Phase 0 stub: returns the version from `Cargo.toml` via the
/// `CARGO_PKG_VERSION` env baked in at compile time.
///
/// Registered as a Tauri command so the frontend↔Rust IPC path is exercised
/// end-to-end from Phase 0.
#[tauri::command]
fn app_version() -> String {
    env!("CARGO_PKG_VERSION").to_string()
}

/// Build and run the Tauri application.
///
/// Called by `main.rs`. All command registration lives here so the binary
/// crate stays thin and commands can be tested without a Tauri context.
#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .invoke_handler(tauri::generate_handler![app_version])
        .run(tauri::generate_context!())
        .expect("error while running Observatory");
}
