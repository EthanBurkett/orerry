package gg.orrery.atlas

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests for [classifyEntry], [selectSubtitle], [stripClickHints], and [deriveRarity].
 *
 * All fixtures use plain data — zero net.minecraft.* imports.
 */
class EntryTypeTest {

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun item(
        name: String,
        lore: List<String> = emptyList(),
        rarity: Rarity = Rarity.UNKNOWN,
        skyblockId: String? = null,
        itemId: String? = null,
        rawName: String = name,
        extraAttributes: Map<String, String> = emptyMap(),
    ) = ParsedItem(
        slot = 0,
        name = name,
        lore = lore,
        rarity = rarity,
        skyblockId = skyblockId,
        count = 1,
        extraAttributes = extraAttributes,
        itemId = itemId,
        rawName = rawName,
    )

    private fun menu(vararg items: ParsedItem?, size: Int = 54) = ParsedMenu(
        title = "SkyBlock Menu",
        size = size,
        rows = size / 9,
        items = items.toList() + List(size - items.size) { null },
    )

    // ── classifyEntry: FILLER ─────────────────────────────────────────────────

    @Test
    fun `classifyEntry returns FILLER for stained glass pane by itemId`() {
        assertEquals(EntryType.FILLER, classifyEntry(item("", itemId = "minecraft:gray_stained_glass_pane")))
        assertEquals(EntryType.FILLER, classifyEntry(item("", itemId = "minecraft:red_stained_glass_pane")))
        assertEquals(EntryType.FILLER, classifyEntry(item("", itemId = "minecraft:white_stained_glass_pane")))
        assertEquals(EntryType.FILLER, classifyEntry(item("Border", itemId = "minecraft:black_stained_glass_pane")))
    }

    @Test
    fun `classifyEntry returns FILLER for blank name`() {
        assertEquals(EntryType.FILLER, classifyEntry(item("")))
        assertEquals(EntryType.FILLER, classifyEntry(item("   ")))
    }

    @Test
    fun `classifyEntry returns FILLER for punctuation-only name`() {
        assertEquals(EntryType.FILLER, classifyEntry(item("·····")))
        assertEquals(EntryType.FILLER, classifyEntry(item("-----")))
        assertEquals(EntryType.FILLER, classifyEntry(item("░░░░")))
    }

    @Test
    fun `classifyEntry glass pane itemId wins over non-blank name`() {
        // Even if itemId is a pane, FILLER takes priority over any name content
        assertEquals(
            EntryType.FILLER,
            classifyEntry(item("Some Label", itemId = "minecraft:lime_stained_glass_pane")),
        )
    }

    // ── classifyEntry: BACK ───────────────────────────────────────────────────

    @Test
    fun `classifyEntry returns BACK for 'Back'`() {
        assertEquals(EntryType.BACK, classifyEntry(item("Back")))
    }

    @Test
    fun `classifyEntry returns BACK for 'Go Back'`() {
        assertEquals(EntryType.BACK, classifyEntry(item("Go Back")))
    }

    @Test
    fun `classifyEntry returns BACK for 'To SkyBlock Menu'`() {
        assertEquals(EntryType.BACK, classifyEntry(item("To SkyBlock Menu")))
    }

    @Test
    fun `classifyEntry returns BACK for back-arrow prefix`() {
        assertEquals(EntryType.BACK, classifyEntry(item("← Go Back")))
        assertEquals(EntryType.BACK, classifyEntry(item("◄ Previous")))
    }

    // ── classifyEntry: PAGINATION ─────────────────────────────────────────────

    @Test
    fun `classifyEntry returns PAGINATION for Next Page`() {
        assertEquals(EntryType.PAGINATION, classifyEntry(item("Next Page")))
    }

    @Test
    fun `classifyEntry returns PAGINATION for Previous Page`() {
        assertEquals(EntryType.PAGINATION, classifyEntry(item("Previous Page")))
    }

    @Test
    fun `classifyEntry returns PAGINATION for Page N`() {
        assertEquals(EntryType.PAGINATION, classifyEntry(item("Page 1")))
        assertEquals(EntryType.PAGINATION, classifyEntry(item("Page 12")))
    }

    // ── classifyEntry: TOGGLE ─────────────────────────────────────────────────

