import { useEffect, useMemo, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { TbBell, TbHome, TbList, TbMenu2, TbSettings, TbUserCircle, TbX } from 'react-icons/tb';
import { useAuthStore } from '../store/authStore';
import { notificationService, type NotificationItem } from '../services/notificationService';

export function Header() {
  const { isAuthenticated, user, logout } = useAuthStore();
  const navigate = useNavigate();
  const location = useLocation();

  const [isMobileOpen, setIsMobileOpen] = useState(false);
  const [isNotificationOpen, setIsNotificationOpen] = useState(false);
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [unreadCount, setUnreadCount] = useState(0);

  const navLinks = useMemo(
    () => [
      { label: '홈', path: '/', icon: <TbHome size={20} />, authOnly: true },
      { label: 'JD 목록', path: '/jd', icon: <TbList size={20} /> },
      { label: '요청 내역', path: '/requests', icon: <TbUserCircle size={20} />, authOnly: true },
    ],
    [],
  );

  const moveToServiceIntro = () => {
    navigate('/service-intro');
  };

  useEffect(() => {
    if (!isAuthenticated) {
      setNotifications([]);
      setUnreadCount(0);
      return;
    }

    const loadNotifications = async () => {
      try {
        const [paged, count] = await Promise.all([
          notificationService.getNotifications(0, 20),
          notificationService.getUnreadCount(),
        ]);
        setNotifications(paged.items || []);
        setUnreadCount(count);
      } catch {
        // ignore
      }
    };

    loadNotifications();
  }, [isAuthenticated]);

  useEffect(() => {
    setIsMobileOpen(false);
    setIsNotificationOpen(false);
  }, [location.pathname]);

  const handleLogout = async () => {
    await logout();
    navigate('/');
  };

  const handleNotificationClick = async (notification: NotificationItem) => {
    if (!notification.isRead) {
      try {
        await notificationService.markAsRead([notification.id]);
        setNotifications((prev) => prev.map((item) => (item.id === notification.id ? { ...item, isRead: true } : item)));
        setUnreadCount((prev) => Math.max(0, prev - 1));
      } catch {
        // ignore
      }
    }

    if (notification.referenceType === 'JOB_SUMMARY' && notification.referenceId) {
      navigate(`/jd/${notification.referenceId}`);
      return;
    }

    if (notification.referenceType === 'USER_REQUEST' && notification.referenceId) {
      navigate(`/requests/${notification.referenceId}`);
    }
  };

  return (
    <header className="fixed z-50 w-full border-b border-gray-100 bg-white/95 backdrop-blur">
      <div className="mx-auto flex h-16 max-w-7xl items-center justify-between px-6">
        <Link to={isAuthenticated ? '/' : '/jd'} className="flex items-center" aria-label="HireLog home">
          <img src="/hirelog_no_bg.png" alt="HireLog" className="h-12 w-auto object-contain sm:h-14" />
        </Link>

        <nav className="hidden items-center gap-8 text-sm font-bold text-gray-500 md:flex">
          {navLinks
            .filter((link) => !link.authOnly || isAuthenticated)
            .map((link) => (
              <Link
                key={link.path}
                to={link.path}
                className={`transition-colors hover:text-[#4CDFD5] ${
                  location.pathname === link.path ? 'border-b-2 border-[#4CDFD5] py-5 text-gray-900' : ''
                }`}
              >
                {link.label}
              </Link>
            ))}

          <button onClick={moveToServiceIntro} className="transition-colors hover:text-[#4CDFD5]">
            서비스 소개
          </button>

          {user?.role === 'ADMIN' && (
            <Link to="/admin" className="font-bold text-[#3FB6B2]">
              관리자
            </Link>
          )}
        </nav>

        <div className="flex items-center gap-4">
          {isAuthenticated ? (
            <>
              <div className="relative">
                <button
                  onClick={() => setIsNotificationOpen((prev) => !prev)}
                  className="relative rounded-xl p-2 text-gray-500 transition hover:bg-gray-50"
                >
                  <TbBell size={22} />
                  {unreadCount > 0 && (
                    <span className="absolute right-1 top-1 flex h-4 min-w-[1rem] items-center justify-center rounded-full bg-rose-500 px-1.5 text-[10px] font-bold text-white">
                      {unreadCount}
                    </span>
                  )}
                </button>

                {isNotificationOpen && (
                  <div className="absolute right-0 mt-3 max-h-[28rem] w-96 overflow-y-auto rounded-2xl border border-gray-100 bg-white shadow-2xl">
                    <div className="border-b border-gray-100 px-4 py-3 text-xs font-semibold uppercase tracking-wider text-gray-500">알림</div>
                    {notifications.length === 0 ? (
                      <div className="p-6 text-center text-sm text-gray-400">새로운 알림이 없습니다.</div>
                    ) : (
                      notifications.map((notification) => (
                        <button
                          key={notification.id}
                          onClick={() => handleNotificationClick(notification)}
                          className={`w-full border-b border-gray-50 px-4 py-3 text-left text-sm transition last:border-none ${
                            notification.isRead
                              ? 'bg-white text-gray-600 hover:bg-gray-50'
                              : 'bg-[#4CDFD5]/5 font-semibold text-gray-800 hover:bg-[#4CDFD5]/10'
                          }`}
                        >
                          <p className="mb-1 line-clamp-1">{notification.title}</p>
                          {notification.message && <p className="line-clamp-2 text-xs font-normal text-gray-500">{notification.message}</p>}
                        </button>
                      ))
                    )}
                  </div>
                )}
              </div>

              <button
                className="flex h-10 w-10 items-center justify-center rounded-full bg-gradient-to-tr from-[#276db8] to-[#4CDFD5] text-sm font-black uppercase text-white"
                onClick={() => navigate('/profile')}
              >
                {user?.username?.charAt(0) || 'U'}
              </button>

              <button onClick={handleLogout} className="text-sm font-bold text-gray-400 hover:text-rose-500">
                로그아웃
              </button>
            </>
          ) : (
            <div className="hidden items-center gap-2 md:flex">
              <Link to="/login" className="rounded-full border border-gray-200 px-4 py-2 text-sm font-semibold text-gray-700 hover:bg-gray-50">
                로그인
              </Link>
              <Link to="/signup" className="rounded-full bg-gray-900 px-4 py-2 text-sm font-semibold text-white hover:bg-black">
                회원가입
              </Link>
            </div>
          )}

          <button className="p-2 text-gray-600 md:hidden" onClick={() => setIsMobileOpen((prev) => !prev)}>
            {isMobileOpen ? <TbX size={26} /> : <TbMenu2 size={26} />}
          </button>
        </div>
      </div>

      {isMobileOpen && (
        <div className="absolute right-6 top-[4.5rem] z-50 w-56 overflow-hidden rounded-2xl border border-gray-100 bg-white/95 shadow-2xl backdrop-blur md:hidden">
          <nav className="flex flex-col space-y-3 px-4 py-4">
            {navLinks
              .filter((link) => !link.authOnly || isAuthenticated)
              .map((link) => (
                <Link
                  key={link.path}
                  to={link.path}
                  onClick={() => setIsMobileOpen(false)}
                  className={`flex items-center gap-2 text-sm font-bold transition-colors ${
                    location.pathname === link.path ? 'text-[#4CDFD5]' : 'text-gray-600 hover:text-[#4CDFD5]'
                  }`}
                >
                  <span className="scale-[0.85]">{link.icon}</span>
                  {link.label}
                </Link>
              ))}

            <button
              onClick={() => {
                setIsMobileOpen(false);
                moveToServiceIntro();
              }}
              className="text-left text-sm font-bold text-gray-600 transition-colors hover:text-[#4CDFD5]"
            >
              서비스 소개
            </button>

            {user?.role === 'ADMIN' && (
              <div className="border-t border-gray-100/50 pt-2">
                <Link
                  to="/admin"
                  onClick={() => setIsMobileOpen(false)}
                  className="flex items-center gap-2 text-sm font-bold text-[#3FB6B2] transition-colors hover:text-[#35A09D]"
                >
                  <TbSettings size={18} />
                  관리자
                </Link>
              </div>
            )}

            {!isAuthenticated && (
              <div className="border-t border-gray-100/50 pt-2">
                <Link
                  to="/login"
                  onClick={() => setIsMobileOpen(false)}
                  className="mb-2 block rounded-lg border border-gray-200 px-3 py-2 text-center text-sm font-semibold text-gray-700"
                >
                  로그인
                </Link>
                <Link
                  to="/signup"
                  onClick={() => setIsMobileOpen(false)}
                  className="block rounded-lg bg-gray-900 px-3 py-2 text-center text-sm font-semibold text-white"
                >
                  회원가입
                </Link>
              </div>
            )}
          </nav>
        </div>
      )}
    </header>
  );
}
