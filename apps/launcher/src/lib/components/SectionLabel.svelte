<svelte:options runes />
<script lang="ts">
  import type { Snippet } from 'svelte';
  /** Uppercase kicker label for section headers (the §5.6 tile "kick" style). */
  interface Props {
    children?: Snippet;
  }
  let { children }: Props = $props();
</script>

<span class="kicker">{@render children?.()}</span>

<style>
  .kicker {
    font-family: var(--font-body);
    font-size: var(--text-caption);
    font-weight: 500;
    letter-spacing: 0.14em;
    text-transform: uppercase;
    color: var(--color-text-low);
  }
</style>
