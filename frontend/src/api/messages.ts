import { apiFetch } from './client';
import type { MessageResponse } from '../types';

export function getMessages(
  chatId: string,
  page = 0,
  size = 50
): Promise<MessageResponse[]> {
  return apiFetch<MessageResponse[]>(
    `/api/chats/${chatId}/messages?page=${page}&size=${size}`
  );
}

export function sendMessage(
  chatId: string,
  content: string
): Promise<MessageResponse> {
  return apiFetch<MessageResponse>(`/api/chats/${chatId}/messages`, {
    method: 'POST',
    body: JSON.stringify({ content }),
  });
}
