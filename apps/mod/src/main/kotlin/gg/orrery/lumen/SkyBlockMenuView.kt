package gg.orrery.lumen

import gg.orrery.atlas.AtlasAdapter
import gg.orrery.atlas.MenuEntry
import gg.orrery.atlas.parseEntries
import gg.orrery.eclipse.Interaction
import gg.orrery.generated.Tokens
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.input.CharInput
import net.minecraft.client.input.KeyInput
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.item.ItemStack
import net.minecraft.screen.GenericContainerScreenHandler
import net.minecraft.screen.slot.SlotActionType
import net.minecraft.text.Text
import org.lwjgl.glfw.GLFW

/**
 * SkyBlockMenuView — the semantic card re-render of the SkyBlock main menu
 * (DESIGN_SPEC §5/§5.1/§5.2/§6.3/§6.4; DESIGN.md "Custom menu rendering"; ADR 0005 §1/§2).
 *
 * The headline Phase 2 deliverable. Instead of the literal slot grid that
 * [OrreryContainerScreen] (the generic fallback) draws, this view renders the recognized
 * SkyBlock menu as the user's mockup: a surface.1 panel with a header (title + optional purse +
 * close ×), a search field that filters entries by title, a 2-column list of labelled entry
 * cards (icon + title + subtitle + chevron, with hover + a rarity tick), and a footer.
 *
 * All content derives from the LIVE [GenericContainerScreenHandler] (same handler / `syncId` the
 * vanilla screen would have used — see [gg.orrery.eclipse.EclipseViews]):
 *   1. Read  — [AtlasAdapter.parse] → [parseEntries] turns slots into semantic [MenuEntry]s; the
 *              icon [ItemStack] for each is fetched back from `handler.slots[backingSlot]`.
 *   2. Render — the mockup, drawn from [LumenWidgets] (tokens + Orrery fonts only).
 *   3. Route — a card click resolves to its backing slot and calls [Interaction.clickSlot].
 *
 * ## Compliance posture (§2, §11)
 * - Extends [HandledScreen] so the vanilla open/close lifecycle + close packet stay byte-correct;
 *   we only replace the *visual* screen. We REUSE the passed-in handler — never build/replace it.
 * - [mouseClicked] hit-tests our own cards, resolves the backing slot, and calls
 *   [Interaction.clickSlot] for the slot the user physically clicked. No vanilla `clickSlot`, no
 *   synthesized / queued / timed actions. Right-click → button 1 PICKUP, shift → QUICK_MOVE,
 *   else PICKUP.
 * - Search filtering is display-only: it only changes which already-received entries are shown.
 *
 * ## Visual identity (§5 restraint, ADR 0005 §2)
 * Colors come ONLY from [Tokens.Color]; text ONLY through [LumenDraw]/[LumenWidgets] (Orrery TTF
 * fonts). Backgrounds void/surface, borders hairline(-bright), title text-hi, subtitle text-mid,
 * hints text-low, key numerals brass-hi, interactive accents cyan. Entry icons are the real items
 * (drawItem) — no invented per-category color; rarity tick uses game-data rarity tokens.
 *
 * ## Yarn 1.21.11 names used
 *   HandledScreen<T : ScreenHandler>(handler, PlayerInventory, Text)   = class_465 ctor
 *   HandledScreen.drawBackground(DrawContext, float, int, int)         = method_2389  (no-op here)
 *   HandledScreen.drawForeground(DrawContext, int, int)               = method_2388  (no-op here)
 *   Screen.render(DrawContext, int mouseX, int mouseY, float delta)    = method_25394
 *   Screen.renderBackground(DrawContext, int, int, float)             = method_25420
 *   Element.mouseClicked(Click, boolean) : boolean                    = method_25402
 *   Element.mouseScrolled(double,double,double horiz,double vert):bool = method_25401
 *   Element.charTyped(CharInput) : boolean                            = method_25400
 *   Screen.keyPressed(KeyInput) : boolean                             = method_25404
 *   Click.x()/y():double · Click.button():int                         = class_11909
 *   CharInput.codepoint():int · KeyInput.key():int (GLFW key)         = class_8016/class_8019-era
 *   GenericContainerScreenHandler.rows:int · ScreenHandler.slots · syncId
 *   DrawContext.fill / drawItem / drawStackOverlay / enableScissor / disableScissor
 *   SlotActionType.PICKUP / QUICK_MOVE
 */
