import {
  createContext,
  useContext,
  useState,
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

function loadStoredAuth(): { user: AuthUser | null; token: string | null } {
  const stored = getToken();
  if (stored) {
    const decoded = decodeToken(stored);
    if (decoded) {
      return { user: decoded, token: stored };
    }
    removeToken();
  }
  return { user: null, token: null };
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [{ user, token }, setAuth] = useState(loadStoredAuth);

  const handleAuth = useCallback(async (authToken: string) => {
    setToken(authToken);
    const decoded = decodeToken(authToken);
    setAuth({ user: decoded, token: authToken });
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
    setAuth({ user: null, token: null });
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
