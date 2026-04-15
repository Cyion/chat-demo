import { apiFetch } from './client';
import type { ChatResponse } from '../types';

export function getChats(): Promise<ChatResponse[]> {
  return apiFetch<ChatResponse[]>('/api/chats');
}

export function createChat(otherUsername: string): Promise<ChatResponse> {
  return apiFetch<ChatResponse>('/api/chats', {
    method: 'POST',
    body: JSON.stringify({ otherUsername }),
  });
}
