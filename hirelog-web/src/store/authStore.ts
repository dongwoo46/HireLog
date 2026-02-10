import { create } from 'zustand';
import { authService } from '../services/auth';
import { memberService } from '../services/memberService';
import type { MemberDetailView } from '../types/member';

export type User = MemberDetailView;

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
    try {
      // Just try to get member info. 
      // If 401 occurs, apiClient interceptor will handle one refresh and retry this call.
      const userData = await memberService.getMe();
      set({ 
        isAuthenticated: true, 
        isInitialized: true,
        user: userData
      });
    } catch (error) {
      set({ isAuthenticated: false, user: null, isInitialized: true });
    }
  },
  
  setUser: (user) => set({ user }),
  setAuthenticated: (isAuthenticated) => set({ isAuthenticated }),
}));
