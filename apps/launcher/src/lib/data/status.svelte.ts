/**
 * Service status for the footer status bar. Phase 0: mock/honest values
 * (Backend up via horizon; Auth + Sextant not yet wired). Later these reflect
 * real health checks. State lives in runes, no storage.
 */

export type ServiceState = 'ok' | 'warn' | 'down' | 'unknown';

export interface Service {
  id: string;
  label: string;
  state: ServiceState;
}

export const services = $state<Service[]>([
  { id: 'backend', label: 'Backend', state: 'ok' },
  { id: 'auth', label: 'Auth', state: 'warn' },
  { id: 'sextant', label: 'Sextant', state: 'unknown' },
]);
