<svelte:options runes />
<script lang="ts">
  /**
   * Loading indicator: concentric ring arcs spinning at different rates and
   * directions — the orrery motif as a spinner (DESIGN_SPEC §5.4). Use for
   * genuine in-place loading; prefer skeletons for content.
   */
  interface Props {
    size?: number;
    label?: string;
  }
  let { size = 20, label = 'Loading' }: Props = $props();
</script>

<svg
  width={size}
  height={size}
  viewBox="0 0 48 48"
  role="status"
  aria-label={label}
  xmlns="http://www.w3.org/2000/svg"
>
  <circle class="arc arc--outer" cx="24" cy="24" r="20" fill="none" stroke="var(--color-brass-base)" stroke-width="2" stroke-linecap="round" stroke-dasharray="40 86" />
  <circle class="arc arc--mid" cx="24" cy="24" r="13" fill="none" stroke="var(--color-cyan-base)" stroke-width="1.75" stroke-linecap="round" stroke-dasharray="22 60" opacity="0.8" />
  <circle cx="24" cy="24" r="2.5" fill="var(--color-brass-hi)" />
</svg>

<style>
  svg {
    display: block;
  }
  .arc {
    transform-origin: 24px 24px;
  }
  .arc--outer {
    animation: spin 1s linear infinite;
  }
  .arc--mid {
    animation: spin 1.4s linear infinite reverse;
  }
  @keyframes spin {
    to {
      transform: rotate(360deg);
    }
  }
  @media (prefers-reduced-motion: reduce) {
    .arc {
      animation-duration: 2.4s;
    }
  }
</style>
