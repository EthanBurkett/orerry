package gg.orrery.atlas

import gg.orrery.atlas.recognizers.SkyBlockMenuRecognizer
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertNotNull

class MenuRegistryTest {

    // Reset registry state between tests so they are isolated.
    @BeforeTest
    fun setUp() {
        MenuRegistry.reset()
    }

    @AfterTest
    fun tearDown() {
        MenuRegistry.reset()
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private fun menu(title: String, size: Int = 54): ParsedMenu =
        ParsedMenu(title = title, size = size, rows = size / 9, items = List(size) { null })

    private fun stubRecognizer(id: String, titleMatch: String): MenuRecognizer =
        object : MenuRecognizer {
            override val id = id
            override fun matches(menu: ParsedMenu) = menu.title == titleMatch
        }

    // ── recognize: null when empty ────────────────────────────────────────────

    @Test
    fun `recognize returns null when registry is empty`() {
        assertNull(MenuRegistry.recognize(menu("SkyBlock Menu")))
    }

    // ── recognize: first match wins ───────────────────────────────────────────

    @Test
    fun `recognize returns first matching recognizer`() {
        val first = stubRecognizer("first", "SkyBlock Menu")
        val second = stubRecognizer("second", "SkyBlock Menu")
        MenuRegistry.register(first)
        MenuRegistry.register(second)

        val result = MenuRegistry.recognize(menu("SkyBlock Menu"))
        assertNotNull(result)
        assertEquals("first", result.id)
    }

    @Test
    fun `recognize returns correct recognizer when first does not match`() {
        val bazaar = stubRecognizer("bazaar", "Bazaar")
        val skyblock = stubRecognizer("skyblock_menu", "SkyBlock Menu")
        MenuRegistry.register(bazaar)
        MenuRegistry.register(skyblock)

        val result = MenuRegistry.recognize(menu("SkyBlock Menu"))
        assertNotNull(result)
        assertEquals("skyblock_menu", result.id)
    }

    // ── recognize: null on no match ───────────────────────────────────────────

    @Test
    fun `recognize returns null when no recognizer matches`() {
        MenuRegistry.register(stubRecognizer("bazaar", "Bazaar"))
        assertNull(MenuRegistry.recognize(menu("Unknown Menu")))
    }

    // ── disabled recognizers are skipped ─────────────────────────────────────

    @Test
    fun `disabled recognizer is skipped even if it would match`() {
        MenuRegistry.register(SkyBlockMenuRecognizer, enabled = false)
        assertNull(MenuRegistry.recognize(menu("SkyBlock Menu")))
    }

    @Test
    fun `recognize falls through to next when first is disabled`() {
        val first = stubRecognizer("first", "SkyBlock Menu")
        val second = stubRecognizer("second", "SkyBlock Menu")
        MenuRegistry.register(first, enabled = false)
        MenuRegistry.register(second, enabled = true)

        val result = MenuRegistry.recognize(menu("SkyBlock Menu"))
        assertNotNull(result)
        assertEquals("second", result.id)
    }

    @Test
    fun `setEnabled can re-enable a disabled recognizer`() {
        MenuRegistry.register(SkyBlockMenuRecognizer, enabled = false)
        MenuRegistry.setEnabled("skyblock_menu", true)

        val result = MenuRegistry.recognize(menu("SkyBlock Menu"))
        assertNotNull(result)
        assertEquals("skyblock_menu", result.id)
    }

    @Test
    fun `setEnabled can disable an enabled recognizer`() {
        MenuRegistry.register(SkyBlockMenuRecognizer, enabled = true)
        MenuRegistry.setEnabled("skyblock_menu", false)

        assertNull(MenuRegistry.recognize(menu("SkyBlock Menu")))
    }

    // ── all property ─────────────────────────────────────────────────────────

    @Test
    fun `all returns all recognizers in registration order`() {
        val a = stubRecognizer("a", "A")
        val b = stubRecognizer("b", "B")
        MenuRegistry.register(a)
        MenuRegistry.register(b)

        val ids = MenuRegistry.all.map { it.id }
        assertEquals(listOf("a", "b"), ids)
    }

    @Test
    fun `all includes disabled recognizers`() {
        MenuRegistry.register(SkyBlockMenuRecognizer, enabled = false)
        assertEquals(1, MenuRegistry.all.size)
        assertEquals("skyblock_menu", MenuRegistry.all[0].id)
    }

    // ── exception safety ─────────────────────────────────────────────────────

    @Test
    fun `recognize skips a recognizer that throws and continues to the next`() {
        val throwing = object : MenuRecognizer {
            override val id = "throwing"
            override fun matches(menu: ParsedMenu): Boolean = throw RuntimeException("oops")
        }
        val safe = stubRecognizer("safe", "SkyBlock Menu")

        MenuRegistry.register(throwing)
        MenuRegistry.register(safe)

        val result = MenuRegistry.recognize(menu("SkyBlock Menu"))
        assertNotNull(result)
        assertEquals("safe", result.id)
    }
}
