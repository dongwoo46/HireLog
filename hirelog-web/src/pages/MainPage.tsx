import { Link } from 'react-router-dom';
import { FaStar } from 'react-icons/fa';

const MainPage = () => {
  return (
    <div className="min-h-screen bg-[#F8F9FA] font-sans relative overflow-hidden flex flex-col justify-between">

      {/* Background Watermark - Adjusted for visibility */}
      <div className="absolute inset-0 flex items-center justify-center pointer-events-none select-none z-0">
        <h1 className="text-[12rem] md:text-[15rem] font-bold text-white leading-none tracking-tighter"
          style={{ textShadow: '0 4px 6px rgba(0,0,0,0.05)', color: '#FFFFFF' }}>
          Hirelog
        </h1>
      </div>

      {/* "Hirelog" text might need to be darker if bg is white, or white if bg is colored. 
          The user image shows white text on very light gray bg. 
          Let's try a very subtle gray text on white bg for the "watermark" effect. 
      */}
      <div className="absolute inset-0 flex items-center justify-center pointer-events-none select-none z-0">
        <h1 className="text-[10rem] md:text-[13rem] font-bold text-gray-100 leading-none tracking-tighter opacity-50">
          Hirelog
        </h1>
      </div>


      {/* Star Icon - Centered Top */}
      <div className="absolute top-12 left-1/2 -translate-x-1/2 z-20">
        <FaStar className="w-16 h-16 text-teal-400" />
      </div>

      {/* Header removed - using global Header */}

      {/* Main Content / Spacer */}
      <div className="relative z-10 flex-grow">
        {/* spacer */}
      </div>

      {/* Bottom Actions - Center Bottom */}
      <div className="relative z-10 w-full flex justify-center gap-6 pb-24">
        <Link
          to="/tools/jd-summary"
          className="px-8 py-3 rounded-full border border-gray-300 bg-white text-teal-600 font-bold hover:bg-gray-50 hover:shadow-md transition-all text-sm"
        >
          JD 등록하기
        </Link>
        <Link
          to="/tools/jd-list"
          className="px-8 py-3 rounded-full border border-gray-300 bg-white text-teal-600 font-bold hover:bg-gray-50 hover:shadow-md transition-all text-sm"
        >
          JD 이력조회
        </Link>
      </div>
    </div>
  );
};

export default MainPage;
