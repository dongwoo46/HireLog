import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { TbCamera, TbChevronLeft, TbCloudUpload, TbFileText, TbLink, TbX } from 'react-icons/tb';
import { toast } from 'react-toastify';
import { adminService } from '../services/adminService';
import { useAuthStore } from '../store/authStore';

type RequestTab = 'url' | 'ocr' | 'text';

const fileToDataUrl = (file: File): Promise<string> =>
  new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => {
      if (typeof reader.result === 'string') {
        resolve(reader.result);
        return;
      }
      reject(new Error('이미지 변환에 실패했습니다.'));
    };
    reader.onerror = () => reject(new Error('이미지 변환에 실패했습니다.'));
    reader.readAsDataURL(file);
  });

function TabButton({
  active,
  onClick,
  icon,
  label,
}: {
  active: boolean;
  onClick: () => void;
  icon: React.ReactNode;
  label: string;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`flex flex-1 items-center justify-center gap-2 rounded-xl py-3 text-sm font-bold transition ${
        active ? 'bg-white text-[#3FB6B2] shadow-sm' : 'text-gray-500 hover:text-gray-700'
      }`}
    >
      {icon}
      {label}
    </button>
  );
}

export default function AdminJobSummaryRequestPage() {
  const navigate = useNavigate();
  const { user, isInitialized } = useAuthStore();

  const [activeTab, setActiveTab] = useState<RequestTab>('url');
  const [brandName, setBrandName] = useState('');
  const [positionName, setPositionName] = useState('');
  const [urlInput, setUrlInput] = useState('');
  const [jdText, setJdText] = useState('');
  const [textSourceUrl, setTextSourceUrl] = useState('');
  const [ocrSourceUrl, setOcrSourceUrl] = useState('');
  const [images, setImages] = useState<File[]>([]);
  const [isLoading, setIsLoading] = useState(false);

  const handleBack = () => {
    if (window.history.length > 1) {
      navigate(-1);
      return;
    }
    navigate('/admin');
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

  const handleRemoveImage = (index: number) => {
    setImages((prev) => prev.filter((_, idx) => idx !== index));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    const trimmedBrandName = brandName.trim();
    const trimmedPositionName = positionName.trim();

    if (!trimmedBrandName || !trimmedPositionName) {
      toast.warn('브랜드명과 포지션명을 입력해주세요.');
      return;
    }

    setIsLoading(true);
    try {
      if (activeTab === 'url') {
        const normalizedUrl = urlInput.trim();
        if (!normalizedUrl) {
          toast.warn('URL을 입력해주세요.');
          return;
        }

        const result = await adminService.createJobSummaryFromUrlDirectly({
          brandName: trimmedBrandName,
          positionName: trimmedPositionName,
          url: normalizedUrl,
        });

        toast.success('URL 기반 관리자 수동 JD 생성이 완료되었습니다.');
        navigate(`/jd/${result.summaryId}`);
        return;
      }

      if (activeTab === 'ocr') {
        if (images.length === 0) {
          toast.warn('OCR용 이미지를 1장 이상 업로드해주세요.');
          return;
        }

        const encodedImages = await Promise.all(images.map((file) => fileToDataUrl(file)));
        const result = await adminService.createJobSummaryFromImagesDirectly({
          brandName: trimmedBrandName,
          positionName: trimmedPositionName,
          images: encodedImages,
          sourceUrl: ocrSourceUrl.trim() || undefined,
        });

        toast.success('OCR 기반 관리자 수동 JD 생성이 완료되었습니다.');
        navigate(`/jd/${result.summaryId}`);
        return;
      }

      const normalizedText = jdText.trim();
      if (!normalizedText) {
        toast.warn('JD 본문을 입력해주세요.');
        return;
      }

      const result = await adminService.createJobSummaryDirectly({
        brandName: trimmedBrandName,
        positionName: trimmedPositionName,
        jdText: normalizedText,
        sourceUrl: textSourceUrl.trim() || undefined,
      });

      toast.success('TEXT 기반 관리자 수동 JD 생성이 완료되었습니다.');
      navigate(`/jd/${result.summaryId}`);
    } catch {
      toast.error('JD 등록 처리에 실패했습니다. 입력값을 다시 확인해주세요.');
    } finally {
      setIsLoading(false);
    }
  };

  if (isInitialized && user?.role !== 'ADMIN') {
    return (
      <div className="min-h-screen bg-[#F8F9FA] px-6 pt-24">
        <div className="mx-auto max-w-3xl rounded-2xl border border-gray-200 bg-white p-8 text-center text-sm text-gray-600">
          관리자만 접근할 수 있는 페이지입니다.
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#F8F9FA] pb-20 pt-24">
      <div className="mx-auto max-w-4xl px-6">
        <button
          onClick={handleBack}
          className="mb-6 flex items-center gap-2 text-gray-500 hover:text-gray-700"
        >
          <TbChevronLeft size={20} />
          뒤로가기
        </button>

        <div className="overflow-hidden rounded-3xl border border-gray-100 bg-white shadow-xl">
          <div className="border-b bg-[#3FB6B2]/10 p-8 md:p-10">
            <h1 className="mb-2 text-3xl font-black text-gray-900">관리자 전용 JD 수동 등록</h1>
            <p className="text-sm text-gray-600">등록 방식 탭을 선택해 URL, OCR, TEXT로 JD를 생성할 수 있습니다.</p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-6 p-8 md:p-10">
            <div className="grid gap-5 md:grid-cols-2">
              <label className="space-y-2">
                <span className="text-sm font-bold text-gray-700">브랜드명</span>
                <input
                  value={brandName}
                  onChange={(e) => setBrandName(e.target.value)}
                  className="w-full rounded-2xl border border-gray-200 px-4 py-3 text-sm focus:border-[#3FB6B2] focus:outline-none focus:ring-4 focus:ring-[#3FB6B2]/15"
                  placeholder="예: Toss"
                  required
                />
              </label>

              <label className="space-y-2">
                <span className="text-sm font-bold text-gray-700">포지션명</span>
                <input
                  value={positionName}
                  onChange={(e) => setPositionName(e.target.value)}
                  className="w-full rounded-2xl border border-gray-200 px-4 py-3 text-sm focus:border-[#3FB6B2] focus:outline-none focus:ring-4 focus:ring-[#3FB6B2]/15"
                  placeholder="예: Backend Engineer"
                  required
                />
              </label>
            </div>

            <div className="flex rounded-2xl bg-gray-100 p-1">
              <TabButton active={activeTab === 'url'} onClick={() => setActiveTab('url')} icon={<TbLink size={17} />} label="URL" />
              <TabButton active={activeTab === 'ocr'} onClick={() => setActiveTab('ocr')} icon={<TbCamera size={17} />} label="OCR" />
              <TabButton active={activeTab === 'text'} onClick={() => setActiveTab('text')} icon={<TbFileText size={17} />} label="TEXT" />
            </div>

            {activeTab === 'url' && (
              <label className="block space-y-2">
                <span className="text-sm font-bold text-gray-700">채용공고 URL</span>
                <input
                  type="url"
                  value={urlInput}
                  onChange={(e) => setUrlInput(e.target.value)}
                  className="w-full rounded-2xl border border-gray-200 px-4 py-3 text-sm focus:border-[#3FB6B2] focus:outline-none focus:ring-4 focus:ring-[#3FB6B2]/15"
                  placeholder="https://..."
                />
              </label>
            )}

            {activeTab === 'ocr' && (
              <div className="space-y-4">
                <label className="block space-y-2">
                  <span className="text-sm font-bold text-gray-700">원본 URL (선택)</span>
                  <input
                    type="url"
                    value={ocrSourceUrl}
                    onChange={(e) => setOcrSourceUrl(e.target.value)}
                    className="w-full rounded-2xl border border-gray-200 px-4 py-3 text-sm focus:border-[#3FB6B2] focus:outline-none focus:ring-4 focus:ring-[#3FB6B2]/15"
                    placeholder="https://..."
                  />
                </label>

                <div className="group relative cursor-pointer rounded-3xl border-2 border-dashed border-gray-200 p-10 text-center transition hover:border-[#3FB6B2] hover:bg-[#3FB6B2]/5">
                  <input
                    type="file"
                    multiple
                    accept="image/*"
                    onChange={handleImageChange}
                    className="absolute inset-0 h-full w-full cursor-pointer opacity-0"
                  />
                  <TbCloudUpload size={40} className="mx-auto mb-3 text-gray-300 group-hover:text-[#3FB6B2]" />
                  <p className="text-sm font-semibold text-gray-700">이미지 업로드</p>
                  <p className="mt-1 text-xs text-gray-500">JPG, PNG 등 OCR 가능한 이미지를 올려주세요.</p>
                </div>

                {images.length > 0 && (
                  <div className="rounded-2xl border border-gray-200 bg-gray-50 p-3">
                    <p className="mb-2 text-xs font-semibold text-gray-600">선택된 이미지 {images.length}개</p>
                    <div className="space-y-2">
                      {images.map((file, index) => (
                        <div key={`${file.name}-${file.size}-${file.lastModified}`} className="flex items-center justify-between rounded-xl bg-white px-3 py-2 text-sm text-gray-700">
                          <span className="truncate pr-2">{file.name}</span>
                          <button
                            type="button"
                            onClick={() => handleRemoveImage(index)}
                            className="rounded-md p-1 text-gray-400 hover:bg-gray-100 hover:text-gray-700"
                            aria-label="이미지 제거"
                          >
                            <TbX size={16} />
                          </button>
                        </div>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            )}

            {activeTab === 'text' && (
              <div className="space-y-4">
                <label className="block space-y-2">
                  <span className="text-sm font-bold text-gray-700">원본 URL (선택)</span>
                  <input
                    type="url"
                    value={textSourceUrl}
                    onChange={(e) => setTextSourceUrl(e.target.value)}
                    className="w-full rounded-2xl border border-gray-200 px-4 py-3 text-sm focus:border-[#3FB6B2] focus:outline-none focus:ring-4 focus:ring-[#3FB6B2]/15"
                    placeholder="https://..."
                  />
                </label>

                <label className="block space-y-2">
                  <span className="text-sm font-bold text-gray-700">JD 본문</span>
                  <textarea
                    value={jdText}
                    onChange={(e) => setJdText(e.target.value)}
                    className="min-h-[320px] w-full rounded-2xl border border-gray-200 p-4 text-sm focus:border-[#3FB6B2] focus:outline-none focus:ring-4 focus:ring-[#3FB6B2]/15"
                    placeholder="관리자 수동등록용 JD 원문을 붙여넣어 주세요."
                  />
                </label>
              </div>
            )}

            <button
              type="submit"
              disabled={isLoading}
              className="w-full rounded-2xl bg-[#3FB6B2] py-4 text-base font-bold text-white transition hover:bg-[#35A09D] disabled:opacity-60"
            >
              {isLoading ? '등록 처리 중...' : '등록하기'}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
