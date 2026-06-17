<svelte:options runes />
<script lang="ts">
  /**
   * The signature motif: concentric orbital rings rotating at different rates —
   * literally an orrery. Used as ambient background art (large, faint, set
   * opacity via the parent) and as the brand mark. Decorative by default.
   */
  interface Props {
    size?: number;
    /** Decorative art (aria-hidden) vs. a labelled mark. */
    label?: string;
    class?: string;
  }
  let { size = 120, label, class: className = '' }: Props = $props();
</script>

<svg
  width={size}
  height={size}
  viewBox="0 0 120 120"
  class={className}
  role={label ? 'img' : 'presentation'}
  aria-label={label}
  aria-hidden={label ? undefined : true}
  xmlns="http://www.w3.org/2000/svg"
>
  <!-- Outer ring: cyan hairline, slow, with tick marks -->
  <g class="ring ring--slow">
    <circle cx="60" cy="60" r="52" fill="none" stroke="var(--color-cyan-base)" stroke-width="0.75" opacity="0.35" />
    {#each Array(12) as _, i (i)}
      {@const rad = ((i / 12) * 360 * Math.PI) / 180}
      <line
        x1={60 + 49 * Math.cos(rad)}
        y1={60 + 49 * Math.sin(rad)}
        x2={60 + 52 * Math.cos(rad)}
        y2={60 + 52 * Math.sin(rad)}
        stroke="var(--color-cyan-base)"
        stroke-width="0.75"
        opacity="0.3"
      />
    {/each}
    <circle cx="60" cy="8" r="3" fill="var(--color-cyan-base)" opacity="0.9" />
  </g>

  <!-- Middle ring: brass, medium -->
  <g class="ring ring--medium">
    <circle cx="60" cy="60" r="36" fill="none" stroke="var(--color-brass-base)" stroke-width="1" opacity="0.85" />
    <circle cx="60" cy="24" r="2.4" fill="var(--color-brass-base)" opacity="0.95" />
  </g>

  <!-- Inner ring: hairline, fast -->
  <g class="ring ring--fast">
    <circle cx="60" cy="60" r="20" fill="none" stroke="var(--color-hairline-bright)" stroke-width="0.75" />
    <circle cx="60" cy="40" r="1.8" fill="var(--color-text-low)" opacity="0.8" />
  </g>

  <!-- Center sun -->
  <circle cx="60" cy="60" r="4" fill="var(--color-brass-hi)" />
  <circle cx="60" cy="60" r="7.5" fill="none" stroke="var(--color-brass-hi)" stroke-width="0.5" opacity="0.3" />
</svg>

<style>
  svg {
    overflow: visible;
    display: block;
  }
  .ring {
    transform-origin: 60px 60px;
  }
  .ring--slow {
    animation: orbit 24s linear infinite;
  }
  .ring--medium {
    animation: orbit 14s linear infinite;
  }
  .ring--fast {
    animation: orbit 7s linear infinite;
  }
  @keyframes orbit {
    to {
      transform: rotate(360deg);
    }
  }
  @media (prefers-reduced-motion: reduce) {
    .ring {
      animation: none;
    }
  }
</style>
