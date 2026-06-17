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

        /**
         * SkyBlock name-color → Rarity mapping.
         *
         * SkyBlock uses a specific § color code as the FIRST character of a
         * custom item display name to encode rarity. This is the mapping:
         *
         * | § code | MC color  | Rarity    |
         * |--------|-----------|-----------|
         * | §f     | White     | COMMON    |
         * | §a     | Green     | UNCOMMON  |
         * | §9     | Blue      | RARE      |
         * | §5     | Purple    | EPIC      |
         * | §6     | Gold      | LEGENDARY |
         * | §d     | Magenta   | MYTHIC    |
         * | §b     | Aqua      | DIVINE    |
         * | §c     | Red       | SPECIAL   |
         *
         * Note: §l (bold) is typically prepended to the color code on rarity lore
         * lines but the color itself is the discriminator. §r/§e/§7 appear on
         * non-rarity names (item type description, meta, etc.) and yield UNKNOWN.
         *
         * Source: SkyBlock's observed encoding, corroborated by SkyHanni / Skyblocker
         * source references. §2 compliant — derived from data the server sent.
         */
        internal val NAME_COLOR_TO_RARITY: Map<Char, Rarity> = mapOf(
            'f' to COMMON,
            'F' to COMMON,
            'a' to UNCOMMON,
            'A' to UNCOMMON,
            '9' to RARE,
            '5' to EPIC,
            '6' to LEGENDARY,
            'd' to MYTHIC,
            'D' to MYTHIC,
            'b' to DIVINE,
            'B' to DIVINE,
            'c' to SPECIAL,
            'C' to SPECIAL,
        )

        /**
         * Derives rarity from a raw name (with § codes intact) by reading
         * the leading § color code (after skipping any §l bold or §r resets).
         * Returns null if the leading color doesn't map to a known rarity.
         */
        internal fun fromRawName(rawName: String): Rarity? {
            // Walk the raw name, skip §l / §r / other non-color codes until
            // we hit a § color code that is in our rarity map.
            var i = 0
            while (i < rawName.length - 1) {
                if (rawName[i] == '§') {
                    val code = rawName[i + 1]
                    val rarity = NAME_COLOR_TO_RARITY[code]
                    if (rarity != null) return rarity
                    // Skip non-color or unmapped code; continue scanning
                    i += 2
                } else {
                    // Hit a non-§ character — no color code prefix found
                    break
                }
            }
            return null
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

/**
 * Derives the most-reliable rarity for [item] using a four-tier priority chain:
 *
 * 1. **ExtraAttributes "rarity" / "tier"** — explicit NBT field set by Hypixel.
 *    Most authoritative; overrides lore and name color.
 * 2. **Lore rarity line** — the "LEGENDARY SWORD" / "RARE" pattern on the last
 *    meaningful lore line, parsed by [parseRarity]. Covers the vast majority of items.
 * 3. **rawName leading § color** — SkyBlock encodes rarity as the first § color in
 *    the custom display name (e.g. §6 = LEGENDARY, §5 = EPIC). Used as a last resort
 *    when no lore line is present (e.g. nav/filler items that have a colored name).
 * 4. **item.rarity** — the rarity already parsed and stored on the [ParsedItem] by
 *    [parseRarity] in AtlasAdapter. Used as a final fallback so that pre-parsed rarity
 *    values are not discarded (backward compatibility; data-faithful).
 *
 * Falls back to [Rarity.UNKNOWN] when no tier yields a match.
 *
 * §2 compliance: every tier reads only data the server already sent in the item NBT /
 * display name / lore — nothing is fabricated.
 */
fun deriveRarity(item: ParsedItem): Rarity {
    // Tier 1: explicit ExtraAttributes "rarity" or "tier" field
    val explicitRarity = (item.extraAttributes["rarity"] ?: item.extraAttributes["tier"])
        ?.let { Rarity.fromWord(it) }
    if (explicitRarity != null) return explicitRarity

    // Tier 2: lore rarity line (last non-blank lore line starting with a rarity keyword)
    val loreRarity = parseRarity(item.lore, "")  // pass "" for name — we handle name below
    if (loreRarity != Rarity.UNKNOWN) return loreRarity

    // Tier 3: rawName leading § color → rarity (e.g. §6§lSword → LEGENDARY)
    val nameColorRarity = Rarity.fromRawName(item.rawName)
    if (nameColorRarity != null) return nameColorRarity

    // Tier 4: pre-parsed item.rarity (already computed by parseRarity in AtlasAdapter)
    if (item.rarity != Rarity.UNKNOWN) return item.rarity

    return Rarity.UNKNOWN
}
