import { useState, useEffect } from 'react';
import { useNavigate, useSearchParams, Link } from 'react-router-dom';
import { authService, type CheckEmailResponse } from '../services/auth';

const SignupPage = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [email, setEmail] = useState('');
  const [existingUser, setExistingUser] = useState<CheckEmailResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [checkingEmail, setCheckingEmail] = useState(false);
  const [error, setError] = useState('');

  // Complete signup form fields
  const [username, setUsername] = useState('');
  const [currentPositionId, setCurrentPositionId] = useState<string>('');
  const [careerYears, setCareerYears] = useState<string>('');
  const [summary, setSummary] = useState('');

  // Email validation
  const [emailError, setEmailError] = useState('');

  useEffect(() => {
    // Try to get email from URL params (if backend redirects with it)
    const emailParam = searchParams.get('email');
    if (emailParam) {
      setEmail(emailParam);
      checkEmailAvailability(emailParam);
    }
  }, [searchParams]);

  const validateEmail = (email: string): boolean => {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!email) {
      setEmailError('이메일은 필수 입력 항목입니다.');
      return false;
    }
    if (!emailRegex.test(email)) {
      setEmailError('유효한 이메일 형식이 아닙니다.');
      return false;
    }
    setEmailError('');
    return true;
  };

  const checkEmailAvailability = async (emailToCheck: string) => {
    if (!validateEmail(emailToCheck)) return;

    setError('');
    setCheckingEmail(true);

    try {
      const response = await authService.checkEmail({ email: emailToCheck });
      setExistingUser(response);
    } catch (err: any) {
      setError(err.response?.data?.message || '이메일 확인 중 오류가 발생했습니다.');
    } finally {
      setCheckingEmail(false);
    }
  };

  const handleBind = async () => {
    setError('');
    setLoading(true);

    try {
      await authService.bind({ email });
      navigate('/');
    } catch (err: any) {
      setError(err.response?.data?.message || '계정 연동 중 오류가 발생했습니다.');
      setLoading(false);
    }
  };

  const handleComplete = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    // Validate email first
    if (!validateEmail(email)) {
      return;
    }

    // Validate username
    if (!username || username.trim().length < 2) {
      setError('닉네임은 2자 이상 50자 이하로 입력해주세요.');
      return;
    }
    if (username.length > 50) {
      setError('닉네임은 2자 이상 50자 이하로 입력해주세요.');
      return;
    }

    // Validate careerYears
    if (careerYears && Number(careerYears) < 0) {
      setError('경력 연차는 음수일 수 없습니다.');
      return;
    }

    // Validate summary
    if (summary && summary.length > 1000) {
      setError('자기소개는 1000자 이내로 입력해주세요.');
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

      // ✅ 백엔드에서 200 OK가 오면 브라우저가 여기서 직접 페이지를 이동시킵니다.
      // 메인 페이지 혹은 대시보드로 이동
      navigate('/'); 
      
    } catch (err: any) {
      // ✅ 여기서 '유효하지 않거나 만료된 가입 세션입니다.' 에러가 잡힐 거예요.
      setError(err.response?.data?.message || '회원가입 중 오류가 발생했습니다.');
      setLoading(false);
    }
  };

  // Show bind UI if existing user found
  if (existingUser?.exists) {
    return (
      <div className="min-h-screen bg-slate-900 flex items-center justify-center p-4">
        <div className="w-full max-w-md bg-white rounded-2xl shadow-xl overflow-hidden">
          <div className="p-8">
            <Link to="/" className="block w-12 h-12 bg-gradient-to-tr from-blue-500 to-purple-600 rounded-lg flex items-center justify-center font-bold text-white text-xl mx-auto mb-4 hover:opacity-80 transition-opacity">
              H
            </Link>
            <h2 className="text-2xl font-bold text-gray-800 text-center mb-2">계정 연동</h2>
            <p className="text-gray-500 text-center mb-8">기존 계정을 찾았습니다</p>

            {error && (
              <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg text-red-700 text-sm">
                {error}
              </div>
            )}

            <div className="p-4 bg-blue-50 border border-blue-200 rounded-lg mb-6">
              <p className="text-sm text-gray-700 mb-2">
                이미 <strong>{existingUser.username}</strong> 계정이 존재합니다.
              </p>
              <p className="text-sm text-gray-600">
                이 계정과 연동하시겠습니까?
              </p>
            </div>

            <button
              onClick={handleBind}
              disabled={loading}
              className="w-full py-3 bg-gradient-to-r from-blue-600 to-purple-600 text-white font-medium rounded-lg hover:shadow-lg transition-all disabled:opacity-50"
            >
              {loading ? '연동 중...' : '계정 연동하기'}
            </button>
          </div>

          <div className="bg-gray-50 px-8 py-4 border-t border-gray-100 text-center">
            <p className="text-sm text-gray-500">
              Already have an account? <a href="/login" className="text-blue-600 hover:underline">Log in</a>
            </p>
          </div>
        </div>
      </div>
    );
  }

  // Check if required fields are complete (without modifying state)
  const isEmailFormatValid = (emailToCheck: string): boolean => {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return !!emailToCheck && emailRegex.test(emailToCheck);
  };

  const isEmailValid = email && !emailError && isEmailFormatValid(email);
  const isUsernameValid = username.trim().length >= 2 && username.length <= 50;
  const requiredFieldsComplete = isEmailValid && isUsernameValid;

  // Show signup form
  return (
    <div className="min-h-screen bg-slate-900 flex items-center justify-center p-4">
      <div className="w-full max-w-md bg-white rounded-2xl shadow-xl overflow-hidden">
        <div className="p-8">
          <Link to="/" className="block w-12 h-12 bg-gradient-to-tr from-blue-500 to-purple-600 rounded-lg flex items-center justify-center font-bold text-white text-xl mx-auto mb-4 hover:opacity-80 transition-opacity">
            H
          </Link>
          <h2 className="text-2xl font-bold text-gray-800 text-center mb-2">프로필 완성하기</h2>
          <p className="text-gray-500 text-center mb-8">거의 다 왔습니다!</p>

          {error && (
            <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg text-red-700 text-sm">
              {error}
            </div>
          )}

          {checkingEmail && (
            <div className="mb-4 p-3 bg-blue-50 border border-blue-200 rounded-lg text-blue-700 text-sm">
              이메일 확인 중...
            </div>
          )}

          <form onSubmit={handleComplete} className="space-y-4">
            {/* Required Fields */}
            <div className="space-y-4">
              <div>
                <label htmlFor="email" className="block text-sm font-medium text-gray-700 mb-1">
                  Email <span className="text-red-500">*</span>
                </label>
                <input
                  id="email"
                  type="email"
                  value={email}
                  onChange={(e) => {
                    setEmail(e.target.value);
                    setEmailError('');
                    setExistingUser(null);
                  }}
                  onBlur={(e) => {
                    if (e.target.value) {
                      checkEmailAvailability(e.target.value);
                    }
                  }}
                  required
                  className={`w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                    emailError ? 'border-red-300' : 'border-gray-300'
                  }`}
                  placeholder="your@email.com"
                />
                {emailError && (
                  <p className="mt-1 text-sm text-red-600">{emailError}</p>
                )}
              </div>

              <div>
                <label htmlFor="username" className="block text-sm font-medium text-gray-700 mb-1">
                  닉네임 <span className="text-red-500">*</span>
                </label>
                <input
                  id="username"
                  type="text"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  required
                  minLength={2}
                  maxLength={50}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                  placeholder="2자 이상 50자 이하"
                />
                <p className="mt-1 text-xs text-gray-500">{username.length}/50</p>
              </div>
            </div>

            {/* Optional Fields - Only show when required fields are complete */}
            {requiredFieldsComplete && (
              <div className="space-y-4 pt-4 border-t border-gray-200">
                <p className="text-sm font-medium text-gray-600 mb-2">추가 정보 (선택사항)</p>
                
                <div>
                  <label htmlFor="positionId" className="block text-sm font-medium text-gray-700 mb-1">
                    현재 포지션 ID
                  </label>
                  <input
                    id="positionId"
                    type="number"
                    value={currentPositionId}
                    onChange={(e) => setCurrentPositionId(e.target.value)}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                    placeholder="선택사항"
                  />
                </div>

                <div>
                  <label htmlFor="careerYears" className="block text-sm font-medium text-gray-700 mb-1">
                    경력 연차
                  </label>
                  <input
                    id="careerYears"
                    type="number"
                    min="0"
                    value={careerYears}
                    onChange={(e) => setCareerYears(e.target.value)}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                    placeholder="선택사항 (0 이상)"
                  />
                </div>

                <div>
                  <label htmlFor="summary" className="block text-sm font-medium text-gray-700 mb-1">
                    자기소개
                  </label>
                  <textarea
                    id="summary"
                    value={summary}
                    onChange={(e) => setSummary(e.target.value)}
                    maxLength={1000}
                    rows={4}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 resize-none"
                    placeholder="자기소개를 입력해주세요 (선택사항)"
                  />
                  <p className="mt-1 text-xs text-gray-500">{summary.length}/1000</p>
                </div>
              </div>
            )}

            <button
              type="submit"
              disabled={!requiredFieldsComplete || loading || checkingEmail}
              className="w-full py-3 bg-gradient-to-r from-blue-600 to-purple-600 text-white font-medium rounded-lg hover:shadow-lg transition-all disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {loading ? '가입 중...' : '가입 완료'}
            </button>

            {!requiredFieldsComplete && (
              <p className="text-xs text-gray-500 text-center">
                필수 항목을 모두 입력해주세요
              </p>
            )}
          </form>
        </div>

        <div className="bg-gray-50 px-8 py-4 border-t border-gray-100 text-center">
          <p className="text-sm text-gray-500">
            Already have an account? <a href="/login" className="text-blue-600 hover:underline">Log in</a>
          </p>
        </div>
      </div>
    </div>
  );
};

export default SignupPage;
