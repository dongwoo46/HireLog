import React, { useState, useEffect, useRef } from 'react';
import { TbBuilding, TbCategory, TbBriefcase, TbSearch, TbTag } from 'react-icons/tb';
import { Modal } from './common/Modal';
import { Button } from './common/Button';
import type { JobSummarySearchReq } from '../types/jobSummary';

// Mock Data for Autocomplete (Moved from JobSummarySearch)
const MOCK_DATA = {
    brands: [
        { id: 1, name: 'Google' },
        { id: 2, name: 'Samsung Electronics' },
        { id: 3, name: 'Naver' },
        { id: 4, name: 'Kakao' },
        { id: 5, name: 'Toss' },
    ],
    positions: [
        { id: 101, name: 'Backend Engineer' },
        { id: 102, name: 'Frontend Engineer' },
        { id: 103, name: 'Product Manager' },
        { id: 104, name: 'Data Scientist' },
    ],
    categories: [
        { id: 1001, name: 'Software Development' },
        { id: 1002, name: 'Design' },
        { id: 1003, name: 'Marketing' },
        { id: 1004, name: 'Project Management' },
    ]
};

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

    // Sync local state when modal opens or parent filters change
    useEffect(() => {
        if (isOpen) {
            setLocalFilters(filters);
        }
    }, [isOpen, filters]);

    const updateParam = (key: keyof JobSummarySearchReq, value: any) => {
        setLocalFilters(prev => ({ ...prev, [key]: value }));
    };

    const handleApply = () => {
        onApply(localFilters);
        onClose();
    };

    const handleReset = () => {
        onReset();
        setLocalFilters({
            keyword: '', // specific reset logic might be needed if keyword is kept outside
            sortBy: 'CREATED_AT_DESC'
        });
        // Ideally parent reset handles the 'default' state, but here we update local view immediately
        // or we can wait for parent update.
        // Let's just reset local fields that are in this modal.
    };

    return (
        <Modal isOpen={isOpen} onClose={onClose} title="상세 필터 설정" maxWidth="max-w-4xl">
            <div className="space-y-12">
                {/* Filter Rows */}
                <FilterRow
                    title="기업명 / 브랜드"
                    icon={<TbBuilding size={14} />}
                    color="text-[#89cbb6]"
                >
                    <AutocompletePicker
                        label="시스템 등록 기업 선택"
                        icon={<TbBuilding size={18} />}
                        selectedId={localFilters.brandId}
                        suggestions={MOCK_DATA.brands}
                        onSelect={(id) => updateParam('brandId', id)}
                    />
                    <MatchInput
                        label="직접 이름 입력"
                        value={localFilters.brandName}
                        onChange={val => updateParam('brandName', val)}
                    />
                </FilterRow>

                <FilterRow
                    title="직무 카테고리"
                    icon={<TbCategory size={14} />}
                    color="text-[#276db8]"
                >
                    <AutocompletePicker
                        label="카테고리 선택"
                        icon={<TbCategory size={18} />}
                        selectedId={localFilters.positionCategoryId}
                        suggestions={MOCK_DATA.categories}
                        onSelect={(id) => updateParam('positionCategoryId', id)}
                    />
                    <MatchInput
                        label="직접 카테고리 입력"
                        value={localFilters.positionCategoryName}
                        onChange={val => updateParam('positionCategoryName', val)}
                    />
                </FilterRow>

                <FilterRow
                    title="포지션 / 역할"
                    icon={<TbBriefcase size={14} />}
                    color="text-gray-900"
                >
                    <AutocompletePicker
                        label="시스템 등록 포지션 선택"
                        icon={<TbBriefcase size={18} />}
                        selectedId={localFilters.positionId}
                        suggestions={MOCK_DATA.positions}
                        onSelect={(id) => updateParam('positionId', id)}
                    />
                    <MatchInput
                        label="직접 포지션 입력"
                        value={localFilters.positionName}
                        onChange={val => updateParam('positionName', val)}
                    />
                </FilterRow>
            </div>

            {/* Footer Actions */}
            <div className="mt-12 pt-8 border-t border-gray-50 flex flex-col md:flex-row gap-6 items-center justify-between">
                <div className="flex items-center gap-4 w-full md:w-auto">
                    <span className="text-[10px] font-black text-gray-400 uppercase tracking-widest whitespace-nowrap">정렬 기준</span>
                    <div className="flex gap-2 w-full md:w-auto">
                        <SortButton
                            active={localFilters.sortBy === 'CREATED_AT_DESC'}
                            label="최신순"
                            onClick={() => updateParam('sortBy', 'CREATED_AT_DESC')}
                        />
                        <SortButton
                            active={localFilters.sortBy === 'CREATED_AT_ASC'}
                            label="오래된순"
                            onClick={() => updateParam('sortBy', 'CREATED_AT_ASC')}
                        />
                    </div>
                </div>

                <div className="flex items-center gap-4 w-full md:w-auto justify-end">
                    <button
                        type="button"
                        onClick={handleReset}
                        className="text-xs font-bold text-gray-400 hover:text-red-400 transition-colors px-4"
                    >
                        필터 초기화
                    </button>
                    <Button
                        onClick={handleApply}
                        variant="primary"
                        className="shadow-lg shadow-[#276db8]/20"
                    >
                        필터 적용하기
                    </Button>
                </div>
            </div>
        </Modal>
    );
};

