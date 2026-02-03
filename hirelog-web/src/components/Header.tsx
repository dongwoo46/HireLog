import { Link, useNavigate } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';
import { TbDoorExit } from 'react-icons/tb';

export function Header() {
  const { isAuthenticated, user, logout } = useAuthStore();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  return (
    <header className="fixed w-full z-50 bg-slate-900/80 backdrop-blur-md border-b border-white/10 transition-all duration-300">
      <div className="max-w-7xl mx-auto px-6 lg:px-8">
        <div className="flex justify-between h-16 items-center">
          {/* Logo Area */}
          <div className="flex">
            <Link to="/" className="flex items-center gap-2 group">
              <div className="w-8 h-8 bg-gradient-to-tr from-blue-500 to-purple-600 rounded-lg flex items-center justify-center font-bold text-white text-lg shadow-lg group-hover:shadow-blue-500/30 transition-all">
                H
              </div>
              <span className="text-xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-blue-400 to-purple-500">
                HireLog
              </span>
            </Link>
            
            {/* Desktop Navigation */}
            <div className="hidden sm:ml-10 sm:flex sm:space-x-8">
              <Link
                to="/"
                className="text-slate-300 hover:text-white px-1 pt-1 text-sm font-medium transition-colors"
              >
                Home
              </Link>
              <Link
                to="/tools/jd-summary"
                className="text-slate-300 hover:text-white px-1 pt-1 text-sm font-medium transition-colors"
              >
                JD Summary
              </Link>
            </div>
          </div>

          {/* Right Side Actions */}
          <div className="flex items-center gap-4">
            {isAuthenticated ? (
              <>
                {/* User Profile Preview */}
                <div className="flex items-center gap-3 pl-4 border-l border-white/10">
                  <div className="w-8 h-8 rounded-full bg-gradient-to-tr from-blue-500 to-purple-600 flex items-center justify-center text-white text-xs font-bold ring-2 ring-slate-800">
                    {user?.name?.charAt(0).toUpperCase() ?? 'U'}
                  </div>
                  <span className="text-sm text-slate-200 font-medium hidden md:block">
                    {user?.name || 'User'}
                  </span>
                </div>

                {/* Door Logout Button */}
                <button
                  onClick={handleLogout}
                  className="p-2 text-slate-400 hover:text-white hover:bg-white/10 rounded-full transition-all flex items-center justify-center group relative"
                  title="Logout"
                >
                  <TbDoorExit size={22} />
                </button>
              </>
            ) : (
                <Link
                  to="/login"
                  className="text-slate-300 hover:text-white text-sm font-medium transition-colors"
                >
                  Log in
                </Link>
            )}
          </div>
        </div>
      </div>
    </header>
  );
}
