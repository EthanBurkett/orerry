<svelte:options runes />
<script lang="ts">
  import { createQuery } from '@tanstack/svelte-query';
  import { getAppVersion } from '$lib/tauri';
  import Sidebar from '$lib/components/Sidebar.svelte';
  import Home from '../routes/Home.svelte';

  // Active page state — drives sidebar active indicator and route rendering.
  let activePage = $state('home');

  function navigate(page: string) {
    activePage = page;
  }

  /*
   * Version query: proves the svelte-query ↔ Tauri command pipeline.
   * This component renders INSIDE <QueryClientProvider> (see App.svelte), so
   * the QueryClient is available via context — createQuery must never be called
   * in the same component that renders the provider, or context is missing and
   * mount throws. The queryFn catch lets plain Vite dev (no Tauri) degrade to
   * 'dev' instead of erroring.
   */
  const versionQuery = createQuery({
    queryKey: ['appVersion'],
    queryFn: () => getAppVersion().catch(() => 'dev'),
    staleTime: Infinity,
  });
</script>

<div class="shell">
  <Sidebar {activePage} onNavigate={navigate} />

  <div class="content">
    {#if activePage === 'home'}
      <Home
        onLaunch={() => console.log('launch')}
        onOpenSettings={() => navigate('settings')}
        onNavigate={navigate}
      />
    {:else}
      <!-- Placeholder for other routes — scaffolded in later tasks -->
      <div class="placeholder">
        <span class="placeholder-label">{activePage}</span>
      </div>
    {/if}
  </div>

  <!-- Version watermark — bottom corner, very subtle -->
  <div class="version-watermark" aria-hidden="true">
    {#if $versionQuery.data}
      v{$versionQuery.data}
    {/if}
  </div>
</div>

<style>
  .shell {
    display: flex;
    width: 100%;
    height: 100%;
    background: var(--color-void-base);
    overflow: hidden;
    position: relative;
  }

  .content {
    flex: 1;
    display: flex;
    flex-direction: column;
    min-width: 0;
    overflow: hidden;
  }

  .placeholder {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .placeholder-label {
    font-family: var(--font-mono);
    font-size: var(--text-small);
    color: var(--color-text-low);
    letter-spacing: 0.06em;
    text-transform: uppercase;
  }

  .version-watermark {
    position: fixed;
    bottom: 8px;
    right: 10px;
    font-family: var(--font-mono);
    font-size: var(--text-caption);
    color: var(--color-text-low);
    opacity: 0.5;
    pointer-events: none;
    z-index: 100;
  }
</style>
