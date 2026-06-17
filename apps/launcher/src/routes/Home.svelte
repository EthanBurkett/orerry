<svelte:options runes />
<script lang="ts">
  import OrreryRings from '$lib/components/OrreryRings.svelte';
  import RarityChip from '$lib/components/RarityChip.svelte';
  import type { Rarity } from '$lib/components/RarityChip.svelte';

  interface Props {
    onLaunch?: () => void;
    onOpenSettings?: () => void;
    onNavigate?: (page: string) => void;
  }

  let { onLaunch, onOpenSettings, onNavigate }: Props = $props();

  // Stubbed account state — replaced with real data via svelte-query in a later task
  const account = $state({
    username: 'Wanderer',
    status: 'ready' as 'ready' | 'offline' | 'in-game',
  });

  const statusLabel = $derived(
    account.status === 'ready' ? 'Ready' :
    account.status === 'in-game' ? 'In Game' : 'Offline'
  );

  const rarities: Rarity[] = [
    'common', 'uncommon', 'rare', 'epic',
    'legendary', 'mythic', 'divine', 'special',
  ];

  // Pinned announcement stub
  const announcement = $state({
    badge: 'UPDATE',
    title: 'Orrery v0.1 — Observatory scaffold',
    body: 'Launcher skeleton complete. Token pipeline verified end-to-end.',
    timestamp: 'Phase 0',
  });
</script>

