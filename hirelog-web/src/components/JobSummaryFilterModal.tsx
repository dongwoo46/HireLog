import React, { useEffect, useMemo, useState } from 'react';
import { TbBriefcase, TbBuilding, TbCategory, TbSearch, TbUsers, TbWorld } from 'react-icons/tb';
import { jdSummaryService, type SearchOptionItem } from '../services/jdSummaryService';
import type { JobSummarySearchReq } from '../types/jobSummary';
import { Modal } from './common/Modal';

interface JobSummaryFilterModalProps {
  isOpen: boolean;
  onClose: () => void;
  filters: JobSummarySearchReq;
  onApply: (filters: JobSummarySearchReq) => void;
  onReset: () => void;
}

const normalizeList = (values?: string[]) =>
  (values || [])
    .map((value) => value.trim())
    .filter(Boolean)
    .filter((value, index, array) => array.indexOf(value) === index);

const COMPANY_DOMAIN_OPTIONS = [
  { value: 'FINTECH', label: '핀테크' },
  { value: 'E_COMMERCE', label: '이커머스' },
  { value: 'FOOD_DELIVERY', label: '배달/음식' },
  { value: 'LOGISTICS', label: '물류/배송' },
  { value: 'MOBILITY', label: '모빌리티' },
  { value: 'HEALTHCARE', label: '헬스케어' },
  { value: 'EDTECH', label: '에듀테크' },
  { value: 'GAME', label: '게임' },
  { value: 'MEDIA_CONTENT', label: '미디어/콘텐츠' },
  { value: 'SOCIAL_COMMUNITY', label: '소셜/커뮤니티' },
  { value: 'TRAVEL_ACCOMMODATION', label: '여행/숙박' },
  { value: 'REAL_ESTATE', label: '부동산' },
  { value: 'HR_RECRUITING', label: 'HR/채용' },
  { value: 'AD_MARKETING', label: '광고/마케팅' },
  { value: 'AI_ML', label: 'AI/ML' },
  { value: 'CLOUD_INFRA', label: '클라우드/인프라' },
  { value: 'SECURITY', label: '보안' },
  { value: 'ENTERPRISE_SW', label: '엔터프라이즈 SW' },
  { value: 'BLOCKCHAIN_CRYPTO', label: '블록체인/크립토' },
  { value: 'MANUFACTURING_IOT', label: '제조/IoT' },
  { value: 'PUBLIC_SECTOR', label: '공공' },
  { value: 'OTHER', label: '기타' },
] as const;

const COMPANY_SIZE_OPTIONS = [
  { value: 'SEED', label: '시드 스타트업' },
  { value: 'EARLY_STARTUP', label: '초기 스타트업' },
  { value: 'GROWTH_STARTUP', label: '성장 스타트업' },
  { value: 'SCALE_UP', label: '스케일업' },
  { value: 'MID_SIZED', label: '중소/중견기업' },
  { value: 'LARGE_CORP', label: '대기업' },
  { value: 'FOREIGN_CORP', label: '외국계' },
  { value: 'UNKNOWN', label: '확인불가' },
] as const;

