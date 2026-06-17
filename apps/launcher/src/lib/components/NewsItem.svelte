<svelte:options runes />
<script lang="ts">
  import type { NewsEntry } from '$lib/data/news';
  import Badge from './Badge.svelte';

  interface Props {
    entry: NewsEntry;
  }
  let { entry }: Props = $props();
</script>

<article class="news-item">
  <div class="top">
    <Badge variant={entry.type === 'update' ? 'brass' : 'cyan'}>
      {entry.type === 'update' ? 'Update' : 'Notice'}
    </Badge>
    <span class="tag">{entry.tag}</span>
    {#if entry.isNew}<span class="new-dot" title="New" aria-label="New"></span>{/if}
  </div>
  <h3 class="title">{entry.title}</h3>
  <p class="body">{entry.body}</p>
</article>

<style>
  .news-item {
    display: flex;
    flex-direction: column;
    gap: 8px;
    padding: 16px 0;
    border-bottom: 1px solid var(--color-hairline-base);
  }
  .news-item:last-child {
    border-bottom: none;
  }
  .top {
    display: flex;
    align-items: center;
    gap: 8px;
  }
  .tag {
    font-family: var(--font-mono);
    font-size: var(--text-caption);
    color: var(--color-text-low);
  }
  .new-dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: var(--color-brass-hi);
    margin-left: auto;
    box-shadow: 0 0 8px 0 color-mix(in oklch, var(--color-brass-hi) 60%, transparent);
  }
  .title {
    font-family: var(--font-display);
    font-size: var(--text-bodySm);
    font-weight: 500;
    color: var(--color-text-hi);
    line-height: 1.35;
  }
  .body {
    font-family: var(--font-body);
    font-size: var(--text-small);
    color: var(--color-text-mid);
    line-height: 1.55;
  }
</style>
