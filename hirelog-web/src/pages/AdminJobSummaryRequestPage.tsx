import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { TbChevronLeft } from 'react-icons/tb';
import { toast } from 'react-toastify';
import { adminService } from '../services/adminService';
import { jdSummaryService } from '../services/jdSummaryService';
import { useAuthStore } from '../store/authStore';

export default function AdminJobSummaryRequestPage() {
  const navigate = useNavigate();
  const { user, isInitialized } = useAuthStore();

  const [brandName, setBrandName] = useState('');
  const [positionName, setPositionName] = useState('');
  const [textSourceUrl, setTextSourceUrl] = useState('');
  const [jdText, setJdText] = useState('');
  const [urlInput, setUrlInput] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  if (isInitialized && user?.role !== 'ADMIN') {
    return (
      <div className="min-h-screen bg-[#F8F9FA] px-6 pt-24">
        <div className="mx-auto max-w-3xl rounded-2xl border border-gray-200 bg-white p-8 text-center text-sm text-gray-600">
          관리자 권한이 필요합니다.
        </div>
      </div>
    );
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!brandName.trim() || !positionName.trim()) {
      toast.warn('브랜드명과 포지션명은 필수입니다.');
      return;
    }

    const normalizedText = jdText.trim();
    const normalizedUrl = urlInput.trim();

    if (!normalizedText && !normalizedUrl) {
      toast.warn('TEXT 또는 URL 중 하나는 입력해주세요.');
      return;
    }

    setIsLoading(true);
    try {
      // URL이 입력되면 URL 등록 흐름을 우선 사용합니다.
      if (normalizedUrl) {
        const result = await jdSummaryService.requestUrl({
          brandName: brandName.trim(),
          brandPositionName: positionName.trim(),
          url: normalizedUrl,
        });

        if (result.isDuplicate && result.jobSummaryId) {
          toast.info('이미 등록된 JD가 있어 해당 상세 페이지로 이동합니다.');
          navigate(`/jd/${result.jobSummaryId}`);
          return;
        }

        toast.success('URL 기반 JD 등록 요청이 완료되었습니다.');
        navigate('/jd');
        return;
      }

      const result = await adminService.createJobSummaryDirectly({
        brandName: brandName.trim(),
        positionName: positionName.trim(),
        jdText: normalizedText,
        sourceUrl: textSourceUrl.trim() || undefined,
      });

      toast.success('관리자 수동 JD 생성이 완료되었습니다.');
      navigate(`/jd/${result.summaryId}`);
    } catch {
      toast.error('JD 등록 처리에 실패했습니다. 입력값을 다시 확인해주세요.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#F8F9FA] pb-20 pt-24">
      <div className="mx-auto max-w-4xl px-6">
        <button
          onClick={() => navigate(-1)}
          className="mb-6 flex items-center gap-2 text-gray-500 hover:text-gray-700"
        >
          <TbChevronLeft size={20} />
          뒤로가기
        </button>

        <div className="overflow-hidden rounded-3xl border border-gray-100 bg-white shadow-xl">
          <div className="border-b bg-[#3FB6B2]/10 p-8 md:p-10">
            <h1 className="mb-2 text-3xl font-black text-gray-900">관리자 전용 JD 수동 등록</h1>
            <p className="text-sm text-gray-600">TEXT 또는 URL 중 하나를 입력해 등록할 수 있습니다.</p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-6 p-8 md:p-10">
            <div className="grid gap-5 md:grid-cols-2">
              <label className="space-y-2">
                <span className="text-sm font-bold text-gray-700">브랜드명</span>
                <input
                  value={brandName}
                  onChange={(e) => setBrandName(e.target.value)}
                  className="w-full rounded-2xl border border-gray-200 px-4 py-3 text-sm focus:border-[#3FB6B2] focus:outline-none focus:ring-4 focus:ring-[#3FB6B2]/15"
                  placeholder="예: 네이버"
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

            <label className="space-y-2 block">
              <span className="text-sm font-bold text-gray-700">공고 URL</span>
              <input
                type="url"
                value={urlInput}
                onChange={(e) => setUrlInput(e.target.value)}
                className="w-full rounded-2xl border border-gray-200 px-4 py-3 text-sm focus:border-[#3FB6B2] focus:outline-none focus:ring-4 focus:ring-[#3FB6B2]/15"
                placeholder="https://..."
              />
              <p className="text-xs text-gray-500">URL이 있으면 URL 등록이 우선 처리됩니다.</p>
            </label>

            <label className="space-y-2 block">
              <span className="text-sm font-bold text-gray-700">원본 공고 URL (TEXT 등록 시 선택)</span>
              <input
                type="url"
                value={textSourceUrl}
                onChange={(e) => setTextSourceUrl(e.target.value)}
                className="w-full rounded-2xl border border-gray-200 px-4 py-3 text-sm focus:border-[#3FB6B2] focus:outline-none focus:ring-4 focus:ring-[#3FB6B2]/15"
                placeholder="https://..."
              />
            </label>

            <label className="space-y-2 block">
              <span className="text-sm font-bold text-gray-700">JD 본문</span>
              <textarea
                value={jdText}
                onChange={(e) => setJdText(e.target.value)}
                className="min-h-[320px] w-full rounded-2xl border border-gray-200 p-4 text-sm focus:border-[#3FB6B2] focus:outline-none focus:ring-4 focus:ring-[#3FB6B2]/15"
                placeholder="채용공고 본문 텍스트를 붙여 넣어 주세요."
              />
            </label>

            <button
              type="submit"
              disabled={isLoading}
              className="w-full rounded-2xl bg-[#3FB6B2] py-4 text-base font-bold text-white transition hover:bg-[#35A09D] disabled:opacity-60"
            >
              {isLoading ? '처리 중...' : '등록하기'}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
