import { useState, useEffect } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';
import { TbLogout, TbChevronDown, TbFileText, TbSearch, TbStar, TbUserCircle, TbPlus, TbMenu2, TbX, TbClipboardCheck } from 'react-icons/tb';

export function Header() {
  const { isAuthenticated, user, logout } = useAuthStore();
  const location = useLocation();
  const navigate = useNavigate();
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);

  const isAuthPage = ['/login', '/signup'].includes(location.pathname);

  // Close mobile menu on route change
  useEffect(() => {
    setIsMobileMenuOpen(false);
  }, [location]);

  const handleLogout = async () => {
    await logout();
    navigate('/');
  };

  const navItems = {
    guest: [
      { label: '서비스 소개', path: 'intro', icon: <TbFileText size={18} /> },
      { label: '사용 가이드', path: 'guide', icon: <TbClipboardCheck size={18} /> },
    ],
    jobSummary: [
      { label: 'JD 요약 요청', path: '/jd/request', icon: <TbFileText size={18} /> },
      { label: 'JD 조회', path: '/jd', icon: <TbSearch size={18} /> },
      { label: '저장된 JD 조회', path: '/jd?filter=saved', icon: <TbStar size={18} /> },
    ],
    userRequest: [
      { label: '나의 요청 내역', path: '/requests', icon: <TbUserCircle size={18} /> },
      { label: '새로운 요청 작성', path: '/requests/new', icon: <TbPlus size={18} /> },
    ]
  };

  // Mode 1: Auth Pages (Logo Only)
  if (isAuthPage) {
    return (
      <header className="fixed w-full z-50 p-6">
        <div className="max-w-7xl mx-auto">
          <Link to="/" className="text-2xl font-black tracking-tight text-gray-900 flex items-center gap-2 group">
            <div className="w-8 h-8 bg-gradient-to-tr from-[#276db8] to-[#89cbb6] rounded-lg shadow-sm group-hover:rotate-12 transition-transform" />
            HireLog
          </Link>
        </div>
      </header>
    );
  }

  return (
    <header className={`fixed w-full z-50 transition-all duration-300 ${
      !isAuthenticated && location.pathname === '/' 
        ? 'bg-[#0f172a]/80 backdrop-blur-xl border-b border-white/5' 
        : 'bg-white/70 backdrop-blur-xl border-b border-gray-100'
    }`}>
      <div className="max-w-7xl mx-auto px-6 h-16 flex justify-between items-center">
        {/* Left Side: Logo & Main Nav */}
        <div className="flex items-center gap-10">
          <Link to="/" className={`text-2xl font-black tracking-tight flex items-center gap-2 group ${
            !isAuthenticated && location.pathname === '/' ? 'text-white' : 'text-gray-900'
          }`}>
            <div className="w-8 h-8 bg-gradient-to-tr from-[#276db8] to-[#89cbb6] rounded-lg shadow-sm group-hover:rotate-12 transition-transform" />
            HireLog
          </Link>

          {/* Navigation Links (Desktop) */}
          <nav className="hidden md:flex items-center gap-1">
            {isAuthenticated ? (
              <>
                <NavDropdown label="JD 요약" items={navItems.jobSummary} />
                <NavDropdown label="사용자 요청" items={navItems.userRequest} />
              </>
            ) : (
              navItems.guest.map((item, idx) => (
                <button
                  key={idx}
                  onClick={() => {
                    const el = document.getElementById(item.path);
                    el?.scrollIntoView({ behavior: 'smooth' });
                  }}
                  className={`px-4 py-2 text-sm font-bold transition-colors ${
                    !isAuthenticated && location.pathname === '/' 
                      ? 'text-gray-400 hover:text-[#89cbb6]' 
                      : 'text-gray-600 hover:text-[#276db8]'
                  }`}
                >
                  {item.label}
                </button>
              ))
            )}
          </nav>
        </div>

        {/* Right Side Actions */}
        <div className="flex items-center gap-2 sm:gap-4">
          {isAuthenticated ? (
            <div className="flex items-center gap-2 sm:gap-3">
              <span className="text-sm font-bold text-gray-600 hidden lg:block">
                {user?.name}님
              </span>
              
              {/* Profile Circle */}
              <div 
                onClick={() => navigate('/profile')}
                className="h-10 px-4 rounded-full bg-gradient-to-tr from-[#276db8] to-[#89cbb6] flex items-center justify-center text-white text-sm font-black shadow-lg shadow-[#89cbb6]/20 cursor-pointer hover:scale-105 transition-all group" 
                title={user?.name}
              >
                <span className="tracking-tighter group-hover:tracking-normal transition-all duration-300">
                  {user?.name || 'User'}
                </span>
              </div>

              {/* Logout Button */}
              <button 
                onClick={handleLogout}
                className="p-2 text-gray-400 hover:text-red-500 transition-colors"
                title="Logout"
              >
                <TbLogout size={24} />
              </button>
            </div>
          ) : (
            <Link
              to="/login"
              className={`px-6 py-2 rounded-full font-bold flex items-center gap-2 transition-all ${
                !isAuthenticated && location.pathname === '/' 
                  ? 'bg-white text-[#0f172a] hover:bg-[#89cbb6]' 
                  : 'text-[#276db8] hover:bg-gray-50 border border-gray-200'
              }`}
            >
              로그인
            </Link>
          )}

          {/* Mobile Menu Button */}
          <button 
            className={`md:hidden p-2 rounded-xl transition-colors ${
              !isAuthenticated && location.pathname === '/' ? 'text-white hover:bg-white/5' : 'text-gray-600 hover:bg-gray-50'
            }`}
            onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
          >
            {isMobileMenuOpen ? <TbX size={28} /> : <TbMenu2 size={28} />}
          </button>
        </div>
      </div>

      {/* Mobile Menu Overlay */}
      <div className={`
        fixed inset-0 top-16 bg-white z-[40] transition-all duration-300 md:hidden
        ${isMobileMenuOpen ? 'opacity-100 translate-y-0' : 'opacity-0 -translate-y-4 pointer-events-none'}
      `}>
        <div className="p-6 space-y-8 overflow-y-auto h-full pb-32">
          {isAuthenticated ? (
            <>
              {/* JobSummary Section */}
              <div>
                <h3 className="text-[10px] font-black text-[#89cbb6] uppercase tracking-[0.3em] mb-4 pl-1 italic">JD 요약 조회</h3>
                <div className="space-y-2">
                  {navItems.jobSummary.map((item, idx) => (
                    <Link
                      key={idx}
                      to={item.path}
                      className="flex items-center gap-4 p-4 bg-gray-50 rounded-2xl active:scale-[0.98] transition-all group"
                    >
                      <div className="w-10 h-10 rounded-xl bg-white flex items-center justify-center text-[#276db8] shadow-sm group-hover:bg-[#276db8] group-hover:text-white transition-all">
                        {item.icon}
                      </div>
                      <span className="text-sm font-bold text-gray-700">{item.label}</span>
                    </Link>
                  ))}
                </div>
              </div>

              {/* UserRequest Section */}
              <div>
                <h3 className="text-[10px] font-black text-[#276db8] uppercase tracking-[0.3em] mb-4 pl-1 italic">메뉴</h3>
                <div className="space-y-2">
                  {navItems.userRequest.map((item, idx) => (
                    <Link
                      key={idx}
                      to={item.path}
                      className="flex items-center gap-4 p-4 bg-gray-50 rounded-2xl active:scale-[0.98] transition-all group"
                    >
                      <div className="w-10 h-10 rounded-xl bg-white flex items-center justify-center text-[#89cbb6] shadow-sm group-hover:bg-[#89cbb6] group-hover:text-white transition-all">
                        {item.icon}
                      </div>
                      <span className="text-sm font-bold text-gray-700">{item.label}</span>
                    </Link>
                  ))}
                </div>
              </div>

              {/* Logout Section */}
              <div className="pt-4 border-t border-gray-100">
                <button 
                  onClick={() => {
                    handleLogout();
                    setIsMobileMenuOpen(false);
                  }}
                  className="w-full flex items-center gap-4 p-4 bg-red-50 text-red-600 rounded-2xl active:scale-[0.98] transition-all"
                >
                  <div className="w-10 h-10 rounded-xl bg-white flex items-center justify-center shadow-sm">
                    <TbLogout size={20} />
                  </div>
                  <span className="text-sm font-bold">로그아웃</span>
                </button>
              </div>
            </>
          ) : (
            <div>
              <h3 className="text-[10px] font-black text-gray-400 uppercase tracking-[0.3em] mb-4 pl-1 italic">서비스 안내</h3>
              <div className="space-y-2">
                {navItems.guest.map((item, idx) => (
                  <button
                    key={idx}
                    onClick={() => {
                      const el = document.getElementById(item.path);
                      el?.scrollIntoView({ behavior: 'smooth' });
                      setIsMobileMenuOpen(false);
                    }}
                    className="w-full flex items-center gap-4 p-4 bg-gray-50 rounded-2xl active:scale-[0.98] transition-all group"
                  >
                    <div className="w-10 h-10 rounded-xl bg-white flex items-center justify-center text-[#276db8] shadow-sm">
                      {item.icon}
                    </div>
                    <span className="text-sm font-bold text-gray-700">{item.label}</span>
                  </button>
                ))}
              </div>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}

function NavDropdown({ label, items }: { label: string; items: { label: string, path: string, icon: React.ReactNode }[] }) {
  return (
    <div className="relative group px-3 py-2 cursor-pointer">
      <div className="flex items-center gap-1 text-sm font-bold text-gray-600 group-hover:text-black transition-colors">
        {label}
        <TbChevronDown size={14} className="group-hover:rotate-180 transition-transform duration-300" />
      </div>

      {/* Dropdown Menu */}
      <div className="absolute top-full left-0 pt-2 opacity-0 translate-y-2 pointer-events-none group-hover:opacity-100 group-hover:translate-y-0 group-hover:pointer-events-auto transition-all duration-200 z-50">
        <div className="w-56 bg-white rounded-2xl shadow-xl border border-gray-100 p-2 overflow-hidden">
          {items.map((item, idx) => (
            <Link
              key={idx}
              to={item.path}
              className="flex items-center gap-3 px-4 py-3 text-sm font-semibold text-gray-600 hover:text-[#276db8] hover:bg-gray-50 rounded-xl transition-all"
            >
              <span className="text-gray-400 group-hover:text-[#276db8]">{item.icon}</span>
              {item.label}
            </Link>
          ))}
        </div>
      </div>
    </div>
  );
}
