<svelte:options runes />
<script lang="ts">
  import HeroPanel from '$lib/components/HeroPanel.svelte';
  import { session } from '$lib/data/session.svelte';

  interface Props {
    onNavigate?: (page: string) => void;
  }
  let { onNavigate }: Props = $props();

  function launch() {
    // Phase 0: launch pipeline not wired. Real launch goes through Tauri (game/).
    console.log('launch requested');
  }
</script>

<HeroPanel
  account={session.account}
  target={session.target}
  onLaunch={launch}
  onSettings={() => onNavigate?.('settings')}
  onAddAccount={() => onNavigate?.('accounts')}
/>
