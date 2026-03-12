import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { TbChevronLeft } from 'react-icons/tb';
import { JobSummarySearch } from '../components/JobSummarySearch';
import type { JobSummarySearchReq } from '../types/jobSummary';
import { useSummaryJobDescription } from '../hooks/useSummaryJobDescription';

export default function JdSummaryPage() {
  const navigate = useNavigate();

  const [brandName, setBrandName] = useState('');
  const [positionName, setPositionName] = useState('');
  const [jdText, setJdText] = useState('');
  const [error, setError] = useState<string | null>(null);

  const summaryMutation = useSummaryJobDescription();

  const handleRegister = () => {
    setError(null);

    if (!brandName.trim() || !positionName.trim() || !jdText.trim()) {
      setError('기업명, 포지션, 공고본문은 필수 입력입니다.');
      return;
    }

    summaryMutation.mutate({ brandName, positionName, jdText });
  };

  return (

    <div className="min-h-screen bg-[#F8F9FA] pt-16 pb-20">

      {/* 🔎 검색바 영역 */}
      <div className="px-6 py-8">
        <div className="max-w-5xl mx-auto">
          <JobSummarySearch
            onSearch={(params: JobSummarySearchReq) => {
              const query = new URLSearchParams();

              Object.entries(params).forEach(([key, value]) => {
                if (value !== undefined && value !== '') {
                  query.set(key, value.toString());
                }
              });

              query.set('page', '0');
              navigate(`/archive?${query.toString()}`);
            }}
            initialParams={{}}
          />
        </div>
      </div>

      {/* 메인 컨텐츠 */}
      <main className="max-w-4xl mx-auto px-6 py-12 bg-white rounded-2xl shadow-sm">

        {/* 타이틀 */}
        <div className="text-center mb-16 relative">
          <h1 className="text-3xl font-bold mb-4 flex justify-center items-center gap-2">
            <span className="text-teal-400">JD</span>
            <span>등록</span>
          </h1>

          <p className="text-gray-500 text-sm">
            분석할 채용 공고의 상세 정보를 입력해 주세요.<br />
            등록 즉시 AI가 핵심 정보를 요약해 드립니다.
          </p>

          <div className="absolute top-0 right-0 hidden md:block">
            <Link
              to="/archive"
              className="flex items-center text-gray-500 hover:text-gray-800 text-sm font-medium"
            >
              <TbChevronLeft className="mr-1 w-3 h-3" />
              최근 등록 이력
            </Link>
          </div>
        </div>

        {/* 폼 */}
        <div className="space-y-12">

          {/* 기업 / 포지션 */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-12">
            <div className="space-y-2">
              <label className="block text-xs font-bold text-gray-500">
                기업명
              </label>
              <input
                type="text"
                value={brandName}
                onChange={(e) => setBrandName(e.target.value)}
                className="w-full border-b border-gray-300 py-2 focus:border-teal-400 outline-none transition-colors text-lg"
              />
            </div>

            <div className="space-y-2">
              <label className="block text-xs font-bold text-gray-500">
                포지션
              </label>
              <input
                type="text"
                value={positionName}
                onChange={(e) => setPositionName(e.target.value)}
                className="w-full border-b border-gray-300 py-2 focus:border-teal-400 outline-none transition-colors text-lg"
              />
            </div>
          </div>

          {/* 공고 본문 */}
          <div className="space-y-2">
            <label className="block text-xs font-bold text-gray-500">
              공고본문
            </label>
            <div className="border border-gray-300 rounded-lg p-4 focus-within:border-teal-400 transition-colors">
              <textarea
                value={jdText}
                onChange={(e) => setJdText(e.target.value)}
                className="w-full h-80 outline-none resize-none text-base"
              />
            </div>
          </div>

          {/* 등록 버튼 */}
          <div className="flex flex-col items-center pt-8 gap-4">
            {error && (
              <p className="text-sm font-bold text-rose-500 bg-rose-50 px-4 py-2 rounded-lg">
                {error}
              </p>
            )}

            <button
              onClick={handleRegister}
              disabled={summaryMutation.isPending}
              className="px-10 py-3 rounded-full border border-teal-500 text-teal-600 font-bold hover:bg-teal-50 transition-colors disabled:opacity-50"
            >
              {summaryMutation.isPending ? '분석 중...' : 'JD 등록하기'}
            </button>
          </div>

        </div>
      </main>
    </div>
  );
}
