<svelte:options runes />
<script lang="ts">
  import type { Snippet } from 'svelte';
  import Spinner from './Spinner.svelte';

  /**
   * The one button vocabulary for the whole launcher.
   *   primary — brass fill, dark ink. The hero / commit action.
   *   ghost   — cyan outline, transparent. Secondary action.
   *   icon    — bare square, icon-only (aria-label required).
   * Every variant carries hover / focus-visible / active / disabled / loading.
   */
  interface Props {
    variant?: 'primary' | 'ghost' | 'icon';
    size?: 'sm' | 'md' | 'lg';
    type?: 'button' | 'submit';
    disabled?: boolean;
    loading?: boolean;
    /** Required for icon variant and any icon-only button. */
    ariaLabel?: string;
    title?: string;
    onclick?: (e: MouseEvent) => void;
    children?: Snippet;
  }
  let {
    variant = 'primary',
    size = 'md',
    type = 'button',
    disabled = false,
    loading = false,
    ariaLabel,
    title,
    onclick,
    children,
  }: Props = $props();
</script>

<button
  class="btn btn--{variant} btn--{size}"
  class:is-loading={loading}
  {type}
  {title}
  aria-label={ariaLabel}
  aria-busy={loading}
  disabled={disabled || loading}
  onclick={(e) => onclick?.(e)}
>
  {#if loading}
    <span class="btn-spinner"><Spinner size={size === 'lg' ? 20 : 16} /></span>
  {/if}
  <span class="btn-label" class:is-hidden={loading}>{@render children?.()}</span>
</button>

<style>
  .btn {
    --btn-radius: var(--radius-md);
    position: relative;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    font-family: var(--font-display);
    font-weight: 500;
    letter-spacing: 0.01em;
    border: 1px solid transparent;
    border-radius: var(--btn-radius);
    cursor: pointer;
    white-space: nowrap;
    transition:
      background var(--duration-fast) var(--ease-instrument),
      border-color var(--duration-fast) var(--ease-instrument),
      color var(--duration-fast) var(--ease-instrument),
      transform var(--duration-fast) var(--ease-instrument),
      box-shadow var(--duration-base) var(--ease-instrument);
  }
  .btn:active {
    transform: scale(0.98);
  }
  .btn:disabled {
    cursor: not-allowed;
    opacity: 0.45;
  }
  .btn:disabled:active {
    transform: none;
  }
  .btn:focus-visible {
    outline: 2px solid var(--color-brass-hi);
    outline-offset: 2px;
  }

  /* sizes */
  .btn--sm {
    height: 30px;
    padding: 0 14px;
    font-size: var(--text-small);
  }
  .btn--md {
    height: 38px;
    padding: 0 18px;
    font-size: var(--text-body);
  }
  .btn--lg {
    height: 52px;
    padding: 0 34px;
    font-size: var(--text-bodyLg);
    --btn-radius: var(--radius-lg);
  }

  /* primary — brass fill */
  .btn--primary {
    background: var(--color-brass-base);
    color: var(--color-ink-on-brass);
    box-shadow: 0 0 0 0 color-mix(in oklch, var(--color-brass-hi) 40%, transparent);
  }
  .btn--primary:hover:not(:disabled) {
    background: var(--color-brass-hi);
    box-shadow: 0 0 22px -4px color-mix(in oklch, var(--color-brass-hi) 55%, transparent);
  }
  .btn--primary:active:not(:disabled) {
    background: var(--color-brass-ember);
  }

  /* ghost — cyan outline */
  .btn--ghost {
    background: transparent;
    border-color: color-mix(in oklch, var(--color-cyan-base) 55%, transparent);
    color: var(--color-cyan-base);
  }
  .btn--ghost:hover:not(:disabled) {
    background: color-mix(in oklch, var(--color-cyan-base) 10%, transparent);
    border-color: var(--color-cyan-hi);
    color: var(--color-cyan-hi);
  }
  .btn--ghost:focus-visible {
    outline-color: var(--color-cyan-hi);
  }

  /* icon — bare square */
  .btn--icon {
    background: transparent;
    color: var(--color-text-mid);
    padding: 0;
    width: 34px;
    height: 34px;
    border-radius: var(--radius-md);
  }
  .btn--icon:hover:not(:disabled) {
    background: var(--color-void-s3);
    color: var(--color-text-hi);
  }

  .btn-spinner {
    position: absolute;
    inset: 0;
    display: grid;
    place-items: center;
  }
  .btn-label {
    display: inline-flex;
    align-items: center;
    gap: 8px;
  }
  .is-hidden {
    visibility: hidden;
  }
</style>
