package gg.orrery.atlas

/**
 * TextCodes — strips Minecraft § color/format codes and interaction hints from strings.
 *
 * MC uses § followed by a single hex digit or letter as a formatting code.
 * This is purely string processing — no MC imports needed.
 */

private val SECTION_PATTERN = Regex("§[0-9a-fk-or]", RegexOption.IGNORE_CASE)

/**
 * Strips all Minecraft § color/format codes from [s], returning the plain text.
 */
fun stripCodes(s: String): String = SECTION_PATTERN.replace(s, "")

/**
 * Interaction hint patterns to strip from item titles (and menu titles).
 *
 * SkyBlock appends or embeds click-action text in item display names:
 * - Trailing parenthetical: "(Click)", "(Right Click)", "(Left Click)"
 * - Trailing or inline imperative: "Click to …", "Right-click to …",
 *   "Right-click …", "Sneak + click …", "Sneak to …"
 *
 * These are stripped before display so the rendered title is clean.
 * The regex is applied AFTER § code stripping (codes are already gone).
 *
 * Priority: trailing patterns are removed first (greedy-last-match),
 * then any remaining inline leading hints.
 */
private val CLICK_HINT_PATTERNS = listOf(
    // Trailing parenthetical hints: " (Click)", " (Right Click)", "(Sneak + Right-click)", etc.
    Regex("""\s*\([^)]*(?:click|right.click|left.click|sneak)[^)]*\)$""", RegexOption.IGNORE_CASE),
    // Trailing "Click to …" / "Right-click to …" / "Sneak+click to …" (no parens)
    Regex("""\s*(?:sneak\s*\+?\s*)?(?:right-?click|left-?click|click)\s+to\s+\S.*$""", RegexOption.IGNORE_CASE),
    // Trailing "Right-click <verb>" or "Click <verb>" without "to"
    Regex("""\s*(?:right-?click|left-?click)\s+\S.*$""", RegexOption.IGNORE_CASE),
    // Trailing "Sneak to …" or "Sneak + …"
    Regex("""\s*sneak\s*(?:\+\s*)?\S.*$""", RegexOption.IGNORE_CASE),
)

/**
 * Removes trailing or inline interaction hints from [s] and trims the result.
 *
 * Examples:
 * - "SkyBlock Menu (Click)"      → "SkyBlock Menu"
 * - "Open Bazaar (Right Click)"  → "Open Bazaar"
 * - "Go Back"                    → "Go Back"  (no change)
 * - "Click to open"              → ""  (entire string was a hint; callers decide how to handle)
 *
 * This is MC-agnostic (no net.minecraft.* imports). It operates on plain stripped strings;
 * call stripCodes first if § codes may be present.
 */
fun stripClickHints(s: String): String {
    var result = s.trim()
    for (pattern in CLICK_HINT_PATTERNS) {
        val replaced = pattern.replace(result, "").trim()
        if (replaced != result) {
            result = replaced
            break  // Apply at most one pattern per call (they are ordered by specificity)
        }
    }
    return result
}
