import React, { useState } from 'react';
import type { JobSummaryView } from '../types/jobSummary';
import { useNavigate } from 'react-router-dom';
import { TbBookmark, TbBookmarkFilled } from 'react-icons/tb';
import { jdSummaryService } from '../services/jdSummaryService';
import { toast } from 'react-toastify';

interface Props {
    summary: JobSummaryView;
}

export const JobSummaryCompactCard: React.FC<Props> = ({ summary }) => {
    const navigate = useNavigate();
    const [isSaved, setIsSaved] = useState(summary.isSaved || false);
    const [isSaving, setIsSaving] = useState(false);

    const handleBookmark = async (e: React.MouseEvent) => {
        e.stopPropagation();
        if (isSaving) return;

        setIsSaving(true);
        try {
            if (isSaved) {
                await jdSummaryService.unsave(summary.id);
                setIsSaved(false);
                toast.info('북마크가 해제되었습니다.');
            } else {
                await jdSummaryService.save(summary);
                setIsSaved(true);
                toast.success('북마크에 저장되었습니다.');
            }
        } catch (error) {
            console.error('Failed to update bookmark', error);
            toast.error('북마크 저장 중 오류가 발생했습니다.');
        } finally {
            setIsSaving(false);
        }
    };

    return (
        <div
            onClick={() => navigate(`/jd/${summary.id}`)}
            className="bg-white rounded-2xl p-6 shadow-sm border border-gray-100 hover:shadow-md transition-all cursor-pointer group flex flex-col gap-4 relative"
        >
            <div className="flex justify-between items-start">
                <h3 className="text-xl font-bold text-gray-900 uppercase tracking-tight">
                    {summary.brandName}
                </h3>
                <span className="text-xs text-gray-300 font-medium whitespace-nowrap">
                    {summary.createdAt?.slice(0, 10).replace(/-/g, '.') || '2024.01.01'}
                </span>
            </div>

            <div className="flex flex-col gap-2">
                <h4 className="text-base text-gray-500 font-medium">
                    {summary.brandPositionName}
                </h4>
                <div className="flex gap-2 mt-1">
                    {(summary.techStackParsed || []).slice(0, 3).map((tech: string, idx: number) => (
                        <span
                            key={idx}
                            className="px-3 py-1 bg-[#4CDFD5]/5 border border-[#4CDFD5]/20 text-[#4CDFD5] text-[11px] font-bold rounded-lg"
                        >
                            {tech}
                        </span>
                    ))}
                    {(!summary.techStackParsed || summary.techStackParsed.length === 0) && (
                        <span className="px-3 py-1 border border-gray-100 text-gray-300 text-[11px] font-semibold rounded-lg">
                            Detailed
                        </span>
                    )}
                </div>
            </div>

            <div className="mt-4 pt-4 border-t border-gray-50 flex items-center justify-between">
                <span className="text-xs font-semibold text-gray-400">
                    리뷰 0개
                </span>
                <button
                    onClick={handleBookmark}
                    className={`transition-colors p-1 ${isSaved ? 'text-[#4CDFD5]' : 'text-gray-200 hover:text-[#4CDFD5]'}`}
                    disabled={isSaving}
                >
                    {isSaved ? <TbBookmarkFilled size={20} /> : <TbBookmark size={20} />}
                </button>
            </div>
        </div>
    );
};
