import { useEffect, useState } from 'react';

const reviews = [
  "지원했던 기록이 쌓이니까 성장 흐름이 보이기 시작했어요.",
  "AI 요약 덕분에 JD 분석 시간이 줄었어요.",
  "이전 지원 공고를 다시 돌아보는 게 정말 도움됐어요.",
];

const LoginPage = () => {
  const [current, setCurrent] = useState(0);

  useEffect(() => {
    const timer = setInterval(() => {
      setCurrent(prev => (prev + 1) % reviews.length);
    }, 3500);
    return () => clearInterval(timer);
  }, []);

  const handleLogin = (provider: 'google' | 'kakao') => {
    window.location.href = `/oauth2/authorization/${provider}`;
  };

  return (
    <div className="min-h-screen flex bg-[#F3F4F6]">

      {/* LEFT - 리뷰 영역 */}
      <div className="hidden md:flex w-1/2 items-center justify-center bg-[#E5E7EB]">
        <div className="max-w-md px-10 text-center">

          <p className="text-lg text-gray-600 leading-relaxed transition-opacity duration-500">
            “{reviews[current]}”
          </p>

        </div>
      </div>

      {/* RIGHT - 로그인 영역 */}
      <div className="w-full md:w-1/2 flex items-center justify-center bg-white">
        <div className="w-full max-w-sm px-8 py-14 space-y-10 text-center">

          <h1 className="text-2xl font-bold text-gray-900">
            HireLog
          </h1>

          <p className="text-sm text-gray-500">
            소셜 계정으로 간편 로그인
          </p>

          <div className="space-y-4">

            {/* Google */}
            <button
              onClick={() => handleLogin('google')}
              className="w-full flex items-center justify-center gap-3 border border-gray-300 rounded-lg py-3 font-medium hover:bg-gray-50 transition"
            >
              {/* Google Icon */}
              <svg className="w-5 h-5" viewBox="0 0 24 24">
                <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" />
                <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" />
                <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" />
                <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" />
              </svg>
              Google로 시작하기
            </button>

            {/* Kakao */}
            <button
              onClick={() => handleLogin('kakao')}
              className="w-full flex items-center justify-center gap-3 bg-[#FEE500] text-black rounded-lg py-3 font-medium hover:opacity-90 transition"
            >
              {/* Kakao Icon */}
              <svg className="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12 3C5.9 3 1 6.9 1 11.8c0 3.2 2.1 6 5.3 7.6-.2.8-1.4 5.3-1.6 6 1.9-1.4 7-4.7 7.7-5.1.5.1 1.1.1 1.6.1 6.1 0 11-3.9 11-8.8S16.1 3 12 3z" />
              </svg>
              카카오로 시작하기
            </button>

          </div>

        </div>
      </div>

    </div>
  );
};

export default LoginPage;
