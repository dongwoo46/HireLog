import { useState, useEffect, useRef } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { authService } from '../services/auth';
import { useAuthStore } from '../store/authStore';
import { TbChevronLeft, TbMail, TbShieldCheck, TbLock } from 'react-icons/tb';

const SignupPage = () => {
  const navigate = useNavigate();
  const { checkAuth } = useAuthStore();
  const [searchParams] = useSearchParams();

  const [email, setEmail] = useState('');
  const [verificationCode, setVerificationCode] = useState('');
  const [isEmailSent, setIsEmailSent] = useState(false);
  const [isEmailVerified, setIsEmailVerified] = useState(false);

  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [agreeToTerms, setAgreeToTerms] = useState(false);

  const [loading, setLoading] = useState(false);
  const [fieldErrors, setFieldErrors] = useState<{ email?: string; code?: string; username?: string; password?: string; common?: string }>({});

  const prevEmailRef = useRef(email);

  useEffect(() => {
    const emailParam = searchParams.get('email');
    if (emailParam) setEmail(emailParam);
  }, [searchParams]);

  useEffect(() => {
    if (prevEmailRef.current !== email) {
      setIsEmailVerified(false);
      setIsEmailSent(false);
      setVerificationCode('');
    }
    prevEmailRef.current = email;
  }, [email]);

  const validateEmailFormat = (value: string) =>
    /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);

  const handleRequestVerification = async () => {
    setFieldErrors({});

    if (!validateEmailFormat(email)) {
      setFieldErrors({ email: '올바른 이메일 형식이 아닙니다.' });
      return;
    }

    setLoading(true);
    try {
      const response = await authService.checkGeneralEmail({ email });
      if (response.exists) {
        setFieldErrors({ email: '이미 사용 중인 이메일입니다.' });
        return;
      }
      setIsEmailSent(true);
    } catch {
      setFieldErrors({ common: '인증코드 요청에 실패했습니다.' });
    } finally {
      setLoading(false);
    }
  };

  const handleVerifyCode = async () => {
    setFieldErrors({});

    if (!verificationCode || verificationCode.length < 6) {
      setFieldErrors({ code: '6자리 인증코드를 입력해주세요.' });
      return;
    }

    setLoading(true);
    try {
      await authService.verifyGeneralCode({ email, code: verificationCode });
      setIsEmailVerified(true);
    } catch {
      setFieldErrors({ code: '인증코드가 올바르지 않거나 만료되었습니다.' });
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async () => {
    setFieldErrors({});

    if (!isEmailVerified) {
      setFieldErrors({ common: '이메일 인증을 완료해주세요.' });
      return;
    }
    if (!username.trim()) {
      setFieldErrors({ username: '닉네임을 입력해주세요.' });
      return;
    }
    if (password.length < 8) {
      setFieldErrors({ password: '비밀번호는 8자 이상이어야 합니다.' });
      return;
    }
    if (!agreeToTerms) {
      setFieldErrors({ common: '개인정보 수집 및 이용에 동의해주세요.' });
      return;
    }

    setLoading(true);
    try {
      await authService.completeGeneral({
        email,
        username,
        password
      });
      await checkAuth();
      navigate('/');
    } catch {
      setFieldErrors({ common: '회원가입에 실패했습니다. 입력값을 확인해주세요.' });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#F4F6F8] flex items-center justify-center px-4">
      <div className="w-full max-w-md bg-white rounded-2xl shadow-sm p-10">
        <h1 className="text-xl font-semibold text-gray-900 mb-8 text-center">일반 회원가입</h1>

        <div className="space-y-2 mb-6">
          <label className="text-sm text-gray-500">이메일</label>
          <div className="relative">
            <TbMail className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
            <input
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="w-full pl-10 pb-2 border-b border-gray-300 focus:border-[#4CDFD5] outline-none transition"
            />
          </div>
          {fieldErrors.email && <p className="text-xs text-red-500">{fieldErrors.email}</p>}
        </div>

        {!isEmailSent && !isEmailVerified && (
          <button
            onClick={() => void handleRequestVerification()}
            disabled={loading}
            className="w-full py-2 rounded-full bg-[#4CDFD5] text-white text-sm font-medium hover:opacity-90 transition disabled:opacity-40"
          >
            인증 요청
          </button>
        )}

        {isEmailSent && !isEmailVerified && (
          <div className="space-y-4 mt-6">
            <div className="relative">
              <TbShieldCheck className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
              <input
                value={verificationCode}
                onChange={(e) => setVerificationCode(e.target.value)}
                maxLength={6}
                placeholder="인증코드 6자리"
                className="w-full pl-10 pb-2 border-b border-gray-300 focus:border-[#4CDFD5] outline-none text-center tracking-widest"
              />
            </div>
            {fieldErrors.code && <p className="text-xs text-red-500">{fieldErrors.code}</p>}
            <button
              onClick={() => void handleVerifyCode()}
              disabled={loading}
              className="w-full py-2 rounded-full bg-[#4CDFD5] text-white text-sm font-medium disabled:opacity-40"
            >
              인증코드 확인
            </button>
          </div>
        )}

        {isEmailVerified && (
          <>
            <div className="space-y-2 mt-8">
              <label className="text-sm text-gray-500">닉네임</label>
              <input
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                className="w-full pb-2 border-b border-gray-300 focus:border-[#4CDFD5] outline-none"
              />
              {fieldErrors.username && <p className="text-xs text-red-500">{fieldErrors.username}</p>}
            </div>

            <div className="space-y-2 mt-6">
              <label className="text-sm text-gray-500">비밀번호</label>
              <div className="relative">
                <TbLock className="absolute left-0 top-1/2 -translate-y-1/2 text-gray-400" />
                <input
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className="w-full pl-7 pb-2 border-b border-gray-300 focus:border-[#4CDFD5] outline-none"
                />
              </div>
              {fieldErrors.password && <p className="text-xs text-red-500">{fieldErrors.password}</p>}
            </div>

            <div className="flex items-center gap-2 mt-6">
              <input
                type="checkbox"
                checked={agreeToTerms}
                onChange={(e) => setAgreeToTerms(e.target.checked)}
              />
              <span className="text-xs text-gray-500">개인정보 수집 및 이용에 동의합니다.</span>
            </div>

            <button
              onClick={() => void handleSubmit()}
              disabled={!agreeToTerms || loading}
              className="w-full mt-6 py-3 rounded-full bg-[#4CDFD5] text-white font-semibold hover:opacity-90 disabled:opacity-30 transition"
            >
              {loading ? '처리 중...' : '가입 완료'}
            </button>
          </>
        )}

        {fieldErrors.common && <p className="mt-4 text-xs text-red-500 text-center">{fieldErrors.common}</p>}

        <button
          onClick={() => navigate('/login')}
          className="mt-8 text-xs text-gray-400 flex items-center gap-1 mx-auto"
        >
          <TbChevronLeft size={14} />
          로그인으로 돌아가기
        </button>
      </div>
    </div>
  );
};

export default SignupPage;
