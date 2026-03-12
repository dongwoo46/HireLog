import { useState, useEffect, useRef } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { authService } from '../services/auth';
import { useAuthStore } from '../store/authStore';
import {
  TbChevronLeft,
  TbMail,
  TbShieldCheck
} from 'react-icons/tb';

const SignupPage = () => {
  const navigate = useNavigate();
  const { checkAuth } = useAuthStore();
  const [searchParams] = useSearchParams();

  const [email, setEmail] = useState('');
  const [verificationCode, setVerificationCode] = useState('');
  const [isEmailSent, setIsEmailSent] = useState(false);
  const [isEmailVerified, setIsEmailVerified] = useState(false);

  const [username, setUsername] = useState('');
  const [agreeToTerms, setAgreeToTerms] = useState(false);

  const [, setLoading] = useState(false);
  const [fieldErrors, setFieldErrors] = useState<{ email?: string; code?: string; username?: string }>({});

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

  const validateEmailFormat = (email: string) =>
    /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);

  const handleRequestVerification = async () => {
    if (!validateEmailFormat(email)) {
      setFieldErrors({ email: '올바른 이메일 형식이 아닙니다.' });
      return;
    }

    setLoading(true);
    try {
      const response = await authService.checkEmail({ email });
      if (!response.exists) setIsEmailSent(true);
    } finally {
      setLoading(false);
    }
  };

  const handleVerifyCode = async () => {
    if (!verificationCode || verificationCode.length < 6) {
      setFieldErrors({ code: '6자리 인증코드를 입력해주세요.' });
      return;
    }

    setLoading(true);
    try {
      await authService.verifyCode({ email, code: verificationCode });
      setIsEmailVerified(true);
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async () => {
    if (!isEmailVerified) return;
    if (!agreeToTerms) return;

    setLoading(true);
    try {
      await authService.complete({
        email,
        username
      });
      await checkAuth();
      navigate('/');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#F4F6F8] flex items-center justify-center px-4">
      <div className="w-full max-w-md bg-white rounded-2xl shadow-sm p-10">

        {/* Header */}
        <h1 className="text-xl font-semibold text-gray-900 mb-8 text-center">
          회원가입
        </h1>

        {/* Email */}
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
          {fieldErrors.email && (
            <p className="text-xs text-red-500">{fieldErrors.email}</p>
          )}
        </div>

        {!isEmailSent && !isEmailVerified && (
          <button
            onClick={handleRequestVerification}
            className="w-full py-2 rounded-full bg-[#4CDFD5] text-white text-sm font-medium hover:opacity-90 transition"
          >
            인증 요청
          </button>
        )}

        {/* Code */}
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
            {fieldErrors.code && (
              <p className="text-xs text-red-500">{fieldErrors.code}</p>
            )}
            <button
              onClick={handleVerifyCode}
              className="w-full py-2 rounded-full bg-[#4CDFD5] text-white text-sm font-medium"
            >
              확인
            </button>
          </div>
        )}

        {/* Username */}
        {isEmailVerified && (
          <>
            <div className="space-y-2 mt-8">
              <label className="text-sm text-gray-500">닉네임</label>
              <input
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                className="w-full pb-2 border-b border-gray-300 focus:border-[#4CDFD5] outline-none"
              />
            </div>

            <div className="flex items-center gap-2 mt-6">
              <input
                type="checkbox"
                checked={agreeToTerms}
                onChange={(e) => setAgreeToTerms(e.target.checked)}
              />
              <span className="text-xs text-gray-500">
                개인정보 수집 및 이용에 동의합니다.
              </span>
            </div>

            <button
              onClick={handleSubmit}
              disabled={!agreeToTerms}
              className="w-full mt-6 py-3 rounded-full bg-[#4CDFD5] text-white font-semibold hover:opacity-90 disabled:opacity-30 transition"
            >
              가입 완료
            </button>
          </>
        )}

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
