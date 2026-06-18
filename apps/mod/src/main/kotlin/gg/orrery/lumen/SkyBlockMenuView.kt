package gg.orrery.lumen

import gg.orrery.atlas.AtlasAdapter
import gg.orrery.atlas.EntryType
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
 * - Keyboard navigation (arrow keys + Enter/Space) only fires Interaction.clickSlot in direct
 *   response to a physical keypress — never from a timer or render loop (§11).
 *
 * ## Visual identity (§5 restraint, ADR 0005 §2)
 * Colors come ONLY from [Tokens.Color]; text ONLY through [LumenDraw]/[LumenWidgets] (Orrery TTF
 * fonts). Backgrounds void/surface, borders hairline(-bright), title text-hi, subtitle text-mid,
 * hints text-low, key numerals brass-hi, interactive accents cyan. Entry icons are the real items
 * (drawItem) — no invented per-category color; rarity tick uses game-data rarity tokens.
 *
 * ## Phase 2 additions
 * - Chrome entries (BACK/PAGINATION) separated from the card grid and rendered as dedicated
 *   header/footer controls. The card grid maps only non-chrome entries, so backingSlot resolution
 *   is always correct (each displayed card maps to its real MenuEntry.backingSlot).
 * - Loading state: a short grace window shows an animated orbital indicator while SkyBlock
 *   loading panes resolve into real items. After the grace window, an empty result shows
 *   "No entries in this menu." instead.
 * - Keyboard/controller navigation: arrow keys move a selected-card index across the 2-column
 *   grid; Enter or Space activates the selected card via Interaction.clickSlot (§2 compliant).
 * - Perf: purse, containerSlots, and filtered card list are derived ONCE per rebuild (not per
 *   frame). contentHeight and maxScroll derive lazily from the cached card list.
 * - Motion: a subtle open animation (alpha 0→1, scale 0.98→1.0) over ~180ms ease-out.
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
 *   DrawContext.getMatrices → org.joml.Matrix3x2fStack (pushMatrix/translate/scale/popMatrix)
 *   SlotActionType.PICKUP / QUICK_MOVE
 */
