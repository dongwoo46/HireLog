import React, { useState, useEffect, useMemo } from 'react';
import { TbSearch, TbAdjustmentsHorizontal, TbChevronDown } from 'react-icons/tb';
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

  const stableInitial = useMemo(() => ({
    ...initialParams,
    keyword: initialParams.keyword || '',
    sortBy: initialParams.sortBy || 'CREATED_AT_DESC'
    // eslint-disable-next-line react-hooks/exhaustive-deps
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
    onSearch(cleanParams(params));
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
      <form
        onSubmit={handleSubmit}
        className="w-full max-w-4xl mx-auto"
      >
        {/* 카드 래퍼 */}
        <div className="bg-white rounded-2xl shadow-md border border-gray-100 px-5 py-4">

          {/* 검색 행 */}
          <div className="flex items-center gap-3">

            {/* 검색 아이콘 */}
            <TbSearch size={20} className="text-[#4CDFD5] shrink-0" />

            {/* 키워드 입력 */}
            <input
              type="text"
              value={params.keyword || ''}
              onChange={e => updateParam('keyword', e.target.value)}
              onKeyDown={e => { if (e.key === 'Enter') handleSubmit(); }}
              placeholder="기업명, 포지션, 기술스택 검색..."
              className="flex-1 outline-none bg-transparent text-gray-800 placeholder-gray-400 font-medium text-base"
            />

            {/* 구분선 */}
            <div className="w-px h-5 bg-gray-200 shrink-0" />

            {/* 경력 선택 */}
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

            {/* 구분선 */}
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

            {/* 구분선 */}
            <div className="w-px h-5 bg-gray-200 shrink-0" />

            <button
              type="button"
              onClick={() => setIsFilterModalOpen(true)}
              className="flex items-center text-gray-500 hover:text-[#4CDFD5] transition-colors shrink-0"
              title="상세 필터"
            >
              <TbAdjustmentsHorizontal size={20} />
            </button>

            {/* 검색 버튼 */}
            <button
              type="submit"
              className="bg-[#4CDFD5] hover:bg-[#3CCFC5] active:bg-[#35C3BA] text-white font-black rounded-xl transition-colors shrink-0 px-5 py-2 text-sm"
            >
              검색
            </button>
          </div>

          {/* 활성 필터 태그 */}
          {(params.brandName || params.positionName || params.positionCategoryName) && (
            <div className="flex flex-wrap gap-2 mt-3 pt-3 border-t border-gray-100">
              {params.brandName && (
                <FilterTag
                  label={`브랜드: ${params.brandName}`}
                  onRemove={() => { updateParam('brandName', undefined); updateParam('brandId', undefined); }}
                />
              )}
              {params.positionName && (
                <FilterTag
                  label={`포지션: ${params.positionName}`}
                  onRemove={() => { updateParam('positionName', undefined); updateParam('positionId', undefined); }}
                />
              )}
              {params.positionCategoryName && (
                <FilterTag
                  label={`카테고리: ${params.positionCategoryName}`}
                  onRemove={() => { updateParam('positionCategoryName', undefined); updateParam('positionCategoryId', undefined); }}
                />
              )}
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

const FilterTag: React.FC<{ label: string; onRemove: () => void }> = ({ label, onRemove }) => (
  <span className="inline-flex items-center gap-1.5 px-3 py-1 bg-[#4CDFD5]/10 text-[#4CDFD5] text-xs font-bold rounded-full border border-[#4CDFD5]/20">
    {label}
    <button type="button" onClick={onRemove} className="hover:text-rose-400 transition-colors font-black leading-none">×</button>
  </span>
);
