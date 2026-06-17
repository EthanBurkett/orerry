package gg.orrery.dev

import com.mojang.brigadier.Command
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.LoreComponent
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.screen.GenericContainerScreenHandler
import net.minecraft.screen.ScreenHandlerType
import net.minecraft.text.Text

/**
 * Dev-only offline preview harness.
 *
 * The user has no Mojang session and cannot join Hypixel, so the Eclipse
 * interception + Lumen render can't be verified against the live SkyBlock menu.
 * Instead, the `/orrery preview` client command opens a SYNTHETIC
 * `GenericContainerScreen` titled "SkyBlock Menu" with mock SkyBlock-style items
 * (names + lore encoding rarity). That triggers the exact same interception path
 * — `MinecraftClientMixin` → `EclipseViews.viewFor` → `OrreryContainerScreen` —
 * so the whole Phase 1 vertical is verifiable in an offline singleplayer world
 * via `./gradlew runClient`.
 *
 * Registered ONLY in the development environment (see OrreryMod). It never ships
 * in a production build and sends nothing to any server — it builds a local mock
 * inventory and opens a screen.
 */
object DevPreview {

    fun register() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.register(
                ClientCommandManager.literal("orrery").then(
                    ClientCommandManager.literal("preview").executes { _ ->
                        MinecraftClient.getInstance().execute { openSkyBlockMenuPreview() }
                        Command.SINGLE_SUCCESS
                    },
                ),
            )
        }
    }

    private fun openSkyBlockMenuPreview() {
        val client = MinecraftClient.getInstance()
        val player = client.player ?: return

        // A 9x6 chest's worth of mock items modelled on the REAL SkyBlock main menu, so the
        // Phase 2 SkyBlockMenuView shows the semantic card list well offline (ADR 0005 §1 / §3
        // "DevPreview mock is updated to resemble the real SkyBlock menu"). Each entry has a
        // thematic item + name + lore (rarity line included) so parseEntries yields good
        // titles/subtitles; one lore line carries a "Purse:" value so the header purse renders.
        val inv = SimpleInventory(54)
        inv.setStack(10, mock(Items.PLAYER_HEAD, "§aYour Skills", listOf("§7Average level §e24.6", "§7Click to view your skills.", "", "§a§lUNCOMMON")))
        inv.setStack(11, mock(Items.PAINTING, "§aCollections", listOf("§7412/588 unlocked", "§7Track gathered resources.", "", "§a§lUNCOMMON")))
        inv.setStack(12, mock(Items.KNOWLEDGE_BOOK, "§eRecipe Book", listOf("§7Browse all known recipes.", "", "§f§lCOMMON")))
        inv.setStack(13, mock(Items.CRAFTING_TABLE, "§eCrafting Table", listOf("§7Craft items anywhere.", "", "§f§lCOMMON")))
        inv.setStack(14, mock(Items.EXPERIENCE_BOTTLE, "§bSkyBlock Leveling", listOf("§7Level §e142", "§7Earn rewards as you progress.", "", "§9§lRARE")))
        inv.setStack(15, mock(Items.ENDER_CHEST, "§9Storage", listOf("§7Access your backpacks", "§7and ender chest.", "", "§9§lRARE")))
        inv.setStack(16, mock(Items.LEATHER_CHESTPLATE, "§aWardrobe", listOf("§79 armor sets", "§7Swap your equipped armor.", "", "§a§lUNCOMMON")))
        inv.setStack(19, mock(Items.BONE, "§6Pets", listOf("§712 owned · 1 active", "§7Summon a loyal companion.", "", "§6§lLEGENDARY")))
        inv.setStack(20, mock(Items.EMERALD, "§eTrades", listOf("§7Trade with NPCs.", "", "§f§lCOMMON")))
        inv.setStack(21, mock(Items.COMPASS, "§bFast Travel", listOf("§7Warp across the islands.", "", "§9§lRARE")))
        inv.setStack(22, mock(Items.NETHER_STAR, "§6Profile: Mango", listOf("§7Purse: §6184,402,118", "§7Bits: §b12,840", "", "§6§lLEGENDARY")))
        inv.setStack(23, mock(Items.CLOCK, "§bCalendar & Events", listOf("§7View upcoming events.", "", "§9§lRARE")))
        inv.setStack(24, mock(Items.DIAMOND, "§bMinions", listOf("§7Manage your minions.", "", "§9§lRARE")))
        inv.setStack(31, mock(Items.BARRIER, "§cClose", listOf("§7Close this menu.")))

        val handler = GenericContainerScreenHandler(
            ScreenHandlerType.GENERIC_9X6, 0, player.inventory, inv, 6,
        )
        client.setScreen(GenericContainerScreen(handler, player.inventory, Text.literal("SkyBlock Menu")))
    }

    private fun mock(item: Item, name: String, lore: List<String>): ItemStack {
        val stack = ItemStack(item)
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name))
        if (lore.isNotEmpty()) {
            stack.set(DataComponentTypes.LORE, LoreComponent(lore.map { Text.literal(it) }))
        }
        return stack
    }
}
