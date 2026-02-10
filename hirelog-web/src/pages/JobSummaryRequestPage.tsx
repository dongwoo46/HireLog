import { useNavigate } from 'react-router-dom';
import { jdSummaryService } from '../services/jdSummaryService';
import { TbChevronLeft, TbFileText, TbCamera, TbLink, TbCloudUpload } from 'react-icons/tb';
import { toast } from 'react-toastify';
import React, { useState } from 'react';

type RequestTab = 'text' | 'ocr' | 'url';

const JobSummaryRequestPage = () => {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState<RequestTab>('text');
  const [isLoading, setIsLoading] = useState(false);
  
  // Form State
  const [brandName, setBrandName] = useState('');
  const [brandPositionName, setBrandPositionName] = useState('');
  const [jdText, setJdText] = useState('');
  const [url, setUrl] = useState('');
  const [images, setImages] = useState<File[]>([]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!brandName || !brandPositionName) {
      toast.warning('회사명과 포지션명을 입력해주세요.');
      return;
    }

    setIsLoading(true);
    try {
      if (activeTab === 'text') {
        if (!jdText) throw new Error('JD 내용을 입력해주세요.');
        await jdSummaryService.requestText({ brandName, brandPositionName, jdText });
      } else if (activeTab === 'ocr') {
        if (images.length === 0) throw new Error('이미지를 업로드해주세요.');
        await jdSummaryService.requestOcr({ brandName, brandPositionName, images });
      } else if (activeTab === 'url') {
        if (!url) throw new Error('URL을 입력해주세요.');
        const res = await jdSummaryService.requestUrl({ brandName, brandPositionName, url });
        if (res.isDuplicate && res.jobSummaryId) {
          toast.info('이미 요약된 공고가 있어 해당 페이지로 이동합니다.');
          navigate(`/jd/${res.jobSummaryId}`);
          return;
        }
      }
      
      navigate('/jd');
    } catch (error: any) {
      toast.error(error.message || '요청 중 오류가 발생했습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleImageChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files) {
      setImages(Array.from(e.target.files));
    }
  };

  return (
    <div className="min-h-screen bg-[#F8F9FA] pt-24 pb-20">
      <div className="max-w-3xl mx-auto px-6">
        <button 
          onClick={() => navigate(-1)}
          className="flex items-center gap-2 text-gray-400 hover:text-gray-600 mb-8 transition-colors"
        >
          <TbChevronLeft size={20} />
          <span>뒤로가기</span>
        </button>

        <div className="bg-white rounded-3xl shadow-xl shadow-[#89cbb6]/5 border border-gray-100 overflow-hidden">
          <div className="p-8 md:p-12 border-b border-gray-50 bg-gradient-to-r from-[#276db8]/5 to-[#89cbb6]/5">
            <h1 className="text-3xl font-black text-gray-900 mb-2">새로운 JD 요약 요청</h1>
            <p className="text-gray-500">공고 정보를 입력하시면 AI가 핵심 내용을 요약해 드립니다.</p>
          </div>

          <form onSubmit={handleSubmit} className="p-8 md:p-12 space-y-8">
            {/* Common Fields */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div className="space-y-2">
                <label className="text-sm font-bold text-gray-700 ml-1">회사명</label>
                <input
                  type="text"
                  placeholder="예: 카카오, 네이버"
                  className="w-full px-5 py-3 rounded-2xl border border-gray-200 focus:border-[#89cbb6] focus:ring-4 focus:ring-[#89cbb6]/10 outline-none transition-all"
                  value={brandName}
                  onChange={(e) => setBrandName(e.target.value)}
                  required
                />
              </div>
              <div className="space-y-2">
                <label className="text-sm font-bold text-gray-700 ml-1">포지션명</label>
                <input
                  type="text"
                  placeholder="예: 백엔드 개발자"
                  className="w-full px-5 py-3 rounded-2xl border border-gray-200 focus:border-[#89cbb6] focus:ring-4 focus:ring-[#89cbb6]/10 outline-none transition-all"
                  value={brandPositionName}
                  onChange={(e) => setBrandPositionName(e.target.value)}
                  required
                />
              </div>
            </div>

            {/* Tab Navigation */}
            <div className="flex p-1 bg-gray-100 rounded-2xl">
              <TabButton 
                active={activeTab === 'text'} 
                onClick={() => setActiveTab('text')} 
                icon={<TbFileText size={20} />} 
                label="텍스트 직접 입력" 
              />
              <TabButton 
                active={activeTab === 'ocr'} 
                onClick={() => setActiveTab('ocr')} 
                icon={<TbCamera size={20} />} 
                label="이미지(OCR)" 
              />
              <TabButton 
                active={activeTab === 'url'} 
                onClick={() => setActiveTab('url')} 
                icon={<TbLink size={20} />} 
                label="공고 URL" 
              />
            </div>

            {/* Tab Content */}
            <div className="min-h-[200px] animate-in fade-in duration-300">
              {activeTab === 'text' && (
                <div className="space-y-2">
                  <label className="text-sm font-bold text-gray-700 ml-1">공고 텍스트</label>
                  <textarea
                    placeholder="채용 공고의 전체 내용을 복사해서 붙여넣어 주세요."
                    className="w-full h-64 px-5 py-4 rounded-2xl border border-gray-200 focus:border-[#89cbb6] focus:ring-4 focus:ring-[#89cbb6]/10 outline-none transition-all resize-none"
                    value={jdText}
                    onChange={(e) => setJdText(e.target.value)}
                  />
                </div>
              )}

              {activeTab === 'ocr' && (
                <div className="space-y-4">
                  <label className="text-sm font-bold text-gray-700 ml-1">공고 이미지 업로드</label>
                  <div className="relative border-2 border-dashed border-gray-200 rounded-3xl p-12 text-center hover:border-[#89cbb6] hover:bg-[#89cbb6]/5 transition-all cursor-pointer group">
                    <input
                      type="file"
                      multiple
                      accept="image/*"
                      onChange={handleImageChange}
                      className="absolute inset-0 w-full h-full opacity-0 cursor-pointer"
                    />
                    <TbCloudUpload size={48} className="mx-auto text-gray-300 group-hover:text-[#89cbb6] mb-4 transition-colors" />
                    <p className="text-gray-500 font-medium">이미지 파일을 드래그하거나 클릭하여 업로드하세요.</p>
                    <p className="text-gray-400 text-sm mt-1">PNG, JPG 형식 지원</p>
                  </div>
                  {images.length > 0 && (
                    <div className="flex flex-wrap gap-2 mt-4">
                      {images.map((img, i) => (
                        <div key={i} className="px-3 py-1 bg-[#89cbb6]/10 text-[#276db8] text-xs font-bold rounded-lg border border-[#89cbb6]/20">
                          {img.name}
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              )}

              {activeTab === 'url' && (
                <div className="space-y-2">
                  <label className="text-sm font-bold text-gray-700 ml-1">공고 URL</label>
                  <input
                    type="url"
                    placeholder="https://example.com/job/123"
                    className="w-full px-5 py-3 rounded-2xl border border-gray-200 focus:border-[#89cbb6] focus:ring-4 focus:ring-[#89cbb6]/10 outline-none transition-all"
                    value={url}
                    onChange={(e) => setUrl(e.target.value)}
                  />
                  <p className="text-xs text-gray-400 mt-2 ml-1">* 지원하지 않는 도메인의 경우 텍스트 직접 입력을 이용해주세요.</p>
                </div>
              )}
            </div>

            <button
              type="submit"
              disabled={isLoading}
              className="w-full py-4 bg-gradient-to-r from-[#276db8] to-[#89cbb6] text-white font-black text-xl rounded-2xl shadow-xl shadow-[#89cbb6]/20 hover:scale-[1.02] active:scale-[0.98] transition-all disabled:opacity-50 disabled:scale-100"
            >
              {isLoading ? '요약 분석 중...' : 'AI 요약 시작하기'}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
};

const TabButton: React.FC<{ active: boolean; onClick: () => void; icon: React.ReactNode; label: string }> = ({ active, onClick, icon, label }) => (
  <button
    type="button"
    onClick={onClick}
    className={`flex-1 flex items-center justify-center gap-2 py-3 rounded-xl text-sm font-bold transition-all ${
      active ? 'bg-white text-[#276db8] shadow-sm' : 'text-gray-400 hover:text-gray-600'
    }`}
  >
    {icon}
    {label}
  </button>
);

export default JobSummaryRequestPage;
