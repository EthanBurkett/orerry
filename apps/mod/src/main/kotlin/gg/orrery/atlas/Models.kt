package gg.orrery.atlas

/**
 * Atlas core data models — verbatim from ADR 0003 §1.
 *
 * These are plain Kotlin data classes with NO net.minecraft.* imports so they
 * are unit-testable without a game classpath.
 *
 * §2 / §6.5 compliance: Atlas only exposes data parsed from containers the
 * server already sent. No fabricated or hidden information.
 */

/**
 * A single parsed item from a SkyBlock container slot.
 *
 * @param slot  Backing ScreenHandler slot index.
 * @param name  Display name with § codes stripped.
 * @param lore  Lore lines with § codes stripped.
 * @param rarity Item rarity parsed from lore.
 * @param skyblockId  ExtraAttributes "id" value if present.
 * @param count Stack count.
 * @param extraAttributes Shallow string view of ExtraAttributes scalar entries.
 * @param itemId  The Minecraft item registry id, e.g. "minecraft:gray_stained_glass_pane".
 *                Populated by [AtlasAdapter] via Registries.ITEM.getId(stack.item).toString().
 *                Null when unavailable. Used by [classifyEntry] (e.g. pane → FILLER).
 *                Core stays MC-agnostic: this is just a plain String.
 * @param rawName The display name WITH § color codes intact (before stripping).
 *                Populated by [AtlasAdapter] via stack.getName().getString() before stripping.
 *                Used by [deriveRarity] to extract SkyBlock's name color → rarity fallback.
 */
data class ParsedItem(
    val slot: Int,
    val name: String,
    val lore: List<String>,
    val rarity: Rarity,
    val skyblockId: String?,
    val count: Int,
    val extraAttributes: Map<String, String>,
    val itemId: String? = null,
    val rawName: String = name,
)

/**
 * A fully parsed SkyBlock container menu.
 *
 * @param title  Menu title with § codes stripped.
 * @param size   Total slot count.
 * @param rows   size / 9.
 * @param items  Items indexed by slot; null entries represent empty slots.
 */
data class ParsedMenu(
    val title: String,
    val size: Int,
    val rows: Int,
    val items: List<ParsedItem?>,
)
