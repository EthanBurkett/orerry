<svelte:options runes />
<script lang="ts">
  import { createQuery } from '@tanstack/svelte-query';
  import { getAppVersion } from '$lib/tauri';
  import WindowChrome from '$lib/components/WindowChrome.svelte';
  import Sidebar from '$lib/components/Sidebar.svelte';
  import NewsRail from '$lib/components/NewsRail.svelte';
  import StatusBar from '$lib/components/StatusBar.svelte';
  import EmptyState from '$lib/components/EmptyState.svelte';
  import Home from '../routes/Home.svelte';

  // Rendered inside <QueryClientProvider> (App.svelte) — context is available here.
  const versionQuery = createQuery({
    queryKey: ['appVersion'],
    queryFn: () => getAppVersion().catch(() => 'dev'),
    staleTime: Infinity,
  });
  const appVersion = $derived($versionQuery.data ?? '0.1.0');

  let activePage = $state('home');
  function navigate(page: string) {
    activePage = page;
  }
  const routeTitle = $derived(
    activePage.charAt(0).toUpperCase() + activePage.slice(1),
  );
</script>

<div class="app">
  <WindowChrome />

  <div class="body">
    <Sidebar {activePage} {appVersion} onNavigate={navigate} />

    <main class="main">
      {#if activePage === 'home'}
        <Home onNavigate={navigate} />
      {:else}
        <div class="route-empty">
          <EmptyState
            title={routeTitle}
            hint="This section arrives in a later phase."
          />
        </div>
      {/if}
    </main>

    <NewsRail />
  </div>

  <StatusBar />
</div>

<style>
  .app {
    display: flex;
    flex-direction: column;
    width: 100%;
    height: 100%;
    background: var(--color-void-base);
    overflow: hidden;
  }
  .body {
    flex: 1;
    display: flex;
    min-height: 0;
  }
  .main {
    flex: 1;
    min-width: 0;
    display: flex;
    overflow: hidden;
  }
  .route-empty {
    flex: 1;
    display: grid;
    place-items: center;
  }

  /* Right rail collapses on narrow windows. */
  @media (max-width: 880px) {
    .body :global(aside[aria-label='Observatory news']) {
      display: none;
    }
  }
</style>
