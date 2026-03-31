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

export const JobSummaryCompactCard: React.FC<Props> = ({ summary }) => {
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
      className="relative flex cursor-pointer flex-col gap-4 rounded-2xl border border-gray-100 bg-white p-6 shadow-sm transition-all hover:shadow-md"
    >
      <div className="flex items-start justify-between">
        <h3 className="text-xl font-bold tracking-tight text-gray-900">{summary.brandName}</h3>
        <span className="whitespace-nowrap text-xs font-medium text-gray-300">
          {summary.createdAt?.slice(0, 10).replace(/-/g, '.') || '2024.01.01'}
        </span>
      </div>

      <div className="flex flex-col gap-2">
        <h4 className="text-base font-medium text-gray-500">{summary.brandPositionName}</h4>
        <div className="mt-1 flex gap-2">
          {(summary.techStackParsed || []).slice(0, 3).map((tech: string, idx: number) => (
            <span
              key={idx}
              className="rounded-lg border border-[#4CDFD5]/20 bg-[#4CDFD5]/5 px-3 py-1 text-[11px] font-bold text-[#4CDFD5]"
            >
              {tech}
            </span>
          ))}
          {(!summary.techStackParsed || summary.techStackParsed.length === 0) && (
            <span className="rounded-lg border border-gray-100 px-3 py-1 text-[11px] font-semibold text-gray-300">Detailed</span>
          )}
        </div>
      </div>

      <div className="mt-4 flex items-center justify-end border-t border-gray-50 pt-4">
        <button
          onClick={handleBookmark}
          className={`p-1 transition-colors ${isSaved ? 'text-[#4CDFD5]' : 'text-gray-200 hover:text-[#4CDFD5]'}`}
          disabled={isSaving}
          title={isAuthenticated ? '저장' : '로그인 필요'}
        >
          {isSaved ? <TbBookmarkFilled size={20} /> : <TbBookmark size={20} />}
        </button>
      </div>
    </div>
  );
};

