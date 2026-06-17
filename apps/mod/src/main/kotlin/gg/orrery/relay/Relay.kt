package gg.orrery.relay

/**
 * Relay — Loopback bridge to the launcher (stub, Phase 0).
 *
 * Phase 3 will implement: a WebSocket client to Observatory's loopback-only WS server.
 * Carries: account/session status, cosmetics manifest, live config pushes, and opt-in
 * telemetry events. Loopback only — never exposed off-device (DESIGN_SPEC §6.8).
 *
 * Messages are typed by packages/protocol (Kotlin mirror of the TS types).
 */
object Relay
