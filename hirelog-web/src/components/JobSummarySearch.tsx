import React, { useState } from 'react';
import { TbSearch, TbFilter, TbAdjustmentsHorizontal } from 'react-icons/tb';
import type { JobSummarySearchReq, CareerType } from '../types/jobSummary';
import { Button } from './common/Button';
import { JobSummaryFilterModal } from './JobSummaryFilterModal';

interface Props {
  onSearch: (params: JobSummarySearchReq) => void;
  initialParams?: JobSummarySearchReq;
  variant?: 'large' | 'small';
}

export const JobSummarySearch: React.FC<Props> = ({ onSearch, initialParams = {}, variant = 'large' }) => {
  const [isFilterModalOpen, setIsFilterModalOpen] = useState(false);
  const [params, setParams] = useState<JobSummarySearchReq>({
    keyword: initialParams.keyword || '',
    careerType: initialParams.careerType || undefined,
    brandId: initialParams.brandId,
    positionId: initialParams.positionId,
    brandPositionId: initialParams.brandPositionId,
    positionCategoryId: initialParams.positionCategoryId,
    brandName: initialParams.brandName || '',
    positionName: initialParams.positionName || '',
    brandPositionName: initialParams.brandPositionName || '',
    positionCategoryName: initialParams.positionCategoryName || '',
    sortBy: initialParams.sortBy || 'CREATED_AT_DESC',
  });

  const handleSubmit = (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    onSearch({
      ...params,
      keyword: params.keyword || undefined,
      brandName: params.brandName || undefined,
      positionName: params.positionName || undefined,
      brandPositionName: params.brandPositionName || undefined,
      positionCategoryName: params.positionCategoryName || undefined,
    });
  };

  const updateParam = (key: keyof JobSummarySearchReq, value: any) => {
    setParams(prev => ({ ...prev, [key]: value }));
  };

  const handleApplyFilters = (newFilters: JobSummarySearchReq) => {
    setParams(newFilters);
    // Automatically trigger search when filters are applied from modal
    onSearch({
      ...newFilters,
      keyword: params.keyword || undefined // Ensure keyword is preserved if not in modal, though it is shared state
    });
  };

  return (
    <div className={`w-full max-w-4xl mx-auto ${variant === 'large' ? 'py-12' : 'py-4'}`}>
      <form onSubmit={handleSubmit} className="space-y-4">
        {/* Main Search Bar */}
        <div className="flex items-center bg-white rounded-[2rem] shadow-log border border-gray-100 p-3 pl-8 gap-4 hover:shadow-log-hover transition-all duration-500">
          <TbSearch className="text-[#89cbb6]" size={28} />
          <input
            type="text"
            className="flex-grow py-3 text-xl font-bold focus:outline-none text-gray-800 placeholder-gray-300 italic"
            placeholder="키워드로 검색 (예: 삼성, 백엔드...)"
            value={params.keyword}
            onChange={(e) => updateParam('keyword', e.target.value)}
          />

          <div className="hidden md:flex items-center gap-2 px-4 border-l border-gray-100">
            <TbFilter className="text-gray-400" size={20} />
            <select
              className="bg-transparent text-sm font-black text-gray-500 outline-none cursor-pointer uppercase tracking-widest italic"
              value={params.careerType || ''}
              onChange={(e) => updateParam('careerType', e.target.value as CareerType || undefined)}
            >
              <option value="">경력 전체</option>
              <option value="NEW">신입</option>
              <option value="EXPERIENCED">경력</option>
            </select>
          </div>

          <div className="hidden md:flex items-center border-l border-gray-100 px-2">
            <button
              type="button"
              onClick={() => setIsFilterModalOpen(true)}
              className="flex items-center gap-2 px-4 py-2 text-sm font-bold text-gray-500 hover:text-[#276db8] hover:bg-gray-50 rounded-xl transition-all"
            >
              <TbAdjustmentsHorizontal size={20} />
              <span className="whitespace-nowrap">상세 필터</span>
            </button>
          </div>

          <Button
            type="submit"
            variant="gradient"
            size="lg"
            className="rounded-2xl shadow-xl shadow-[#89cbb6]/20"
          >
            검색하기
          </Button>
        </div>
      </form>

      <JobSummaryFilterModal
        isOpen={isFilterModalOpen}
        onClose={() => setIsFilterModalOpen(false)}
        filters={params}
        onApply={handleApplyFilters}
        onReset={() => {
          setParams({
            keyword: params.keyword, // Keep keyword
            careerType: params.careerType, // Keep career type
            sortBy: 'CREATED_AT_DESC'
          });
        }}
      />
    </div>
  );
};

