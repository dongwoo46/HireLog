import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { TbCamera, TbChevronLeft, TbCloudUpload, TbFileText, TbLink } from 'react-icons/tb';
import { toast } from 'react-toastify';
import { jdSummaryService } from '../services/jdSummaryService';

type RequestTab = 'text' | 'ocr' | 'url';

const JobSummaryRequestPage = () => {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState<RequestTab>('url');
  const [isLoading, setIsLoading] = useState(false);

  const [brandName, setBrandName] = useState('');
  const [brandPositionName, setBrandPositionName] = useState('');
  const [jdText, setJdText] = useState('');
  const [url, setUrl] = useState('');
  const [images, setImages] = useState<File[]>([]);

  const getErrorMessage = (error: unknown) => {
    if (error instanceof Error && error.message.trim()) {
      return error.message;
    }
    return '요청 중 오류가 발생했습니다.';
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!brandName || !brandPositionName) {
      toast.warning('회사명과 포지션명을 입력해주세요.');
      return;
    }

    setIsLoading(true);
    try {
      if (activeTab === 'text') {
        if (!jdText.trim()) throw new Error('JD 내용을 입력해주세요.');
        await jdSummaryService.requestText({ brandName, brandPositionName, jdText });
      } else if (activeTab === 'ocr') {
        if (images.length === 0) throw new Error('이미지를 업로드해주세요.');
        await jdSummaryService.requestOcr({ brandName, brandPositionName, images });
      } else {
        if (!url.trim()) throw new Error('URL을 입력해주세요.');
        const res = await jdSummaryService.requestUrl({ brandName, brandPositionName, url });
        if (res.isDuplicate && res.jobSummaryId) {
          navigate(`/jd/${res.jobSummaryId}`);
          return;
        }
      }

      navigate('/jd');
    } catch (error: unknown) {
      toast.error(getErrorMessage(error));
    } finally {
      setIsLoading(false);
    }
  };

  const handleImageChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const selectedFiles = e.target.files ? Array.from(e.target.files) : [];
    if (selectedFiles.length === 0) return;

    setImages((prev) => {
      const existing = new Set(prev.map((file) => `${file.name}-${file.size}-${file.lastModified}`));
      const merged = [...prev];

      selectedFiles.forEach((file) => {
        const key = `${file.name}-${file.size}-${file.lastModified}`;
        if (!existing.has(key)) {
          merged.push(file);
        }
      });

      return merged;
    });

    e.target.value = '';
  };

  return (
    <div className="min-h-screen bg-[#F8F9FA] pt-24 pb-20">
      <div className="mx-auto max-w-3xl px-6">
        <button
          onClick={() => navigate(-1)}
          className="mb-8 flex items-center gap-2 text-gray-400 hover:text-gray-600"
        >
          <TbChevronLeft size={20} />
          뒤로가기
        </button>

        <div className="overflow-hidden rounded-3xl border border-gray-100 bg-white shadow-xl">
          <div className="border-b bg-[#4CDFD5]/10 p-8 md:p-12">
            <h1 className="mb-2 text-3xl font-black text-gray-900">새로운 JD 요약 요청</h1>
            <p className="text-gray-500">공고 정보를 입력하면 핵심 내용을 정리해드립니다.</p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-8 p-8 md:p-12">
            <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
              <input
                type="text"
                placeholder="회사명"
                className="w-full rounded-2xl border border-gray-200 px-5 py-3 outline-none focus:border-[#4CDFD5] focus:ring-4 focus:ring-[#4CDFD5]/20"
                value={brandName}
                onChange={(e) => setBrandName(e.target.value)}
                required
              />
              <input
                type="text"
                placeholder="포지션명"
                className="w-full rounded-2xl border border-gray-200 px-5 py-3 outline-none focus:border-[#4CDFD5] focus:ring-4 focus:ring-[#4CDFD5]/20"
                value={brandPositionName}
                onChange={(e) => setBrandPositionName(e.target.value)}
                required
              />
            </div>

            <div className="flex rounded-2xl bg-gray-100 p-1">
              <TabButton active={activeTab === 'url'} onClick={() => setActiveTab('url')} icon={<TbLink />} label="URL" />
              <TabButton active={activeTab === 'text'} onClick={() => setActiveTab('text')} icon={<TbFileText />} label="텍스트 입력" />
              <TabButton active={activeTab === 'ocr'} onClick={() => setActiveTab('ocr')} icon={<TbCamera />} label="이미지(OCR)" />
            </div>

            {activeTab === 'url' && (
              <div className="space-y-4">
                <input
                  type="url"
                  placeholder="https://example.com/job"
                  className="w-full rounded-2xl border border-gray-200 px-5 py-3 outline-none focus:border-[#4CDFD5] focus:ring-4 focus:ring-[#4CDFD5]/20"
                  value={url}
                  onChange={(e) => setUrl(e.target.value)}
                />
              </div>
            )}

            {activeTab === 'text' && (
              <textarea
                className="h-64 w-full resize-none rounded-2xl border border-gray-200 px-5 py-4 outline-none focus:border-[#4CDFD5] focus:ring-4 focus:ring-[#4CDFD5]/20"
                value={jdText}
                onChange={(e) => setJdText(e.target.value)}
              />
            )}

            {activeTab === 'ocr' && (
              <div className="space-y-3">
                <div className="group relative cursor-pointer rounded-3xl border-2 border-dashed border-gray-200 p-12 text-center transition-all hover:border-[#4CDFD5] hover:bg-[#4CDFD5]/5">
                  <input
                    type="file"
                    multiple
                    accept="image/*"
                    onChange={handleImageChange}
                    className="absolute inset-0 h-full w-full cursor-pointer opacity-0"
                  />
                  <TbCloudUpload size={48} className="mx-auto mb-4 text-gray-300 group-hover:text-[#4CDFD5]" />
                  <p className="text-gray-500">이미지 업로드</p>
                </div>

                {images.length > 0 && (
                  <div className="rounded-xl border border-gray-200 p-3 text-xs text-gray-600">
                    {images.length}개 파일 선택됨
                  </div>
                )}
              </div>
            )}

            <button
              type="submit"
              disabled={isLoading}
              className="w-full rounded-2xl bg-[#4CDFD5] py-4 text-xl font-black text-white transition-all hover:bg-[#3CCFC5] active:bg-[#35C3BA] disabled:opacity-50"
            >
              {isLoading ? '요약 분석 중...' : '요약 요청하기'}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
};

const TabButton: React.FC<{
  active: boolean;
  onClick: () => void;
  icon: React.ReactNode;
  label: string;
}> = ({ active, onClick, icon, label }) => (
  <button
    type="button"
    onClick={onClick}
    className={`flex-1 flex items-center justify-center gap-2 rounded-xl py-3 text-sm font-bold transition-all ${
      active ? 'bg-white text-[#4CDFD5] shadow-sm' : 'text-gray-400 hover:text-gray-600'
    }`}
  >
    {icon}
    {label}
  </button>
);

export default JobSummaryRequestPage;
