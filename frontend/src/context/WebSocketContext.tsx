import {
  createContext,
  useContext,
  useEffect,
  useRef,
  useCallback,
  type ReactNode,
} from 'react';
import { Client } from '@stomp/stompjs';
import { useAuth } from './AuthContext';
import type { MessageResponse } from '../types';

type MessageHandler = (message: MessageResponse) => void;

interface WebSocketContextType {
  subscribe: (chatId: string, handler: MessageHandler) => () => void;
}

const WebSocketContext = createContext<WebSocketContextType | null>(null);

export function WebSocketProvider({ children }: { children: ReactNode }) {
  const { token, isAuthenticated } = useAuth();
  const clientRef = useRef<Client | null>(null);
  const handlersRef = useRef<Map<string, Set<MessageHandler>>>(new Map());

  useEffect(() => {
    if (!isAuthenticated || !token) {
      return;
    }

    const client = new Client({
      brokerURL: `${window.location.protocol === 'https:' ? 'wss:' : 'ws:'}//${window.location.host}/ws`,
      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
    });

    client.onConnect = () => {
      // Re-subscribe to all active subscriptions after reconnect
      for (const chatId of handlersRef.current.keys()) {
        client.subscribe(`/topic/chat/${chatId}`, (frame) => {
          const msg: MessageResponse = JSON.parse(frame.body);
          const handlers = handlersRef.current.get(chatId);
          handlers?.forEach((h) => h(msg));
        });
      }
    };

    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
      clientRef.current = null;
    };
  }, [token, isAuthenticated]);

  const subscribe = useCallback(
    (chatId: string, handler: MessageHandler) => {
      if (!handlersRef.current.has(chatId)) {
        handlersRef.current.set(chatId, new Set());
      }
      handlersRef.current.get(chatId)!.add(handler);

      // If client is connected, subscribe immediately
      const client = clientRef.current;
      if (client?.connected) {
        const existingHandlers = handlersRef.current.get(chatId)!;
        if (existingHandlers.size === 1) {
          // First handler for this chat, create STOMP subscription
          client.subscribe(`/topic/chat/${chatId}`, (frame) => {
            const msg: MessageResponse = JSON.parse(frame.body);
            const handlers = handlersRef.current.get(chatId);
            handlers?.forEach((h) => h(msg));
          });
        }
      }

      return () => {
        const handlers = handlersRef.current.get(chatId);
        if (handlers) {
          handlers.delete(handler);
          if (handlers.size === 0) {
            handlersRef.current.delete(chatId);
          }
        }
      };
    },
    []
  );

  return (
    <WebSocketContext.Provider value={{ subscribe }}>
      {children}
    </WebSocketContext.Provider>
  );
}

export function useWebSocket() {
  const context = useContext(WebSocketContext);
  if (!context) {
    throw new Error('useWebSocket must be used within a WebSocketProvider');
  }
  return context;
}
