package gg.orrery.atlas

/**
 * EntryType — semantic slot classification for SkyBlock container items.
 *
 * MC-agnostic: ZERO net.minecraft imports. Classification operates entirely on
 * the plain-string fields of [ParsedItem] (ADR 0003 §1, ADR 0005).
 *
 * §2 compliance: every signal used here derives from data the server already
 * sent (item id, name, lore). Nothing is fabricated.
 */
enum class EntryType {
    /** A glass-pane / blank-name decorative slot — hidden from the UI. */
    FILLER,

    /** "Back", "Go Back", "To <Menu>" — returns to a prior screen. */
    BACK,

    /** "Next Page", "Previous Page", "Page N" — list/grid navigation. */
    PAGINATION,

    /** Has a "Currently: …" or "Enabled/Disabled" lore line — on/off toggle. */
    TOGGLE,

    /** Nav entry with a "Click to view/open/browse…" action lore line; not a real SkyBlock item. */
    NAVIGATION,

    /** Has a skyblockId or a non-UNKNOWN rarity — a real SkyBlock gear/accessory/material item. */
    ITEM,

    /** Informational slot (stats, profile, purse display) with no click-action hint. */
    INFO,

    /** Fallback when no other rule matches. */
    OTHER,
}

// ── regex helpers ────────────────────────────────────────────────────────────

/** Matches stained-glass-pane item ids (any color). */
private val GLASS_PANE_PATTERN = Regex(""".*stained_glass_pane$""", RegexOption.IGNORE_CASE)

/**
 * True if [line] looks like an interaction hint ("Click to …", "Right-click …", etc.).
 * Used internally to distinguish NAVIGATION vs INFO entries.
 */
private val ACTION_HINT_PATTERN = Regex(
    """^(?:click|right-?click|left-?click|sneak)\s+to\s+(?:view|open|browse|visit|go|access|select|see)""",
    RegexOption.IGNORE_CASE,
)

/**
 * True if [line] contains an "x/y" or "N%" progress/count indicator.
 */
private val PROGRESS_PATTERN = Regex("""\d+\s*/\s*\d+|\d+(?:\.\d+)?%""")

/**
 * Matches "Currently: …" or standalone "Enabled" / "Disabled" toggle indicators.
 */
private val TOGGLE_PATTERN = Regex(
    """^Currently:\s*\S|^(?:Enabled|Disabled)$""",
    RegexOption.IGNORE_CASE,
)

/**
 * Matches "Next Page", "Previous Page", or "Page N" (N = one or more digits or a word).
 */
private val PAGINATION_PATTERN = Regex(
    """^(?:Next Page|Previous Page|Page\s+\S+)$""",
    RegexOption.IGNORE_CASE,
)

/**
 * Matches back-navigation titles: "Back", "Go Back", "To <X>", or a leading back-arrow (←, ◄).
 * "To " prefix (e.g. "To SkyBlock Menu") is a common SkyBlock pattern for navigation returns.
 */
private val BACK_PATTERN = Regex(
    """^(?:Back|Go Back|To\s+\S.*|[←◄].*)$""",
    RegexOption.IGNORE_CASE,
)

// ── classifyEntry ────────────────────────────────────────────────────────────

/**
 * Classifies a [ParsedItem] into an [EntryType] using data signals derived
 * exclusively from the item's id, name, and lore (§2 compliant).
 *
 * Rule order (first match wins):
 *
 * 1. **FILLER** — itemId ends with "stained_glass_pane" OR title is blank/punctuation-only.
 *    These decorative slots are invisible to the semantic UI.
 *
 * 2. **BACK** — title matches the back-navigation pattern
 *    ("Back", "Go Back", "To <Somewhere>", or back-arrow prefix).
 *
 * 3. **PAGINATION** — title matches "Next Page", "Previous Page", or "Page N".
 *
 * 4. **TOGGLE** — any lore line matches "Currently: <state>" or is bare "Enabled"/"Disabled".
 *
 * 5. **NAVIGATION** — any lore line starts with a "Click to view/open/browse/…" action hint
 *    AND the item has no skyblockId and rarity is UNKNOWN (it's a menu nav node, not an item).
 *
 * 6. **ITEM** — has a skyblockId OR a non-UNKNOWN rarity. A real SkyBlock item
 *    (gear, accessory, material, consumable) present in the container.
 *
 * 7. **INFO** — no click-action lore hint and no item signals; informational display only
 *    (e.g. a purse/stats/profile card).
 *
 * 8. **OTHER** — fallback for anything that doesn't fit the above.
 */
fun classifyEntry(item: ParsedItem): EntryType {
    val title = item.name.trim()

    // Rule 1: FILLER — glass pane by itemId, or blank/punctuation-only title
    if (item.itemId != null && GLASS_PANE_PATTERN.matches(item.itemId)) return EntryType.FILLER
    if (isFiller(title)) return EntryType.FILLER

    // Rule 2: BACK — back-navigation label
    if (BACK_PATTERN.matches(title)) return EntryType.BACK

    // Rule 3: PAGINATION — page navigation
    if (PAGINATION_PATTERN.matches(title)) return EntryType.PAGINATION

    // Rule 4: TOGGLE — lore line with a toggle state indicator
    if (item.lore.any { TOGGLE_PATTERN.containsMatchIn(it) }) return EntryType.TOGGLE

    // Rule 5: NAVIGATION — click-to-view lore hint + no item identity
    val hasActionHint = item.lore.any { ACTION_HINT_PATTERN.containsMatchIn(it) }
    if (hasActionHint && item.skyblockId == null && item.rarity == Rarity.UNKNOWN) {
        return EntryType.NAVIGATION
    }

    // Rule 6: ITEM — real SkyBlock item (has skyblockId or a meaningful rarity)
    if (item.skyblockId != null || item.rarity != Rarity.UNKNOWN) return EntryType.ITEM

    // Rule 7: INFO — no action hint, no item signals → informational card
    if (!hasActionHint) return EntryType.INFO

    // Rule 8: OTHER — fallback
    return EntryType.OTHER
}
