package gg.orrery.lumen

import gg.orrery.generated.Tokens
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext

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
 *   DrawContext.drawText(TextRenderer, Text/String, int x, int y, int argb, boolean shadow)
 *                                                                     = method_51439 / method_51433
 *   TextRenderer (net.minecraft.client.font.TextRenderer)             = class_327
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
     * TODO(phase2-msdf): per ADR 0004, Phase 1 glyphs are rasterised with Minecraft's vanilla
     * [TextRenderer]. This is the ONLY place the vanilla font touches Lumen output; everything
     * else (color, layout, chrome) is 100% token-driven. Phase 2 swaps this body for the MSDF
     * custom-typeface atlas in one place. Color still comes from [Tokens.Color]; only the glyph
     * raster is temporary.
     *
     * @param argb token text color (e.g. [Tokens.Color.textHi]).
     */
    fun text(ctx: DrawContext, textRenderer: TextRenderer, str: String, x: Int, y: Int, argb: Int, shadow: Boolean = false) {
        ctx.drawText(textRenderer, str, x, y, argb, shadow)
    }
}
