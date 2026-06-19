package gg.orrery.lumen

import gg.orrery.atlas.MenuEntry
import gg.orrery.atlas.Rarity
import gg.orrery.generated.Tokens
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.world.item.ItemStack
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.FontDescription
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier

/**
 * LumenWidgets — reusable, token-driven semantic draw helpers for Orrery's custom menu views
 * (DESIGN_SPEC §5/§5.1/§5.2, §6.4; ADR 0005 §1/§2; DESIGN.md "Custom menu rendering").
 *
 * These compose [LumenDraw] primitives + [LumenFonts] into the building blocks of the semantic
 * SkyBlock-menu mockup: a panel header, a search field, labelled entry cards (icon + title +
 * subtitle + chevron), a chevron glyph, and a footer divider / progress bar.
 *
 * ## Identity discipline (§5 restraint, ADR 0005 §2)
 * - Every color resolves to [Tokens.Color] — backgrounds void/surface, borders hairline,
 *   text hi/mid/low, key numerals brass-hi, interactive accents cyan. No per-category rainbow.
 * - Entry icons are the REAL backing [ItemStack] drawn via [DrawContext.drawItem] — data, not
 *   invented art (ADR 0004 / ADR 0005 §2).
 * - A thin rarity tick may use a rarity color — game-data, allowed (ADR 0005 §2).
 * - Text is rendered ONLY through [LumenDraw.text] (vanilla TextRenderer + an Orrery TTF font
 *   identifier). No Minecraft pixel font is used directly.
 *
 * ## Compliance (§2/§11)
 * These are pure DRAW helpers. They never act on the menu, hold no handler, construct no
 * packets, and call no network path. Click routing lives entirely in the view
 * ([SkyBlockMenuView]) and goes through [gg.orrery.eclipse.Interaction.clickSlot].
 *
 * ## Yarn 1.21.11 names used
 *   DrawContext.fill(int,int,int,int,int argb)                         = method_25294
 *   DrawContext.drawItem(ItemStack, int x, int y)                      = method_51427 (16px)
 *   DrawContext.drawStackOverlay(TextRenderer, ItemStack, int, int)    = method_51432
 *   DrawContext.enableScissor/disableScissor(int...)                   = method_44379/method_44380
 *   DrawContext.drawText(TextRenderer, Text, int, int, int argb, bool) = method_51439
 *   TextRenderer.getWidth(StringVisitable) : int                       = method_27525
 *   TextRenderer.fontHeight : int
 *   Style.EMPTY.withFont(StyleSpriteSource.Font(Identifier))           = method_27704
 */
object LumenWidgets {

    // -- layout constants (px). Instrument-precise rhythm (DESIGN.md spacing: 4/8/12/16…). --
    // FLAG(screenshot): CARD_HEIGHT/ICON_TILE/SEARCH_HEIGHT/HEADER_HEIGHT are tuned against the
    // smaller font sizes (display ~9.5px / body ~8px); verify on a real screenshot pass and nudge
    // here if titles/subtitles crowd. All single-line edits.
    const val CARD_HEIGHT = 34
    const val CARD_GAP = 6
    const val ICON_TILE = 28          // surface.2 icon tile edge inside a card
    const val SEARCH_HEIGHT = 20
    const val HEADER_HEIGHT = 26

    // -- icon rendering (L2 #1) --
    /**
     * Scale-up factor for the native-16px item inside the [ICON_TILE]. ~1.4x makes a 16px item
     * read at ~22px so it fills the 28px tile without touching the hairline. FLAG(screenshot):
     * tune if icons look cramped or overflow the tile.
     */
    const val ICON_SCALE = 1.4f

    // -- rarity tint on the icon tile (L2 #4) --
    /**
     * Blend weight of the rarity color mixed into the icon-tile FILL (over surface.2). Kept low so
     * the tint reads as a faint wash, not a colored block (§5 restraint). FLAG(screenshot): tune.
     */
    const val RARITY_TILE_TINT = 0.12f

    /** Blend weight of the rarity color mixed into the icon-tile BORDER (over hairline). */
    const val RARITY_BORDER_TINT = 0.45f

