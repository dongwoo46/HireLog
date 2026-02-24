import { useState, useEffect, useRef } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { authService, type CheckEmailResponse } from '../services/auth';
import { useAuthStore } from '../store/authStore';
import { toast } from 'react-toastify';
import { 
  TbMail, TbShieldCheck, TbUserCircle, TbBriefcase, TbCalendar, 
  TbFileDescription, TbChevronLeft, TbSend, TbCheck, TbLock
} from 'react-icons/tb';

const POSITIONS = [
  { id: 1, name: '백엔드 개발자' },
  { id: 2, name: '프론트엔드 개발자' },
  { id: 3, name: '데이터 엔지니어' },
  { id: 4, name: '기획자 (PM/PO)' },
  { id: 5, name: '프로덕트 디자이너' },
  { id: 6, name: '데브옵스/인프라' },
  { id: 7, name: '머신러닝/AI' },
];

const RESERVED_USERNAMES = new Set(["ADMIN", "ROOT", "SYSTEM", "관리자"]);
const BANNED_WORDS = ["씨발", "시발", "좆", "병신", "개새끼", "새끼", "미친놈", "미친년", "멍청", "등신", "병자", "폐인", "장애인", "장애", "정신병", "버러지", "fuck", "shit", "bitch", "asshole", "bastard", "fucking", "motherfucker", "sibal"];

