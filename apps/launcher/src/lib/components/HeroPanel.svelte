<svelte:options runes />
<script lang="ts">
  import type { Account, LaunchTarget } from '$lib/data/session.svelte';
  import Avatar from './Avatar.svelte';
  import Badge from './Badge.svelte';
  import Button from './Button.svelte';
  import OrbitalMotif from './OrbitalMotif.svelte';
  import EmptyState from './EmptyState.svelte';

  interface Props {
    account: Account | null;
    target: LaunchTarget;
    onLaunch?: () => void;
    onSettings?: () => void;
    onAddAccount?: () => void;
  }
  let { account, target, onLaunch, onSettings, onAddAccount }: Props = $props();

  const statusLabel: Record<string, string> = {
    ready: 'Ready to launch',
    'in-game': 'In game',
    offline: 'Offline',
  };
</script>

<section class="hero" aria-label="Launch">
  <!-- Ambient orbital art — large, faint, behind the content -->
  <div class="ambient" aria-hidden="true">
    <OrbitalMotif size={620} />
  </div>

  {#if account}
    <div class="stack">
      <div class="account">
        <Avatar size={60} username={account.username} />
        <div class="account-meta">
          <div class="name-row">
            <h1 class="username">{account.username}</h1>
            {#if account.rank}<Badge variant="brass" size="md">{account.rank}</Badge>{/if}
          </div>
          <span class="status status--{account.status}">{statusLabel[account.status]}</span>
        </div>
      </div>

      <p class="target">
        Minecraft <span class="num">{target.minecraft}</span>
        <span class="dot" aria-hidden="true">·</span>
        Fabric <span class="num">{target.fabric}</span>
      </p>

      <div class="actions">
        <Button variant="primary" size="lg" onclick={() => onLaunch?.()} ariaLabel="Launch Orrery">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor" aria-hidden="true">
            <path d="M8 5.14v14.72L19 12 8 5.14z" />
          </svg>
          Play
        </Button>
        <Button variant="ghost" size="lg" onclick={() => onSettings?.()}>Settings</Button>
      </div>

      <dl class="stats">
        <div class="stat">
          <dt>Last played</dt>
          <dd>{account.lastPlayed}</dd>
        </div>
        <div class="stat">
          <dt>Playtime</dt>
          <dd>{account.playtime}</dd>
        </div>
        <div class="stat">
          <dt>Profile</dt>
          <dd>{account.profile}</dd>
        </div>
      </dl>
    </div>
  {:else}
    <div class="stack stack--empty">
      <EmptyState title="No account selected" hint="Add a Microsoft account to launch the game and sync your config.">
        <Button variant="primary" size="md" onclick={() => onAddAccount?.()}>Add account</Button>
      </EmptyState>
    </div>
  {/if}
</section>

<style>
  .hero {
    position: relative;
    flex: 1;
    min-width: 0;
    display: flex;
    align-items: center;
    padding: 48px 56px;
    overflow: hidden;
  }
  .ambient {
    position: absolute;
    top: 50%;
    right: -120px;
    transform: translateY(-50%);
    opacity: 0.08;
    pointer-events: none;
  }
  .stack {
    position: relative;
    z-index: 1;
    display: flex;
    flex-direction: column;
    gap: 26px;
    max-width: 560px;
  }
  .stack--empty {
    width: 100%;
    align-items: flex-start;
  }

  .account {
    display: flex;
    align-items: center;
    gap: 16px;
  }
  .account-meta {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }
  .name-row {
    display: flex;
    align-items: center;
    gap: 10px;
  }
  .username {
    font-family: var(--font-display);
    font-size: var(--text-h1);
    font-weight: 700;
    color: var(--color-text-hi);
    line-height: 1;
    letter-spacing: 0.01em;
  }
  .status {
    font-family: var(--font-mono);
    font-size: var(--text-small);
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

  .target {
    font-family: var(--font-body);
    font-size: var(--text-body);
    color: var(--color-text-low);
  }
  .target .num {
    font-family: var(--font-mono);
    font-weight: 500;
    color: var(--color-text-mid);
    font-variant-numeric: tabular-nums;
  }
  .target .dot {
    color: var(--color-hairline-bright);
    margin: 0 4px;
  }

  .actions {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .stats {
    display: flex;
    gap: 40px;
    margin: 4px 0 0;
    padding-top: 22px;
    border-top: 1px solid var(--color-hairline-base);
  }
  .stat {
    display: flex;
    flex-direction: column;
    gap: 6px;
  }
  .stat dt {
    font-family: var(--font-body);
    font-size: var(--text-caption);
    letter-spacing: 0.06em;
    text-transform: uppercase;
    color: var(--color-text-low);
  }
  .stat dd {
    font-family: var(--font-mono);
    font-size: var(--text-bodyLg);
    font-weight: 500;
    color: var(--color-text-hi);
    font-variant-numeric: tabular-nums;
  }

  @media (max-width: 1040px) {
    .hero {
      padding: 40px;
    }
    .stats {
      gap: 28px;
    }
  }
</style>
