import { create } from 'zustand';
import { authService } from '../services/auth';

export interface User {
  id: string;
  email: string;
  name: string;
  avatarUrl?: string;
}

interface AuthState {
  isAuthenticated: boolean;
  user: User | null;
  isInitialized: boolean;
  
  // Actions
  login: () => void;
  logout: () => Promise<void>;
  checkAuth: () => Promise<void>;
  setUser: (user: User | null) => void;
  setAuthenticated: (isAuthenticated: boolean) => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  isAuthenticated: false,
  user: null,
  isInitialized: false,

  login: () => set({ isAuthenticated: true }),

  logout: async () => {
    try {
      await authService.logout();
    } catch (error) {
      console.error('Logout failed', error);
    } finally {
      set({ isAuthenticated: false, user: null });
    }
  },

  checkAuth: async () => {
    // Prevent double-execution is less critical here if triggered manually,
    // but useful for the initial mount check. 
    // We handle the "once" check in the component or via a flag here if we want.
    // However, the store action itself should just do the work.
    
    try {
      await authService.refreshToken();
      set({ 
        isAuthenticated: true, 
        isInitialized: true,
        user: { id: '1', email: 'user@example.com', name: 'Demo User' } // Mock
      });
    } catch (error) {
      set({ isAuthenticated: false, user: null, isInitialized: true });
    }
  },
  
  setUser: (user) => set({ user }),
  setAuthenticated: (isAuthenticated) => set({ isAuthenticated }),
}));
