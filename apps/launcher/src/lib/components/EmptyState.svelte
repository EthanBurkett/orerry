<svelte:options runes />
<script lang="ts">
  import type { Snippet } from 'svelte';
  import OrbitalMotif from './OrbitalMotif.svelte';

  /** Teaching empty state: faint motif + a title and a hint, optional action. */
  interface Props {
    title: string;
    hint?: string;
    children?: Snippet;
  }
  let { title, hint, children }: Props = $props();
</script>

<div class="empty">
  <div class="empty-motif" aria-hidden="true">
    <OrbitalMotif size={88} />
  </div>
  <p class="empty-title">{title}</p>
  {#if hint}<p class="empty-hint">{hint}</p>{/if}
  {#if children}<div class="empty-action">{@render children()}</div>{/if}
</div>

<style>
  .empty {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    text-align: center;
    gap: 4px;
    padding: 32px 20px;
  }
  .empty-motif {
    opacity: 0.22;
    margin-bottom: 8px;
  }
  .empty-title {
    font-family: var(--font-display);
    font-size: var(--text-body);
    font-weight: 500;
    color: var(--color-text-mid);
  }
  .empty-hint {
    font-family: var(--font-body);
    font-size: var(--text-small);
    color: var(--color-text-low);
    max-width: 32ch;
    line-height: 1.5;
  }
  .empty-action {
    margin-top: 12px;
  }
</style>
