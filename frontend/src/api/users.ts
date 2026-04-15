import { apiFetch } from './client';
import type { UserSummary } from '../types';

export function searchUsers(username: string): Promise<UserSummary[]> {
  return apiFetch<UserSummary[]>(
    `/api/users/search?username=${encodeURIComponent(username)}`
  );
}
