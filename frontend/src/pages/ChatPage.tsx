import { useState, useEffect, useRef, type FormEvent } from 'react';
import { useParams } from 'react-router-dom';
import { getMessages, sendMessage } from '../api/messages';
import { getChats } from '../api/chats';
import { useAuth } from '../context/AuthContext';
import { useWebSocket } from '../context/WebSocketContext';
import type { MessageResponse, ChatResponse } from '../types';

export default function ChatPage() {
  const { chatId } = useParams<{ chatId: string }>();
  const [messages, setMessages] = useState<MessageResponse[]>([]);
  const [input, setInput] = useState('');
  const [sending, setSending] = useState(false);
  const [chat, setChat] = useState<ChatResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const { user } = useAuth();
  const { subscribe, connected } = useWebSocket();
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  // Load chat info and messages
  useEffect(() => {
    if (!chatId) return;

    async function load() {
      try {
        const [chats, msgs] = await Promise.all([
          getChats(),
          getMessages(chatId!),
        ]);
        const currentChat = chats.find((c) => c.id === chatId) ?? null;
        setChat(currentChat);
        setMessages(msgs);
      } finally {
        setLoading(false);
      }
    }

    load();
  }, [chatId]);

  // Subscribe to WebSocket messages
  useEffect(() => {
    if (!chatId || !connected) return;

    const unsub = subscribe(chatId, (msg: MessageResponse) => {
      setMessages((prev) => {
        if (prev.some((m) => m.id === msg.id)) return prev;
        return [...prev, msg];
      });
    });

    return unsub;
  }, [chatId, subscribe, connected]);

  // Auto-scroll to bottom
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  // Focus input on load
  useEffect(() => {
    if (!loading) inputRef.current?.focus();
  }, [loading]);

  async function handleSend(e: FormEvent) {
    e.preventDefault();
    if (!chatId || !input.trim() || sending) return;

    setSending(true);
    try {
      const sentMsg = await sendMessage(chatId, input.trim());
      setMessages((prev) => {
        if (prev.some((m) => m.id === sentMsg.id)) return prev;
        return [...prev, sentMsg];
      });
      setInput('');
    } finally {
      setSending(false);
      inputRef.current?.focus();
    }
  }

  const otherUser =
    chat?.participants.find((p) => p.id !== user?.id) ??
    chat?.participants[0];

  if (loading) {
    return (
      <div className="h-full flex items-center justify-center">
        <p className="text-gray-500">Loading...</p>
      </div>
    );
  }

  return (
    <div className="h-full flex flex-col">
      {/* Header */}
      <header className="bg-white border-b border-gray-200 px-4 py-3 flex items-center gap-3 shrink-0">
        <div className="w-9 h-9 bg-indigo-100 rounded-full flex items-center justify-center text-indigo-600 font-semibold">
          {otherUser?.username[0].toUpperCase()}
        </div>
        <h2 className="font-semibold text-gray-900">{otherUser?.username}</h2>
      </header>

      {/* Messages */}
      <div className="flex-1 overflow-y-auto px-4 py-4 space-y-2">
        {messages.length === 0 && (
          <p className="text-center text-gray-400 py-8 text-sm">
            No messages yet. Say hi!
          </p>
        )}

        {messages.map((msg) => {
          const isOwn = msg.senderId === user?.id;
          return (
            <div
              key={msg.id}
              className={`flex ${isOwn ? 'justify-end' : 'justify-start'}`}
            >
              <div
                className={`max-w-[75%] px-4 py-2 rounded-2xl ${
                  isOwn
                    ? 'bg-indigo-600 text-white rounded-br-md'
                    : 'bg-white text-gray-900 border border-gray-200 rounded-bl-md'
                }`}
              >
                <p className="break-words whitespace-pre-wrap">{msg.content}</p>
                <p
                  className={`text-xs mt-1 ${
                    isOwn ? 'text-indigo-200' : 'text-gray-400'
                  }`}
                >
                  {new Date(msg.createdAt).toLocaleTimeString([], {
                    hour: '2-digit',
                    minute: '2-digit',
                  })}
                </p>
              </div>
            </div>
          );
        })}
        <div ref={messagesEndRef} />
      </div>

      {/* Input */}
      <form
        onSubmit={handleSend}
        className="bg-white border-t border-gray-200 px-4 py-3 flex gap-2 sticky bottom-0"
      >
        <input
          ref={inputRef}
          type="text"
          placeholder="Type a message..."
          value={input}
          onChange={(e) => setInput(e.target.value)}
          maxLength={2000}
          className="flex-1 px-4 py-2 border border-gray-300 rounded-full focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
        />
        <button
          type="submit"
          disabled={!input.trim() || sending}
          className="bg-indigo-600 text-white px-5 py-2 rounded-full font-medium hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
        >
          Send
        </button>
      </form>
    </div>
  );
}
