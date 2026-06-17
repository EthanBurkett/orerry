<svelte:options runes />
<script lang="ts">
  import OrbitalMotif from './OrbitalMotif.svelte';

  /**
   * Player avatar slot. Renders a Minecraft skin-head when `src` is provided;
   * otherwise an orbital placeholder mark (signed-out / no-skin state). Square
   * with a hairline frame and a faint brass glow — an instrument readout, not a
   * generic round avatar.
   */
  interface Props {
    size?: number;
    username?: string;
    src?: string | null;
  }
  let { size = 44, username, src = null }: Props = $props();
</script>

<div
  class="avatar"
  style="--s:{size}px"
  role="img"
  aria-label={username ? `${username} avatar` : 'no account'}
>
  {#if src}
    <img class="skin" {src} alt="" />
  {:else}
    <div class="placeholder" aria-hidden="true">
      <OrbitalMotif size={Math.round(size * 0.7)} />
    </div>
  {/if}
</div>

<style>
  .avatar {
    width: var(--s);
    height: var(--s);
    display: grid;
    place-items: center;
    background: var(--color-void-s2);
    border: 1px solid var(--color-hairline-base);
    border-radius: var(--radius-md);
    box-shadow: inset 0 0 16px -8px color-mix(in oklch, var(--color-brass-base) 50%, transparent);
    overflow: hidden;
    flex-shrink: 0;
  }
  .skin {
    width: 100%;
    height: 100%;
    object-fit: cover;
    /* Minecraft heads are low-res; keep the pixels crisp. */
    image-rendering: pixelated;
  }
  .placeholder {
    opacity: 0.85;
  }
</style>
