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
  const [textSourceUrl, setTextSourceUrl] = useState('');
  const [jdText, setJdText] = useState('');
  const [urlInput, setUrlInput] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const handleBack = () => {
    if (window.history.length > 1) {
      navigate(-1);
      return;
    }
    navigate('/admin');
  };

  if (isInitialized && user?.role !== 'ADMIN') {
    return (
      <div className="min-h-screen bg-[#F8F9FA] px-6 pt-24">
        <div className="mx-auto max-w-3xl rounded-2xl border border-gray-200 bg-white p-8 text-center text-sm text-gray-600">
          愿由ъ옄 沅뚰븳???꾩슂?⑸땲??
        </div>
      </div>
    );
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!brandName.trim() || !positionName.trim()) {
      toast.warn('釉뚮옖?쒕챸怨??ъ??섎챸? ?꾩닔?낅땲??');
      return;
    }

    const normalizedText = jdText.trim();
    const normalizedUrl = urlInput.trim();

    if (!normalizedText && !normalizedUrl) {
      toast.warn('TEXT ?먮뒗 URL 以??섎굹???낅젰?댁＜?몄슂.');
      return;
    }

    setIsLoading(true);
    try {
      // URL???낅젰?섎㈃ URL ?깅줉 ?먮쫫???곗꽑 ?ъ슜?⑸땲??
      if (normalizedUrl) {
        const result = await adminService.createJobSummaryFromUrlDirectly({
          brandName: brandName.trim(),
          positionName: positionName.trim(),
          url: normalizedUrl,
        });
        toast.success('URL 기반 관리자 수동 JD 생성이 완료되었습니다.');
        navigate(`/jd/${result.summaryId}`);
        return;
      }
      const result = await adminService.createJobSummaryDirectly({
        brandName: brandName.trim(),
        positionName: positionName.trim(),
        jdText: normalizedText,
        sourceUrl: textSourceUrl.trim() || undefined,
      });

      toast.success('愿由ъ옄 ?섎룞 JD ?앹꽦???꾨즺?섏뿀?듬땲??');
      navigate(`/jd/${result.summaryId}`);
    } catch {
      toast.error('JD ?깅줉 泥섎━???ㅽ뙣?덉뒿?덈떎. ?낅젰媛믪쓣 ?ㅼ떆 ?뺤씤?댁＜?몄슂.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#F8F9FA] pb-20 pt-24">
      <div className="mx-auto max-w-4xl px-6">
        <button
          onClick={handleBack}
          className="mb-6 flex items-center gap-2 text-gray-500 hover:text-gray-700"
        >
          <TbChevronLeft size={20} />
          ?ㅻ줈媛湲?        </button>

        <div className="overflow-hidden rounded-3xl border border-gray-100 bg-white shadow-xl">
          <div className="border-b bg-[#3FB6B2]/10 p-8 md:p-10">
            <h1 className="mb-2 text-3xl font-black text-gray-900">愿由ъ옄 ?꾩슜 JD ?섎룞 ?깅줉</h1>
            <p className="text-sm text-gray-600">TEXT ?먮뒗 URL 以??섎굹瑜??낅젰???깅줉?????덉뒿?덈떎.</p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-6 p-8 md:p-10">
            <div className="grid gap-5 md:grid-cols-2">
              <label className="space-y-2">
                <span className="text-sm font-bold text-gray-700">釉뚮옖?쒕챸</span>
                <input
                  value={brandName}
                  onChange={(e) => setBrandName(e.target.value)}
                  className="w-full rounded-2xl border border-gray-200 px-4 py-3 text-sm focus:border-[#3FB6B2] focus:outline-none focus:ring-4 focus:ring-[#3FB6B2]/15"
                  placeholder="예: Toss"
                  required
                />
              </label>

              <label className="space-y-2">
                <span className="text-sm font-bold text-gray-700">?ъ??섎챸</span>
                <input
                  value={positionName}
                  onChange={(e) => setPositionName(e.target.value)}
                  className="w-full rounded-2xl border border-gray-200 px-4 py-3 text-sm focus:border-[#3FB6B2] focus:outline-none focus:ring-4 focus:ring-[#3FB6B2]/15"
                  placeholder="?? Backend Engineer"
                  required
                />
              </label>
            </div>

            <label className="space-y-2 block">
              <span className="text-sm font-bold text-gray-700">怨듦퀬 URL</span>
              <input
                type="url"
                value={urlInput}
                onChange={(e) => setUrlInput(e.target.value)}
                className="w-full rounded-2xl border border-gray-200 px-4 py-3 text-sm focus:border-[#3FB6B2] focus:outline-none focus:ring-4 focus:ring-[#3FB6B2]/15"
                placeholder="https://..."
              />
              <p className="text-xs text-gray-500">URL???덉쑝硫?URL ?깅줉???곗꽑 泥섎━?⑸땲??</p>
            </label>

            <label className="space-y-2 block">
              <span className="text-sm font-bold text-gray-700">?먮낯 怨듦퀬 URL (TEXT ?깅줉 ???좏깮)</span>
              <input
                type="url"
                value={textSourceUrl}
                onChange={(e) => setTextSourceUrl(e.target.value)}
                className="w-full rounded-2xl border border-gray-200 px-4 py-3 text-sm focus:border-[#3FB6B2] focus:outline-none focus:ring-4 focus:ring-[#3FB6B2]/15"
                placeholder="https://..."
              />
            </label>

            <label className="space-y-2 block">
              <span className="text-sm font-bold text-gray-700">JD 蹂몃Ц</span>
              <textarea
                value={jdText}
                onChange={(e) => setJdText(e.target.value)}
                className="min-h-[320px] w-full rounded-2xl border border-gray-200 p-4 text-sm focus:border-[#3FB6B2] focus:outline-none focus:ring-4 focus:ring-[#3FB6B2]/15"
                placeholder="梨꾩슜怨듦퀬 蹂몃Ц ?띿뒪?몃? 遺숈뿬 ?ｌ뼱 二쇱꽭??"
              />
            </label>

            <button
              type="submit"
              disabled={isLoading}
              className="w-full rounded-2xl bg-[#3FB6B2] py-4 text-base font-bold text-white transition hover:bg-[#35A09D] disabled:opacity-60"
            >
              {isLoading ? '泥섎━ 以?..' : '?깅줉?섍린'}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}