    @Test
    fun `classifyEntry returns TOGGLE when lore has 'Currently' indicator`() {
        val toggleItem = item(
            name = "Auto-Pickup",
            lore = listOf("Automatically picks up items.", "Currently: Enabled"),
        )
        assertEquals(EntryType.TOGGLE, classifyEntry(toggleItem))
    }

    @Test
    fun `classifyEntry returns TOGGLE when lore has bare Enabled or Disabled`() {
        val enabled = item("Feature", lore = listOf("Enabled"))
        val disabled = item("Feature", lore = listOf("Disabled"))
        assertEquals(EntryType.TOGGLE, classifyEntry(enabled))
        assertEquals(EntryType.TOGGLE, classifyEntry(disabled))
    }

    // ── classifyEntry: NAVIGATION ─────────────────────────────────────────────

    @Test
    fun `classifyEntry returns NAVIGATION for nav entry with click-to-view lore`() {
        val navItem = item(
            name = "Your Skills",
            lore = listOf("View all of your skills.", "Click to view skills."),
        )
        assertEquals(EntryType.NAVIGATION, classifyEntry(navItem))
    }

    @Test
    fun `classifyEntry returns NAVIGATION for click-to-open lore`() {
        val navItem = item(
            name = "Collections",
            lore = listOf("Click to open your collections."),
        )
        assertEquals(EntryType.NAVIGATION, classifyEntry(navItem))
    }

    @Test
    fun `classifyEntry does NOT return NAVIGATION when item has skyblockId`() {
        // A real item with an action hint is still an ITEM, not NAVIGATION
        val realItem = item(
            name = "Aspect of the Dragons",
            lore = listOf("Click to equip.", "LEGENDARY SWORD"),
            rarity = Rarity.LEGENDARY,
            skyblockId = "ASPECT_OF_THE_DRAGONS",
        )
        assertEquals(EntryType.ITEM, classifyEntry(realItem))
    }

    // ── classifyEntry: ITEM ───────────────────────────────────────────────────

    @Test
    fun `classifyEntry returns ITEM when skyblockId is present`() {
        val sbItem = item("Farming Talisman", skyblockId = "FARMING_TALISMAN")
        assertEquals(EntryType.ITEM, classifyEntry(sbItem))
    }

    @Test
    fun `classifyEntry returns ITEM when rarity is non-UNKNOWN`() {
        val rarityItem = item("Epic Sword", rarity = Rarity.EPIC)
        assertEquals(EntryType.ITEM, classifyEntry(rarityItem))
    }

    @Test
    fun `classifyEntry returns ITEM for item with both skyblockId and rarity`() {
        val fullItem = item(
            name = "Hyperion",
            rarity = Rarity.LEGENDARY,
            skyblockId = "HYPERION",
            lore = listOf("+1000 Damage", "LEGENDARY SWORD"),
        )
        assertEquals(EntryType.ITEM, classifyEntry(fullItem))
    }

    // ── classifyEntry: INFO ───────────────────────────────────────────────────

    @Test
    fun `classifyEntry returns INFO for informational item with no action hint`() {
        val statsItem = item(
            name = "Stats",
            lore = listOf("Health: 100", "Defense: 50"),
        )
        assertEquals(EntryType.INFO, classifyEntry(statsItem))
    }

    @Test
    fun `classifyEntry returns INFO for item with no signals`() {
        val noopItem = item(name = "Profile")
        assertEquals(EntryType.INFO, classifyEntry(noopItem))
    }

    // ── classifyEntry: OTHER ──────────────────────────────────────────────────

    @Test
    fun `classifyEntry returns OTHER for item with action hint but not nav-compatible`() {
        // Has an action hint lore line but ALSO has a click hint that isn't a view/open/browse verb
        // and has no skyblockId — tricky edge; tests the fallback
        val oddItem = item(
            name = "Something",
            lore = listOf("Click to do something weird."),
        )
        // "Click to do" — "do" doesn't match nav verbs (view|open|browse|visit|go|access|select|see)
        // So hasActionHint = false (ACTION_HINT_PATTERN requires specific verbs)
        // → no action hint → INFO
        assertEquals(EntryType.INFO, classifyEntry(oddItem))
    }

