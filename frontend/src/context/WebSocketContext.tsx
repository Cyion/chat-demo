import {
  createContext,
  useContext,
  useEffect,
  useRef,
  useState,
  useCallback,
  type ReactNode,
} from 'react';
import { Client, type StompSubscription } from '@stomp/stompjs';
import { useAuth } from './AuthContext';
import type { MessageResponse, ChatResponse } from '../types';

type MessageHandler = (message: MessageResponse) => void;
type NewChatHandler = (chat: ChatResponse) => void;

interface WebSocketContextType {
  subscribe: (chatId: string, handler: MessageHandler) => () => void;
  subscribeToNewChats: (handler: NewChatHandler) => () => void;
  connected: boolean;
}

const WebSocketContext = createContext<WebSocketContextType | null>(null);

export function WebSocketProvider({ children }: { children: ReactNode }) {
  const { token, isAuthenticated, user } = useAuth();
  const clientRef = useRef<Client | null>(null);
  const handlersRef = useRef<Map<string, Set<MessageHandler>>>(new Map());
  const subscriptionsRef = useRef<Map<string, StompSubscription>>(new Map());
  const newChatHandlersRef = useRef<Set<NewChatHandler>>(new Set());
  const userSubRef = useRef<StompSubscription | null>(null);
  const [connected, setConnected] = useState(false);

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
      subscriptionsRef.current.clear();
      userSubRef.current = null;
      setConnected(true);
    };

    client.onWebSocketClose = () => {
      setConnected(false);
    };

    client.onStompError = (frame) => {
      console.error('STOMP error:', frame.headers['message'], frame.body);
    };

    client.onWebSocketError = (event) => {
      console.error('WebSocket error:', event);
    };

    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
      clientRef.current = null;
      setConnected(false);
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
        if (!subscriptionsRef.current.has(chatId)) {
          const sub = client.subscribe(`/topic/chat/${chatId}`, (frame) => {
            const msg: MessageResponse = JSON.parse(frame.body);
            const handlers = handlersRef.current.get(chatId);
            handlers?.forEach((h) => h(msg));
          });
          subscriptionsRef.current.set(chatId, sub);
        }
      }

      return () => {
        const handlers = handlersRef.current.get(chatId);
        if (handlers) {
          handlers.delete(handler);
          if (handlers.size === 0) {
            handlersRef.current.delete(chatId);
            const sub = subscriptionsRef.current.get(chatId);
            if (sub) {
              try { sub.unsubscribe(); } catch { /* connection already closed */ }
              subscriptionsRef.current.delete(chatId);
            }
          }
        }
      };
    },
    []
  );

  // Subscribe to user topic whenever connected — handles initial connect and reconnects
  useEffect(() => {
    if (!connected || !user) return;

    const client = clientRef.current;
    if (!client?.connected) return;

    const sub = client.subscribe(`/topic/user/${user.id}`, (frame) => {
      const chat: ChatResponse = JSON.parse(frame.body);
      newChatHandlersRef.current.forEach((h) => h(chat));
    });
    userSubRef.current = sub;

    return () => {
      try { sub.unsubscribe(); } catch { /* connection already closed */ }
      userSubRef.current = null;
    };
  }, [connected, user]);

  const subscribeToNewChats = useCallback(
    (handler: NewChatHandler) => {
      newChatHandlersRef.current.add(handler);

      return () => {
        newChatHandlersRef.current.delete(handler);
      };
    },
    []
  );

  return (
    <WebSocketContext.Provider value={{ subscribe, subscribeToNewChats, connected }}>
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
