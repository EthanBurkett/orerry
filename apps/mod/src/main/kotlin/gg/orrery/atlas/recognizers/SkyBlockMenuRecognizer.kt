package gg.orrery.atlas.recognizers

import gg.orrery.atlas.MenuRecognizer
import gg.orrery.atlas.ParsedMenu

/**
 * SkyBlockMenuRecognizer — fingerprints the SkyBlock main menu.
 *
 * Matches exactly when the menu title (§ codes already stripped by Atlas)
 * equals "SkyBlock Menu". This is the first recognizer shipped for Phase 1.
 *
 * Per DESIGN_SPEC §6.5: recognizers must be resilient to minor Hypixel changes.
 * Title-only matching is intentionally minimal here; key-slot item signatures
 * can be added in Phase 2 if Hypixel reuses this title on a different layout.
 */
object SkyBlockMenuRecognizer : MenuRecognizer {

    override val id: String = "skyblock_menu"

    override fun matches(menu: ParsedMenu): Boolean =
        menu.title == "SkyBlock Menu"
}
