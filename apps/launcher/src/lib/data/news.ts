/**
 * Observatory news / changelog feed. Phase 0: static mock content. Later this
 * comes from Comet (releases) and a notices endpoint via svelte-query.
 */

export type NewsType = 'update' | 'notice';

export interface NewsEntry {
  id: string;
  type: NewsType;
  /** Short version/phase tag, e.g. "v0.1" or "Phase 0". */
  tag: string;
  title: string;
  body: string;
  isNew: boolean;
}

export const news: NewsEntry[] = [
  {
    id: 'phase0-scaffold',
    type: 'update',
    tag: 'v0.1',
    title: 'Observatory scaffold online',
    body: 'Launcher, mod, and backend skeletons build green. The design-token pipeline is verified end to end.',
    isNew: true,
  },
  {
    id: 'eclipse-next',
    type: 'notice',
    tag: 'Phase 1',
    title: 'Eclipse spine is next',
    body: 'First custom menu: intercept the SkyBlock main menu, render it in Lumen, route one click through the clickSlot chokepoint.',
    isNew: true,
  },
  {
    id: 'tokens-locked',
    type: 'notice',
    tag: '§5',
    title: 'Identity locked',
    body: 'Brass instrument in the void. One token source drives the launcher and the in-game UI; they cannot drift.',
    isNew: false,
  },
];

export const newCount = news.filter((n) => n.isNew).length;
