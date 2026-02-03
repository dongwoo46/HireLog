import { Outlet } from 'react-router-dom';
import { Header } from '../components/Header';
import { useAuthStore } from '../store/authStore';
import { useEffect, useRef } from 'react';

export function RootLayout() {
  const checkAuth = useAuthStore((state) => state.checkAuth);
  const isInitialized = useRef(false);

  useEffect(() => {
    if (!isInitialized.current) {
      isInitialized.current = true;
      checkAuth();
    }
  }, [checkAuth]);

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col">
      <Header />
      <main className="flex-grow">
        <Outlet />
      </main>
    </div>
  );
}