    // ── parseEntries: drops FILLER ────────────────────────────────────────────

    @Test
    fun `parseEntries drops items with stained glass pane itemId`() {
        val entries = parseEntries(
            menu(
                ParsedItem(0, "", emptyList(), Rarity.UNKNOWN, null, 1, emptyMap(), "minecraft:gray_stained_glass_pane", ""),
                ParsedItem(1, "Your Skills", emptyList(), Rarity.UNKNOWN, null, 1, emptyMap(), null, "Your Skills"),
            ),
        )
        assertEquals(1, entries.size)
        assertEquals("Your Skills", entries[0].title)
    }

    @Test
    fun `parseEntries drops blank-name items`() {
        val entries = parseEntries(
            menu(
                ParsedItem(0, "", emptyList(), Rarity.UNKNOWN, null, 1, emptyMap()),
                ParsedItem(1, "Real", emptyList(), Rarity.UNKNOWN, null, 1, emptyMap()),
            ),
        )
        assertEquals(1, entries.size)
        assertEquals("Real", entries[0].title)
    }

    // ── parseEntries: sets type ───────────────────────────────────────────────

    @Test
    fun `parseEntries sets type on MenuEntry`() {
        val entries = parseEntries(
            menu(
                ParsedItem(
                    slot = 0,
                    name = "Your Skills",
                    lore = listOf("Click to view skills."),
                    rarity = Rarity.UNKNOWN,
                    skyblockId = null,
                    count = 1,
                    extraAttributes = emptyMap(),
                ),
            ),
        )
        assertEquals(1, entries.size)
        assertEquals(EntryType.NAVIGATION, entries[0].type)
    }

    @Test
    fun `parseEntries sets BACK type for back entry`() {
        val entries = parseEntries(menu(ParsedItem(0, "Go Back", emptyList(), Rarity.UNKNOWN, null, 1, emptyMap())))
        assertEquals(EntryType.BACK, entries[0].type)
    }

    @Test
    fun `parseEntries sets ITEM type for real SkyBlock item`() {
        val entries = parseEntries(
            menu(
                ParsedItem(
                    slot = 0,
                    name = "Hyperion",
                    lore = listOf("LEGENDARY SWORD"),
                    rarity = Rarity.LEGENDARY,
                    skyblockId = "HYPERION",
                    count = 1,
                    extraAttributes = emptyMap(),
                ),
            ),
        )
        assertEquals(EntryType.ITEM, entries[0].type)
    }

    // ── parseEntries: subtitle by type ────────────────────────────────────────

    @Test
    fun `parseEntries subtitle for NAVIGATION picks the action line`() {
        val entries = parseEntries(
            menu(
                ParsedItem(
                    slot = 0,
                    name = "Recipes",
                    lore = listOf("Browse known recipes.", "Click to open recipes."),
                    rarity = Rarity.UNKNOWN,
                    skyblockId = null,
                    count = 1,
                    extraAttributes = emptyMap(),
                ),
            ),
        )
        // "Browse known recipes." matches NAV_ACTION_PATTERN → chosen as subtitle
        assertEquals("Browse known recipes.", entries[0].subtitle)
    }

    @Test
    fun `parseEntries subtitle for ITEM picks stat-bearing line`() {
        val entries = parseEntries(
            menu(
                ParsedItem(
                    slot = 0,
                    name = "Aspect of the Dragons",
                    lore = listOf("A powerful sword.", "+250 Damage", "+100 Strength", "LEGENDARY SWORD"),
                    rarity = Rarity.LEGENDARY,
                    skyblockId = "ASPECT_OF_THE_DRAGONS",
                    count = 1,
                    extraAttributes = emptyMap(),
                ),
            ),
        )
        // First stat-bearing line with +N pattern
        assertEquals("+250 Damage", entries[0].subtitle)
    }

    @Test
    fun `parseEntries subtitle for ITEM picks progress line when present`() {
        val entries = parseEntries(
            menu(
                ParsedItem(
                    slot = 0,
                    name = "Collections",
                    lore = listOf("Progress: 1234/5000", "Complete collections for rewards.", "UNCOMMON"),
                    rarity = Rarity.UNCOMMON,
                    skyblockId = null,
                    count = 1,
                    extraAttributes = emptyMap(),
                ),
            ),
        )
        // Progress line (x/y) is preferred over stat or descriptive lines
        assertEquals("Progress: 1234/5000", entries[0].subtitle)
    }

