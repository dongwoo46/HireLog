import { TbBuilding, TbPencil, TbBook, TbLayoutCards, TbQuote } from 'react-icons/tb';

const LoginPage = () => {

  const handleLogin = (provider: 'google' | 'kakao') => {
    // Redirect to backend OAuth endpoint via proxy
    window.location.href = `/oauth2/authorization/${provider}`;
  };

  return (
    <div className="min-h-screen bg-[#F8F9FA] flex flex-col items-center justify-center p-6 font-primary">
      {/* Decorative Elements */}
      <div className="fixed top-0 left-0 w-full h-1 bg-gradient-to-r from-[#276db8] via-[#89cbb6] to-[#276db8]" />
      
      <div className="w-full max-w-[1000px] flex flex-col md:flex-row bg-white rounded-[3rem] shadow-2xl overflow-hidden border border-gray-100 ring-1 ring-gray-200/50">
        
        {/* Left Side: Branding & Trust */}
        <div className="w-full md:w-1/2 p-12 lg:p-16 flex flex-col justify-between bg-[#0f172a] text-white relative overflow-hidden">
          {/* Subtle Background Pattern */}
          <div className="absolute inset-0 opacity-10 pointer-events-none">
            <div className="absolute top-10 right-10 w-64 h-64 border-2 border-white rounded-full translate-x-1/2 -translate-y-1/2" />
            <div className="absolute bottom-20 left-10 w-32 h-32 bg-white rounded-full -translate-x-1/2 blur-3xl opacity-20" />
          </div>

          <div className="relative z-10">
            <div className="flex items-center gap-3 mb-10">
              <div className="w-10 h-10 bg-gradient-to-tr from-[#276db8] to-[#89cbb6] rounded-xl shadow-lg rotate-3" />
              <span className="text-2xl font-black tracking-tighter italic">HireLog</span>
            </div>

            <div className="space-y-6">
              <h1 className="text-5xl font-black tracking-tight leading-none italic">
                당신의 커리어,<br />
                <span className="text-[#89cbb6]">기록되다.</span>
              </h1>
              <p className="text-gray-400 text-lg leading-relaxed max-w-sm">
                채용은 단순히 지원하는 것이 아닙니다. 당신의 성장을 하나하나 기록하는 과정입니다.
              </p>
            </div>
          </div>

          <div className="relative z-10 pt-10">
            <div className="flex items-center gap-4 p-6 bg-white/5 rounded-3xl border border-white/10 backdrop-blur-sm">
              <div className="w-12 h-12 bg-[#89cbb6] rounded-2xl flex items-center justify-center text-[#0f172a] shadow-xl">
                <TbQuote size={28} />
              </div>
              <div>
                <p className="text-xs font-black uppercase tracking-widest text-[#89cbb6] mb-1">로그 오늘의 한마디</p>
                <p className="text-sm font-medium italic text-gray-300">"미래를 예측하는 가장 좋은 방법은 현재를 기록하는 것이다."</p>
              </div>
            </div>
          </div>
        </div>

        {/* Right Side: Login Action */}
        <div className="w-full md:w-1/2 p-12 lg:p-16 flex flex-col justify-center">
          <div className="w-full max-w-sm mx-auto space-y-10">
            <div className="text-center md:text-left">
              <h2 className="text-3xl font-black tracking-tight text-gray-900 mb-2 italic">다시 만나서 반가워요</h2>
              <p className="text-gray-400 font-medium">소셜 계정으로 로그인하고 기록을 이어나가세요.</p>
            </div>

            <div className="space-y-4">
              {/* Google Button */}
              <button
                onClick={() => handleLogin('google')}
                className="w-full h-16 flex items-center justify-between px-8 bg-white border border-gray-100 rounded-[1.25rem] hover:shadow-xl hover:shadow-gray-200/50 hover:-translate-y-1 transition-all group"
              >
                <div className="flex items-center gap-4">
                  <div className="w-8 h-8 flex items-center justify-center">
                    <svg className="w-6 h-6" viewBox="0 0 24 24">
                      <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" />
                      <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" />
                      <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" />
                      <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" />
                    </svg>
                  </div>
                  <span className="font-black text-gray-700">Google로 시작하기</span>
                </div>
                <div className="text-[10px] font-black uppercase tracking-widest text-[#276db8] opacity-0 group-hover:opacity-100 transition-opacity italic">로그인</div>
              </button>

              {/* Kakao Button */}
              <button
                onClick={() => handleLogin('kakao')}
                className="w-full h-16 flex items-center justify-between px-8 bg-[#FEE500] rounded-[1.25rem] hover:shadow-xl hover:shadow-[#FEE500]/30 hover:-translate-y-1 transition-all group"
              >
                <div className="flex items-center gap-4">
                  <div className="w-8 h-8 flex items-center justify-center text-[#3C1E1E]">
                    <svg className="w-6 h-6" viewBox="0 0 24 24" fill="currentColor">
                      <path d="M12 3C5.9 3 1 6.9 1 11.8c0 3.2 2.1 6 5.3 7.6-.2.8-1.4 5.3-1.6 6 1.9-1.4 7-4.7 7.7-5.1.5.1 1.1.1 1.6.1 6.1 0 11-3.9 11-8.8S16.1 3 12 3z" />
                    </svg>
                  </div>
                  <span className="font-black text-[#3C1E1E]">Kakao로 시작하기</span>
                </div>
                <div className="text-[10px] font-black uppercase tracking-widest text-[#3C1E1E]/50 opacity-0 group-hover:opacity-100 transition-opacity italic">로그인</div>
              </button>
            </div>

            <div className="p-6 bg-blue-50/50 rounded-2xl border border-blue-100/50">
              <div className="flex gap-3">
                <div className="text-[#276db8] pt-0.5">
                  <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor font-black italic">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                </div>
                <div>
                  <p className="text-xs font-bold text-[#276db8] leading-relaxed">
                    별도의 회원가입 절차 없이 소셜 계정으로 첫 로그인을 하시면 자동으로 '로그북'이 생성됩니다.
                  </p>
                </div>
              </div>
            </div>

            <div className="text-center">
              <p className="text-[10px] font-black text-gray-300 uppercase tracking-[0.2em] mb-4">함께하는 전문가들</p>
              <div className="flex justify-center gap-6 text-gray-200">
                <TbBuilding size={20} />
                <TbBook size={20} />
                <TbLayoutCards size={20} />
                <TbPencil size={20} />
              </div>
            </div>
          </div>
        </div>

      </div>
    </div>
  );
};

export default LoginPage;
