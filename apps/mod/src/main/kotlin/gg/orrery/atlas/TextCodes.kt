package gg.orrery.atlas

/**
 * TextCodes — strips Minecraft § color/format codes from strings.
 *
 * MC uses § followed by a single hex digit or letter as a formatting code.
 * This is purely string processing — no MC imports needed.
 */

private val SECTION_PATTERN = Regex("§[0-9a-fk-or]", RegexOption.IGNORE_CASE)

/**
 * Strips all Minecraft § color/format codes from [s], returning the plain text.
 */
fun stripCodes(s: String): String = SECTION_PATTERN.replace(s, "")