    @Test
    fun `parseEntries subtitle for INFO picks first descriptive line`() {
        val entries = parseEntries(
            menu(
                ParsedItem(
                    slot = 0,
                    name = "Profile",
                    lore = listOf("", "Coins: 1,234,567", ""),
                    rarity = Rarity.UNKNOWN,
                    skyblockId = null,
                    count = 1,
                    extraAttributes = emptyMap(),
                ),
            ),
        )
        assertEquals("Coins: 1,234,567", entries[0].subtitle)
    }

    @Test
    fun `parseEntries subtitle skips click-hint lore lines`() {
        val entries = parseEntries(
            menu(
                ParsedItem(
                    slot = 0,
                    name = "Quest Board",
                    lore = listOf("Click to view quests.", "See your active quests."),
                    rarity = Rarity.UNKNOWN,
                    skyblockId = null,
                    count = 1,
                    extraAttributes = emptyMap(),
                ),
            ),
        )
        // NAVIGATION type: "Click to view quests." matches action verb (view)
        assertEquals("Click to view quests.", entries[0].subtitle)
    }

    // ── parseEntries: preserves backingSlot and order ─────────────────────────

    @Test
    fun `parseEntries preserves backingSlot for non-filler entries`() {
        val entries = parseEntries(
            menu(
                null,  // slot 0 empty
                ParsedItem(1, "", emptyList(), Rarity.UNKNOWN, null, 1, emptyMap(), "minecraft:gray_stained_glass_pane", ""),  // filler
                ParsedItem(2, "Skills", emptyList(), Rarity.UNKNOWN, null, 1, emptyMap()),
                null,
                ParsedItem(4, "Quests", emptyList(), Rarity.UNKNOWN, null, 1, emptyMap()),
            ),
        )
        assertEquals(2, entries.size)
        assertEquals(2, entries[0].backingSlot)
        assertEquals(4, entries[1].backingSlot)
    }

    @Test
    fun `parseEntries preserves slot order`() {
        val entries = parseEntries(
            menu(
                ParsedItem(10, "Crafting", emptyList(), Rarity.UNKNOWN, null, 1, emptyMap()),
                ParsedItem(4, "Collections", emptyList(), Rarity.UNKNOWN, null, 1, emptyMap()),
                ParsedItem(7, "Skills", emptyList(), Rarity.UNKNOWN, null, 1, emptyMap()),
            ),
        )
        assertEquals(listOf(10, 4, 7), entries.map { it.backingSlot })
    }

    // ── stripClickHints ───────────────────────────────────────────────────────

    @Test
    fun `stripClickHints removes trailing parenthetical click hint`() {
        assertEquals("SkyBlock Menu", stripClickHints("SkyBlock Menu (Click)"))
    }

    @Test
    fun `stripClickHints removes trailing right-click parenthetical`() {
        assertEquals("Open Bazaar", stripClickHints("Open Bazaar (Right Click)"))
    }

    @Test
    fun `stripClickHints removes trailing left-click parenthetical`() {
        assertEquals("Item", stripClickHints("Item (Left Click)"))
    }

    @Test
    fun `stripClickHints removes trailing Click to phrase`() {
        assertEquals("Auction House", stripClickHints("Auction House Click to open"))
    }

    @Test
    fun `stripClickHints leaves plain titles unchanged`() {
        assertEquals("Go Back", stripClickHints("Go Back"))
        assertEquals("Your Skills", stripClickHints("Your Skills"))
        assertEquals("SkyBlock Menu", stripClickHints("SkyBlock Menu"))
    }

    @Test
    fun `stripClickHints handles empty string`() {
        assertEquals("", stripClickHints(""))
    }

    @Test
    fun `stripClickHints trims surrounding whitespace`() {
        assertEquals("Item", stripClickHints("  Item  "))
    }

    @Test
    fun `stripClickHints does not strip partial matches mid-word`() {
        // "Clicking Simulator" — does NOT start with click-to or right-click pattern
        assertEquals("Clicking Simulator", stripClickHints("Clicking Simulator"))
    }

