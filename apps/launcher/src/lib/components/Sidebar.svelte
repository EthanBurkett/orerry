<svelte:options runes />
<script lang="ts">
  import OrbitalMotif from './OrbitalMotif.svelte';
  import NavItem from './NavItem.svelte';
  import ProfileChip from './ProfileChip.svelte';
  import { session } from '$lib/data/session.svelte';

  interface Props {
    activePage?: string;
    appVersion?: string;
    onNavigate?: (page: string) => void;
  }
  let { activePage = 'home', appVersion = '0.1.0', onNavigate }: Props = $props();

  const items = [
    { id: 'home', label: 'Home', icon: 'M3 9.5L12 3l9 6.5V20a1 1 0 01-1 1H4a1 1 0 01-1-1V9.5zM9 21V12h6v9' },
    { id: 'accounts', label: 'Accounts', icon: 'M12 12a4.5 4.5 0 100-9 4.5 4.5 0 000 9zm0 2.4c-3.6 0-8 1.8-8 5.1V21h16v-1.5c0-3.3-4.4-5.1-8-5.1z' },
    { id: 'mods', label: 'Mods', icon: 'M11 4a1 1 0 10-2 0v1H6a2 2 0 00-2 2v2.5H2.5a1.5 1.5 0 000 3H4V15a2 2 0 002 2h1v1a1 1 0 102 0v-1h2v1a1 1 0 102 0v-1h1a2 2 0 002-2v-2.5h1.5a1.5 1.5 0 000-3H18V7a2 2 0 00-2-2h-2.5V4a1 1 0 10-2 0v1H11V4z' },
    { id: 'settings', label: 'Settings', icon: 'M12 15a3 3 0 100-6 3 3 0 000 6zm7.4-3a7.4 7.4 0 00-.1-1.2l2-1.6-2-3.4-2.4 1a7 7 0 00-2-1.2L14.4 2H9.6l-.5 2.4a7 7 0 00-2 1.2l-2.4-1-2 3.4 2 1.6a7.4 7.4 0 000 2.4l-2 1.6 2 3.4 2.4-1a7 7 0 002 1.2l.5 2.4h4.8l.5-2.4a7 7 0 002-1.2l2.4 1 2-3.4-2-1.6c.06-.4.1-.8.1-1.2z' },
    { id: 'cosmetics', label: 'Cosmetics', icon: 'M12 2l2.4 7.4H22l-6.2 4.5 2.4 7.4L12 17l-6.2 4.3 2.4-7.4L2 9.4h7.6L12 2z' },
  ];
</script>

<nav class="sidebar" aria-label="Main navigation">
  <div class="brand">
    <OrbitalMotif size={30} label="Orrery" />
    <span class="brand-name">Observatory</span>
  </div>

  <ul class="nav" role="list">
    {#each items as item (item.id)}
      <li>
        <NavItem
          label={item.label}
          icon={item.icon}
          active={activePage === item.id}
          onclick={() => onNavigate?.(item.id)}
        />
      </li>
    {/each}
  </ul>

  <div class="foot">
    <ProfileChip account={session.account} version={appVersion} />
  </div>
</nav>

<style>
  .sidebar {
    display: flex;
    flex-direction: column;
    width: 220px;
    flex-shrink: 0;
    height: 100%;
    background: var(--color-void-s1);
    border-right: 1px solid var(--color-hairline-base);
    padding: 18px 14px 16px;
    gap: 8px;
  }
  .brand {
    display: flex;
    align-items: center;
    gap: 11px;
    padding: 4px 6px 16px;
  }
  .brand-name {
    font-family: var(--font-display);
    font-size: var(--text-body);
    font-weight: 500;
    letter-spacing: 0.02em;
    color: var(--color-text-hi);
  }
  .nav {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: 3px;
  }
  .foot {
    margin-top: auto;
    display: flex;
    flex-direction: column;
    gap: 8px;
    padding-top: 12px;
  }
</style>
