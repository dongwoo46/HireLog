import { useState, useEffect } from 'react';
import { useNavigate, useSearchParams, Link } from 'react-router-dom';
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
  
  // UI Status
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(''); // Global/Process error
  const [fieldErrors, setFieldErrors] = useState<{email?: string; code?: string; username?: string}>({});

  useEffect(() => {
    const emailParam = searchParams.get('email');
    if (emailParam) {
      setEmail(emailParam);
    }
  }, [searchParams]);

  const validateEmailFormat = (email: string) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);

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
      
      // If duplicate, we still 'send code' but user enters a specific "Duplicate Found" state technically,
      // but to keep it inline, we might just show the code input AND a message saying "Account exists".
      // Let's handle the "Bind" scenario slightly differently or inline.
      // User said: "만약 이메일 중복이면... 기존 이메일과 합치겠냐 요청을 하고... 확인하면 인증코드 발송"
      
      if (response.exists) {
        // We will pause here and show a "Duplicate" UI inline? 
        // Or we can just set a state `showDuplicatePrompt` and let the user click "Yes" to actually send code.
        // For simplicity and matching the request:
        // "이메일 중복이면 사용자에게... 요청을 하고 그것을 확인하면 이메일 인증코드 발송"
        // So we won't set `isEmailSent` yet.
      } else {
        // New user -> Code automtically sent? Actually backend might send it only on `send-code`?
        // Wait, `checkEmail` doesn't send code in `SignupController`? 
        // `checkEmail` returns boolean. If false (not exist), we might need to call `send-code`?
        // Ah, previous logic was: `checkEmail` DOES NOT send code. `sendCode` sends code.
        // BUT wait, `checkEmail` doc says: "중복 아님: 인증코드 발송 후 exists=false 반환"
        // So `checkEmail` DOES send code if new.
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
      // We keep `existingUser` populated so we know it's a bind later
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
      // Code section disappears or becomes read only? User said: "인증하고나면 이메일쪽은 수정불가가되고"
      // Verification section can hide or show "Verified" status.
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
    // Email input becomes editable again
  };

  // 4. Complete / Bind
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
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
            handleReset(); // Reset to allow re-verification
        }
    } else {
        // Signup Flow
        setFieldErrors(prev => ({ ...prev, username: '' }));
        if (!username || username.trim().length < 2) {
            setFieldErrors(prev => ({ ...prev, username: '내용을 입력해주세요.' }));
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
    <div className="min-h-screen bg-slate-900 flex items-center justify-center p-4">
      <div className="w-full max-w-lg bg-white rounded-2xl shadow-xl overflow-hidden animate-fade-in">
        <div className="p-8">
          <Link to="/" className="block w-12 h-12 bg-gradient-to-tr from-blue-500 to-purple-600 rounded-lg flex items-center justify-center font-bold text-white text-xl mx-auto mb-6 hover:opacity-80 transition-opacity">
            H
          </Link>
          
          <h2 className="text-2xl font-bold text-gray-800 text-center mb-8">
            {existingUser?.exists && isEmailVerified ? '계정 연동하기' : '회원가입'}
          </h2>

          {error && (
            <div className="mb-6 p-3 bg-red-50 border border-red-200 rounded-lg text-red-700 text-sm text-center">
              {error}
            </div>
          )}

          <div className="space-y-6">
            
            {/* 1. Email Input Section */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                이메일 <span className="text-red-500">*</span>
              </label>
              <div className="flex gap-2">
                <input
                  type="email"
                  value={email}
                  disabled={isEmailSent || isEmailVerified} // Locked if sent or verified
                  onChange={(e) => setEmail(e.target.value)}
                  className={`flex-1 px-4 py-3 border rounded-lg outline-none transition-all ${
                    isEmailVerified 
                      ? 'bg-gray-50 text-gray-500 border-gray-200' 
                      : 'bg-white border-gray-300 focus:ring-2 focus:ring-blue-500'
                  }`}
                  placeholder="name@example.com"
                />
                
                {isEmailVerified ? (
                  <button
                    type="button"
                    onClick={handleReset}
                    className="px-4 py-2 text-sm font-medium text-gray-600 bg-gray-100 rounded-lg hover:bg-gray-200 transition-colors"
                  >
                    변경
                  </button>
                ) : (
                    !isEmailSent && (
                        <button
                        type="button"
                        onClick={handleRequestVerification}
                        disabled={loading}
                        className="px-6 py-2 bg-blue-600 text-white font-medium rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-50 whitespace-nowrap"
                        >
                        인증요청
                        </button>
                    )
                )}
              </div>
              {fieldErrors.email && <p className="mt-1 text-sm text-red-600">{fieldErrors.email}</p>}
              
              {/* Duplicate Warning */}
              {existingUser?.exists && !isEmailSent && (
                 <div className="mt-3 p-3 bg-blue-50 border border-blue-100 rounded-lg">
                    <p className="text-sm text-blue-800 mb-2">
                        이미 가입된 계정(<strong>{existingUser.username}</strong>)이 있습니다.<br/>
                        기존 계정과 연동하시겠습니까?
                    </p>
                    <button
                        onClick={handleConfirmBind}
                        disabled={loading}
                        className="text-sm font-bold text-blue-700 underline hover:text-blue-900"
                    >
                        네, 인증번호를 보내주세요
                    </button>
                 </div>
              )}
            </div>

            {/* 2. Verification Code Section (Visible only when sent and NOT verified) */}
            {isEmailSent && !isEmailVerified && (
              <div className="animate-slide-down">
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  인증코드
                </label>
                <div className="flex gap-2">
                  <input
                    type="text"
                    maxLength={6}
                    value={verificationCode}
                    onChange={(e) => setVerificationCode(e.target.value)}
                    className="flex-1 px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none uppercase tracking-widest text-center"
                    placeholder="000000"
                  />
                  <button
                    type="button"
                    onClick={handleVerifyCode}
                    disabled={loading}
                    className="px-6 py-2 bg-blue-600 text-white font-medium rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-50 whitespace-nowrap"
                  >
                    확인
                  </button>
                </div>
                {fieldErrors.code && <p className="mt-1 text-sm text-red-600">{fieldErrors.code}</p>}
                
                <div className="mt-2 text-right">
                    <button onClick={handleReset} className="text-xs text-gray-500 underline">
                        이메일 재입력
                    </button>
                </div>
              </div>
            )}
            
            {/* Verified Confirmation */}
            {isEmailVerified && (
                 <div className="p-3 bg-green-50 border border-green-200 rounded-lg flex items-center gap-2 text-green-700 text-sm">
                    <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" /></svg>
                    이메일 인증이 완료되었습니다.
                 </div>
            )}

            {/* 3. Profile Form (Visible only after verification) */}
            {isEmailVerified && (
                <div className="space-y-6 pt-6 border-t border-gray-100 animate-slide-down">
                    {existingUser?.exists ? (
                         <div className="text-center py-4">
                            <p className="text-gray-600 mb-4">
                                <strong>{existingUser.username}</strong> 계정으로 연동됩니다.
                            </p>
                         </div>
                    ) : (
                        <>
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">
                                닉네임 <span className="text-red-500">*</span>
                                </label>
                                <input
                                type="text"
                                value={username}
                                onChange={(e) => setUsername(e.target.value)}
                                className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none"
                                placeholder="사용하실 닉네임을 입력하세요"
                                maxLength={50}
                                />
                                {fieldErrors.username && <p className="mt-1 text-sm text-red-600">{fieldErrors.username}</p>}
                            </div>

                            <div className="grid grid-cols-2 gap-4">
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">포지션</label>
                                    <select
                                        value={currentPositionId}
                                        onChange={(e) => setCurrentPositionId(e.target.value ? Number(e.target.value) : '')}
                                        className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none bg-white font-medium text-gray-700"
                                    >
                                        <option value="">선택해주세요</option>
                                        {POSITIONS.map((pos) => (
                                            <option key={pos.id} value={pos.id}>
                                                {pos.name}
                                            </option>
                                        ))}
                                    </select>
                                </div>
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">경력 (연차)</label>
                                    <input
                                        type="number"
                                        value={careerYears}
                                        onChange={(e) => setCareerYears(e.target.value)}
                                        className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none"
                                        placeholder="0"
                                    />
                                </div>
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">자기소개</label>
                                <textarea
                                    value={summary}
                                    onChange={(e) => setSummary(e.target.value)}
                                    rows={3}
                                    className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none resize-none"
                                    placeholder="간단한 자기소개를 입력해주세요"
                                />
                            </div>
                        </>
                    )}

                    <button
                        onClick={handleSubmit}
                        disabled={loading}
                        className="w-full py-4 bg-gradient-to-r from-blue-600 to-purple-600 text-white font-bold rounded-lg hover:shadow-lg transition-all disabled:opacity-50"
                    >
                        {loading ? '처리 중...' : (existingUser?.exists ? '계정 연동 완료' : '회원가입 완료')}
                    </button>

                </div>
            )}

          </div>
        </div>
      </div>
    </div>
  );
};

export default SignupPage;
