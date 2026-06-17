<svelte:options runes />
<script lang="ts">
  import type { Account } from '$lib/data/session.svelte';
  import Avatar from './Avatar.svelte';
  import Badge from './Badge.svelte';

  /** Compact persistent identity for the sidebar foot. */
  interface Props {
    account: Account | null;
    version: string;
  }
  let { account, version }: Props = $props();

  const statusLabel: Record<string, string> = {
    ready: 'Ready',
    'in-game': 'In game',
    offline: 'Offline',
  };
</script>

<div class="chip">
  {#if account}
    <Avatar size={34} username={account.username} />
    <div class="meta">
      <div class="line">
        <span class="name">{account.username}</span>
        {#if account.rank}<Badge variant="brass">{account.rank}</Badge>{/if}
      </div>
      <span class="status status--{account.status}">{statusLabel[account.status]}</span>
    </div>
  {:else}
    <Avatar size={34} />
    <div class="meta">
      <span class="name name--muted">No account</span>
      <span class="status status--offline">Sign in to play</span>
    </div>
  {/if}
</div>
<span class="version">Observatory v{version}</span>

<style>
  .chip {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 8px;
    background: var(--color-void-s2);
    border: 1px solid var(--color-hairline-base);
    border-radius: var(--radius-lg);
  }
  .meta {
    display: flex;
    flex-direction: column;
    gap: 3px;
    min-width: 0;
  }
  .line {
    display: flex;
    align-items: center;
    gap: 6px;
    min-width: 0;
  }
  .name {
    font-family: var(--font-body);
    font-size: var(--text-bodySm);
    font-weight: 500;
    color: var(--color-text-hi);
    line-height: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .name--muted {
    color: var(--color-text-mid);
  }
  .status {
    font-family: var(--font-mono);
    font-size: var(--text-caption);
    line-height: 1;
  }
  .status--ready {
    color: var(--color-semantic-success);
  }
  .status--in-game {
    color: var(--color-cyan-base);
  }
  .status--offline {
    color: var(--color-text-low);
  }
  .version {
    font-family: var(--font-mono);
    font-size: var(--text-caption);
    color: var(--color-text-low);
    padding-left: 4px;
  }
</style>