export const JobSummaryFilterModal: React.FC<JobSummaryFilterModalProps> = ({
  isOpen,
  onClose,
  filters,
  onApply,
  onReset,
}) => {
  const [localFilters, setLocalFilters] = useState<JobSummarySearchReq>(filters);

  const [brandQuery, setBrandQuery] = useState('');
  const [categoryQuery, setCategoryQuery] = useState('');
  const [positionQuery, setPositionQuery] = useState('');
  const [techStackQuery, setTechStackQuery] = useState('');

  const [brandOptions, setBrandOptions] = useState<SearchOptionItem[]>([]);
  const [categoryOptions, setCategoryOptions] = useState<SearchOptionItem[]>([]);
  const [positionOptions, setPositionOptions] = useState<SearchOptionItem[]>([]);
  const [techStackOptions, setTechStackOptions] = useState<SearchOptionItem[]>([]);

  const [brandLoading, setBrandLoading] = useState(false);
  const [categoryLoading, setCategoryLoading] = useState(false);
  const [positionLoading, setPositionLoading] = useState(false);
  const [techStackLoading, setTechStackLoading] = useState(false);

  const selectedBrandNames = useMemo(
    () => normalizeList([...(localFilters.brandNames || []), localFilters.brandName || '']),
    [localFilters.brandNames, localFilters.brandName],
  );
  const selectedCategoryNames = useMemo(
    () => normalizeList([...(localFilters.positionCategoryNames || []), localFilters.positionCategoryName || '']),
    [localFilters.positionCategoryNames, localFilters.positionCategoryName],
  );
  const selectedPositionNames = useMemo(
    () => normalizeList([...(localFilters.positionNames || []), localFilters.positionName || '']),
    [localFilters.positionNames, localFilters.positionName],
  );
  const selectedTechStacks = useMemo(() => normalizeList(localFilters.techStacks), [localFilters.techStacks]);
  const selectedCompanyDomains = useMemo(
    () => normalizeList(localFilters.companyDomains),
    [localFilters.companyDomains],
  );
  const selectedCompanySizes = useMemo(
    () => normalizeList(localFilters.companySizes),
    [localFilters.companySizes],
  );

  useEffect(() => {
    if (!isOpen) return;
    setLocalFilters(filters);
    setBrandQuery('');
    setCategoryQuery('');
    setPositionQuery('');
    setTechStackQuery('');
  }, [isOpen, filters]);

  useEffect(() => {
    if (!isOpen) return;
    const timer = setTimeout(async () => {
      try {
        setBrandLoading(true);
        const items = await jdSummaryService.searchBrands(brandQuery || undefined);
        setBrandOptions(items);
      } catch {
        setBrandOptions([]);
      } finally {
        setBrandLoading(false);
      }
    }, 250);
    return () => clearTimeout(timer);
  }, [isOpen, brandQuery]);

  useEffect(() => {
    if (!isOpen) return;
    const timer = setTimeout(async () => {
      try {
        setCategoryLoading(true);
        const items = await jdSummaryService.searchCategories(categoryQuery || undefined);
        setCategoryOptions(items);
      } catch {
        setCategoryOptions([]);
      } finally {
        setCategoryLoading(false);
      }
    }, 250);
    return () => clearTimeout(timer);
  }, [isOpen, categoryQuery]);

  useEffect(() => {
    if (!isOpen) return;
    const timer = setTimeout(async () => {
      try {
        setPositionLoading(true);
        const items = await jdSummaryService.searchPositions(positionQuery || undefined);
        setPositionOptions(items);
      } catch {
        setPositionOptions([]);
      } finally {
        setPositionLoading(false);
      }
    }, 250);
    return () => clearTimeout(timer);
  }, [isOpen, positionQuery]);

  useEffect(() => {
    if (!isOpen) return;
    const timer = setTimeout(async () => {
      try {
        setTechStackLoading(true);
        const items = await jdSummaryService.searchTechStacks(techStackQuery || undefined);
        setTechStackOptions(items);
      } catch {
        setTechStackOptions([]);
      } finally {
        setTechStackLoading(false);
      }
    }, 250);
    return () => clearTimeout(timer);
  }, [isOpen, techStackQuery]);

  const toggleStringList = (
    key:
      | 'brandNames'
      | 'positionCategoryNames'
      | 'positionNames'
      | 'techStacks'
      | 'companyDomains'
      | 'companySizes',
    value: string,
  ) => {
    const target = value.trim();
    if (!target) return;

    setLocalFilters((prev) => {
      const current = normalizeList((prev[key] as string[] | undefined) || []);
      const next = current.includes(target) ? current.filter((item) => item !== target) : [...current, target];

      const clearedSingle = (() => {
        switch (key) {
          case 'brandNames':
            return { brandName: undefined };
          case 'positionCategoryNames':
            return { positionCategoryName: undefined };
          case 'positionNames':
            return { positionName: undefined };
          default:
            return {};
        }
      })();

      return {
        ...prev,
        ...clearedSingle,
        [key]: next.length > 0 ? next : undefined,
      };
    });
  };

  const handleApply = () => {
    const payload: JobSummarySearchReq = {
      ...localFilters,
      brandName: undefined,
      positionCategoryName: undefined,
      positionName: undefined,
      brandNames: selectedBrandNames.length > 0 ? selectedBrandNames : undefined,
      positionCategoryNames: selectedCategoryNames.length > 0 ? selectedCategoryNames : undefined,
      positionNames: selectedPositionNames.length > 0 ? selectedPositionNames : undefined,
      techStacks: selectedTechStacks.length > 0 ? selectedTechStacks : undefined,
      companyDomains: selectedCompanyDomains.length > 0 ? selectedCompanyDomains : undefined,
      companySizes: selectedCompanySizes.length > 0 ? selectedCompanySizes : undefined,
    };

    onApply(payload);
    onClose();
  };

  const handleReset = () => {
    onReset();
    setLocalFilters({
      keyword: localFilters.keyword,
      careerType: localFilters.careerType,
      sortBy: 'CREATED_AT_DESC',
    });
    setBrandQuery('');
    setCategoryQuery('');
    setPositionQuery('');
    setTechStackQuery('');
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="상세 필터 설정" maxWidth="max-w-5xl">
      <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
        <FilterSection title="기업 / 브랜드" icon={<TbBuilding />}>
          <SearchableMultiPicker
            placeholder="브랜드 검색"
            query={brandQuery}
            onQueryChange={setBrandQuery}
            selectedValues={selectedBrandNames}
            options={brandOptions}
            loading={brandLoading}
            onToggle={(name) => toggleStringList('brandNames', name)}
          />
        </FilterSection>

        <FilterSection title="직무 카테고리" icon={<TbCategory />}>
          <SearchableMultiPicker
            placeholder="직무 카테고리 검색"
            query={categoryQuery}
            onQueryChange={setCategoryQuery}
            selectedValues={selectedCategoryNames}
            options={categoryOptions}
            loading={categoryLoading}
            onToggle={(name) => toggleStringList('positionCategoryNames', name)}
          />
        </FilterSection>

        <FilterSection title="포지션" icon={<TbBriefcase />}>
          <SearchableMultiPicker
            placeholder="포지션 검색"
            query={positionQuery}
            onQueryChange={setPositionQuery}
            selectedValues={selectedPositionNames}
            options={positionOptions}
            loading={positionLoading}
            onToggle={(name) => toggleStringList('positionNames', name)}
          />
        </FilterSection>

        <FilterSection title="기술 스택" icon={<TbSearch />}>
          <SearchableMultiPicker
            placeholder="기술 스택 검색"
            query={techStackQuery}
            onQueryChange={setTechStackQuery}
            selectedValues={selectedTechStacks}
            options={techStackOptions}
            loading={techStackLoading}
            onToggle={(name) => toggleStringList('techStacks', name)}
          />
        </FilterSection>

        <FilterSection title="회사 도메인" icon={<TbWorld />}>
          <StaticMultiPicker
            options={COMPANY_DOMAIN_OPTIONS}
            selectedValues={selectedCompanyDomains}
            onToggle={(value) => toggleStringList('companyDomains', value)}
          />
        </FilterSection>

        <FilterSection title="회사 규모" icon={<TbUsers />}>
          <StaticMultiPicker
            options={COMPANY_SIZE_OPTIONS}
            selectedValues={selectedCompanySizes}
            onToggle={(value) => toggleStringList('companySizes', value)}
          />
        </FilterSection>
      </div>

      <div className="mt-8 border-t border-gray-100 pt-5">
        <div className="grid grid-cols-3 gap-2 sm:flex sm:flex-wrap sm:gap-3">
          <SortButton
            active={localFilters.sortBy === 'CREATED_AT_DESC'}
            onClick={() => setLocalFilters((prev) => ({ ...prev, sortBy: 'CREATED_AT_DESC' }))}
            mobileLabel="최신순"
          >
            최신순
          </SortButton>
          <SortButton
            active={localFilters.sortBy === 'CREATED_AT_ASC'}
            onClick={() => setLocalFilters((prev) => ({ ...prev, sortBy: 'CREATED_AT_ASC' }))}
            mobileLabel="오래된순"
          >
            오래된순
          </SortButton>
          <SortButton
            active={localFilters.sortBy === 'SAVE_COUNT_DESC'}
            onClick={() => setLocalFilters((prev) => ({ ...prev, sortBy: 'SAVE_COUNT_DESC' }))}
            mobileLabel="북마크순"
          >
            북마크순
          </SortButton>
        </div>

        <div className="mt-3 grid grid-cols-2 gap-2 sm:mt-4 sm:flex sm:items-center sm:justify-end">
          <button
            type="button"
            onClick={handleReset}
            className="h-10 rounded-lg border border-gray-200 px-3 text-sm font-semibold text-gray-600 transition hover:bg-gray-50 sm:px-4"
          >
            필터 초기화
          </button>
          <button
            type="button"
            onClick={handleApply}
            className="h-10 rounded-lg bg-[#4CDFD5] px-3 text-sm font-semibold text-white transition hover:bg-[#3CCFC5] sm:px-4"
          >
            필터 적용
          </button>
        </div>
      </div>
    </Modal>
  );
};

