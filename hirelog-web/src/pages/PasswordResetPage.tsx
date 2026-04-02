import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { authService } from '../services/auth';

const PasswordResetPage = () => {
  const navigate = useNavigate();

  const [email, setEmail] = useState('');
  const [code, setCode] = useState('');
  const [newPassword, setNewPassword] = useState('');

  const [isCodeSent, setIsCodeSent] = useState(false);
  const [isVerified, setIsVerified] = useState(false);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  const clearNotice = () => {
    setMessage('');
    setError('');
  };

  const handleSendCode = async () => {
    clearNotice();
    if (!email) {
      setError('이메일을 입력해주세요.');
      return;
    }

    setLoading(true);
    try {
      await authService.sendPasswordResetCode({ email });
      setIsCodeSent(true);
      setMessage('인증코드를 전송했습니다.');
    } catch (e: any) {
      setError(e?.response?.data?.message ?? '인증코드 전송에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const handleVerifyCode = async () => {
    clearNotice();
    if (!code || code.length !== 6) {
      setError('6자리 인증코드를 입력해주세요.');
      return;
    }

    setLoading(true);
    try {
      await authService.verifyPasswordResetCode({ email, code });
      setIsVerified(true);
      setMessage('이메일 인증이 완료되었습니다.');
    } catch (e: any) {
      setError(e?.response?.data?.message ?? '인증코드 확인에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const handleResetPassword = async () => {
    clearNotice();
    if (newPassword.length < 8) {
      setError('비밀번호는 8자 이상 입력해주세요.');
      return;
    }

    setLoading(true);
    try {
      await authService.resetPassword({ email, newPassword });
      setMessage('비밀번호가 변경되었습니다. 로그인 해주세요.');
      setTimeout(() => navigate('/login'), 800);
    } catch (e: any) {
      setError(e?.response?.data?.message ?? '비밀번호 변경에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#F4F6F8] flex items-center justify-center px-4">
      <div className="w-full max-w-md bg-white rounded-2xl shadow-sm p-10">
        <h1 className="text-xl font-semibold text-gray-900 mb-8 text-center">비밀번호 찾기</h1>

        <div className="space-y-4">
          <div>
            <label className="block text-sm text-gray-600 mb-2">이메일</label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="w-full px-4 py-3 border border-gray-300 rounded-lg outline-none focus:border-[#4CDFD5]"
              placeholder="you@example.com"
              disabled={isVerified}
            />
          </div>

          {!isCodeSent && (
            <button
              onClick={() => void handleSendCode()}
              disabled={loading}
              className="w-full py-3 rounded-full bg-[#4CDFD5] text-white font-semibold hover:opacity-90 disabled:opacity-40 transition"
            >
              인증코드 전송
            </button>
          )}

          {isCodeSent && !isVerified && (
            <>
              <div>
                <label className="block text-sm text-gray-600 mb-2">인증코드</label>
                <input
                  value={code}
                  onChange={(e) => setCode(e.target.value)}
                  maxLength={6}
                  className="w-full px-4 py-3 border border-gray-300 rounded-lg outline-none focus:border-[#4CDFD5] tracking-widest text-center"
                  placeholder="000000"
                />
              </div>
              <button
                onClick={() => void handleVerifyCode()}
                disabled={loading}
                className="w-full py-3 rounded-full bg-[#4CDFD5] text-white font-semibold hover:opacity-90 disabled:opacity-40 transition"
              >
                인증코드 확인
              </button>
            </>
          )}

          {isVerified && (
            <>
              <div>
                <label className="block text-sm text-gray-600 mb-2">새 비밀번호</label>
                <input
                  type="password"
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  className="w-full px-4 py-3 border border-gray-300 rounded-lg outline-none focus:border-[#4CDFD5]"
                  placeholder="8자 이상"
                />
              </div>
              <button
                onClick={() => void handleResetPassword()}
                disabled={loading}
                className="w-full py-3 rounded-full bg-[#4CDFD5] text-white font-semibold hover:opacity-90 disabled:opacity-40 transition"
              >
                비밀번호 변경
              </button>
            </>
          )}
        </div>

        {message && <p className="mt-4 text-sm text-emerald-600">{message}</p>}
        {error && <p className="mt-4 text-sm text-rose-500">{error}</p>}

        <div className="mt-8 text-center">
          <Link to="/login" className="text-sm text-gray-500 hover:text-gray-700">
            로그인으로 돌아가기
          </Link>
        </div>
      </div>
    </div>
  );
};

export default PasswordResetPage;

