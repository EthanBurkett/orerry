package gg.orrery.lumen

import net.minecraft.util.Identifier

/**
 * LumenFonts — Orrery's custom typeface identifiers (ADR 0005 §3, DESIGN_SPEC §5.3).
 *
 * Each identifier resolves to a Minecraft TTF font-provider JSON under
 * `assets/orrery/font/<name>.json`, which in turn loads a static TrueType file from
 * `assets/orrery/font/<file>.ttf`. Minecraft's native font pipeline (TextRenderer) renders
 * glyphs from these providers when text is styled with one of these identifiers.
 *
 * This is the "native MC TTF provider" approach mandated by ADR 0005 §3 — no MSDF GL
 * shader, no custom render pipeline. The vanilla TextRenderer handles the rasterisation;
 * Orrery supplies the font files and the identifier so the pixel font is never used.
 *
 * ## Font mapping (§5.3)
 * - [DISPLAY] → Space Grotesk Medium (`space_grotesk.ttf`) — headings, key UI, the
 *   "technical-instrument character" of the identity.
 * - [BODY]    → Inter Regular (`inter.ttf`, variable font loaded as static; see NOTE below)
 *   — prose, labels, most UI text.
 * - [MONO]    → JetBrains Mono Regular (`jetbrains_mono.ttf`) — prices, stats, counts,
 *   tabular numerals.
 *
 * NOTE on Inter: the Inter v4.1 release ships only a variable-axis TTF (InterVariable.ttf)
 * and a TTC collection; no isolated static-weight TTF is available without extracting from
 * the TTC. The variable TTF is valid TrueType (magic 00 01 00 00) and loads correctly via
 * Java's Font.createFont, which MC's TTF provider calls internally. At render size 11px the
 * variable font defaults to the Regular axis position. If a future MC version or JVM rejects
 * variable fonts, the fallback is to replace inter.ttf with space_grotesk.ttf and update
 * body.json accordingly.
 *
 * ## Yarn 1.21.11
 * - [Identifier.of] is `method_60655` in Yarn 1.21.11 — the two-arg static factory.
 *   The old constructor `new Identifier(namespace, path)` is deprecated in 1.21.x.
 */
object LumenFonts {

    /**
     * Display typeface — Space Grotesk Medium.
     * Use for menu titles, headings, the Orrery wordmark, and key UI labels (§5.3).
     * Font JSON: `assets/orrery/font/display.json` → `assets/orrery/font/space_grotesk.ttf`.
     */
    val DISPLAY: Identifier = Identifier.of("orrery", "display")

    /**
     * Body typeface — Inter Regular (variable font, defaults to Regular axis at render scale).
     * Use for prose, labels, secondary text, and most UI copy (§5.3).
     * Font JSON: `assets/orrery/font/body.json` → `assets/orrery/font/inter.ttf`.
     */
    val BODY: Identifier = Identifier.of("orrery", "body")

    /**
     * Monospace typeface — JetBrains Mono Regular.
     * Use for prices, stats, counts, and any data that benefits from tabular numerals (§5.3).
     * Font JSON: `assets/orrery/font/mono.json` → `assets/orrery/font/jetbrains_mono.ttf`.
     */
    val MONO: Identifier = Identifier.of("orrery", "mono")
}
