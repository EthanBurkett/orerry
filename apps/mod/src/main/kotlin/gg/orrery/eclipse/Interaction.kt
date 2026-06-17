package gg.orrery.eclipse

import net.minecraft.client.MinecraftClient
import net.minecraft.screen.slot.SlotActionType

/**
 * THE single compliance chokepoint for all menu interactions (DESIGN_SPEC §2.3, §6.3, §11;
 * ADR 0003 §2).
 *
 * This is the ONLY place in the entire codebase permitted to call the vanilla
 * `interactionManager.clickSlot(...)`. No other file, class, object, or function may call
 * `interactionManager.clickSlot` directly — doing so is a §11 compliance violation and will
 * be caught and fail the build by `ComplianceFitnessTest`.
 *
 * ## Why this exists
 *
 * Orrery intercepts SkyBlock menus and renders its own UI in their place (Eclipse/Lumen).
 * When the user clicks an Orrery widget, the action must be routed back through the vanilla
 * interaction path so the server receives byte-identical traffic to a normal player clicking
 * that slot. This function is the single gate through which every such action must pass.
 *
 * ## What this function does (and does NOT do)
 *
 * - Calls `ClientPlayerInteractionManager.clickSlot(...)` exactly as vanilla would, emitting
 *   the identical `ClickSlotC2SPacket` that vanilla emits.
 * - Constructs NO packets. Performs NO queuing, batching, timers, reordering, or synthesized
 *   actions of any kind.
 * - Is a no-op if `player` or `interactionManager` is null (e.g. not in-game).
 * - Must only be invoked from a direct user-input handler. It must NEVER be called from a
 *   scheduler, coroutine timer, render loop, or any automated path. See DESIGN_SPEC §11.
 *
 * ## How to call it
 *
 * All callers should use this object's convenience wrappers (`leftClick`, `rightClick`,
 * `shiftClick`) or this function directly. Never bypass it.
 *
 * Yarn 1.21.11 names used:
 * - `MinecraftClient.getInstance()` → `net.minecraft.client.MinecraftClient`
 * - `MinecraftClient.interactionManager` → `net.minecraft.client.network.ClientPlayerInteractionManager`
 * - `ClientPlayerInteractionManager.clickSlot(int syncId, int slotId, int button, SlotActionType action, PlayerEntity player)`
 * - `SlotActionType` → `net.minecraft.screen.slot.SlotActionType`
 */
object Interaction {

    /**
     * The sole sanctioned call to the vanilla `interactionManager.clickSlot`. See class-level
     * KDoc for the full compliance contract.
     *
     * @param syncId     The `syncId` of the open `ScreenHandler` (obtain from `handler.syncId`).
     * @param slotIndex  The real backing slot index this widget maps to.
     * @param button     Mouse button: 0 = left, 1 = right, 2 = middle.
     * @param action     The `SlotActionType` (PICKUP, QUICK_MOVE, THROW, etc.).
     */
    fun clickSlot(syncId: Int, slotIndex: Int, button: Int, action: SlotActionType) {
        val client = MinecraftClient.getInstance()
        val player = client.player ?: return          // not in-game — no-op
        val manager = client.interactionManager ?: return  // manager unavailable — no-op
        manager.clickSlot(syncId, slotIndex, button, action, player)
    }

    // -------------------------------------------------------------------------
    // Convenience wrappers — all route through clickSlot above; none bypass it.
    // -------------------------------------------------------------------------

    /**
     * Left-click (PICKUP) the slot at [slotIndex] in the handler identified by [syncId].
     * Equivalent to a plain left mouse button press on a vanilla slot.
     */
    fun leftClick(syncId: Int, slotIndex: Int) =
        clickSlot(syncId, slotIndex, 0, SlotActionType.PICKUP)

    /**
     * Right-click (PICKUP with button=1) the slot at [slotIndex].
     * Equivalent to a right mouse button press on a vanilla slot (picks up half the stack,
     * or places one item).
     */
    fun rightClick(syncId: Int, slotIndex: Int) =
        clickSlot(syncId, slotIndex, 1, SlotActionType.PICKUP)

    /**
     * Shift-click (QUICK_MOVE) the slot at [slotIndex].
     * Equivalent to Shift+Left-click on a vanilla slot.
     */
    fun shiftClick(syncId: Int, slotIndex: Int) =
        clickSlot(syncId, slotIndex, 0, SlotActionType.QUICK_MOVE)
}
