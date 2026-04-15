import { useEffect, useState } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { TbMessageCircle, TbPlus } from 'react-icons/tb';
import { useAuthStore } from '../store/authStore';
import { jdSummaryService } from '../services/jdSummaryService';
import type { JobSummarySearchReq, JobSummaryView } from '../types/jobSummary';
import { JobSummarySearch } from '../components/JobSummarySearch';
import { JobSummaryCard } from '../components/JobSummaryCard';
import { RagChatModal } from '../components/rag/RagChatModal';

const MainPage = () => {
  const navigate = useNavigate();
  const { isInitialized, isAuthenticated } = useAuthStore();

  const [featuredJds, setFeaturedJds] = useState<JobSummaryView[]>([]);
  const [isRagModalOpen, setIsRagModalOpen] = useState(false);

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
    const nextParams = new URLSearchParams();
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== '') {
        nextParams.set(key, value.toString());
      }
    });

    navigate(`/jd?${nextParams.toString()}`);
  };

  if (!isInitialized) return null;

  if (!isAuthenticated) {
    return <Navigate to="/jd" replace />;
  }

  return (
    <div className="min-h-screen bg-white">
      <section className="relative overflow-hidden border-b border-gray-50 pb-32 pt-40">
        <div className="relative z-10 mx-auto max-w-7xl px-6">
          <div className="mx-auto max-w-4xl text-center">
            <div className="mb-10 inline-flex items-center gap-2 rounded-full border border-[#89cbb6]/20 bg-[#89cbb6]/10 px-4 py-2">
              <span className="h-2 w-2 animate-pulse rounded-full bg-[#276db8]" />
              <span className="text-[10px] font-black uppercase tracking-[0.3em] text-[#276db8]">개인 채용 로그북</span>
            </div>

            <h1 className="mb-10 text-6xl font-black leading-[1.0] tracking-tighter text-gray-900 md:text-8xl">
              당신의 성장도
              <br />
              <span className="text-[#3FB6B2]">기록하세요</span>
            </h1>

            <p className="mx-auto mb-16 max-w-2xl text-xl font-medium leading-relaxed text-gray-400 md:text-2xl">
              HireLog는 지원 기록을 정리하고 다음 합격 전략으로 연결해 주는 커리어 로그 서비스입니다.
            </p>

            <JobSummarySearch onSearch={handleSearch} />
          </div>
        </div>
      </section>

      <section className="bg-[#F8F9FA]/50 py-24">
        <div className="mx-auto max-w-7xl px-6">
          <div className="mb-16 flex items-end justify-between">
            <div>
              <h2 className="mb-4 text-xs font-black uppercase tracking-[0.4em] text-[#89cbb6]">Recent Logs</h2>
              <h3 className="text-4xl font-black tracking-tight text-gray-900">최근 수집된 채용 기록</h3>
            </div>
            <button
              onClick={() => navigate('/jd')}
              className="text-sm font-black uppercase tracking-widest text-[#276db8] transition-colors hover:text-[#89cbb6]"
            >
              전체보기
            </button>
          </div>

          <div className="grid grid-cols-1 gap-8 md:grid-cols-3">
            {(featuredJds || []).length > 0
              ? featuredJds.map((jd) => <JobSummaryCard key={jd.id} summary={jd} />)
              : [1, 2, 3].map((i) => <div key={i} className="h-80 rounded-3xl border border-gray-100 bg-white shadow-sm" />)}
          </div>
        </div>
      </section>

      <section className="border-t border-gray-100 bg-white py-24">
        <div className="mx-auto max-w-7xl px-6 text-center">
          <h2 className="mb-8 text-xs font-black uppercase tracking-[0.5em] text-gray-400">서비스 살펴보기</h2>
          <h3 className="mb-12 text-4xl font-black uppercase tracking-tight text-gray-900">기록을 전략으로 바꾸는 방법</h3>

          <div className="mb-10 flex justify-center">
            <button
              onClick={() => navigate('/service-intro')}
              className="rounded-[24px] border border-gray-200 px-12 py-4 text-base font-bold text-gray-700 transition hover:bg-gray-50"
            >
              서비스 소개 페이지 보기
            </button>
          </div>

          <button
            onClick={() => navigate('/jd/request')}
            className="mx-auto flex items-center gap-4 rounded-[24px] bg-[#0f172a] px-16 py-6 text-xl font-bold text-white shadow-2xl transition-all hover:scale-105"
          >
            <TbPlus size={24} />
            JD 요약 요청하기
          </button>
        </div>
      </section>

      <button
        type="button"
        onClick={() => setIsRagModalOpen(true)}
        className="fixed bottom-8 right-8 z-50 inline-flex items-center gap-2 rounded-full bg-[#4CDFD5] px-5 py-3 text-sm font-bold text-[#083a37] shadow-lg transition hover:brightness-95"
      >
        <TbMessageCircle size={18} />
        {'\uCC44\uC6A9 \uB3C4\uC6B0\uBBF8'}
      </button>

      <RagChatModal isOpen={isRagModalOpen} onClose={() => setIsRagModalOpen(false)} />
    </div>
  );
};

export default MainPage;
