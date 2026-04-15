import { useState, useEffect, useRef } from 'react';
import { searchUsers } from '../api/users';
import { createChat } from '../api/chats';
import { useAuth } from '../context/AuthContext';
import { useNavigate } from 'react-router-dom';
import type { UserSummary } from '../types';

interface Props {
  onClose: () => void;
}

export default function UserSearchModal({ onClose }: Props) {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<UserSummary[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const { user } = useAuth();
  const navigate = useNavigate();
  const debounceRef = useRef<ReturnType<typeof setTimeout>>(undefined);
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    inputRef.current?.focus();
  }, []);

  useEffect(() => {
    if (query.trim().length < 2) {
      setResults([]);
      return;
    }

    clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(async () => {
      setLoading(true);
      setError('');
      try {
        const users = await searchUsers(query.trim());
        setResults(users.filter((u) => u.id !== user?.id));
      } catch {
        setError('Failed to search users');
      } finally {
        setLoading(false);
      }
    }, 300);

    return () => clearTimeout(debounceRef.current);
  }, [query, user?.id]);

  async function handleSelect(selectedUser: UserSummary) {
    try {
      const chat = await createChat(selectedUser.username);
      onClose();
      navigate(`/chats/${chat.id}`);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create chat');
    }
  }

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-xl shadow-xl w-full max-w-md max-h-[80vh] flex flex-col">
        <div className="flex items-center justify-between p-4 border-b">
          <h2 className="text-lg font-semibold text-gray-900">New Chat</h2>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600 text-2xl leading-none"
          >
            &times;
          </button>
        </div>

        <div className="p-4">
          <input
            ref={inputRef}
            type="text"
            placeholder="Search by username..."
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
          />
        </div>

        {error && (
          <div className="px-4 pb-2 text-sm text-red-600">{error}</div>
        )}

        <div className="flex-1 overflow-y-auto px-4 pb-4">
          {loading && (
            <p className="text-sm text-gray-500 text-center py-4">
              Searching...
            </p>
          )}

          {!loading && results.length === 0 && query.trim().length >= 2 && (
            <p className="text-sm text-gray-500 text-center py-4">
              No users found
            </p>
          )}

          {results.map((u) => (
            <button
              key={u.id}
              onClick={() => handleSelect(u)}
              className="w-full text-left px-3 py-3 hover:bg-gray-50 rounded-lg flex items-center gap-3 transition-colors"
            >
              <div className="w-10 h-10 bg-indigo-100 rounded-full flex items-center justify-center text-indigo-600 font-semibold">
                {u.username[0].toUpperCase()}
              </div>
              <span className="text-gray-900 font-medium">{u.username}</span>
            </button>
          ))}
        </div>
      </div>
    </div>
  );
}
