<svelte:options runes />
<script lang="ts">
  import { onMount } from 'svelte';
  import OrbitalMotif from './OrbitalMotif.svelte';
  import {
    minimizeWindow,
    toggleMaximizeWindow,
    closeWindow,
    isWindowMaximized,
    onWindowResized,
  } from '$lib/window';

  /**
   * Custom frameless titlebar. Left = orbital mark + wordmark; the brand and the
   * flexible center are drag handles (data-tauri-drag-region); right = themed
   * window controls. The window runs with decorations:false.
   */
  let maximized = $state(false);

  onMount(() => {
    let unsub: () => void = () => {};
    isWindowMaximized().then((m) => (maximized = m));
    onWindowResized((m) => (maximized = m)).then((fn) => (unsub = fn));
    return () => unsub();
  });

  async function onToggleMax() {
    await toggleMaximizeWindow();
    maximized = await isWindowMaximized();
  }
</script>

<header class="chrome">
  <div class="brand" data-tauri-drag-region>
    <OrbitalMotif size={20} />
    <span class="wordmark" aria-label="Orrery">
      <span class="wordmark-o">O</span>rrery
    </span>
  </div>

  <div class="drag" data-tauri-drag-region></div>

  <div class="controls">
    <button class="ctrl" aria-label="Minimize" title="Minimize" onclick={minimizeWindow}>
      <svg viewBox="0 0 12 12" width="12" height="12" aria-hidden="true">
        <line x1="2.5" y1="6" x2="9.5" y2="6" stroke="currentColor" stroke-width="1" stroke-linecap="round" />
      </svg>
    </button>
    <button
      class="ctrl"
      aria-label={maximized ? 'Restore' : 'Maximize'}
      title={maximized ? 'Restore' : 'Maximize'}
      onclick={onToggleMax}
    >
      {#if maximized}
        <svg viewBox="0 0 12 12" width="12" height="12" aria-hidden="true" fill="none" stroke="currentColor" stroke-width="1">
          <rect x="2.5" y="3.5" width="6" height="6" rx="1" />
          <path d="M4.5 3.5V2.5h6v6h-1" />
        </svg>
      {:else}
        <svg viewBox="0 0 12 12" width="12" height="12" aria-hidden="true" fill="none" stroke="currentColor" stroke-width="1">
          <rect x="2.5" y="2.5" width="7" height="7" rx="1" />
        </svg>
      {/if}
    </button>
    <button class="ctrl ctrl--close" aria-label="Close" title="Close" onclick={closeWindow}>
      <svg viewBox="0 0 12 12" width="12" height="12" aria-hidden="true">
        <path d="M3 3l6 6M9 3l-6 6" stroke="currentColor" stroke-width="1" stroke-linecap="round" />
      </svg>
    </button>
  </div>
</header>

<style>
  .chrome {
    display: flex;
    align-items: center;
    height: 40px;
    background: var(--color-void-s1);
    border-bottom: 1px solid var(--color-hairline-base);
    flex-shrink: 0;
    user-select: none;
  }
  .brand {
    display: flex;
    align-items: center;
    gap: 9px;
    padding: 0 14px;
    height: 100%;
  }
  .wordmark {
    font-family: var(--font-display);
    font-size: var(--text-bodySm);
    font-weight: 700;
    letter-spacing: 0.04em;
    color: var(--color-text-hi);
    line-height: 1;
  }
  .wordmark-o {
    color: var(--color-brass-hi);
  }
  .drag {
    flex: 1;
    height: 100%;
  }
  .controls {
    display: flex;
    height: 100%;
  }
  .ctrl {
    width: 46px;
    height: 100%;
    display: grid;
    place-items: center;
    background: transparent;
    border: none;
    color: var(--color-text-mid);
    cursor: pointer;
    transition:
      background var(--duration-fast) var(--ease-instrument),
      color var(--duration-fast) var(--ease-instrument);
  }
  .ctrl:hover {
    background: var(--color-void-s3);
    color: var(--color-text-hi);
  }
  .ctrl:focus-visible {
    outline: 2px solid var(--color-brass-base);
    outline-offset: -3px;
  }
  .ctrl--close:hover {
    background: color-mix(in oklch, var(--color-semantic-danger) 90%, transparent);
    color: var(--color-text-hi);
  }
</style>