    // -- card text metrics, proportional to TextRenderer.fontHeight (L2 #2) --
    // Titles/subtitles are positioned from the live font height so they always fit the card and
    // never clip when the font size is tuned in the *.json providers. These are ratios/offsets,
    // not hardcoded pixel rows.
    private const val CARD_TEXT_INSET = 6   // top/bottom inset for the two-line title+subtitle layout

    // -- pagination controls height (footer) --
    private const val PAGINATION_BTN_H = 14

    // ── text measurement with an Orrery font ─────────────────────────────────

    /** Builds a [Text] styled with the given Orrery [font] (so width + draw use the same glyphs). */
    private fun styled(str: String, font: Identifier): Component =
        Component.literal(str).setStyle(Style.EMPTY.withFont(FontDescription.Resource(font)))

    /** Measured pixel width of [str] when drawn in the Orrery [font]. */
    fun measure(tr: Font, str: String, font: Identifier): Int =
        tr.width(styled(str, font))

    /**
     * Draws [str] in [font], truncated with an ellipsis so it never exceeds [maxWidth] px.
     * Used for titles/subtitles that may overflow a card column.
     */
    fun textClipped(
        ctx: GuiGraphicsExtractor,
        tr: Font,
        str: String,
        x: Int,
        y: Int,
        maxWidth: Int,
        argb: Int,
        font: Identifier,
    ) {
        if (maxWidth <= 0) return
        if (measure(tr, str, font) <= maxWidth) {
            LumenDraw.text(ctx, tr, str, x, y, argb, font = font)
            return
        }
        val ellipsis = "…"
        val budget = maxWidth - measure(tr, ellipsis, font)
        if (budget <= 0) {
            LumenDraw.text(ctx, tr, ellipsis, x, y, argb, font = font)
            return
        }
        var end = str.length
        while (end > 0 && measure(tr, str.substring(0, end), font) > budget) end--
        LumenDraw.text(ctx, tr, str.substring(0, end) + ellipsis, x, y, argb, font = font)
    }

    // ── header ───────────────────────────────────────────────────────────────

    /**
     * Result bundle for [headerWithBack]: the close-glyph rect and the back-arrow rect.
     */
    data class HeaderResult(
        /** Screen-space rect [x,y,w,h] of the close × glyph (for hit-testing in the view). */
        val closeRect: IntArray,
        /** Screen-space rect [x,y,w,h] of the back ‹ glyph, or null when no BACK entry. */
        val backRect: IntArray?,
    )

