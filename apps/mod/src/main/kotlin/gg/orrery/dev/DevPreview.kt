package gg.orrery.dev

import com.mojang.brigadier.Command
import net.fabricmc.fabric.api.client.command.v2.ClientCommands
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.component.ItemLore
import net.minecraft.world.SimpleContainer
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.inventory.ChestMenu
import net.minecraft.world.inventory.MenuType
import net.minecraft.network.chat.Component

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
                ClientCommands.literal("orrery").then(
                    ClientCommands.literal("preview").executes { _ ->
                        Minecraft.getInstance().execute { openSkyBlockMenuPreview() }
                        Command.SINGLE_SUCCESS
                    },
                ),
            )
        }
    }

    private fun openSkyBlockMenuPreview() {
        val client = Minecraft.getInstance()
        val player = client.player ?: return

        // A 9x6 chest's worth of mock items modelled on the REAL SkyBlock main menu, so the
        // Phase 2 SkyBlockMenuView shows the semantic card list well offline (ADR 0005 §1 / §3
        // "DevPreview mock is updated to resemble the real SkyBlock menu"). Each entry has a
        // thematic item + name + lore (rarity line included) so parseEntries yields good
        // titles/subtitles; one lore line carries a "Purse:" value so the header purse renders.
        val inv = SimpleContainer(54)
        inv.setItem(10, mock(Items.PLAYER_HEAD, "§aYour Skills", listOf("§7Average level §e24.6", "§7Click to view your skills.", "", "§a§lUNCOMMON")))
        inv.setItem(11, mock(Items.PAINTING, "§aCollections", listOf("§7412/588 unlocked", "§7Track gathered resources.", "", "§a§lUNCOMMON")))
        inv.setItem(12, mock(Items.KNOWLEDGE_BOOK, "§eRecipe Book", listOf("§7Browse all known recipes.", "", "§f§lCOMMON")))
        inv.setItem(13, mock(Items.CRAFTING_TABLE, "§eCrafting Table", listOf("§7Craft items anywhere.", "", "§f§lCOMMON")))
        inv.setItem(14, mock(Items.EXPERIENCE_BOTTLE, "§bSkyBlock Leveling", listOf("§7Level §e142", "§7Earn rewards as you progress.", "", "§9§lRARE")))
        inv.setItem(15, mock(Items.ENDER_CHEST, "§9Storage", listOf("§7Access your backpacks", "§7and ender chest.", "", "§9§lRARE")))
        inv.setItem(16, mock(Items.LEATHER_CHESTPLATE, "§aWardrobe", listOf("§79 armor sets", "§7Swap your equipped armor.", "", "§a§lUNCOMMON")))
        inv.setItem(19, mock(Items.BONE, "§6Pets", listOf("§712 owned · 1 active", "§7Summon a loyal companion.", "", "§6§lLEGENDARY")))
        inv.setItem(20, mock(Items.EMERALD, "§eTrades", listOf("§7Trade with NPCs.", "", "§f§lCOMMON")))
        inv.setItem(21, mock(Items.COMPASS, "§bFast Travel", listOf("§7Warp across the islands.", "", "§9§lRARE")))
        inv.setItem(22, mock(Items.NETHER_STAR, "§6Profile: Mango", listOf("§7Purse: §6184,402,118", "§7Bits: §b12,840", "", "§6§lLEGENDARY")))
        inv.setItem(23, mock(Items.CLOCK, "§bCalendar & Events", listOf("§7View upcoming events.", "", "§9§lRARE")))
        inv.setItem(24, mock(Items.DIAMOND, "§bMinions", listOf("§7Manage your minions.", "", "§9§lRARE")))
        inv.setItem(31, mock(Items.BARRIER, "§cClose", listOf("§7Close this menu.")))

        val handler = ChestMenu(
            MenuType.GENERIC_9x6, 0, player.inventory, inv, 6,
        )
        client.setScreen(ContainerScreen(handler, player.inventory, Component.literal("SkyBlock Menu")))
    }

    private fun mock(item: Item, name: String, lore: List<String>): ItemStack {
        val stack = ItemStack(item)
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name))
        if (lore.isNotEmpty()) {
            stack.set(DataComponents.LORE, ItemLore(lore.map { Component.literal(it) }))
        }
        return stack
    }
}