// --- Helper Components (Moved from JobSummarySearch) ---

const FilterRow = ({ title, icon, color, children }: { title: string, icon: React.ReactNode, color: string, children: React.ReactNode }) => (
    <div className="space-y-4">
        <h3 className={`text-[10px] font-black ${color} uppercase tracking-[0.4em] flex items-center gap-2 mb-2`}>
            {icon} {title}
        </h3>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {children}
        </div>
    </div>
);

const MatchInput = ({ label, value, onChange }: { label: string, value?: string, onChange: (val: string) => void }) => (
    <div className="space-y-1">
        <label className="text-[9px] font-black text-gray-300 uppercase tracking-[0.2em] ml-2">{label}</label>
        <div className="relative group">
            <div className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-300 group-hover:text-[#276db8] transition-colors">
                <TbSearch size={18} />
            </div>
            <input
                type="text"
                className="w-full bg-gray-50 border border-gray-100 rounded-2xl pl-12 pr-4 py-3 text-sm font-bold text-gray-700 outline-none focus:bg-white focus:border-[#276db8] focus:ring-4 focus:ring-[#276db8]/5 transition-all"
                placeholder="검색할 이름을 입력하세요..."
                value={value || ''}
                onChange={e => onChange(e.target.value)}
            />
        </div>
    </div>
);

const SortButton = ({ active, label, onClick }: { active: boolean, label: string, onClick: () => void }) => (
    <button
        type="button"
        onClick={onClick}
        className={`px-4 py-2 rounded-xl text-xs font-black uppercase tracking-widest transition-all ${active ? 'bg-gray-900 text-white shadow-lg' : 'bg-gray-50 text-gray-400 hover:bg-gray-100'
            }`}
    >
        {label}
    </button>
);

interface AutocompleteProps {
    label: string;
    icon: React.ReactNode;
    selectedId?: number;
    suggestions: { id: number, name: string }[];
    onSelect: (id: number | undefined) => void;
}

const AutocompletePicker: React.FC<AutocompleteProps> = ({ label, icon, selectedId, suggestions, onSelect }) => {
    const [isOpen, setIsOpen] = useState(false);
    const [inputValue, setInputValue] = useState('');
    const containerRef = useRef<HTMLDivElement>(null);

    const selectedName = suggestions.find(s => s.id === selectedId)?.name;

    const filtered = suggestions.filter(s =>
        s.name.toLowerCase().includes((inputValue || '').toLowerCase())
    );

    useEffect(() => {
        const handleClickOutside = (e: MouseEvent) => {
            if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
                setIsOpen(false);
            }
        };
        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    return (
        <div className="relative space-y-1" ref={containerRef}>
            <label className="text-[9px] font-black text-gray-300 uppercase tracking-[0.2em] ml-2">{label}</label>
            <div className="relative group">
                <div className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-300 group-hover:text-[#276db8] transition-colors">
                    {icon}
                </div>
                <input
                    type="text"
                    className={`
            w-full bg-gray-50 border border-gray-100 rounded-2xl pl-12 pr-10 py-3 text-sm font-bold text-gray-700 outline-none 
            focus:bg-white focus:border-[#276db8] focus:ring-4 focus:ring-[#276db8]/5 transition-all
            ${selectedId ? 'border-2 border-[#89cbb6] bg-white' : ''}
          `}
                    placeholder={selectedName || `검색하여 선택...`}
                    value={inputValue}
                    onChange={(e) => {
                        setInputValue(e.target.value);
                        setIsOpen(true);
                        if (selectedId) onSelect(undefined);
                    }}
                    onFocus={() => setIsOpen(true)}
                />
                {selectedId ? (
                    <button
                        type="button"
                        onClick={() => { setInputValue(''); onSelect(undefined); }}
                        className="absolute right-4 top-1/2 -translate-y-1/2 text-[#89cbb6] hover:text-red-400 transition-colors"
                    >
                        <TbTag size={18} />
                    </button>
                ) : null}
            </div>

            {/* Suggestion Dropdown */}
            {isOpen && filtered.length > 0 && (
                <div className="absolute z-50 top-full left-0 right-0 mt-2 bg-white border border-gray-100 rounded-2xl shadow-xl overflow-hidden animate-in fade-in slide-in-from-top-2 duration-200">
                    {filtered.map(item => (
                        <button
                            key={item.id}
                            type="button"
                            onClick={() => {
                                onSelect(item.id);
                                setInputValue('');
                                setIsOpen(false);
                            }}
                            className="w-full text-left px-5 py-3 text-sm font-bold text-gray-600 hover:bg-gray-50 hover:text-[#276db8] flex justify-between items-center transition-colors"
                        >
                            <span>{item.name}</span>
                            <span className="text-[10px] text-gray-300 font-mono">ID: {item.id}</span>
                        </button>
                    ))}
                </div>
            )}
        </div>
    );
};
