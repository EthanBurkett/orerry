import { QueryClient } from '@tanstack/svelte-query';

/**
 * Shared QueryClient instance for the Observatory launcher.
 *
 * Defaults are conservative for a desktop app:
 * - staleTime: 30 s — launcher data (profiles, versions) doesn't need
 *   aggressive refetching; most changes come from Tauri command mutations.
 * - gcTime: 5 min — keep unused query data in the cache briefly to avoid
 *   re-fetching when the user navigates between launcher pages.
 * - retry: 1 — network calls go to localhost Tauri commands or the Horizon
 *   backend; a single retry is sufficient.
 */
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      gcTime: 5 * 60_000,
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
});
