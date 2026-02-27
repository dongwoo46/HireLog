import React, { useState, useEffect, useMemo } from 'react';
import {
  TbSearch,
  TbAdjustmentsHorizontal,
  TbChevronDown,
  TbInfoCircle,
  TbClock,
  TbX
} from 'react-icons/tb';
import type { JobSummarySearchReq, CareerType } from '../types/jobSummary';
import { JobSummaryFilterModal } from './JobSummaryFilterModal';

interface Props {
  onSearch: (params: JobSummarySearchReq) => void;
  initialParams?: JobSummarySearchReq;
}

export const JobSummarySearch: React.FC<Props> = ({
  onSearch,
  initialParams = {},
}) => {

  const [isFilterModalOpen, setIsFilterModalOpen] = useState(false);
  const [recentKeywords, setRecentKeywords] = useState<string[]>([]);

  /* ------------------ 최근 검색어 로드 ------------------ */
  useEffect(() => {
    const stored = localStorage.getItem('recent_keywords');
    if (stored) {
      setRecentKeywords(JSON.parse(stored));
    }
  }, []);

  const saveRecentKeyword = (keyword: string) => {
    if (!keyword.trim()) return;

    const updated = [
      keyword,
      ...recentKeywords.filter(k => k !== keyword)
    ].slice(0, 5);

    setRecentKeywords(updated);
    localStorage.setItem('recent_keywords', JSON.stringify(updated));
  };

  const removeRecentKeyword = (keyword: string) => {
    const updated = recentKeywords.filter(k => k !== keyword);
    setRecentKeywords(updated);
    localStorage.setItem('recent_keywords', JSON.stringify(updated));
  };

  const clearRecentKeywords = () => {
    setRecentKeywords([]);
    localStorage.removeItem('recent_keywords');
  };

  /* ------------------ 초기값 안정화 ------------------ */
  const stableInitial = useMemo(() => ({
    ...initialParams,
    keyword: initialParams.keyword || '',
    sortBy: initialParams.sortBy || 'CREATED_AT_DESC'
  }), [initialParams.keyword, initialParams.careerType, initialParams.sortBy]);

  const [params, setParams] = useState<JobSummarySearchReq>(stableInitial);

  useEffect(() => {
    setParams(prev => {
      if (JSON.stringify(prev) === JSON.stringify(stableInitial)) {
        return prev;
      }
      return stableInitial;
    });
  }, [stableInitial]);

  const cleanParams = (obj: JobSummarySearchReq): JobSummarySearchReq => {
    const cleaned: any = {};
    Object.entries(obj).forEach(([key, value]) => {
      if (value !== undefined && value !== '' && value !== null) {
        cleaned[key] = value;
      }
    });
    return cleaned;
  };

  const handleSubmit = (e?: React.FormEvent) => {
    if (e) e.preventDefault();

    const cleaned = cleanParams(params);

    if (params.keyword) {
      saveRecentKeyword(params.keyword);
    }

    onSearch(cleaned);
  };

  const updateParam = (key: keyof JobSummarySearchReq, value: any) => {
    setParams(prev => ({ ...prev, [key]: value }));
  };

  const handleApplyFilters = (newFilters: JobSummarySearchReq) => {
    const merged = { ...params, ...newFilters };
    const cleaned = cleanParams(merged);
    setParams(cleaned);
    onSearch(cleaned);
  };

  return (
    <>
      <form onSubmit={handleSubmit} className="w-full max-w-4xl mx-auto">

        <div className="bg-white rounded-2xl shadow-md border border-gray-100 px-5 py-4 relative">

          <div className="flex items-center gap-3">

            <TbSearch size={20} className="text-[#4CDFD5] shrink-0" />

            {/* 🔥 키워드 입력 */}
            <input
              type="text"
              value={params.keyword || ''}
              onChange={e => updateParam('keyword', e.target.value)}
              onKeyDown={e => { if (e.key === 'Enter') handleSubmit(); }}
              placeholder="예: 네이버, 프론트엔드, React"
              className="flex-1 outline-none bg-transparent text-gray-800 placeholder-gray-400 font-medium text-base"
            />

            {/* 🔥 검색 팁 */}
            <div className="relative group shrink-0">
              <TbInfoCircle
                size={18}
                className="text-gray-400 hover:text-[#4CDFD5] cursor-pointer"
              />
              <div className="
                absolute right-0 top-8 w-64
                bg-white border border-gray-100 shadow-xl rounded-xl p-4
                text-xs text-gray-600 leading-relaxed
                opacity-0 group-hover:opacity-100
                transition-all duration-200 z-50
              ">
                <div className="font-bold text-[#4CDFD5] mb-2">검색 팁</div>
                <ul className="space-y-1">
                  <li>• 기업명: 네이버, 카카오</li>
                  <li>• 포지션: 프론트엔드, 백엔드</li>
                  <li>• 기술스택: React, Spring</li>
                  <li>• 복합 검색 가능 (예: 네이버 React)</li>
                </ul>
              </div>
            </div>

            <div className="w-px h-5 bg-gray-200 shrink-0" />

            {/* 경력 */}
            <div className="relative shrink-0">
              <select
                value={params.careerType || ''}
                onChange={e => updateParam('careerType', e.target.value as CareerType || undefined)}
                className="appearance-none bg-transparent text-gray-500 font-bold cursor-pointer outline-none pr-5 text-sm"
              >
                <option value="">전체 경력</option>
                <option value="NEW">신입</option>
                <option value="EXPERIENCED">경력</option>
                <option value="ANY">무관</option>
              </select>
              <TbChevronDown size={13} className="absolute right-0 top-1/2 -translate-y-1/2 text-gray-400 pointer-events-none" />
            </div>

            <div className="w-px h-5 bg-gray-200 shrink-0" />

            {/* 정렬 */}
            <div className="relative shrink-0">
              <select
                value={params.sortBy || 'CREATED_AT_DESC'}
                onChange={e => updateParam('sortBy', e.target.value)}
                className="appearance-none bg-transparent text-gray-500 font-bold cursor-pointer outline-none pr-5 text-sm"
              >
                <option value="CREATED_AT_DESC">최신순</option>
                <option value="CREATED_AT_ASC">오래된순</option>
              </select>
              <TbChevronDown size={13} className="absolute right-0 top-1/2 -translate-y-1/2 text-gray-400 pointer-events-none" />
            </div>

            <div className="w-px h-5 bg-gray-200 shrink-0" />

            <button
              type="button"
              onClick={() => setIsFilterModalOpen(true)}
              className="flex items-center text-gray-500 hover:text-[#4CDFD5] transition-colors shrink-0"
            >
              <TbAdjustmentsHorizontal size={20} />
            </button>

            <button
              type="submit"
              className="bg-[#4CDFD5] hover:bg-[#3CCFC5] text-white font-black rounded-xl transition-colors shrink-0 px-5 py-2 text-sm"
            >
              검색
            </button>
          </div>

          {/* 🔥 최근 검색어 */}
          {recentKeywords.length > 0 && (
            <div className="mt-4 pt-3 border-t border-gray-100">

              <div className="flex justify-between items-center mb-2">
                <div className="flex items-center gap-2 text-xs text-gray-400 font-semibold">
                  <TbClock size={14} />
                  최근 검색어
                </div>

                <button
                  type="button"
                  onClick={clearRecentKeywords}
                  className="text-xs text-gray-400 hover:text-rose-400 transition"
                >
                  전체삭제
                </button>
              </div>

              <div className="flex flex-wrap gap-2">
                {recentKeywords.map((keyword, idx) => (
                  <div
                    key={idx}
                    className="group flex items-center gap-1 px-3 py-1 bg-gray-100 text-gray-600 text-xs font-semibold rounded-full hover:bg-[#4CDFD5]/10 hover:text-[#4CDFD5] transition"
                  >
                    <button
                      type="button"
                      onClick={() => {
                        updateParam('keyword', keyword);
                        onSearch({ ...params, keyword });
                      }}
                    >
                      {keyword}
                    </button>

                    <button
                      type="button"
                      onClick={() => removeRecentKeyword(keyword)}
                      className="opacity-0 group-hover:opacity-100 transition text-gray-400 hover:text-rose-400"
                    >
                      <TbX size={12} />
                    </button>
                  </div>
                ))}
              </div>
            </div>
          )}

        </div>
      </form>

      <JobSummaryFilterModal
        isOpen={isFilterModalOpen}
        onClose={() => setIsFilterModalOpen(false)}
        filters={params}
        onApply={handleApplyFilters}
        onReset={() => {
          const reset: JobSummarySearchReq = {
            keyword: params.keyword,
            careerType: params.careerType,
            sortBy: 'CREATED_AT_DESC'
          };
          setParams(reset);
          onSearch(cleanParams(reset));
        }}
      />
    </>
  );
};
