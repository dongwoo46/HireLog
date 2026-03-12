import React, { useState, useEffect } from 'react';
import { TbBuilding, TbCategory, TbBriefcase, TbSearch } from 'react-icons/tb';
import { Modal } from './common/Modal';
import { Button } from './common/Button';
import type { JobSummarySearchReq } from '../types/jobSummary';

interface JobSummaryFilterModalProps {
    isOpen: boolean;
    onClose: () => void;
    filters: JobSummarySearchReq;
    onApply: (filters: JobSummarySearchReq) => void;
    onReset: () => void;
}

export const JobSummaryFilterModal: React.FC<JobSummaryFilterModalProps> = ({
    isOpen,
    onClose,
    filters,
    onApply,
    onReset
}) => {

    const [localFilters, setLocalFilters] = useState<JobSummarySearchReq>(filters);

    useEffect(() => {
        if (isOpen) setLocalFilters(filters);
    }, [isOpen, filters]);

    const updateParam = (key: keyof JobSummarySearchReq, value: any) => {
        setLocalFilters(prev => ({ ...prev, [key]: value }));
    };

    // 🔥 수정: 적용 시 모든 string 값 trim 처리
    const handleApply = () => {

        const trimmedFilters: any = {};

        Object.entries(localFilters).forEach(([key, value]) => {
            if (typeof value === 'string') {
                const trimmed = value.trim();
                if (trimmed !== '') {
                    trimmedFilters[key] = trimmed;
                }
            } else if (value !== undefined && value !== null) {
                trimmedFilters[key] = value;
            }
        });

        onApply(trimmedFilters);
        onClose();
    };

    const handleReset = () => {
        onReset();
        setLocalFilters({
            keyword: localFilters.keyword,
            careerType: localFilters.careerType,
            sortBy: 'CREATED_AT_DESC'
        });
    };

    return (
        <Modal isOpen={isOpen} onClose={onClose} title="상세 필터 설정" maxWidth="max-w-4xl">

            <div className="space-y-14">

                {/* 기업 */}
                <FilterSection title="기업 / 브랜드" icon={<TbBuilding />}>
                    <FilterInput
                        placeholder="기업명을 입력하세요"
                        value={localFilters.brandName}
                        onChange={val => updateParam('brandName', val)}
                    />
                </FilterSection>

                {/* 카테고리 */}
                <FilterSection title="직무 카테고리" icon={<TbCategory />}>
                    <FilterInput
                        placeholder="카테고리를 입력하세요"
                        value={localFilters.positionCategoryName}
                        onChange={val => updateParam('positionCategoryName', val)}
                    />
                </FilterSection>

                {/* 포지션 */}
                <FilterSection title="포지션 / 역할" icon={<TbBriefcase />}>
                    <FilterInput
                        placeholder="포지션을 입력하세요"
                        value={localFilters.positionName}
                        onChange={val => updateParam('positionName', val)}
                    />
                </FilterSection>

            </div>

            {/* Footer */}
            <div className="mt-16 pt-8 border-t border-gray-100 flex justify-between items-center">

                <div className="flex gap-3">
                    <SortButton
                        active={localFilters.sortBy === 'CREATED_AT_DESC'}
                        onClick={() => updateParam('sortBy', 'CREATED_AT_DESC')}
                    >
                        최신순
                    </SortButton>

                    <SortButton
                        active={localFilters.sortBy === 'CREATED_AT_ASC'}
                        onClick={() => updateParam('sortBy', 'CREATED_AT_ASC')}
                    >
                        오래된순
                    </SortButton>
                </div>

                <div className="flex gap-4 items-center">

                    <button
                        onClick={handleReset}
                        className="text-sm font-semibold text-gray-400 hover:text-rose-400 transition"
                    >
                        필터 초기화
                    </button>

                    <Button
                        onClick={handleApply}
                        className="bg-gradient-to-r from-[#4CDFD5] to-[#3FB6B2] text-white font-bold px-8 py-3 rounded-2xl shadow-lg shadow-[#4CDFD5]/30 hover:scale-105 transition-all"
                    >
                        필터 적용하기
                    </Button>

                </div>

            </div>

        </Modal>
    );
};


/* ---------- 공통 컴포넌트 ---------- */

const FilterSection = ({
    title,
    icon,
    children
}: {
    title: string;
    icon: React.ReactNode;
    children: React.ReactNode;
}) => (
    <div className="space-y-4">
        <h3 className="flex items-center gap-2 text-sm font-black text-[#4CDFD5] tracking-widest uppercase">
            {icon} {title}
        </h3>
        {children}
    </div>
);

const FilterInput = ({
    placeholder,
    value,
    onChange
}: {
    placeholder: string;
    value?: string;
    onChange: (val: string) => void;
}) => (
    <div className="relative group">

        <TbSearch
            className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-300 group-focus-within:text-[#4CDFD5] transition"
            size={18}
        />

        <input
            type="text"
            value={value || ''}
            onChange={e => onChange(e.target.value)}
            placeholder={placeholder}
            className="w-full pl-12 pr-4 py-4 bg-gray-50 border border-gray-100 rounded-2xl text-sm font-semibold text-gray-700 outline-none transition-all
                 focus:bg-white focus:border-[#4CDFD5] focus:ring-4 focus:ring-[#4CDFD5]/20"
        />

    </div>
);

const SortButton = ({
    active,
    onClick,
    children
}: {
    active: boolean;
    onClick: () => void;
    children: React.ReactNode;
}) => (
    <button
        onClick={onClick}
        className={`px-5 py-2 rounded-xl text-sm font-bold transition-all ${active
            ? 'bg-gradient-to-r from-[#4CDFD5] to-[#3FB6B2] text-white shadow-md'
            : 'bg-gray-100 text-gray-500 hover:bg-gray-200'
            }`}
    >
        {children}
    </button>
);
