package gg.orrery.atlas

/**
 * MenuRegistry — first-match registry of [MenuRecognizer]s.
 *
 * Verbatim from ADR 0003 §1: first match wins; null = fall through to vanilla.
 *
 * Design for Ephemeris-driven remote disable: each registration carries an
 * [enabled] flag (defaults true). Disabled recognizers are skipped by
 * [recognize] without being removed — they can be re-enabled without
 * re-registering. A bad parse therefore degrades to vanilla, never breaks
 * (DESIGN_SPEC §6.5).
 */
object MenuRegistry {

    private data class Entry(
        val recognizer: MenuRecognizer,
        var enabled: Boolean = true,
    )

    private val entries = mutableListOf<Entry>()

    /**
     * Registers [recognizer] with the given [enabled] state (default true).
     *
     * Recognizers are checked in registration order; the first match wins.
     */
    fun register(recognizer: MenuRecognizer, enabled: Boolean = true) {
        entries.add(Entry(recognizer, enabled))
    }

    /**
     * Sets the enabled flag for the recognizer with [id].
     *
     * Used by Ephemeris remote-disable: flagging a stale recognizer off causes
     * [recognize] to skip it and fall through to vanilla, preventing broken
     * menu parses from reaching users.
     */
    fun setEnabled(id: String, enabled: Boolean) {
        entries.find { it.recognizer.id == id }?.enabled = enabled
    }

    /**
     * Returns the first enabled recognizer whose [MenuRecognizer.matches]
     * returns true for [menu], or null if none matches (fall through to vanilla).
     *
     * Exceptions inside a recognizer's [matches] call are caught and logged;
     * that recognizer is treated as non-matching so the rest are still checked.
     */
    fun recognize(menu: ParsedMenu): MenuRecognizer? {
        for (entry in entries) {
            if (!entry.enabled) continue
            val matched = try {
                entry.recognizer.matches(menu)
            } catch (e: Exception) {
                // A buggy recognizer must never crash the pipeline; degrade gracefully.
                false
            }
            if (matched) return entry.recognizer
        }
        return null
    }

    /** All registered recognizers (enabled and disabled), in registration order. */
    val all: List<MenuRecognizer>
        get() = entries.map { it.recognizer }

    /** Clears all registered recognizers. Intended for tests only. */
    internal fun reset() {
        entries.clear()
    }
}
