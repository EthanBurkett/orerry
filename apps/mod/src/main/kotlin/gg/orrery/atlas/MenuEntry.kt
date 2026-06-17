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
    /** Primary label. § codes are already stripped on [ParsedItem.name]. */
    val title: String,
    /**
     * First descriptive lore line that is non-blank and is NOT the rarity line,
     * or null when no such line exists.
     */
    val subtitle: String?,
    /** Rarity parsed from lore (already present on [ParsedItem.rarity]). */
    val rarity: Rarity,
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

// ── parseEntries ─────────────────────────────────────────────────────────────

/**
 * Converts a [ParsedMenu] into an ordered list of semantic [MenuEntry] values.
 *
 * Rules (from ADR 0005):
 * 1. Null slots (empty chest slots) are skipped.
 * 2. Filler/decorative items are skipped via [isFiller].
 * 3. [MenuEntry.title] = [ParsedItem.name] (§ codes already stripped).
 * 4. [MenuEntry.subtitle] = the first lore line that is non-blank AND is not a
 *    rarity line (detected by [isRarityLine]). Null if no such line exists.
 * 5. [MenuEntry.rarity] = [ParsedItem.rarity].
 * 6. [MenuEntry.backingSlot] = [ParsedItem.slot].
 * 7. Entries are returned in ascending slot order (items list is already ordered).
 *
 * Deterministic and side-effect free.
 */
fun parseEntries(menu: ParsedMenu): List<MenuEntry> =
    menu.items
        .filterNotNull()
        .filter { !isFiller(it.name) }
        .map { item ->
            val subtitle = item.lore
                .firstOrNull { line -> line.isNotBlank() && !isRarityLine(line) }
            MenuEntry(
                backingSlot = item.slot,
                title = item.name,
                subtitle = subtitle,
                rarity = item.rarity,
            )
        }