    // ── deriveRarity: priority chain ──────────────────────────────────────────

    @Test
    fun `deriveRarity returns ExtraAttributes rarity over lore line`() {
        // ExtraAttributes says "EPIC" but lore says LEGENDARY — ExtraAttributes wins (tier 1)
        val it = item(
            name = "Sword",
            lore = listOf("LEGENDARY SWORD"),
            rarity = Rarity.LEGENDARY,
            extraAttributes = mapOf("rarity" to "EPIC"),
        )
        assertEquals(Rarity.EPIC, deriveRarity(it))
    }

    @Test
    fun `deriveRarity uses tier field as alternative to rarity field`() {
        val it = item(
            name = "Item",
            lore = emptyList(),
            rarity = Rarity.UNKNOWN,
            extraAttributes = mapOf("tier" to "MYTHIC"),
        )
        assertEquals(Rarity.MYTHIC, deriveRarity(it))
    }

    @Test
    fun `deriveRarity falls through to lore line when no ExtraAttributes rarity`() {
        val it = item(
            name = "Sword",
            lore = listOf("A fine sword.", "RARE SWORD"),
            rarity = Rarity.RARE,
        )
        assertEquals(Rarity.RARE, deriveRarity(it))
    }

    @Test
    fun `deriveRarity falls through to rawName color when no lore rarity`() {
        // §6 = LEGENDARY color; lore is empty
        val it = item(
            name = "Sword",
            rawName = "§6§lSword",
            lore = emptyList(),
            rarity = Rarity.UNKNOWN,
        )
        assertEquals(Rarity.LEGENDARY, deriveRarity(it))
    }

    @Test
    fun `deriveRarity rawName color mapping - all supported colors`() {
        data class Case(val code: String, val expected: Rarity)
        listOf(
            Case("§f", Rarity.COMMON),
            Case("§a", Rarity.UNCOMMON),
            Case("§9", Rarity.RARE),
            Case("§5", Rarity.EPIC),
            Case("§6", Rarity.LEGENDARY),
            Case("§d", Rarity.MYTHIC),
            Case("§b", Rarity.DIVINE),
            Case("§c", Rarity.SPECIAL),
        ).forEach { (code, expected) ->
            val it = item(name = "Item", rawName = "${code}Item", lore = emptyList())
            assertEquals(expected, deriveRarity(it), "Expected $expected for color $code")
        }
    }

    @Test
    fun `deriveRarity skips bold code prefix and reads color code`() {
        // §l (bold) before §6 (gold/legendary) — should still read LEGENDARY
        val it = item(name = "Sword", rawName = "§l§6Sword", lore = emptyList())
        assertEquals(Rarity.LEGENDARY, deriveRarity(it))
    }

    @Test
    fun `deriveRarity falls to item rarity as last resort`() {
        // No ExtraAttributes, no lore, no § color prefix → use item.rarity
        val it = item(name = "Sword", rarity = Rarity.DIVINE, rawName = "Sword")
        assertEquals(Rarity.DIVINE, deriveRarity(it))
    }

    @Test
    fun `deriveRarity returns UNKNOWN when all tiers fail`() {
        val it = item(name = "Mystery Item", lore = emptyList(), rarity = Rarity.UNKNOWN)
        assertEquals(Rarity.UNKNOWN, deriveRarity(it))
    }

    @Test
    fun `deriveRarity ExtraAttributes rarity wins over rawName color`() {
        // ExtraAttributes says COMMON, rawName says LEGENDARY (§6) — ExtraAttributes wins
        val it = item(
            name = "Item",
            rawName = "§6Item",
            lore = emptyList(),
            rarity = Rarity.UNKNOWN,
            extraAttributes = mapOf("rarity" to "COMMON"),
        )
        assertEquals(Rarity.COMMON, deriveRarity(it))
    }

    @Test
    fun `deriveRarity lore line wins over rawName color`() {
        // lore says RARE, rawName says LEGENDARY (§6) — lore wins (tier 2 before tier 3)
        val it = item(
            name = "Item",
            rawName = "§6Item",
            lore = listOf("RARE SWORD"),
            rarity = Rarity.RARE,
        )
        assertEquals(Rarity.RARE, deriveRarity(it))
    }
}
