package gg.orrery.atlas

/**
 * MenuRecognizer — interface for fingerprinting a parsed SkyBlock menu.
 *
 * Recognizers match on title pattern + key-slot items, NOT absolute layout,
 * so they survive minor Hypixel changes (DESIGN_SPEC §6.5).
 *
 * Verbatim from ADR 0003 §1.
 */
interface MenuRecognizer {
    /** Stable identifier, e.g. "skyblock_menu". */
    val id: String

    /**
     * Returns true if this recognizer claims ownership of [menu].
     *
     * Implementations should check title pattern and/or key-slot item signatures.
     * They must never throw — a recognizer that errors is treated as non-matching.
     */
    fun matches(menu: ParsedMenu): Boolean
}
