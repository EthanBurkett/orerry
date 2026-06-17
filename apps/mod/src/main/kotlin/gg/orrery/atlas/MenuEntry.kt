package gg.orrery.atlas

/**
 * MenuEntry — a single semantic entry extracted from a [ParsedMenu].
 *
 * Produced by [parseEntries]; consumed by SkyBlockMenuView to render entry cards.
 *
 * MC-agnostic: ZERO net.minecraft imports. Operates only on [ParsedMenu]/[ParsedItem]
 * plain data (ADR 0005, ADR 0003 §1).
 *
 * §2 / §6.5 compliance: all fields derive exclusively from data already present in
 * [ParsedMenu]; nothing is fabricated.
 */
data class MenuEntry(
    /** Backing ScreenHandler slot index — used for icon rendering + click routing. */
    val backingSlot: Int,
    /** Primary label. § codes and click-hints are stripped. */
    val title: String,
    /**
     * Context-dependent secondary lore line selected by [EntryType]:
     * - NAVIGATION  → the action line ("Browse known recipes")
     * - ITEM (gear) → first stat-bearing line (contains a stat name or a +N / number)
     * - collection/progress → first line with "x/y" or "%"
     * - default     → first descriptive line that is not blank, not a rarity line,
     *                 and not a pure click-hint line.
     * null when no suitable line exists.
     */
    val subtitle: String?,
    /** Rarity derived via [deriveRarity] (ExtraAttributes > lore line > name color). */
    val rarity: Rarity,
    /** Semantic slot classification — used by the view to route rendering / filtering. */
    val type: EntryType,
)

// ── rarity-line heuristic ────────────────────────────────────────────────────

/**
 * Returns true when [line] looks like a SkyBlock rarity line.
 *
 * Heuristic: the line's words are all UPPERCASE and the first token matches a
 * known [Rarity] name (e.g. "LEGENDARY SWORD", "RARE", "EPIC ACCESSORY").
 * Lines that merely contain an upper-case word mixed with lower-case (e.g.
 * "Right-click to use!") are NOT matched.
 */
internal fun isRarityLine(line: String): Boolean {
    val trimmed = line.trim()
    if (trimmed.isEmpty()) return false
    // All characters that are letters must be upper-case (digits/spaces/hyphens ok)
    if (trimmed.any { it.isLetter() && it.isLowerCase() }) return false
    val firstToken = trimmed.substringBefore(' ')
    return Rarity.fromWord(firstToken) != null
}

// ── filler heuristic ─────────────────────────────────────────────────────────

/**
 * Returns true when the item's title indicates it is decorative filler and
 * should be excluded from semantic entries.
 *
 * Filler items are those whose name is:
 * - blank / whitespace-only, OR
 * - composed entirely of non-letter, non-digit characters (punctuation-only,
 *   e.g. "·····", "-----", "     ").
 */
internal fun isFiller(title: String): Boolean {
    val trimmed = title.trim()
    if (trimmed.isEmpty()) return true
    // A title consisting solely of punctuation/symbols with no letter or digit is filler
    return trimmed.none { it.isLetterOrDigit() }
}

// ── click-hint lore line heuristic ───────────────────────────────────────────

/**
 * Returns true when [line] is a standalone interaction hint in the lore
 * (e.g. "CLICK to view!", "Right-click to open."), and therefore should
 * not be used as a subtitle.
 *
 * Distinct from [stripClickHints] which operates on titles: this detects
 * entire lore lines that are only a click instruction.
 */
private val LORE_HINT_PATTERN = Regex(
    """^(?:click|right-?click|left-?click|sneak)""",
    RegexOption.IGNORE_CASE,
)

private fun isClickHintLine(line: String): Boolean = LORE_HINT_PATTERN.containsMatchIn(line.trim())

// ── stat-bearing line heuristic ───────────────────────────────────────────────

/**
 * Returns true when [line] looks like a stat-bearing lore line (for ITEM entries).
 *
 * A line is stat-bearing if it contains:
 * - a numeric modifier like "+100" or "-50", OR
 * - a keyword from SkyBlock's common stat names.
 */
private val STAT_KEYWORDS = setOf(
    "Damage", "Defense", "Health", "Speed", "Strength", "Intelligence",
    "Crit Chance", "Crit Damage", "Attack Speed", "Ferocity", "Magic Find",
    "Mining Speed", "Mining Fortune", "Farming Fortune", "Foraging Fortune",
    "Sea Creature Chance", "Pet Luck", "True Defense", "Vitality",
)

private val STAT_NUMBER_PATTERN = Regex("""[+\-]\d+""")

