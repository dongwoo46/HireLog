import { useNavigate, useParams } from 'react-router-dom';
import { useEffect, useState } from 'react';
import { jdSummaryService } from '../services/jdSummaryService';
import type { JobSummaryDetailView } from '../types/jobSummary';
import { TbChevronLeft, TbFileText, TbPlus, TbDownload, TbShare } from 'react-icons/tb';

const JobSummaryDetailPage = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [jd, setJd] = useState<JobSummaryDetailView | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const fetchJd = async () => {
      if (!id) return;
      setIsLoading(true);
      try {
        const data = await jdSummaryService.getDetail(parseInt(id));
        setJd(data);
      } catch (error) {
        console.error('Failed to fetch JD', error);
      } finally {
        setIsLoading(false);
      }
    };
    fetchJd();
  }, [id]);

  if (isLoading) {
    return (
      <div className="min-h-screen bg-white pt-32 px-6 flex justify-center items-center">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-[#89cbb6]"></div>
      </div>
    );
  }

  if (!jd) return null;

  return (
    <div className="min-h-screen bg-white pt-24 pb-32">
      <div className="max-w-5xl mx-auto px-6">
        {/* Navigation & Metadata */}
        <div className="flex items-center justify-between mb-12 animate-in fade-in slide-in-from-top-4 duration-700">
          <button 
            onClick={() => navigate(-1)}
            className="flex items-center gap-2 text-gray-400 hover:text-gray-900 transition-colors font-bold uppercase text-[10px] tracking-[0.2em]"
          >
            <TbChevronLeft size={18} />
            Back to Logs
          </button>
          
          <div className="flex items-center gap-4">
            <span className="text-[10px] font-black text-gray-300 uppercase tracking-widest">
              Record ID: <strong>#{jd.id.toString().padStart(5, '0')}</strong>
            </span>
            <div className="w-1 h-1 rounded-full bg-gray-200" />
            <span className="text-[10px] font-black text-gray-300 uppercase tracking-widest">
              Last Updated: <strong>{new Date().toLocaleDateString()}</strong>
            </span>
          </div>
        </div>

        {/* Log Header Card */}
        <div className="log-card p-10 md:p-16 mb-16 overflow-hidden relative border-0">
           <div className="absolute top-0 left-0 w-full h-2 mint-gradient-bg" />
          <div className="absolute top-0 right-0 w-1/2 h-full bg-gradient-to-l from-[#89cbb6]/5 to-transparent pointer-events-none" />
          
          <div className="relative z-10">
            <div className="flex items-center gap-3 mb-6">
              <span className="text-sm font-black text-[#89cbb6] uppercase tracking-[0.3em] italic">{jd.brandName}</span>
              <div className="w-1 h-5 bg-gray-100" />
              <span className="text-[10px] font-bold text-gray-400 uppercase tracking-widest">Job Logistics</span>
            </div>

            <h1 className="text-4xl md:text-7xl font-black text-gray-900 mb-10 tracking-tighter leading-tight italic">
              {jd.brandPositionName}
            </h1>
            
            <div className="flex flex-wrap gap-4 items-center">
              <div className="log-badge border-[#276db8] text-[#276db8] bg-[#276db8]/5 px-4 py-2 text-xs">
                {jd.careerType === 'NEW' ? 'New Entrance' : jd.careerType === 'EXPERIENCED' ? `Experience ${jd.careerYears}+` : 'Universal'}
              </div>
              
              <div className="flex flex-wrap gap-2">
                {(jd.techStack || '').split(',').map((tech, i) => (
                  <span key={i} className="px-3 py-1.5 bg-gray-50 text-gray-500 rounded-xl text-xs font-bold border border-gray-100">
                    {tech.trim()}
                  </span>
                ))}
              </div>
            </div>
          </div>
        </div>

        {/* Detailed Sections */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-12">
          {/* Main Info */}
          <div className="lg:col-span-8 space-y-16">
            <ReportSection title="Executive Summary" icon={<TbFileText size={24} />}>
              <p className="text-lg text-gray-700 leading-relaxed font-medium whitespace-pre-line">
                {jd.summaryText}
              </p>
            </ReportSection>
            
            <ReportSection title="Key Responsibilities" icon={<TbFileText size={24} />}>
              <ul className="space-y-4">
                {jd.responsibilities.split('\n').map((line, i) => (
                  <li key={i} className="flex gap-4 group">
                    <span className="text-[#89cbb6] font-bold mt-1 group-hover:scale-125 transition-transform italic">0{i+1}</span>
                    <p className="text-gray-600 leading-relaxed font-medium">{line.replace(/^- /, '')}</p>
                  </li>
                ))}
              </ul>
            </ReportSection>

            <ReportSection title="Requirements & Preferred" icon={<TbFileText size={24} />}>
              <div className="space-y-10">
                <div>
                  <h4 className="text-xs font-black text-[#276db8] uppercase tracking-widest mb-4">Required Qualifications</h4>
                  <ul className="space-y-3">
                    {jd.requiredQualifications.split('\n').map((line, i) => (
                      <li key={i} className="flex gap-3 text-gray-600 font-medium">
                        <span className="text-[#89cbb6]">•</span>
                        {line.replace(/^- /, '')}
                      </li>
                    ))}
                  </ul>
                </div>
                {jd.preferredQualifications && (
                  <div>
                    <h4 className="text-xs font-black text-gray-400 uppercase tracking-widest mb-4">Preferred Background</h4>
                    <ul className="space-y-3">
                      {jd.preferredQualifications.split('\n').map((line, i) => (
                        <li key={i} className="flex gap-3 text-gray-500 font-medium">
                          <span className="text-gray-300">/</span>
                          {line.replace(/^- /, '')}
                        </li>
                      ))}
                    </ul>
                  </div>
                )}
              </div>
            </ReportSection>
          </div>

          {/* Sidebar / AI Insights */}
          <div className="lg:col-span-4">
            <div className="sticky top-32 space-y-8">
              <div className="log-card p-8 bg-black text-white border-0 overflow-hidden relative group">
                <div className="absolute top-0 left-0 w-full h-1 bg-gradient-to-r from-[#276db8] to-[#89cbb6]" />
                <h3 className="text-xs font-black text-[#89cbb6] uppercase tracking-[0.3em] mb-6 italic">AI Analysis</h3>
                <p className="text-gray-300 leading-relaxed font-medium italic mb-8">
                  "{jd.insights || "Analyzing this record for deeper career insights..."}"
                </p>
                <button 
                  onClick={() => navigate('/jd/request')}
                  className="w-full py-4 rounded-2xl border border-white/20 text-white font-bold text-sm hover:bg-white/10 transition-all flex items-center justify-center gap-2"
                >
                  <TbPlus size={18} />
                  Request New Insight
                </button>
              </div>

              <div className="p-8 rounded-3xl border border-gray-100 bg-gray-50/50">
                <h4 className="text-[10px] font-black text-gray-400 uppercase tracking-widest mb-6">Tools & Actions</h4>
                <div className="space-y-3">
                  <SideAction icon={<TbDownload size={20} />} label="Download Log Report" />
                  <SideAction icon={<TbShare size={20} />} label="Share Entry" />
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

const ReportSection: React.FC<{ title: string; icon: React.ReactNode; children: React.ReactNode }> = ({ title, icon, children }) => (
  <div className="animate-in fade-in slide-in-from-bottom-8 duration-1000">
    <div className="flex items-center gap-4 mb-8">
      <div className="w-10 h-10 rounded-xl bg-gray-50 border border-gray-100 flex items-center justify-center text-[#276db8]">
        {icon}
      </div>
      <h2 className="text-2xl font-black text-gray-900 tracking-tight italic uppercase">{title}</h2>
    </div>
    <div className="pl-14">
      {children}
    </div>
  </div>
);

const SideAction: React.FC<{ icon: React.ReactNode; label: string }> = ({ icon, label }) => (
  <button className="w-full flex items-center gap-4 p-4 rounded-2xl hover:bg-white hover:shadow-log transition-all group">
    <span className="text-gray-400 group-hover:text-[#276db8] transition-colors">{icon}</span>
    <span className="text-xs font-black text-gray-600 uppercase tracking-wider">{label}</span>
  </button>
);

export default JobSummaryDetailPage;
