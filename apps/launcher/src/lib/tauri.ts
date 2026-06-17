/**
 * Typed wrappers around Tauri IPC commands.
 *
 * All Tauri commands are invoked via `@tauri-apps/api/core`'s `invoke`.
 * This module is the only place in the Svelte UI that calls `invoke` directly;
 * components use these typed helpers so the command names and payload shapes
 * are never scattered across the codebase.
 *
 * Commands are stubs at Phase 0 — the Rust side returns static data.
 * Expand these alongside the Rust core modules (auth/, game/, etc.).
 */
import { invoke } from '@tauri-apps/api/core';

/** Returns the running Observatory version string. */
export async function getAppVersion(): Promise<string> {
  return invoke<string>('app_version');
}