class SkyBlockMenuView(
    handler: GenericContainerScreenHandler,
    playerInventory: PlayerInventory,
    title: Text,
) : HandledScreen<GenericContainerScreenHandler>(handler, playerInventory, title) {

    // ── live entries (L2 #5) ────────────────────────────────────────────────────
    // SkyBlock mutates menu contents after open (loading panes → real items). We therefore
    // re-read entries from the LIVE handler rather than freezing them at construction. To avoid
    // re-parsing every frame we rebuild only when the container's slot contents actually change
    // (detected via a cheap fingerprint) or after a short fallback interval.
    private var entries: List<MenuEntry> = emptyList()
    private var lastSlotFingerprint: Int = Int.MIN_VALUE
    private var lastRebuildMs: Long = 0L

    // ── perf: cached derived state, recomputed only in rebuildEntriesIfStale ────
    /**
     * Cached list of container slots (never player inventory), updated on every rebuild.
     * Avoids calling handler.slots.filter on every frame.
     */
    private var cachedContainerSlots: List<net.minecraft.screen.slot.Slot> = emptyList()

    /**
     * Cached purse string (or null), updated on every rebuild (§4 perf fix).
     * Avoids re-running AtlasAdapter.parse every frame in derivePurse().
     */
    private var cachedPurse: String? = null

    /**
     * Cached chrome entries:
     * - [cachedBackEntry]: the BACK entry (there should be at most one), if present.
     * - [cachedPrevEntry]: the PAGINATION entry whose title implies "previous page".
     * - [cachedNextEntry]: the PAGINATION entry whose title implies "next page".
     */
    private var cachedBackEntry: MenuEntry? = null
    private var cachedPrevEntry: MenuEntry? = null
    private var cachedNextEntry: MenuEntry? = null

    /**
     * Cached page indicator string, e.g. "Page 2/5", or null if not derivable.
     * Derived from pagination entry titles during rebuild.
     */
    private var cachedPageIndicator: String? = null

    /**
     * Cached list of non-chrome entries (NAVIGATION, ITEM, INFO, TOGGLE, OTHER) — the entries
     * actually shown as cards. Updated on rebuild. Each entry's [MenuEntry.backingSlot] is the
     * authoritative slot index, preserving correct routing even after chrome filtering.
     */
    private var cachedCardEntries: List<MenuEntry> = emptyList()

    // ── open time for loading state + motion ─────────────────────────────────────
    /**
     * Wall-clock ms at which this screen was created. Used for:
     * - the loading-state grace window (real items usually arrive within 2–3 ticks ~100ms)
     * - the open animation (fade + scale over [OPEN_ANIM_MS])
     */
    private val openTimeMs: Long = System.currentTimeMillis()

    // ── keyboard navigation ───────────────────────────────────────────────────────
    /**
     * Index of the currently keyboard-selected card in [cachedCardEntries], or null when no
     * card is selected. The selected card receives a distinct STRONG highlight (brass accent +
     * brighter surface/border). Mouse hover is independent (see [cardIndexAt]).
     */
    private var selectedCardIndex: Int? = null

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

    // back-arrow control rect (in header) — set each render pass
    private var backRect: IntArray? = null
    // pagination control rects (in footer) — set each render pass
    private var prevRect: IntArray? = null
    private var nextRect: IntArray? = null

    // grid: 2 columns of cards inside the body viewport
    private val columns = 2
    private val cardGap = LumenWidgets.CARD_GAP

    // ── filtered view of card entries (case-insensitive substring on title) ──────
    // NOTE: we filter cachedCardEntries (non-chrome only). Each entry retains its original
    // backingSlot so the click routing remains correct.
    private val visibleCards: List<MenuEntry>
        get() = if (query.isBlank()) cachedCardEntries
        else cachedCardEntries.filter { it.title.contains(query.trim(), ignoreCase = true) }

    /**
     * The handler slots that back the menu (the container, never the player inventory) — in the
     * SAME order [AtlasAdapter.parse] enumerates them, so position i here == MenuEntry.backingSlot.
     * This is the single source of truth for resolving an entry's icon + click slot.
     *
     * PERF: direct callers in render now use [cachedContainerSlots] instead; this private helper
     * is only called from within [rebuildEntriesIfStale].
     */
    private fun computeContainerSlots() = handler.slots.filter { it.inventory !is PlayerInventory }

    /**
     * Cheap content fingerprint of the container slots: folds each stack's item + count so the view
     * rebuilds when loading panes resolve into real items (or counts change), but not every frame.
     */
    private fun slotFingerprint(): Int {
        var h = 1
        for (slot in cachedContainerSlots.ifEmpty { computeContainerSlots() }) {
            val s = slot.stack
            h = 31 * h + if (s.isEmpty) 0 else (System.identityHashCode(s.item) * 31 + s.count)
        }
        return h
    }

    /**
     * Rebuilds [entries] and ALL derived state from the live handler when slot contents changed
     * or the interval elapsed. Caches:
     * - [cachedContainerSlots]  — slot list (avoids per-frame filter)
     * - [cachedPurse]           — purse string (avoids per-frame AtlasAdapter.parse)
     * - [cachedBackEntry]       — BACK chrome entry
     * - [cachedPrevEntry]       — prev-page PAGINATION entry
     * - [cachedNextEntry]       — next-page PAGINATION entry
     * - [cachedPageIndicator]   — page indicator string ("Page X/Y"), if derivable
     * - [cachedCardEntries]     — non-chrome entries for the card grid
     */
    private fun rebuildEntriesIfStale() {
        val now = System.currentTimeMillis()
        val fp = slotFingerprint()
        if (fp == lastSlotFingerprint && now - lastRebuildMs < REBUILD_INTERVAL_MS) return

        // Re-parse from live handler
        cachedContainerSlots = computeContainerSlots()
        val parsed = AtlasAdapter.parse(title, handler)
        entries = parseEntries(parsed)

        lastSlotFingerprint = fp
        lastRebuildMs = now

        // ── cache purse (§4 perf: no longer called in derivePurse every frame) ──
        var purse: String? = null
        for (item in parsed.items) {
            if (item == null) continue
            for (line in item.lore + item.name) {
                val m = PURSE_REGEX.find(line) ?: continue
                purse = "⛁ " + m.groupValues[1]
                break
            }
            if (purse != null) break
        }
        cachedPurse = purse

        // ── separate chrome entries from card entries ──────────────────────────
        var backEntry: MenuEntry? = null
        var prevEntry: MenuEntry? = null
        var nextEntry: MenuEntry? = null
        val cardEntries = mutableListOf<MenuEntry>()

        for (entry in entries) {
            when (entry.type) {
                EntryType.BACK -> {
                    if (backEntry == null) backEntry = entry  // take the first BACK entry
                }
                EntryType.PAGINATION -> {
                    val t = entry.title
                    if (isPrevPage(t)) {
                        if (prevEntry == null) prevEntry = entry
                    } else {
                        if (nextEntry == null) nextEntry = entry
                    }
                }
                else -> cardEntries.add(entry)
            }
        }

        cachedBackEntry = backEntry
        cachedPrevEntry = prevEntry
        cachedNextEntry = nextEntry
        cachedCardEntries = cardEntries

        // ── derive page indicator ───────────────────────────────────────────────
        // Try to find "Page X/Y" or "Page X of Y" from any pagination entry title or subtitle
        var pageIndicator: String? = null
        val paginationEntries = entries.filter { it.type == EntryType.PAGINATION }
        for (pe in paginationEntries) {
            val m = PAGE_INDICATOR_REGEX.find(pe.title) ?: PAGE_INDICATOR_REGEX.find(pe.subtitle ?: "")
            if (m != null) {
                pageIndicator = m.value
                break
            }
        }
        cachedPageIndicator = pageIndicator

        // Clamp selected card index to valid range after rebuild
        selectedCardIndex = selectedCardIndex?.coerceIn(0, (cardEntries.size - 1).coerceAtLeast(0))
            ?.let { if (cardEntries.isEmpty()) null else it }
    }

    override fun init() {
        super.init()
        // GUI-scale aware: a FIXED design size in GUI-scaled units (like every vanilla screen),
        // centered, clamped to the screen on small windows.
        val margin = 16
        panelW = DESIGN_PANEL_W.coerceAtMost(width - margin * 2)
        panelH = DESIGN_PANEL_H.coerceAtMost(height - margin * 2)
        panelX = (width - panelW) / 2
        panelY = (height - panelH) / 2
        scrollOffset = 0
        // Force an immediate rebuild on re-init
        lastSlotFingerprint = Int.MIN_VALUE
        lastRebuildMs = 0L
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
            val n = visibleCards.size
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
        // L2 #5 (perf): refresh entries from the live handler (loading panes → real items),
        // rebuilding all derived cached state in one pass.
        rebuildEntriesIfStale()

        renderBackground(context, mouseX, mouseY, delta)

        // ── open animation (§5 motion) ────────────────────────────────────────────
        // Fade alpha 0→1 and scale 0.98→1.0 over OPEN_ANIM_MS with cubic-ease-out feel.
        val elapsed = (System.currentTimeMillis() - openTimeMs).toFloat()
        val animT = if (elapsed >= OPEN_ANIM_MS) 1f else easeOutCubic(elapsed / OPEN_ANIM_MS)
        val alpha = animT  // 0..1
        val panelScale = 0.98f + 0.02f * animT  // 0.98..1.0

        val matrices = context.matrices
        val panelCx = (panelX + panelW / 2).toFloat()
        val panelCy = (panelY + panelH / 2).toFloat()

        // Apply scale+alpha transform for the open animation
        matrices.pushMatrix()
        matrices.translate(panelCx, panelCy)
        matrices.scale(panelScale)
        matrices.translate(-panelCx, -panelCy)

        // scrim alpha also fades in
        val scrimAlpha = ((OVERLAY_SCRIM ushr 24) * alpha).toInt().coerceIn(0, 0xFF)
        val scrim = (OVERLAY_SCRIM and 0x00FFFFFF) or (scrimAlpha shl 24)
        LumenDraw.fillRect(context, 0, 0, width, height, scrim)
        LumenDraw.panel(context, panelX, panelY, panelW, panelH, Tokens.Color.voidS1, Tokens.Color.hairlineBase)

        // header (title + optional back-arrow + purse + close ×)
        val headerResult = LumenWidgets.headerWithBack(
            context, textRenderer, contentX, headerY, contentW,
            title.string, cachedPurse, cachedBackEntry != null, mouseX, mouseY,
        )
        closeRect = headerResult.closeRect
        backRect = if (cachedBackEntry != null) headerResult.backRect else null

        // search field
        LumenWidgets.searchField(context, textRenderer, contentX, searchY, contentW, query, searchFocused)

        // body: 2-column scrollable card list, clipped to the body viewport
        scrollOffset = scrollOffset.coerceIn(0, maxScroll)
        val list = visibleCards
        context.enableScissor(contentX, bodyY, contentX + contentW, bodyBottom)

        // Determine loading vs. empty vs. populated states
        val allEmpty = cachedContainerSlots.all { it.stack.isEmpty }
        val withinGrace = elapsed < LOADING_GRACE_MS
        val isLoading = (allEmpty || entries.isEmpty()) && withinGrace

        when {
            isLoading -> {
                // Loading state: animated orbital indicator + "Loading…"
                LumenWidgets.loadingState(context, textRenderer, contentX, bodyY, contentW, bodyH, elapsed)
            }
            list.isEmpty() -> {
                val msg = if (entries.isEmpty()) "No entries in this menu." else "No matches."
                LumenDraw.text(
                    context, textRenderer, msg,
                    contentX, bodyY + 6, Tokens.Color.textLow, font = LumenFonts.BODY,
                )
            }
            else -> {
                val hovered = cardIndexAt(mouseX.toDouble(), mouseY.toDouble())
                // auto-scroll selected card into view
                ensureSelectedVisible()
                list.forEachIndexed { i, entry ->
                    val (cx, cy) = cardTopLeft(i)
                    // skip cards fully outside the viewport (cheap cull)
                    if (cy + LumenWidgets.CARD_HEIGHT < bodyY || cy > bodyBottom) return@forEachIndexed
                    val isHovered = hovered == i
                    val isSelected = selectedCardIndex == i
                    LumenWidgets.entryCard(
                        context, textRenderer, cx, cy, cardW, entry,
                        iconFor(entry), hovered = isHovered, selected = isSelected,
                    )
                }
            }
        }
        context.disableScissor()

        // footer: divider + entry count + optional pagination controls
        val count = list.size
        val total = cachedCardEntries.size
        val leftLabel = if (query.isBlank()) "$total entries" else "$count of $total entries"
        val footerResult = LumenWidgets.footerWithPagination(
            context, textRenderer, contentX, bodyBottom + 4, contentW,
            leftLabel, cachedPrevEntry, cachedNextEntry, cachedPageIndicator, mouseX, mouseY,
        )
        prevRect = footerResult.prevRect
        nextRect = footerResult.nextRect

        matrices.popMatrix()

        // hovered card → vanilla item tooltip (data, not chrome) — drawn AFTER popMatrix so it
        // isn't affected by the open-animation scale transform
        val hovered = cardIndexAt(mouseX.toDouble(), mouseY.toDouble())
        if (hovered != null) {
            val entry = visibleCards.getOrNull(hovered)
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

        // back-arrow click
        backRect?.let { r ->
            if (mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3]) {
                val entry = cachedBackEntry
                if (entry != null) {
                    val slot = slotFor(entry)
                    if (slot != null) {
                        Interaction.clickSlot(handler.syncId, slot.index, 0, SlotActionType.PICKUP)
                    }
                }
                return true
            }
        }

        // pagination prev click
        prevRect?.let { r ->
            if (mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3]) {
                val entry = cachedPrevEntry
                if (entry != null) {
                    val slot = slotFor(entry)
                    if (slot != null) {
                        Interaction.clickSlot(handler.syncId, slot.index, 0, SlotActionType.PICKUP)
                    }
                }
                return true
            }
        }

        // pagination next click
        nextRect?.let { r ->
            if (mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3]) {
                val entry = cachedNextEntry
                if (entry != null) {
                    val slot = slotFor(entry)
                    if (slot != null) {
                        Interaction.clickSlot(handler.syncId, slot.index, 0, SlotActionType.PICKUP)
                    }
                }
                return true
            }
        }

        // focus / unfocus the search field
        searchFocused = mx >= contentX && mx < contentX + contentW &&
            my >= searchY && my < searchY + LumenWidgets.SEARCH_HEIGHT
        if (searchFocused) {
            selectedCardIndex = null  // clicking search clears keyboard selection
            return true
        }

        // card click → route to backing slot through the chokepoint
        val index = cardIndexAt(mx, my)
        if (index != null) {
            val entry = visibleCards.getOrNull(index)
            if (entry != null) {
                val slot = slotFor(entry)
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

    // ── input: keyboard ─────────────────────────────────────────────────────────

    override fun charTyped(input: CharInput): Boolean {
        if (searchFocused) {
            val cp = input.codepoint()
            if (cp >= 0x20 && cp != 0x7F) {
                query += cp.toChar()
                scrollOffset = 0
                selectedCardIndex = null
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
        } else {
            // ── keyboard/controller navigation (§3) ──────────────────────────────
            // Arrow keys navigate the 2-column grid; Enter/Space activate the selected card.
            // All Interaction.clickSlot calls here are in direct response to a physical keypress
            // (this is a keyPressed handler — §11 compliant).
            val list = visibleCards
            val n = list.size
            if (n > 0) {
                when (input.key()) {
                    GLFW.GLFW_KEY_RIGHT -> {
                        val cur = selectedCardIndex ?: -1
                        selectedCardIndex = (cur + 1).coerceAtMost(n - 1)
                        return true
                    }
                    GLFW.GLFW_KEY_LEFT -> {
                        val cur = selectedCardIndex ?: 0
                        selectedCardIndex = (cur - 1).coerceAtLeast(0)
                        return true
                    }
                    GLFW.GLFW_KEY_DOWN -> {
                        val cur = selectedCardIndex ?: -1
                        val next = cur + columns
                        selectedCardIndex = if (next < n) next else (n - 1).coerceAtLeast(0)
                        return true
                    }
                    GLFW.GLFW_KEY_UP -> {
                        val cur = selectedCardIndex ?: 0
                        val prev = cur - columns
                        selectedCardIndex = if (prev >= 0) prev else 0
                        return true
                    }
                    GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER, GLFW.GLFW_KEY_SPACE -> {
                        val sel = selectedCardIndex
                        if (sel != null && sel in list.indices) {
                            val entry = list[sel]
                            val slot = slotFor(entry)
                            if (slot != null) {
                                // Direct response to physical keypress — §11 compliant.
                                Interaction.clickSlot(handler.syncId, slot.index, 0, SlotActionType.PICKUP)
                            }
                        }
                        return true
                    }
                }
            }
        }
        // default handles Esc → close, etc.
        return super.keyPressed(input)
    }

    // ── data helpers ─────────────────────────────────────────────────────────────

    /**
     * The live backing [ItemStack] for an entry. [MenuEntry.backingSlot] is the entry's position in
     * the parse-order container-slot list (NOT a raw index into handler.slots), so we resolve it
     * against [cachedContainerSlots] — the same enumeration the parser used. Uses the cached slot
     * list (per-rebuild, not per-frame).
     */
    private fun iconFor(entry: MenuEntry): ItemStack? =
        cachedContainerSlots.getOrNull(entry.backingSlot)?.stack

    /** The live backing [net.minecraft.screen.slot.Slot] for an entry (for click routing). */
    private fun slotFor(entry: MenuEntry) =
        cachedContainerSlots.getOrNull(entry.backingSlot)

    // ── geometry ─────────────────────────────────────────────────────────────────

    /** Top-left (x,y) of card [i] in the 2-column grid, in screen space (scroll-adjusted). */
    private fun cardTopLeft(i: Int): Pair<Int, Int> {
        val col = i % columns
        val row = i / columns
        val x = contentX + col * (cardW + cardGap)
        val y = bodyY + row * (LumenWidgets.CARD_HEIGHT + cardGap) - scrollOffset
        return x to y
    }

    /** Hit-test: index into [visibleCards] of the card under (mx,my), clipped to the body. */
    private fun cardIndexAt(mx: Double, my: Double): Int? {
        if (my < bodyY || my >= bodyBottom) return null
        val list = visibleCards
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

    /**
     * Ensures the currently selected card is within the body viewport by adjusting [scrollOffset].
     * Called each render pass before drawing cards.
     */
    private fun ensureSelectedVisible() {
        val sel = selectedCardIndex ?: return
        val (_, cy) = cardTopLeft(sel)
        val cardH = LumenWidgets.CARD_HEIGHT
        // If card top is above the viewport, scroll up
        if (cy < bodyY) {
            scrollOffset = (scrollOffset - (bodyY - cy)).coerceAtLeast(0)
        }
        // If card bottom is below the viewport, scroll down
        if (cy + cardH > bodyBottom) {
            scrollOffset = (scrollOffset + (cy + cardH - bodyBottom)).coerceAtMost(maxScroll)
        }
    }

    // ── animation helpers ─────────────────────────────────────────────────────────

    /** Cubic ease-out: t goes 0→1, output eases from fast→slow. Feel: cubic-bezier(0.2,0.8,0.2,1). */
    private fun easeOutCubic(t: Float): Float {
        val s = 1f - t
        return 1f - s * s * s
    }

    private companion object {
        /**
         * Fixed design size of the menu panel, in GUI-scaled units. Kept constant (not a % of the
         * window) so the menu is GUI-scale aware: the GUI Scale option zooms it uniformly and the
         * proportions never drift with window size/resolution. Clamped to the screen on small
         * windows; the body scrolls past [DESIGN_PANEL_H]. FLAG(screenshot): tune to taste.
         */
        private const val DESIGN_PANEL_W = 480
        private const val DESIGN_PANEL_H = 340

        /**
         * Fallback rebuild cadence (ms) for live slot updates (L2 #5). Most rebuilds are triggered
         * by the slot-content fingerprint changing; this interval is a safety net for changes the
         * fingerprint can't see (e.g. same item identity, mutated NBT/lore). Cheap: a parse is
         * O(slots). FLAG: lower if live updates feel laggy, raise if profiling shows cost.
         */
        private const val REBUILD_INTERVAL_MS = 250L

        /**
         * Grace window (ms) during which an empty/all-filler result is shown as the loading state
         * rather than "No entries in this menu." SkyBlock menus typically populate within 2-3 ticks
         * (~100ms); 800ms gives generous headroom for slow connections. After this window, an empty
         * result is shown as the genuine "No entries" message.
         */
        private const val LOADING_GRACE_MS = 800f

        /**
         * Duration (ms) of the open animation (fade + scale). Matches Tokens.Motion.base (180ms).
         */
        private const val OPEN_ANIM_MS = 180f  // == Tokens.Motion.base

        /** Faint void scrim over the whole screen behind the panel (token void at low alpha). */
        private val OVERLAY_SCRIM = (Tokens.Color.voidBase and 0x00FFFFFF) or (0xC0 shl 24)

        /** "Purse: 1,234,567" / "Coins: 1,234" — captures the numeric run (commas allowed). */
        private val PURSE_REGEX = Regex("""(?:Purse|Coins)\s*[:=]?\s*([\d,]+)""", RegexOption.IGNORE_CASE)

        /**
         * Regex to find a page indicator in a pagination entry's title or subtitle.
         * Matches patterns like "Page 2/5", "Page 2 of 5", "2/5".
         */
        private val PAGE_INDICATOR_REGEX = Regex(
            """Page\s+(\d+)\s*(?:/|of)\s*(\d+)|\b(\d+)\s*/\s*(\d+)\b""",
            RegexOption.IGNORE_CASE,
        )

        /**
         * Classifies a pagination entry title as "previous page."
         * Prev if title contains: "Previous", "Back", "‹", or "←".
         */
        fun isPrevPage(title: String): Boolean =
            title.contains("previous", ignoreCase = true) ||
            title.contains("back", ignoreCase = true) ||
            title.contains("‹") ||
            title.contains("←") ||
            title.contains("◄")
    }
}