const SearchableMultiPicker = ({
  placeholder,
  query,
  onQueryChange,
  selectedValues,
  options,
  loading,
  onToggle,
}: {
  placeholder: string;
  query: string;
  onQueryChange: (value: string) => void;
  selectedValues: string[];
  options: SearchOptionItem[];
  loading: boolean;
  onToggle: (name: string) => void;
}) => (
  <div className="space-y-3">
    <FilterInput placeholder={placeholder} value={query} onChange={onQueryChange} />

    <div className="max-h-28 overflow-y-auto rounded-lg border border-gray-100 bg-gray-50 p-2">
      {loading ? (
        <div className="px-2 py-1 text-xs text-gray-400">검색 중...</div>
      ) : options.length === 0 ? (
        <div className="px-2 py-1 text-xs text-gray-400">결과가 없습니다.</div>
      ) : (
        <div className="flex flex-wrap gap-2">
          {options.map((option) => {
            const selected = selectedValues.includes(option.name);
            return (
              <button
                key={option.id}
                type="button"
                onClick={() => onToggle(option.name)}
                className={`rounded-lg border px-3 py-1.5 text-xs font-semibold transition ${
                  selected
                    ? 'border-[#3FB6B2] bg-[#3FB6B2]/10 text-[#2b8f8c]'
                    : 'border-gray-200 bg-white text-gray-600 hover:border-[#3FB6B2]/40 hover:text-[#2b8f8c]'
                }`}
              >
                {option.name}
              </button>
            );
          })}
        </div>
      )}
    </div>

    {selectedValues.length > 0 && (
      <div className="flex flex-wrap gap-2">
        {selectedValues.map((value) => (
          <button
            key={value}
            type="button"
            onClick={() => onToggle(value)}
            className="rounded-lg border border-[#3FB6B2] bg-[#3FB6B2]/10 px-3 py-1 text-xs font-semibold text-[#2b8f8c]"
          >
            {value}
          </button>
        ))}
      </div>
    )}
  </div>
);

