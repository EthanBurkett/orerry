<svelte:options runes />
<script lang="ts">
  export type Rarity =
    | 'common'
    | 'uncommon'
    | 'rare'
    | 'epic'
    | 'legendary'
    | 'mythic'
    | 'divine'
    | 'special';

  interface Props {
    rarity: Rarity;
    label?: string;
  }

  let { rarity, label }: Props = $props();

  const displayLabel = $derived(label ?? rarity);

  /*
   * Rarity background colors come from --color-rarity-* CSS vars (emitted by
   * @orrery/design-tokens/css). No hex values here.
   *
   * Text contrast: rarity backgrounds are mid-saturation pastels on a dark
   * surface. All need very dark text. We derive per-rarity dark text from the
   * void palette rather than hardcoding arbitrary darks.
   *
   * The map drives a data-rarity attribute so CSS handles the actual color
   * selection — zero hardcoded hex in template or script.
   */
</script>

<span
  class="rarity-chip"
  data-rarity={rarity}
  aria-label="{displayLabel} rarity"
>
  {displayLabel}
</span>

<style>
  .rarity-chip {
    display: inline-flex;
    align-items: center;
    padding: 3px 10px;
    border-radius: var(--radius-md);
    font-family: var(--font-body);
    font-size: var(--text-small);
    font-weight: 500;
    letter-spacing: 0.01em;
    text-transform: capitalize;
    /* default dark text — all rarity backgrounds are light enough */
    color: oklch(15% 0.02 270);
    line-height: 1.4;
    user-select: none;
    white-space: nowrap;
  }

  .rarity-chip[data-rarity="common"]    { background: var(--color-rarity-common); }
  .rarity-chip[data-rarity="uncommon"]  { background: var(--color-rarity-uncommon); }
  .rarity-chip[data-rarity="rare"]      { background: var(--color-rarity-rare); }
  .rarity-chip[data-rarity="epic"]      { background: var(--color-rarity-epic); }
  .rarity-chip[data-rarity="legendary"] { background: var(--color-rarity-legendary); }
  .rarity-chip[data-rarity="mythic"]    { background: var(--color-rarity-mythic); }
  .rarity-chip[data-rarity="divine"]    { background: var(--color-rarity-divine); }
  .rarity-chip[data-rarity="special"]   { background: var(--color-rarity-special); }
</style>
