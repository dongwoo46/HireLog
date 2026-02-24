import { useNavigate, Link } from 'react-router-dom';
import { useEffect, useState } from 'react';
import { useAuthStore } from '../store/authStore';
import { jdSummaryService } from '../services/jdSummaryService';
import type { JobSummaryView, JobSummarySearchReq } from '../types/jobSummary';
import { JobSummarySearch } from '../components/JobSummarySearch';

const MainPage = () => {
  const navigate = useNavigate();
  const { isInitialized, isAuthenticated } = useAuthStore();

  const [featuredJds, setFeaturedJds] = useState<JobSummaryView[]>([]);

  useEffect(() => {
    if (!isInitialized || !isAuthenticated) return;

    const loadFeatured = async () => {
      const result = await jdSummaryService.search({
        size: 4,
        sortBy: 'CREATED_AT_DESC',
      });
      setFeaturedJds(result?.items || []);
    };

    loadFeatured();
  }, [isInitialized, isAuthenticated]);

  const handleSearch = (params: JobSummarySearchReq) => {
    const nextParams = new URLSearchParams();
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== '') {
        nextParams.set(key, value.toString());
      }
    });

    navigate(`/jd?${nextParams.toString()}`);
  };

  if (!isInitialized) return null;

  /* ===============================
     🔓 로그인 안 된 상태
  =============================== */
  if (!isAuthenticated) {
    return (
      <div className="min-h-screen bg-[#0f172a] text-white">

        {/* HERO */}
        <section className="pt-40 pb-32 px-6 text-center relative overflow-hidden">
          <div className="absolute -top-40 left-1/2 -translate-x-1/2 w-[700px] h-[700px] bg-[#2ec4b6]/20 blur-[150px] rounded-full -z-10" />

          <p className="text-xs tracking-[0.4em] text-gray-400 mb-10">
            SMART CAREER LOGBOOK
          </p>

          <h1 className="text-5xl md:text-6xl font-black leading-tight mb-10">
            당신의 커리어를
            <br />
            <span className="text-[#2ec4b6]">
              기록하세요.
            </span>
          </h1>

          <p className="text-gray-400 max-w-xl mx-auto mb-16">
            데이터 기반 커리어 관리 플랫폼 HireLog
          </p>

          <div className="flex justify-center gap-6">
            <Link
              to="/login"
              className="px-8 py-4 bg-[#2ec4b6] text-black font-bold rounded-2xl shadow-lg shadow-[#2ec4b6]/30 hover:scale-105 transition-all block"
            >
              로그인
            </Link>

            <Link
              to="/signup"
              className="px-8 py-4 border border-white/20 rounded-2xl hover:bg-white/5 transition-all block"
            >
              회원가입
            </Link>
          </div>
        </section>

        {/* 서비스 소개 */}
        <section className="bg-[#0b121a] py-28">
          <div className="max-w-5xl mx-auto px-6 text-center">
            <h3 className="text-3xl font-bold mb-16">
              HireLog와 함께 성장하세요
            </h3>

            <div className="grid md:grid-cols-3 gap-14">
              {[
                { icon: '📊', title: 'JD 자동 분석', desc: 'AI가 핵심을 정리합니다.' },
                { icon: '🧠', title: '전략 수립', desc: '데이터 기반 면접 전략.' },
                { icon: '📈', title: '커리어 자산화', desc: '지원 이력 관리.' },
              ].map((item, idx) => (
                <div key={idx} className="space-y-4">
                  <div className="w-16 h-16 mx-auto bg-[#2ec4b6]/10 rounded-2xl flex items-center justify-center text-2xl">
                    {item.icon}
                  </div>
                  <p className="font-semibold">{item.title}</p>
                  <p className="text-sm text-gray-400">{item.desc}</p>
                </div>
              ))}
            </div>
          </div>
        </section>
      </div>
    );
  }

  /* ===============================
     🔐 로그인 된 상태
  =============================== */
  return (
    <div className="min-h-screen bg-[#f8fafb]">
      {/* HERO + 검색 */}
      <section className="pt-32 pb-24 text-center px-6 relative z-10">
        <p className="text-xs tracking-widest text-gray-400 mb-8">
          SMART CAREER LOGBOOK
        </p>

        <h1 className="text-4xl md:text-5xl font-bold leading-tight mb-8">
          당신의 성장을
          <br />
          <span className="text-[#2ec4b6] block mt-3">
            기록하세요.
          </span>
        </h1>

        <p className="text-gray-500 mb-14 max-w-2xl mx-auto">
          AI 기반 JD 분석 & 커리어 데이터 관리
        </p>

        {/* 검색바 */}
        <JobSummarySearch
          onSearch={handleSearch}
        />
      </section>

      {/* 최근 JD */}
      <section className="max-w-6xl mx-auto px-6 pb-24 relative z-20">
        <div className="flex justify-between items-center mb-8">
          <h2 className="text-xl font-bold text-gray-900">
            최근 수집된 채용 기록
          </h2>
          <button
            onClick={() => navigate('/jd')}
            className="text-[#2ec4b6] font-semibold hover:underline cursor-pointer p-2 flex items-center gap-1 group"
          >
            더보기 <span className="group-hover:translate-x-1 transition-transform">→</span>
          </button>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
          {featuredJds.map((jd) => (
            <div
              key={jd.id}
              onClick={() => navigate(`/jd/${jd.id}`)}
              className="group relative bg-white rounded-[2.5rem] p-10 shadow-sm border border-gray-100/50 hover:shadow-2xl hover:shadow-[#4CDFD5]/15 hover:-translate-y-2 transition-all duration-500 cursor-pointer overflow-hidden active:scale-95"
            >
              <div className="absolute top-0 right-0 p-5 opacity-0 group-hover:opacity-100 transition-all duration-500 transform translate-x-2 group-hover:translate-x-0">
                <div className="w-10 h-10 rounded-2xl bg-[#4CDFD5]/10 flex items-center justify-center text-[#4CDFD5] shadow-inner">
                  <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2 / 5} d="M9 5l7 7-7 7" />
                  </svg>
                </div>
              </div>

              <div className="mb-8">
                <h3 className="font-extrabold text-gray-900 text-xl leading-tight mb-2 group-hover:text-[#276db8] transition-colors line-clamp-1">
                  {jd.brandName}
                </h3>
                <p className="text-gray-400 text-sm font-semibold line-clamp-1">
                  {jd.brandPositionName}
                </p>
              </div>

              <div className="pt-7 border-t border-gray-50 flex flex-col gap-4">
                <div className="flex gap-2">
                  <span className="px-3 py-1 bg-[#4CDFD5]/5 text-[#4CDFD5] text-[10px] font-black rounded-lg uppercase tracking-wider">
                    {jd.careerType === 'NEW' ? 'Newbie' : jd.careerType === 'EXPERIENCED' ? 'Expert' : 'Any'}
                  </span>
                  <span className="px-3 py-1 bg-blue-50 text-blue-400 text-[10px] font-black rounded-lg uppercase tracking-wider">
                    요약완료
                  </span>
                </div>
                <div className="flex justify-between items-center">
                  <p className="text-[10px] font-black text-gray-300 uppercase tracking-[0.2em]">
                    {jd.createdAt?.slice(0, 10).replace(/-/g, '.')}
                  </p>
                  <div className="w-1.5 h-1.5 rounded-full bg-[#4CDFD5] opacity-0 group-hover:opacity-100 transition-opacity" />
                </div>
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* 서비스 소개 (로그인 후에도 유지) */}
      <section className="bg-white py-28 relative z-10 border-t border-gray-50">
        <div className="max-w-5xl mx-auto px-6 text-center">
          <h3 className="text-2xl font-black text-gray-900 mb-16 tracking-tight">
            HireLog의 핵심 기능
          </h3>

          <div className="grid md:grid-cols-3 gap-16">
            {[
              { icon: '📊', title: 'JD 자동 분석', desc: 'AI가 핵심 내용을 정교하게 분석합니다' },
              { icon: '🧠', title: '전략 수립', desc: '데이터 기반의 완벽한 취업 전략' },
              { icon: '📈', title: '커리어 자산화', desc: '당신의 이력을 한 곳에서 통합 관리' },
            ].map((item, idx) => (
              <div key={idx} className="group">
                <div className="w-20 h-20 mx-auto bg-gray-50 rounded-[2rem] flex items-center justify-center mb-6 text-3xl group-hover:bg-[#4CDFD5]/10 group-hover:rotate-12 transition-all duration-500">
                  {item.icon}
                </div>
                <p className="font-extrabold text-gray-900 mb-2">{item.title}</p>
                <p className="text-sm text-gray-400 font-medium">{item.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>
    </div>
  );
};

export default MainPage;
