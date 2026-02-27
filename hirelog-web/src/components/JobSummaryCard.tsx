// import React, { useState } from 'react';
// import type { JobSummaryView } from '../types/jobSummary';
// import { useNavigate } from 'react-router-dom';
// import { TbBookmark, TbBookmarkFilled } from 'react-icons/tb';
// import { jdSummaryService } from '../services/jdSummaryService';
// import { toast } from 'react-toastify';

// interface Props {
//   summary: JobSummaryView;
// }

// export const JobSummaryCard: React.FC<Props> = ({ summary }) => {
//   const navigate = useNavigate();
//   const [isSaved, setIsSaved] = useState(summary.isSaved || false);
//   const [isSaving, setIsSaving] = useState(false);

//   const handleBookmark = async (e: React.MouseEvent) => {
//     e.stopPropagation();
//     if (isSaving) return;

//     setIsSaving(true);
//     try {
//       if (isSaved) {
//         await jdSummaryService.unsave(summary.id);
//         setIsSaved(false);
//         toast.info('북마크가 해제되었습니다.');
//       } else {
//         await jdSummaryService.save(summary);
//         setIsSaved(true);
//         toast.success('북마크에 저장되었습니다.');
//       }
//     } catch (error) {
//       console.error('Failed to update bookmark', error);
//       toast.error('북마크 저장 중 오류가 발생했습니다.');
//     } finally {
//       setIsSaving(false);
//     }
//   };

//   // Wide layout for the list page
//   return (
//     <div
//       onClick={() => navigate(`/jd/${summary.id}`)}
//       className="bg-white rounded-[2rem] p-8 shadow-sm border border-gray-100 hover:shadow-md transition-all cursor-pointer group flex flex-col gap-4"
//     >
//       <div className="flex justify-between items-start">
//         <div className="flex items-center gap-3">
//           <h3 className="text-xl font-black text-gray-900 uppercase tracking-tight">
//             {summary.brandName}
//           </h3>
//           <span className="px-3 py-1 bg-[#4CDFD5]/10 text-[#4CDFD5] text-[10px] font-bold rounded-lg uppercase">
//             {summary.careerType === 'NEW' ? '신입' : summary.careerType === 'EXPERIENCED' ? '경력' : '무관'}
//           </span>
//           <span className="px-3 py-1 bg-blue-50 text-blue-400 text-[10px] font-bold rounded-lg uppercase">
//             지원함
//           </span>
//         </div>
//         <button
//           onClick={handleBookmark}
//           className="text-gray-300 hover:text-[#4CDFD5] transition-colors"
//         >
//           {isSaved ? <TbBookmarkFilled size={24} className="text-[#4CDFD5]" /> : <TbBookmark size={24} />}
//         </button>
//       </div>

//       <div className="flex flex-col gap-1">
//         <h4 className="text-lg font-medium text-gray-700">
//           {summary.brandPositionName}
//         </h4>
//         <div className="flex gap-2 mt-1">
//           {(summary.techStackParsed || []).slice(0, 3).map((tech: string, idx: number) => (
//             <span
//               key={idx}
//               className="px-3 py-1 border border-[#4CDFD5]/30 text-[#4CDFD5] text-[11px] font-medium rounded-lg"
//             >
//               {tech}
//             </span>
//           ))}
//           {(!summary.techStackParsed || summary.techStackParsed.length === 0) && (
//             <span className="px-3 py-1 border border-gray-100 text-gray-400 text-[11px] font-medium rounded-lg">
//               General
//             </span>
//           )}
//         </div>
//       </div>

//       <div className="mt-4 pt-6 border-t border-gray-50 flex items-center justify-between">
//         <div className="flex items-center gap-4 text-xs font-semibold text-gray-400">
//           <span>{summary.createdAt?.slice(0, 10).replace(/-/g, '.') || '2024.01.01'}</span>
//           <span>리뷰 0개</span>
//           <span>공개</span>
//         </div>

//         <div className="flex items-center gap-4">
//           <div className="w-16 h-6 bg-gray-50 rounded-lg" />
//           <span className="text-[10px] font-bold text-gray-300 uppercase">공개</span>
//         </div>
//       </div>
//     </div>
//   );
// };
import React, { useState } from 'react';
import type { JobSummaryView } from '../types/jobSummary';
import { useNavigate } from 'react-router-dom';
import { TbBookmark, TbBookmarkFilled } from 'react-icons/tb';
import { jdSummaryService } from '../services/jdSummaryService';
import { toast } from 'react-toastify';

interface Props {
  summary: JobSummaryView;
}

export const JobSummaryCard: React.FC<Props> = ({ summary }) => {
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
      className="
        bg-white 
        rounded-2xl 
        border 
        border-gray-100 
        p-6
        hover:shadow-lg 
        hover:-translate-y-1
        transition-all 
        duration-300
        cursor-pointer 
        flex 
        flex-col 
        justify-between
        h-full
      "
    >
      {/* 상단 */}
      <div>
        <div className="flex justify-between items-start mb-4">
          <div className="flex flex-col gap-2">
            <h3 className="text-lg font-bold text-gray-900">
              {summary.brandPositionName}
            </h3>

            <span className="text-sm text-gray-500 font-medium">
              {summary.brandName}
            </span>
          </div>

          <button
            onClick={handleBookmark}
            className="text-gray-300 hover:text-[#4CDFD5] transition-colors"
          >
            {isSaved ? (
              <TbBookmarkFilled size={22} className="text-[#4CDFD5]" />
            ) : (
              <TbBookmark size={22} />
            )}
          </button>
        </div>

        {/* 경력 타입 */}
        <div className="flex gap-2 flex-wrap mb-3">
          <span className="px-2 py-1 bg-[#4CDFD5]/10 text-[#4CDFD5] text-xs font-semibold rounded-md">
            {summary.careerType === 'NEW'
              ? '신입'
              : summary.careerType === 'EXPERIENCED'
                ? '경력'
                : '무관'}
          </span>
        </div>

        {/* 기술스택 */}
        <div className="flex gap-2 flex-wrap">
          {(summary.techStackParsed || []).slice(0, 3).map((tech, idx) => (
            <span
              key={idx}
              className="px-2 py-1 bg-gray-50 text-gray-600 text-xs rounded-md"
            >
              {tech}
            </span>
          ))}

          {(!summary.techStackParsed ||
            summary.techStackParsed.length === 0) && (
              <span className="px-2 py-1 bg-gray-50 text-gray-400 text-xs rounded-md">
                General
              </span>
            )}
        </div>
      </div>

      {/* 하단 */}
      <div className="mt-6 pt-4 border-t border-gray-100 flex justify-between items-center text-xs text-gray-400">
        <span>
          {summary.createdAt?.slice(0, 10).replace(/-/g, '.') || '2024.01.01'}
        </span>
        <span>리뷰 0</span>
      </div>
    </div>
  );
};
