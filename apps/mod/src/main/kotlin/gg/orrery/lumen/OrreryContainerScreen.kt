package gg.orrery.lumen

import gg.orrery.eclipse.Interaction
import gg.orrery.generated.Tokens
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.screen.GenericContainerScreenHandler
import net.minecraft.screen.slot.SlotActionType
import net.minecraft.text.Text

/**
 * OrreryContainerScreen — the first Orrery custom menu (DESIGN_SPEC §6.3, §6.4; ADR 0003 §4).
 *
 * Renders a recognized SkyBlock chest container as a fully custom Orrery screen, reading slots
 * from the LIVE [GenericContainerScreenHandler] (the same handler/`syncId` the vanilla screen
 * would have used — see [gg.orrery.eclipse.EclipseViews]). Clicks route through the single
 * compliance chokepoint [Interaction.clickSlot]; vanilla `clickSlot` never fires from here.
 *
 * ## Compliance posture (§2, §11)
 *
 * - Extends [HandledScreen] so the vanilla container open/close lifecycle (and the close
 *   packet on Esc / screen change) stays byte-correct. We only replace the *visual* screen.
 * - We REUSE the passed-in handler; we never build/replace it. Same `syncId` -> clicks hit
 *   real server slots.
 * - [mouseClicked] hit-tests our own grid, resolves the backing slot index, and calls
 *   [Interaction.clickSlot] for the slot the user physically clicked — no synthesized,
 *   queued, or timed actions. It returns true WITHOUT calling super's slot handling, so
 *   vanilla `clickSlot` is never invoked.
 *
 * ## Visual identity (§5, §5.1)
 *
 * - Void background, a `surface.1` panel with a hairline border, the menu title, and each
 *   container slot as a `surface.2` cell with a hairline (hover -> `surface.3`). Laid out in
 *   Orrery's OWN grid, not vanilla slot positions.
 * - All colors from [Tokens.Color]. No chest texture, no beveled buttons, no vanilla slot
 *   grid. Item icons are drawn via [DrawContext.drawItem] — those are data (server item
 *   models), allowed per ADR 0004. Glyphs use the vanilla font for now via [LumenDraw.text]
 *   (TODO(phase2-msdf)).
 *
 * ## Yarn 1.21.11 names used
 *   HandledScreen<T : ScreenHandler>                                  = class_465
 *   HandledScreen(handler, PlayerInventory, Text title)              ctor (Ldhi;Lddl;Lyh;)V
 *   HandledScreen.drawBackground(DrawContext, float, int, int)        = method_2389
 *   HandledScreen.drawForeground(DrawContext, int, int)              = method_2388
 *   Screen.render(DrawContext, int mouseX, int mouseY, float delta)   = method_25394
 *   Screen.renderBackground(DrawContext, int, int, float)            = method_25420
 *   Element.mouseClicked(Click, boolean) : boolean                   = method_25402
 *   Click.x()/y() : double, Click.button() : int                     = class_11909
 *   GenericContainerScreenHandler.getRows() : int                    = method_17388
 *   ScreenHandler.slots : DefaultedList<Slot>; Slot.index, Slot.getStack()
 *   DrawContext.fill / drawItem / drawStackOverlay / drawText
 *   SlotActionType.PICKUP / QUICK_MOVE
 */
