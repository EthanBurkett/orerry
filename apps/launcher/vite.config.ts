import { defineConfig } from 'vite';
import { svelte } from '@sveltejs/vite-plugin-svelte';
import tailwindcss from '@tailwindcss/vite';
import { resolve } from 'path';

// https://vitejs.dev/config/
// Tauri expects the dev server on a fixed port and disables HMR in production.
// TAURI_ENV_DEBUG is set by the Tauri CLI during `tauri dev`.
const isTauriDev = process.env['TAURI_ENV_DEBUG'] !== undefined;

export default defineConfig({
  plugins: [
    // Tailwind v4: CSS-first, no tailwind.config file.
    // The @theme block comes from @orrery/design-tokens/css, imported in app.css.
    tailwindcss(),
    svelte(),
  ],

  resolve: {
    alias: {
      $lib: resolve('./src/lib'),
    },
  },

  // Tauri expects a fixed port in dev mode
  server: {
    port: 1420,
    strictPort: true,
    // Tauri dev watch: don't open a browser tab
    open: !isTauriDev,
    // Never watch the Rust crate / its target dir. Cargo locks files there
    // during `tauri dev`'s rebuilds; watching them throws EBUSY and crashes
    // the Vite dev server (which runs as Tauri's beforeDevCommand).
    watch: {
      ignored: ['**/src-tauri/**'],
    },
  },

  // Tauri uses Vite in prod-build mode; ensure assets are inlined-friendly
  build: {
    // Tauri embeds the dist — use relative paths
    outDir: 'dist',
    emptyOutDir: true,
  },

  // Prevent Vite from obscuring Rust errors in Tauri
  clearScreen: false,
});
