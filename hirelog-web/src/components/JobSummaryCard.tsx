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
      className="flex h-full cursor-pointer flex-col justify-between rounded-2xl border border-gray-100 bg-white p-6 transition-all duration-300 hover:-translate-y-1 hover:shadow-lg"
    >
      <div>
        <div className="mb-4 flex items-start justify-between">
          <div className="flex flex-col gap-2">
            <h3 className="text-lg font-bold text-gray-900">{summary.brandPositionName}</h3>
            <span className="text-sm font-medium text-gray-500">{summary.brandName}</span>
          </div>

          <button
            onClick={handleBookmark}
            className="text-gray-300 transition-colors hover:text-[#4CDFD5]"
            title={isAuthenticated ? '저장' : '로그인 필요'}
          >
            {isSaved ? <TbBookmarkFilled size={22} className="text-[#4CDFD5]" /> : <TbBookmark size={22} />}
          </button>
        </div>

        <div className="mb-3 flex flex-wrap gap-2">
          <span className="rounded-md bg-[#4CDFD5]/10 px-2 py-1 text-xs font-semibold text-[#4CDFD5]">
            {summary.careerType === 'NEW' ? '신입' : summary.careerType === 'EXPERIENCED' ? '경력' : '무관'}
          </span>
        </div>

        <div className="flex flex-wrap gap-2">
          {(summary.techStackParsed || []).slice(0, 3).map((tech, idx) => (
            <span key={idx} className="rounded-md bg-gray-50 px-2 py-1 text-xs text-gray-600">
              {tech}
            </span>
          ))}

          {(!summary.techStackParsed || summary.techStackParsed.length === 0) && (
            <span className="rounded-md bg-gray-50 px-2 py-1 text-xs text-gray-400">General</span>
          )}
        </div>
      </div>

      <div className="mt-6 flex items-center justify-between border-t border-gray-100 pt-4 text-xs text-gray-400">
        <span>{summary.createdAt?.slice(0, 10).replace(/-/g, '.') || '2024.01.01'}</span>
        <span>리뷰 0</span>
      </div>
    </div>
  );
};

