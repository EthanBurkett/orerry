<svelte:options runes />
<script lang="ts">
  import type { Snippet } from 'svelte';

  /**
   * Small pill label: counts ("2 new"), ranks ("MVP+"), news types. Tint and
   * border derive from one accent token via color-mix; foreground/base pair is
   * injected as CSS vars so there's one rule, no per-variant selector pruning.
   */
  type Variant = 'brass' | 'cyan' | 'neutral' | 'success' | 'warning' | 'danger';
  interface Props {
    variant?: Variant;
    size?: 'sm' | 'md';
    children?: Snippet;
  }
  let { variant = 'neutral', size = 'sm', children }: Props = $props();

  const map: Record<Variant, { fg: string; base: string }> = {
    brass: { fg: 'var(--color-brass-hi)', base: 'var(--color-brass-base)' },
    cyan: { fg: 'var(--color-cyan-hi)', base: 'var(--color-cyan-base)' },
    neutral: { fg: 'var(--color-text-mid)', base: 'var(--color-hairline-bright)' },
    success: { fg: 'var(--color-semantic-success)', base: 'var(--color-semantic-success)' },
    warning: { fg: 'var(--color-semantic-warning)', base: 'var(--color-semantic-warning)' },
    danger: { fg: 'var(--color-semantic-danger)', base: 'var(--color-semantic-danger)' },
  };
  const c = $derived(map[variant]);
</script>

<span
  class="badge badge--{size}"
  style="--badge-fg:{c.fg}; --badge-base:{c.base}"
>{@render children?.()}</span>

<style>
  .badge {
    display: inline-flex;
    align-items: center;
    font-family: var(--font-mono);
    font-weight: 500;
    line-height: 1;
    white-space: nowrap;
    color: var(--badge-fg);
    background: color-mix(in oklch, var(--badge-base) 13%, transparent);
    border: 1px solid color-mix(in oklch, var(--badge-base) 30%, transparent);
    border-radius: var(--radius-pill);
  }
  .badge--sm {
    font-size: var(--text-caption);
    padding: 2px 7px;
    letter-spacing: 0.02em;
  }
  .badge--md {
    font-size: var(--text-small);
    padding: 3px 10px;
  }
</style>
