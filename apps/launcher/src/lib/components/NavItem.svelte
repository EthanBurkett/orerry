<svelte:options runes />
<script lang="ts">
  /**
   * Sidebar nav row: icon + label. Active = brass left-accent bar + brass
   * icon/label + a subtle surface fill (never a boxed outline).
   */
  interface Props {
    label: string;
    /** SVG path data, drawn as a stroked 24x24 icon. */
    icon: string;
    active?: boolean;
    onclick?: () => void;
  }
  let { label, icon, active = false, onclick }: Props = $props();
</script>

<button
  class="nav-item"
  class:is-active={active}
  aria-current={active ? 'page' : undefined}
  onclick={() => onclick?.()}
>
  <span class="bar" aria-hidden="true"></span>
  <svg class="icon" viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
    <path d={icon} />
  </svg>
  <span class="label">{label}</span>
</button>

<style>
  .nav-item {
    position: relative;
    display: flex;
    align-items: center;
    gap: 13px;
    width: 100%;
    height: 44px;
    padding: 0 16px;
    background: transparent;
    border: none;
    border-radius: var(--radius-md);
    cursor: pointer;
    color: var(--color-text-mid);
    text-align: left;
    transition:
      background var(--duration-fast) var(--ease-instrument),
      color var(--duration-fast) var(--ease-instrument);
  }
  .label {
    font-family: var(--font-body);
    font-size: var(--text-bodySm);
    font-weight: 500;
    letter-spacing: 0.01em;
    line-height: 1;
  }
  .icon {
    flex-shrink: 0;
  }
  .bar {
    position: absolute;
    left: -8px;
    top: 50%;
    transform: translateY(-50%) scaleY(0);
    width: 3px;
    height: 22px;
    border-radius: var(--radius-pill);
    background: var(--color-brass-base);
    transition: transform var(--duration-base) var(--ease-instrument);
  }

  .nav-item:hover {
    background: var(--color-void-s2);
    color: var(--color-text-hi);
  }
  .nav-item:focus-visible {
    outline: 2px solid var(--color-brass-base);
    outline-offset: -2px;
    color: var(--color-text-hi);
  }

  .nav-item.is-active {
    color: var(--color-brass-base);
    background: color-mix(in oklch, var(--color-brass-base) 9%, var(--color-void-s1));
  }
  .nav-item.is-active .bar {
    transform: translateY(-50%) scaleY(1);
  }
  .nav-item.is-active:hover {
    color: var(--color-brass-hi);
  }

  @media (prefers-reduced-motion: reduce) {
    .bar {
      transition: none;
    }
  }
</style>
