<svelte:options runes />
<script lang="ts">
  interface NavItem {
    id: string;
    label: string;
    icon: string; // SVG path data
  }

  interface Props {
    activePage?: string;
    onNavigate?: (page: string) => void;
  }

  let { activePage = 'home', onNavigate }: Props = $props();

  const navItems: NavItem[] = [
    {
      id: 'home',
      label: 'Home',
      // House icon
      icon: 'M3 9.5L12 3l9 6.5V20a1 1 0 01-1 1H4a1 1 0 01-1-1V9.5zM9 21V12h6v9',
    },
    {
      id: 'accounts',
      label: 'Accounts',
      // Person icon
      icon: 'M12 12c2.7 0 4.8-2.1 4.8-4.8S14.7 2.4 12 2.4 7.2 4.5 7.2 7.2 9.3 12 12 12zm0 2.4c-3.2 0-9.6 1.6-9.6 4.8v2.4h19.2v-2.4c0-3.2-6.4-4.8-9.6-4.8z',
    },
    {
      id: 'mods',
      label: 'Mods',
      // Puzzle piece icon
      icon: 'M11 4a1 1 0 10-2 0v1H6a2 2 0 00-2 2v2.5H2.5a1.5 1.5 0 000 3H4V15a2 2 0 002 2h1v1a1 1 0 102 0v-1h2v1a1 1 0 102 0v-1h1a2 2 0 002-2v-2.5h1.5a1.5 1.5 0 000-3H18V7a2 2 0 00-2-2h-2.5V4a1 1 0 10-2 0v1H11V4z',
    },
    {
      id: 'settings',
      label: 'Settings',
      // Gear icon
      icon: 'M12 15a3 3 0 100-6 3 3 0 000 6zm7-3a7 7 0 11-14 0 7 7 0 0114 0zM3.05 11H1v2h2.05A8.98 8.98 0 0011 20.95V23h2v-2.05A8.98 8.98 0 0020.95 13H23v-2h-2.05A8.98 8.98 0 0013 3.05V1h-2v2.05A8.98 8.98 0 003.05 11z',
    },
    {
      id: 'cosmetics',
      label: 'Cosmetics',
      // Sparkle/star icon
      icon: 'M12 2l2.4 7.4H22l-6.2 4.5 2.4 7.4L12 17l-6.2 4.3 2.4-7.4L2 9.4h7.6L12 2z',
    },
  ];

  function handleNav(id: string) {
    onNavigate?.(id);
  }
</script>

<nav class="sidebar" aria-label="Main navigation">
  <!-- Logo mark -->
  <div class="sidebar-logo" aria-hidden="true">
    <svg width="28" height="28" viewBox="0 0 28 28" fill="none" xmlns="http://www.w3.org/2000/svg">
      <circle cx="14" cy="14" r="12.5" stroke="var(--color-cyan-base)" stroke-width="0.75" opacity="0.35" />
      <circle cx="14" cy="14" r="8.5" stroke="var(--color-brass-base)" stroke-width="1" opacity="0.85" />
      <circle cx="14" cy="14" r="4.5" stroke="var(--color-hairline-bright)" stroke-width="0.75" />
      <circle cx="14" cy="14" r="2" fill="var(--color-brass-hi)" />
    </svg>
  </div>

  <div class="sidebar-divider" aria-hidden="true"></div>

  <!-- Nav items -->
  <ul class="nav-list" role="list">
    {#each navItems as item (item.id)}
      <li>
        <button
          class="nav-item"
          class:nav-item--active={activePage === item.id}
          onclick={() => handleNav(item.id)}
          aria-label={item.label}
          aria-current={activePage === item.id ? 'page' : undefined}
          title={item.label}
        >
          <svg
            width="20"
            height="20"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="1.5"
            stroke-linecap="round"
            stroke-linejoin="round"
            aria-hidden="true"
          >
            <path d={item.icon} />
          </svg>
          <span class="nav-label">{item.label}</span>
        </button>
      </li>
    {/each}
  </ul>
</nav>

<style>
  .sidebar {
    display: flex;
    flex-direction: column;
    align-items: center;
    width: 56px;
    height: 100%;
    background: var(--color-void-s1);
    border-right: 1px solid var(--color-hairline-base);
    padding: 16px 0 20px;
    flex-shrink: 0;
    gap: 0;
  }

  .sidebar-logo {
    width: 36px;
    height: 36px;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 4px;
  }

  .sidebar-divider {
    width: 28px;
    height: 1px;
    background: var(--color-hairline-base);
    margin: 12px 0;
  }

  .nav-list {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: 2px;
    width: 100%;
    flex: 1;
  }

  .nav-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 3px;
    width: 100%;
    padding: 10px 0 8px;
    background: none;
    border: none;
    cursor: pointer;
    color: var(--color-text-low);
    transition:
      color var(--duration-fast) var(--ease-instrument),
      background var(--duration-fast) var(--ease-instrument);
    position: relative;
  }

  .nav-item:hover {
    color: var(--color-text-mid);
    background: var(--color-void-s2);
  }

  .nav-item:focus-visible {
    outline: 1.5px solid var(--color-brass-base);
    outline-offset: -2px;
    color: var(--color-text-mid);
  }

  .nav-item--active {
    color: var(--color-brass-base);
  }

  .nav-item--active::before {
    content: '';
    position: absolute;
    left: 0;
    top: 50%;
    transform: translateY(-50%);
    width: 2px;
    height: 20px;
    background: var(--color-brass-base);
    border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
  }

  .nav-item--active:hover {
    color: var(--color-brass-hi);
    background: var(--color-void-s2);
  }

  .nav-label {
    font-family: var(--font-body);
    font-size: 9px;
    font-weight: 500;
    letter-spacing: 0.04em;
    text-transform: uppercase;
    line-height: 1;
  }
</style>
