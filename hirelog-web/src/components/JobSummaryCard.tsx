import React from 'react';
import type { JobSummaryView } from '../types/jobSummary';
import { useNavigate } from 'react-router-dom';
import { TbFileText, TbChevronRight } from 'react-icons/tb';

interface Props {
  summary: JobSummaryView;
}

export const JobSummaryCard: React.FC<Props> = ({ summary }) => {
  const navigate = useNavigate();

  return (
    <div 
      onClick={() => navigate(`/jd/${summary.id}`)}
      className="log-card p-6 flex flex-col h-full cursor-pointer group"
    >
      <div className="flex justify-between items-start mb-6">
        <div className="w-14 h-14 mint-gradient-bg rounded-2xl flex items-center justify-center text-white shadow-lg shadow-[#89cbb6]/20 transition-transform group-hover:scale-110 duration-500">
          <TbFileText size={28} />
        </div>
        <span className="px-3 py-1 bg-gray-50 border border-gray-100 text-[10px] font-black text-gray-400 rounded-full tracking-widest group-hover:border-[#89cbb6]/30 group-hover:text-[#276db8] transition-colors">
          #{summary.id.toString().padStart(4, '0')}
        </span>
      </div>

      <div className="flex-grow">
        <div className="flex items-center gap-2 mb-2">
          <span className="text-xs font-black text-[#89cbb6] uppercase tracking-wider">{summary.brandName}</span>
          <div className="w-1 h-1 rounded-full bg-gray-200" />
          <span className="text-[10px] font-bold text-gray-400 uppercase tracking-widest italic">Entry</span>
        </div>
        <h3 className="text-xl font-black text-gray-900 mb-3 group-hover:text-[#276db8] transition-colors leading-tight">
          {summary.brandPositionName}
        </h3>
        <p className="text-sm text-gray-500 line-clamp-3 leading-relaxed font-medium">
          {summary.summaryText}
        </p>
      </div>

      <div className="mt-8 pt-6 border-t border-dashed border-gray-100 flex items-center justify-between">
        <div className="flex items-center gap-2">
          <span className="log-badge text-[#276db8] border-[#276db8]/20 bg-[#276db8]/5">
            {summary.careerType === 'NEW' ? 'New' : summary.careerType === 'EXPERIENCED' ? 'Exp' : 'Any'}
          </span>
        </div>
        <TbChevronRight size={20} className="text-gray-300 group-hover:text-[#276db8] group-hover:translate-x-1 transition-all" />
      </div>
    </div>
  );
};