    /**
     * Header row with optional back-arrow control:
     * - If [hasBack], draws a "‹" back-arrow glyph to the LEFT of the title (text-low, cyan on
     *   hover). The view hit-tests [HeaderResult.backRect] and routes the click through
     *   Interaction.clickSlot on the BACK entry's backing slot.
     * - Menu [title] in the DISPLAY font (text-hi), shifted right if the back arrow is shown.
     * - An optional [purse] value on the right (mono, brass-hi — a "key numeral").
     * - A close × (text-low, cyan on hover) on the far right.
     *
     * Returns [HeaderResult] so the view can hit-test both glyphs.
     */
    fun headerWithBack(
        ctx: GuiGraphicsExtractor,
        tr: Font,
        x: Int,
        y: Int,
        w: Int,
        title: String,
        purse: String?,
        hasBack: Boolean,
        mouseX: Int,
        mouseY: Int,
    ): HeaderResult {
        // close ×
        val closeGlyph = "×"
        val closeW = measure(tr, closeGlyph, LumenFonts.DISPLAY) + 6
        val closeH = HEADER_HEIGHT
        val closeX = x + w - closeW
        val closeY = y
        val closeHover = mouseX >= closeX && mouseX < closeX + closeW &&
            mouseY >= closeY && mouseY < closeY + closeH
        val closeColor = if (closeHover) Tokens.Color.cyanBase else Tokens.Color.textLow
        LumenDraw.text(
            ctx, tr, closeGlyph,
            closeX + 3, y + (HEADER_HEIGHT - tr.lineHeight) / 2,
            closeColor, font = LumenFonts.DISPLAY,
        )

        // back-arrow ‹ (left of title, only when BACK entry exists)
        var backResult: IntArray? = null
        var titleX = x
        if (hasBack) {
            val backGlyph = "‹"
            val backW = measure(tr, backGlyph, LumenFonts.DISPLAY) + 8
            val backH = HEADER_HEIGHT
            val backX = x
            val backY = y
            val backHover = mouseX >= backX && mouseX < backX + backW &&
                mouseY >= backY && mouseY < backY + backH
            val backColor = if (backHover) Tokens.Color.cyanBase else Tokens.Color.textLow
            LumenDraw.text(
                ctx, tr, backGlyph,
                backX + 2, y + (HEADER_HEIGHT - tr.lineHeight) / 2,
                backColor, font = LumenFonts.DISPLAY,
            )
            backResult = intArrayOf(backX, backY, backW, backH)
            titleX = backX + backW + 2
        }

        // purse value (mono, brass-hi) left of the close glyph, if derivable
        var titleMaxX = closeX - 8
        if (purse != null) {
            val pw = measure(tr, purse, LumenFonts.MONO)
            LumenDraw.text(
                ctx, tr, purse,
                closeX - 10 - pw, y + (HEADER_HEIGHT - tr.lineHeight) / 2,
                Tokens.Color.brassHi, font = LumenFonts.MONO,
            )
            titleMaxX = closeX - 10 - pw - 6
        }

        // title (DISPLAY, text-hi) — clipped to available space
        val titleMaxW = titleMaxX - titleX
        textClipped(
            ctx, tr, title, titleX, y + (HEADER_HEIGHT - tr.lineHeight) / 2,
            titleMaxW, Tokens.Color.textHi, LumenFonts.DISPLAY,
        )

        return HeaderResult(
            closeRect = intArrayOf(closeX, closeY, closeW, closeH),
            backRect = backResult,
        )
    }

    /**
     * Legacy [header] — kept for call-compatibility. Delegates to [headerWithBack] with no back
     * arrow. Returns only the close rect (as before).
     */
    fun header(
        ctx: GuiGraphicsExtractor,
        tr: Font,
        x: Int,
        y: Int,
        w: Int,
        title: String,
        purse: String?,
        mouseX: Int,
        mouseY: Int,
    ): IntArray {
        return headerWithBack(ctx, tr, x, y, w, title, purse, hasBack = false, mouseX, mouseY).closeRect
    }

    // ── search field ───────────────────────────────────────────────────────────

    /**
     * Search bar: a surface.2 inset with a hairline (hairline-bright when [focused]) and either
     * the typed [query] (text-hi) or the placeholder "Search the menu…" (text-low). A small drawn
     * magnifier (a hairline lens ring + handle — primitives, not a font glyph, since the text TTFs
     * have no magnifier emoji) sits at the left. Display-only filtering UI (§2 compliant).
     */
    fun searchField(
        ctx: GuiGraphicsExtractor,
        tr: Font,
        x: Int,
        y: Int,
        w: Int,
        query: String,
        focused: Boolean,
    ) {
        LumenDraw.panel(
            ctx, x, y, w, SEARCH_HEIGHT,
            fill = Tokens.Color.voidS2,
            border = if (focused) Tokens.Color.hairlineBright else Tokens.Color.hairlineBase,
        )
        // Magnifier drawn from primitives (the text fonts have no magnifier glyph): a 6x6 lens
        // ring (hairline-bright) + a short diagonal handle, vertically centered. text-low feel.
        val lensX = x + 6
        val lensY = y + (SEARCH_HEIGHT - 6) / 2
        val ring = Tokens.Color.textLow
        LumenDraw.fillRect(ctx, lensX + 1, lensY, 4, 1, ring)          // top
        LumenDraw.fillRect(ctx, lensX + 1, lensY + 5, 4, 1, ring)      // bottom
        LumenDraw.fillRect(ctx, lensX, lensY + 1, 1, 4, ring)          // left
        LumenDraw.fillRect(ctx, lensX + 5, lensY + 1, 1, 4, ring)      // right
        LumenDraw.fillRect(ctx, lensX + 6, lensY + 6, 2, 2, ring)      // handle
        val textY = y + (SEARCH_HEIGHT - tr.lineHeight) / 2
        val textX = x + 18
        if (query.isEmpty()) {
            LumenDraw.text(ctx, tr, "Search the menu…", textX, textY, Tokens.Color.textLow, font = LumenFonts.BODY)
        } else {
            textClipped(ctx, tr, query, textX, textY, w - 18 - 6, Tokens.Color.textHi, LumenFonts.BODY)
            // a blinking-free caret cue: a thin brass tick after the text when focused
            if (focused) {
                val caretX = (textX + measure(tr, query, LumenFonts.BODY)).coerceAtMost(x + w - 4)
                LumenDraw.fillRect(ctx, caretX + 1, y + 4, 1, SEARCH_HEIGHT - 8, Tokens.Color.brassBase)
            }
        }
    }

