package gg.orrery.atlas

import net.minecraft.core.component.DataComponents
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.network.chat.Component

/**
 * AtlasAdapter — the ONLY Atlas file that imports net.minecraft.*.
 *
 * Converts a live MC [ScreenHandler] + title [Text] into a [ParsedMenu].
 * All parsing logic that can be expressed on plain strings lives in the
 * MC-free core files (TextCodes, Rarity, Models) so they remain unit-testable
 * without a game classpath (ADR 0003 §1).
 *
 * §2 / §6.5 compliance: this adapter only reads data the server already sent
 * in the container open packet. It never issues extra network requests.
 *
 * --- Yarn 1.21.11 API names used (build.6) ---
 *
 * ScreenHandler:
 *   .slots : DefaultedList<Slot>        — all registered slots in order
 *   .syncId : Int                       — container sync id (shared with server)
 *
 * Slot (net.minecraft.screen.slot.Slot):
 *   .stack : ItemStack                  — the item in this slot (EMPTY if none)
 *   .index : Int                        — slot index within the handler
 *
 * ItemStack:
 *   .isEmpty : Boolean                  — true when the stack is air / empty
 *   .count : Int                        — stack size
 *   .getName() : Text                   — display name (CUSTOM_NAME if set, else item name)
 *   .get(DataComponentTypes.LORE)       — LoreComponent? (null if no lore)
 *   .get(DataComponentTypes.CUSTOM_DATA) — NbtComponent? (null if no custom data)
 *
 * LoreComponent:
 *   .lines() : List<Text>              — unstyled lore Text lines
 *
 * NbtComponent:
 *   .copyNbt() : NbtCompound           — returns a copy of the underlying compound
 *
 * NbtCompound (net.minecraft.nbt):
 *   .contains(key) : Boolean
 *   .getKeys() : Set<String>
 *   .get(key) : NbtElement?            — raw element access; call .type on the result
 *   .getString(key, fallback) : String — string with fallback (not Optional in 1.21.11)
 *   .getCompoundOrEmpty(key) : NbtCompound — sub-compound or empty compound (not Optional)
 *   NbtElement.STRING_TYPE = 8.toByte()
 *
 * Text:
 *   .getString() : String              — full string content (may include § codes from
 *                                        SkyBlock's use of literal § in Text literals)
 *
 * Confidence: HIGH for all of the above — these are stable Yarn names present
 * in yarn-1.21.1+build.3 and corroborated by the 1.21.8 docs; the 1.21.11+build.6
 * yarn line is a patch release with no breaking API changes in these classes.
 * The one uncertain area: SkyBlock's custom_data ExtraAttributes compound uses
 * raw NbtCompound keyed by "ExtraAttributes"; we use copyNbt() then look up the
 * sub-compound. This is the conventional approach used by every SkyBlock parser
 * (SkyHanni, Skyblocker) and is correct for current Hypixel encoding.
 */
object AtlasAdapter {

    /**
     * Convenience entry point: parses the open container and runs it through
     * [MenuRegistry].
     *
     * Returns a [Pair] of the parsed menu and the first matching
     * [MenuRecognizer] (or null when no recognizer claims this menu —
     * fall through to vanilla). Placed here (not on [Atlas]) so the Atlas
     * facade stays free of net.minecraft.* imports (ADR 0003 §1).
     *
     * §2 compliance: [parse] only reads data already delivered to the client
     * in the container-open packet.
     */
    fun parseAndRecognize(title: Component, handler: AbstractContainerMenu): Pair<ParsedMenu, MenuRecognizer?> {
        val menu = parse(title, handler)
        val recognizer = MenuRegistry.recognize(menu)
        return menu to recognizer
    }

