package gg.orrery.lumen

import gg.orrery.generated.Tokens
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.world.item.ItemStack
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.FontDescription
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier

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
    fun fillRect(ctx: GuiGraphicsExtractor, x: Int, y: Int, w: Int, h: Int, argb: Int) {
        ctx.fill(x, y, x + w, y + h, argb)
    }

    /**
     * A 1px hairline border around the rectangle (x,y,w,h) in token color [argb].
     * Drawn as four thin filled edges — no texture, no bevel.
     */
    fun hairlineBorder(ctx: GuiGraphicsExtractor, x: Int, y: Int, w: Int, h: Int, argb: Int) {
        ctx.fill(x, y, x + w, y + 1, argb)                 // top
        ctx.fill(x, y + h - 1, x + w, y + h, argb)         // bottom
        ctx.fill(x, y, x + 1, y + h, argb)                 // left
        ctx.fill(x + w - 1, y, x + w, y + h, argb)         // right
    }

    /**
     * A surface panel: a filled [fill] rectangle with a hairline border [border].
     * The fundamental Orrery container/cell shape (surface.N fill + hairline).
     */
    fun panel(ctx: GuiGraphicsExtractor, x: Int, y: Int, w: Int, h: Int, fill: Int, border: Int) {
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
        ctx: GuiGraphicsExtractor,
        textRenderer: Font,
        str: String,
        x: Int,
        y: Int,
        argb: Int,
        shadow: Boolean = false,
        font: Identifier = LumenFonts.DISPLAY,
    ) {
        val styled: Component = Component.literal(str)
            .setStyle(Style.EMPTY.withFont(FontDescription.Resource(font)))
        ctx.text(textRenderer, styled, x, y, argb, shadow)
    }

    /**
     * Draws an item icon scaled up about its center so a native-16px item fills a larger tile and
     * reads clearly (L2 #1). Items render through the GUI render-state batch, which captures the
     * current 2D matrix transform at submit time, so we push a scale-about-center transform, submit
     * the item + its count/durability overlay, then pop.
     *
     * @param ctx    current [DrawContext].
     * @param tr     [TextRenderer] for the stack overlay (count / durability).
     * @param stack  the item to draw (no-op when empty).
     * @param x      left edge of the *unscaled* 16px item box, in screen px.
     * @param y      top edge of the *unscaled* 16px item box, in screen px.
     * @param scale  uniform scale factor about the item's center (e.g. 1.4).
     *
     * Yarn 1.21.11: [DrawContext.getMatrices] = method_51448 → [org.joml.Matrix3x2fStack]
     * (pushMatrix / translate / scale / popMatrix); [DrawContext.drawItem] = method_51427.
     */
    fun itemScaled(
        ctx: GuiGraphicsExtractor,
        tr: Font,
        stack: ItemStack,
        x: Int,
        y: Int,
        scale: Float,
    ) {
        if (stack.isEmpty) return
        val cx = x + ITEM_PX / 2f
        val cy = y + ITEM_PX / 2f
        val matrices = ctx.pose()
        matrices.pushMatrix()
        // scale about the item's center so it grows in place rather than off toward the origin
        matrices.translate(cx, cy)
        matrices.scale(scale)
        matrices.translate(-cx, -cy)
        ctx.item(stack, x, y)
        ctx.itemDecorations(tr, stack, x, y)
        matrices.popMatrix()
    }

    /**
     * Linear blend of two ARGB colors by [t] in 0..1 (0 → [a], 1 → [b]), per-channel including
     * alpha. Used to derive subtle tinted surfaces/borders from a base token + a rarity token
     * (a "color-mix" over Tokens — the result is still a function of tokens only, no new brand
     * color is introduced).
     */
    fun mix(a: Int, b: Int, t: Float): Int {
        val f = t.coerceIn(0f, 1f)
        val inv = 1f - f
        val aa = (a ushr 24) and 0xFF
        val ar = (a ushr 16) and 0xFF
        val ag = (a ushr 8) and 0xFF
        val ab = a and 0xFF
        val ba = (b ushr 24) and 0xFF
        val br = (b ushr 16) and 0xFF
        val bg = (b ushr 8) and 0xFF
        val bb = b and 0xFF
        val ra = (aa * inv + ba * f).toInt() and 0xFF
        val rr = (ar * inv + br * f).toInt() and 0xFF
        val rg = (ag * inv + bg * f).toInt() and 0xFF
        val rb = (ab * inv + bb * f).toInt() and 0xFF
        return (ra shl 24) or (rr shl 16) or (rg shl 8) or rb
    }

    /** Native item icon edge length in GUI px (vanilla draws items at 16px). */
    const val ITEM_PX = 16
}
