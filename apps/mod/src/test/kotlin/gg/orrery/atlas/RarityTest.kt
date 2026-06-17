package gg.orrery.atlas

import kotlin.test.Test
import kotlin.test.assertEquals

class RarityTest {

    // ── individual rarities from a trailing single word ──────────────────────

    @Test
    fun `parseRarity detects COMMON`() {
        assertEquals(Rarity.COMMON, parseRarity(listOf("COMMON"), ""))
    }

    @Test
    fun `parseRarity detects UNCOMMON`() {
        assertEquals(Rarity.UNCOMMON, parseRarity(listOf("UNCOMMON"), ""))
    }

    @Test
    fun `parseRarity detects RARE`() {
        assertEquals(Rarity.RARE, parseRarity(listOf("RARE"), ""))
    }

    @Test
    fun `parseRarity detects EPIC`() {
        assertEquals(Rarity.EPIC, parseRarity(listOf("EPIC"), ""))
    }

    @Test
    fun `parseRarity detects LEGENDARY`() {
        assertEquals(Rarity.LEGENDARY, parseRarity(listOf("LEGENDARY"), ""))
    }

    @Test
    fun `parseRarity detects MYTHIC`() {
        assertEquals(Rarity.MYTHIC, parseRarity(listOf("MYTHIC"), ""))
    }

    @Test
    fun `parseRarity detects DIVINE`() {
        assertEquals(Rarity.DIVINE, parseRarity(listOf("DIVINE"), ""))
    }

    @Test
    fun `parseRarity detects SPECIAL`() {
        assertEquals(Rarity.SPECIAL, parseRarity(listOf("SPECIAL"), ""))
    }

    // ── rarity as last word in a category line ────────────────────────────────

    @Test
    fun `parseRarity detects LEGENDARY from LEGENDARY SWORD line`() {
        assertEquals(Rarity.LEGENDARY, parseRarity(listOf("LEGENDARY SWORD"), ""))
    }

    @Test
    fun `parseRarity detects RARE from RARE SWORD`() {
        assertEquals(Rarity.RARE, parseRarity(listOf("RARE SWORD"), ""))
    }

    @Test
    fun `parseRarity detects MYTHIC from MYTHIC ACCESSORY`() {
        assertEquals(Rarity.MYTHIC, parseRarity(listOf("MYTHIC ACCESSORY"), ""))
    }

    @Test
    fun `parseRarity detects EPIC from EPIC ARMOR line`() {
        assertEquals(Rarity.EPIC, parseRarity(listOf("EPIC CHESTPLATE"), ""))
    }

    // ── case-insensitivity ────────────────────────────────────────────────────

    @Test
    fun `parseRarity is case-insensitive`() {
        assertEquals(Rarity.RARE, parseRarity(listOf("rare sword"), ""))
        assertEquals(Rarity.EPIC, parseRarity(listOf("Epic"), ""))
    }

    // ── uses LAST meaningful lore line ───────────────────────────────────────

    @Test
    fun `parseRarity reads last non-blank lore line`() {
        val lore = listOf(
            "Some description line",
            "",
            "Right-click to use!",
            "LEGENDARY SWORD",
        )
        assertEquals(Rarity.LEGENDARY, parseRarity(lore, ""))
    }

    @Test
    fun `parseRarity skips trailing blank lines`() {
        val lore = listOf("RARE HELMET", "", "")
        assertEquals(Rarity.RARE, parseRarity(lore, ""))
    }

    // ── UNKNOWN fallback ─────────────────────────────────────────────────────

    @Test
    fun `parseRarity returns UNKNOWN for empty lore and empty name`() {
        assertEquals(Rarity.UNKNOWN, parseRarity(emptyList(), ""))
    }

    @Test
    fun `parseRarity returns UNKNOWN when no rarity keyword present`() {
        assertEquals(Rarity.UNKNOWN, parseRarity(listOf("Some random text"), "Random Item"))
    }

    @Test
    fun `parseRarity returns UNKNOWN for blank lore lines only`() {
        assertEquals(Rarity.UNKNOWN, parseRarity(listOf("", "  "), ""))
    }
}
