import { useState, useEffect, useCallback } from 'react';
import { Link, useParams } from 'react-router-dom';
import { getChats } from '../api/chats';
import { useAuth } from '../context/AuthContext';
import { useWebSocket } from '../context/WebSocketContext';
import type { ChatResponse, MessageResponse } from '../types';

interface Props {
  reloadKey: number;
}

export default function ChatSidebar({ reloadKey }: Props) {
  const [chats, setChats] = useState<ChatResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const { user } = useAuth();
  const { subscribe, subscribeToNewChats } = useWebSocket();
  const { chatId: activeChatId } = useParams<{ chatId: string }>();

  const loadChats = useCallback(async () => {
    try {
      const data = await getChats();
      setChats(data);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadChats();
  }, [loadChats, reloadKey]);

  // Subscribe to all chat topics for real-time last message updates
  useEffect(() => {
    const unsubscribes: (() => void)[] = [];

    for (const chat of chats) {
      const unsub = subscribe(chat.id, (msg: MessageResponse) => {
        setChats((prev) =>
          prev
            .map((c) => (c.id === msg.chatId ? { ...c, lastMessage: msg } : c))
            .sort((a, b) => {
              const aTime = a.lastMessage?.createdAt || a.createdAt;
              const bTime = b.lastMessage?.createdAt || b.createdAt;
              return bTime.localeCompare(aTime);
            })
        );
      });
      unsubscribes.push(unsub);
    }

    return () => unsubscribes.forEach((u) => u());
  }, [chats.length, subscribe]);

  // Subscribe to new chat notifications
  useEffect(() => {
    const unsub = subscribeToNewChats((newChat: ChatResponse) => {
      setChats((prev) => {
        if (prev.some((c) => c.id === newChat.id)) return prev;
        return [newChat, ...prev];
      });
    });

    return unsub;
  }, [subscribeToNewChats]);

  function getOtherParticipant(chat: ChatResponse) {
    return chat.participants.find((p) => p.id !== user?.id) ?? chat.participants[0];
  }

  function formatTime(dateStr: string) {
    const date = new Date(dateStr);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));

    if (diffDays === 0) {
      return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    }
    if (diffDays === 1) return 'Yesterday';
    if (diffDays < 7) return date.toLocaleDateString([], { weekday: 'short' });
    return date.toLocaleDateString([], { month: 'short', day: 'numeric' });
  }

  if (loading) {
    return (
      <div className="h-full flex items-center justify-center">
        <p className="text-gray-500">Loading chats...</p>
      </div>
    );
  }

  return (
    <div className="h-full flex flex-col overflow-y-auto">
      {chats.length === 0 ? (
        <div className="text-center py-12">
          <p className="text-gray-500 mb-2">No chats yet</p>
          <p className="text-sm text-gray-400">
            Search for a user above to start a conversation
          </p>
        </div>
      ) : (
        <ul>
          {chats.map((chat) => {
            const other = getOtherParticipant(chat);
            const isActive = chat.id === activeChatId;
            return (
              <li key={chat.id}>
                <Link
                  to={`/chats/${chat.id}`}
                  className={`flex items-center gap-3 px-4 py-3 transition-colors border-b border-gray-100 ${
                    isActive
                      ? 'bg-indigo-50'
                      : 'hover:bg-gray-100'
                  }`}
                >
                  <div className="w-12 h-12 bg-indigo-100 rounded-full flex items-center justify-center text-indigo-600 font-semibold text-lg shrink-0">
                    {other.username[0].toUpperCase()}
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex justify-between items-baseline">
                      <span className="font-medium text-gray-900 truncate">
                        {other.username}
                      </span>
                      <span className="text-xs text-gray-400 ml-2 shrink-0">
                        {formatTime(
                          chat.lastMessage?.createdAt || chat.createdAt
                        )}
                      </span>
                    </div>
                    {chat.lastMessage && (
                      <p className="text-sm text-gray-500 truncate mt-0.5">
                        {chat.lastMessage.senderId === user?.id ? 'You: ' : ''}
                        {chat.lastMessage.content}
                      </p>
                    )}
                  </div>
                </Link>
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}
