package gg.orrery.atlas

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MenuEntryTest {

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun item(
        slot: Int,
        name: String,
        lore: List<String> = emptyList(),
        rarity: Rarity = Rarity.UNKNOWN,
    ) = ParsedItem(
        slot = slot,
        name = name,
        lore = lore,
        rarity = rarity,
        skyblockId = null,
        count = 1,
        extraAttributes = emptyMap(),
    )

    private fun menu(vararg items: ParsedItem?, size: Int = 54) = ParsedMenu(
        title = "SkyBlock Menu",
        size = size,
        rows = size / 9,
        items = items.toList(),
    )

    // ── title extraction ──────────────────────────────────────────────────────

    @Test
    fun `parseEntries extracts title from item name`() {
        val entry = parseEntries(menu(item(0, "Your Skills"))).single()
        assertEquals("Your Skills", entry.title)
    }

    @Test
    fun `parseEntries uses backingSlot from item slot`() {
        val entry = parseEntries(menu(item(7, "Profile"))).single()
        assertEquals(7, entry.backingSlot)
    }

    @Test
    fun `parseEntries carries rarity through`() {
        val entry = parseEntries(menu(item(0, "Sword", rarity = Rarity.LEGENDARY))).single()
        assertEquals(Rarity.LEGENDARY, entry.rarity)
    }

    // ── subtitle extraction ───────────────────────────────────────────────────

    @Test
    fun `subtitle is first non-blank non-rarity lore line`() {
        val lore = listOf(
            "View your skills and",
            "track your progress.",
            "",
            "UNCOMMON",
        )
        val entry = parseEntries(menu(item(0, "Skills", lore, Rarity.UNCOMMON))).single()
        assertEquals("View your skills and", entry.subtitle)
    }

    @Test
    fun `subtitle skips leading blank lore lines`() {
        val lore = listOf("", "  ", "First real line", "COMMON")
        val entry = parseEntries(menu(item(0, "Something", lore, Rarity.COMMON))).single()
        assertEquals("First real line", entry.subtitle)
    }

    @Test
    fun `subtitle skips rarity-only line (single word)`() {
        val lore = listOf("RARE")
        val entry = parseEntries(menu(item(0, "Item", lore, Rarity.RARE))).single()
        assertNull(entry.subtitle)
    }

    @Test
    fun `subtitle skips rarity with category word (EPIC SWORD)`() {
        val lore = listOf("EPIC SWORD")
        val entry = parseEntries(menu(item(0, "Blade", lore, Rarity.EPIC))).single()
        assertNull(entry.subtitle)
    }

    @Test
    fun `subtitle is null when lore is empty`() {
        val entry = parseEntries(menu(item(0, "Quests"))).single()
        assertNull(entry.subtitle)
    }

    @Test
    fun `subtitle is null when lore contains only blank and rarity lines`() {
        val lore = listOf("", "LEGENDARY SWORD", "")
        val entry = parseEntries(menu(item(0, "Excalibur", lore, Rarity.LEGENDARY))).single()
        assertNull(entry.subtitle)
    }

    @Test
    fun `subtitle is the first descriptive line even when followed by more descriptive lines`() {
        val lore = listOf(
            "Line one",
            "Line two",
            "MYTHIC ACCESSORY",
        )
        val entry = parseEntries(menu(item(0, "Ring", lore, Rarity.MYTHIC))).single()
        assertEquals("Line one", entry.subtitle)
    }

    // ── rarity-line heuristic edge cases ─────────────────────────────────────

    @Test
    fun `isRarityLine returns true for bare rarity word`() {
        assertTrue(isRarityLine("COMMON"))
        assertTrue(isRarityLine("LEGENDARY"))
        assertTrue(isRarityLine("DIVINE"))
        assertTrue(isRarityLine("SPECIAL"))
    }

    @Test
    fun `isRarityLine returns true for rarity followed by category`() {
        assertTrue(isRarityLine("EPIC SWORD"))
        assertTrue(isRarityLine("MYTHIC ACCESSORY"))
        assertTrue(isRarityLine("UNCOMMON HELMET"))
    }

    @Test
    fun `isRarityLine returns false for mixed-case descriptive lines`() {
        // These are real lore description lines that happen to start with a capital
        kotlin.test.assertFalse(isRarityLine("Right-click to open the menu."))
        kotlin.test.assertFalse(isRarityLine("Deal damage to enemies nearby."))
        kotlin.test.assertFalse(isRarityLine("Click to use!"))
    }

    @Test
    fun `isRarityLine returns false for blank line`() {
        kotlin.test.assertFalse(isRarityLine(""))
        kotlin.test.assertFalse(isRarityLine("   "))
    }

    // ── filler filtering ─────────────────────────────────────────────────────

    @Test
    fun `parseEntries skips null slots`() {
        val entries = parseEntries(menu(null, item(1, "Real Item"), null))
        assertEquals(1, entries.size)
        assertEquals("Real Item", entries[0].title)
    }

    @Test
    fun `parseEntries skips blank-name items`() {
        val entries = parseEntries(menu(item(0, ""), item(1, "Real Item"), item(2, "   ")))
        assertEquals(1, entries.size)
        assertEquals("Real Item", entries[0].title)
    }

    @Test
    fun `parseEntries skips punctuation-only titles`() {
        // Glass pane filler typically has a name that is just decorative characters
        val entries = parseEntries(
            menu(
                item(0, "·····"),
                item(1, "-----"),
                item(2, "░░░░"),
                item(3, "Real Entry"),
            )
        )
        assertEquals(1, entries.size)
        assertEquals("Real Entry", entries[0].title)
    }

    @Test
    fun `parseEntries preserves backingSlot indices correctly`() {
        val entries = parseEntries(
            menu(
                null,           // slot 0 → skipped
                item(1, ""),    // slot 1 → filler, skipped
                item(2, "Alpha"),
                null,           // slot 3 → skipped
                item(4, "Beta"),
            )
        )
        assertEquals(2, entries.size)
        assertEquals(2, entries[0].backingSlot)
        assertEquals(4, entries[1].backingSlot)
    }

    // ── ordering ──────────────────────────────────────────────────────────────

    @Test
    fun `parseEntries returns entries in slot order`() {
        val entries = parseEntries(
            menu(
                item(10, "Crafting"),
                item(4, "Collections"),
                item(7, "Skills"),
            )
        )
        // items list order is preserved as given (slot 10 then 4 then 7)
        assertEquals(listOf(10, 4, 7), entries.map { it.backingSlot })
    }

    // ── full realistic menu fixture ───────────────────────────────────────────

    @Test
    fun `parseEntries handles a realistic SkyBlock menu fixture`() {
        val skyblockMenuItems: List<ParsedItem?> = listOf(
            // slot 0: filler
            item(0, ""),
            // slot 1: glass pane filler
            item(1, "·····"),
            // slot 2: null
            null,
            // slot 10: real entry — Your Skills
            item(
                slot = 10,
                name = "Your Skills",
                lore = listOf(
                    "View your skills and track",
                    "your progression.",
                    "",
                    "CLICK to view!",
                    "",
                    "UNCOMMON",
                ),
                rarity = Rarity.UNCOMMON,
            ),
            // slot 11: real entry — Quests (no lore besides rarity)
            item(
                slot = 11,
                name = "Quests",
                lore = listOf("COMMON"),
                rarity = Rarity.COMMON,
            ),
            // slot 12: real entry — Collections with leading blank lore
            item(
                slot = 12,
                name = "Collections",
                lore = listOf("", "Unlock rewards.", "RARE"),
                rarity = Rarity.RARE,
            ),
        )

        val menu = ParsedMenu(
            title = "SkyBlock Menu",
            size = 54,
            rows = 6,
            items = skyblockMenuItems + List(54 - skyblockMenuItems.size) { null },
        )

        val entries = parseEntries(menu)

        assertEquals(3, entries.size)

        val skills = entries[0]
        assertEquals(10, skills.backingSlot)
        assertEquals("Your Skills", skills.title)
        assertEquals("View your skills and track", skills.subtitle)
        assertEquals(Rarity.UNCOMMON, skills.rarity)

        val quests = entries[1]
        assertEquals(11, quests.backingSlot)
        assertEquals("Quests", quests.title)
        assertNull(quests.subtitle)
        assertEquals(Rarity.COMMON, quests.rarity)

        val collections = entries[2]
        assertEquals(12, collections.backingSlot)
        assertEquals("Collections", collections.title)
        assertEquals("Unlock rewards.", collections.subtitle)
        assertEquals(Rarity.RARE, collections.rarity)
    }
}
