<svelte:options runes />
<script lang="ts">
  interface Props {
    size?: number;
    class?: string;
  }

  let { size = 120, class: className = '' }: Props = $props();
</script>

<!--
  Orbital rings motif — three concentric rings rotating at different rates,
  literally modelling an orrery mechanism. Ambient decoration only; never
  shown as a blocking spinner (use a skeleton instead).
-->
<svg
  width={size}
  height={size}
  viewBox="0 0 120 120"
  role="img"
  aria-label="Orrery orbital rings"
  class={className}
  xmlns="http://www.w3.org/2000/svg"
>
  <!-- Outer ring: cyan hairline, slow rotation -->
  <circle
    cx="60"
    cy="60"
    r="52"
    fill="none"
    stroke="var(--color-cyan-base)"
    stroke-width="0.75"
    opacity="0.35"
    class="ring-outer"
  />
  <!-- Tick marks on outer ring -->
  {#each Array(12) as _, i}
    {@const angle = (i / 12) * 360}
    {@const rad = (angle * Math.PI) / 180}
    {@const x1 = 60 + 49 * Math.cos(rad)}
    {@const y1 = 60 + 49 * Math.sin(rad)}
    {@const x2 = 60 + 52 * Math.cos(rad)}
    {@const y2 = 60 + 52 * Math.sin(rad)}
    <line
      x1={x1}
      y1={y1}
      x2={x2}
      y2={y2}
      stroke="var(--color-cyan-base)"
      stroke-width="0.75"
      opacity="0.3"
      class="ring-outer"
    />
  {/each}

  <!-- Planet on outer ring -->
  <circle cx="60" cy="8" r="3" fill="var(--color-cyan-base)" opacity="0.9" class="ring-outer" />

  <!-- Middle ring: brass, medium rotation -->
  <circle
    cx="60"
    cy="60"
    r="36"
    fill="none"
    stroke="var(--color-brass-base)"
    stroke-width="1"
    opacity="0.85"
    class="ring-middle"
  />
  <!-- Planet on middle ring -->
  <circle cx="60" cy="24" r="2.4" fill="var(--color-brass-base)" opacity="0.95" class="ring-middle" />

  <!-- Inner ring: hairline-bright, fast rotation -->
  <circle
    cx="60"
    cy="60"
    r="20"
    fill="none"
    stroke="var(--color-hairline-bright)"
    stroke-width="0.75"
    class="ring-inner"
  />
  <!-- Planet on inner ring -->
  <circle cx="60" cy="40" r="1.8" fill="var(--color-text-low)" opacity="0.8" class="ring-inner" />

  <!-- Center sun dot -->
  <circle cx="60" cy="60" r="4" fill="var(--color-brass-hi)" />
  <circle cx="60" cy="60" r="7" fill="none" stroke="var(--color-brass-hi)" stroke-width="0.5" opacity="0.3" />
</svg>

<style>
  svg {
    overflow: visible;
  }

  .ring-outer {
    transform-origin: 60px 60px;
    animation: orbit-slow 24s linear infinite;
  }

  .ring-middle {
    transform-origin: 60px 60px;
    animation: orbit-medium 14s linear infinite;
  }

  .ring-inner {
    transform-origin: 60px 60px;
    animation: orbit-fast 7s linear infinite;
  }

  @keyframes orbit-slow {
    to { transform: rotate(360deg); }
  }

  @keyframes orbit-medium {
    to { transform: rotate(360deg); }
  }

  @keyframes orbit-fast {
    to { transform: rotate(360deg); }
  }

  @media (prefers-reduced-motion: reduce) {
    .ring-outer,
    .ring-middle,
    .ring-inner {
      animation: none;
    }
  }
</style>
