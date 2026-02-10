import { useNavigate } from 'react-router-dom';
import { JobSummarySearch } from '../components/JobSummarySearch';
import { JobSummaryCard } from '../components/JobSummaryCard';
import type { JobSummarySearchReq, JobSummaryView } from '../types/jobSummary';
import { useEffect, useState } from 'react';
import { jdSummaryService } from '../services/jdSummaryService';
import { TbPlus } from 'react-icons/tb';
import { useAuthStore } from '../store/authStore';
import { GuestLanding } from '../components/GuestLanding';

const MainPage = () => {
  const navigate = useNavigate();
  const [featuredJds, setFeaturedJds] = useState<JobSummaryView[]>([]);
  const { isInitialized, isAuthenticated } = useAuthStore();

  useEffect(() => {
    if (!isInitialized) return;

    const loadFeatured = async () => {
      try {
        const result = await jdSummaryService.search({ size: 3, sortBy: 'CREATED_AT_DESC' });
        setFeaturedJds(result?.items || []);
      } catch (error) {
        console.error('Failed to load featured JDs', error);
      }
    };
    loadFeatured();
  }, [isInitialized]);

  const handleSearch = (params: JobSummarySearchReq) => {
    const query = new URLSearchParams();
    if (params.keyword) query.append('keyword', params.keyword);
    if (params.careerType) query.append('careerType', params.careerType);
    navigate(`/jd?${query.toString()}`);
  };

  if (!isInitialized) return null;

  if (!isAuthenticated) {
    return <GuestLanding />;
  }

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

            <JobSummarySearch onSearch={handleSearch} />
          </div>
        </div>

        {/* Decorative elements */}
        <div className="absolute top-1/2 left-0 -translate-y-1/2 w-64 h-64 bg-[#276db8]/5 rounded-full blur-[120px] pointer-events-none" />
        <div className="absolute bottom-0 right-0 w-96 h-96 bg-[#89cbb6]/5 rounded-full blur-[150px] pointer-events-none" />
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
              전체 기록 보기
              <div className="w-8 h-8 rounded-full border border-gray-200 flex items-center justify-center group-hover:border-[#89cbb6] group-hover:bg-[#89cbb6]/5 transition-all">
                →
              </div>
            </button>
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
