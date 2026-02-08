import { createContext, useContext, useState, type ReactNode, useEffect, useRef } from 'react';

import { authService } from '../services/auth';
import { getCookie, isTokenExpired } from '../utils/authUtils';

interface User {
  id: string;
  email: string;
  name: string;
}

interface AuthContextType {
  isAuthenticated: boolean;
  user: User | null;
  login: () => void;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [isAuthenticated, setIsAuthenticated] = useState<boolean>(false);
  const [user, setUser] = useState<User | null>(null);
  const isInitialized = useRef(false);

  // Check auth on mount
  useEffect(() => {
    // Prevent double-execution in StrictMode (Dev) or unnecessary re-checks
    if (isInitialized.current) return;
    isInitialized.current = true;

    const checkAuth = async () => {
      const accessToken = getCookie('accessToken');

      // If we have a valid access token, skip refresh
      if (accessToken && !isTokenExpired(accessToken)) {
        setIsAuthenticated(true);
        // Mock user data for now - in real app, we might need a separate /me call
        setUser({
          id: '1',
          email: 'user@example.com',
          name: 'Demo User',
        });
        return;
      }

      try {
        await authService.refreshToken();
        setIsAuthenticated(true);
        // Mock user data for now
        setUser({
          id: '1',
          email: 'user@example.com',
          name: 'Demo User',
        });
      } catch (error) {
        setIsAuthenticated(false);
        setUser(null);
      }
    };
    checkAuth();
  }, []);

  const login = () => {
    setIsAuthenticated(true);
  };

  const logout = async () => {
    try {
      await authService.logout();
    } catch (error) {
      console.error('Logout failed', error);
      // Force logout anyway
    } finally {
      setIsAuthenticated(false);
      setUser(null);
      // Optional: Redirect to login page is handled by the component calling logout or routing
    }
  };

  // Listen for auth:logout event from apiClient
  useEffect(() => {
    const handleLogoutEvent = () => {
      logout();
    };
    window.addEventListener('auth:logout', handleLogoutEvent);
    return () => {
      window.removeEventListener('auth:logout', handleLogoutEvent);
    };
  }, []);

  return (
    <AuthContext.Provider value={{ isAuthenticated, user, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
