import { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { getChats } from '../api/chats';
import { useAuth } from '../context/AuthContext';
import { useWebSocket } from '../context/WebSocketContext';
import UserSearchModal from '../components/UserSearchModal';
import type { ChatResponse, MessageResponse } from '../types';

export default function ChatListPage() {
  const [chats, setChats] = useState<ChatResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [showSearch, setShowSearch] = useState(false);
  const { user, logout } = useAuth();
  const { subscribe } = useWebSocket();

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
  }, [loadChats]);

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
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <p className="text-gray-500">Loading chats...</p>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col">
      {/* Header */}
      <header className="bg-white border-b px-4 py-3 flex items-center justify-between sticky top-0 z-10">
        <h1 className="text-xl font-bold text-gray-900">Chats</h1>
        <div className="flex items-center gap-3">
          <span className="text-sm text-gray-500">{user?.username}</span>
          <button
            onClick={logout}
            className="text-sm text-gray-500 hover:text-gray-700"
          >
            Logout
          </button>
        </div>
      </header>

      {/* New chat button */}
      <div className="p-4">
        <button
          onClick={() => setShowSearch(true)}
          className="w-full bg-indigo-600 text-white py-2 px-4 rounded-lg font-medium hover:bg-indigo-700 transition-colors"
        >
          + New Chat
        </button>
      </div>

      {/* Chat list */}
      <div className="flex-1">
        {chats.length === 0 ? (
          <div className="text-center py-12">
            <p className="text-gray-500 mb-2">No chats yet</p>
            <p className="text-sm text-gray-400">
              Start a conversation by searching for a user
            </p>
          </div>
        ) : (
          <ul>
            {chats.map((chat) => {
              const other = getOtherParticipant(chat);
              return (
                <li key={chat.id}>
                  <Link
                    to={`/chats/${chat.id}`}
                    className="flex items-center gap-3 px-4 py-3 hover:bg-gray-100 transition-colors border-b border-gray-100"
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

      {showSearch && (
        <UserSearchModal
          onClose={() => {
            setShowSearch(false);
            loadChats();
          }}
        />
      )}
    </div>
  );
}
