<svelte:options runes />
<script lang="ts">
  import { createQuery } from '@tanstack/svelte-query';
  import { getAppVersion } from '$lib/tauri';
  import { services } from '$lib/data/status.svelte';
  import StatusDot from './StatusDot.svelte';

  // Rendered inside <QueryClientProvider> (Shell), so context is available.
  const versionQuery = createQuery({
    queryKey: ['appVersion'],
    queryFn: () => getAppVersion().catch(() => 'dev'),
    staleTime: Infinity,
  });
</script>

<footer class="status-bar">
  <div class="services">
    {#each services as svc (svc.id)}
      <div class="svc" title="{svc.label}: {svc.state}">
        <StatusDot state={svc.state} />
        <span class="svc-label">{svc.label}</span>
      </div>
    {/each}
  </div>

  <span class="version">v{$versionQuery.data ?? '…'}</span>
</footer>

<style>
  .status-bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    height: 28px;
    padding: 0 16px;
    background: var(--color-void-s1);
    border-top: 1px solid var(--color-hairline-base);
    flex-shrink: 0;
  }
  .services {
    display: flex;
    align-items: center;
    gap: 18px;
  }
  .svc {
    display: flex;
    align-items: center;
    gap: 7px;
  }
  .svc-label {
    font-family: var(--font-mono);
    font-size: var(--text-caption);
    color: var(--color-text-low);
  }
  .version {
    font-family: var(--font-mono);
    font-size: var(--text-caption);
    color: var(--color-text-low);
    font-variant-numeric: tabular-nums;
  }
</style>