const StaticMultiPicker = ({
  options,
  selectedValues,
  onToggle,
}: {
  options: ReadonlyArray<{ value: string; label: string }>;
  selectedValues: string[];
  onToggle: (value: string) => void;
}) => (
  <div className="space-y-3">
    <div className="flex flex-wrap gap-2">
      {options.map((option) => {
        const selected = selectedValues.includes(option.value);
        return (
          <button
            key={option.value}
            type="button"
            onClick={() => onToggle(option.value)}
            className={`rounded-lg border px-3 py-1.5 text-xs font-semibold transition ${
              selected
                ? 'border-[#3FB6B2] bg-[#3FB6B2]/10 text-[#2b8f8c]'
                : 'border-gray-200 bg-white text-gray-600 hover:border-[#3FB6B2]/40 hover:text-[#2b8f8c]'
            }`}
          >
            {option.label}
          </button>
        );
      })}
    </div>

    {selectedValues.length > 0 && (
      <div className="flex flex-wrap gap-2">
        {selectedValues.map((value) => {
          const label = options.find((option) => option.value === value)?.label || value;
          return (
            <button
              key={value}
              type="button"
              onClick={() => onToggle(value)}
              className="rounded-lg border border-[#3FB6B2] bg-[#3FB6B2]/10 px-3 py-1 text-xs font-semibold text-[#2b8f8c]"
            >
              {label}
            </button>
          );
        })}
      </div>
    )}
  </div>
);

const FilterSection = ({
  title,
  icon,
  children,
}: {
  title: string;
  icon: React.ReactNode;
  children: React.ReactNode;
}) => (
  <div className="space-y-3">
    <h3 className="flex items-center gap-2 text-sm font-bold text-[#3FB6B2]">
      {icon}
      {title}
    </h3>
    {children}
  </div>
);

const FilterInput = ({
  placeholder,
  value,
  onChange,
}: {
  placeholder: string;
  value: string;
  onChange: (value: string) => void;
}) => (
  <div className="relative">
    <TbSearch className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={16} />
    <input
      type="text"
      value={value}
      onChange={(e) => onChange(e.target.value)}
      placeholder={placeholder}
      className="w-full rounded-lg border border-gray-200 bg-white py-2.5 pl-10 pr-3 text-sm outline-none focus:border-[#3FB6B2]"
    />
  </div>
);

const SortButton = ({
  active,
  onClick,
  mobileLabel,
  children,
}: {
  active: boolean;
  onClick: () => void;
  mobileLabel?: string;
  children: React.ReactNode;
}) => (
  <button
    type="button"
    onClick={onClick}
    className={`inline-flex h-10 w-full items-center justify-center whitespace-nowrap rounded-lg px-2 text-[11px] font-semibold leading-tight transition sm:h-auto sm:w-auto sm:px-4 sm:py-2 sm:text-sm sm:leading-normal ${
      active ? 'bg-[#3FB6B2] text-white' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
    }`}
  >
    {mobileLabel ? (
      <>
        <span className="sm:hidden">{mobileLabel}</span>
        <span className="hidden sm:inline">{children}</span>
      </>
    ) : (
      children
    )}
  </button>
);
