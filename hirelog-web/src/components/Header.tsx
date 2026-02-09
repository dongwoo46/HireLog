import { Link, useLocation } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';
import { TbMenu2 } from 'react-icons/tb';

export function Header() {
  const { isAuthenticated, user } = useAuthStore();
  const location = useLocation();

  const isAuthPage = ['/login', '/signup'].includes(location.pathname);

  // Mode 1: Auth Pages (Logo Only)
  if (isAuthPage) {
    return (
      <header className="fixed w-full z-50 p-6">
        <div className="max-w-7xl mx-auto">
          <Link to="/" className="text-2xl font-bold tracking-tight text-gray-900">
            HireLog
          </Link>
        </div>
      </header>
    );
  }

  // Mode 2: Main App (Logo + Profile + Menu)
  return (
    <header className="fixed w-full z-50 bg-white/80 backdrop-blur-md border-b border-gray-100 transition-all duration-300">
      <div className="max-w-7xl mx-auto px-6 h-16 flex justify-between items-center">
        {/* Logo */}
        <Link to="/" className="text-2xl font-bold tracking-tight text-gray-900">
          HireLog
        </Link>

        {/* Right Side Actions */}
        <div className="flex items-center gap-4">

          {/* Profile Circle */}
          {isAuthenticated ? (
            <div className="w-10 h-10 rounded-full bg-teal-400 flex items-center justify-center text-white font-bold shadow-sm cursor-pointer" title={user?.name}>
              {user?.name?.charAt(0).toUpperCase() || 'U'}
            </div>
          ) : (
            <div className="w-10 h-10 rounded-full bg-orange-400 flex items-center justify-center shadow-sm cursor-pointer" title="Guest">
              {/* Orange circle for Guest as requested */}
            </div>
          )}

          {/* Menu Button (Hamburger) */}
          <button className="p-2 text-gray-600 hover:text-black">
            <TbMenu2 size={24} />
          </button>
        </div>
      </div>
    </header>
  );
}
