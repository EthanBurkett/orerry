import './app.css';
import App from './App.svelte';
import { mount } from 'svelte';

/**
 * Observatory launcher entry point.
 * Plain Svelte 5 SPA — no SvelteKit adapter, no SSR.
 * The Tauri webview renders this as the launcher window.
 */
const app = mount(App, {
  target: document.getElementById('app')!,
});

export default app;
