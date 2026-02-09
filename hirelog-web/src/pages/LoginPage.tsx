import { FaStar } from 'react-icons/fa';

const LoginPage = () => {
  const handleLogin = (provider: 'google' | 'kakao') => {
    // Redirect to backend OAuth endpoint
    window.location.href = `http://localhost:8080/oauth2/authorization/${provider}`;
  };

  return (
    <div className="flex min-h-screen font-sans">
      {/* Left Side */}
      <div className="hidden lg:flex w-1/2 bg-gray-100 flex-col items-center justify-center relative">
        {/* Logo removed - handled by Global Header */}
        <h1 className="text-4xl font-bold text-black tracking-tight">
          리뷰 돌아가게 할거임 .
        </h1>
      </div>

      {/* Right Side */}
      <div className="w-full lg:w-1/2 bg-white flex flex-col justify-center items-center px-8">
        <div className="w-full max-w-sm flex flex-col items-center">
          {/* Star Icon */}
          <div className="mb-12">
            <FaStar className="w-24 h-24 text-cyan-300" />
          </div>

          {/* Divider */}
          <div className="relative w-full flex items-center justify-center mb-8">
            <div className="absolute inset-0 flex items-center">
              <div className="w-full border-t border-gray-200"></div>
            </div>
            <span className="relative bg-white px-4 text-gray-500 text-sm">
              소셜 계정으로 간편로그인
            </span>
          </div>

          {/* Buttons */}
          <div className="w-full space-y-4">
            {/* Google Button - White with Border */}
            <button
              onClick={() => handleLogin('google')}
              className="w-full h-14 flex items-center justify-center border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors bg-white group"
            >
              {/* Reusing the SVG from previous code as an icon, but keeping it simple/centered as per the 'box' look */}
              <svg className="w-6 h-6" viewBox="0 0 24 24">
                <path
                  fill="#4285F4"
                  d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
                />
                <path
                  fill="#34A853"
                  d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
                />
                <path
                  fill="#FBBC05"
                  d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"
                />
                <path
                  fill="#EA4335"
                  d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"
                />
              </svg>
            </button>

            {/* Kakao Button - Yellow */}
            <button
              onClick={() => handleLogin('kakao')}
              className="w-full h-14 flex items-center justify-center rounded-lg hover:opacity-90 transition-colors bg-[#FFFF00]" // Using bright yellow as per image
            >
              <svg className="w-6 h-6 text-[#3C1E1E]" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12 3C5.9 3 1 6.9 1 11.8c0 3.2 2.1 6 5.3 7.6-.2.8-1.4 5.3-1.6 6 1.9-1.4 7-4.7 7.7-5.1.5.1 1.1.1 1.6.1 6.1 0 11-3.9 11-8.8S16.1 3 12 3z" />
              </svg>
            </button>
          </div>

          {/* Footer */}
          <div className="w-full mt-8 pt-6 border-t border-gray-200 text-center">
            <span className="text-gray-500 text-sm">아직 회원이 아니신가요? </span>
            <a href="/signup" className="text-blue-500 text-sm font-medium hover:underline ml-2">회원가입</a>
          </div>
        </div>
      </div>
    </div>
  );
};

export default LoginPage;