private fun isStatLine(line: String): Boolean {
    if (STAT_NUMBER_PATTERN.containsMatchIn(line)) return true
    return STAT_KEYWORDS.any { line.contains(it, ignoreCase = true) }
}

// ── progress/count line heuristic ─────────────────────────────────────────────

private val PROGRESS_PATTERN_SUBTITLE = Regex("""\d+\s*/\s*\d+|\d+(?:\.\d+)?%""")

private fun isProgressLine(line: String): Boolean =
    PROGRESS_PATTERN_SUBTITLE.containsMatchIn(line)

// ── subtitle selector ─────────────────────────────────────────────────────────

/**
 * Selects the best subtitle lore line for an entry of the given [type].
 *
 * Rules (applied in order):
 * - NAVIGATION → first lore line that contains an action verb ("view", "open", "browse", etc.)
 * - ITEM (with lore indicating a progress/collection context) → first "x/y" or "%" line
 * - ITEM (gear/stat context) → first stat-bearing line (+N or stat keyword)
 * - ITEM (default) → first descriptive line (non-blank, non-rarity, non-click-hint)
 * - collection (title or any lore implies a count) → first "x/y" or "%" line
 * - default → first non-blank, non-rarity, non-click-hint descriptive line
 */
private val NAV_ACTION_PATTERN = Regex(
    """(?:view|open|browse|visit|go|access|select|see)""",
    RegexOption.IGNORE_CASE,
)

internal fun selectSubtitle(type: EntryType, lore: List<String>): String? {
    if (lore.isEmpty()) return null

    return when (type) {
        EntryType.NAVIGATION -> {
            // Prefer the action line — the lore line that describes what clicking does
            lore.firstOrNull { line ->
                line.isNotBlank() && NAV_ACTION_PATTERN.containsMatchIn(line)
            } ?: lore.firstOrNull { it.isNotBlank() && !isRarityLine(it) && !isClickHintLine(it) }
        }

        EntryType.ITEM -> {
            // If any lore line has a progress pattern (x/y, %) prefer that
            val progressLine = lore.firstOrNull { it.isNotBlank() && isProgressLine(it) }
            if (progressLine != null) return progressLine

            // Otherwise pick the first stat-bearing line
            val statLine = lore.firstOrNull { it.isNotBlank() && isStatLine(it) }
            if (statLine != null) return statLine

            // Fallback: first descriptive line
            lore.firstOrNull { line ->
                line.isNotBlank() && !isRarityLine(line) && !isClickHintLine(line)
            }
        }

        else -> {
            // For BACK, PAGINATION, TOGGLE, INFO, OTHER, FILLER (filtered before reaching here):
            // Check for progress line first (collection/profile type entries)
            val progressLine = lore.firstOrNull { it.isNotBlank() && isProgressLine(it) }
            if (progressLine != null) return progressLine

            // Default: first descriptive line
            lore.firstOrNull { line ->
                line.isNotBlank() && !isRarityLine(line) && !isClickHintLine(line)
            }
        }
    }
}

// ── parseEntries ─────────────────────────────────────────────────────────────

/**
 * Converts a [ParsedMenu] into an ordered list of semantic [MenuEntry] values.
 *
 * Rules (from ADR 0005):
 * 1. Null slots (empty chest slots) are skipped.
 * 2. Filler/decorative items are dropped — [classifyEntry] returns FILLER for
 *    glass-pane itemIds and blank/punctuation-only names. FILLER entries are excluded.
 * 3. [MenuEntry.title] = [ParsedItem.name] with click-hints stripped via [stripClickHints].
 * 4. [MenuEntry.type] = result of [classifyEntry].
 * 5. [MenuEntry.rarity] = result of [deriveRarity] (ExtraAttributes > lore > name color).
 * 6. [MenuEntry.subtitle] = selected by [selectSubtitle] according to the entry type.
 * 7. [MenuEntry.backingSlot] = [ParsedItem.slot].
 * 8. Entries are returned in ascending slot order (items list is already ordered).
 *
 * Deterministic and side-effect free.
 */
fun parseEntries(menu: ParsedMenu): List<MenuEntry> =
    menu.items
        .filterNotNull()
        .mapNotNull { item ->
            val type = classifyEntry(item)
            // Drop FILLER entries — punch-list: hide decorative glass-pane slots
            if (type == EntryType.FILLER) return@mapNotNull null

            val cleanTitle = stripClickHints(item.name)
            val rarity = deriveRarity(item)
            val subtitle = selectSubtitle(type, item.lore)

            MenuEntry(
                backingSlot = item.slot,
                title = cleanTitle,
                subtitle = subtitle,
                rarity = rarity,
                type = type,
            )
        }
