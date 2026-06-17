package gg.orrery.eclipse

/**
 * Eclipse — Menu override engine (stub, Phase 0).
 *
 * Phase 1 will: intercept container opens, fingerprint menus via Atlas,
 * suppress vanilla HandledScreen, drive the Lumen custom Screen, and
 * route all user input through the single clickSlot chokepoint (DESIGN_SPEC §6.3, §11).
 *
 * Compliance: Eclipse is the ONLY place that may call interactionManager.clickSlot.
 * No other subsystem may emit interaction packets. (§2, §11)
 */
object Eclipse
