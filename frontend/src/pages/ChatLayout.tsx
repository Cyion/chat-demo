import { useState, useEffect, useRef } from 'react';
import { Outlet, useNavigate } from 'react-router-dom';
import ChatSidebar from '../components/ChatSidebar';
import { useAuth } from '../context/AuthContext';
import { searchUsers } from '../api/users';
import { createChat } from '../api/chats';
import type { UserSummary } from '../types';

export default function ChatLayout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<UserSummary[]>([]);
  const [searchOpen, setSearchOpen] = useState(false);
  const [error, setError] = useState('');
  const debounceRef = useRef<ReturnType<typeof setTimeout>>(undefined);
  const searchRef = useRef<HTMLDivElement>(null);
  const [reloadKey, setReloadKey] = useState(0);

  // Debounced search
  useEffect(() => {
    if (query.trim().length < 2) {
      return;
    }

    clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(async () => {
      setError('');
      try {
        const users = await searchUsers(query.trim());
        setResults(users.filter((u) => u.id !== user?.id));
        setSearchOpen(true);
      } catch {
        setError('Failed to search users');
      }
    }, 300);

    return () => clearTimeout(debounceRef.current);
  }, [query, user?.id]);

  // Close dropdown on outside click
  useEffect(() => {
    function handleClick(e: MouseEvent) {
      if (searchRef.current && !searchRef.current.contains(e.target as Node)) {
        setSearchOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClick);
    return () => document.removeEventListener('mousedown', handleClick);
  }, []);

  async function handleSelect(selectedUser: UserSummary) {
    try {
      const chat = await createChat(selectedUser.username);
      setQuery('');
      setResults([]);
      setSearchOpen(false);
      setReloadKey((k) => k + 1);
      navigate(`/chats/${chat.id}`);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create chat');
    }
  }

  return (
    <div className="h-screen flex flex-col bg-gray-50">
      {/* Top header */}
      <header className="bg-white border-b px-6 py-3 flex items-center gap-6 shrink-0 z-20">
        <h1 className="text-xl font-bold text-gray-900 shrink-0">Chats</h1>

        {/* Search bar */}
        <div ref={searchRef} className="flex-1 max-w-md mx-auto relative">
          <input
            type="text"
            placeholder="Search users to start a chat..."
            value={query}
            onChange={(e) => {
              setQuery(e.target.value);
              if (e.target.value.trim().length >= 2) {
                setSearchOpen(true);
              } else {
                setResults([]);
                setSearchOpen(false);
                setError('');
              }
            }}
            onFocus={() => {
              if (results.length > 0) setSearchOpen(true);
            }}
            className="w-full px-4 py-2 border border-gray-300 rounded-full text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
          />

          {/* Dropdown results */}
          {searchOpen && query.trim().length >= 2 && (
            <div className="absolute top-full mt-1 w-full bg-white border border-gray-200 rounded-xl shadow-lg max-h-64 overflow-y-auto z-30">
              {error && (
                <p className="px-4 py-2 text-sm text-red-600">{error}</p>
              )}
              {results.length === 0 && !error && (
                <p className="px-4 py-3 text-sm text-gray-500 text-center">
                  No users found
                </p>
              )}
              {results.map((u) => (
                <button
                  key={u.id}
                  onClick={() => handleSelect(u)}
                  className="w-full text-left px-4 py-2.5 hover:bg-gray-50 flex items-center gap-3 transition-colors"
                >
                  <div className="w-8 h-8 bg-indigo-100 rounded-full flex items-center justify-center text-indigo-600 font-semibold text-sm shrink-0">
                    {u.username[0].toUpperCase()}
                  </div>
                  <span className="text-sm text-gray-900 font-medium">
                    {u.username}
                  </span>
                </button>
              ))}
            </div>
          )}
        </div>

        {/* User info & logout */}
        <div className="flex items-center gap-3 shrink-0">
          <span className="text-sm text-gray-500">{user?.username}</span>
          <button
            onClick={logout}
            className="text-sm text-gray-500 hover:text-gray-700"
          >
            Logout
          </button>
        </div>
      </header>

      {/* Body */}
      <div className="flex-1 flex min-h-0">
        {/* Sidebar */}
        <div className="w-80 shrink-0 border-r border-gray-200 bg-white">
          <ChatSidebar reloadKey={reloadKey} />
        </div>

        {/* Main content */}
        <div className="flex-1 flex flex-col min-w-0">
          <Outlet />
        </div>
      </div>
    </div>
  );
}
