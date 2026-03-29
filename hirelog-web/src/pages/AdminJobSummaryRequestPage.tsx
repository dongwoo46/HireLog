import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { TbChevronLeft } from 'react-icons/tb';
import { toast } from 'react-toastify';
import { adminService } from '../services/adminService';
import { useAuthStore } from '../store/authStore';

export default function AdminJobSummaryRequestPage() {
  const navigate = useNavigate();
  const { user, isInitialized } = useAuthStore();

  const [brandName, setBrandName] = useState('');
  const [positionName, setPositionName] = useState('');
  const [sourceUrl, setSourceUrl] = useState('');
  const [jdText, setJdText] = useState('');
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

    if (!brandName.trim() || !positionName.trim() || !jdText.trim()) {
      toast.warn('브랜드명, 포지션명, JD 원문은 필수입니다.');
      return;
    }

    setIsLoading(true);
    try {
      const result = await adminService.createJobSummaryDirectly({
        brandName: brandName.trim(),
        positionName: positionName.trim(),
        jdText: jdText.trim(),
        sourceUrl: sourceUrl.trim() || undefined,
      });

      toast.success('관리자 전용 JD 생성이 완료되었습니다.');
      navigate(`/jd/${result.summaryId}`);
    } catch {
      toast.error('JD 생성에 실패했습니다. 입력값을 다시 확인해 주세요.');
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
            <p className="text-sm text-gray-600">
              일반 요청 파이프라인을 거치지 않고 즉시 Job Summary를 생성합니다.
            </p>
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
              <span className="text-sm font-bold text-gray-700">원본 공고 URL (선택)</span>
              <input
                type="url"
                value={sourceUrl}
                onChange={(e) => setSourceUrl(e.target.value)}
                className="w-full rounded-2xl border border-gray-200 px-4 py-3 text-sm focus:border-[#3FB6B2] focus:outline-none focus:ring-4 focus:ring-[#3FB6B2]/15"
                placeholder="https://..."
              />
            </label>

            <label className="space-y-2 block">
              <span className="text-sm font-bold text-gray-700">JD 원문</span>
              <textarea
                value={jdText}
                onChange={(e) => setJdText(e.target.value)}
                className="min-h-[320px] w-full rounded-2xl border border-gray-200 p-4 text-sm focus:border-[#3FB6B2] focus:outline-none focus:ring-4 focus:ring-[#3FB6B2]/15"
                placeholder="채용공고 원문 텍스트를 붙여 넣어 주세요."
                required
              />
            </label>

            <button
              type="submit"
              disabled={isLoading}
              className="w-full rounded-2xl bg-[#3FB6B2] py-4 text-base font-bold text-white transition hover:bg-[#35A09D] disabled:opacity-60"
            >
              {isLoading ? '생성 중...' : '관리자 전용 JD 생성'}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}

