package gg.orrery.atlas

import gg.orrery.atlas.recognizers.SkyBlockMenuRecognizer

/**
 * Atlas — SkyBlock data model and menu fingerprinting facade (Phase 1).
 *
 * Atlas parses SkyBlock container items + NBT + lore into typed structures
 * ([ParsedItem], [ParsedMenu]) and fingerprints menus via [MenuRegistry] so
 * Eclipse knows what it is looking at (DESIGN_SPEC §6.3, §6.5, ADR 0003).
 *
 * Compliance (§2 / §6.5): Atlas ONLY exposes data parsed from containers the
 * server already sent. It never fabricates hidden information, never issues
 * extra network requests, and never reads data the player cannot see.
 *
 * MC-agnostic: this file has zero net.minecraft.* imports. The MC boundary
 * lives entirely in [AtlasAdapter]. The convenience entry point that accepts
 * MC types ([AtlasAdapter.parseAndRecognize]) is on [AtlasAdapter] itself so
 * this facade stays unit-testable without a game classpath (ADR 0003 §1).
 */
object Atlas {

    /**
     * Registers all built-in recognizers into [MenuRegistry].
     *
     * Call this once during mod initialization (e.g. from Eclipse setup).
     * Recognizers can be disabled later via [MenuRegistry.setEnabled] without
     * re-registering — a stale recognizer disabled by Ephemeris degrades to
     * vanilla, never breaks (DESIGN_SPEC §6.5).
     */
    fun registerDefaultRecognizers() {
        MenuRegistry.register(SkyBlockMenuRecognizer)
    }
}