    // ── entry card ───────────────────────────────────────────────────────────

    /**
     * One labelled entry card: a surface tile (surface.3 + hairline-bright when [hovered] or
     * [selected], brass left-accent when [selected]) holding an icon tile rendering the real [icon]
     * stack, the entry [MenuEntry.title] (DISPLAY, text-hi), [MenuEntry.subtitle] (BODY, text-mid),
     * a chevron › (text-low) on the right, and a subtle rarity tick (rarity color) on the left edge.
     *
     * Keyboard-[selected] state renders with a STRONG highlight: brass left-accent + brighter surface
     * + hairline-bright border, distinct from mouse hover. Mouse [hovered] renders with a milder brass
     * accent (same size, different color: brassBase vs. brassHi). Both can coexist but selected wins
     * visually on the surface color.
     *
     * Pure draw; the view owns hit-testing + routing.
     */
    fun entryCard(
        ctx: GuiGraphicsExtractor,
        tr: Font,
        x: Int,
        y: Int,
        w: Int,
        entry: MenuEntry,
        icon: ItemStack?,
        hovered: Boolean,
        selected: Boolean = false,
    ) {
        // Card surface. Selected > hovered > base for the surface and border colors.
        val cardFill = when {
            selected -> Tokens.Color.voidS3
            hovered -> Tokens.Color.voidS3
            else -> Tokens.Color.voidS2
        }
        val cardBorder = when {
            selected -> Tokens.Color.hairlineBright
            hovered -> Tokens.Color.hairlineBright
            else -> Tokens.Color.hairlineBase
        }
        LumenDraw.panel(ctx, x, y, w, CARD_HEIGHT, fill = cardFill, border = cardBorder)

        val rarityColor = rarityColor(entry.rarity)

        // rarity tick — a thin vertical mark in the rarity color (game-data; allowed).
        if (rarityColor != null) {
            LumenDraw.fillRect(ctx, x + 1, y + 4, 2, CARD_HEIGHT - 8, rarityColor)
        }

        // Keyboard-selected accent: a STRONG brass-hi left bar (visually distinct from hover).
        // Mouse hover: a milder brassBase bar. Both are drawn just inside the rarity tick.
        when {
            selected -> LumenDraw.fillRect(ctx, x + 3, y + 4, 2, CARD_HEIGHT - 8, Tokens.Color.brassHi)
            hovered  -> LumenDraw.fillRect(ctx, x + 3, y + 4, 2, CARD_HEIGHT - 8, Tokens.Color.brassBase)
        }

        // ── icon tile ──────────────────────────────────────────────────────────────
        val tileX = x + 6
        val tileY = y + (CARD_HEIGHT - ICON_TILE) / 2

        // Rarity tint on the tile itself (L2 #4): faint rarity wash over surface.2 + a slightly
        // stronger rarity-mixed border. Subtle (§5 restraint). Falls back to plain tokens when the
        // rarity is UNKNOWN (no rarity color).
        val tileFill = if (rarityColor != null) {
            LumenDraw.mix(Tokens.Color.voidS2, rarityColor, RARITY_TILE_TINT)
        } else {
            Tokens.Color.voidS2
        }
        val tileBorder = if (rarityColor != null) {
            LumenDraw.mix(Tokens.Color.hairlineBase, rarityColor, RARITY_BORDER_TINT)
        } else {
            Tokens.Color.hairlineBase
        }
        LumenDraw.panel(ctx, tileX, tileY, ICON_TILE, ICON_TILE, tileFill, tileBorder)

        // The real item, scaled up so it fills the tile (L2 #1). Item coords are the *unscaled*
        // 16px box centered in the tile; LumenDraw.itemScaled scales about that box's center.
        if (icon != null && !icon.isEmpty) {
            val ix = tileX + (ICON_TILE - LumenDraw.ITEM_PX) / 2
            val iy = tileY + (ICON_TILE - LumenDraw.ITEM_PX) / 2
            LumenDraw.itemScaled(ctx, tr, icon, ix, iy, ICON_SCALE)
        } else {
            // Graceful fallback (L2 #1): a faint hairline placeholder glyph so nav entries without
            // a real item still look intentional (a small concentric "orbital" tick — §5 motif).
            iconPlaceholder(ctx, tileX, tileY)
        }

        // ── chevron on the right ─────────────────────────────────────────────────────
        val chevron = "›"
        val chevW = measure(tr, chevron, LumenFonts.DISPLAY)
        val chevX = x + w - chevW - 8
        LumenDraw.text(
            ctx, tr, chevron, chevX, y + (CARD_HEIGHT - tr.lineHeight) / 2,
            if (hovered || selected) Tokens.Color.textMid else Tokens.Color.textLow, font = LumenFonts.DISPLAY,
        )

        // ── text column between icon tile and chevron ────────────────────────────────
        // Positions derive from tr.fontHeight so they stay correct as the font size is tuned (L2 #2).
        val textX = tileX + ICON_TILE + 8
        val textMaxW = chevX - 4 - textX
        if (entry.subtitle.isNullOrBlank()) {
            // single line, vertically centered
            val ty = y + (CARD_HEIGHT - tr.lineHeight) / 2
            textClipped(ctx, tr, entry.title, textX, ty, textMaxW, Tokens.Color.textHi, LumenFonts.DISPLAY)
        } else {
            // Two lines: title near the top inset, subtitle near the bottom inset, both measured
            // from the live font height so neither clips against the card edges.
            val titleY = y + CARD_TEXT_INSET
            val subY = y + CARD_HEIGHT - tr.lineHeight - CARD_TEXT_INSET
            textClipped(ctx, tr, entry.title, textX, titleY, textMaxW, Tokens.Color.textHi, LumenFonts.DISPLAY)
            textClipped(ctx, tr, entry.subtitle, textX, subY, textMaxW, Tokens.Color.textMid, LumenFonts.BODY)
        }
    }

