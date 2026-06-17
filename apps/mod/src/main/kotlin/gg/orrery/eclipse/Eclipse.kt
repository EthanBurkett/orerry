package gg.orrery.eclipse

/**
 * Eclipse — Menu override engine (DESIGN_SPEC §6.3, ADR 0003).
 *
 * Eclipse is the spine of Orrery's custom-UI system. Its three responsibilities are:
 *
 * 1. **Intercept** — a Mixin into the handled-screen lifecycle calls Atlas to fingerprint
 *    the incoming `ScreenHandler`. If a recognizer owns it, Eclipse suppresses the vanilla
 *    `HandledScreen` and opens an Orrery Lumen `Screen` instead.
 *
 * 2. **Present** — the Lumen `Screen` reads from the live `ScreenHandler` slot list and
 *    renders the Orrery custom UI using only design tokens (DESIGN_SPEC §5).
 *
 * 3. **Route** — every user click on an Orrery widget resolves to the backing slot index
 *    and is forwarded through [Interaction.clickSlot], the single compliance chokepoint
 *    (DESIGN_SPEC §2.3, §11; ADR 0003 §2). No other code path may call the vanilla
 *    `interactionManager.clickSlot` — doing so is a §11 violation caught by
 *    `ComplianceFitnessTest`.
 *
 * **Phase status:** the Mixin interception and the Lumen Screen are Phase 1 work (later
 * tasks). This object is the facade and doc anchor. [Interaction] (the chokepoint) and
 * the §11 fitness test are live as of Task A2.
 *
 * Compliance:
 * - Eclipse is the ONLY subsystem permitted to drive `interactionManager.clickSlot`, and
 *   only through [Interaction].
 * - Unrecognized menus fall through to vanilla untouched (degradation, never breakage).
 */
object Eclipse {

    /**
     * Registers Eclipse's built-in menu views (the recognizer-id -> Lumen-screen factories
     * in [EclipseViews]). Call once at client init, after Atlas recognizers are registered,
     * so the interception mixin has a live view to swap in.
     *
     * Phase 1 wires the SkyBlock main menu ("skyblock_menu") to the Orrery container screen.
     */
    fun registerDefaults() {
        EclipseViews.registerDefaults()
    }
}
