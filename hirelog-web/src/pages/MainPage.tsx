import { useNavigate } from 'react-router-dom';
import { useEffect, useState } from 'react';
import { TbChevronDown, TbAdjustmentsHorizontal } from 'react-icons/tb';
import { useAuthStore } from '../store/authStore';
import { jdSummaryService } from '../services/jdSummaryService';
import type { JobSummaryView, CareerType } from '../types/jobSummary';
import FilterModal from '../components/FilterModal';

const MainPage = () => {
  const navigate = useNavigate();
  const { isInitialized, isAuthenticated } = useAuthStore();

  const [featuredJds, setFeaturedJds] = useState<JobSummaryView[]>([]);
  const [isFilterOpen, setIsFilterOpen] = useState(false);
  const [career, setCareer] = useState<CareerType>('ANY');
  const [keyword, setKeyword] = useState('');
  const [sortBy, setSortBy] = useState('CREATED_AT_DESC');
  const [isCareerOpen, setIsCareerOpen] = useState(false);

  useEffect(() => {
    if (!isInitialized) return;

    const loadFeatured = async () => {
      const result = await jdSummaryService.search({
        size: 3,
        sortBy: 'CREATED_AT_DESC',
      });
      setFeaturedJds(result?.items || []);
    };

    loadFeatured();
  }, [isInitialized]);

  const handleSearch = () => {
    navigate(`/jd?keyword=${keyword}&careerType=${career}&sortBy=${sortBy}`);
  };

  if (!isInitialized) return null;

  if (!isAuthenticated) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        로그인이 필요합니다.
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#f5f7f8]">

      {/* HERO */}
      <section className="pt-32 pb-24 text-center px-6">

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
          HireLog는 단순한 요약을 넘어,
          당신의 성장을 기록하고 분석하는 AI 커리어 일지입니다.
        </p>

        {/* 검색바 */}
        <div className="max-w-4xl mx-auto bg-white shadow-md rounded-2xl p-3 flex items-center gap-4 border border-[#2ec4b6]/40 relative">

          <input
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            placeholder="키워드로 검색 (예: 상장, 백엔드...)"
            className="flex-1 px-4 py-3 rounded-xl outline-none"
          />

          {/* 경력 필터 */}
          <div className="relative">
            <button
              onClick={() => setIsCareerOpen(!isCareerOpen)}
              className="flex items-center gap-1 text-gray-600 font-medium"
            >
              {career === 'NEW'
                ? '신입'
                : career === 'EXPERIENCED'
                  ? '경력'
                  : '경력 전체'}
              <TbChevronDown />
            </button>

            {isCareerOpen && (
              <div className="absolute right-0 mt-2 bg-white shadow-lg rounded-xl w-28 p-2 z-50">
                <button
                  onClick={() => {
                    setCareer('NEW');
                    setIsCareerOpen(false);
                  }}
                  className="block w-full text-left px-3 py-2 hover:bg-gray-100 rounded"
                >
                  신입
                </button>
                <button
                  onClick={() => {
                    setCareer('EXPERIENCED');
                    setIsCareerOpen(false);
                  }}
                  className="block w-full text-left px-3 py-2 hover:bg-gray-100 rounded"
                >
                  경력
                </button>
                <button
                  onClick={() => {
                    setCareer('ANY');
                    setIsCareerOpen(false);
                  }}
                  className="block w-full text-left px-3 py-2 hover:bg-gray-100 rounded"
                >
                  전체
                </button>
              </div>
            )}
          </div>

          {/* 상세 필터 */}
          <button
            onClick={() => setIsFilterOpen(true)}
            className="flex items-center gap-1 text-gray-600 font-medium"
          >
            <TbAdjustmentsHorizontal />
            상세 필터
          </button>

          <button
            onClick={handleSearch}
            className="bg-[#2ec4b6] text-white px-6 py-3 rounded-xl font-semibold hover:opacity-90 transition"
          >
            검색하기
          </button>
        </div>
      </section>

      {/* 최근 수집된 채용 기록 */}
      <section className="max-w-6xl mx-auto px-6 pb-24">

        <div className="flex justify-between items-center mb-6">
          <h2 className="text-xl font-bold">
            최근 수집된 채용 기록
          </h2>

          <button
            onClick={() => navigate('/jd')}
            className="text-[#2ec4b6] font-semibold"
          >
            더보기 →
          </button>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {featuredJds.map((jd) => (
            <div
              key={jd.id}
              className="bg-white rounded-xl p-6 shadow-sm border hover:shadow-md transition cursor-pointer"
              onClick={() => navigate(`/jd/${jd.id}`)}
            >
              <h3 className="font-semibold text-lg">
                {jd.brandName}
              </h3>

              <p className="text-gray-500 text-sm mt-2">
                {jd.brandPositionName}
              </p>

              <p className="text-xs text-gray-400 mt-4">
                {jd.createdAt?.slice(0, 10)}
              </p>
            </div>
          ))}
        </div>
      </section>

      {/* 서비스 소개 섹션 */}
      <section className="bg-white py-24 mt-10">
        <div className="max-w-5xl mx-auto px-6 text-center">

          <h3 className="text-2xl font-bold mb-12">
            HireLog와 함께 성장하세요
          </h3>

          <div className="grid md:grid-cols-3 gap-10">

            <div>
              <div className="w-14 h-14 mx-auto bg-[#2ec4b6]/20 rounded-full flex items-center justify-center mb-4">
                📊
              </div>
              <p className="font-semibold mb-2">JD 분석 자동화</p>
              <p className="text-sm text-gray-500">
                AI가 공고를 분석하여 핵심을 정리합니다.
              </p>
            </div>

            <div>
              <div className="w-14 h-14 mx-auto bg-[#2ec4b6]/20 rounded-full flex items-center justify-center mb-4">
                🧠
              </div>
              <p className="font-semibold mb-2">면접 대비 전략</p>
              <p className="text-sm text-gray-500">
                기록된 데이터를 기반으로 전략을 세웁니다.
              </p>
            </div>

            <div>
              <div className="w-14 h-14 mx-auto bg-[#2ec4b6]/20 rounded-full flex items-center justify-center mb-4">
                📈
              </div>
              <p className="font-semibold mb-2">커리어 자산화</p>
              <p className="text-sm text-gray-500">
                지원 이력을 자산처럼 관리하세요.
              </p>
            </div>

          </div>
        </div>
      </section>

      {/* 필터 모달 */}
      {isFilterOpen && (
        <FilterModal
          onClose={() => setIsFilterOpen(false)}
          sortBy={sortBy}
          setSortBy={setSortBy}
        />
      )}

    </div>
  );
};

export default MainPage;
