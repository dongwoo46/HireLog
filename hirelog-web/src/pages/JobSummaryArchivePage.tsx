import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { jdSummaryService } from '../services/jdSummaryService';
import { JobSummaryCard } from '../components/JobSummaryCard';
import type { HiringStage, HiringStageResult, JobSummaryView } from '../types/jobSummary';
import { HIRING_STAGE_LABELS, HIRING_STAGE_RESULT_LABELS } from '../types/jobSummary';
import { TbAdjustmentsHorizontal, TbBookmark, TbChevronLeft, TbSearch, TbSend } from 'react-icons/tb';
import { Modal } from '../components/common/Modal';
import { useMyJobsFilterStore } from '../store/myJobsFilterStore';

const STAGE_OPTIONS = Object.entries(HIRING_STAGE_LABELS) as Array<[HiringStage, string]>;
const RESULT_OPTIONS: HiringStageResult[] = ['PASSED', 'FAILED', 'PENDING'];

const JobSummaryArchivePage = () => {
  const navigate = useNavigate();
  const {
    activeTab,
    brandQuery,
    searchBrandName,
    stage,
    result,
    setActiveTab,
    setBrandQuery,
    setSearchBrandName,
    setStage,
    setResult,
  } = useMyJobsFilterStore();
  const [items, setItems] = useState<JobSummaryView[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  const [isFilterOpen, setIsFilterOpen] = useState(false);
  const [draftStage, setDraftStage] = useState<HiringStage | undefined>(undefined);
  const [draftResult, setDraftResult] = useState<HiringStageResult | undefined>(undefined);

  useEffect(() => {
    const timer = setTimeout(() => {
      setSearchBrandName(brandQuery.trim());
    }, 250);

    return () => clearTimeout(timer);
  }, [brandQuery]);

  useEffect(() => {
    setIsLoading(true);
    const timer = setTimeout(async () => {
      try {
        if (activeTab === 'SAVED') {
          const data = await jdSummaryService.getMySaves(0, 50, searchBrandName || undefined);
          setItems(data.items || []);
        } else {
          const data = await jdSummaryService.getMyApplies(
            0,
            50,
            searchBrandName || undefined,
            stage,
            result,
          );
          setItems(data.items || []);
        }
      } catch (error) {
        console.error('Failed to fetch my jobs', error);
      } finally {
        setIsLoading(false);
      }
    }, 200);

    return () => clearTimeout(timer);
  }, [activeTab, searchBrandName, stage, result]);

  useEffect(() => {
    if (!isFilterOpen) return;
    setDraftStage(stage);
    setDraftResult(result);
  }, [isFilterOpen, stage, result]);

  const filterLabel = useMemo(() => {
    if (!stage && !result) return '필터 없음';
    const stageText = stage ? HIRING_STAGE_LABELS[stage] : '전체 단계';
    const resultText = result ? HIRING_STAGE_RESULT_LABELS[result] : '전체 상태';
    return `${stageText} / ${resultText}`;
  }, [stage, result]);

  return (
    <div className="min-h-screen bg-[#F8F9FA] pb-20 pt-24 px-6">
      <div className="mx-auto max-w-6xl">
        <button
          onClick={() => navigate('/profile')}
          className="group mb-8 flex items-center gap-2 text-gray-400 transition-all hover:text-gray-700"
        >
          <TbChevronLeft size={20} className="transition-transform group-hover:-translate-x-1" />
          <span className="font-semibold">프로필로 돌아가기</span>
        </button>

        <div className="mb-6">
          <h1 className="text-2xl font-bold text-gray-900">내 공고</h1>
          <p className="mt-1 text-sm text-gray-500">저장한 공고와 지원한 공고를 분리해서 관리할 수 있습니다.</p>
        </div>

        <div className="mb-5 flex border-b border-gray-200">
          <TabButton
            active={activeTab === 'SAVED'}
            onClick={() => setActiveTab('SAVED')}
            icon={<TbBookmark size={18} />}
            label="저장한 공고"
          />
          <TabButton
            active={activeTab === 'APPLY'}
            onClick={() => setActiveTab('APPLY')}
            icon={<TbSend size={18} />}
            label="지원한 공고"
          />
        </div>

        <div className="mb-5 flex flex-wrap items-center gap-3">
          <div className="relative w-full max-w-sm">
            <TbSearch className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={16} />
            <input
              value={brandQuery}
              onChange={(e) => setBrandQuery(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter') {
                  e.preventDefault();
                  setSearchBrandName(brandQuery.trim());
                }
              }}
              placeholder="토스, 네이버"
              className="w-full rounded-lg border border-gray-200 bg-white py-2.5 pl-9 pr-3 text-sm outline-none focus:border-[#3FB6B2]"
            />
          </div>

          {activeTab === 'APPLY' && (
            <button
              type="button"
              onClick={() => setIsFilterOpen(true)}
              className="inline-flex items-center gap-2 rounded-lg border border-gray-200 bg-white px-4 py-2.5 text-sm font-semibold text-gray-700 transition hover:border-[#3FB6B2]/40 hover:text-[#2b8f8c]"
            >
              <TbAdjustmentsHorizontal size={16} />
              필터
            </button>
          )}
        </div>

        {activeTab === 'APPLY' && (
          <p className="mb-6 text-xs text-gray-500">선택된 필터: {filterLabel}</p>
        )}

        {isLoading ? (
          <div className="space-y-4">
            {[...Array(3)].map((_, i) => (
              <div key={i} className="h-32 animate-pulse rounded-xl border border-gray-100 bg-white" />
            ))}
          </div>
        ) : items.length > 0 ? (
          <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
            {items.map((item) => (
              <JobSummaryCard key={`${item.id}-${item.memberSaveType}`} summary={item} />
            ))}
          </div>
        ) : (
          <div className="rounded-3xl border border-dashed border-gray-200 bg-white py-28 text-center">
            <p className="text-gray-400">
              {activeTab === 'SAVED' ? '저장한 공고가 없습니다.' : '조건에 맞는 지원 공고가 없습니다.'}
            </p>
          </div>
        )}
      </div>

      <Modal isOpen={isFilterOpen} onClose={() => setIsFilterOpen(false)} title="지원한 공고 필터" maxWidth="max-w-md">
        <div className="space-y-5">
          <div className="space-y-2">
            <label className="text-sm font-semibold text-gray-700">Stage</label>
            <select
              value={draftStage || ''}
              onChange={(e) => setDraftStage((e.target.value || undefined) as HiringStage | undefined)}
              className="w-full rounded-lg border border-gray-200 bg-white px-3 py-2.5 text-sm outline-none focus:border-[#3FB6B2]"
            >
              <option value="">전체 단계</option>
              {STAGE_OPTIONS.map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </select>
          </div>

          <div className="space-y-2">
            <label className="text-sm font-semibold text-gray-700">상태</label>
            <select
              value={draftResult || ''}
              onChange={(e) => setDraftResult((e.target.value || undefined) as HiringStageResult | undefined)}
              className="w-full rounded-lg border border-gray-200 bg-white px-3 py-2.5 text-sm outline-none focus:border-[#3FB6B2]"
            >
              <option value="">전체 상태</option>
              {RESULT_OPTIONS.map((value) => (
                <option key={value} value={value}>
                  {HIRING_STAGE_RESULT_LABELS[value]}
                </option>
              ))}
            </select>
          </div>

          <div className="flex items-center justify-between pt-2">
            <button
              type="button"
              onClick={() => {
                setDraftStage(undefined);
                setDraftResult(undefined);
                setStage(undefined);
                setResult(undefined);
                setIsFilterOpen(false);
              }}
              className="text-sm font-semibold text-gray-400 transition hover:text-rose-400"
            >
              초기화
            </button>
            <button
              type="button"
              onClick={() => {
                setStage(draftStage);
                setResult(draftResult);
                setIsFilterOpen(false);
              }}
              className="rounded-lg bg-[#3FB6B2] px-4 py-2 text-sm font-semibold text-white transition hover:bg-[#35a09d]"
            >
              적용
            </button>
          </div>
        </div>
      </Modal>
    </div>
  );
};

const TabButton = ({
  active,
  onClick,
  icon,
  label,
}: {
  active: boolean;
  onClick: () => void;
  icon: React.ReactNode;
  label: string;
}) => (
  <button
    onClick={onClick}
    className={`flex items-center gap-2 border-b-2 px-6 py-4 text-sm font-semibold transition-all ${
      active ? 'border-[#4CDFD5] text-[#0f172a]' : 'border-transparent text-gray-400 hover:text-gray-600'
    }`}
  >
    {icon}
    {label}
  </button>
);

export default JobSummaryArchivePage;
