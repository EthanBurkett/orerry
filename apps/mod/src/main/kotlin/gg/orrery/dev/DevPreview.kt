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

        // A 9x6 chest's worth of mock items, exercising several rarities + lore.
        val inv = SimpleInventory(54)
        inv.setStack(10, mock(Items.NETHER_STAR, "§6SkyBlock Menu", listOf("§7Your profile, stats,", "§7and more.", "", "§eClick to open!")))
        inv.setStack(12, mock(Items.DIAMOND_SWORD, "§5Aspect of the End", listOf("§7Damage: §c+100", "§7Strength: §c+100", "", "§5EPIC SWORD")))
        inv.setStack(13, mock(Items.GOLDEN_APPLE, "§6Hegemony Artifact", listOf("§7Grants bonus stats.", "", "§6§lLEGENDARY ACCESSORY")))
        inv.setStack(14, mock(Items.ENCHANTED_BOOK, "§dGiant Killer V", listOf("§7Deal more damage to", "§7high-health enemies.", "", "§9§lRARE")))
        inv.setStack(15, mock(Items.WHEAT, "§fWheat", listOf("§7A common crop.", "", "§f§lCOMMON")))
        inv.setStack(16, mock(Items.DRAGON_EGG, "§c§lWither", listOf("§7Catacombs class.", "", "§c§lSPECIAL")))
        inv.setStack(28, mock(Items.EMERALD, "§aProfile: Mango", listOf("§7Coins: §6128,402", "", "§a§lUNCOMMON")))
        inv.setStack(31, mock(Items.BARRIER, "§cClose", listOf("§7Close this menu.")))
        inv.setStack(34, mock(Items.CLOCK, "§bCalendar & Events", listOf("§7View upcoming events.", "", "§9§lRARE")))

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
