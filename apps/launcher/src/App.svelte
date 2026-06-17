<svelte:options runes />
<script lang="ts">
  // App.svelte is ONLY the QueryClientProvider boundary. Everything that calls
  // createQuery/createMutation must live in a descendant (Shell and below), so
  // the QueryClient is reachable via context. Do not call createQuery here.
  import { QueryClientProvider } from '@tanstack/svelte-query';
  import { queryClient } from '$lib/queryClient';
  import Shell from '$lib/Shell.svelte';
</script>

<QueryClientProvider client={queryClient}>
  <Shell />
</QueryClientProvider>
