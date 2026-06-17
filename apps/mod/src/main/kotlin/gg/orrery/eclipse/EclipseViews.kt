package gg.orrery.eclipse

import gg.orrery.atlas.AtlasAdapter
import gg.orrery.lumen.OrreryContainerScreen
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.screen.GenericContainerScreenHandler
import net.minecraft.text.Text

/**
 * EclipseViews — the menu ownership registry (DESIGN_SPEC §6.3, ADR 0003 §4).
 *
 * Maps an Atlas recognizer id -> a [ViewFactory] that builds an Orrery Lumen [Screen]
 * from the *live* [GenericContainerScreenHandler] (same `syncId`, so clicks hit real
 * slots) and its title.
 *
 * ## Flow (the interception spine)
 *
 * The `MinecraftClientMixin` intercepts `setScreen(...)`. When the incoming screen is a
 * vanilla chest screen (a `GenericContainerScreen`, which every SkyBlock menu uses) and
 * is NOT already an Orrery screen, it calls [viewFor]. [viewFor] runs
 * [AtlasAdapter.parseAndRecognize] (Atlas) and, if a recognizer owns the menu and a
 * factory is registered for that recognizer id, builds the Orrery [Screen]. The mixin
 * then swaps the vanilla screen for it.
 *
 * ## Compliance (§2)
 *
 * This is render-only routing. The swap REUSES the existing [GenericContainerScreenHandler]
 * — it does NOT create, replace, or suppress the handler, so the open/close lifecycle and
 * the close packet remain 100% vanilla-correct. No packet is constructed here. The only
 * path that ever acts on the menu is [Interaction.clickSlot], called from the Orrery screen
 * on a physical user click.
 *
 * Unrecognized menus return null from [viewFor] -> the mixin leaves vanilla untouched
 * (degrade, never break — §6.5).
 */
object EclipseViews {

    /**
     * Builds an Orrery [Screen] from a live container handler + title.
     *
     * The factory obtains the [PlayerInventory] itself (from the client player) so the
     * resulting [net.minecraft.client.gui.screen.ingame.HandledScreen] has the inventory
     * it needs for the vanilla open/close lifecycle.
     */
    fun interface ViewFactory {
        fun build(handler: GenericContainerScreenHandler, title: Text, playerInventory: PlayerInventory): Screen
    }

    private val factories = mutableMapOf<String, ViewFactory>()

    /** Registers [factory] as the owner of menus recognized as [recognizerId]. */
    fun register(recognizerId: String, factory: ViewFactory) {
        factories[recognizerId] = factory
    }

    /**
     * Returns an Orrery [Screen] for [handler]+[title] if a recognizer owns the menu and a
     * factory is registered for it; otherwise null (fall through to vanilla).
     *
     * Runs Atlas (parse + recognize) against the live handler. Reuses [handler] verbatim,
     * so the produced screen shares the same `syncId` as the vanilla screen would have.
     */
    fun viewFor(handler: GenericContainerScreenHandler, title: Text): Screen? {
        val (_, recognizer) = AtlasAdapter.parseAndRecognize(title, handler)
        val factory = recognizer?.let { factories[it.id] } ?: return null
        val playerInventory = MinecraftClient.getInstance().player?.inventory ?: return null
        return factory.build(handler, title, playerInventory)
    }

    /**
     * JVM-callable entry point for the Java `MinecraftClientMixin`.
     *
     * Kept as a plain static-style method (callable as `EclipseViews.INSTANCE.viewFor(...)`
     * from Java) so the mixin needs no Kotlin-specific interop gymnastics. Returns null when
     * the menu is not owned by Orrery.
     */
    @JvmStatic
    fun orreryScreenFor(handler: GenericContainerScreenHandler, title: Text): Screen? =
        viewFor(handler, title)

    /**
     * Registers all built-in views. Phase 1 maps the SkyBlock main menu
     * (recognizer id "skyblock_menu") to the Orrery container screen.
     *
     * Call once at init, after Atlas recognizers are registered.
     */
    fun registerDefaults() {
        register("skyblock_menu") { handler, title, inv ->
            OrreryContainerScreen(handler, inv, title)
        }
    }

    /** Clears all registered factories. Intended for tests only. */
    internal fun reset() {
        factories.clear()
    }
}
