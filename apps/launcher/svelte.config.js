import { vitePreprocess } from '@sveltejs/vite-plugin-svelte';

/** @type {import('@sveltejs/vite-plugin-svelte').SvelteConfig} */
const config = {
  // Use vitePreprocess to handle <script lang="ts"> and PostCSS in <style>.
  // No SvelteKit adapter — this is a plain Svelte 5 SPA inside a Tauri shell.
  preprocess: vitePreprocess(),

  compilerOptions: {
    // Do NOT enable runes globally here — that would force runes mode on
    // third-party library components (e.g. @tanstack/svelte-query's
    // QueryClientProvider.svelte) that still use legacy `export let` syntax,
    // causing a compile error. Our own components declare `<svelte:options runes />`
    // individually, which works in Svelte 5 without breaking library components.
  },
};

export default config;