class SkyBlockMenuView(
    handler: GenericContainerScreenHandler,
    playerInventory: PlayerInventory,
    title: Text,
) : HandledScreen<GenericContainerScreenHandler>(handler, playerInventory, title) {

    // -- parsed entries (from the live handler, once at construction) --
    private val entries: List<MenuEntry> = run {
        val parsed = AtlasAdapter.parse(title, handler)
        parseEntries(parsed)
    }

    // -- search state (display-only filtering) --
    private var query: String = ""
    private var searchFocused: Boolean = false

    // -- scroll state --
    private var scrollOffset: Int = 0   // px scrolled down

    // -- panel / layout geometry (computed in [init]) --
    private var panelX = 0
    private var panelY = 0
    private var panelW = 0
    private var panelH = 0
    private var closeRect: IntArray? = null   // [x,y,w,h] of the header close glyph

    // grid: 2 columns of cards inside the body viewport
    private val columns = 2
    private val cardGap = LumenWidgets.CARD_GAP

    // ── filtered view of entries (case-insensitive substring on title) ──
    private val visibleEntries: List<MenuEntry>
        get() = if (query.isBlank()) entries
        else entries.filter { it.title.contains(query.trim(), ignoreCase = true) }

    override fun init() {
        super.init()
        // Generous, fixed panel sized to the screen; body scrolls if entries overflow.
        panelW = (width * 0.62).toInt().coerceIn(360, 560)
        panelH = (height * 0.80).toInt().coerceIn(280, 520)
        panelX = (width - panelW) / 2
        panelY = (height - panelH) / 2
        scrollOffset = 0
    }

    // -- inner content rectangle (inside panel padding) --
    private val padding = 14

    private val contentX get() = panelX + padding
    private val contentW get() = panelW - padding * 2
    private val headerY get() = panelY + padding
    private val searchY get() = headerY + LumenWidgets.HEADER_HEIGHT + 8
    private val bodyY get() = searchY + LumenWidgets.SEARCH_HEIGHT + 10
    private val footerH = 22
    private val bodyBottom get() = panelY + panelH - padding - footerH
    private val bodyH get() = bodyBottom - bodyY

    private val cardW get() = (contentW - (columns - 1) * cardGap) / columns

    /** Total scrollable content height (all rows of cards). */
    private val contentHeight: Int
        get() {
            val n = visibleEntries.size
            if (n == 0) return 0
            val rows = (n + columns - 1) / columns
            return rows * LumenWidgets.CARD_HEIGHT + (rows - 1) * cardGap
        }

    private val maxScroll: Int
        get() = (contentHeight - bodyH).coerceAtLeast(0)

    // ── suppress vanilla chrome (§5.1) ─────────────────────────────────────────

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        // no-op — no chest texture, no vanilla slot grid.
    }

    override fun drawForeground(context: DrawContext, mouseX: Int, mouseY: Int) {
        // no-op — Orrery draws its own title/labels in [render].
    }

    // ── render ─────────────────────────────────────────────────────────────────

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context, mouseX, mouseY, delta)

        // scrim + surface.1 panel + hairline border
        LumenDraw.fillRect(context, 0, 0, width, height, OVERLAY_SCRIM)
        LumenDraw.panel(context, panelX, panelY, panelW, panelH, Tokens.Color.voidS1, Tokens.Color.hairlineBase)

        // header (title + optional purse + close ×)
        closeRect = LumenWidgets.header(
            context, textRenderer, contentX, headerY, contentW,
            title.string, derivePurse(), mouseX, mouseY,
        )

        // search field
        LumenWidgets.searchField(context, textRenderer, contentX, searchY, contentW, query, searchFocused)

        // body: 2-column scrollable card list, clipped to the body viewport
        scrollOffset = scrollOffset.coerceIn(0, maxScroll)
        val list = visibleEntries
        context.enableScissor(contentX, bodyY, contentX + contentW, bodyBottom)
        if (list.isEmpty()) {
            val msg = if (entries.isEmpty()) "No entries in this menu." else "No matches."
            LumenDraw.text(
                context, textRenderer, msg,
                contentX, bodyY + 6, Tokens.Color.textLow, font = LumenFonts.BODY,
            )
        } else {
            val hovered = cardIndexAt(mouseX.toDouble(), mouseY.toDouble())
            list.forEachIndexed { i, entry ->
                val (cx, cy) = cardTopLeft(i)
                // skip cards fully outside the viewport (cheap cull)
                if (cy + LumenWidgets.CARD_HEIGHT < bodyY || cy > bodyBottom) return@forEachIndexed
                LumenWidgets.entryCard(
                    context, textRenderer, cx, cy, cardW, entry,
                    iconFor(entry), hovered == i,
                )
            }
        }
        context.disableScissor()

        // footer: hairline divider + entry count (no fabricated stats — §2)
        val count = list.size
        val total = entries.size
        val left = if (query.isBlank()) "$total entries" else "$count of $total entries"
        LumenWidgets.footer(
            context, textRenderer, contentX, bodyBottom + 4, contentW,
            left, right = null,
        )

        // hovered card → vanilla item tooltip (data, not chrome)
        val hovered = cardIndexAt(mouseX.toDouble(), mouseY.toDouble())
        if (hovered != null) {
            val entry = visibleEntries.getOrNull(hovered)
            val stack = entry?.let { iconFor(it) }
            if (stack != null && !stack.isEmpty) {
                context.drawItemTooltip(textRenderer, stack, mouseX, mouseY)
            }
        }
    }

    // ── input: clicks ───────────────────────────────────────────────────────────

    override fun mouseClicked(click: Click, doubled: Boolean): Boolean {
        val mx = click.x
        val my = click.y

        // close ×
        closeRect?.let { r ->
            if (mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3]) {
                close()
                return true
            }
        }

        // focus / unfocus the search field
        searchFocused = mx >= contentX && mx < contentX + contentW &&
            my >= searchY && my < searchY + LumenWidgets.SEARCH_HEIGHT
        if (searchFocused) return true

        // card click → route to backing slot through the chokepoint
        val index = cardIndexAt(mx, my)
        if (index != null) {
            val entry = visibleEntries.getOrNull(index)
            if (entry != null) {
                val slot = handler.slots.getOrNull(entry.backingSlot)
                if (slot != null) {
                    val button = click.button()                 // 0 = left, 1 = right
                    val shift = MinecraftClient.getInstance().isShiftPressed
                    val action = when {
                        button == 1 -> SlotActionType.PICKUP            // right-click
                        shift -> SlotActionType.QUICK_MOVE              // shift+left
                        else -> SlotActionType.PICKUP                  // left-click
                    }
                    // THE only sanctioned action path (§2.3, §11). Real slot index, same syncId.
                    Interaction.clickSlot(handler.syncId, slot.index, button, action)
                }
            }
            return true
        }
        return super.mouseClicked(click, doubled)
    }

    // ── input: scroll ───────────────────────────────────────────────────────────

    override fun mouseScrolled(
        mouseX: Double,
        mouseY: Double,
        horizontalAmount: Double,
        verticalAmount: Double,
    ): Boolean {
        if (maxScroll <= 0) return true
        val step = (LumenWidgets.CARD_HEIGHT + cardGap)
        scrollOffset = (scrollOffset - (verticalAmount * step).toInt()).coerceIn(0, maxScroll)
        return true
    }

    // ── input: keyboard (search) ────────────────────────────────────────────────

    override fun charTyped(input: CharInput): Boolean {
        if (searchFocused) {
            val cp = input.codepoint()
            if (cp >= 0x20 && cp != 0x7F) {
                query += cp.toChar()
                scrollOffset = 0
                return true
            }
        }
        return super.charTyped(input)
    }

    override fun keyPressed(input: KeyInput): Boolean {
        if (searchFocused) {
            when (input.key()) {
                GLFW.GLFW_KEY_BACKSPACE -> {
                    if (query.isNotEmpty()) query = query.dropLast(1)
                    scrollOffset = 0
                    return true
                }
                GLFW.GLFW_KEY_ESCAPE -> {
                    // first Esc clears focus rather than closing the menu
                    searchFocused = false
                    return true
                }
                GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                    searchFocused = false
                    return true
                }
            }
        }
        // default handles Esc → close, etc.
        return super.keyPressed(input)
    }

    // ── data helpers ─────────────────────────────────────────────────────────────

    /** The live backing [ItemStack] for an entry, fetched from the handler's slot list. */
    private fun iconFor(entry: MenuEntry): ItemStack? =
        handler.slots.getOrNull(entry.backingSlot)?.stack

    /**
     * Derives a purse / coins value to show in the header, ONLY if one is genuinely present in
     * the parsed data (§2 — never fabricate). Looks for a "Purse: <num>" or "Coins: <num>" lore
     * line on any entry and renders it as a brass-hi key numeral. Returns null otherwise.
     */
    private fun derivePurse(): String? {
        val parsed = AtlasAdapter.parse(title, handler)
        for (item in parsed.items) {
            if (item == null) continue
            for (line in item.lore + item.name) {
                val m = PURSE_REGEX.find(line) ?: continue
                return "⛁ " + m.groupValues[1]
            }
        }
        return null
    }

    // ── geometry ─────────────────────────────────────────────────────────────────

    /** Top-left (x,y) of card [i] in the 2-column grid, in screen space (scroll-adjusted). */
    private fun cardTopLeft(i: Int): Pair<Int, Int> {
        val col = i % columns
        val row = i / columns
        val x = contentX + col * (cardW + cardGap)
        val y = bodyY + row * (LumenWidgets.CARD_HEIGHT + cardGap) - scrollOffset
        return x to y
    }

    /** Hit-test: index into [visibleEntries] of the card under (mx,my), clipped to the body. */
    private fun cardIndexAt(mx: Double, my: Double): Int? {
        if (my < bodyY || my >= bodyBottom) return null
        val list = visibleEntries
        for (i in list.indices) {
            val (cx, cy) = cardTopLeft(i)
            if (mx >= cx && mx < cx + cardW && my >= cy && my < cy + LumenWidgets.CARD_HEIGHT) {
                // also require the card to be within the visible viewport vertically
                if (cy + LumenWidgets.CARD_HEIGHT <= bodyY || cy >= bodyBottom) return null
                return i
            }
        }
        return null
    }

    private companion object {
        /** Faint void scrim over the whole screen behind the panel (token void at low alpha). */
        private val OVERLAY_SCRIM = (Tokens.Color.voidBase and 0x00FFFFFF) or (0xC0 shl 24)

        /** "Purse: 1,234,567" / "Coins: 1,234" — captures the numeric run (commas allowed). */
        private val PURSE_REGEX = Regex("""(?:Purse|Coins)\s*[:=]?\s*([\d,]+)""", RegexOption.IGNORE_CASE)
    }
}