    /**
     * Parses the open container described by [title] and [handler] into a
     * [ParsedMenu] with all § codes stripped and ExtraAttributes flattened.
     */
    fun parse(title: Component, handler: AbstractContainerMenu): ParsedMenu {
        // Strip § codes from the raw title, then strip any trailing click-hints
        // (e.g. "SkyBlock Menu (Click)" → "SkyBlock Menu"). Punch-list item.
        val strippedTitle = stripClickHints(stripCodes(title.getString()))

        // Container slots ONLY — exclude the player inventory (hotbar + main inv).
        // A SkyBlock menu IS the container's slots; the player's own items must
        // never appear as menu entries (data-layer fix). Container slots are
        // registered with the handler before the player inventory, so a slot's
        // position in this filtered list equals its clickSlot slot id — that is
        // the index the backing-slot round-trip and the icon lookup both use, so
        // the indices stay aligned (punch-list #1 + #2).
        val containerSlots = handler.slots.filter { it.container !is Inventory }
        val size = containerSlots.size

        val items: List<ParsedItem?> = containerSlots.mapIndexed { slotId, slot ->
            parseItem(slotId, slot.item)
        }

        return ParsedMenu(
            title = strippedTitle,
            size = size,
            rows = size / 9,
            items = items,
        )
    }

    /**
     * Converts a single [ItemStack] at [slotIndex] into a [ParsedItem], or
     * returns null if the slot is empty.
     */
    private fun parseItem(slotIndex: Int, stack: ItemStack): ParsedItem? {
        if (stack.isEmpty) return null

        // Minecraft item registry id — e.g. "minecraft:gray_stained_glass_pane".
        // Used by classifyEntry to identify filler panes without relying on name heuristics.
        val itemId: String = BuiltInRegistries.ITEM.getKey(stack.item).toString()

        // Display name — getName() returns CUSTOM_NAME if set, else the item's
        // built-in name. SkyBlock always uses CUSTOM_NAME for its items.
        // rawName retains § color codes so deriveRarity can read the leading color.
        val rawName = stack.hoverName.getString()
        val name = stripCodes(rawName)

        // Lore — stored in DataComponentTypes.LORE as a LoreComponent.
        // lines() returns the List<Text> of unstyled lore entries.
        val loreComponent = stack.get(DataComponents.LORE)
        val lore: List<String> = loreComponent
            ?.lines()
            ?.map { stripCodes(it.getString()) }
            ?: emptyList()

        // ExtraAttributes — SkyBlock stores item NBT in the custom_data component
        // as a compound with a top-level "ExtraAttributes" sub-compound.
        val customData = stack.get(DataComponents.CUSTOM_DATA)
        val extraAttributes: Map<String, String>
        val skyblockId: String?

        if (customData != null) {
            val rootNbt: CompoundTag = customData.copyTag()
            // SkyBlock encodes extra data under the "ExtraAttributes" key.
            // In Yarn 1.21.11, getCompoundOrEmpty(key) returns NbtCompound (never null).
            val extraNbt: CompoundTag = if (rootNbt.contains("ExtraAttributes")) {
                rootNbt.getCompoundOrEmpty("ExtraAttributes")
            } else {
                // Fallback: treat the root compound itself as attributes if no
                // ExtraAttributes key is present (non-standard, but best-effort).
                rootNbt
            }

            // Flatten scalar (STRING type = 8) entries to Map<String,String>.
            // We skip non-string types (lists, compounds, numbers) to keep
            // the surface area predictable; callers needing numeric values
            // should use a typed parser in a future Atlas phase.
            //
            // In Yarn 1.21.11:
            //   NbtCompound.getKeys() : Set<String>
            //   NbtCompound.get(key)  : NbtElement? — call getType() on the element
            //   NbtElement.STRING_TYPE == 8
            //   NbtCompound.getString(key, fallback) : String  (avoids Optional unwrap)
            val attrs = mutableMapOf<String, String>()
            for (key in extraNbt.keySet()) {
                val element: Tag? = extraNbt.get(key)
                if (element != null && element.id == Tag.TAG_STRING) {
                    attrs[key] = extraNbt.getStringOr(key, "")
                }
            }
            extraAttributes = attrs
            skyblockId = attrs["id"]
        } else {
            extraAttributes = emptyMap()
            skyblockId = null
        }

        val rarity = parseRarity(lore, name)

        return ParsedItem(
            slot = slotIndex,
            name = name,
            lore = lore,
            rarity = rarity,
            skyblockId = skyblockId,
            count = stack.count,
            extraAttributes = extraAttributes,
            itemId = itemId,
            rawName = rawName,
        )
    }
}
