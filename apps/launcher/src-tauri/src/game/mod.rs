//! # game — version resolution, asset download, launch pipeline
//!
//! Responsible for:
//! - Resolving the target Minecraft version manifest from Mojang's version API.
//! - Downloading the official client jar, libraries, and assets.
//! - Assembling the full classpath (LWJGL, natives, Fabric loader, Orrery mod).
//! - Spawning the JVM and handing off to the Relay loopback WS.
//!
//! # Compliance invariant (DESIGN_SPEC §7.2)
//! **Bring-your-own legitimate game files; never redistribute Mojang code.**
//! This module *downloads* official Mojang artifacts from Mojang's own CDN at
//! runtime using the user's entitlement. It never bundles, ships, or caches
//! those files in the Orrery distribution. The launcher distribution contains
//! only Orrery's own code and assets.
//!
//! **Phase 0 stub** — module compiles; no real launch logic yet.
//! Phase 1 will add `VersionManifest`, `AssetIndex`, and `LaunchConfig`.
