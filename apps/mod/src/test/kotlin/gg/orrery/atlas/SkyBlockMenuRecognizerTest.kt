package gg.orrery.atlas

import gg.orrery.atlas.recognizers.SkyBlockMenuRecognizer
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class SkyBlockMenuRecognizerTest {

    private fun menu(title: String, itemCount: Int = 54): ParsedMenu =
        ParsedMenu(
            title = title,
            size = itemCount,
            rows = itemCount / 9,
            items = List(itemCount) { null },
        )

    @Test
    fun `matches returns true for exact title 'SkyBlock Menu'`() {
        assertTrue(SkyBlockMenuRecognizer.matches(menu("SkyBlock Menu")))
    }

    @Test
    fun `matches returns false for different title`() {
        assertFalse(SkyBlockMenuRecognizer.matches(menu("Bazaar")))
    }

    @Test
    fun `matches returns false for partial title`() {
        assertFalse(SkyBlockMenuRecognizer.matches(menu("SkyBlock")))
        assertFalse(SkyBlockMenuRecognizer.matches(menu("Menu")))
    }

    @Test
    fun `matches returns false for empty title`() {
        assertFalse(SkyBlockMenuRecognizer.matches(menu("")))
    }

    @Test
    fun `matches is case-sensitive`() {
        // Title matching should be exact — Hypixel sends the exact casing.
        assertFalse(SkyBlockMenuRecognizer.matches(menu("skyblock menu")))
        assertFalse(SkyBlockMenuRecognizer.matches(menu("SKYBLOCK MENU")))
    }

    @Test
    fun `id is skyblock_menu`() {
        assertEquals("skyblock_menu", SkyBlockMenuRecognizer.id)
    }
}
