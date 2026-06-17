/**
 * Window-control helpers for the custom frameless titlebar (WindowChrome).
 *
 * The launcher runs with `decorations: false`, so the titlebar's min/maximize/
 * close buttons drive the native window through Tauri's window API. In a plain
 * browser (`pnpm dev:web`, no Tauri) every call is a guarded no-op so the UI
 * still renders for design iteration.
 */
import { getCurrentWindow } from '@tauri-apps/api/window';

function inTauri(): boolean {
  return typeof window !== 'undefined' && '__TAURI_INTERNALS__' in window;
}

export async function minimizeWindow(): Promise<void> {
  if (!inTauri()) return;
  try {
    await getCurrentWindow().minimize();
  } catch (err) {
    console.warn('minimizeWindow failed', err);
  }
}

export async function toggleMaximizeWindow(): Promise<void> {
  if (!inTauri()) return;
  try {
    await getCurrentWindow().toggleMaximize();
  } catch (err) {
    console.warn('toggleMaximizeWindow failed', err);
  }
}

export async function closeWindow(): Promise<void> {
  if (!inTauri()) return;
  try {
    await getCurrentWindow().close();
  } catch (err) {
    console.warn('closeWindow failed', err);
  }
}

export async function isWindowMaximized(): Promise<boolean> {
  if (!inTauri()) return false;
  try {
    return await getCurrentWindow().isMaximized();
  } catch {
    return false;
  }
}

/**
 * Subscribe to native resize so the maximize/restore icon can stay in sync.
 * Returns an unsubscribe fn (no-op outside Tauri).
 */
export async function onWindowResized(
  cb: (maximized: boolean) => void,
): Promise<() => void> {
  if (!inTauri()) return () => {};
  try {
    const win = getCurrentWindow();
    const unlisten = await win.onResized(async () => {
      cb(await win.isMaximized());
    });
    return unlisten;
  } catch {
    return () => {};
  }
}
