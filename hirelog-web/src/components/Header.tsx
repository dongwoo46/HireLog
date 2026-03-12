import { useState, useEffect } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';
import {
  TbBell,
  TbMenu2,
  TbX,
  TbHome,
  TbList,
  TbPlus,
  TbUserCircle,
  TbLogout,
  TbSettings
} from 'react-icons/tb';
import { apiClient } from '../utils/apiClient';

interface Notification {
  id: number;
  message: string;
  type: 'APPROVED' | 'REJECTED' | 'INFO';
  isRead: boolean;
  createdAt: string;
}

export function Header() {
  const { isAuthenticated, user, logout } = useAuthStore();
  const navigate = useNavigate();
  const location = useLocation();

  const [isMobileOpen, setIsMobileOpen] = useState(false);
  const [isNotificationOpen, setIsNotificationOpen] = useState(false);
  const [notifications, setNotifications] = useState<Notification[]>([]);

  const unreadCount = notifications.filter(n => !n.isRead).length;

  /* ---------- 알림 로드 ---------- */
  useEffect(() => {
    if (!isAuthenticated) return;

    const load = async () => {
      try {
        const res = await apiClient.get('/notifications');
        if (Array.isArray(res.data)) {
          setNotifications(res.data);
        }
      } catch (err) {
        console.warn('Notification load failed');
      }
    };

    load();
  }, [isAuthenticated]);

  /* ---------- 라우트 변경 시 메뉴 닫기 ---------- */
  useEffect(() => {
    setIsMobileOpen(false);
    setIsNotificationOpen(false);
  }, [location.pathname]);

  /* ---------- 읽음 처리 ---------- */
  const handleRead = async (id: number) => {
    try {
      await apiClient.patch(`/notifications/${id}/read`);
      setNotifications(prev =>
        prev.map(n =>
          n.id === id ? { ...n, isRead: true } : n
        )
      );
    } catch (err) {
      console.error(err);
    }
  };

  const handleLogout = async () => {
    await logout();
    navigate('/');
  };

  const navLinks = [
    { label: '홈', path: '/', icon: <TbHome size={20} /> },
    { label: 'JD 목록', path: '/jd', icon: <TbList size={20} /> },
    { label: 'JD 등록', path: '/jd/request', icon: <TbPlus size={20} /> },
    { label: '사용자 요청', path: '/requests', icon: <TbUserCircle size={20} /> },
  ];

  return (
    <header className="fixed w-full bg-white/80 backdrop-blur-md border-b border-gray-100 z-50">
      <div className="max-w-7xl mx-auto px-6 h-16 flex justify-between items-center">

        {/* 로고 */}
        <Link
          to="/"
          className="text-xl font-black tracking-tighter text-gray-900 flex items-center gap-2"
        >
          <div className="w-8 h-8 bg-gradient-to-tr from-[#276db8] to-[#4CDFD5] rounded-lg" />
          HireLog
        </Link>

        {/* 데스크탑 네비 */}
        <nav className="hidden md:flex items-center gap-8 text-sm font-bold text-gray-500">
          {navLinks.map(link => (
            <Link
              key={link.path}
              to={link.path}
              className={`transition-colors hover:text-[#4CDFD5] ${location.pathname === link.path
                  ? 'text-gray-900 border-b-2 border-[#4CDFD5] py-5'
                  : ''
                }`}
            >
              {link.label}
            </Link>
          ))}

          {user?.role === 'ADMIN' && (
            <Link
              to="/admin"
              className="text-[#3FB6B2] font-bold"
            >
              관리자
            </Link>
          )}
        </nav>

        {/* 오른쪽 */}
        <div className="flex items-center gap-6">

          {isAuthenticated ? (
            <>
              {/* 알림 */}
              <div className="relative">
                <button
                  onClick={() => setIsNotificationOpen(prev => !prev)}
                  className="relative p-2 rounded-xl text-gray-500 hover:bg-gray-50"
                >
                  <TbBell size={22} />
                  {unreadCount > 0 && (
                    <span className="absolute top-1 right-1 bg-rose-500 text-white text-[10px] font-bold px-1.5 h-4 min-w-[1rem] flex items-center justify-center rounded-full">
                      {unreadCount}
                    </span>
                  )}
                </button>

                {isNotificationOpen && (
                  <div className="absolute right-0 mt-3 w-80 bg-white border border-gray-100 rounded-2xl shadow-2xl">
                    {notifications.length === 0 ? (
                      <div className="p-6 text-center text-gray-400 text-sm">
                        새로운 알림이 없습니다.
                      </div>
                    ) : (
                      notifications.map(n => (
                        <div
                          key={n.id}
                          onClick={() => handleRead(n.id)}
                          className={`p-4 text-sm cursor-pointer ${!n.isRead ? 'bg-[#4CDFD5]/5 font-bold' : ''
                            }`}
                        >
                          {n.message}
                        </div>
                      ))
                    )}
                  </div>
                )}
              </div>

              {/* 프로필 */}
              <div
                className="h-10 w-10 rounded-full bg-gradient-to-tr from-[#276db8] to-[#4CDFD5] text-white flex items-center justify-center text-sm font-black cursor-pointer uppercase"
                onClick={() => navigate('/profile')}
              >
                {user?.username?.charAt(0) || 'U'}
              </div>

              <button
                onClick={handleLogout}
                className="text-gray-400 hover:text-rose-500 text-sm font-bold"
              >
                로그아웃
              </button>
            </>
          ) : (
            <Link
              to="/login"
              className="px-6 py-2 bg-gray-900 text-white text-sm font-bold rounded-full"
            >
              로그인
            </Link>
          )}

          {/* 모바일 버튼 */}
          <button
            className="md:hidden p-2 text-gray-600"
            onClick={() => setIsMobileOpen(prev => !prev)}
          >
            {isMobileOpen ? <TbX size={26} /> : <TbMenu2 size={26} />}
          </button>
        </div>
      </div>
    </header>
  );
}
