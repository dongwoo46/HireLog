import { useState, useEffect } from 'react';
import { useNavigate, useSearchParams, Link } from 'react-router-dom';
import { authService } from '../services/auth';
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

const RecoveryPage = () => {
  const navigate = useNavigate();
  const { checkAuth } = useAuthStore();
  const [searchParams] = useSearchParams();
  
  // -- State --
  const [email, setEmail] = useState('');
  const [isEmailSent, setIsEmailSent] = useState(false);
  const [isEmailVerified, setIsEmailVerified] = useState(false);
  const [verificationCode, setVerificationCode] = useState('');
  
  // Profile Form
  const [username, setUsername] = useState('');
  const [currentPositionId, setCurrentPositionId] = useState<number | ''>('');
  const [careerYears, setCareerYears] = useState<string>('');
  const [summary, setSummary] = useState('');
  
  // UI Status
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [fieldErrors, setFieldErrors] = useState<{email?: string; code?: string; username?: string}>({});

  useEffect(() => {
    const emailParam = searchParams.get('email');
    if (emailParam) {
      setEmail(emailParam);
    }
  }, [searchParams]);

  const validateEmailFormat = (email: string) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);

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
      // For recovery, we send the code using the specific recovery endpoint.
      await authService.sendRecoveryCode({ email });
      setIsEmailSent(true);
    } catch (err: any) {
      setError(err.response?.data?.message || '인증코드 발송 중 오류가 발생했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const handleVerifyCode = async () => {
    setFieldErrors(prev => ({ ...prev, code: '' }));
    if (!verificationCode || verificationCode.length < 6) {
      setFieldErrors(prev => ({ ...prev, code: '6자리 인증코드를 입력해주세요.' }));
      return;
    }

    setLoading(true);
    try {
      await authService.verifyRecoveryCode({ email, code: verificationCode });
      setIsEmailVerified(true);
    } catch (err: any) {
       setFieldErrors(prev => ({ ...prev, code: '인증번호가 일치하지 않습니다.' }));
    } finally {
      setLoading(false);
    }
  };

  const handleReset = () => {
    setIsEmailVerified(false);
    setIsEmailSent(false);
    setVerificationCode('');
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!isEmailVerified) return;

    setFieldErrors(prev => ({ ...prev, username: '' }));
    if (!username || username.trim().length < 2) {
        setFieldErrors(prev => ({ ...prev, username: '내용을 입력해주세요.' }));
        return;
    }

    setLoading(true);
    try {
        await authService.completeRecovery({
            email,
            username: username.trim(),
            currentPositionId: currentPositionId ? Number(currentPositionId) : undefined,
            careerYears: careerYears ? Number(careerYears) : undefined,
            summary: summary.trim() || undefined,
        });
        await checkAuth();
        navigate('/');
    } catch (err: any) {
        setError(err.response?.data?.message || '계정 복구 실패');
        setLoading(false);
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
            계정 복구
          </h2>

          <p className="text-gray-600 text-sm text-center mb-8">
            기존 정보를 입력하여 계정을 복구하고 다시 시작하세요.
          </p>

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
                  disabled={isEmailSent || isEmailVerified}
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
            </div>

            {/* 2. Verification Code Section */}
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
              </div>
            )}
            
            {/* Verified Confirmation */}
            {isEmailVerified && (
                 <div className="p-3 bg-green-50 border border-green-200 rounded-lg flex items-center gap-2 text-green-700 text-sm">
                    <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" /></svg>
                    이메일 인증이 완료되었습니다.
                 </div>
            )}

            {/* 3. Profile Form */}
            {isEmailVerified && (
                <div className="space-y-6 pt-6 border-t border-gray-100 animate-slide-down">
                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">
                        닉네임 <span className="text-red-500">*</span>
                        </label>
                        <input
                        type="text"
                        value={username}
                        onChange={(e) => setUsername(e.target.value)}
                        className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none"
                        placeholder="복구할 계정의 닉네임을 입력하세요"
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

                    <button
                        onClick={handleSubmit}
                        disabled={loading}
                        className="w-full py-4 bg-gradient-to-r from-blue-600 to-purple-600 text-white font-bold rounded-lg hover:shadow-lg transition-all disabled:opacity-50"
                    >
                        {loading ? '처리 중...' : '계정 복구 완료'}
                    </button>

                </div>
            )}

          </div>
        </div>
      </div>
    </div>
  );
};

export default RecoveryPage;
