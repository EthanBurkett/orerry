package gg.orrery.lumen

import gg.orrery.generated.Tokens
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Style
import net.minecraft.text.StyleSpriteSource
import net.minecraft.text.Text
import net.minecraft.util.Identifier

/**
 * LumenDraw — token-driven primitive draw helpers over [DrawContext] (DESIGN_SPEC §5, §6.4).
 *
 * Every color resolves to [Tokens.Color] (ARGB ints, 0xAARRGGBB). NOTHING here hardcodes a
 * color, uses a vanilla widget texture, or draws vanilla chrome. This is the entire pixel
 * vocabulary the first Orrery screen draws from.
 *
 * Geometry note: tokens express radius/scale, but `DrawContext.fill` only paints axis-aligned
 * rectangles — there is no rounded-rect primitive in the vanilla GUI pipeline at this layer.
 * Phase 1 therefore approximates panels as crisp hairline-bordered rectangles (the identity's
 * "hairline slots" read; §5.1). Rounded corners + gradients arrive with the Lumen render path
 * in Phase 2.
 *
 * --- Yarn 1.21.11 names used ---
 *   DrawContext.fill(int x1, int y1, int x2, int y2, int argb)        = method_25294
 *   DrawContext.drawText(TextRenderer, Text, int x, int y, int argb, boolean shadow)
 *                                                                     = method_51439
 *   TextRenderer (net.minecraft.client.font.TextRenderer)             = class_327
 *   Text.literal(String)                                              = method_43470
 *   Style.EMPTY.withFont(StyleSpriteSource.Font(Identifier))          = method_27704
 *   StyleSpriteSource.Font(Identifier)                                = class_11721 ctor
 */
object LumenDraw {

    /** A filled, axis-aligned rectangle in token color [argb]. (x,y) top-left; w,h size. */
    fun fillRect(ctx: DrawContext, x: Int, y: Int, w: Int, h: Int, argb: Int) {
        ctx.fill(x, y, x + w, y + h, argb)
    }

    /**
     * A 1px hairline border around the rectangle (x,y,w,h) in token color [argb].
     * Drawn as four thin filled edges — no texture, no bevel.
     */
    fun hairlineBorder(ctx: DrawContext, x: Int, y: Int, w: Int, h: Int, argb: Int) {
        ctx.fill(x, y, x + w, y + 1, argb)                 // top
        ctx.fill(x, y + h - 1, x + w, y + h, argb)         // bottom
        ctx.fill(x, y, x + 1, y + h, argb)                 // left
        ctx.fill(x + w - 1, y, x + w, y + h, argb)         // right
    }

    /**
     * A surface panel: a filled [fill] rectangle with a hairline border [border].
     * The fundamental Orrery container/cell shape (surface.N fill + hairline).
     */
    fun panel(ctx: DrawContext, x: Int, y: Int, w: Int, h: Int, fill: Int, border: Int) {
        fillRect(ctx, x, y, w, h, fill)
        hairlineBorder(ctx, x, y, w, h, border)
    }

    /**
     * The SINGLE Lumen text helper.
     *
     * Renders [str] through the vanilla [TextRenderer] using one of Orrery's custom typefaces
     * (ADR 0005 §3). The glyph is rasterised from the TTF font-provider registered under the
     * given [font] identifier — NOT Minecraft's pixel font. Supersedes the ADR 0004 vanilla-font
     * placeholder and the former `TODO(phase2-msdf)` note: Phase 2 uses the native MC TTF
     * provider path instead of an MSDF GL shader (see ADR 0005 §3 rationale).
     *
     * A styled [Text] is constructed from [str] with [Style.withFont] pointing at [font], then
     * drawn via [DrawContext.drawText]. Color still comes from the [argb] token (e.g.
     * [Tokens.Color.textHi]); only the glyph atlas is ours.
     *
     * In 1.21.11, [Style.withFont] takes a [StyleSpriteSource]; [StyleSpriteSource.Font] is the
     * subtype that wraps an [Identifier] referencing a font JSON under `assets/<ns>/font/<path>.json`.
     *
     * @param ctx          the current [DrawContext].
     * @param textRenderer the client [TextRenderer] (supplied by the calling [Screen]).
     * @param str          the string to draw.
     * @param x            left edge in screen pixels.
     * @param y            top edge in screen pixels.
     * @param argb         token text color (ARGB int, e.g. [Tokens.Color.textHi]).
     * @param shadow       whether to draw a drop shadow (default false — Orrery uses token layers).
     * @param font         the Orrery font identifier (default [LumenFonts.DISPLAY]).
     *
     * Yarn 1.21.11: [DrawContext.drawText] with [Text] = method_51439.
     */
    fun text(
        ctx: DrawContext,
        textRenderer: TextRenderer,
        str: String,
        x: Int,
        y: Int,
        argb: Int,
        shadow: Boolean = false,
        font: Identifier = LumenFonts.DISPLAY,
    ) {
        val styled: Text = Text.literal(str)
            .setStyle(Style.EMPTY.withFont(StyleSpriteSource.Font(font)))
        ctx.drawText(textRenderer, styled, x, y, argb, shadow)
    }
}
