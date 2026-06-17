/**
 * Session state — the selected account and the resolved launch target.
 *
 * Phase 0: mock data, held in Svelte runes. Real data arrives later from the
 * auth chain (Meridian) and game-file resolution (game/), wired via Tauri
 * commands + svelte-query. No browser storage — state lives here.
 */

export type AccountStatus = 'ready' | 'in-game' | 'offline';

export interface Account {
  username: string;
  /** Hypixel rank label, e.g. "MVP+". Empty string = no rank. */
  rank: string;
  status: AccountStatus;
  /** Mock quick-stats; replaced by real profile data later. */
  lastPlayed: string;
  playtime: string;
  profile: string;
}

export interface LaunchTarget {
  minecraft: string;
  fabric: string;
}

/** Reactive session singleton. `account = null` models the signed-out state. */
export const session = $state<{
  account: Account | null;
  target: LaunchTarget;
}>({
  account: {
    username: 'Wanderer',
    rank: 'MVP+',
    status: 'ready',
    lastPlayed: '2d ago',
    playtime: '142h',
    profile: 'Mango',
  },
  target: {
    minecraft: '1.21.11',
    fabric: '0.16.x',
  },
});
