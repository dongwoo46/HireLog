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
      toast.info('저장 기능은 로그인 후 사용할 수 있어요.');
      navigate('/login');
      return;
    }

    setIsSaving(true);
    try {
      if (isSaved) {
        await jdSummaryService.unsave(summary.id);
        setIsSaved(false);
        toast.info('저장을 해제했습니다.');
      } else {
        await jdSummaryService.save(summary);
        setIsSaved(true);
        toast.success('공고를 저장했습니다.');
      }
    } catch (error) {
      console.error('Failed to update bookmark', error);
      toast.error('저장 상태 변경 중 오류가 발생했습니다.');
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

        <div className="mb-2 flex flex-wrap gap-1 sm:mb-3 sm:gap-2">
          <span className="rounded-md bg-[#4CDFD5]/10 px-1.5 py-0.5 text-[10px] font-semibold text-[#4CDFD5] sm:px-2 sm:py-1 sm:text-xs">
            {summary.careerType === 'NEW' ? '신입' : summary.careerType === 'EXPERIENCED' ? '경력' : '무관'}
          </span>
        </div>

        <div className="flex flex-wrap gap-1 sm:gap-2">
          {(summary.techStackParsed || []).slice(0, 3).map((tech, idx) => (
            <span key={idx} className="rounded-md bg-gray-50 px-1.5 py-0.5 text-[10px] text-gray-600 sm:px-2 sm:py-1 sm:text-xs">
              {tech}
            </span>
          ))}

          {(!summary.techStackParsed || summary.techStackParsed.length === 0) && (
            <span className="rounded-md bg-gray-50 px-1.5 py-0.5 text-[10px] text-gray-400 sm:px-2 sm:py-1 sm:text-xs">General</span>
          )}
        </div>
      </div>

      <div className="mt-3 flex items-center justify-end border-t border-gray-100 pt-2 text-[10px] text-gray-400 sm:mt-6 sm:pt-4 sm:text-xs">
        <span>{summary.createdAt?.slice(0, 10).replace(/-/g, '.') || '2024.01.01'}</span>
      </div>
    </div>
  );
};