    /**
     * Faint placeholder for an empty/air icon tile (L2 #1): two concentric hairline ticks forming a
     * tiny orbital mark (the §5 motif) so nav entries without a backing item read as intentional
     * rather than broken. Token colors only.
     */
    private fun iconPlaceholder(ctx: GuiGraphicsExtractor, tileX: Int, tileY: Int) {
        val cx = tileX + ICON_TILE / 2
        val cy = tileY + ICON_TILE / 2
        val c = Tokens.Color.hairlineBright
        // outer ring (8x8 hairline square approximating a ring)
        LumenDraw.hairlineBorder(ctx, cx - 4, cy - 4, 8, 8, c)
        // inner pip
        LumenDraw.fillRect(ctx, cx - 1, cy - 1, 2, 2, c)
    }

    // ── footer ───────────────────────────────────────────────────────────────

    /**
     * Result bundle for [footerWithPagination]: the prev and next button rects (for hit-testing).
     */
    data class FooterResult(
        /** Screen-space rect [x,y,w,h] of the "‹ Prev" button, or null if no prev entry. */
        val prevRect: IntArray?,
        /** Screen-space rect [x,y,w,h] of the "Next ›" button, or null if no next entry. */
        val nextRect: IntArray?,
    )

    /**
     * Footer with optional pagination controls.
     *
     * Draws:
     * - Hairline divider
     * - [left] string (BODY, text-low) — e.g. entry count
     * - If pagination entries exist: "‹ Prev" and/or "Next ›" buttons (BODY, text-low / cyan on
     *   hover) centered in the footer. An optional page indicator (e.g. "Page 2/5") appears
     *   between the prev/next buttons when genuinely derivable.
     *
     * Returns [FooterResult] with the hit-test rects for the view's mouseClicked handler.
     * Degrades gracefully: if [prevEntry]/[nextEntry] are null, no controls are shown.
     */
    fun footerWithPagination(
        ctx: GuiGraphicsExtractor,
        tr: Font,
        x: Int,
        y: Int,
        w: Int,
        left: String,
        prevEntry: MenuEntry?,
        nextEntry: MenuEntry?,
        pageIndicator: String?,
        mouseX: Int,
        mouseY: Int,
    ): FooterResult {
        LumenDraw.fillRect(ctx, x, y, w, 1, Tokens.Color.hairlineBase)
        val ty = y + 6

        // left label (entry count etc.)
        LumenDraw.text(ctx, tr, left, x, ty, Tokens.Color.textLow, font = LumenFonts.BODY)

        var prevRect: IntArray? = null
        var nextRect: IntArray? = null

        val hasPrev = prevEntry != null
        val hasNext = nextEntry != null

        if (hasPrev || hasNext) {
            // Lay out pagination controls on the right side of the footer.
            // Order: [‹ Prev]  [Page X/Y]  [Next ›]
            val prevLabel = "‹ Prev"
            val nextLabel = "Next ›"

            var cx = x + w  // build from right to left

            // Next › button
            if (hasNext) {
                val nw = measure(tr, nextLabel, LumenFonts.BODY) + 8
                val nx = cx - nw
                val nh = PAGINATION_BTN_H
                val ny = ty - (PAGINATION_BTN_H - tr.lineHeight) / 2
                val nextHover = mouseX >= nx && mouseX < nx + nw &&
                    mouseY >= ny && mouseY < ny + nh
                LumenDraw.text(
                    ctx, tr, nextLabel, nx + 4, ty,
                    if (nextHover) Tokens.Color.cyanBase else Tokens.Color.textLow,
                    font = LumenFonts.BODY,
                )
                nextRect = intArrayOf(nx, ny, nw, nh)
                cx = nx - 4
            }

            // Page indicator (only if genuinely derivable; §2 — never fabricate)
            if (pageIndicator != null) {
                val pw = measure(tr, pageIndicator, LumenFonts.BODY)
                cx -= pw
                LumenDraw.text(ctx, tr, pageIndicator, cx, ty, Tokens.Color.textMid, font = LumenFonts.BODY)
                cx -= 4
            }

            // ‹ Prev button
            if (hasPrev) {
                val pw = measure(tr, prevLabel, LumenFonts.BODY) + 8
                val px = cx - pw
                val ph = PAGINATION_BTN_H
                val py = ty - (PAGINATION_BTN_H - tr.lineHeight) / 2
                val prevHover = mouseX >= px && mouseX < px + pw &&
                    mouseY >= py && mouseY < py + ph
                LumenDraw.text(
                    ctx, tr, prevLabel, px + 4, ty,
                    if (prevHover) Tokens.Color.cyanBase else Tokens.Color.textLow,
                    font = LumenFonts.BODY,
                )
                prevRect = intArrayOf(px, py, pw, ph)
            }
        }

        return FooterResult(prevRect = prevRect, nextRect = nextRect)
    }