const SignupPage = () => {
  const navigate = useNavigate();
  const { checkAuth } = useAuthStore();
  const [searchParams] = useSearchParams();

  // -- State --
  const [email, setEmail] = useState('');
  const [isEmailSent, setIsEmailSent] = useState(false);
  const [isEmailVerified, setIsEmailVerified] = useState(false);
  const [verificationCode, setVerificationCode] = useState('');
  const [existingUser, setExistingUser] = useState<CheckEmailResponse | null>(null);

  const [username, setUsername] = useState('');
  const [currentPositionId, setCurrentPositionId] = useState<number | ''>('');
  const [careerYears, setCareerYears] = useState<string>('');
  const [summary, setSummary] = useState('');
  const [agreeToTerms, setAgreeToTerms] = useState(false);

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [fieldErrors, setFieldErrors] = useState<{ email?: string; code?: string; username?: string }>({});

  // Reset verification when email changes
  const prevEmailRef = useRef(email);
  useEffect(() => {
    if (prevEmailRef.current !== email && (isEmailSent || isEmailVerified)) {
      setIsEmailVerified(false);
      setIsEmailSent(false);
      setVerificationCode('');
      setExistingUser(null);
      setError('');
      setFieldErrors({});
      toast.info('이메일이 변경되어 다시 인증이 필요합니다.');
    }
    prevEmailRef.current = email;
  }, [email, isEmailSent, isEmailVerified]);

  useEffect(() => {
    const emailParam = searchParams.get('email');
    if (emailParam) setEmail(emailParam);
  }, [searchParams]);

  const validateEmailFormat = (email: string) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);

  const validateUsername = (name: string): string | null => {
    if (!name) return null;
    const upperName = name.toUpperCase();
    if (RESERVED_USERNAMES.has(upperName)) return '사용할 수 없는 닉네임입니다.';
    for (const word of BANNED_WORDS) {
      if (name.includes(word)) return '사용할 수 없는 단어가 포함되어 있습니다.';
    }
    return null;
  };

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
      if (!response.exists) {
        setIsEmailSent(true);
        toast.success('인증번호가 발송되었습니다.');
      }
    } catch (err: any) {
      setError(err.response?.data?.message || '이메일 확인 중 오류가 발생했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const handleConfirmBind = async () => {
    setLoading(true);
    try {
      await authService.sendCode({ email });
      setIsEmailSent(true);
      toast.success('기존 계정 연동을 위한 인증번호가 발송되었습니다.');
    } catch (err: any) {
      setError(err.response?.data?.message || '인증코드 발송 실패');
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
      await authService.verifyCode({ email, code: verificationCode });
      setIsEmailVerified(true);
      toast.success('이메일 인증에 성공했습니다.');
    } catch (err: any) {
      setFieldErrors(prev => ({ ...prev, code: '인증번호가 일치하지 않습니다.' }));
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async () => {
    if (!isEmailVerified) return;

    if (existingUser?.exists) {
      setLoading(true);
      try {
        await authService.bind({ email });
        await checkAuth();
        navigate('/');
      } catch (err: any) {
        setError(err.response?.data?.message || '요청 실패');
        setLoading(false);
      }
    } else {
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
        toast.warning('개인정보 수집 및 이용에 동의해주세요.');
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
    <div className="min-h-screen bg-[#F8F9FA] pt-28 pb-20 px-6 font-primary">
      <div className="max-w-2xl mx-auto">
        <button 
          onClick={() => navigate('/login')}
          className="flex items-center gap-2 text-gray-400 hover:text-gray-600 mb-8 transition-colors group"
        >
          <TbChevronLeft size={20} className="group-hover:-translate-x-1 transition-transform" />
          <span className="font-bold">로그인으로 돌아가기</span>
        </button>

        <div className="bg-white rounded-[3rem] shadow-xl border border-gray-100 overflow-hidden">
          <div className="p-10 lg:p-12 border-b border-gray-50 bg-gradient-to-r from-[#0f172a] via-[#1e293b] to-[#0f172a] text-white relative">
            <div className="absolute top-10 right-10 w-32 h-32 bg-[#89cbb6] rounded-full blur-[80px] opacity-20 pointer-events-none" />
            <h1 className="text-4xl font-black mb-3 italic tracking-tight">당신만의 로그북 만들기</h1>
            <p className="text-gray-400 font-medium">전문적인 커리어 기록이 여기서 시작됩니다.</p>
          </div>

          <div className="p-10 lg:p-12 space-y-12">
            
            {/* Global Error Display */}
            {error && (
              <div className="p-6 bg-red-50 border border-red-100 text-red-600 rounded-2xl flex items-center gap-4 animate-in fade-in slide-in-from-top-2 duration-300">
                <div className="w-10 h-10 bg-red-100 rounded-full flex items-center justify-center shrink-0">
                  <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                  </svg>
                </div>
                <p className="font-bold text-sm leading-relaxed">{error}</p>
              </div>
            )}
            
            {/* Step 1: Email Verification */}
            <section className="space-y-6">
              <div className="flex items-center gap-3 mb-2">
                <div className={`w-8 h-8 rounded-lg flex items-center justify-center font-black ${isEmailVerified ? 'bg-[#89cbb6] text-white' : 'bg-gray-100 text-gray-400'}`}>
                  {isEmailVerified ? <TbCheck size={20} /> : '01'}
                </div>
                <h3 className="text-lg font-black italic">이메일 인증</h3>
              </div>

              <div className="space-y-4 max-w-lg">
                <div className="relative group">
                  <div className="absolute left-5 top-1/2 -translate-y-1/2 text-gray-400 group-focus-within:text-[#276db8] transition-colors">
                    <TbMail size={22} />
                  </div>
                  <input
                    type="text"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    disabled={isEmailSent && !isEmailVerified}
                    placeholder="name@hirelog.co.kr"
                    className={`w-full h-16 pl-14 pr-32 bg-gray-50 border rounded-2xl outline-none font-bold transition-all
                      ${fieldErrors.email ? 'border-red-500' : 'border-gray-100 focus:bg-white focus:border-[#276db8] focus:ring-4 focus:ring-[#276db8]/5'}
                      ${isEmailVerified ? 'border-[#89cbb6] bg-[#89cbb6]/5 text-[#276db8]' : ''}
                    `}
                  />
                  {!isEmailSent && !isEmailVerified && !existingUser?.exists && (
                    <button 
                      onClick={handleRequestVerification}
                      disabled={loading || !email}
                      className="absolute right-3 top-1/2 -translate-y-1/2 px-6 py-2.5 bg-[#0f172a] text-white rounded-xl text-xs font-black uppercase tracking-widest hover:bg-slate-900 disabled:opacity-30 transition-all"
                    >
                      인증요청
                    </button>
                  )}
                </div>
                {fieldErrors.email && <p className="text-xs text-red-500 font-bold ml-2 italic">{fieldErrors.email}</p>}

                {/* Confirm Bind UI */}
                {existingUser?.exists && !isEmailSent && (
                  <div className="p-6 bg-blue-50/50 rounded-2xl border border-blue-100/50 flex flex-col sm:flex-row items-center justify-between gap-4">
                    <p className="text-sm font-bold text-gray-600">이미 등록된 이메일입니다. 계정을 연동할까요?</p>
                    <button onClick={handleConfirmBind} className="px-6 py-2.5 bg-[#276db8] text-white rounded-xl text-xs font-black uppercase tracking-widest hover:shadow-lg transition-all">연동 인증</button>
                  </div>
                )}

                {/* Verification Code Input */}
                {isEmailSent && !isEmailVerified && (
                  <div className="space-y-4 animate-in fade-in slide-in-from-top-2 duration-300">
                    <div className="relative group">
                      <div className="absolute left-5 top-1/2 -translate-y-1/2 text-gray-400">
                        <TbShieldCheck size={22} />
                      </div>
                      <input
                        type="text"
                        value={verificationCode}
                        onChange={(e) => setVerificationCode(e.target.value)}
                        placeholder="######"
                        maxLength={6}
                        className={`w-full h-16 pl-14 pr-32 bg-gray-50 border rounded-2xl outline-none font-black text-center tracking-[0.5em] text-xl 
                          ${fieldErrors.code ? 'border-red-500' : 'border-gray-100 focus:border-[#89cbb6]'}
                        `}
                      />
                      <button 
                        onClick={handleVerifyCode}
                        disabled={loading}
                        className="absolute right-3 top-1/2 -translate-y-1/2 px-8 py-2.5 bg-[#89cbb6] text-white rounded-xl text-xs font-black uppercase tracking-widest hover:shadow-lg disabled:opacity-50 transition-all"
                      >
                        확인
                      </button>
                    </div>
                  </div>
                )}
              </div>
            </section>

            {/* Step 2: Information Entry */}
            <section className={`space-y-8 pb-4 transition-all duration-700 ${isEmailVerified ? 'opacity-100 translate-y-0' : 'opacity-30 pointer-events-none translate-y-4'}`}>
              <div className="flex items-center gap-3 mb-6">
                <div className="w-8 h-8 rounded-lg bg-gray-100 text-gray-400 flex items-center justify-center font-black">02</div>
                <h3 className="text-lg font-black italic">기본 정보 입력</h3>
                {!isEmailVerified && <TbLock size={18} className="text-gray-300" />}
              </div>

              {existingUser?.exists ? (
                <div className="p-10 bg-gray-50 rounded-[2.5rem] border border-gray-100 text-center space-y-6">
                  <div className="w-20 h-20 bg-[#276db8] rounded-3xl mx-auto flex items-center justify-center text-white text-3xl font-black shadow-xl rotate-3">
                    {existingUser.username?.charAt(0).toUpperCase()}
                  </div>
                  <div>
                    <h4 className="text-2xl font-black italic mb-1">{existingUser.username}</h4>
                    <p className="text-gray-400 font-medium">기존의 로그북 계정과 연동됩니다.</p>
                  </div>
                  <button
                    onClick={handleSubmit}
                    disabled={loading}
                    className="w-full h-16 bg-[#0f172a] text-white rounded-2xl font-black text-lg hover:shadow-xl shadow-[#0f172a]/10 transition-all"
                  >
                    연동하고 시작하기
                  </button>
                </div>
              ) : (
                <div className="space-y-10">
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
                    {/* Nickname */}
                    <div className="space-y-3">
                      <label className="text-[10px] font-black text-gray-400 uppercase tracking-widest ml-1">닉네임</label>
                      <div className="relative group">
                        <TbUserCircle className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-300 group-focus-within:text-[#276db8] transition-colors" size={20} />
                        <input
                          type="text"
                          value={username}
                          onChange={(e) => setUsername(e.target.value)}
                          placeholder="활동할 이름을 입력하세요"
                          className={`w-full h-14 pl-12 pr-4 bg-gray-50 border rounded-2xl outline-none font-bold transition-all
                            ${fieldErrors.username ? 'border-red-500' : 'border-gray-100 focus:bg-white focus:border-[#276db8] focus:ring-4 focus:ring-[#276db8]/5'}
                          `}
                        />
                      </div>
                      {fieldErrors.username && <p className="text-xs text-red-500 italic font-bold ml-1">{fieldErrors.username}</p>}
                    </div>

                    {/* Position */}
                    <div className="space-y-3">
                      <label className="text-[10px] font-black text-gray-400 uppercase tracking-widest ml-1">희망 직무</label>
                      <div className="relative group">
                        <TbBriefcase className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-300 pointer-events-none" size={20} />
                        <select
                          value={currentPositionId}
                          onChange={(e) => setCurrentPositionId(e.target.value ? Number(e.target.value) : '')}
                          className="w-full h-14 pl-12 pr-10 bg-gray-50 border border-gray-100 rounded-2xl outline-none font-bold appearance-none focus:bg-white focus:border-[#276db8] transition-all"
                        >
                          <option value="">포지션 선택</option>
                          {POSITIONS.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}
                        </select>
                      </div>
                    </div>

                    {/* Career Years */}
                    <div className="space-y-3">
                      <label className="text-[10px] font-black text-gray-400 uppercase tracking-widest ml-1">경력 연차</label>
                      <div className="relative group">
                        <TbCalendar className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-300" size={20} />
                        <input
                          type="number"
                          value={careerYears}
                          onChange={(e) => setCareerYears(e.target.value)}
                          placeholder="0 연차"
                          className="w-full h-14 pl-12 pr-4 bg-gray-50 border border-gray-100 rounded-2xl outline-none font-bold focus:bg-white focus:border-[#276db8] transition-all"
                        />
                      </div>
                    </div>
                  </div>

                  {/* Summary */}
                  <div className="space-y-3">
                    <label className="text-[10px] font-black text-gray-400 uppercase tracking-widest ml-1">프로페셔널 한 줄 요약</label>
                    <div className="relative group">
                      <TbFileDescription className="absolute left-5 top-5 text-gray-300" size={20} />
                      <textarea
                        value={summary}
                        onChange={(e) => setSummary(e.target.value)}
                        placeholder="자신만의 차별화된 커리어 브랜딩 한 줄을 작성해 보세요."
                        className="w-full h-32 pl-14 pr-6 py-5 bg-gray-50 border border-gray-100 rounded-[2rem] outline-none font-medium resize-none focus:bg-white focus:border-[#276db8] transition-all"
                      />
                    </div>
                  </div>

                  {/* Terms & Submit */}
                  <div className="pt-6 space-y-8">
                    <label className="flex items-center gap-4 cursor-pointer group">
                      <div className="relative">
                        <input
                          type="checkbox"
                          checked={agreeToTerms}
                          onChange={(e) => setAgreeToTerms(e.target.checked)}
                          className="sr-only"
                        />
                        <div className={`w-6 h-6 rounded-lg border-2 flex items-center justify-center transition-all ${agreeToTerms ? 'bg-gray-900 border-gray-900' : 'bg-white border-gray-200 group-hover:border-gray-400'}`}>
                          {agreeToTerms && <TbCheck size={16} className="text-white" />}
                        </div>
                      </div>
                      <span className="text-sm font-bold text-gray-500 group-hover:text-gray-900 transition-colors">개인정보 수집 및 이용에 동의합니다. (필수)</span>
                    </label>

                    <button
                      onClick={handleSubmit}
                      disabled={loading || !agreeToTerms}
                      className="w-full h-20 bg-gradient-to-r from-[#0f172a] to-[#1e293b] text-white rounded-3xl font-black text-xl hover:scale-[1.02] shadow-2xl shadow-[#0f172a]/20 active:scale-95 transition-all disabled:opacity-30 disabled:scale-100 flex items-center justify-center gap-3 italic"
                    >
                      <TbSend size={24} />
                      회원가입 완료
                    </button>
                  </div>
                </div>
              )}
            </section>
          </div>
        </div>
      </div>
    </div>
  );
};

export default SignupPage;
