package gg.orrery.atlas

/**
 * Rarity — SkyBlock item rarities, verbatim from ADR 0003 §1.
 *
 * SkyBlock encodes rarity on the last meaningful lore line as the leading word
 * in a pattern like "LEGENDARY SWORD", "RARE", "MYTHIC ACCESSORY". The rarity
 * keyword is always the first word on that line; any additional words describe
 * the item type (sword, armor, accessory, etc.). We match the first word of the
 * last non-blank lore line against the known rarity set, case-insensitive.
 * Falls back to UNKNOWN.
 */
enum class Rarity {
    COMMON, UNCOMMON, RARE, EPIC, LEGENDARY, MYTHIC, DIVINE, SPECIAL, UNKNOWN;

    companion object {
        private val RARITY_NAMES: Set<String> = values()
            .filter { it != UNKNOWN }
            .map { it.name }
            .toSet()

        /** Returns the Rarity matching [word] (case-insensitive), or null. */
        internal fun fromWord(word: String): Rarity? {
            val upper = word.uppercase()
            return if (upper in RARITY_NAMES) valueOf(upper) else null
        }
    }
}

/**
 * Parses the SkyBlock rarity from the item's lore lines and display name.
 *
 * SkyBlock places the rarity on the LAST non-blank lore line in the format
 * "RARITY [TYPE]" — e.g. "LEGENDARY SWORD", "RARE", "MYTHIC ACCESSORY".
 * The rarity keyword is always the FIRST word on that line. Falls back to
 * scanning the name if lore yields no match, then returns UNKNOWN.
 */
fun parseRarity(lore: List<String>, name: String): Rarity {
    // Walk lore in reverse to find the last non-blank line, then check name as fallback
    val candidates = lore.asReversed().filter { it.isNotBlank() } + listOf(name)
    for (line in candidates) {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) continue
        // The rarity keyword is the FIRST word on the line (e.g. "LEGENDARY SWORD" → LEGENDARY)
        val firstWord = trimmed.substringBefore(' ')
        val rarity = Rarity.fromWord(firstWord)
        if (rarity != null) return rarity
    }
    return Rarity.UNKNOWN
}
