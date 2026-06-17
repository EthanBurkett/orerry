package gg.orrery.atlas

import kotlin.test.Test
import kotlin.test.assertEquals

class TextCodesTest {

    @Test
    fun `stripCodes removes color codes`() {
        assertEquals("Hello World", stripCodes("§aHello §bWorld"))
    }

    @Test
    fun `stripCodes removes format codes`() {
        // §l bold, §o italic, §n underline, §m strikethrough, §k obfuscated, §r reset
        assertEquals("bold italic", stripCodes("§lbold §oitalic"))
        assertEquals("text", stripCodes("§ktext"))
        assertEquals("reset", stripCodes("§rreset"))
    }

    @Test
    fun `stripCodes removes numeric color codes`() {
        assertEquals("red green", stripCodes("§4red §2green"))
    }

    @Test
    fun `stripCodes is case-insensitive on the code letter`() {
        // §A and §a are both valid
        assertEquals("Hello", stripCodes("§AHello"))
    }

    @Test
    fun `stripCodes leaves plain text unchanged`() {
        val plain = "No codes here."
        assertEquals(plain, stripCodes(plain))
    }

    @Test
    fun `stripCodes handles empty string`() {
        assertEquals("", stripCodes(""))
    }

    @Test
    fun `stripCodes handles string that is only codes`() {
        assertEquals("", stripCodes("§a§b§c§l§r"))
    }

    @Test
    fun `stripCodes handles SkyBlock-style lore line`() {
        // Typical SkyBlock lore: "§7Damage: §c+100"
        assertEquals("Damage: +100", stripCodes("§7Damage: §c+100"))
    }

    @Test
    fun `stripCodes handles rarity line`() {
        assertEquals("LEGENDARY SWORD", stripCodes("§6§lLEGENDARY SWORD"))
    }
}
