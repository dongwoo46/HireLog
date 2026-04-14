import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { TbBookmark, TbBookmarkFilled } from 'react-icons/tb';
import { toast } from 'react-toastify';
import { jdSummaryService } from '../services/jdSummaryService';
import { useAuthStore } from '../store/authStore';
import type { JobSummaryView } from '../types/jobSummary';

interface Props {
  summary: JobSummaryView;
}

const DOMAIN_LABELS: Record<string, string> = {
  FINTECH: '핀테크',
  E_COMMERCE: '이커머스',
  FOOD_DELIVERY: '푸드/배달',
  LOGISTICS: '물류/유통',
  MOBILITY: '모빌리티',
  HEALTHCARE: '헬스케어',
  EDTECH: '에듀테크',
  GAME: '게임',
  MEDIA_CONTENT: '미디어/콘텐츠',
  SOCIAL_COMMUNITY: '소셜/커뮤니티',
  TRAVEL_ACCOMMODATION: '여행/숙박',
  REAL_ESTATE: '부동산',
  HR_RECRUITING: 'HR/채용',
  AD_MARKETING: '광고/마케팅',
  AI_ML: 'AI/ML',
  CLOUD_INFRA: '클라우드/인프라',
  SECURITY: '보안',
  ENTERPRISE_SW: '엔터프라이즈 SW',
  BLOCKCHAIN_CRYPTO: '블록체인/크립토',
  MANUFACTURING_IOT: '제조/IoT',
  PUBLIC_SECTOR: '공공',
  OTHER: '기타',
};

const SIZE_LABELS: Record<string, string> = {
  SEED: '시드 단계',
  EARLY_STARTUP: '초기 스타트업',
  GROWTH_STARTUP: '성장 스타트업',
  SCALE_UP: '스케일업',
  MID_SIZED: '중견/중소 기업',
  LARGE_CORP: '대기업',
  FOREIGN_CORP: '외국계',
  UNKNOWN: '확인 불가',
};

const toDomainLabel = (value?: string) => (value ? DOMAIN_LABELS[value] ?? value : '-');
const toSizeLabel = (value?: string) => (value ? SIZE_LABELS[value] ?? value : '-');
const shouldShowSize = (value?: string) => value === 'LARGE_CORP' || value === 'UNKNOWN';

export const JobSummaryCard: React.FC<Props> = ({ summary }) => {
  const navigate = useNavigate();
  const { isAuthenticated } = useAuthStore();

  const [isSaved, setIsSaved] = useState(summary.isSaved || false);
  const [isSaving, setIsSaving] = useState(false);

  useEffect(() => {
    setIsSaved(Boolean(summary.isSaved));
  }, [summary.isSaved]);

  const handleBookmark = async (e: React.MouseEvent) => {
    e.stopPropagation();
    if (isSaving) return;

    if (!isAuthenticated) {
      toast.info('저장은 로그인 후 사용할 수 있어요.');
      navigate('/login');
      return;
    }

    setIsSaving(true);
    try {
      if (isSaved) {
        await jdSummaryService.unsave(summary.id);
        setIsSaved(false);
        toast.info('저장을 해제했어요.');
      } else {
        await jdSummaryService.save(summary);
        setIsSaved(true);
        toast.success('저장했어요.');
      }
    } catch (error) {
      console.error('Failed to update bookmark', error);
      toast.error('저장 상태 변경에 실패했어요.');
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <div
      onClick={() => navigate(`/jd/${summary.id}`)}
      className="flex h-full cursor-pointer flex-col justify-between rounded-xl border border-gray-100 bg-white p-3 transition-all duration-300 hover:-translate-y-1 hover:shadow-lg sm:rounded-2xl sm:p-6"
    >
      <div>
        <div className="mb-2 flex items-start justify-between sm:mb-4">
          <div className="flex min-w-0 flex-col gap-1 sm:gap-2">
            <h3 className="text-sm font-bold leading-tight text-gray-900 sm:text-lg">{summary.brandPositionName}</h3>
            <span className="truncate text-xs font-medium text-gray-500 sm:text-sm">{summary.brandName}</span>
          </div>

          <button
            onClick={handleBookmark}
            className="text-gray-300 transition-colors hover:text-[#4CDFD5]"
            title={isAuthenticated ? '저장' : '로그인 필요'}
          >
            {isSaved ? (
              <TbBookmarkFilled className="h-4 w-4 text-[#4CDFD5] sm:h-[22px] sm:w-[22px]" />
            ) : (
              <TbBookmark className="h-4 w-4 sm:h-[22px] sm:w-[22px]" />
            )}
          </button>
        </div>

        <div className="mb-2 flex flex-wrap gap-1.5 sm:mb-3 sm:gap-2">
          <span className="rounded-full bg-[#4CDFD5]/15 px-2 py-0.5 text-[10px] font-semibold text-[#2ca9a1] sm:px-2.5 sm:text-xs">
            {summary.careerType === 'NEW' ? '신입' : summary.careerType === 'EXPERIENCED' ? '경력' : '무관'}
          </span>
          <span className="rounded-full bg-indigo-50 px-2 py-0.5 text-[10px] font-medium text-indigo-600 sm:px-2.5 sm:text-xs">
            {toDomainLabel(summary.companyDomain)}
          </span>
          {shouldShowSize(summary.companySize) && (
            <span className="rounded-full bg-amber-50 px-2 py-0.5 text-[10px] font-medium text-amber-700 sm:px-2.5 sm:text-xs">
              {toSizeLabel(summary.companySize)}
            </span>
          )}
        </div>

        <div className="flex flex-wrap gap-1.5 sm:gap-2">
          {(summary.techStackParsed || []).slice(0, 2).map((tech, idx) => (
            <span
              key={idx}
              className="rounded-md border border-gray-100 bg-gray-50 px-1.5 py-0.5 text-[10px] text-gray-600 sm:px-2 sm:py-1 sm:text-xs"
            >
              {tech}
            </span>
          ))}

          {(summary.techStackParsed?.length || 0) > 2 && (
            <span className="rounded-md border border-gray-100 bg-white px-1.5 py-0.5 text-[10px] font-medium text-gray-500 sm:px-2 sm:py-1 sm:text-xs">
              +{(summary.techStackParsed?.length || 0) - 2}
            </span>
          )}

          {(!summary.techStackParsed || summary.techStackParsed.length === 0) && (
            <span className="rounded-md border border-dashed border-gray-200 bg-white px-1.5 py-0.5 text-[10px] text-gray-400 sm:px-2 sm:py-1 sm:text-xs">
              기술스택 정보 없음
            </span>
          )}
        </div>
      </div>

      <div className="mt-3 flex items-center justify-end border-t border-gray-100 pt-2 text-[10px] text-gray-400 sm:mt-6 sm:pt-4 sm:text-xs">
        <span>{summary.createdAt?.slice(0, 10).replace(/-/g, '.') || '2024.01.01'}</span>
      </div>
    </div>
  );
};
