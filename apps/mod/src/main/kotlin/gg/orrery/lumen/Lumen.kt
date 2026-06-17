package gg.orrery.lumen

/**
 * Lumen — Orrery's custom UI toolkit (DESIGN_SPEC §6.4, §5).
 *
 * Lumen renders Orrery's custom menus and HUD entirely from the generated design tokens
 * ([gg.orrery.generated.Tokens]) — zero hardcoded colors, zero vanilla widget textures or
 * chrome. The custom typeface is the keystone (§5.1).
 *
 * ## Phase 1 surface (this slice)
 *
 * - [LumenDraw] — token-driven primitive draw helpers over `DrawContext` (rects, hairline
 *   borders, panels) plus a SINGLE text helper. Per ADR 0004 that text helper temporarily
 *   rasterises glyphs with the vanilla `TextRenderer` (marked `TODO(phase2-msdf)`); it is
 *   the only place the vanilla font touches Lumen output.
 * - [OrreryContainerScreen] — the first custom `Screen`. Extends `HandledScreen` so the
 *   open/close lifecycle + close packet stay vanilla-correct, reads slots from the live
 *   `ScreenHandler`, draws the menu in Orrery's own grid, and routes clicks through the
 *   Eclipse `Interaction.clickSlot` chokepoint (§11) — never vanilla `clickSlot`.
 *
 * ## Compliance
 *
 * Lumen has NO dependency on network/Relay. Action-producing widgets fire input callbacks
 * that ultimately reach [gg.orrery.eclipse.Interaction.clickSlot] (§11); they cannot bypass
 * it.
 *
 * ## Later (Phase 2+)
 *
 * Retained-mode component tree, flexbox-ish layout solver, MSDF custom-font atlas, animation,
 * and the custom render path for gradients/layering/transitions (§6.4).
 */
object Lumen
