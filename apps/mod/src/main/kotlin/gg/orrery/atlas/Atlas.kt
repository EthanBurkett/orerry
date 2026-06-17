package gg.orrery.atlas

/**
 * Atlas — SkyBlock data model and parsers (stub, Phase 0).
 *
 * Phase 1/2 will implement: typed parsers for SkyBlock item model (rarity, item id,
 * ExtraAttributes, enchants, reforges, lore → structured fields), and menu fingerprinting
 * (stable recognizers: title pattern + key slot items) so Eclipse knows what menu
 * the server just opened (DESIGN_SPEC §6.5).
 *
 * Atlas only exposes data parsed from received containers or fetched via Sextant —
 * no fabricating hidden information (§2.4, §6.5).
 */
object Atlas