class OrreryContainerScreen(
    handler: GenericContainerScreenHandler,
    playerInventory: PlayerInventory,
    title: Text,
) : HandledScreen<GenericContainerScreenHandler>(handler, playerInventory, title) {

    // --- Orrery grid geometry (px). Computed in [init]. Independent of vanilla positions. ---
    private val cellSize = 20          // slot cell edge
    private val cellGap = 2            // gap between cells
    private val panelPadding = 12      // inner padding of the surface.1 panel
    private val titleBarHeight = 22    // space reserved for the title above the grid

    private var cols = 9
    private var rows = 1

    private var gridX = 0              // top-left of the slot grid
    private var gridY = 0
    private var panelX = 0
    private var panelY = 0
    private var panelW = 0
    private var panelH = 0

    /** Number of container slots (rows * 9), i.e. the menu proper, excluding player inventory. */
    private val containerSlotCount: Int
        get() = handler.rows * 9

    override fun init() {
        super.init()
        cols = 9
        rows = handler.rows

        val gridW = cols * cellSize + (cols - 1) * cellGap
        val gridH = rows * cellSize + (rows - 1) * cellGap

        panelW = gridW + panelPadding * 2
        panelH = gridH + panelPadding * 2 + titleBarHeight
        panelX = (width - panelW) / 2
        panelY = (height - panelH) / 2

        gridX = panelX + panelPadding
        gridY = panelY + panelPadding + titleBarHeight
    }

    // -------------------------------------------------------------------------------------
    // Suppress ALL vanilla chrome. drawBackground would paint the chest texture; drawForeground
    // would paint the vanilla title + "Inventory" label. We draw our own in [render].
    // -------------------------------------------------------------------------------------

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        // no-op — no chest texture, no vanilla slot grid (§5.1).
    }

    override fun drawForeground(context: DrawContext, mouseX: Int, mouseY: Int) {
        // no-op — Orrery draws its own title in [render]; suppress vanilla labels.
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        // Dim/blur the world behind, like any screen, then draw 100% Orrery content.
        renderBackground(context, mouseX, mouseY, delta)

        // Void backdrop across the panel area + the surface.1 panel with hairline border.
        LumenDraw.fillRect(context, 0, 0, width, height, OVERLAY_SCRIM)
        LumenDraw.panel(
            context, panelX, panelY, panelW, panelH,
            fill = Tokens.Color.voidS1, border = Tokens.Color.hairlineBase,
        )

        // Title (token text, vanilla glyphs for now — TODO(phase2-msdf)).
        LumenDraw.text(
            context, textRenderer, title.string,
            panelX + panelPadding, panelY + panelPadding,
            Tokens.Color.textHi, shadow = false,
        )

        // Slot grid: each container slot a surface.2 cell with hairline; hover -> surface.3.
        val hovered = slotIndexAt(mouseX.toDouble(), mouseY.toDouble())
        for (index in 0 until containerSlotCount) {
            val (cx, cy) = cellTopLeft(index)
            val isHover = index == hovered
            LumenDraw.panel(
                context, cx, cy, cellSize, cellSize,
                fill = if (isHover) Tokens.Color.voidS3 else Tokens.Color.voidS2,
                border = if (isHover) Tokens.Color.hairlineBright else Tokens.Color.hairlineBase,
            )

            val slot = handler.slots.getOrNull(index) ?: continue
            val stack = slot.stack
            if (!stack.isEmpty) {
                // Item icons are data (server models), allowed per ADR 0004.
                context.drawItem(stack, cx + 2, cy + 2)
                context.drawStackOverlay(textRenderer, stack, cx + 2, cy + 2)
            }
        }

        // Vanilla tooltip for the hovered slot's stack (data, not chrome).
        if (hovered != null) {
            val stack = handler.slots.getOrNull(hovered)?.stack
            if (stack != null && !stack.isEmpty) {
                context.drawItemTooltip(textRenderer, stack, mouseX, mouseY)
            }
        }
    }

    /**
     * Route a physical click to the backing slot via the compliance chokepoint.
     *
     * Returns true when the click lands on a container cell (we handled it and DO NOT call
     * super, so vanilla `clickSlot` never fires). Off-grid clicks fall through to vanilla
     * screen behavior (e.g. closing on outside-click).
     */
    override fun mouseClicked(click: Click, doubled: Boolean): Boolean {
        val index = slotIndexAt(click.x, click.y)
        if (index != null) {
            val slot = handler.slots.getOrNull(index)
            if (slot != null) {
                val button = click.button()                 // 0 = left, 1 = right
                val shift = MinecraftClient.getInstance().isShiftPressed
                val action = when {
                    button == 1 -> SlotActionType.PICKUP                                  // right-click
                    shift -> SlotActionType.QUICK_MOVE                                     // shift+left
                    else -> SlotActionType.PICKUP                                          // left-click
                }
                // THE only sanctioned action path (§2.3, §11). Real slot index, same syncId.
                Interaction.clickSlot(handler.syncId, slot.index, button, action)
            }
            return true
        }
        return super.mouseClicked(click, doubled)
    }

    // ----------------------------------- geometry -----------------------------------------

    /** Top-left (x,y) of the cell for container slot [index] in the Orrery grid. */
    private fun cellTopLeft(index: Int): Pair<Int, Int> {
        val col = index % cols
        val row = index / cols
        val x = gridX + col * (cellSize + cellGap)
        val y = gridY + row * (cellSize + cellGap)
        return x to y
    }

    /**
     * Hit-test: returns the container slot index under (mx,my), or null if none.
     * Only the menu's own slots (0 until rows*9) are hit-testable — never the player inventory.
     */
    private fun slotIndexAt(mx: Double, my: Double): Int? {
        for (index in 0 until containerSlotCount) {
            val (cx, cy) = cellTopLeft(index)
            if (mx >= cx && mx < cx + cellSize && my >= cy && my < cy + cellSize) {
                return index
            }
        }
        return null
    }

    private companion object {
        /** A faint void scrim over the whole screen behind the panel (token void at low alpha). */
        private val OVERLAY_SCRIM = (Tokens.Color.voidBase and 0x00FFFFFF) or (0xC0 shl 24)
    }
}
