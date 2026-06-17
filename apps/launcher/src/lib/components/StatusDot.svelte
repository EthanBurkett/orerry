<svelte:options runes />
<script lang="ts">
  import type { ServiceState } from '$lib/data/status.svelte';

  /** A status indicator dot with a soft concentric glow, colored by state. */
  interface Props {
    state?: ServiceState;
    size?: number;
  }
  let { state = 'unknown', size = 7 }: Props = $props();

  const colorFor: Record<ServiceState, string> = {
    ok: 'var(--color-semantic-success)',
    warn: 'var(--color-semantic-warning)',
    down: 'var(--color-semantic-danger)',
    unknown: 'var(--color-text-low)',
  };
  const dot = $derived(colorFor[state]);
</script>

<span
  class="dot"
  style="--dot:{dot}; --size:{size}px"
  role="img"
  aria-label="status: {state}"
></span>

<style>
  .dot {
    display: inline-block;
    width: var(--size);
    height: var(--size);
    border-radius: 50%;
    background: var(--dot);
    box-shadow: 0 0 0 3px color-mix(in oklch, var(--dot) 18%, transparent);
    flex-shrink: 0;
  }
</style>
