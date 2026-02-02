import { createContext, useContext, useState, type ReactNode, useEffect } from 'react';
import { authService } from '../services/auth';

export interface User {
  id: string;
  email: string;
  name: string;
  avatarUrl?: string;
}

interface AuthContextType {
  isAuthenticated: boolean;
  user: User | null;
  login: () => void;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  // TODO: In a real app, we might check an initial "me" endpoint or just default to false
  // and rely on the first 401 to log us out, or a successful login to set true.
  // For now, let's default to false, and rely on LoginPage calling login().
  // Whether we have an access token is determining factor. 
  // We can decode JWT here if we had the library, or fetch /me.
  const [isAuthenticated, setIsAuthenticated] = useState<boolean>(false);
  const [user, setUser] = useState<User | null>(null);

  // Check auth on mount
  useEffect(() => {
    const checkAuth = async () => {
      try {
        await authService.refreshToken();
        setIsAuthenticated(true);
        // Mock user data for now since we don't have a /me endpoint or JWT decoder yet
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