<div class="home">
  <!-- Background orbital rings — ambient, very large, behind everything -->
  <div class="bg-rings" aria-hidden="true">
    <OrreryRings size={480} class="bg-rings-svg" />
  </div>

  <!-- Header bar -->
  <header class="home-header">
    <!-- Wordmark -->
    <div class="wordmark" role="img" aria-label="Orrery">
      <OrreryRings size={36} />
      <span class="wordmark-text" aria-hidden="true">
        <span class="wordmark-o">O</span><span class="wordmark-rest">rrery</span>
      </span>
    </div>

    <!-- Account chip -->
    <div class="account-chip" role="status" aria-label="Logged in as {account.username}">
      <div class="account-avatar" aria-hidden="true">
        <!-- Placeholder avatar: orbital mark -->
        <svg width="28" height="28" viewBox="0 0 28 28" fill="none" xmlns="http://www.w3.org/2000/svg">
          <circle cx="14" cy="14" r="13" stroke="var(--color-brass-base)" stroke-width="1" opacity="0.6" />
          <circle cx="14" cy="14" r="8" stroke="var(--color-brass-base)" stroke-width="0.75" opacity="0.4" />
          <circle cx="14" cy="14" r="3.5" fill="var(--color-brass-hi)" opacity="0.9" />
        </svg>
      </div>
      <div class="account-info">
        <span class="account-name">{account.username}</span>
        <span
          class="account-status"
          class:status--ready={account.status === 'ready'}
          class:status--in-game={account.status === 'in-game'}
          class:status--offline={account.status === 'offline'}
        >
          {statusLabel}
        </span>
      </div>
    </div>
  </header>

  <!-- Main content area -->
  <main class="home-main">
    <!-- Left: play zone -->
    <section class="play-zone" aria-label="Launch">
      <div class="version-info">
        <span class="version-label">Minecraft</span>
        <span class="version-number">1.21.11</span>
        <span class="version-sep" aria-hidden="true">·</span>
        <span class="version-label">Fabric</span>
        <span class="version-number">0.16.x</span>
      </div>

      <div class="play-actions">
        <button
          class="btn-play"
          onclick={() => onLaunch?.()}
          aria-label="Launch Orrery"
        >
          <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
            <path d="M8 5.14v14.72L19 12 8 5.14z" />
          </svg>
          Play
        </button>
        <button
          class="btn-settings"
          onclick={() => onOpenSettings?.()}
          aria-label="Open settings"
        >
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <circle cx="12" cy="12" r="3" />
            <path d="M12 2v3M12 19v3M4.22 4.22l2.12 2.12M17.66 17.66l2.12 2.12M2 12h3M19 12h3M4.22 19.78l2.12-2.12M17.66 6.34l2.12-2.12" />
          </svg>
          Settings
        </button>
      </div>

      <!-- Rarity row — decorative brand moment -->
      <div class="rarity-row" aria-label="SkyBlock item rarities">
        {#each rarities as rarity (rarity)}
          <RarityChip {rarity} />
        {/each}
      </div>
    </section>

    <!-- Right: news / status panel -->
    <aside class="news-panel" aria-label="News and updates">
      <div class="panel-header">
        <span class="panel-title">Observatory</span>
        <span class="panel-badge">1 new</span>
      </div>

      <div class="panel-divider" aria-hidden="true"></div>

      <article class="announcement" aria-label="Pinned announcement: {announcement.title}">
        <div class="announcement-top">
          <span class="announcement-badge">{announcement.badge}</span>
          <span class="announcement-time">{announcement.timestamp}</span>
        </div>
        <h2 class="announcement-title">{announcement.title}</h2>
        <p class="announcement-body">{announcement.body}</p>
      </article>

      <div class="panel-divider" aria-hidden="true"></div>

      <!-- System status row -->
      <div class="status-row">
        <div class="status-item">
          <span class="status-dot status-dot--ok" aria-hidden="true"></span>
          <span class="status-item-label">Backend</span>
        </div>
        <div class="status-item">
          <span class="status-dot status-dot--ok" aria-hidden="true"></span>
          <span class="status-item-label">Auth</span>
        </div>
        <div class="status-item">
          <span class="status-dot status-dot--ok" aria-hidden="true"></span>
          <span class="status-item-label">Sextant</span>
        </div>
      </div>
    </aside>
  </main>
</div>

<style>
  .home {
    position: relative;
    display: flex;
    flex-direction: column;
    width: 100%;
    height: 100%;
    overflow: hidden;
    background: var(--color-void-base);
  }

  /* ------------------------------------------------------------------ */
  /* Background rings — ambient, bottom-right corner, very subtle        */
  /* ------------------------------------------------------------------ */
  .bg-rings {
    position: absolute;
    bottom: -160px;
    right: -160px;
    pointer-events: none;
    opacity: 0.07;
    z-index: 0;
  }

  :global(.bg-rings-svg) {
    display: block;
  }

  /* ------------------------------------------------------------------ */
  /* Header                                                              */
  /* ------------------------------------------------------------------ */
  .home-header {
    position: relative;
    z-index: 1;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 14px 20px 12px;
    border-bottom: 1px solid var(--color-hairline-base);
    flex-shrink: 0;
  }

  .wordmark {
    display: flex;
    align-items: center;
    gap: 10px;
    user-select: none;
  }

  .wordmark-text {
    font-family: var(--font-display);
    font-size: var(--text-h3);
    font-weight: 700;
    letter-spacing: 0.5px;
    line-height: 1;
  }

  .wordmark-o {
    color: var(--color-brass-hi);
  }

  .wordmark-rest {
    color: var(--color-text-hi);
  }

  .account-chip {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 6px 10px 6px 6px;
    background: var(--color-void-s2);
    border: 1px solid var(--color-hairline-base);
    border-radius: var(--radius-lg);
    cursor: default;
  }

  .account-avatar {
    width: 28px;
    height: 28px;
    flex-shrink: 0;
  }

  .account-info {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  .account-name {
    font-family: var(--font-body);
    font-size: var(--text-small);
    font-weight: 500;
    color: var(--color-text-hi);
    line-height: 1;
  }

  .account-status {
    font-family: var(--font-mono);
    font-size: var(--text-caption);
    font-weight: 400;
    line-height: 1;
  }

  .status--ready   { color: var(--color-semantic-success); }
  .status--in-game { color: var(--color-cyan-base); }
  .status--offline { color: var(--color-text-low); }

  /* ------------------------------------------------------------------ */
  /* Main                                                                */
  /* ------------------------------------------------------------------ */
  .home-main {
    position: relative;
    z-index: 1;
    display: flex;
    flex: 1;
    min-height: 0;
    gap: 0;
  }

  /* ------------------------------------------------------------------ */
  /* Play zone                                                           */
  /* ------------------------------------------------------------------ */
  .play-zone {
    flex: 1;
    display: flex;
    flex-direction: column;
    justify-content: center;
    padding: 32px 32px 28px;
    gap: 20px;
  }

  .version-info {
    display: flex;
    align-items: center;
    gap: 6px;
  }

  .version-label {
    font-family: var(--font-body);
    font-size: var(--text-small);
    color: var(--color-text-low);
    font-weight: 400;
  }

  .version-number {
    font-family: var(--font-mono);
    font-size: var(--text-small);
    color: var(--color-text-mid);
    font-weight: 500;
  }

  .version-sep {
    color: var(--color-hairline-bright);
    margin: 0 2px;
  }

  .play-actions {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  .btn-play {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    padding: 0 28px;
    height: 44px;
    background: var(--color-brass-base);
    border: none;
    border-radius: var(--radius-md);
    font-family: var(--font-display);
    font-size: var(--text-bodyLg);
    font-weight: 500;
    /* Dark text on brass — uses a token-tinted dark, not a raw hex */
    color: oklch(15% 0.06 60);
    cursor: pointer;
    transition:
      background var(--duration-fast) var(--ease-instrument),
      transform var(--duration-fast) var(--ease-instrument);
    letter-spacing: 0.01em;
  }

  .btn-play:hover {
    background: var(--color-brass-hi);
  }

  .btn-play:active {
    transform: scale(0.98);
  }

  .btn-play:focus-visible {
    outline: 2px solid var(--color-brass-hi);
    outline-offset: 2px;
  }

  .btn-settings {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 0 18px;
    height: 44px;
    background: transparent;
    border: 1px solid var(--color-cyan-base);
    border-radius: var(--radius-md);
    font-family: var(--font-display);
    font-size: var(--text-body);
    font-weight: 500;
    color: var(--color-cyan-base);
    cursor: pointer;
    transition:
      background var(--duration-fast) var(--ease-instrument),
      color var(--duration-fast) var(--ease-instrument),
      border-color var(--duration-fast) var(--ease-instrument);
    letter-spacing: 0.01em;
  }

  .btn-settings:hover {
    background: color-mix(in oklch, var(--color-cyan-base) 10%, transparent);
    color: var(--color-cyan-hi);
    border-color: var(--color-cyan-hi);
  }

  .btn-settings:focus-visible {
    outline: 2px solid var(--color-cyan-base);
    outline-offset: 2px;
  }

  .rarity-row {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    align-items: center;
    padding-top: 4px;
  }

  /* ------------------------------------------------------------------ */
  /* News panel                                                          */
  /* ------------------------------------------------------------------ */
  .news-panel {
    width: 260px;
    flex-shrink: 0;
    display: flex;
    flex-direction: column;
    background: var(--color-void-s1);
    border-left: 1px solid var(--color-hairline-base);
    padding: 20px 18px;
    gap: 14px;
    overflow-y: auto;
  }

  .panel-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-shrink: 0;
  }

  .panel-title {
    font-family: var(--font-display);
    font-size: var(--text-body);
    font-weight: 500;
    color: var(--color-text-hi);
    letter-spacing: 0.01em;
  }

  .panel-badge {
    font-family: var(--font-mono);
    font-size: var(--text-caption);
    font-weight: 500;
    color: var(--color-brass-hi);
    background: color-mix(in oklch, var(--color-brass-base) 14%, transparent);
    border: 1px solid color-mix(in oklch, var(--color-brass-base) 30%, transparent);
    border-radius: var(--radius-pill);
    padding: 2px 7px;
  }

  .panel-divider {
    height: 1px;
    background: var(--color-hairline-base);
    flex-shrink: 0;
  }

  .announcement {
    display: flex;
    flex-direction: column;
    gap: 6px;
  }

  .announcement-top {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  .announcement-badge {
    font-family: var(--font-mono);
    font-size: var(--text-caption);
    font-weight: 500;
    color: var(--color-brass-base);
    letter-spacing: 0.08em;
  }

  .announcement-time {
    font-family: var(--font-mono);
    font-size: var(--text-caption);
    color: var(--color-text-low);
  }

  .announcement-title {
    font-family: var(--font-display);
    font-size: var(--text-bodySm);
    font-weight: 500;
    color: var(--color-text-hi);
    line-height: 1.35;
    margin: 0;
  }

  .announcement-body {
    font-family: var(--font-body);
    font-size: var(--text-small);
    color: var(--color-text-mid);
    line-height: 1.55;
    margin: 0;
  }

  /* ------------------------------------------------------------------ */
  /* System status row                                                   */
  /* ------------------------------------------------------------------ */
  .status-row {
    display: flex;
    align-items: center;
    gap: 14px;
    margin-top: auto;
    padding-top: 4px;
  }

  .status-item {
    display: flex;
    align-items: center;
    gap: 5px;
  }

  .status-dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    flex-shrink: 0;
  }

  .status-dot--ok { background: var(--color-semantic-success); }
  /* Variant classes applied dynamically by parent — declare as :global to avoid
     Svelte's unused-selector error on classes not present in the static template. */
  :global(.status-dot--warn)    { background: var(--color-semantic-warning); }
  :global(.status-dot--error)   { background: var(--color-semantic-danger); }
  :global(.status-dot--unknown) { background: var(--color-text-low); }

  .status-item-label {
    font-family: var(--font-mono);
    font-size: var(--text-caption);
    color: var(--color-text-low);
  }
</style>