    /**
     * Legacy [footer] — kept for call-compatibility. Delegates to a direct draw without
     * pagination controls, returning nothing.
     */
    fun footer(
        ctx: GuiGraphicsExtractor,
        tr: Font,
        x: Int,
        y: Int,
        w: Int,
        left: String,
        right: String?,
    ) {
        LumenDraw.fillRect(ctx, x, y, w, 1, Tokens.Color.hairlineBase)
        val ty = y + 6
        LumenDraw.text(ctx, tr, left, x, ty, Tokens.Color.textLow, font = LumenFonts.BODY)
        if (right != null) {
            val rw = measure(tr, right, LumenFonts.MONO)
            LumenDraw.text(ctx, tr, right, x + w - rw, ty, Tokens.Color.brassHi, font = LumenFonts.MONO)
        }
    }

    // ── loading state ─────────────────────────────────────────────────────────

    /**
     * Animated loading state for the body area. Shown while SkyBlock loading panes are still
     * resolving (within the grace window).
     *
     * Draws a small animated orbital indicator (rotating ring of ticks, primitives only — no
     * external assets) and "Loading…" in BODY font (text-low), centered in the body area.
     * The animation uses [elapsedMs] to drive the frame, never a timer/coroutine (§11 compliant).
     *
     * The orbital tick is a concentric arrangement of 8 small filled squares arranged in a ring,
     * with the current "lit" pip advancing at ~8 fps. Purely from DrawContext.fill primitives and
     * token colors — no textures, no raw hex.
     *
     * @param elapsedMs milliseconds since the screen opened (drives the animation frame).
     */
    fun loadingState(
        ctx: GuiGraphicsExtractor,
        tr: Font,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        elapsedMs: Float,
    ) {
        val cx = x + w / 2
        val cy = y + h / 2 - tr.lineHeight / 2 - 6  // slightly above center to leave room for text

        // Orbital ring: 8 pips arranged in a 12px radius circle.
        // Each pip is a 2x2 filled square. The "lit" pip cycles every 125ms (8 fps).
        val pipCount = 8
        val radius = 10
        val frame = ((elapsedMs / 125f) % pipCount).toInt()

        for (i in 0 until pipCount) {
            val angle = (i.toDouble() * 2.0 * Math.PI / pipCount) - Math.PI / 2.0  // start at top
            val px = (cx + radius * Math.cos(angle)).toInt() - 1
            val py = (cy + radius * Math.sin(angle)).toInt() - 1

            // Lit pip: brassHi; trailing pip: brassBase; rest: hairlineBright
            val pipColor = when {
                i == frame -> Tokens.Color.brassHi
                i == (frame + pipCount - 1) % pipCount -> Tokens.Color.brassBase
                else -> Tokens.Color.hairlineBright
            }
            LumenDraw.fillRect(ctx, px, py, 2, 2, pipColor)
        }

        // "Loading…" text below the orbital ring
        val label = "Loading…"
        val lw = measure(tr, label, LumenFonts.BODY)
        LumenDraw.text(
            ctx, tr, label,
            cx - lw / 2, cy + radius + 4,
            Tokens.Color.textLow, font = LumenFonts.BODY,
        )
    }

