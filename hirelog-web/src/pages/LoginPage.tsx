import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { authService } from '../services/auth';
import { useAuthStore } from '../store/authStore';

const LoginPage = () => {
  const navigate = useNavigate();
  const { checkAuth } = useAuthStore();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSocialLogin = (provider: 'google' | 'kakao') => {
    window.location.href = `/oauth2/authorization/${provider}`;
  };

  const handlePasswordLogin = async () => {
    if (!email || !password) {
      setError('이메일과 비밀번호를 입력해주세요.');
      return;
    }

    setError('');
    setLoading(true);
    try {
      await authService.loginWithPassword({ email, password });
      await checkAuth();
      navigate('/');
    } catch {
      setError('로그인에 실패했습니다. 이메일 또는 비밀번호를 확인해주세요.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#F4F6F8] flex items-center justify-center px-4">
      <div className="w-full max-w-md bg-white rounded-2xl shadow-sm p-10">
        <h1 className="text-xl font-semibold text-gray-900 mb-8 text-center">로그인</h1>

        <div className="space-y-4">
          <div>
            <label className="block text-sm text-gray-600 mb-2">이메일</label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="w-full px-4 py-3 border border-gray-300 rounded-lg outline-none focus:border-[#4CDFD5]"
              placeholder="you@example.com"
            />
          </div>

          <div>
            <label className="block text-sm text-gray-600 mb-2">비밀번호</label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full px-4 py-3 border border-gray-300 rounded-lg outline-none focus:border-[#4CDFD5]"
              placeholder="비밀번호"
              onKeyDown={(e) => {
                if (e.key === 'Enter') {
                  void handlePasswordLogin();
                }
              }}
            />
          </div>
        </div>

        {error && <p className="mt-3 text-sm text-rose-500">{error}</p>}

        <button
          onClick={() => void handlePasswordLogin()}
          disabled={loading}
          className="w-full mt-6 py-3 rounded-full bg-[#4CDFD5] text-white font-semibold hover:opacity-90 disabled:opacity-40 transition"
        >
          {loading ? '로그인 중...' : '일반 로그인'}
        </button>

        <div className="my-6 flex items-center gap-3">
          <div className="h-px flex-1 bg-gray-200" />
          <span className="text-xs text-gray-400">또는</span>
          <div className="h-px flex-1 bg-gray-200" />
        </div>

        <div className="space-y-3">
          <button
            onClick={() => handleSocialLogin('google')}
            className="w-full flex items-center justify-center gap-3 border border-gray-300 rounded-lg py-3 font-medium hover:bg-gray-50 transition"
          >
            Google로 로그인
          </button>
          <button
            onClick={() => handleSocialLogin('kakao')}
            className="w-full flex items-center justify-center gap-3 bg-[#FEE500] text-black rounded-lg py-3 font-medium hover:opacity-90 transition"
          >
            카카오로 로그인
          </button>
        </div>

        <div className="mt-8 text-center text-sm text-gray-500 space-x-3">
          <Link to="/signup" className="hover:text-gray-700">일반 회원가입</Link>
          <Link to="/password-reset" className="hover:text-gray-700">비밀번호 찾기</Link>
          <Link to="/recovery" className="hover:text-gray-700">계정 복구</Link>
        </div>
      </div>
    </div>
  );
};

export default LoginPage;
