<svelte:options runes />
<script lang="ts">
  import { news, newCount } from '$lib/data/news';
  import SectionLabel from './SectionLabel.svelte';
  import Badge from './Badge.svelte';
  import NewsItem from './NewsItem.svelte';
  import EmptyState from './EmptyState.svelte';
</script>

<aside class="rail" aria-label="Observatory news">
  <header class="head">
    <SectionLabel>Observatory</SectionLabel>
    {#if newCount > 0}<Badge variant="brass">{newCount} new</Badge>{/if}
  </header>

  <div class="list">
    {#if news.length > 0}
      {#each news as entry (entry.id)}
        <NewsItem {entry} />
      {/each}
    {:else}
      <EmptyState title="All quiet" hint="Release notes and notices will appear here." />
    {/if}
  </div>
</aside>

<style>
  .rail {
    width: 300px;
    flex-shrink: 0;
    display: flex;
    flex-direction: column;
    background: var(--color-void-s1);
    border-left: 1px solid var(--color-hairline-base);
  }
  .head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 20px 20px 12px;
    border-bottom: 1px solid var(--color-hairline-base);
    flex-shrink: 0;
  }
  .list {
    flex: 1;
    overflow-y: auto;
    padding: 4px 20px 16px;
  }
</style>
