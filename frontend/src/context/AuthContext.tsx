import {
  createContext,
  useContext,
  useState,
  useEffect,
  useCallback,
  type ReactNode,
} from 'react';
import type { AuthUser } from '../types';
import { getToken, setToken, removeToken } from '../api/client';
import * as authApi from '../api/auth';

interface AuthContextType {
  user: AuthUser | null;
  token: string | null;
  isAuthenticated: boolean;
  login: (username: string, password: string) => Promise<void>;
  register: (username: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | null>(null);

function decodeToken(token: string): AuthUser | null {
  try {
    const payload = token.split('.')[1];
    const decoded = JSON.parse(atob(payload));
    if (decoded.exp && decoded.exp * 1000 < Date.now()) {
      return null;
    }
    return { id: decoded.sub, username: decoded.username };
  } catch {
    return null;
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [token, setTokenState] = useState<string | null>(null);

  useEffect(() => {
    const stored = getToken();
    if (stored) {
      const decoded = decodeToken(stored);
      if (decoded) {
        setTokenState(stored);
        setUser(decoded);
      } else {
        removeToken();
      }
    }
  }, []);

  const handleAuth = useCallback(async (authToken: string) => {
    setToken(authToken);
    setTokenState(authToken);
    const decoded = decodeToken(authToken);
    setUser(decoded);
  }, []);

  const login = useCallback(
    async (username: string, password: string) => {
      const response = await authApi.login({ username, password });
      await handleAuth(response.token);
    },
    [handleAuth]
  );

  const register = useCallback(
    async (username: string, password: string) => {
      const response = await authApi.register({ username, password });
      await handleAuth(response.token);
    },
    [handleAuth]
  );

  const logout = useCallback(() => {
    removeToken();
    setTokenState(null);
    setUser(null);
  }, []);

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        isAuthenticated: !!user,
        login,
        register,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
