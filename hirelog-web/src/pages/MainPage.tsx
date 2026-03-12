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
      try {
        const result = await jdSummaryService.search({ size: 3, sortBy: 'CREATED_AT_DESC' });
        setFeaturedJds(result?.items || []);
      } catch (error) {
        console.error('Failed to load featured JDs', error);
      }
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
    <div className="min-h-screen bg-white">
      {/* Hero Section */}
      <section className="relative pt-40 pb-32 overflow-hidden border-b border-gray-50">
        <div className="max-w-7xl mx-auto px-6 relative z-10">
          <div className="max-w-4xl mx-auto text-center">
            <div className="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-[#89cbb6]/10 border border-[#89cbb6]/20 mb-10 animate-in fade-in slide-in-from-bottom-4 duration-1000">
              <span className="w-2 h-2 rounded-full bg-[#276db8] animate-pulse" />
              <span className="text-[10px] font-black text-[#276db8] uppercase tracking-[0.3em]">스마트 채용 로그북</span>
            </div>

            <h1 className="text-6xl md:text-8xl font-black text-gray-900 leading-[1.0] mb-10 tracking-tighter italic">
              당신의 성장을 <br />
              <span className="mint-gradient-text">기록하세요.</span>
            </h1>

            <p className="text-xl md:text-2xl text-gray-400 mb-16 leading-relaxed font-medium max-w-2xl mx-auto">
              HireLog는 단순한 요약을 넘어, 당신의 성장을 <br />
              기록하고 분석하는 AI 커리어 일지입니다.
            </p>

            {/* 검색바 */}
            <JobSummarySearch
              onSearch={handleSearch}
            />
          </section>

          {/* Featured Entry Section */}
          <section className="py-24 bg-[#F8F9FA]/50">
            <div className="max-w-7xl mx-auto px-6">
              <div className="flex items-end justify-between mb-16">
                <div>
                  <h2 className="text-xs font-black text-[#89cbb6] uppercase tracking-[0.4em] mb-4 italic">Recent Logs</h2>
                  <h3 className="text-4xl font-black text-gray-900 tracking-tight">최근 수집된 채용 기록</h3>
                </div>
                <button
                  onClick={() => navigate('/jd')}
                  className="group flex items-center gap-2 text-sm font-black text-[#276db8] uppercase tracking-widest hover:text-[#89cbb6] transition-colors"
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

                  <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
                    {(featuredJds || []).length > 0 ? (
                      featuredJds.map((jd) => (
                        <JobSummaryCard key={jd.id} summary={jd} />
                      ))
                    ) : (
                      [1, 2, 3].map((i) => (
                        <div key={i} className="bg-white rounded-3xl h-80 border border-gray-100 animate-pulse shadow-log" />
                      ))
                    )}
                  </div>
              </div>
          </section>

          {/* Quick Action Footer */}
          <section className="bg-white border-t border-gray-100 py-32">
            <div className="max-w-7xl mx-auto px-6 text-center">
              <h2 className="text-xs font-black text-gray-400 uppercase tracking-[0.5em] mb-8">도움이 필요하신가요?</h2>
              <h3 className="text-4xl font-black text-gray-900 mb-12 italic uppercase tracking-tight">맞춤형 채용 공고 분석 요청</h3>
              <button
                onClick={() => navigate('/jd/request')}
                className="px-16 py-6 rounded-[24px] bg-[#0f172a] text-white font-bold text-xl shadow-2xl hover:scale-105 transition-all flex items-center gap-4 mx-auto"
              >
                <TbPlus size={24} />
                요청 시작하기
              </button>
            </div>
          </section>
        </div>
        );
};

        export default MainPage;