    /**
     * A thin brass progress bar: a surface.2 track with a brass fill at [fraction] (0..1).
     * Used by the footer when a level/progress value is genuinely derivable from the data.
     */
    fun progressBar(ctx: GuiGraphicsExtractor, x: Int, y: Int, w: Int, h: Int, fraction: Float) {
        val f = fraction.coerceIn(0f, 1f)
        LumenDraw.panel(ctx, x, y, w, h, Tokens.Color.voidS2, Tokens.Color.hairlineBase)
        val fillW = ((w - 2) * f).toInt()
        if (fillW > 0) LumenDraw.fillRect(ctx, x + 1, y + 1, fillW, h - 2, Tokens.Color.brassBase)
    }

    // ── rarity → token ─────────────────────────────────────────────────────────

    /** Maps a [Rarity] to its game-data color token, or null for UNKNOWN (no tick). */
    private fun rarityColor(rarity: Rarity): Int? = when (rarity) {
        Rarity.COMMON -> Tokens.Color.rarityCommon
        Rarity.UNCOMMON -> Tokens.Color.rarityUncommon
        Rarity.RARE -> Tokens.Color.rarityRare
        Rarity.EPIC -> Tokens.Color.rarityEpic
        Rarity.LEGENDARY -> Tokens.Color.rarityLegendary
        Rarity.MYTHIC -> Tokens.Color.rarityMythic
        Rarity.DIVINE -> Tokens.Color.rarityDivine
        Rarity.SPECIAL -> Tokens.Color.raritySpecial
        Rarity.UNKNOWN -> null
    }
}
