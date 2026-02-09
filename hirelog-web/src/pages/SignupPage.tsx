import { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { authService, type CheckEmailResponse } from '../services/auth';
import { useAuthStore } from '../store/authStore';

const POSITIONS = [
  { id: 1, name: '백엔드 개발자' },
  { id: 2, name: '프론트엔드 개발자' },
  { id: 3, name: '데이터 엔지니어' },
  { id: 4, name: '기획자 (PM/PO)' },
  { id: 5, name: '프로덕트 디자이너' },
  { id: 6, name: '데브옵스/인프라' },
  { id: 7, name: '머신러닝/AI' },
];

const RESERVED_USERNAMES = new Set([
  "ADMIN", "ROOT", "SYSTEM", "관리자"
]);

const BANNED_WORDS = [
  "씨발", "시발", "좆", "병신", "개새끼", "새끼",
  "미친놈", "미친년",
  "멍청", "등신", "병자", "폐인",
  "장애인", "장애", "정신병", "버러지",
  "fuck", "shit", "bitch", "asshole", "bastard",
  "fucking", "motherfucker", "sibal"
];

const SignupPage = () => {
  const navigate = useNavigate();
  const { checkAuth } = useAuthStore();
  const [searchParams] = useSearchParams();

  // -- State --
  // Email & Verification
  const [email, setEmail] = useState('');
  const [isEmailSent, setIsEmailSent] = useState(false); // Code sent?
  const [isEmailVerified, setIsEmailVerified] = useState(false); // Code verified?
  const [verificationCode, setVerificationCode] = useState('');
  const [existingUser, setExistingUser] = useState<CheckEmailResponse | null>(null); // For duplicate check

  // Profile Form
  const [username, setUsername] = useState('');
  const [currentPositionId, setCurrentPositionId] = useState<number | ''>('');
  const [careerYears, setCareerYears] = useState<string>('');
  const [summary, setSummary] = useState('');
  const [agreeToTerms, setAgreeToTerms] = useState(false);

  // UI Status
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(''); // Global/Process error
  const [fieldErrors, setFieldErrors] = useState<{ email?: string; code?: string; username?: string }>({});

  useEffect(() => {
    const emailParam = searchParams.get('email');
    if (emailParam) {
      setEmail(emailParam);
    }
  }, [searchParams]);

  const validateEmailFormat = (email: string) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);

  const validateUsername = (name: string): string | null => {
    if (!name) return null;
    const upperName = name.toUpperCase();
    if (RESERVED_USERNAMES.has(upperName)) {
      return '사용할 수 없는 닉네임입니다.';
    }
    for (const word of BANNED_WORDS) {
      if (name.includes(word)) {
        return '사용할 수 없는 단어가 포함되어 있습니다.';
      }
    }
    return null;
  };

  // 1. Send Email (Check Duplicate & Send Code)
  const handleRequestVerification = async () => {
    setFieldErrors(prev => ({ ...prev, email: '' }));
    setError('');

    if (!email) {
      setFieldErrors(prev => ({ ...prev, email: '이메일을 입력해주세요.' }));
      return;
    }
    if (!validateEmailFormat(email)) {
      setFieldErrors(prev => ({ ...prev, email: '올바른 이메일 형식이 아닙니다.' }));
      return;
    }

    setLoading(true);
    try {
      const response = await authService.checkEmail({ email });
      setExistingUser(response);

      if (response.exists) {
        // Handle duplicate logic if needed, currently just showing message in render
      } else {
        setIsEmailSent(true);
      }
    } catch (err: any) {
      setError(err.response?.data?.message || '이메일 확인 중 오류가 발생했습니다.');
    } finally {
      setLoading(false);
    }
  };

  // 1-b. Confirm Duplicate Bind -> Send Code
  const handleConfirmBind = async () => {
    setLoading(true);
    try {
      await authService.sendCode({ email });
      setIsEmailSent(true);
    } catch (err: any) {
      setError(err.response?.data?.message || '인증코드 발송 실패');
    } finally {
      setLoading(false);
    }
  };

  // 2. Verify Code
  const handleVerifyCode = async () => {
    setFieldErrors(prev => ({ ...prev, code: '' }));
    if (!verificationCode || verificationCode.length < 6) {
      setFieldErrors(prev => ({ ...prev, code: '6자리 인증코드를 입력해주세요.' }));
      return;
    }

    setLoading(true);
    try {
      await authService.verifyCode({ email, code: verificationCode });
      setIsEmailVerified(true);
    } catch (err: any) {
      setFieldErrors(prev => ({ ...prev, code: '인증번호가 일치하지 않습니다.' }));
    } finally {
      setLoading(false);
    }
  };

  // 3. Reset (Change Email)
  const handleReset = () => {
    setIsEmailVerified(false);
    setIsEmailSent(false);
    setExistingUser(null);
    setVerificationCode('');
  };

  // 4. Complete / Bind
  const handleSubmit = async () => {
    if (!isEmailVerified) return;

    if (existingUser?.exists) {
      // Bind Flow
      setLoading(true);
      try {
        await authService.bind({ email });
        await checkAuth();
        navigate('/');
      } catch (err: any) {
        setError(err.response?.data?.message || '요청 실패');
        setLoading(false);
        handleReset();
      }
    } else {
      // Signup Flow
      setFieldErrors(prev => ({ ...prev, username: '' }));
      if (!username || username.trim().length < 2) {
        setFieldErrors(prev => ({ ...prev, username: '닉네임을 2글자 이상 입력해주세요.' }));
        return;
      }

      const usernameError = validateUsername(username);
      if (usernameError) {
        setFieldErrors(prev => ({ ...prev, username: usernameError }));
        return;
      }

      if (!agreeToTerms) {
        alert('개인정보 수집 및 이용에 동의해주세요.');
        return;
      }

      setLoading(true);
      try {
        await authService.complete({
          email,
          username: username.trim(),
          currentPositionId: currentPositionId ? Number(currentPositionId) : undefined,
          careerYears: careerYears ? Number(careerYears) : undefined,
          summary: summary.trim() || undefined,
        });
        await checkAuth();
        navigate('/');
      } catch (err: any) {
        setError(err.response?.data?.message || '가입 완료 실패');
        setLoading(false);
      }
    }
  };

  return (
    <div className="min-h-screen bg-white font-sans text-gray-900">
      {/* Top Bar removed - handled by Global Header */}

      <div className="flex flex-col items-center pt-10 pb-20 px-4">
        <h1 className="text-3xl font-bold mb-16">회원가입</h1>

        <div className="w-full max-w-md space-y-6">

          {/* Error Message */}
          {error && (
            <div className="p-4 bg-red-50 text-red-600 text-sm rounded-lg text-center">
              {error}
            </div>
          )}

          {/* 1. Email Input */}
          <div className="space-y-2">
            <label className="block text-sm text-gray-500 font-medium">이메일</label>
            <div className="relative">
              <input
                type="text"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                disabled={isEmailSent || isEmailVerified}
                placeholder="email@hirelog.co.kr"
                className={`w-full h-12 px-4 border rounded-lg outline-none transition-colors 
                        ${fieldErrors.email ? 'border-red-500' : 'border-gray-200 focus:border-cyan-400'}
                        ${(isEmailSent || isEmailVerified) ? 'bg-gray-50 text-gray-500' : 'bg-white'}
                      `}
              />
              {/* Status Indicator Dot (Example from design) */}
              {isEmailVerified && <div className="absolute right-4 top-1/2 -translate-y-1/2 w-2 h-2 bg-blue-500 rounded-full"></div>}
            </div>
            {fieldErrors.email && <p className="text-xs text-red-500">{fieldErrors.email}</p>}

            {/* Duplicate Warning & Bind Logic */}
            {existingUser?.exists && !isEmailSent && (
              <div className="mt-2 text-center">
                <p className="text-sm text-gray-600 mb-2">이미 가입된 이메일입니다. 기존 계정과 연동하시겠습니까?</p>
                <button onClick={handleConfirmBind} className="text-sm text-cyan-500 font-bold underline">인증번호 발송</button>
              </div>
            )}

            {/* Initial "Request Verification" Button */}
            {!isEmailSent && !isEmailVerified && !existingUser?.exists && (
              <div className="pt-4 flex justify-center">
                <button
                  onClick={handleRequestVerification}
                  disabled={loading}
                  className="bg-teal-400 hover:bg-teal-500 text-white font-bold py-3 px-10 rounded-full transition-colors disabled:opacity-50"
                >
                  인증요청
                </button>
              </div>
            )}
          </div>

          {/* 2. Verification Code */}
          {isEmailSent && !isEmailVerified && (
            <div className="space-y-2 animate-fade-in-up">
              <label className="block text-sm text-gray-500 font-medium">인증코드</label>
              <input
                type="text"
                value={verificationCode}
                onChange={(e) => setVerificationCode(e.target.value)}
                placeholder="000000"
                maxLength={6}
                className={`w-full h-12 px-4 border rounded-lg outline-none transition-colors text-center tracking-widest
                        ${fieldErrors.code ? 'border-red-500' : 'border-gray-200 focus:border-cyan-400'}
                      `}
              />
              {fieldErrors.code && <p className="text-xs text-red-500">{fieldErrors.code}</p>}

              <div className="pt-4 flex justify-center gap-4">
                <button
                  onClick={handleReset}
                  className="text-gray-400 text-sm hover:text-gray-600 underline"
                >
                  이메일 재입력
                </button>
                <button
                  onClick={handleVerifyCode}
                  disabled={loading}
                  className="bg-cyan-400 hover:bg-cyan-500 text-white font-bold py-3 px-12 rounded-full transition-colors disabled:opacity-50"
                >
                  확인
                </button>
              </div>
            </div>
          )}

          {/* 3. Profile Form (After Verification) */}
          {isEmailVerified && (
            <div className="space-y-6 pt-4 animate-fade-in-up">
              {/* If existing user binding, simplified message */}
              {existingUser?.exists ? (
                <div className="text-center">
                  <p className="text-lg mb-6"><strong>{existingUser.username}</strong> 계정과 연동됩니다.</p>
                  <button
                    onClick={handleSubmit}
                    disabled={loading}
                    className="bg-teal-400 hover:bg-teal-500 text-white font-bold py-3 px-16 rounded-full transition-colors w-full sm:w-auto"
                  >
                    계정 연동 완료
                  </button>
                </div>
              ) : (
                <>
                  {/* Nickname */}
                  <div className="space-y-2">
                    <label className="block text-sm text-gray-500 font-medium">닉네임</label>
                    <input
                      type="text"
                      value={username}
                      onChange={(e) => {
                        setUsername(e.target.value);
                        // Real-time validation optional, or clear error
                        if (fieldErrors.username) setFieldErrors(prev => ({ ...prev, username: '' }));
                      }}
                      placeholder="닉네임을 입력하세요"
                      className={`w-full h-12 px-4 border rounded-lg outline-none transition-colors 
                                        ${fieldErrors.username ? 'border-red-500' : 'border-gray-200 focus:border-cyan-400'}
                                    `}
                    />
                    {fieldErrors.username && <p className="text-xs text-red-500">{fieldErrors.username}</p>}
                  </div>

                  {/* Position & Career Grid */}
                  <div className="grid grid-cols-2 gap-4">
                    <div className="space-y-2">
                      <label className="block text-sm text-gray-500 font-medium">포지션</label>
                      <select
                        value={currentPositionId}
                        onChange={(e) => setCurrentPositionId(e.target.value ? Number(e.target.value) : '')}
                        className="w-full h-12 px-4 border border-gray-200 rounded-lg outline-none focus:border-cyan-400 bg-white text-gray-800"
                      >
                        <option value="">선택해주세요</option>
                        {POSITIONS.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}
                      </select>
                    </div>
                    <div className="space-y-2">
                      <label className="block text-sm text-gray-500 font-medium">경력 (연차)</label>
                      <input
                        type="number"
                        value={careerYears}
                        onChange={(e) => setCareerYears(e.target.value)}
                        placeholder="0"
                        className="w-full h-12 px-4 border border-gray-200 rounded-lg outline-none focus:border-cyan-400"
                      />
                    </div>
                  </div>

                  {/* Self Intro */}
                  <div className="space-y-2">
                    <label className="block text-sm text-gray-500 font-medium">자기소개</label>
                    <textarea
                      value={summary}
                      onChange={(e) => setSummary(e.target.value)}
                      placeholder="간단한 자기소개를 입력해주세요"
                      className="w-full h-32 px-4 py-3 border border-gray-200 rounded-lg outline-none focus:border-cyan-400 resize-none"
                    />
                  </div>

                  {/* Terms */}
                  <div className="flex items-center gap-2">
                    <input
                      type="checkbox"
                      id="terms"
                      checked={agreeToTerms}
                      onChange={(e) => setAgreeToTerms(e.target.checked)}
                      className="w-4 h-4 text-cyan-400 border-gray-300 rounded focus:ring-cyan-400"
                    />
                    <label htmlFor="terms" className="text-sm text-gray-500 select-none">개인정보 수집 및 이용에 동의합니다.</label>
                  </div>

                  {/* Submit Button */}
                  <div className="pt-6 flex justify-center">
                    <button
                      onClick={handleSubmit}
                      disabled={loading}
                      className="bg-teal-400 hover:bg-teal-500 text-white font-bold py-3 px-16 rounded-full transition-colors disabled:opacity-50"
                    >
                      가입완료
                    </button>
                  </div>
                </>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default SignupPage;
