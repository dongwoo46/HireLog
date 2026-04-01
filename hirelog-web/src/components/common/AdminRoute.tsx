import { Navigate, Outlet } from 'react-router-dom';
import { useAuthStore } from '../../store/authStore';

export function AdminRoute() {
  const { isInitialized, isAuthenticated, user } = useAuthStore();

  if (!isInitialized) return null;
  if (!isAuthenticated) return <Navigate to="/login" replace />;
  if (!user || user.role !== 'ADMIN') return <Navigate to="/" replace />;

  return <Outlet />;
}
